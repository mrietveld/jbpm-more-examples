package org.jbpm.more.test;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.more.HandleExceptionServiceTaskHandler;
import org.jbpm.more.BrokenService;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeManager;

public class CatchIntermediateErrorTest extends JbpmJUnitBaseTestCase {

    @Test
    public void runthrowEscalationProcess() {
        // Read in bpmn2
        RuntimeManager runtimeManager = createRuntimeManager("intermediate-throw-escalation-process.bpmn2");
        KieSession ksession = runtimeManager.getRuntimeEngine(null).getKieSession();

        // Setup session, handlers, etc.
        ksession.getWorkItemManager().registerWorkItemHandler("Service Task", new HandleExceptionServiceTaskHandler());

        Map<String, Object> processParams = new HashMap<String, Object>();

        // Input map for service (as an example -- can also be a string or whatever
        // - The input can also be generated *in* the process instead of being submitted
        // - regardless, it's always easier to submit "value holders" instead of values
        //    (objects that contain values instead of the values themselves)
        Map<String, Object> serviceInputmap = new HashMap<String, Object>();
        String [] input = { "1 " , " 2" , "3." };
        serviceInputmap.put(BrokenService.MY_SERVICE_VAR, input );

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
