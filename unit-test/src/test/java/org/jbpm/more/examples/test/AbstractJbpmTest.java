package org.jbpm.more.examples.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.event.Event;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.drools.core.impl.EnvironmentFactory;
import org.h2.tools.DeleteDbFiles;
import org.jbpm.process.audit.JPAProcessInstanceDbLog;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.services.task.deadlines.DeadlinesDecorator;
import org.jbpm.services.task.identity.MvelUserGroupCallbackImpl;
import org.jbpm.services.task.identity.UserGroupLifeCycleManagerDecorator;
import org.jbpm.services.task.identity.UserGroupTaskInstanceServiceDecorator;
import org.jbpm.services.task.identity.UserGroupTaskQueryServiceDecorator;
import org.jbpm.services.task.impl.TaskAdminServiceImpl;
import org.jbpm.services.task.impl.TaskContentServiceImpl;
import org.jbpm.services.task.impl.TaskDeadlinesServiceImpl;
import org.jbpm.services.task.impl.TaskIdentityServiceImpl;
import org.jbpm.services.task.impl.TaskInstanceServiceImpl;
import org.jbpm.services.task.impl.TaskQueryServiceImpl;
import org.jbpm.services.task.impl.TaskServiceEntryPointImpl;
import org.jbpm.services.task.internals.lifecycle.LifeCycleManager;
import org.jbpm.services.task.internals.lifecycle.MVELLifeCycleManager;
import org.jbpm.services.task.subtask.SubTaskDecorator;
import org.jbpm.shared.services.api.JbpmServicesPersistenceManager;
import org.jbpm.shared.services.impl.JbpmLocalTransactionManager;
import org.jbpm.shared.services.impl.JbpmServicesPersistenceManagerImpl;
import org.jbpm.shared.services.impl.events.JbpmServicesEventImpl;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.kie.api.KieBase;
import org.kie.api.definition.process.Node;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.NodeInstanceContainer;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderError;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.task.api.TaskAdminService;
import org.kie.internal.task.api.TaskContentService;
import org.kie.internal.task.api.TaskDeadlinesService;
import org.kie.internal.task.api.TaskIdentityService;
import org.kie.internal.task.api.TaskInstanceService;
import org.kie.internal.task.api.TaskQueryService;
import org.kie.internal.task.api.TaskService;
import org.kie.internal.task.api.UserGroupCallback;
import org.kie.internal.task.api.model.NotificationEvent;
import org.kie.internal.task.api.model.Task;

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
        this.logger = Logger.getLogger(this.getClass().getCanonicalName());
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
            //logger = LoggerFactory.getLogger(getClass());
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

    protected KieBase createKnowledgeBase(String... bpmn2Filename) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        for (String f : bpmn2Filename) {
            kbuilder.add(ResourceFactory.newClassPathResource(f), ResourceType.BPMN2);
        }

        // Check for errors
        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                boolean errors = false;
                for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                    logger.warning(error.toString());
                    errors = true;
                }
                assertFalse("Could not build knowldge base.", errors);
            }
        }
        return kbuilder.newKnowledgeBase();
    }

    protected KieSession createKnowledgeSession(KieBase kbase) {
        StatefulKnowledgeSession ksession;
        final KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
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

    protected KieSession createKnowledgeSession(String... bpmn2Filename) {
        KieBase kbase = createKnowledgeBase(bpmn2Filename);
        return createKnowledgeSession(kbase);
    }

    public KieSession reloadSession(StatefulKnowledgeSession ksession) throws SystemException {
        int id = ksession.getId();

        KieBase kbase = ksession.getKieBase();

        // Close/clean-up
        ksession.dispose();
        emf.close();

        ksession.dispose();

        // Reload
        return loadSession(id, kbase);
    }

    public KieSession loadSession(int id, String... process) {
        KieBase kbase = createKnowledgeBase(process);
        return loadSession(id, kbase);
    }

    protected StatefulKnowledgeSession loadSession(int id, KieBase kbase) {
        Environment env = EnvironmentFactory.newEnvironment();
        emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);

        KieSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        StatefulKnowledgeSession ksession = JPAKnowledgeService.loadStatefulKnowledgeSession(id, kbase, config, env);

        // logging
        new JPAWorkingMemoryDbLogger(ksession);
        JPAProcessInstanceDbLog.setEnvironment(env);

        return ksession;
    }

//    public TaskServiceEntryPoint getAndRegisterTaskService(StatefulKnowledgeSession ksession) {
//        // Create task service (using hornetq? substitute the correct code here.. )
//        .service.TaskService taskService = new org.jbpm.task.service.TaskService(emf,
//                SystemEventListenerFactory.getSystemEventListener());
//        LocalTaskService localTaskService = new LocalTaskService(taskService);
//        
//        // work item handler
//        SyncWSHumanTaskHandler humanTaskHandler = new SyncWSHumanTaskHandler(localTaskService, ksession);
//        humanTaskHandler.setLocal(true);
//        humanTaskHandler.connect();
//       
//        // Register the handler
//        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
//        
//        return localTaskService;
//    }

    protected void createTaskServiceEntryPoint(EntityManagerFactory emf) { 
            EntityManager em = emf.createEntityManager();
            
            logger = java.util.logging.LogManager.getLogManager().getLogger("");

            
            JbpmServicesPersistenceManager pm = new JbpmServicesPersistenceManagerImpl();
            ((JbpmServicesPersistenceManagerImpl)pm).setEm(em);
            ((JbpmServicesPersistenceManagerImpl)pm).setTransactionManager(new JbpmLocalTransactionManager());
            TaskService taskService = new TaskServiceEntryPointImpl();
            
            Event<Task> taskEvents = new JbpmServicesEventImpl<Task>();
            
            Event<NotificationEvent> notificationEvents = new JbpmServicesEventImpl<NotificationEvent>();
            
            UserGroupCallback userGroupCallback = new MvelUserGroupCallbackImpl();
            
            TaskQueryService queryService = new TaskQueryServiceImpl();
            ((TaskQueryServiceImpl)queryService).setPm(pm);
            
            
            UserGroupTaskQueryServiceDecorator userGroupTaskQueryServiceDecorator = new UserGroupTaskQueryServiceDecorator();
            userGroupTaskQueryServiceDecorator.setPm(pm);
            userGroupTaskQueryServiceDecorator.setUserGroupCallback(userGroupCallback);
            userGroupTaskQueryServiceDecorator.setDelegate(queryService);
            
            ((TaskServiceEntryPointImpl)taskService).setTaskQueryService(userGroupTaskQueryServiceDecorator);
            
            TaskIdentityService identityService = new TaskIdentityServiceImpl();
            ((TaskIdentityServiceImpl)identityService).setPm(pm);
            ((TaskServiceEntryPointImpl)taskService).setTaskIdentityService(identityService);
            
            TaskAdminService adminService = new TaskAdminServiceImpl();
            ((TaskAdminServiceImpl)adminService).setPm(pm);
            ((TaskServiceEntryPointImpl)taskService).setTaskAdminService(adminService);
            
            TaskInstanceService instanceService = new TaskInstanceServiceImpl();
            ((TaskInstanceServiceImpl)instanceService).setPm(pm);
            ((TaskInstanceServiceImpl)instanceService).setTaskQueryService(userGroupTaskQueryServiceDecorator);
            ((TaskInstanceServiceImpl)instanceService).setTaskEvents(taskEvents);
            
            UserGroupTaskInstanceServiceDecorator userGroupTaskInstanceDecorator = new UserGroupTaskInstanceServiceDecorator();
            userGroupTaskInstanceDecorator.setPm(pm);
            userGroupTaskInstanceDecorator.setUserGroupCallback(userGroupCallback);
            userGroupTaskInstanceDecorator.setDelegate(instanceService);
            
            TaskContentService contentService = new TaskContentServiceImpl();
            ((TaskContentServiceImpl)contentService).setPm(pm);
            ((TaskServiceEntryPointImpl)taskService).setTaskContentService(contentService);
            
            LifeCycleManager mvelLifeCycleManager = new MVELLifeCycleManager();
            ((MVELLifeCycleManager)mvelLifeCycleManager).setPm(pm);
            ((MVELLifeCycleManager)mvelLifeCycleManager).setTaskIdentityService(identityService);
            ((MVELLifeCycleManager)mvelLifeCycleManager).setTaskQueryService(userGroupTaskQueryServiceDecorator);
            ((MVELLifeCycleManager)mvelLifeCycleManager).setTaskContentService(contentService);
            ((MVELLifeCycleManager)mvelLifeCycleManager).setTaskEvents(taskEvents);
            ((MVELLifeCycleManager)mvelLifeCycleManager).setLogger(logger);
            ((MVELLifeCycleManager)mvelLifeCycleManager).initMVELOperations();
            
            
            UserGroupLifeCycleManagerDecorator userGroupLifeCycleDecorator = new UserGroupLifeCycleManagerDecorator();
            userGroupLifeCycleDecorator.setPm(pm);
            userGroupLifeCycleDecorator.setUserGroupCallback(userGroupCallback);
            userGroupLifeCycleDecorator.setManager(mvelLifeCycleManager);
            ((TaskInstanceServiceImpl)instanceService).setLifeCycleManager(userGroupLifeCycleDecorator);
            
            
            TaskDeadlinesService deadlinesService = new TaskDeadlinesServiceImpl();
            ((TaskDeadlinesServiceImpl)deadlinesService).setPm(pm);
            ((TaskDeadlinesServiceImpl)deadlinesService).setLogger(logger);
            ((TaskDeadlinesServiceImpl)deadlinesService).setNotificationEvents(notificationEvents);
            ((TaskDeadlinesServiceImpl)deadlinesService).init();
            
            SubTaskDecorator subTaskDecorator = new SubTaskDecorator();
            subTaskDecorator.setInstanceService(userGroupTaskInstanceDecorator);
            subTaskDecorator.setPm(pm);
            subTaskDecorator.setQueryService(userGroupTaskQueryServiceDecorator);
            
            DeadlinesDecorator deadlinesDecorator = new DeadlinesDecorator();
            deadlinesDecorator.setPm(pm);
            deadlinesDecorator.setQueryService(userGroupTaskQueryServiceDecorator);
            deadlinesDecorator.setDeadlineService(deadlinesService);
            deadlinesDecorator.setQueryService(userGroupTaskQueryServiceDecorator);
            deadlinesDecorator.setInstanceService(subTaskDecorator);
            
            ((TaskServiceEntryPointImpl)taskService).setTaskInstanceService(deadlinesDecorator);

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
