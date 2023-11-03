package de.intranda.goobi.plugins;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IDashboardPlugin;

import de.intranda.digiverso.model.helper.DashboardHelperTasks;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.CloseStepHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.HistoryManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class BarcodeDashboardPlugin implements IDashboardPlugin {

    private static final String PLUGIN_NAME = "intranda_dashboard_barcode";
    // optional actions
    private static final String ACTION_TAKE_NEW_TASK = "NEW";
    private static final String ACTION_FINISH_OLD_TASK = "DONE";
    private static final String ACTION_TAKE_NEW_AND_FINISH = "BOTH";
    private static final String ACTION_RELOCATION = "RELOC";
    // name of the process property used to hold the location
    private static final String PROPERTY_PROCESS_LOCATION = "process_location";
    // configuration entries in messages_*.properties for different header messages to print
    private static final String HEADER_PROCESS_NOT_FOUND = "plugin_dashboard_barcode_processNotFoundError";
    private static final String HEADER_STEP_NONE_AVAILABLE = "plugin_dashboard_barcode_stepNoneAvailable";
    private static final String HEADER_STEP_ASSIGNMENT_ERROR = "plugin_dashboard_barcode_stepAlreadyAssignedError";
    private static final String HEADER_STEP_ASSIGNMENT_SUCCESS = "plugin_dashboard_barcode_stepAssignedSuccess";
    private static final String HEADER_STEP_SAVING_ERROR = "plugin_dashboard_barcode_stepSavingError";
    private static final String HEADER_STEP_NONE_CLOSABLE = "plugin_dashboard_barcode_stepNoneToClose";
    private static final String HEADER_STEP_CLOSING_ERROR = "plugin_dashboard_barcode_stepClosingError";
    private static final String HEADER_STEP_CLOSING_SUCCESS = "plugin_dashboard_barcode_stepClosingSuccess";
    private static final String HEADER_RELOCATION_SUCCESS = "plugin_dashboard_barcode_relocatedTo";
    private static final String HEADER_RELOCATION_EMPTY_LOCATION_ERROR = "plugin_dashboard_barcode_emptyLocationError";

    @Getter
    private String title = PLUGIN_NAME;

    @Getter
    private PluginType type = PluginType.Dashboard;
    
    @Getter
    private String guiPath = "/uii/plugin_dashboard_barcode.xhtml";

    private XMLConfiguration pluginConfig;

    private transient DashboardHelperTasks tasksHelper;

    private User currentUser = Helper.getCurrentUser();

    @Getter
    @Setter
    private String action = ACTION_TAKE_NEW_TASK;
    @Getter
    private String location = ""; // target location for relocation, which will only be used when the option "relocation" is selected
    @Getter
    private String barcode = ""; // barcode is actually the title of the Goobi process

    public void setLocation(String location) {
        // location is set after action, hence we can use action's value to control
        if (ACTION_RELOCATION.equals(action)) {
            log.debug("setting location with " + location);
            this.location = location;
        }
    }

    public void setBarcode(String barcode) {
        if (StringUtils.isNotBlank(barcode)) {
            log.debug("setting up barcode with " + barcode);
            this.barcode = barcode;
        }
    }

    /**
     * Constructor
     */
    public BarcodeDashboardPlugin() {
        log.info("Barcode dashboard plugin started");
        pluginConfig = ConfigPlugins.getPluginConfig(PLUGIN_NAME);
    }   

    /* ======= Methods used for the DashboardHelperTasks part ======= */
    public DashboardHelperTasks getTasksHelper() {
        if (tasksHelper == null) {
            tasksHelper = new DashboardHelperTasks(pluginConfig);
        }
        return tasksHelper;
    }

    public String getFormattedDate(Date date) {
        if (date == null) {
            return "-";
        }
        DateFormat dateFormat = getDateFormat(DateFormat.DEFAULT);
        return dateFormat.format(date);
    }

    public String getFormattedTime(Date date) {
        if (date == null) {
            return "-";
        }
        DateFormat dateFormat = getTimeFormat(DateFormat.MEDIUM);
        return dateFormat.format(date);
    }

    private DateFormat getTimeFormat(int formatType) {
        DateFormat dateFormat = DateFormat.getTimeInstance(formatType);
        Locale userLang = FacesContextHelper.getCurrentFacesContext().getViewRoot().getLocale();
        if (userLang != null) {
            dateFormat = DateFormat.getTimeInstance(formatType, userLang);
        }
        return dateFormat;
    }

    private DateFormat getDateFormat(int formatType) {
        DateFormat dateFormat = DateFormat.getDateInstance(formatType);
        Locale userLang = FacesContextHelper.getCurrentFacesContext().getViewRoot().getLocale();
        if (userLang != null) {
            dateFormat = DateFormat.getDateInstance(formatType, userLang);
        }
        return dateFormat;
    }

    /* ======= Methods used for the barcode reader part ======= */
    /**
     * main method triggered by a click on the button for execution
     */
    public void execute() {
        Process process = ProcessManager.getProcessByExactTitle(barcode);
        if (process == null) {
            printMessage(HEADER_PROCESS_NOT_FOUND, "", "...", LogType.ERROR);
            return;
        }

        switch (action) {
            case ACTION_TAKE_NEW_TASK:
                takeNewTask(process);
                break;
            case ACTION_FINISH_OLD_TASK:
                finishOldTask(process);
                break;
            case ACTION_TAKE_NEW_AND_FINISH:
                takeAndFinishTask(process);
                break;
            case ACTION_RELOCATION:
                changeLocation(process);
                break;
            default:
                // no other options
        }
    }

    /**
     * method handling the option "take new tasks"
     * 
     * @param process Goobi process
     */
    private void takeNewTask(Process process) {
        // get all available tasks for the current user
        List<Step> availableSteps = getAvailableTasks(process);

        // assign the task(s) to the current user
        for (Step step : availableSteps) {
            assignStepToUser(step, currentUser);
        }
    }

    /**
     * get a list of available Goobi steps that can be assigned to the current user
     * 
     * @param process Goobi process
     * @return a list of Goobi steps which can be assigned to the current user
     */
    private List<Step> getAvailableTasks(Process process) {
        List<Step> results = new ArrayList<>();
        List<Step> steps = process.getSchritteList();
        for (Step step : steps) {
            if (isStepAvailableForUser(step, currentUser)) {
                results.add(step);
            }
        }

        if (results.isEmpty()) {
            printMessage(HEADER_STEP_NONE_AVAILABLE, "", ".", LogType.WARN);
        }

        return results;
    }

    /**
     * check if the Goobi step can be assigned to the given user
     * 
     * @param step Goobi step
     * @param user Goobi user
     * @return true if the step is open and not assigned yet and the user has right to accept it; false otherwise
     */
    private boolean isStepAvailableForUser(Step step, User user) {
        // check step status
        StepStatus status = step.getBearbeitungsstatusEnum();
        if (!StepStatus.OPEN.equals(status)) {
            return false;
        }

        // task still available, check settings of user group
        List<Usergroup> stepUserGroups = step.getBenutzergruppen();
        List<Usergroup> userGroups = user.getBenutzergruppen();
        for (Usergroup stepUserGroup : stepUserGroups) {
            for (Usergroup userGroup : userGroups) {
                if (stepUserGroup.equals(userGroup)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * assign a Goobi step to a Goobi user
     * 
     * @param step Goobi step
     * @param user Goobi user
     * @return true if the step is successfully assigned to the user, false if any error occurred
     */
    private boolean assignStepToUser(Step step, User user) {
        log.debug("the step '" + step.getTitel() + "' will be assigned to user: " + user.getNachVorname());

        step.setBearbeitungsbenutzer(user);
        step.setBearbeitungsstatusEnum(StepStatus.INWORK);
        step.setEditTypeEnum(StepEditType.MANUAL_SINGLE);
        Date date = new Date();
        step.setBearbeitungszeitpunkt(date);
        if (step.getBearbeitungsbeginn() == null) {
            step.setBearbeitungsbeginn(date);
        }

        try {
            StepManager.saveStep(step);

            HistoryManager.addHistory(step.getBearbeitungsbeginn(), step.getReihenfolge().doubleValue(),
                    step.getTitel(), HistoryEventType.stepInWork.getValue(), step.getProzess().getId());

            printMessage(HEADER_STEP_ASSIGNMENT_SUCCESS, step.getTitel(), LogType.INFO);

            return true;

        } catch (DAOException e) {
            printMessage(HEADER_STEP_SAVING_ERROR, step.getTitel(), LogType.ERROR);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * method handling the option "finish assigned tasks"
     * 
     * @param process Goobi process
     */
    private void finishOldTask(Process process) {
        // check if there is any task that is ready to finish
        List<Step> allSteps = process.getSchritte();
        boolean anyStepClosed = false;
        for (Step step : allSteps) {
            if (isStepCloseableForUser(step, currentUser)) {
                anyStepClosed = closeStep(step, currentUser) || anyStepClosed;
            }
        }

        if (!anyStepClosed) {
            printMessage(HEADER_STEP_NONE_CLOSABLE, "", ".", LogType.WARN);
        }
    }

    /**
     * check if the Goobi step can be closed by the given user
     * 
     * @param step Goobi step
     * @param user Goobi user
     * @return true if the step has been assigned to the given user and it has status INWORK
     */
    private boolean isStepCloseableForUser(Step step, User user) {
        User stepUser = step.getBearbeitungsbenutzer();
        return stepUser != null
                && StepStatus.INWORK.equals(step.getBearbeitungsstatusEnum())
                && stepUser.equals(user);
    }

    /**
     * close the Goobi step
     * 
     * @param step Goobi step
     * @param user Goobi user
     * @return true if the step is successfully closed, false if any exception occurred
     */
    private boolean closeStep(Step step, User user) {
        try {
            CloseStepHelper.closeStep(step, user);
            printMessage(HEADER_STEP_CLOSING_SUCCESS, step.getTitel(), LogType.INFO);
            return true;

        } catch (Exception e) {
            printMessage(HEADER_STEP_CLOSING_ERROR, step.getTitel(), LogType.ERROR);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * method handling the option "take new tasks and finish them immediately"
     * 
     * @param process Goobi process
     */
    private void takeAndFinishTask(Process process) {
        // get all available tasks for the current user
        List<Step> availableSteps = getAvailableTasks(process);

        // assign the task(s) to the current user
        for (Step step : availableSteps) {
            log.debug("assigning and closing step: " + step.getTitel());
            // use boolean chain to prevent closeStep from running when assignStepToUser did not succeed
            // the variable b here is only needed for syntax
            boolean b = assignStepToUser(step, currentUser) && closeStep(step, currentUser);
        }
    }

    /**
     * method handling the option "relocation"
     * 
     * @param process Goobi process
     */
    private void changeLocation(Process process) {
        log.debug("changing location to " + location);
        if (StringUtils.isBlank(location)) {
            printMessage(HEADER_RELOCATION_EMPTY_LOCATION_ERROR, "", "", LogType.ERROR);
            return;
        }

        saveLocationAsProperty(process, location);
        addJournalEntryForLocationChange(process, location);
    }

    /**
     * save the input location as a process property
     * 
     * @param process Goobi process
     * @param location location that shall be saved as a process property
     */
    private void saveLocationAsProperty(Process process, String location) {
        // get the Processproperty object
        Processproperty property = getProcesspropertyObject(process.getId(), PROPERTY_PROCESS_LOCATION, true);
        property.setWert(location);

        // save location to this Processproperty object
        PropertyManager.saveProcessProperty(property);
    }

    /**
     * get the Processproperty object
     * 
     * @param processId id of the Goobi process
     * @param title title of the Processproperty object
     * @param overwrite true if an existing old Processproperty object should be used, false if a new one should be created no matter what
     * @return the Processproperty object that is to be saved
     */
    private Processproperty getProcesspropertyObject(int processId, String title, boolean overwrite) {
        if (overwrite) {
            // try to retrieve the old object first
            List<Processproperty> props = PropertyManager.getProcessPropertiesForProcess(processId);
            for (Processproperty p : props) {
                if (title.equals(p.getTitel())) {
                    return p;
                }
            }
        }
        // otherwise, create a new one
        Processproperty property = new Processproperty();
        property.setTitel(title);
        property.setProcessId(processId);

        return property;
    }

    /**
     * add a journal entry for the change of location
     * 
     * @param process Goobi process
     * @param location new location
     */
    private void addJournalEntryForLocationChange(Process process, String location) {
        // prepare a message based on location
        String message = getMessageToPrint(HEADER_RELOCATION_SUCCESS, ": ", location);
        printMessage(message, LogType.INFO);

        // add journal entry
        Helper.addMessageToProcessJournal(process.getId(), LogType.INFO, message, currentUser.getNachVorname());
    }

    /**
     * print messages to the log as well as to the page
     * 
     * @param header message header whose translation shall be used
     * @param text message that shall be printed without translation, normally this is something that does not change w.r.t. locales e.g. a file name
     * @param logType LogType
     */
    private void printMessage(String header, String text, LogType logType) {
        printMessage(header, ": ", text, logType);
    }

    /**
     * print messages to the log as well as to the page
     * 
     * @param header message header whose translation shall be used
     * @param separator that shall be used to combine the translated header and the text
     * @param text message that shall be printed without translation, normally this is something that does not change w.r.t. locales e.g. a file name
     * @param logType LogType
     */
    private void printMessage(String header, String separator, String text, LogType logType) {
        String message = getMessageToPrint(header, separator, text);
        printMessage(message, logType);
    }

    /**
     * print messages to the log as well as to the page
     * 
     * @param message message that shall be printed
     * @param logType LogType
     */
    private void printMessage(String message, LogType logType) {
        switch (logType) {
            case ERROR:
                log.error(message);
                Helper.setFehlerMeldung(message);
                break;
            case DEBUG:
                log.debug(message);
                break;
            case WARN:
                log.warn(message);
                Helper.setFehlerMeldung(message);
                break;
            default: // INFO
                log.info(message);
                Helper.setMeldung(message);
                break;
        }
    }

    /**
     * get the message that shall be used to print
     * 
     * @param header message header whose translation shall be used
     * @param separator that shall be used to combine the translated header and the text
     * @param text message that shall be printed without translation, normally this is something that does not change w.r.t. locales e.g. a file name
     * @return the translated header and the text combined by the separator
     */
    private String getMessageToPrint(String header, String separator, String text) {
        return Helper.getTranslation(header) + separator + text;
    }

}
