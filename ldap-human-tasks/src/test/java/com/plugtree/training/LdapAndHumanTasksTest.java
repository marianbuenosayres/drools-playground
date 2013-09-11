package com.plugtree.training;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.drools.core.io.impl.ClassPathResource;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.identity.LDAPUserGroupCallbackImpl;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
import org.jbpm.shared.services.impl.JbpmJTATransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message.Level;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.server.ApacheDSContainer;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class LdapAndHumanTasksTest {

	private PoolingDataSource ds;
    private ApacheDSContainer container;
    private TaskService service;

    @Before
    public void setUp() throws Exception {
        container = new ApacheDSContainer("o=mojo", "classpath:identity-repository.ldif");
        container.setPort(9898);
        container.afterPropertiesSet();
        
        LdapContextSource cs = new LdapContextSource();
        cs.setUrl("ldap://localhost:9898/");
        cs.setBase("o=mojo");
        cs.setUserDn("uid=admin,ou=system");
        cs.setPassword("secret");
        cs.afterPropertiesSet();
        LdapTemplate ldapTemplate = new LdapTemplate(cs);
        ldapTemplate.afterPropertiesSet();

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

    @Test
    public void testProcessWithHumanTasks() throws InterruptedException {

    	//make sure users exist
    	LDAPUserGroupCallbackImpl userGroupCallback = new LDAPUserGroupCallbackImpl();
    	Assert.assertTrue(userGroupCallback.existsUser("john"));
    	Assert.assertTrue(userGroupCallback.existsUser("louis"));
    	Assert.assertTrue(userGroupCallback.existsUser("mary"));
    	Assert.assertTrue(userGroupCallback.existsUser("george"));
    	
        KieSession ksession = this.initializeSession();
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);
        NonManagedLocalHTWorkItemHandler htHandler = this.createTaskHandler(ksession);
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", htHandler);

        Map<String, Object> initialParams = new HashMap<String, Object>();
        initialParams.put("actorId", "john");
        ProcessInstance processInstance = ksession.startProcess("sampleReviewProcess", initialParams);
        assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
        // first, john will make its own review..
        List<TaskSummary> tasks = this.service.getTasksOwned("john", "en-UK");
        assertEquals(1, tasks.size());

        this.service.start(tasks.get(0).getId(), "john");
        this.service.complete(tasks.get(0).getId(), "john", null);

        // now, a user with role HR will see the task.. louis is one of them
        List<TaskSummary> louisTasks = this.service.getTasksAssignedAsPotentialOwner("louis", "en-UK");
        Assert.assertEquals(1, louisTasks.size());
        this.service.claim(louisTasks.get(0).getId(), "louis");
        this.service.start(louisTasks.get(0).getId(), "louis");
        this.service.complete(louisTasks.get(0).getId(), "louis", null);

        // now, a user with role PM will see the task.. mary is one of them
        List<TaskSummary> maryTasks = this.service.getTasksAssignedAsPotentialOwner("mary", "en-UK");
        Assert.assertEquals(1, maryTasks.size());
        this.service.claim(maryTasks.get(0).getId(), "mary");
        this.service.start(maryTasks.get(0).getId(), "mary");
        this.service.complete(maryTasks.get(0).getId(), "mary", null);

        assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }

    @After
    public void tearDown() {
        container.stop();
        if (ds != null) {
        	ds.close();
        }
    }

    //init the Knowledge Session
    private KieSession initializeSession() {
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	KieBuilder kbuilder = ks.newKieBuilder(kfs);
    	kfs.write(new ClassPathResource("process/sampleReviewProcess.bpmn2"));
    	kbuilder.buildAll();
        if (kbuilder.getResults().hasMessages(Level.ERROR)) {
        	String message = ">>> Knowledge couldn't be parsed! " + kbuilder.getResults();
        	throw new IllegalStateException(message);
        }
        KieContainer kc = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
        return kc.newKieSession();
    }

    //Creates a local task service and attaches it to a human task handler
    private NonManagedLocalHTWorkItemHandler createTaskHandler(KieSession ksession) {
		// Create the task service 
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.services.task");
		// Create LDAP user group callback
		LDAPUserGroupCallbackImpl userGroupCallback = new LDAPUserGroupCallbackImpl();
		//start taskService
		TaskService taskService = HumanTaskServiceFactory.newTaskServiceConfigurator()
			.transactionManager(new JbpmJTATransactionManager())
			.entityManagerFactory(emf)
			.userGroupCallback(userGroupCallback)
			.getTaskService();

        NonManagedLocalHTWorkItemHandler taskHandler = new NonManagedLocalHTWorkItemHandler(ksession, taskService);
        this.service = taskService;
        return taskHandler;
    }
}
