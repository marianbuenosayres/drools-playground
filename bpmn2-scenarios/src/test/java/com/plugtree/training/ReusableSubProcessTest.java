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
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReusableSubProcessTest {

    private StatefulKnowledgeSession ksession;
    
    @Before
    public void setup() {
        this.ksession = this.createKnowledgeSession();
        
        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger(ksession);
    }

    @Test
    public void reusableProcessTest() {
        
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("data1", "my initial data 1");
		parameters.put("data3", "my initial data 3");
		
		ProcessInstance process = ksession.startProcess("com.plugtree.training.parentProcess", parameters);
		
        //The process will run until there are no more nodes to execute.
        //Because this process doesn't have any wait-state, the process is
        //running from start to end
		Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());

        //Because the process changed the reference of messages variable, we
        //need to get it again.
        //It is a good practice to retrieve the process variables after its 
        //execution instead of use the old variables passed as parameters.
        String data2 = (String) ((WorkflowProcessInstance)process).getVariable("data2");
        
        Assert.assertNotNull(data2);
    }
    
    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public StatefulKnowledgeSession createKnowledgeSession(){
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("parentProcess.bpmn2"), ResourceType.BPMN2);
        kbuilder.add(new ClassPathResource("childProcess.bpmn2"), ResourceType.BPMN2);
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
