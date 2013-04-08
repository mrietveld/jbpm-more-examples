package org.jbpm;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.bpmn2.handler.ServiceTaskHandler;
import org.jbpm.task.TaskService;
import org.jbpm.task.query.TaskSummary;
import org.junit.Test;

import com.test.HandleExceptionServiceTaskHandler;

public class CatchIntermediateErrorTest extends AbstractJbpmTest {

    private static final String defaultLanguage = "en-UK";

    @Test
    public void runthrowEscalationProcess() { 
        // Read in bpmn2
        StatefulKnowledgeSession ksession = createKnowledgeSession("intermediate-throw-escalation-process.bpmn2");
        
        // Setup session, handlers, etc. 
        ksession.getWorkItemManager().registerWorkItemHandler("Service Task", new HandleExceptionServiceTaskHandler());
        
        Map<String, Object> processParams = new HashMap<String, Object>();
        
        // Input map for service (as an example -- can also be a string or whatever
        // - The input can also be generated *in* the process instead of being submitted
        // - regardless, it's always easier to submit "value holders" instead of values 
        //    (objects that contain values instead of the values themselves)
        Map<String, Object> serviceInputmap = new HashMap<String, Object>();
        int [] input = { 1 , 2 , 3};
        serviceInputmap.put("service-input-var", input );
        /**
         *   "serviceInput" 
         * corresponds to 
         *   <property id="serviceInput" itemSubjectRef="_serviceMessageItem" />
         * in the bpmn2
         */   
        processParams.put("serviceInput", serviceInputmap);
        
        // Run process
        ksession.startProcess("ErrorHandlerProcess", processParams);
    }

    /**
        TaskService taskService = getAndRegisterTaskService(ksession);
        
        ProcessInstance processInstance = ksession.startProcess("adsf");

        // Retrieve and complete task
        String userId = "playerOne";
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(userId, defaultLanguage);
        long taskId = tasks.get(0).getId();
        taskService.getTask(taskId).getTaskData().get
        taskService.complete(taskId, userId, null);
    }
    
            // start a new process instance
            HandleExceptionServiceTaskHandler raiseExceptionTaskhandler = new HandleExceptionServiceTaskHandler(ksession);
            
            ksession.getWorkItemManager().registerWorkItemHandler("Service Task",raiseExceptionTaskhandler);
            
            //GenericHTWorkItemHandler humanTaskWorkItemHandler;
            //AsyncMinaTaskClient asyncMinaTaskClient=new AsyncMinaTaskClient();
            
            
            //SyncTaskServiceWrapper syncTaskSeviceWrapper=new SyncTaskServiceWrapper(asyncMinaTaskClient);
            
            //humanTaskWorkItemHandler=new MinaHTWorkItemHandler(syncTaskSeviceWrapper, ksession);
            
            //
            //ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskWorkItemHandler);
            Map<String, Object> params = new HashMap<String, Object>();
            //params.put("raiseException", false);
            
            ksession.startProcess("ErrorHandler", params);
            
            Collection<ProcessInstance>coll=ksession.getProcessInstances();
            //ksession.getProcessInstance(coll.)
            
            
            //logger.close();
            System.out.println("Process Completed");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    */
}
