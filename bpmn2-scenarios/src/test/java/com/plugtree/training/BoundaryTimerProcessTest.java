package com.plugtree.training;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.SessionPseudoClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.event.KieRuntimeEventManager;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.io.ResourceFactory;

import com.plugtree.training.handler.HumanTaskMockHandler;

public class BoundaryTimerProcessTest {

    private KieSession ksession;
    private HumanTaskMockHandler humanTaskMockHandler;

    @Before
    public void setup() throws IOException {
        this.ksession = this.createKieSession();

        //Console log. Try to analyze it first
        KieServices.Factory.get().getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);
    }

    @Test
    public void validScenarioTest() throws InterruptedException {
        Map<String,Object> parameters = new HashMap<String, Object>();
        parameters.put("emailService", new ArrayList<String>()); 
        
        //Start the process using its id
        WorkflowProcessInstance process = (WorkflowProcessInstance) ksession.startProcess("com.plugtree.training.boundaryTimerProcess",parameters);
        
        //The process is in the Human Task waiting for its completion
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());

        //advance 1 hour and then complete the task
        SessionPseudoClock clock = ksession.getSessionClock();
        clock.advanceTime(1, TimeUnit.HOURS);
        
        //The Human Task is completed
        this.humanTaskMockHandler.completeWorkItem();

        //The process reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
        
        //The boss shouldn't be notified
        List<?> emailService = (List<?>) ((WorkflowProcessInstance)process).getVariable("emailService");
        Assert.assertTrue(emailService.isEmpty());
    }
    
    @Test
    public void invalidScenarioTest() throws InterruptedException {
        
        Map<String,Object> parameters = new HashMap<String, Object>();
        parameters.put("emailService", new ArrayList<String>()); 
        
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.boundaryTimerProcess",parameters);

        //The process is in the Human Task waiting for its completion
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());

        //advance 3 hours and then complete the task
        SessionPseudoClock clock = ksession.getSessionClock();
        clock.advanceTime(3, TimeUnit.HOURS);
        
        //The Human Task is completed
        this.humanTaskMockHandler.completeWorkItem();
        
        //The process reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
        
        //The boss should have been notified
        List<?> emailService = (List<?>) ((WorkflowProcessInstance)process).getVariable("emailService");

        Assert.assertFalse(emailService.isEmpty());
    }

    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public KieSession createKieSession() {
    	
    	KieServices ks = KieServices.Factory.get();
    	//Create file system
    	KieFileSystem kfs = ks.newKieFileSystem();
    	//Add simpleProcess.bpmn to kfs
    	kfs.write("src/main/resources/boundaryTimerProcess.bpmn2", ResourceFactory.newClassPathResource("boundaryTimerProcess.bpmn2"));
    	//Create builder for the file system
        KieBuilder kbuilder = ks.newKieBuilder(kfs);

        System.out.println("Compiling resources");
        kbuilder.buildAll();
        
        //Check for errors
        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            System.out.println(kbuilder.getResults());
            throw new RuntimeException("Error building kbase!");
        }
        //Create a module for the jar and a container for its knowledge bases and sessions
        KieModule kmodule = kbuilder.getKieModule();
        KieContainer kcontainer = ks.newKieContainer(kmodule.getReleaseId());
        
        //CONFIGURE SESSION'S INTERNAL CLOCK TO WORK WITH A MOCK VERSION 
        KieSessionConfiguration ksconf = ks.newKieSessionConfiguration();
    	ksconf.setOption(ClockTypeOption.get("pseudo"));
        
        //Create a kie session from the kcontainer
        KieSession newSession = kcontainer.newKieSession(ksconf);

        //Register Human Task Handler
        humanTaskMockHandler = new HumanTaskMockHandler();
        newSession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskMockHandler);
        return newSession;
    }
}