package org.jbpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import junit.framework.Assert;

import org.drools.ClockType;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListenerFactory;
import org.drools.audit.WorkingMemoryInMemoryLogger;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.definition.process.Node;
import org.drools.impl.EnvironmentFactory;
import org.drools.io.ResourceFactory;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.process.NodeInstance;
import org.drools.runtime.process.NodeInstanceContainer;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.h2.tools.DeleteDbFiles;
import org.jbpm.process.audit.JPAProcessInstanceDbLog;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.workitem.wsht.SyncWSHumanTaskHandler;
import org.jbpm.task.TaskService;
import org.jbpm.task.service.local.LocalTaskService;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 * Base test case for the jbpm-bpmn2 module.
 * 
 * Please keep this test class in the org.jbpm.bpmn2 package or otherwise give it a unique name.
 * 
 */
public abstract class AbstractJbpmTest extends Assert {

    protected final static String EOL = System.getProperty("line.separator");

    protected EntityManagerFactory emf;
    protected PoolingDataSource ds;

    protected Logger logger = null;
    protected String dbFilename = "jbpm-test";

    @Rule
    public TestName testName = new TestName();

    protected AbstractJbpmTest() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    public static PoolingDataSource setupPoolingDataSource(String dbFilename) {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setUniqueName("jdbc/jbpmDS");
        pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
        pds.setMaxPoolSize(5);
        pds.setAllowLocalTransactions(true);
        pds.getDriverProperties().put("user", "sa");
        pds.getDriverProperties().put("password", "");
        pds.getDriverProperties().put("url", "jdbc:h2:file:" + dbFilename + ";MVCC=TRUE");
        pds.getDriverProperties().put("driverClassName", "org.h2.Driver");
        pds.init();
        return pds;
    }

    @Before
    public void setUp() throws Exception {
        if (logger == null) {
            logger = LoggerFactory.getLogger(getClass());
        }
        ds = setupPoolingDataSource(dbFilename);
        emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
    }

    @After
    public void tearDown() throws Exception {
        if (emf != null) {
            emf.close();
            emf = null;
        }
        if (ds != null) {
            ds.close();
            ds = null;
        }
        DeleteDbFiles.execute(".", dbFilename, true);

        // Clean up possible transactions
        Transaction tx = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
        if (tx != null) {
            int testTxState = tx.getStatus();
            if (testTxState != Status.STATUS_NO_TRANSACTION && testTxState != Status.STATUS_ROLLEDBACK
                    && testTxState != Status.STATUS_COMMITTED) {
                String txStatus = null;
                switch (testTxState) {
                case Status.STATUS_ACTIVE:
                    txStatus = "active";
                    break;
                case Status.STATUS_COMMITTING:
                    txStatus = "committing";
                    break;
                case Status.STATUS_MARKED_ROLLBACK:
                    txStatus = "marked for rollback";
                    break;
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    txStatus = "prepared to commit";
                    break;
                case Status.STATUS_ROLLING_BACK:
                    txStatus = "currently rolling back";
                    break;
                case Status.STATUS_UNKNOWN:
                    txStatus = "unknown";
                    break;
                }
                try {
                    tx.rollback();
                } catch (Throwable t) {
                    // do nothing..
                }
                fail("The transaction should be closed but has status '" + txStatus + "'");
            }
        }
    }

    protected KnowledgeBase createKnowledgeBase(String... bpmn2Filename) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        for (String f : bpmn2Filename) {
            kbuilder.add(ResourceFactory.newClassPathResource(f), ResourceType.BPMN2);
        }

        // Check for errors
        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                boolean errors = false;
                for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                    logger.warn(error.toString());
                    errors = true;
                }
                assertFalse("Could not build knowldge base.", errors);
            }
        }
        return kbuilder.newKnowledgeBase();
    }

    protected StatefulKnowledgeSession createKnowledgeSession(KnowledgeBase kbase) {
        StatefulKnowledgeSession ksession;
        final KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        // Do NOT use the Pseudo clock yet..
        // conf.setOption( ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ) );

        Environment env = EnvironmentFactory.newEnvironment();
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
        ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, conf, env);

        // logging
        new JPAWorkingMemoryDbLogger(ksession);
        JPAProcessInstanceDbLog.setEnvironment(env);

        return ksession;
    }

    protected StatefulKnowledgeSession createKnowledgeSession(String... bpmn2Filename) {
        KnowledgeBase kbase = createKnowledgeBase(bpmn2Filename);
        return createKnowledgeSession(kbase);
    }

    public StatefulKnowledgeSession reloadSession(StatefulKnowledgeSession ksession) throws SystemException {
        int id = ksession.getId();

        KnowledgeBase kbase = ksession.getKnowledgeBase();

        // Close/clean-up
        KnowledgeSessionConfiguration config = ksession.getSessionConfiguration();
        ksession.dispose();
        emf.close();

        ksession.dispose();

        // Reload
        return loadSession(id, kbase);
    }

    public StatefulKnowledgeSession loadSession(int id, String... process) {
        KnowledgeBase kbase = createKnowledgeBase(process);
        return loadSession(id, kbase);
    }

    protected StatefulKnowledgeSession loadSession(int id, KnowledgeBase kbase) {
        Environment env = EnvironmentFactory.newEnvironment();
        emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);

        KnowledgeSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        StatefulKnowledgeSession ksession = JPAKnowledgeService.loadStatefulKnowledgeSession(id, kbase, config, env);

        // logging
        new JPAWorkingMemoryDbLogger(ksession);
        JPAProcessInstanceDbLog.setEnvironment(env);

        return ksession;
    }

    public TaskService getAndRegisterTaskService(StatefulKnowledgeSession ksession) {
        // Create task service (using hornetq? substitute the correct code here.. )
        org.jbpm.task.service.TaskService taskService = new org.jbpm.task.service.TaskService(emf,
                SystemEventListenerFactory.getSystemEventListener());
        LocalTaskService localTaskService = new LocalTaskService(taskService);
        
        // work item handler
        SyncWSHumanTaskHandler humanTaskHandler = new SyncWSHumanTaskHandler(localTaskService, ksession);
        humanTaskHandler.setLocal(true);
        humanTaskHandler.connect();
       
        // Register the handler
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
        
        return localTaskService;
    }

    protected void clearHistory() {
        JPAProcessInstanceDbLog.clear();
    }

    public Object getVariableValue(String name, long processInstanceId, StatefulKnowledgeSession ksession) {
        return ((WorkflowProcessInstance) ksession.getProcessInstance(processInstanceId)).getVariable(name);
    }

    // assert functions ---------------------------------------------------------------------------------------

    public void assertProcessInstanceCompleted(long processInstanceId, StatefulKnowledgeSession ksession) {
        assertNull(ksession.getProcessInstance(processInstanceId));
    }

    public void assertProcessInstanceAborted(long processInstanceId, StatefulKnowledgeSession ksession) {
        assertNull(ksession.getProcessInstance(processInstanceId));
    }

    public void assertProcessInstanceActive(long processInstanceId, StatefulKnowledgeSession ksession) {
        assertNotNull(ksession.getProcessInstance(processInstanceId));
    }

    public void assertNodeActive(long processInstanceId, StatefulKnowledgeSession ksession, String... name) {
        List<String> names = new ArrayList<String>();
        for (String n : name) {
            names.add(n);
        }
        ProcessInstance processInstance = ksession.getProcessInstance(processInstanceId);
        if (processInstance instanceof WorkflowProcessInstance) {
            assertNodeActive((WorkflowProcessInstance) processInstance, names);
        }
        if (!names.isEmpty()) {
            String s = names.get(0);
            for (int i = 1; i < names.size(); i++) {
                s += ", " + names.get(i);
            }
            fail("Node(s) not active: " + s);
        }
    }

    private void assertNodeActive(NodeInstanceContainer container, List<String> names) {
        for (NodeInstance nodeInstance : container.getNodeInstances()) {
            String nodeName = nodeInstance.getNodeName();
            if (names.contains(nodeName)) {
                names.remove(nodeName);
            }
            if (nodeInstance instanceof NodeInstanceContainer) {
                assertNodeActive((NodeInstanceContainer) nodeInstance, names);
            }
        }
    }

    public void assertNodeTriggered(long processInstanceId, String... nodeNames) {
        List<String> names = new ArrayList<String>();
        for (String nodeName : nodeNames) {
            names.add(nodeName);
        }
        List<NodeInstanceLog> logs = JPAProcessInstanceDbLog.findNodeInstances(processInstanceId);
        if (logs != null) {
            for (NodeInstanceLog l : logs) {
                String nodeName = l.getNodeName();
                if (l.getType() == NodeInstanceLog.TYPE_ENTER && names.contains(nodeName)) {
                    names.remove(nodeName);
                }
            }
        }
        if (!names.isEmpty()) {
            String s = names.get(0);
            for (int i = 1; i < names.size(); i++) {
                s += ", " + names.get(i);
            }
            fail("Node(s) not executed: " + s);
        }
    }

    public void assertProcessVarExists(ProcessInstance process, String... processVarNames) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        List<String> names = new ArrayList<String>();
        for (String nodeName : processVarNames) {
            names.add(nodeName);
        }

        for (String pvar : instance.getVariables().keySet()) {
            if (names.contains(pvar)) {
                names.remove(pvar);
            }
        }

        if (!names.isEmpty()) {
            String s = names.get(0);
            for (int i = 1; i < names.size(); i++) {
                s += ", " + names.get(i);
            }
            fail("Process Variable(s) do not exist: " + s);
        }

    }

    public void assertNodeExists(ProcessInstance process, String... nodeNames) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        List<String> names = new ArrayList<String>();
        for (String nodeName : nodeNames) {
            names.add(nodeName);
        }

        for (Node node : instance.getNodeContainer().getNodes()) {
            if (names.contains(node.getName())) {
                names.remove(node.getName());
            }
        }

        if (!names.isEmpty()) {
            String s = names.get(0);
            for (int i = 1; i < names.size(); i++) {
                s += ", " + names.get(i);
            }
            fail("Node(s) do not exist: " + s);
        }
    }

    public void assertNumOfIncommingConnections(ProcessInstance process, String nodeName, int num) {
        assertNodeExists(process, nodeName);
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        for (Node node : instance.getNodeContainer().getNodes()) {
            if (node.getName().equals(nodeName)) {
                if (node.getIncomingConnections().size() != num) {
                    fail("Expected incomming connections: " + num + " - found " + node.getIncomingConnections().size());
                } else {
                    break;
                }
            }
        }
    }

    public void assertNumOfOutgoingConnections(ProcessInstance process, String nodeName, int num) {
        assertNodeExists(process, nodeName);
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        for (Node node : instance.getNodeContainer().getNodes()) {
            if (node.getName().equals(nodeName)) {
                if (node.getOutgoingConnections().size() != num) {
                    fail("Expected outgoing connections: " + num + " - found " + node.getOutgoingConnections().size());
                } else {
                    break;
                }
            }
        }
    }

    public void assertVersionEquals(ProcessInstance process, String version) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        if (!instance.getWorkflowProcess().getVersion().equals(version)) {
            fail("Expected version: " + version + " - found " + instance.getWorkflowProcess().getVersion());
        }
    }

    public void assertProcessNameEquals(ProcessInstance process, String name) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        if (!instance.getWorkflowProcess().getName().equals(name)) {
            fail("Expected name: " + name + " - found " + instance.getWorkflowProcess().getName());
        }
    }

    public void assertPackageNameEquals(ProcessInstance process, String packageName) {
        WorkflowProcessInstanceImpl instance = (WorkflowProcessInstanceImpl) process;
        if (!instance.getWorkflowProcess().getPackageName().equals(packageName)) {
            fail("Expected package name: " + packageName + " - found " + instance.getWorkflowProcess().getPackageName());
        }
    }

}
