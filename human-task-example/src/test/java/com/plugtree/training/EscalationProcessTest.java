package com.plugtree.training;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.core.ClockType;
import org.drools.core.common.InternalKnowledgeRuntime;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.jbpm.process.core.timer.TimerServiceRegistry;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
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
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.logger.KnowledgeRuntimeLogger;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class EscalationProcessTest {

	private PoolingDataSource ds;
    private KnowledgeRuntimeLogger fileLogger;
    private KieSession ksession;
    private TaskService taskService;
    
    //private DefaultEscalatedDeadlineHandler defaultEscalatedDeadlineHandler;
    private NonManagedLocalHTWorkItemHandler humanTaskHandler;
    
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
    	params.put("NotStartedReassign", "[users:|groups:operators]@[10s]");
    	params.put("NotCompletedNotify", "");
    	params.put("NotStartedNotify", "");
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
        
        //No Reassignment yet (mary is an operator, but the task is claimed by john for now)
        List<Status> status = new ArrayList<Status>();
        status.add(Status.Ready);
        results = taskService.getTasksAssignedAsPotentialOwnerByStatus("mary", status, "en-UK");
        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
       
        System.out.println("Sleeping");
 
        Thread.sleep(10000);

        System.out.println("Coming back");
        
        //The task was Reassigned to operators
        results = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        
        Task maryTask = taskService.getTaskById(results.get(0).getId());
        
        //mary claims the task
        taskService.claim(maryTask.getId(), "mary");
        
        //mary completes the task
        taskService.start(maryTask.getId(), "mary");
        taskService.complete(maryTask.getId(), "mary", null);
        
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
		TaskService taskService = HumanTaskServiceFactory.newTaskServiceConfigurator()
			.entityManagerFactory(emf)
			.userGroupCallback(userGroupCallback)
			.getTaskService();
		return taskService;
	}
}
