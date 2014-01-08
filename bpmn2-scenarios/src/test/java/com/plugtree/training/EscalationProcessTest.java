package com.plugtree.training;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.io.ResourceFactory;

public class EscalationProcessTest {

    private KieSession ksession;
    
    @Before
    public void setup() throws IOException{
        this.ksession = this.createKieSession();
        
        //Console log. Try to analyze it first
        KieServices.Factory.get().getLoggers().newConsoleLogger((KnowledgeRuntimeEventManager) this.ksession);
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
    public KieSession createKieSession() {
    	KieServices ks = KieServices.Factory.get();
    	//Create file system
    	KieFileSystem kfs = ks.newKieFileSystem();
    	//Add simpleProcess.bpmn to kfs
    	kfs.write("src/main/resources/escalationProcess.bpmn2", ResourceFactory.newClassPathResource("escalationProcess.bpmn2"));
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
        
        //Create a kie session from the kcontainer
        return kcontainer.newKieSession();
    }
}
