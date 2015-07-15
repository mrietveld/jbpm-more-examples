package org.jbpm.more.examples;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;

import com.test.Person;

public class VariablePersistenceTest extends JbpmJUnitBaseTestCase {

    private static final String defaultLanguage = "en-UK";

    @Test
    public void runthrowEscalationProcess() { 
        // Read in bpmn2
        RuntimeEngine runtimeEngine = createRuntimeManager("intermediate-throw-escalation-process.bpmn2").getRuntimeEngine(null);
        KieSession ksession = runtimeEngine.getKieSession();
        
        Map<String, Object> processParams = new HashMap<String, Object>();
        
        processParams.put("name",  "sam");
        Person person = new Person();
        processParams.put("person",  person);
        
        // Run process
        ksession.startProcess("ErrorHandlerProcess", processParams);
    }

}
