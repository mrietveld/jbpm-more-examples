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

		// Original timer duration is 8 seconds
		ProcessInstance processInstance = ksession.startProcess("com.sample.bpmn.hello");
		System.out.println("Started process instance " + processInstance.getId());

		// Upgrade to 3 seconds
		long newSleep = 3;
		ksession.execute(new UpgradeCommand(processInstance.getId(), newSleep*1000));

		// Sleep 3
		Thread.sleep(newSleep*1000);
		System.out.println("Waited " + newSleep + "s ...");

		ProcessInstance procInst = ksession.getProcessInstance(processInstance.getId());
		assertTrue( "The process should have completed earlier than initially planned..",
		        procInst == null || procInst.getState() == ProcessInstance.STATE_COMPLETED);

		manager.disposeRuntimeEngine(engine);
		manager.close();
	}

	public DynamicBoundaryTimerProcessTest() {
		super(true, true);
	}

}