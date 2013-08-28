package com.plugtree.training;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EscalationProcessTest {

    private StatefulKnowledgeSession ksession;
    
    @Before
    public void setup() throws IOException{
        this.ksession = this.createKnowledgeSession();
        
        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
    }

    @Test
    public void validScenarioTest(){
        
        List<String> errorList = new ArrayList<String>();
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inputData", new Object());
        parameters.put("errorList", errorList) ;
        
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.escalationProcess", parameters);
        
        //The process is executed until it reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
        
        Assert.assertTrue(errorList.isEmpty());
    }
    
    @Test
    public void invalidScenarioTest(){
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inputData", null);
        parameters.put("errorList", new ArrayList<String>()) ;
        
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.escalationProcess", parameters);
        
        //Because of the escalation event was thrown, the "Do Your Job" task
        //was skipped and the process is COMPLETED
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
        
        List<?> errorList = (List<?>) ((WorkflowProcessInstance)process).getVariable("errorList");
        
        Assert.assertFalse(errorList.isEmpty());
        Assert.assertEquals(1,errorList.size());
        Assert.assertTrue(errorList.contains("Invalid input data!"));
        
    }
    
    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public StatefulKnowledgeSession createKnowledgeSession() {
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("escalationProcess.bpmn2"), ResourceType.BPMN2);
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

        //return a new statefull session
        return kbase.newStatefulKnowledgeSession();
    }
}
