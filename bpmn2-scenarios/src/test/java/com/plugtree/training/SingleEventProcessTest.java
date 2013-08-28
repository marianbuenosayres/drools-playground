package com.plugtree.training;

import java.io.IOException;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleEventProcessTest {

    private StatefulKnowledgeSession ksession;
    
    @Before
    public void setup() throws IOException{
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("singleEventProcess.bpmn2"), ResourceType.BPMN2);
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
        this.ksession = kbase.newStatefulKnowledgeSession();
    }

    @Test
    public void simpleProcessTest(){
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.singleEventProcess");
        
        //The process is in the gateway waiting for the event
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //The event arrives
        ksession.signalEvent("payment", null);
        
        //The process continues until it reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }
}
	