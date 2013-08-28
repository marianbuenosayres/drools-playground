package com.plugtree.training;

import java.util.HashMap;
import java.util.Map;

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
import org.junit.Test;

public class GatewayVarietyProcessTest {

    private StatefulKnowledgeSession ksession;

    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public StatefulKnowledgeSession createKnowledgeSession() {
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("gatewayVarietyProcess.bpmn2"), ResourceType.BPMN2);
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

        //return a new stateful session
        return kbase.newStatefulKnowledgeSession();
    }

    @Test
    public void gatewayVarietyProcessTest(){
        this.ksession = this.createKnowledgeSession();
        
    	//Start the process using its id
    	Map<String, Object> variables = new HashMap<String, Object>();
    	variables.put("parameter", 100);
        ProcessInstance process = ksession.startProcess("com.plugtree.training.gatewayVarietyProcess", variables);

        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        ksession.signalEvent("externalSignal", null);
        
        //after the signal, we finish the process
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }
}
