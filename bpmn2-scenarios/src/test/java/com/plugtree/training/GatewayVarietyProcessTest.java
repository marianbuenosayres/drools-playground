package com.plugtree.training;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.io.ResourceFactory;

public class GatewayVarietyProcessTest {

    private KieSession ksession;

    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public KieSession createKieSession() {
    	
    	KieServices ks = KieServices.Factory.get();
    	//Create file system
    	KieFileSystem kfs = ks.newKieFileSystem();
    	//Add simpleProcess.bpmn to kfs
    	kfs.write("src/main/resources/gatewayVarietyProcess.bpmn2", ResourceFactory.newClassPathResource("gatewayVarietyProcess.bpmn2"));
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

    @Test
    public void gatewayVarietyProcessTest(){
        this.ksession = this.createKieSession();
        
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
