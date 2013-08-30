package com.plugtree.training;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class HumanTaskServiceTest {

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
		userGroupCallback.addUser("mary", "users");
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
    	//kbuilder.add(new ClassPathResource("humanTaskProcess.bpmn2"), ResourceType.BPMN2);
		
		//create the work item handler for human task
		WorkItemHandler humanTaskHandler = new NonManagedLocalHTWorkItemHandler(ksession, taskService);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "first value");
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("com.plugtree.training.humanTaskProcess", initData);
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		List<TaskSummary> tasks = taskService.getTasksOwned("john", "en-UK");
		assertNotNull(tasks);
		assertEquals(1, tasks.size());
		
		TaskSummary firstTask = tasks.iterator().next();
		assertEquals(Status.Reserved, firstTask.getStatus());
		taskService.start(firstTask.getId(), "john");
		
		Map<String, Object> results1 = new HashMap<String, Object>();
		results1.put("data2", "second value");
		taskService.complete(firstTask.getId(), "john", results1);
		//up to here, all direct interaction is handled through task service
		// the handler is in charge of getting to the next task
		
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		assertNotNull(instance.getVariable("datatwo"));
		assertEquals(instance.getVariable("datatwo"), "second value");
		
		List<String> groupIds = new ArrayList<String>();
		groupIds.add("users");
		List<TaskSummary> groupTasks = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
		assertNotNull(groupTasks);
		assertEquals(1, groupTasks.size());
		
		TaskSummary secondTask = groupTasks.iterator().next();
		assertEquals(Status.Ready, secondTask.getStatus());
		taskService.claim(secondTask.getId(), "mary");
		taskService.start(secondTask.getId(), "mary");

		Map<String, Object> results2 = new HashMap<String, Object>();
		results2.put("data3", "third value");
		taskService.complete(secondTask.getId(), "mary", results2);
		
		//now that the second task is completed, the process is completed as well
		assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
		assertNotNull(instance.getVariable("datathree"));
		assertEquals(instance.getVariable("datathree"), "third value");
		
	}
}
