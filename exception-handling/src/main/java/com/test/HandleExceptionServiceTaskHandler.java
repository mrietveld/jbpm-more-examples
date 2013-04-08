package com.test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Much of this code is simply copy/pasted from
 * org.jbpm.bpmn2.handler.ServiceTaskHandler
 * 
 * 
 */
public class HandleExceptionServiceTaskHandler implements WorkItemHandler {

    public static String FAILURE_SERVICE = "com.test.failure.service";
    public static String FAILURE_OPERATION = "com.test.failure.operation";
    public static String FAILURE_PARAMETER_TYPE = "com.test.failure.parameter.type";
    public static String FAILURE_REASON = "com.test.failure.reason";
    public static String FAILURE_TRACE = "com.test.failure.trace";

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
