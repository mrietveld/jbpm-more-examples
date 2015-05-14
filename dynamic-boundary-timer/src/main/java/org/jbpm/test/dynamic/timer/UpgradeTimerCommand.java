package org.jbpm.test.dynamic.timer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.drools.core.common.InternalKnowledgeRuntime;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.timer.TimerInstance;
import org.jbpm.process.instance.timer.TimerManager;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.NodeInstance;

public class UpgradeTimerCommand implements GenericCommand<Object> {
    private static final long serialVersionUID = 1L;
    
    private final long processInstanceId;
    private final long timerSeconds;
    
    public UpgradeTimerCommand(long processInstanceId, long seconds) {
        this.processInstanceId = processInstanceId;
        this.timerSeconds = seconds;
    }
    
    public Object execute(org.kie.internal.command.Context context) {
        KieSession ksession = ((KnowledgeCommandContext) context).getKieSession();
        RuleFlowProcessInstance wfp = (RuleFlowProcessInstance) 
            ksession.getProcessInstance(processInstanceId);
        HumanTaskNodeInstance taskNodeInstance = null;
        for (NodeInstance nodeInstance: wfp.getNodeInstances()) {
            if (nodeInstance instanceof HumanTaskNodeInstance) {
                taskNodeInstance = (HumanTaskNodeInstance) nodeInstance;
            }
        }
        TimerManager timerManager = ((InternalProcessRuntime) ((InternalKnowledgeRuntime)
            ksession).getProcessRuntime()).getTimerManager();
        List<Long> newTimerInstances = new ArrayList<Long>();
        for (long timerInstanceId: taskNodeInstance.getTimerInstances()) {
            System.out.println("Found timer " + timerInstanceId);
            
            TimerInstance oldTimerInstance = timerManager.getTimerMap().get(timerInstanceId);
            timerManager.cancelTimer(timerInstanceId);
            TimerInstance newTimerInstance = new TimerInstance();
            long timePassed = new Date().getTime() - oldTimerInstance.getActivated().getTime();
            // wait timerSeconds seconds (minus time already passed)
            long delay = this.timerSeconds*1000 - timePassed;
            if( delay < 0 ) { 
                delay = 0;
            }
            newTimerInstance.setDelay(delay);
            System.out.println("Setting timer delay " + newTimerInstance.getDelay());
            
            newTimerInstance.setPeriod(0);
            newTimerInstance.setTimerId(oldTimerInstance.getTimerId());
            timerManager.registerTimer(newTimerInstance, wfp);
            newTimerInstances.add(newTimerInstance.getId());
        }
        taskNodeInstance.internalSetTimerInstances(newTimerInstances);
        return null;
    }
}