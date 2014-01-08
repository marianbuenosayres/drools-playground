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
import org.kie.api.event.KieRuntimeEventManager;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.io.ResourceFactory;

public class MultipleInstanceProcessTest {

    private KieSession ksession;
    
    @Before
    public void setup() throws IOException{
        this.ksession = this.createKieSession();
        //Console log. Try to analyze it first
        KieServices.Factory.get().getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);
    }

    @Test
    public void multipleNodeInstanceProcessTest(){
        
        List<Number> numbers = new ArrayList<Number>();
        numbers.add(2);
        numbers.add(4);
        numbers.add(56);
        numbers.add(7);
        numbers.add(10);
        numbers.add(13);
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("numberList", numbers);
        parameters.put("errorList", new ArrayList<Number>());
        
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.multipleInstanceProcess",parameters);
        
        //The process will run until there are no more nodes to execute.
        //Because this process doesn't have any wait-state, the process is
        //running from start to end
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
        //Let see the error list. It should contain 2 values: 7 and 13
        List<?> errorList = (List<?>) ((WorkflowProcessInstance)process).getVariable("errorList");
        
        Assert.assertFalse(errorList.isEmpty());
        Assert.assertEquals(2,errorList.size());
        Assert.assertTrue(errorList.contains(7));
        Assert.assertTrue(errorList.contains(13));
        
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
    	kfs.write("src/main/resources/multipleInstanceProcess.bpmn2", ResourceFactory.newClassPathResource("multipleInstanceProcess.bpmn2"));
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
