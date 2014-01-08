package com.plugtree.training;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message.Level;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.io.ResourceFactory;

public class SingleEventProcessTest {

    private KieSession ksession;
    
    @Before
    public void setup() throws IOException{
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();

    	//Add contents to the file system
    	kfs.write("src/main/resources/singleEventProcess.bpmn2", ResourceFactory.newClassPathResource("singleEventProcess.bpmn2"));
        
        //Create the kbuilder
        KieBuilder kbuilder = ks.newKieBuilder(kfs);
        System.out.println("Compiling resources");
        kbuilder.buildAll();
        
        //Check for errors
        if (kbuilder.getResults().hasMessages(Level.ERROR)) {
            System.out.println("Error building kbase: " + kbuilder.getResults());
            throw new RuntimeException("Error building kbase!");
        }
        //Create a knowledge module and a container to access its bases and sessions
        KieModule kmodule = kbuilder.getKieModule();
        KieContainer kcontainer = ks.newKieContainer(kmodule.getReleaseId());

        this.ksession = kcontainer.newKieSession();
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
	