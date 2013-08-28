package com.plugtree.training;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.drools.time.SessionPseudoClock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.plugtree.training.handler.HumanTaskMockHandler;

public class BoundaryTimerProcessTest {

    private StatefulKnowledgeSession ksession;
    private HumanTaskMockHandler humanTaskMockHandler;

    @Before
    public void setup() throws IOException {
        this.ksession = this.createKnowledgeSession();

        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);

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
    public StatefulKnowledgeSession createKnowledgeSession() {
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("boundaryTimerProcess.bpmn2"), ResourceType.BPMN2);
        System.out.println("Compiling resources");

        //Check for errors
        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                    System.out.println("Error building kbase: " + error.getMessage());
                }
            }
            throw new RuntimeException("Error building kbase!");
        }

        //Create a knowledge base and add the generated package
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
 
        
    	//CONFIGURE SESSION'S INTERNAL CLOCK TO WORK WITH A MOCK VERSION 
        KnowledgeSessionConfiguration ksessionConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
    	ksessionConf.setOption(ClockTypeOption.get("pseudo"));
        //create a new statefull session
        final StatefulKnowledgeSession newSession = kbase.newStatefulKnowledgeSession(ksessionConf, KnowledgeBaseFactory.newEnvironment());
        //Register Human Task Handler
        humanTaskMockHandler = new HumanTaskMockHandler();

        newSession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskMockHandler);

        return newSession;
    }
}