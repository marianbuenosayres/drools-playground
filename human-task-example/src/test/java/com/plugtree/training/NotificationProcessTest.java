package com.plugtree.training;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.core.ClockType;
import org.drools.core.common.InternalKnowledgeRuntime;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.jbpm.process.core.timer.TimerServiceRegistry;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.deadlines.DeadlinesDecorator;
import org.jbpm.services.task.deadlines.notifications.impl.MockNotificationListener;
import org.jbpm.services.task.identity.UserGroupLifeCycleManagerDecorator;
import org.jbpm.services.task.identity.UserGroupTaskInstanceServiceDecorator;
import org.jbpm.services.task.identity.UserGroupTaskQueryServiceDecorator;
import org.jbpm.services.task.impl.TaskInstanceServiceImpl;
import org.jbpm.services.task.impl.TaskServiceEntryPointImpl;
import org.jbpm.services.task.subtask.SubTaskDecorator;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
import org.jbpm.shared.services.impl.JbpmJTATransactionManager;
import org.jbpm.shared.services.impl.events.JbpmServicesEventImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.logger.KnowledgeRuntimeLogger;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;
import org.kie.internal.task.api.TaskInstanceService;
import org.kie.internal.task.api.TaskQueryService;
import org.kie.internal.task.api.UserGroupCallback;
import org.kie.internal.task.api.model.Notification;
import org.kie.internal.task.api.model.NotificationEvent;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class NotificationProcessTest {

	private PoolingDataSource ds;
    private KnowledgeRuntimeLogger fileLogger;
    private KieSession ksession;
    private TaskService taskService;
    
    //private DefaultEscalatedDeadlineHandler defaultEscalatedDeadlineHandler;
    private NonManagedLocalHTWorkItemHandler humanTaskHandler;
	private MockNotificationListener notificationsListener;
    
    @Before
    public void setup() throws IOException{
    	this.ds = new PoolingDataSource();
		this.ds.setUniqueName("jdbc/testDS");
		this.ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		this.ds.setMaxPoolSize(3);
		this.ds.setAllowLocalTransactions(true);
		this.ds.getDriverProperties().setProperty("URL", "jdbc:h2:tasks;MVCC=true;DB_CLOSE_ON_EXIT=FALSE");
		this.ds.getDriverProperties().setProperty("user", "sa");
		this.ds.getDriverProperties().setProperty("password", "sasa");
		this.ds.init();

		this.notificationsListener = new MockNotificationListener();
		this.ksession = this.createKSession();
        this.taskService = this.createTaskService();
        
        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);
        
        //File logger: try to open its output using Audit View in eclipse
        File logFile = File.createTempFile("process-output", "");
        System.out.println("Log file= "+logFile.getAbsolutePath()+".log");
        fileLogger = KnowledgeRuntimeLoggerFactory.newFileLogger((KnowledgeRuntimeEventManager) ksession,logFile.getAbsolutePath());
        
        //Configure Sync WIHandler for Human Tasks that supports deadlines
        //(check its implementation)
        humanTaskHandler = new NonManagedLocalHTWorkItemHandler(ksession, taskService);
        this.ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
    }

	@After
    public void cleanup(){
        if (this.fileLogger != null){
            this.fileLogger.close();
        }
        if (this.ds != null) {
        	ds.close();
        }
    } 
    
    @Test
    public void taskEscalationTest() throws Exception {
        //Start the process using its id
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("NotCompletedReassign", "");
    	params.put("NotStartedReassign", "");
    	params.put("NotCompletedNotify", "");
    	params.put("NotStartedNotify", 
    			"from:mgmt|tousers:|togroups:operators|replyTo:boss|subject:Task Not Started|" +
    			"body:It's been 10 seconds and this task hasn't started yet. Please let management know why]" +
    			"@[10s]");
        ProcessInstance process = ksession.startProcess("escalationSampleProcess", params);
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //john is an operator and has one potencial task
        List<TaskSummary> results = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        TaskSummary task = results.iterator().next();
        
        Thread.sleep(1000);
        
        //john claims the task after one second
        taskService.claim(task.getId(), "john");
        
        //No notifications yet
        Assert.assertTrue(notificationsListener.getEventsRecieved().isEmpty());
        
        System.out.println("Sleeping");
 
        Thread.sleep(10000);

        System.out.println("Coming back");
        
        //A notification was sent
        Assert.assertFalse(notificationsListener.getEventsRecieved().isEmpty());
        NotificationEvent event = notificationsListener.getEventsRecieved().iterator().next();
        Assert.assertNotNull(event);
        Notification msg = (Notification) event.getNotification();
        Assert.assertEquals("Task Not Started", msg.getSubjects().get(0).getText());
        Assert.assertEquals("operators", msg.getRecipients().get(0).getId());
        
        //john completes the task
        taskService.start(task.getId(), "john");
        taskService.complete(task.getId(), "john", null);
        
        //The process should be completed
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }
	
    /**
     * Creates a ksession from a the default kbase
     */
    public KieSession createKSession(){
		//get the services instance
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	kfs.write(new ClassPathResource("test/escalationSampleProcess.bpmn2"));
    	KieBuilder kbuilder = ks.newKieBuilder(kfs);
    	kbuilder.buildAll();
    	//get the classpath based container
		KieContainer kc = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
    	Properties ksprops = new Properties();
    	ksprops.put("drools.timerService", PseudoClockScheduler.class.getName());
		KieSessionConfiguration ksconf = ks.newKieSessionConfiguration(ksprops);
		ksconf.setOption(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));
    	KieSession ksession = kc.newKieSession(ksconf);
    	ksession.getEnvironment().set("deploymentId", "test");
    	TimerServiceRegistry.getInstance().registerTimerService(
    			"test-timerServiceId", 
    			((InternalKnowledgeRuntime)ksession).getTimerService());
    	return ksession;
    }
    
    private TaskService createTaskService() {
		// Create the task service 
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.services.task");
		DefaultUserGroupCallbackImpl userGroupCallback = new DefaultUserGroupCallbackImpl();
		userGroupCallback.addUser("john", "operators");
		userGroupCallback.addUser("mary", "operators");
		userGroupCallback.addUser("boss", "mgmt");
		userGroupCallback.addUser("Administrator", "mgmt", "operators");
		HumanTaskServiceFactory.setEntityManagerFactory(emf);
		HumanTaskServiceFactory.setJbpmServicesTransactionManager(new JbpmJTATransactionManager());
	//	HumanTaskServiceFactory.getUserGroupLifeCycleDecorator().setUserGroupCallback(userGroupCallback);
		TaskService taskService = HumanTaskServiceFactory.newTaskService();
		TaskServiceEntryPointImpl taskServiceImpl = (TaskServiceEntryPointImpl) taskService;
		taskServiceImpl.registerTaskNotificationEventListener(new MockEventListener(this.notificationsListener));
		setUserGroupCallback(userGroupCallback, taskServiceImpl);
		return taskService;
	}

    @SuppressWarnings({"unchecked", "serial"})
	private void setUserGroupCallback(UserGroupCallback userGroupCallback, TaskServiceEntryPointImpl taskServiceImpl) {
		TaskInstanceService instanceService = taskServiceImpl.getTaskInstanceService();
		TaskQueryService queryService = taskServiceImpl.getTaskQueryService();
		UserGroupTaskQueryServiceDecorator queryDecorator = (UserGroupTaskQueryServiceDecorator) queryService;
		queryDecorator.setUserGroupCallback(userGroupCallback);
		DeadlinesDecorator instanceServiceImpl = (DeadlinesDecorator) instanceService;
		try {
			java.lang.reflect.Field instanceField = DeadlinesDecorator.class.getDeclaredField("instanceService");
			instanceField.setAccessible(true);
			SubTaskDecorator subTaskDecorator = (SubTaskDecorator) instanceField.get(instanceServiceImpl);
			java.lang.reflect.Field subInstanceField = SubTaskDecorator.class.getDeclaredField("instanceService");
			subInstanceField.setAccessible(true);
			UserGroupTaskInstanceServiceDecorator userGroupDecorator = (UserGroupTaskInstanceServiceDecorator) subInstanceField.get(subTaskDecorator);
			userGroupDecorator.setUserGroupCallback(userGroupCallback);
			java.lang.reflect.Field subSubInstanceField = UserGroupTaskInstanceServiceDecorator.class.getDeclaredField("delegate");
			subSubInstanceField.setAccessible(true);
			TaskInstanceServiceImpl taskInstanceImpl = (TaskInstanceServiceImpl) subSubInstanceField.get(userGroupDecorator);
			java.lang.reflect.Field lcmField = TaskInstanceServiceImpl.class.getDeclaredField("lifeCycleManager");
			lcmField.setAccessible(true);
			UserGroupLifeCycleManagerDecorator lfManager = (UserGroupLifeCycleManagerDecorator) lcmField.get(taskInstanceImpl);
			lfManager.setUserGroupCallback(userGroupCallback);
			//more to do with Observes registration events that anything else, but here for simplicity
			java.lang.reflect.Field notifField = TaskServiceEntryPointImpl.class.getDeclaredField("taskNotificationEvents");
			notifField.setAccessible(true);
			JbpmServicesEventImpl<NotificationEvent> listener = (JbpmServicesEventImpl<NotificationEvent>) notifField.get(taskServiceImpl);
			listener.select(new AnnotationLiteral<Observes>(){});
		} catch (Exception e) {
			throw new RuntimeException("Couldn't set user group callback", e);
		}
	}
}
