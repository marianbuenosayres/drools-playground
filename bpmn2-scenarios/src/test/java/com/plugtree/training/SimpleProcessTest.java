package com.plugtree.training;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.junit.Assert;
import org.junit.Test;

public class SimpleProcessTest {

    private StatefulKnowledgeSession ksession;

    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public StatefulKnowledgeSession createKnowledgeSession() {
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("simpleProcess.bpmn2"), ResourceType.BPMN2);
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
    public void simpleProcessTest(){
        this.ksession = this.createKnowledgeSession();
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
