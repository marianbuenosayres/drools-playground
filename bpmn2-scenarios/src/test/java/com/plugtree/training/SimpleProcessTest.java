package com.plugtree.training;

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
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.io.ResourceFactory;

public class SimpleProcessTest {

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
    	kfs.write("src/main/resources/simpleProcess.bpmn2", ResourceFactory.newClassPathResource("simpleProcess.bpmn2"));
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
    public void simpleProcessTest(){
        this.ksession = this.createKieSession();
    	//Register WorkItemManagers for all the generic tasks in the process
    	TestAsyncWorkItemHandler task11Handler = new TestAsyncWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("task1.1", task11Handler);
        TestAsyncWorkItemHandler task12Handler = new TestAsyncWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("task1.2", task12Handler);
        TestAsyncWorkItemHandler task13Handler = new TestAsyncWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("task1.3", task13Handler);
        TestAsyncWorkItemHandler task21Handler = new TestAsyncWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("task2.1", task21Handler);
        TestAsyncWorkItemHandler task22Handler = new TestAsyncWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("task2.2", task22Handler);
    	
    	
    	//Start the process using its id
        ProcessInstance process = ksession.startProcess("com.plugtree.training.simpleprocess");
        
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        task11Handler.complete();
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        task13Handler.complete();
        task12Handler.complete();
        //up to here, we are at the script task and afterwards we have to 
        //complete either task2.1 or task2.2
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        task22Handler.complete();
        //after completing just one, we finish the process
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }

    public static class TestAsyncWorkItemHandler implements WorkItemHandler {
    	
    	private long workItemId;
    	private WorkItemManager manager;
    	
    	@Override
    	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    		//do nothing
    	}
    	
    	@Override
    	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    		// Register work item id to know when to continue
    		this.workItemId = workItem.getId();
    		// Register work item manager to continue operation internally
    		this.manager = manager;
    	}
    	
    	public void complete() {
    		this.manager.completeWorkItem(workItemId, null);
    	}
    	
    	public long getWorkItemId() {
			return workItemId;
		}
    }
}
