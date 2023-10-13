package de.intranda.goobi.plugins;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IDashboardPlugin;

import de.intranda.digiverso.model.helper.DashboardHelperBarcode;
import de.intranda.digiverso.model.helper.DashboardHelperTasks;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class BarcodeDashboardPlugin implements IDashboardPlugin {

    private static final String PLUGIN_NAME = "intranda_dashboard_barcode";
    private static final String ACTION_OR = "OR";
    private static final String ACTION_AND = "AND";
    private static final String ACTION_RELOCATION = "LOC";
    private static final String PROPERTY_PROCESS_LOCATION = "process_location";

    @Getter
    private String title = "intranda_dashboard_barcode";

    @Getter
    private PluginType type = PluginType.Dashboard;
    
    @Getter
    private String guiPath = "/uii/plugin_dashboard_barcode.xhtml";
    
    @Getter
    private String value;

    private XMLConfiguration pluginConfig;

    private transient DashboardHelperTasks tasksHelper;
    private transient DashboardHelperBarcode barcodeHelper;

    private User currentUser = Helper.getCurrentUser();

    @Setter
    private List<String> choices;

    @Getter
    private String action = ACTION_OR;
    @Getter
    private String location = "";
    @Getter
    private String barcode = "";


    public void setAction(String action) {
        log.debug("setting up action with " + action);
        this.action = action;
    }

    public void setLocation(String location) {
        // location is set after action, hence we can use action's value to control
        if (ACTION_RELOCATION.equals(action)) {
            log.debug("setting location with " + location);
            this.location = location;
        }
    }

    public void setBarcode(String barcode) {
        log.debug("setting up barcode with " + barcode);
        // TODO: validate barcode
        this.barcode = barcode;
    }

    /**
     * Constructor
     */
    public BarcodeDashboardPlugin() {
        log.info("Barcode dashboard plugin started");
        value = ConfigPlugins.getPluginConfig(title).getString("value", "default value");
        pluginConfig = ConfigPlugins.getPluginConfig(PLUGIN_NAME);
    }   

    public DashboardHelperTasks getTasksHelper() {
        if (tasksHelper == null) {
            tasksHelper = new DashboardHelperTasks(pluginConfig);
        }
        return tasksHelper;
    }

    public DashboardHelperBarcode getBarcodeHelper() {
        log.info("getting barcodeHelper");
        if (barcodeHelper == null) {
            barcodeHelper = new DashboardHelperBarcode(pluginConfig);
        }
        return barcodeHelper;
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

    public void execute() {
        // get process title based on the input barcode
        Process process = getProcess(barcode);
        if (process == null) {
            log.debug("process not found, aborting...");
            return;
        }

        switch (action) {
            case "OR":
                takeOrFinishTask(process);
                break;
            case "AND":
                takeAndFinishTask(process);
                break;
            case "LOC":
                changeLocation(process);
                break;
            default:
                // no other options
        }

    }

    private Process getProcess(String barcode) {
        //        String processTitle = getProcessTitle(barcode);
        Process process = ProcessManager.getProcessByExactTitle(barcode);

        return process;
    }

    private String getProcessTitle(String barcode) {
        // for process title that are typed into the text input
        return barcode;
    }

    private void takeOrFinishTask(Process process) {
        boolean success = true;
        log.debug("current user is: " + currentUser.getNachVorname());
        log.debug("current user has id = " + currentUser.getId());
        List<Step> currentUserStepsInWork = currentUser.getBearbeitungsschritte();
        List<Usergroup> currentUserUsergroups = currentUser.getBenutzergruppen();
        if (currentUserStepsInWork != null) {
            log.debug("current user has following tasks to do:");
            for (Step step : currentUserStepsInWork) {
                log.debug(step.getTitel());
            }
        }
        if (currentUserUsergroups != null) {
            log.debug("current user belongs to the following user groups:");
            for (Usergroup group : currentUserUsergroups) {
                log.debug(group.getTitel());
            }
        }

        List<Step> steps = process.getSchritteList();
        for (Step step : steps) {
            log.debug("------- step " + step.getTitel() + " -------");
            log.debug("This step has status: " + step.getBearbeitungsstatusAsString());
            User user = step.getBearbeitungsbenutzer();
            log.debug("user = " + String.valueOf(user));
            List<User> users = step.getBenutzerList();
            List<Usergroup> userGroups = step.getBenutzergruppenList();
            //            step.setBearbeitungsstatusEnum(StepStatus.INWORK);
            log.debug("This step has following users:");
            for (User u : users) {
                log.debug(u.getNachVorname());
            }
            log.debug("This step has following user groups:");
            for (Usergroup group : userGroups) {
                log.debug(group.getTitel());
            }
        }

    }

    private void takeAndFinishTask(Process process) {
        boolean success = takeTask(process) && finishTask(process);
    }

    private boolean takeTask(Process process) {
        log.debug("taking a new task");

        return true;
    }

    private boolean finishTask(Process process) {
        log.debug("finishing a task");

        return true;
    }

    private boolean changeLocation(Process process) {
        log.debug("changing location to " + location);

        return saveLocationAsProperty(process, location) && addJournalEntryForLocationChange(process, location);
    }

    private boolean saveLocationAsProperty(Process process, String location) {
        // get the Processproperty object
        Processproperty property = getProcesspropertyObject(process.getId(), PROPERTY_PROCESS_LOCATION, true);
        property.setWert(location);

        // save location to this Processproperty object
        PropertyManager.saveProcessProperty(property);

        return true;
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

    private boolean addJournalEntryForLocationChange(Process process, String location) {
        // prepare a message based on location
        String message = "Relocated to: " + location;

        // add journal entry
        Helper.addMessageToProcessJournal(process.getId(), LogType.INFO, message, currentUser.getNachVorname());

        return true;
    }

    public List<String> getChoices() {
        String choice1 = "plugin_dashboard_barcode_takeOrFinishTask";
        String choice2 = "plugin_dashboard_barcode_takeAndFinishTask";
        String choice3 = "plugin_dashboard_barcode_changeLocationOnly";
        return Arrays.asList(choice1, choice2, choice3).stream().map(Helper::getTranslation).collect(Collectors.toList());
    }

}
