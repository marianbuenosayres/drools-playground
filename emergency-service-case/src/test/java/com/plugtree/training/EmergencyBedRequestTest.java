package com.plugtree.training;


import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.executor.impl.ClassCacheManager;
import org.jbpm.executor.impl.ExecutorImpl;
import org.jbpm.executor.impl.ExecutorQueryServiceImpl;
import org.jbpm.executor.impl.ExecutorRequestAdminServiceImpl;
import org.jbpm.executor.impl.ExecutorRunnable;
import org.jbpm.executor.impl.ExecutorServiceImpl;
import org.jbpm.executor.impl.runtime.RuntimeManagerRegistry;
import org.jbpm.executor.impl.wih.AsyncWorkItemHandler;
import org.jbpm.process.instance.event.listeners.RuleAwareProcessEventLister;
import org.jbpm.shared.services.impl.JbpmJTATransactionManager;
import org.jbpm.shared.services.impl.JbpmServicesPersistenceManagerImpl;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.task.TaskService;
import org.kie.internal.executor.api.ExecutorService;

import bitronix.tm.TransactionManagerServices;

/**
 * Base test class used by the rest of the tests in this project.
 * @author esteban.aliverti
 */
public class EmergencyBedRequestTest {

    private KieSession session;

    @Before
    public void setUp() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieContainer kc = ks.getKieClasspathContainer();
        this.session = kc.newKieSession("ksession1");
    }

    @After
    public void tearDown() {
    	this.session.dispose();
    	TransactionManagerServices.getTransactionManager().shutdown();
    }

    private void initSession(WorkItemHandler htHandler, WorkItemHandler nsHandler) {
        session.addEventListener(new DefaultAgendaEventListener(){
            @Override
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
                session.fireAllRules();
            }
        });
        session.addEventListener(new RuleAwareProcessEventLister());
        
        //register the same handler for all the Work Items present in the process.
        this.session.getWorkItemManager().registerWorkItemHandler("Human Task", htHandler);
        this.session.getWorkItemManager().registerWorkItemHandler("NotificationSystem", nsHandler);
    }
    
    
    @Test
    public void doTest() {
    	TestAsyncWorkItemHandler handler = new TestAsyncWorkItemHandler();
    	initSession(handler, handler);
        //prepare input parameters for the process:
        String date = DateFormat.getDateInstance().format(new Date());
        String patientAge = "21";
        String patientGender = "F";
        String patientStatus = "Critical";
        
        Map<String, Object> inputVariables = new HashMap<String, Object>();
        inputVariables.put("bedRequestDate", date);
        inputVariables.put("patientAge", patientAge);
        inputVariables.put("patientGender", patientGender);
        inputVariables.put("patientStatus", patientStatus);
        
        //Start the process using its ID and pass the input variables
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) session.startProcess(
        		"com.plugtree.training.checkInPatient", inputVariables);
        
        //**************   Assign Bed rules   **************//
        //The process begins with the activation and firing of the ruleflow group
        //'assign-bed', that assigns the process variable 'asignbed' to 
        //'intensive-care-bed' if the patient status is critical
        Assert.assertEquals("intensive-care-bed", processInstance.getVariable("assignbed"));
        
        //**************   Coordinate Staff   **************//
        
        //The process must be in the 'Coordinate Staff' task. Let's check the
        //input parameters received by the handler associated to that task.
        Assert.assertEquals("Coordinate Staff", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        Assert.assertEquals(date, handler.getLastItem().getParameter("requestDate"));
        Assert.assertEquals(patientAge, handler.getLastItem().getParameter("requestPatientAge"));
        Assert.assertEquals(patientGender, handler.getLastItem().getParameter("requestPatientGender"));
        Assert.assertEquals(patientStatus, handler.getLastItem().getParameter("requestPatientStatus"));
        
        //let's complete the task emulating the results of this task.
        Map<String,Object> taskResults = new HashMap<String, Object>();
        taskResults.put("gateSelected", "3C");
        session.getWorkItemManager().completeWorkItem(handler.getLastItem().getId(), taskResults);
        
        //**************   Notify Gate to Ambulance   **************//
        //Now we are at 'Notify Gate to Ambulance' task. Let's check that the input
        //parameters configured for this tasks arrived as expected.
        Assert.assertEquals("Notify Gate to Ambulance", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        Assert.assertEquals("3C", handler.getLastItem().getParameter("gateNumber"));
        
        //let's complete the task with a mocked resource
        taskResults = new HashMap<String, Object>();
        taskResults.put("notified", "true");
        session.getWorkItemManager().completeWorkItem(handler.getLastItem().getId(), taskResults);
        
        //**************   Check In Patient   **************//
        //In 'Check In Patient' task we are expecting a 'notified'
        //parameter containing the value returned by the last task
        Assert.assertEquals("Check In Patient", processInstance.getNodeInstances().iterator().next().getNodeName());
        Assert.assertEquals("true", handler.getLastItem().getParameter("notified"));
        
        //let's complete the task passing the mocked results
        taskResults = new HashMap<String, Object>();
        String checkinDate = DateFormat.getTimeInstance().format(new Date());
        taskResults.put("checkinResult", "Check in completed successfuly");
        taskResults.put("checkinTime", checkinDate);
        session.getWorkItemManager().completeWorkItem(handler.getLastItem().getId(), taskResults);
        
        //The process should be completed now. Let's check the 2 output
        //parameters of the last task: they should be mapped to process variables.
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
        Assert.assertEquals("Check in completed successfuly", processInstance.getVariable("result"));
        Assert.assertEquals(checkinDate, processInstance.getVariable("bedResponseTime"));
        
    }
    
    @Test
    public void doTestWithExecutor() throws Exception {
    	TestAsyncWorkItemHandler htHandler = new TestAsyncWorkItemHandler();
    	ExecutorService executorService = newExecutorService();
        executorService.setInterval(1); //seconds
        executorService.init();
    	AsyncWorkItemHandler executorHandler = new AsyncWorkItemHandler(executorService, NotifySystemCommand.class.getName());
    	initSession(htHandler, executorHandler);
    	
    	//prepare input parameters for the process:
        String date = DateFormat.getDateInstance().format(new Date());
        String patientAge = "21";
        String patientGender = "F";
        String patientStatus = "Critical";
        
        Map<String, Object> inputVariables = new HashMap<String, Object>();
        inputVariables.put("bedRequestDate", date);
        inputVariables.put("patientAge", patientAge);
        inputVariables.put("patientGender", patientGender);
        inputVariables.put("patientStatus", patientStatus);
        
        //Start the process using its ID and pass the input variables
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) session.startProcess(
        		"com.plugtree.training.checkInPatient", inputVariables);
        
        //**************   Assign Bed rules   **************//
        //The process begins with the activation and firing of the ruleflow group
        //'assign-bed', that assigns the process variable 'asignbed' to 
        //'intensive-care-bed' if the patient status is critical
        Assert.assertEquals("intensive-care-bed", processInstance.getVariable("assignbed"));
        
        //**************   Coordinate Staff   **************//
        
        //The process must be in the 'Coordinate Staff' task. Let's check the
        //input parameters received by the handler associated to that task.
        Assert.assertEquals("Coordinate Staff", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        Assert.assertEquals(date, htHandler.getLastItem().getParameter("requestDate"));
        Assert.assertEquals(patientAge, htHandler.getLastItem().getParameter("requestPatientAge"));
        Assert.assertEquals(patientGender, htHandler.getLastItem().getParameter("requestPatientGender"));
        Assert.assertEquals(patientStatus, htHandler.getLastItem().getParameter("requestPatientStatus"));
        
        //let's complete the task emulating the results of this task.
        Map<String,Object> taskResults = new HashMap<String, Object>();
        taskResults.put("gateSelected", "3C");
        session.getWorkItemManager().completeWorkItem(htHandler.getLastItem().getId(), taskResults);
        
        //**************   Notify Gate to Ambulance   **************//
        //Now we are at 'Notify Gate to Ambulance' task. Let's check that the input
        //parameters configured for this tasks arrived as expected.
        Assert.assertEquals("Notify Gate to Ambulance", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        //let the executor complete the task
        Thread.sleep(5000);
        
        //by now the task should be completed, so we should be at check in Patient
        Assert.assertEquals("Check In Patient", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        //**************   Check In Patient   **************//
        //In 'Check In Patient' task we are expecting a 'notified'
        //parameter containing the value returned by the last task
        Assert.assertEquals("true", htHandler.getLastItem().getParameter("notified"));
        
        //let's complete the task passing the mocked results
        taskResults = new HashMap<String, Object>();
        String checkinDate = DateFormat.getTimeInstance().format(new Date());
        taskResults.put("checkinResult", "Check in completed successfuly");
        taskResults.put("checkinTime", checkinDate);
        session.getWorkItemManager().completeWorkItem(htHandler.getLastItem().getId(), taskResults);
        
        //The process should be completed now. Let's check the 2 output
        //parameters of the last task: they should be mapped to process variables.
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
        Assert.assertEquals("Check in completed successfuly", processInstance.getVariable("result"));
        Assert.assertEquals(checkinDate, processInstance.getVariable("bedResponseTime"));
    }

	private ExecutorService newExecutorService() {
		RuntimeManager manager = new RuntimeManager() {
			@Override
			public RuntimeEngine getRuntimeEngine(Context<?> context) {
				return new RuntimeEngine() {
					@Override
					public KieSession getKieSession() {
						return session;
					}
					@Override
					public TaskService getTaskService() {
						return null;
					}
				};
			}
			@Override
			public String getIdentifier() {
				return "testDeploymentId";
			}
			@Override
			public void disposeRuntimeEngine(RuntimeEngine runtime) { }

			@Override
			public void close() { }
			
		};
		RuntimeManagerRegistry.get().addRuntimeManager("testDeploymentId", manager);
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.executor");
	    EntityManager em = emf.createEntityManager();
		ExecutorServiceImpl service = new ExecutorServiceImpl();
		JbpmServicesPersistenceManagerImpl pm = new JbpmServicesPersistenceManagerImpl();
		pm.setEm(em);
		pm.setTransactionManager(new JbpmJTATransactionManager());
		pm.setUseSharedEntityManager(false);
		ExecutorRequestAdminServiceImpl adminService = new ExecutorRequestAdminServiceImpl();
		ExecutorImpl executor = new ExecutorImpl();
		ExecutorRunnable runnable = new ExecutorRunnable();
		ExecutorQueryServiceImpl queryService = new ExecutorQueryServiceImpl();
		queryService.setPm(pm);
		ClassCacheManager cacheManager = new ClassCacheManager();
		runnable.setClassCacheManager(cacheManager);
		runnable.setPm(pm);
		runnable.setQueryService(queryService);
		executor.setExecutorRunnable(runnable);
		executor.setClassCacheManager(cacheManager);
		executor.setPm(pm);
		executor.setQueryService(queryService);
		executor.setRetries(3);
		executor.setThreadPoolSize(3);
		adminService.setPm(pm);
		service.setAdminService(adminService);
		service.setExecutor(executor);
		service.setQueryService(queryService);
		return service;
	}

}
