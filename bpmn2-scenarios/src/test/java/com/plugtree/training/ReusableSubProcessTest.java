package com.plugtree.training;

import java.util.HashMap;
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

public class ReusableSubProcessTest {

    private KieSession ksession;
    
    @Before
    public void setup() {
        this.ksession = this.createKieSession();
        
        //Console log. Try to analyze it first
        KieServices.Factory.get().getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);
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
    public KieSession createKieSession(){
    	KieServices ks = KieServices.Factory.get();
    	//Create file system
    	KieFileSystem kfs = ks.newKieFileSystem();
    	//Add simpleProcess.bpmn to kfs
    	kfs.write("src/main/resources/parentProcess.bpmn2", ResourceFactory.newClassPathResource("parentProcess.bpmn2"));
    	kfs.write("src/main/resources/childProcess.bpmn2", ResourceFactory.newClassPathResource("childProcess.bpmn2"));
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
