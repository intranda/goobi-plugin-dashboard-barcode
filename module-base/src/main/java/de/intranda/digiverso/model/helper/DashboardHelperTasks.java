/**
 * This file is part of a plugin for the Goobi Application - a Workflow tool for the support of mass digitization.
 * <p>
 * Visit the websites for more information. - https://goobi.io - https://www.intranda.com - https://github.com/intranda/goobi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * <p>
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package de.intranda.digiverso.model.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.managedbeans.StepBean;

import de.intranda.digiverso.model.tasks.TaskChangeType;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ControllingManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.Getter;
import lombok.Setter;

public class DashboardHelperTasks {

    private XMLConfiguration config;
    private int showLastChangesSize;
    @Getter
    private List<TaskChangeType> taskChangeHistory = new ArrayList<>();
    @Getter
    @Setter
    private TaskChangeType currentStep;

    public DashboardHelperTasks(XMLConfiguration pluginConfig) {
        config = pluginConfig;
        showLastChangesSize = config.getInt("tasks-latestChanges-size", 10);

        User user = Helper.getCurrentUser();
        if (user != null) {

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT schritte.ProzesseID, schritte.SchritteID, schritte.titel");
            sql.append(" FROM schritte LEFT JOIN prozesse ON schritte.ProzesseID = prozesse.ProzesseID");
            sql.append(" WHERE Bearbeitungsstatus = 3 AND BearbeitungsBenutzerID = " + user.getId() + " AND prozesse.IstTemplate = 0");
            sql.append(" ORDER BY BearbeitungsEnde DESC LIMIT " + showLastChangesSize + ";");

            List<Object[]> rawvalues = ControllingManager.getResultsAsObjectList(sql.toString());
            for (Object[] objArr : rawvalues) {
                String processId = (String) objArr[0];
                String stepId = (String) objArr[1];
                Step currentStep = null;
                Step followingStep = null;
                Process process = ProcessManager.getProcessById(Integer.parseInt(processId));
                for (Step step : process.getSchritte()) {
                    if (step.getId().equals(Integer.valueOf(stepId))) {
                        currentStep = step;
                    } else if (currentStep != null && followingStep == null) {
                        followingStep = step;
                        break;
                    }
                }
                if (currentStep != null) {
                    TaskChangeType tct = new TaskChangeType(currentStep, followingStep, process);
                    taskChangeHistory.add(tct);
                }
            }
            if (taskChangeHistory.size() > 1) {
                // Ensure null-safe comparison for the date
                taskChangeHistory.sort(Comparator.comparing(
                        taskChangeType -> taskChangeType.getClosedStep().getBearbeitungsende(),
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }

    }

    public String reOpenTask() {
        currentStep.getClosedStep().setBearbeitungsstatusEnum(StepStatus.INWORK);
        currentStep.getClosedStep().setBearbeitungsende(null);
        if (currentStep.getFollowingStep() != null) {
            currentStep.getFollowingStep().setBearbeitungsstatusEnum(StepStatus.LOCKED);
        }
        try {
            ProcessManager.saveProcess(currentStep.getProcess());
        } catch (DAOException e) {
        }

        StepBean bean = Helper.getBeanByClass(StepBean.class);
        bean.setMySchritt(currentStep.getClosedStep());

        return "task_edit";
    }

}
