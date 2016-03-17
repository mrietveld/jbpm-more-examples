package org.jbpm.more.test;

import org.jbpm.more.UpgradeCommand;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;

/**
 * This is a sample file to test a process.
 */
public class DynamicBoundaryTimerProcessTest extends JbpmJUnitBaseTestCase {

	@Test
	public void testProcess() throws Exception {
		RuntimeManager manager = createRuntimeManager("dynamic-boundary-timer.bpmn2");
		RuntimeEngine engine = getRuntimeEngine(null);
		KieSession ksession = engine.getKieSession();

		ProcessInstance processInstance = ksession.startProcess("com.sample.bpmn.hello");
		System.out.println("Started process instance " + processInstance.getId());

		ksession.execute(new UpgradeCommand(processInstance.getId()));

		Thread.sleep(15000);

		System.out.println("Waited 15s ...");

		Thread.sleep(15000);

		System.out.println("Waited 30s ...");
		manager.disposeRuntimeEngine(engine);
		manager.close();
	}

	public DynamicBoundaryTimerProcessTest() {
		super(true, true);
	}

}