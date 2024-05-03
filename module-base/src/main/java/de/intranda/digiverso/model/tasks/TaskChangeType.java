package de.intranda.digiverso.model.tasks;

import org.goobi.beans.Process;
import org.goobi.beans.Step;

import de.sub.goobi.helper.enums.StepStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class TaskChangeType {

    @Getter
    private Step closedStep;
    @Getter
    private Step followingStep;

    @Getter
    private Process process;

    /**
     * Check if the closed step can be opened again. This is possible, when it is either the last step of the process or when the following step still
     * has the status open.
     * 
     * @return
     */

    public boolean isAssignable() {
        return followingStep == null || followingStep.getBearbeitungsstatusEnum() == StepStatus.OPEN
                || followingStep.getBearbeitungsstatusEnum() == StepStatus.LOCKED;
    }

}
