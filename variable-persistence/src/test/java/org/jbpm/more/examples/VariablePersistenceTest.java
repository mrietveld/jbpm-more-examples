package org.jbpm.more.examples;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.more.examples.test.AbstractJbpmTest;
import org.junit.Test;
import org.kie.api.runtime.KieSession;

import com.test.Person;

public class VariablePersistenceTest extends AbstractJbpmTest {

    private static final String defaultLanguage = "en-UK";

    @Test
    public void runthrowEscalationProcess() { 
        // Read in bpmn2
        KieSession ksession = createKnowledgeSession("intermediate-throw-escalation-process.bpmn2");
        
        Map<String, Object> processParams = new HashMap<String, Object>();
        
        processParams.put("name",  "sam");
        Person person = new Person();
        processParams.put("person",  person);
        
        // Run process
        ksession.startProcess("ErrorHandlerProcess", processParams);
    }

}
