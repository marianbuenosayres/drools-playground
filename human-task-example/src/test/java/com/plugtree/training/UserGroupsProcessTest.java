/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.plugtree.training;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.core.io.impl.ClassPathResource;
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
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.logger.KnowledgeRuntimeLogger;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class UserGroupsProcessTest {

	private PoolingDataSource ds;
    private KnowledgeRuntimeLogger fileLogger;
    private KieSession ksession;
    private TaskService taskService;
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
        
        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);
        
        //File logger: try to open its output using Audit View in eclipse
        File logFile = File.createTempFile("process-output", "");
        System.out.println("Log file= "+logFile.getAbsolutePath()+".log");
        fileLogger = KnowledgeRuntimeLoggerFactory.newFileLogger((KnowledgeRuntimeEventManager) ksession, logFile.getAbsolutePath());
        
    }

    @After
    public void cleanup(){
        if (this.fileLogger != null){
            this.fileLogger.close();
        }
        if (this.ds != null) {
        	this.ds.close();
        }
    } 
    
    @Test
    public void taskGroupTest() {
    	//krisv should belong to GroupA in this configuration
        this.taskService = this.createTaskService("krisv", "GroupA");
        
        //Configure WIHandler for Human Tasks
        humanTaskHandler = new NonManagedLocalHTWorkItemHandler(ksession, taskService);
        
        this.ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);

        //Start the process using its id
        ProcessInstance process = ksession.startProcess("userGroupSampleProcess");
        
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //krisv doesn't have a task for itself. (see the definition of the porcess)
        List<TaskSummary> results = taskService.getTasksOwned("krisv", "en-UK");
        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
        
        //But if krisv is in GroupA, then he has 1 task (because the group has
        //1 task). Here we are making the relation between user kris and group 
        //GroupA
        results = taskService.getTasksAssignedAsPotentialOwner("krisv", "en-UK");
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        TaskSummary krisvsTask = results.get(0);
        
        //krisv claims the task (always indicating the group)
        taskService.claim(krisvsTask.getId(), "krisv");
        
        //krisv completes the task. There is no need to specify the groups
        //anymore because the task is already claimed.
        taskService.start(krisvsTask.getId(), "krisv");
        taskService.complete(krisvsTask.getId(), "krisv", null);
        
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
    	kfs.write(new ClassPathResource("test/userGroupsSampleProcess.bpmn2"));
    	KieBuilder kbuilder = ks.newKieBuilder(kfs);
    	kbuilder.buildAll();
    	//get the classpath based container
    	KieContainer kc = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
    	return kc.newKieSession();
    }
    
    public TaskService createTaskService(String userId, String... roles) {
		// Create the task service 
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.services.task");
		DefaultUserGroupCallbackImpl userGroupCallback = new DefaultUserGroupCallbackImpl();
		userGroupCallback.addUser("Administrator", roles); //Administrator user must exist
		userGroupCallback.addUser(userId, roles);
		return HumanTaskServiceFactory.newTaskServiceConfigurator()
			.entityManagerFactory(emf)
			//.transactionManager(new JbpmJTATransactionManager())
			.userGroupCallback(userGroupCallback)
			.getTaskService();
    }
}
