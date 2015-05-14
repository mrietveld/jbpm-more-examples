package com.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.drools.core.common.InternalKnowledgeRuntime;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.timer.TimerInstance;
import org.jbpm.process.instance.timer.TimerManager;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;

/**
 * This is a sample file to test a process.
 */
public class ProcessTest extends JbpmJUnitBaseTestCase {

	@Test
	public void testProcess() throws Exception {
		RuntimeManager manager = createRuntimeManager("sample.bpmn");
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
	
	public class UpgradeCommand implements GenericCommand<Object> {
		private static final long serialVersionUID = 1L;
		
		private long processInstanceId;
		
		public UpgradeCommand(long processInstanceId) {
			this.processInstanceId = processInstanceId;
		}
		
		public Object execute(org.kie.internal.command.Context context) {
			KieSession ksession = ((KnowledgeCommandContext) context).getKieSession();
			RuleFlowProcessInstance wfp = (RuleFlowProcessInstance) 
				ksession.getProcessInstance(processInstanceId);
			HumanTaskNodeInstance taskNodeInstance = null;
			for (NodeInstance nodeInstance: wfp.getNodeInstances()) {
				if (nodeInstance instanceof HumanTaskNodeInstance) {
					taskNodeInstance = (HumanTaskNodeInstance) nodeInstance;
				}
			}
			TimerManager timerManager = ((InternalProcessRuntime) ((InternalKnowledgeRuntime)
				ksession).getProcessRuntime()).getTimerManager();
			List<Long> newTimerInstances = new ArrayList<Long>();
			for (long timerInstanceId: taskNodeInstance.getTimerInstances()) {
				System.out.println("Found timer " + timerInstanceId);
				TimerInstance oldTimerInstance = timerManager.getTimerMap().get(timerInstanceId);
				timerManager.cancelTimer(timerInstanceId);
				TimerInstance newTimerInstance = new TimerInstance();
				long timePassed = new Date().getTime() - oldTimerInstance.getActivated().getTime();
				// wait 25 second (minus time already passed)
				newTimerInstance.setDelay(25000 - timePassed);
				System.out.println("Setting timer delay " + newTimerInstance.getDelay());
				newTimerInstance.setPeriod(0);
				newTimerInstance.setTimerId(oldTimerInstance.getTimerId());
				timerManager.registerTimer(newTimerInstance, wfp);
				newTimerInstances.add(newTimerInstance.getId());
			}
			taskNodeInstance.internalSetTimerInstances(newTimerInstances);
			return null;
		}
	}

	
	public ProcessTest() {
		super(true, true);
	}

}