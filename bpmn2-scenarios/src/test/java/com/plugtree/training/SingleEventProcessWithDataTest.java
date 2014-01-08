package com.plugtree.training;

import java.io.File;
import java.io.IOException;

import org.junit.After;
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
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.logger.KnowledgeRuntimeLogger;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;

public class SingleEventProcessWithDataTest {

    private KnowledgeRuntimeLogger fileLogger;
    private KieSession ksession;
    
    @Before
    public void setup() throws IOException{
        this.ksession = this.createKieSession();
        
        //Console log. Try to analyze it first
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);
        
        //File logger: try to open its output using Audit View in eclipse
        File logFile = File.createTempFile("process-output", "");
        System.out.println("Log file= "+logFile.getAbsolutePath()+".log");
        fileLogger = KnowledgeRuntimeLoggerFactory.newFileLogger((KnowledgeRuntimeEventManager) ksession,logFile.getAbsolutePath());
    }

    @After
    public void cleanup(){
        if (this.fileLogger != null){
            this.fileLogger.close();
        }
    } 
    
    @Test
    public void validPaymentTest(){
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.singleEventProcessWithData");
        
        //The process is in the gateway waiting for the event
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //The event arrives
        ksession.signalEvent("payment", 110);
        
        //The process continues until it reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
        
        //Validate paymentAmount process variable
        WorkflowProcessInstance wfProcess = (WorkflowProcessInstance) process;
        Assert.assertNotNull(wfProcess.getVariable("paymentAmount"));
        Assert.assertEquals(wfProcess.getVariable("paymentAmount"), 110);
    }
    
    @Test
    public void invalidPaymentTest(){
        //Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.singleEventProcessWithData");
        
        //The process is in the gateway waiting for the event
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        
        //The event arrives
        ksession.signalEvent("payment", 90);
        
        //The process continues until it reaches the end node
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());

        //Validate paymentAmount process variable
        WorkflowProcessInstance wfProcess = (WorkflowProcessInstance) process;
        Assert.assertNotNull(wfProcess.getVariable("paymentAmount"));
        Assert.assertEquals(wfProcess.getVariable("paymentAmount"), 90);
    }
    
    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public KieSession createKieSession() {
    	
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();

    	//Add contents to the file system
    	kfs.write("src/main/resources/singleEventProcessWithData.bpmn2", ResourceFactory.newClassPathResource("singleEventProcessWithData.bpmn2"));
        
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

        //return a new session
        return kcontainer.newKieSession();
    }
}
