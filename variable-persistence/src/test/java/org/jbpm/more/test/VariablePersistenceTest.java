package org.jbpm.more.test;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.more.Person;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeManager;

public class VariablePersistenceTest extends JbpmJUnitBaseTestCase {

    @Test
    public void runthrowEscalationProcess() {
        // Read in bpmn2
        RuntimeManager runtimeManager = createRuntimeManager("variable-persistence-script-and-service.bpmn2");
        KieSession ksession = runtimeManager.getRuntimeEngine(null).getKieSession();

        Map<String, Object> processParams = new HashMap<String, Object>();

        processParams.put("name",  "sam");
        Person person = new Person();
        processParams.put("person",  person);

        // Run process
        ksession.startProcess("ErrorHandlerProcess", processParams);
    }

}
