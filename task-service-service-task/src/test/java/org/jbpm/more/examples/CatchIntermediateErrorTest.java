package org.jbpm.more.examples;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;

import com.test.HandleExceptionServiceTaskHandler;
import com.test.MyService;

public class CatchIntermediateErrorTest extends JbpmJUnitBaseTestCase {

    private static final String defaultLanguage = "en-UK";

    @Test
    public void runthrowEscalationProcess() { 
        // Read in bpmn2
        RuntimeEngine runtimeEngine = createRuntimeManager("intermediate-throw-escalation-process.bpmn2").getRuntimeEngine(null);
        KieSession ksession = runtimeEngine.getKieSession();
        
        // Setup session, handlers, etc. 
        ksession.getWorkItemManager().registerWorkItemHandler("Service Task", new HandleExceptionServiceTaskHandler());
        
        Map<String, Object> processParams = new HashMap<String, Object>();
        
        // Input map for service (as an example -- can also be a string or whatever
        // - The input can also be generated *in* the process instead of being submitted
        // - regardless, it's always easier to submit "value holders" instead of values 
        //    (objects that contain values instead of the values themselves)
        Map<String, Object> serviceInputmap = new HashMap<String, Object>();
        String [] input = { "1 " , " 2" , "3." };
        serviceInputmap.put(MyService.MY_SERVICE_VAR, input );
        
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

}
