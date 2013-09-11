package com.plugtree.training;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListener;
import org.drools.SystemEventListenerFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.easymock.EasyMock;
import org.jbpm.process.workitem.wsht.AsyncGenericHTWorkItemHandler;
import org.jbpm.task.AsyncTaskService;
import org.jbpm.task.Status;
import org.jbpm.task.identity.DefaultUserGroupCallbackImpl;
import org.jbpm.task.identity.UserGroupCallbackManager;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.jms.JMSTaskClientConnector;
import org.jbpm.task.service.jms.JMSTaskClientHandler;
import org.jbpm.task.service.jms.JMSTaskServer;
import org.jbpm.task.service.responsehandlers.BlockingTaskOperationResponseHandler;
import org.jbpm.task.service.responsehandlers.BlockingTaskSummaryResponseHandler;
import org.jbpm.task.utils.ContentMarshallerHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class JMSTaskServiceTest {

	private PoolingDataSource ds;
	
	@Before
	public void setUp() throws Exception {
		this.ds = new PoolingDataSource();
		this.ds.setUniqueName("jdbc/testDS");
		this.ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		this.ds.setMaxPoolSize(3);
		this.ds.setAllowLocalTransactions(true);
		this.ds.getDriverProperties().setProperty("URL", "jdbc:h2:tasks;MVCC=true;DB_CLOSE_ON_EXIT=FALSE");
		this.ds.getDriverProperties().setProperty("user", "sa");
		this.ds.getDriverProperties().setProperty("password", "sasa");
		this.ds.init();
	}
	
	@After
	public void tearDown() throws Exception {
		if (this.ds != null) {
			this.ds.close();
		}
	}
	
	@Test
	public void testJMSTaskService() throws Exception {

		//start DefaultUserGroupCallbackImpl
		Properties userGroups = new Properties();
		userGroups.setProperty("john", "users");
		userGroups.setProperty("mary", "users");
		userGroups.setProperty("Administrator", "users");
		SystemEventListener systemEventListener = SystemEventListenerFactory.getSystemEventListener();
		UserGroupCallbackManager.getInstance().setCallback(new DefaultUserGroupCallbackImpl(userGroups));
		//start taskService
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.task");
		TaskService internalTaskService = new TaskService(emf, systemEventListener);

		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		Context context = EasyMock.createMock(Context.class);
        EasyMock.expect(context.lookup("ConnectionFactory")).andReturn(factory).anyTimes();
        EasyMock.replay(context);

        Properties serverConnProperties = new Properties();
        serverConnProperties.setProperty("JMSTaskServer.connectionFactory", "ConnectionFactory");
        serverConnProperties.setProperty("JMSTaskServer.transacted", "true");
        serverConnProperties.setProperty("JMSTaskServer.acknowledgeMode", "AUTO_ACKNOWLEDGE");
        serverConnProperties.setProperty("JMSTaskServer.queueName", "tasksQueue");
        serverConnProperties.setProperty("JMSTaskServer.responseQueueName", "tasksResponseQueue");

		JMSTaskServer taskServer = new JMSTaskServer(internalTaskService, serverConnProperties, context);
		Thread thread = new Thread(taskServer);
		thread.start();

        Properties clientConnProperties = new Properties();
        clientConnProperties.setProperty("JMSTaskClient.connectionFactory", "ConnectionFactory");
        clientConnProperties.setProperty("JMSTaskClient.transactedQueue", "true");
        clientConnProperties.setProperty("JMSTaskClient.acknowledgeMode", "AUTO_ACKNOWLEDGE");
        clientConnProperties.setProperty("JMSTaskClient.queueName", "tasksQueue");
        clientConnProperties.setProperty("JMSTaskClient.responseQueueName", "tasksResponseQueue");

		AsyncTaskService taskClient = new TaskClient(new JMSTaskClientConnector("client-jms", 
				new JMSTaskClientHandler(SystemEventListenerFactory.getSystemEventListener()), 
				clientConnProperties, context));
		
		//create the knowledge session
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("humanTaskProcess.bpmn2"), ResourceType.BPMN2);
        System.out.println("Compiling resources");
        
        //Check for errors
        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                    System.out.println("Error building kbase: " + error.getMessage());
                }
            }
            throw new RuntimeException("Error building kbase!");
        }

        //Create a knowledge base and add the generated package
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
		
		//use human-task-process with AsyncMinaHTWorkItemHandler and MinaTaskServer / TaskClient
		AsyncGenericHTWorkItemHandler humanTaskHandler = new AsyncGenericHTWorkItemHandler(ksession);
		
		humanTaskHandler.setClient(taskClient);
		humanTaskHandler.setIpAddress("127.0.0.1"); //quickfix to generic connect issue
		humanTaskHandler.setPort(20); //quickfix to generic connect issue
		
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "first value");
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("com.plugtree.training.humanTaskProcess", initData);
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());

		final long waitTime = 7000;
		
		BlockingTaskSummaryResponseHandler responseHandler1 = new BlockingTaskSummaryResponseHandler();
		taskClient.getTasksOwned("john", "en-UK", responseHandler1);
		List<TaskSummary> tasks = responseHandler1.getResults();
		assertNotNull(tasks);
		assertEquals(1, tasks.size());
		
		TaskSummary firstTask = tasks.iterator().next();
		assertEquals(Status.Reserved, firstTask.getStatus());

		BlockingTaskOperationResponseHandler responseHandler2 = new BlockingTaskOperationResponseHandler();
		taskClient.start(firstTask.getId(), "john", responseHandler2);
		responseHandler2.waitTillDone(waitTime);
		
		Map<String, Object> results1 = new HashMap<String, Object>();
		results1.put("data2", "second value");
		ContentData outputData1 = ContentMarshallerHelper.marshal(results1, ksession.getEnvironment());
		BlockingTaskOperationResponseHandler responseHandler3 = new BlockingTaskOperationResponseHandler();
		taskClient.complete(firstTask.getId(), "john", outputData1, responseHandler3);
		responseHandler3.waitTillDone(waitTime);
		
		Thread.sleep(1000); //there's a race condition here between the thread that completes the task and the thread that updates the ksession
		
		//up to here, all direct interaction is handled through task service asynchronously
		// the handler is in charge of getting to the next task
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		assertNotNull(instance.getVariable("datatwo"));
		assertEquals(instance.getVariable("datatwo"), "second value");
		
		List<String> groupIds = new ArrayList<String>();
		groupIds.add("users");
		BlockingTaskSummaryResponseHandler responseHandler4 = new BlockingTaskSummaryResponseHandler();
		taskClient.getTasksAssignedAsPotentialOwner("mary", "en-UK", responseHandler4);
		List<TaskSummary> groupTasks = responseHandler4.getResults();
		assertNotNull(groupTasks);
		assertEquals(1, groupTasks.size());
		
		TaskSummary secondTask = groupTasks.iterator().next();
		assertEquals(Status.Ready, secondTask.getStatus());
		BlockingTaskOperationResponseHandler responseHandler5 = new BlockingTaskOperationResponseHandler();
		taskClient.claim(secondTask.getId(), "mary", responseHandler5);
		responseHandler5.waitTillDone(waitTime);
		BlockingTaskOperationResponseHandler responseHandler6 = new BlockingTaskOperationResponseHandler();
		taskClient.start(secondTask.getId(), "mary", responseHandler6);
		responseHandler6.waitTillDone(waitTime);

		Map<String, Object> results2 = new HashMap<String, Object>();
		results2.put("data3", "third value");
		ContentData outputData2 = ContentMarshallerHelper.marshal(results2, ksession.getEnvironment());
		BlockingTaskOperationResponseHandler responseHandler7 = new BlockingTaskOperationResponseHandler();
		taskClient.complete(secondTask.getId(), "mary", outputData2, responseHandler7);
		responseHandler7.waitTillDone(waitTime);

		Thread.sleep(1500); //there's a race condition here between the thread that completes the task and the thread that updates the ksession
		
		//now that the second task is completed, the process is completed as well
		assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
		assertNotNull(instance.getVariable("datathree"));
		assertEquals(instance.getVariable("datathree"), "third value");
		
		taskServer.stop();
	}
}
