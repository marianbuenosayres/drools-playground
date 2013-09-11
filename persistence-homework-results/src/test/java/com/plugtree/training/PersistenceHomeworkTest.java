package com.plugtree.training;

import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.core.base.MapGlobalResolver;
import org.drools.core.io.impl.ClassPathResource;
import org.jbpm.process.audit.AuditLogService;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class PersistenceHomeworkTest {

	private PoolingDataSource ds;
	
	@Before
	public void setUp() throws Exception {
		this.ds = new PoolingDataSource();
		this.ds.setUniqueName("jdbc/testDS");
		this.ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		this.ds.setMaxPoolSize(3);
		this.ds.setAllowLocalTransactions(true);
		this.ds.getDriverProperties().setProperty("URL", "jdbc:h2:workflow;MVCC=true;DB_CLOSE_ON_EXIT=FALSE");
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
	public void testPersistentProcess() throws Exception {
		
		// Create the knowledge moudule 
		KieServices ks = KieServices.Factory.get();
		KieFileSystem kfs = ks.newKieFileSystem();	
		kfs.write(new ClassPathResource("test/htHomeworkProcess.bpmn2"));
		KieBuilder kbuilder = ks.newKieBuilder(kfs);
		kbuilder.buildAll();
		if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
			throw new IllegalArgumentException("Problem parsing process" + kbuilder.getResults());
		}
		KieContainer kc = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
    	//Register WorkItemManagers for all the generic tasks in the process
    	TestAsyncWorkItemHandler handler = new TestAsyncWorkItemHandler();

        //Create a kie base
        KieBase kbase = kc.getKieBase();

		//creation of persistence context
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		Environment env = KnowledgeBaseFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		env.set( EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager() );
		env.set( EnvironmentName.GLOBALS, new MapGlobalResolver() );

		//creation of session config
		Properties sessionProperties = new Properties();
		sessionProperties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		sessionProperties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KieSessionConfiguration ksessionConf = ks.newKieSessionConfiguration(sessionProperties);
		
		// Create the KIE session
    	KieSession ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, ksessionConf, env);
        new JPAWorkingMemoryDbLogger(ksession);
		
		//create the work item handler for human task
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", handler);
		
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("htHomeworkProcess", null);
		Assert.assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		Assert.assertNotNull(instance.getVariable("requestId"));
		String requestId = instance.getVariable("requestId").toString();
		
		WorkItem item = handler.getItem();
		Assert.assertNotNull(item);
		Assert.assertNotNull(item.getParameter("TaskName"));
		String taskName = item.getParameter("TaskName").toString();
		Assert.assertTrue(taskName.contains(requestId));

		int ksessionId = ksession.getId();
		
		KieSession ksession2 = JPAKnowledgeService.loadStatefulKnowledgeSession(ksessionId, kbase, ksessionConf, env);
		ksession2.getWorkItemManager().registerWorkItemHandler("Human Task", handler);
        new JPAWorkingMemoryDbLogger(ksession2);

		//complete the second task
		ksession2.getWorkItemManager().completeWorkItem(item.getId(), null);
		
        AuditLogService audit = new JPAAuditLogService(env);
        List<ProcessInstanceLog> logs = audit.findProcessInstances();
        Assert.assertEquals(1, logs.size());
        ProcessInstanceLog log = logs.iterator().next();
        Assert.assertEquals(instance.getId(), log.getProcessInstanceId());
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, log.getStatus().intValue());
	}
	
	private static class TestAsyncWorkItemHandler implements WorkItemHandler {
		private WorkItem item = null;
		@Override
		public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
			this.item = null;
		}
		@Override
		public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
			this.item = workItem;
		}
		public WorkItem getItem() {
			WorkItem result = this.item;
			this.item = null;
			return result;
		}
	}
}
