package org.jbpm.test.exceptions;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Much of this code is simply copy/pasted from
 * org.jbpm.bpmn2.handler.ServiceTaskHandler
 * 
 * 
 */
public class HandleExceptionServiceTaskHandler implements WorkItemHandler {

    public static final String FAILURE_SERVICE = "org.jbpm.test.exceptions.failure.service";
    public static final String FAILURE_OPERATION = "org.jbpm.test.exceptions.failure.operation";
    public static final String FAILURE_PARAMETER_TYPE = "org.jbpm.test.exceptions.failure.parameter.type";
    
    public static final String FAILURE_EXCEPTION = "org.jbpm.test.exceptions.failure.exception";
    public static final String FAILURE_REASON = "org.jbpm.test.exceptions.failure.reason";
    public static final String FAILURE_TRACE = "org.jbpm.test.exceptions.failure.trace";

    private static Logger logger = LoggerFactory.getLogger(HandleExceptionServiceTaskHandler.class);

    public HandleExceptionServiceTaskHandler() {
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        // Retrieve info
        String serviceInterface = (String) workItem.getParameter("Interface");
        String operation = (String) workItem.getParameter("Operation");
        String parameterType = (String) workItem.getParameter("ParameterType");
        Object parameter = workItem.getParameter("Parameter");

        Map inputMap = null;
        if( parameter instanceof Map ) { 
           inputMap = (Map) parameter; 
        }
        
        // Call Service (class)
        try {
            Class<?> c = Class.forName(serviceInterface);
            Object instance = c.newInstance();
            Class<?>[] classes = null;
            Object[] params = null;
            if (parameterType != null) {
                classes = new Class<?>[] { Class.forName(parameterType) };
                params = new Object[] { parameter };
            }
            Method method = c.getMethod(operation, classes);
            Object result = method.invoke(instance, params);

            // The code that then saves the failure information if the service failed
            if( result instanceof Boolean ) { 
               if( ! (Boolean) result )  {
                  addFailureInformation(workItem, inputMap); 
               }
            }
            
            // Store results
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("Result", result);

            // Complete work item
            manager.completeWorkItem(workItem.getId(), results);
        } catch (Throwable t) {
            handleException(workItem, inputMap, t, manager);
        }
    }

    /**
     * This method saves the failure information to a Map -- the Map instance
     * should be a process variable so that other nodes can access this information. 
     * </p>
     * This example only deals with *1* service call -- but if you're dealing with 
     * multiple service calls (or multiple instances), you could change your Map<String, Object> to 
     * a Map<Long, Map<String, Object>> which you could then use to associate a work item id (or other
     * id) with the (failure) information for a service. 
     * </p>
     * Lastly, since you're using persistence, remember to make sure that if you use something besides a 
     * Map to hold this information, then you need to make that "something" object Serializable as well.
     * 
     * @param workItem The workItem associated with the failed service call. 
     * @param inputMap A process variable of the form Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    private void addFailureInformation(WorkItem workItem, Map inputMap) { 
        String serviceInterface = (String) workItem.getParameter("Interface");
        inputMap.put(FAILURE_SERVICE, serviceInterface );
        String operation = (String) workItem.getParameter("Operation");
        inputMap.put(FAILURE_OPERATION, operation );
        String parameterType = (String) workItem.getParameter("ParameterType");
        inputMap.put(FAILURE_PARAMETER_TYPE, parameterType );
    }
    
    @SuppressWarnings("unchecked")
    private void handleException(WorkItem workItem, Map inputMap, Throwable t, WorkItemManager manager) {
        addFailureInformation(workItem, inputMap);
        
        inputMap.put(FAILURE_REASON, t.getClass().getSimpleName() + ": " + t.getMessage());
        inputMap.put(FAILURE_TRACE, t.getStackTrace());

        // otherwise, the process stays hung
        manager.abortWorkItem(workItem.getId());
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        workItem.getResults().put(FAILURE_REASON, "Work item aborted.");
        manager.abortWorkItem(workItem.getId());
    }

}
