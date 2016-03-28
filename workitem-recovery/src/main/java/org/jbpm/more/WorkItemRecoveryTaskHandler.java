package org.jbpm.more;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.workflow.instance.WorkflowRuntimeException;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Much of this code is simply copy/pasted from
 * org.jbpm.bpmn2.handler.ServiceTaskHandler
 *
 *
 */
public class WorkItemRecoveryTaskHandler implements WorkItemHandler {

    public static final String DEPLOYMENT_ID = "RecoveryDeploymentId";
    public static final String PROC_INST_ID = "RecoveryProcInstId";
    public static final String NODE_INST_ID = "RecoveryNodeId";

    private static Logger logger = LoggerFactory.getLogger(WorkItemRecoveryTaskHandler.class);

    public WorkItemRecoveryTaskHandler() {
    }

    public void executeWorkItem( WorkItem workItem, WorkItemManager manager ) {
        // Retrieve info
        Map inputMap = workItem.getParameters();

        // Do actual work..
        String deploymentId = (String) inputMap.get(DEPLOYMENT_ID);
        String procInstIdStr = (String) inputMap.get(PROC_INST_ID);
        long procInstId = Long.parseLong(procInstIdStr);
        String nodeInstIdStr = (String) inputMap.get(NODE_INST_ID);
        long nodeId = Long.parseLong(nodeInstIdStr);

        System.out.println("RECOVERY: " + deploymentId + " [" + procInstId + ", " + nodeId + "]");

        RuntimeManager rm = RuntimeManagerRegistry.get().getManager(deploymentId);
        RuntimeEngine engine = rm.getRuntimeEngine(ProcessInstanceIdContext.get());
        WorkItemManager workItemManager = (org.drools.core.process.instance.WorkItemManager) engine.getKieSession().getWorkItemManager();

        Set<org.drools.core.process.instance.WorkItem> workItems = ((org.drools.core.process.instance.WorkItemManager) workItemManager).getWorkItems();
        List<WorkItem> recoveryWorkItems = new ArrayList<WorkItem>(2);
        for( WorkItem checkWorkItem : workItems ) {
            if( checkWorkItem.getProcessInstanceId() == procInstId ) {
                recoveryWorkItems.add(workItem);
                // TODO: log which ones..
            }
        }

        for( WorkItem recoverWorkitem : recoveryWorkItems ) {
            if( ((org.drools.core.process.instance.WorkItem) recoverWorkitem).getNodeId() == nodeId ) {
                workItemManager.completeWorkItem(recoverWorkitem.getId(), null);
                // only recover 1 at a time, to be safe
                break;
            }
        }

        // Complete THIS work item
        manager.completeWorkItem(workItem.getId(), null);
    }

    @Override
    public void abortWorkItem( WorkItem workItem, WorkItemManager manager ) {
        // DBG Auto-generated method stub

    }

}
