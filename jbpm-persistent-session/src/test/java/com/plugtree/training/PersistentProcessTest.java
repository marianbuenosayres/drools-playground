package com.plugtree.training;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.drools.core.base.MapGlobalResolver;
import org.drools.core.io.impl.ClassPathResource;
import org.jbpm.process.audit.AuditLogService;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.junit.After;
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
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class PersistentProcessTest {

	private KieBase kbase;
	private KieSession ksession;
	private KieSessionConfiguration ksessionConf;
	private Environment environment;
	
	private TestAsyncWorkItemHandler task11Handler;
	private TestAsyncWorkItemHandler task12Handler;
	private TestAsyncWorkItemHandler task13Handler;
	private TestAsyncWorkItemHandler task21Handler;
	private TestAsyncWorkItemHandler task22Handler;
	
	private PoolingDataSource ds = new PoolingDataSource();

    @Before
    public void setUp() {
        //System.setProperty("java.naming.factory.initial", "bitronix.tm.jndi.BitronixInitialContextFactory");

        ds.setUniqueName("jdbc/testDS");


        //NON XA CONFIGS
        ds.setClassName("org.h2.jdbcx.JdbcDataSource");
        ds.setMaxPoolSize(3);
        ds.setAllowLocalTransactions(true);
        ds.getDriverProperties().put("user", "sa");
        ds.getDriverProperties().put("password", "sasa");
        ds.getDriverProperties().put("URL", "jdbc:h2:workflow;MVCC=TRUE;DB_CLOSE_ON_EXIT=FALSE");

        ds.init();

        this.createJPASession();
    }

	public void createJPASession() {
		
		KieServices ks = KieServices.Factory.get();
		KieFileSystem kfs = ks.newKieFileSystem();	
		kfs.write(new ClassPathResource("simpleProcess.bpmn2"));
		KieBuilder kbuilder = ks.newKieBuilder(kfs);
		kbuilder.buildAll();
		if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
			throw new IllegalArgumentException("Problem parsing process" + kbuilder.getResults());
		}
		KieContainer kc = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
    	//Register WorkItemManagers for all the generic tasks in the process
    	this.task11Handler = new TestAsyncWorkItemHandler();
    	this.task12Handler = new TestAsyncWorkItemHandler();
    	this.task13Handler = new TestAsyncWorkItemHandler();
    	this.task21Handler = new TestAsyncWorkItemHandler();
    	this.task22Handler = new TestAsyncWorkItemHandler();

        //Create a kie base
        this.kbase = kc.getKieBase();

		//creation of persistence context
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		this.environment = KnowledgeBaseFactory.newEnvironment();
		this.environment.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		this.environment.set( EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager() );
		this.environment.set( EnvironmentName.GLOBALS, new MapGlobalResolver() );
		
		//creation of session config
		Properties sessionProperties = new Properties();
		sessionProperties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		sessionProperties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		this.ksessionConf = ks.newKieSessionConfiguration(sessionProperties);
		
		ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, this.ksessionConf, this.environment);

    	ksession.getWorkItemManager().registerWorkItemHandler("task1.1", task11Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task1.2", task12Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task1.3", task13Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task2.1", task21Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task2.2", task22Handler);
        new JPAWorkingMemoryDbLogger(ksession);
	}
	
	public void reloadSession(int sessionId) {
		ksession = JPAKnowledgeService.loadStatefulKnowledgeSession(sessionId, this.kbase, this.ksessionConf, this.environment);
		
		//We have to register the work item handlers again
    	ksession.getWorkItemManager().registerWorkItemHandler("task1.1", task11Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task1.2", task12Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task1.3", task13Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task2.1", task21Handler);
        ksession.getWorkItemManager().registerWorkItemHandler("task2.2", task22Handler);
        new JPAWorkingMemoryDbLogger(ksession);
	}
	
	@Test
	public void testPersistentProcess() {
		int sessionId = ksession.getId();
		
    	//Start the process using its id
        ProcessInstance processInstance1 = ksession.startProcess("com.plugtree.training.simpleprocess");
        
        long processInstanceId = processInstance1.getId();
        
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, processInstance1.getState());
        ksession.getWorkItemManager().completeWorkItem(task11Handler.getWorkItemId(), null);
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, processInstance1.getState());
        ksession.getWorkItemManager().completeWorkItem(task13Handler.getWorkItemId(), null);
        ksession.getWorkItemManager().completeWorkItem(task12Handler.getWorkItemId(), null);

		//Load the session from the database again into ksession
		reloadSession(sessionId);
		
		//reload Process Instance (old bean is stale after session disposal)
		ProcessInstance processInstance2 = this.ksession.getProcessInstance(processInstanceId);

        //up to here, we are at the script task and afterwards we have to 
        //complete either task2.1 or task2.2
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, processInstance2.getState());
        ksession.getWorkItemManager().completeWorkItem(task22Handler.getWorkItemId(), null);
        //after completing just one, we finish the process, and not only appears in the audit logs
        
        AuditLogService audit = new JPAAuditLogService(environment);
        List<ProcessInstanceLog> logs = audit.findProcessInstances();
        Assert.assertEquals(1, logs.size());
        ProcessInstanceLog log = logs.iterator().next();
        Assert.assertEquals(processInstance2.getId(), log.getProcessInstanceId());
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, log.getStatus().intValue());
	}
	
	@After
	public void tearDown() {
		this.kbase = null;
		this.ksessionConf = null;
		this.environment = null;
		this.ksession = null;
		//close datasource
        ds.close();
	}

    public static class TestAsyncWorkItemHandler implements WorkItemHandler {
    	
    	private long workItemId;
    	private WorkItemManager manager;
    	
    	@Override
    	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    		//do nothing
    	}
    	
    	@Override
    	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    		// Register work item id to know when to continue
    		this.workItemId = workItem.getId();
    		// Register work item manager to continue operation internally
    		this.manager = manager;
    		System.out.println("Entering task " + workItem.getName());
    		System.out.println("Parameters:");
    		for (Map.Entry<String, Object> entry : workItem.getParameters().entrySet()) {
    			System.out.println(">>>" + entry.getKey() + " = " + String.valueOf(entry.getValue()));
    		}
    	}
    	
    	public void complete() {
    		this.manager.completeWorkItem(workItemId, null);
    	}
    	
    	public long getWorkItemId() {
			return workItemId;
		}
    }
}

