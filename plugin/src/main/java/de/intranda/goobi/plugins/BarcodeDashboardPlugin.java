package de.intranda.goobi.plugins;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IDashboardPlugin;

import de.intranda.digiverso.model.helper.DashboardHelperTasks;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class BarcodeDashboardPlugin implements IDashboardPlugin {

    private static final String PLUGIN_NAME = "intranda_dashboard_barcode";

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

}
