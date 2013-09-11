package com.plugtree.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
import org.jbpm.shared.services.impl.JbpmJTATransactionManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.task.api.model.InternalTask;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class HtHomeworkTest {

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
	public void testLocalTaskService() throws Exception {
		
		// Create the task service 
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.services.task");
		DefaultUserGroupCallbackImpl userGroupCallback = new DefaultUserGroupCallbackImpl();
		//create mock groups
		userGroupCallback.addUser("john", "users");
		userGroupCallback.addUser("Administrator", "users");
		//start taskService
		TaskService taskService =HumanTaskServiceFactory.newTaskServiceConfigurator()
			.transactionManager(new JbpmJTATransactionManager())
			.entityManagerFactory(emf)
			.userGroupCallback(userGroupCallback)
			.getTaskService();
		
		// Create the KIE session
		KieServices ks = KieServices.Factory.get();
 	   	KieContainer kc = ks.getKieClasspathContainer();
    		KieSession ksession = kc.newKieSession();
		
		//create the work item handler for human task
		WorkItemHandler humanTaskHandler = new NonManagedLocalHTWorkItemHandler(ksession, taskService);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
		
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("htHomeworkProcess", null);
		Assert.assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
		Assert.assertNotNull(tasks);
		Assert.assertEquals(1, tasks.size());
		
		TaskSummary firstTask = tasks.iterator().next();
		
		Assert.assertNotNull(instance.getVariable("requestId"));
		String requestId = instance.getVariable("requestId").toString();

		InternalTask actualFirstTask = (InternalTask) taskService.getTaskById(firstTask.getId());
		System.out.println("requestId = " + requestId);
		System.out.println("formName = " + actualFirstTask.getFormName());
		Assert.assertNotNull(actualFirstTask.getFormName());
		Assert.assertTrue(actualFirstTask.getFormName().contains(requestId));

		taskService.claim(firstTask.getId(), "john");
		taskService.start(firstTask.getId(), "john");
		taskService.complete(firstTask.getId(), "john", null);
		
		//now that the second task is completed, the process is completed as well
		Assert.assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
	}
}
