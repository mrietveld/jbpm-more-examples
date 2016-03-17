package org.jbpm.more.test;

import java.io.InputStream;

import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorIO;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.conf.DeploymentDescriptor;

public class ChangeTaskPrioTest extends JbpmJUnitBaseTestCase {

    @Test
    public void runthrowEscalationProcess() {
        InputStream input = this.getClass().getResourceAsStream("/deployment/deployment-descriptor-defaults-and-ms.xml");
        DeploymentDescriptor descriptor = DeploymentDescriptorIO.fromXml(input);

        RuntimeEnvironment environment = RuntimeEnvironmentBuilder.Factory.get()
                .newDefaultBuilder()
                .addAsset(ResourceFactory.newClassPathResource("BPMN2-ScriptTask.bpmn2"), ResourceType.BPMN2)
                .addEnvironmentEntry("KieDeploymentDescriptor", descriptor)
                .get();

        //-----------------------------------------

        RuntimeManager manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(environment, "manager1");
        KieSession ksession = manager.getRuntimeEngine(null).getKieSession();

        ksession.startProcess("asdf");

    }

}
