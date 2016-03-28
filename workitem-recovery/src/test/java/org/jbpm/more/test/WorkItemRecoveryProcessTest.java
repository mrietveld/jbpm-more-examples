package org.jbpm.more.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jbpm.more.BrokenWorkItemHandler;
import org.jbpm.more.WorkItemRecoveryTaskHandler;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.jbpm.workflow.instance.WorkflowRuntimeException;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;

import com.sun.xml.bind.v2.model.runtime.RuntimeReferencePropertyInfo;

public class WorkItemRecoveryProcessTest extends JbpmJUnitBaseTestCase {

    private static final String BROKEN_WORKITEM_PROCESS_ID = "org.jbpm.more.broken.workitem";
    private static final String WORKITEM_RECOVERY_PROCESS_ID = "org.jbpm.more.workitem.recovery";

    private static final Pattern nodeIdRegex = Pattern.compile(".*from node (\\d)");

    @Test
    public void workItemRecoveryTest() {

        // setup
        RuntimeManager runtimeManager = createRuntimeManager("workitem-incomplete.bpmn2", "workitem-recovery.bpmn2");
        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);
        KieSession kieSession = runtimeEngine.getKieSession();
        kieSession.getWorkItemManager().registerWorkItemHandler("Broken", new BrokenWorkItemHandler());
        kieSession.getWorkItemManager().registerWorkItemHandler("Recovery", new WorkItemRecoveryTaskHandler());
        String deploymentId = runtimeManager.getIdentifier();

        Map<Long, Long> procInstIdNodeIdMap = new HashMap<Long, Long>();

        for( int i = 0; i < 5; ++i ) {
            try {
                kieSession.startProcess(BROKEN_WORKITEM_PROCESS_ID);
            } catch (WorkflowRuntimeException wre) {
                procInstIdNodeIdMap.put(wre.getProcessInstanceId(), wre.getNodeId());
            }
        }

        for( Entry<Long, Long> procInstIdNodeIdEntry : procInstIdNodeIdMap.entrySet() ) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("deploymentId", deploymentId);
            params.put("procInstId", procInstIdNodeIdEntry.getKey().toString());
            params.put("nodeId", procInstIdNodeIdEntry.getValue().toString());
            kieSession.startProcess(WORKITEM_RECOVERY_PROCESS_ID, params);
        }

        runtimeManager.disposeRuntimeEngine(runtimeEngine);
        runtimeManager.close();
    }
}
