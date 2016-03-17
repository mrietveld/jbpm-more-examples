package org.jbpm.more;

import java.util.List;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class ChangePrioWorkItemHandler implements WorkItemHandler {

    @Override
    public void executeWorkItem( WorkItem workItem, WorkItemManager manager ) {
        String deploymentId = ((WorkItemImpl) workItem).getDeploymentId();
        RuntimeManager rm = RuntimeManagerRegistry.get().getManager(deploymentId);
        RuntimeEngine engine = rm.getRuntimeEngine(ProcessInstanceIdContext.get());
        TaskService taskService = engine.getTaskService(); 
     
        String procInstIdStr = (String) workItem.getParameter("procInstId_input");
        List<Long> taskList = taskService.getTasksByProcessInstanceId(Long.parseLong(procInstIdStr));
        String userId = (String) workItem.getParameter("taskUserId");
        
        
//        ((org.kie.internal.task.api.InternalTaskService)taskService).setPriority(task.getId(), 2);

    }

    @Override
    public void abortWorkItem( WorkItem workItem, WorkItemManager manager ) {
        // DBG Auto-generated method stub
        
    }

}
