package org.jbpm.more;


import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class BrokenWorkItemHandler implements WorkItemHandler {

    @Override
    public void executeWorkItem( WorkItem workItem, WorkItemManager manager ) {
        throwExceptionSoThatWorkItemIsNOTCompleted(workItem);
        manager.completeWorkItem(workItem.getId(), null);
    }

    private void throwExceptionSoThatWorkItemIsNOTCompleted(WorkItem workItem) {
        throw new RuntimeException("Did not complete work item " + workItem.getName() + "/" + workItem.getId()
            + " from node " + ((org.drools.core.process.instance.WorkItem) workItem).getNodeId());
    }

    @Override
    public void abortWorkItem( WorkItem workItem, WorkItemManager manager ) {
        // do nothing
    }

}
