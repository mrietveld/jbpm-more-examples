package org.jbpm.test.dynamic.timer;

import java.util.Date;

import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;

/**
 * This is a sample file to test a process.
 */
public class ProcessTest extends JbpmJUnitBaseTestCase {

	public ProcessTest() {
		super(true, true);
	}

	@Test
	public void testProcess() throws Exception {
		RuntimeManager manager = createRuntimeManager("dynamic-timer.bpmn2");
		RuntimeEngine engine = getRuntimeEngine(null);
		KieSession ksession = engine.getKieSession();
	
		// process has boundary timer on task for 3 seconds
		System.out.println( "Started process at " + new Date());
		ProcessInstance processInstance = ksession.startProcess("org.jbpm.test.dynamic.timer");
		System.out.println("Started process instance " + processInstance.getId());
	
		// upgrade timer to 5 seconds instead of 3
		ksession.execute(new UpgradeTimerCommand(processInstance.getId(), 5));
	
		long wait = 5;
		Thread.sleep(wait*1000);
		System.out.println("Waited " + wait + "s to end test");
		
		manager.disposeRuntimeEngine(engine);
		manager.close();
	}
	
}