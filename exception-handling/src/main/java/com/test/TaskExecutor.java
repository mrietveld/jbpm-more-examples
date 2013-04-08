package com.test;

import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;


public abstract class TaskExecutor implements WorkItemHandler {
	
	public abstract Map<String, Object> runTask(Map<String, Object> input)throws Exception;

	private final StatefulKnowledgeSession ksession = null;
	
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Map<String, Object> input = workItem.getParameters();
        try { 
            workItem.getResults().putAll(runTask(input));
        } catch( Throwable t ){ 
            StatefulKnowledgeSession ksession = getKnowledgeSession(workItem);
            ksession.signalEvent("exception-signal", workItem);
        } 
        
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        
    }
    
    private StatefulKnowledgeSession getKnowledgeSession(WorkItem workItem) { 
        StatefulKnowledgeSession ksession = null;
        
        return ksession;
    }

}

