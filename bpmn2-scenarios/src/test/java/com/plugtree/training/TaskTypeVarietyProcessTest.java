package com.plugtree.training;

import java.util.HashMap;
import java.util.Map;

import org.drools.core.WorkingMemory;
import org.drools.core.event.ActivationCancelledEvent;
import org.drools.core.event.ActivationCreatedEvent;
import org.drools.core.event.AfterActivationFiredEvent;
import org.drools.core.event.AgendaGroupPoppedEvent;
import org.drools.core.event.AgendaGroupPushedEvent;
import org.drools.core.event.BeforeActivationFiredEvent;
import org.drools.core.event.RuleFlowGroupActivatedEvent;
import org.drools.core.event.RuleFlowGroupDeactivatedEvent;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message.Level;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.io.ResourceFactory;

import com.plugtree.training.handler.HumanTaskMockHandler;

public class TaskTypeVarietyProcessTest {

    private KieSession ksession;

    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public KieSession createKieSession() {
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();

    	//Add contents to the file system
    	kfs.write("src/main/resources/taskTypeVarietyProcess.bpmn2", ResourceFactory.newClassPathResource("taskTypeVarietyProcess.bpmn2"));
    	kfs.write("src/main/resources/taskTypeVarietyRules.drl", ResourceFactory.newClassPathResource("taskTypeVarietyRules.drl"));
        
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

    @Test
    public void taskTypeVarietyProcessTest(){
        this.ksession = this.createKieSession();
        
        //BEGIN: REGISTER EVENT LISTENER TO FIRE RULES FOR BUSINESS RULE TASK TO WORK
        final org.drools.core.event.AgendaEventListener agendaEventListener = new org.drools.core.event.AgendaEventListener() {
            public void activationCreated(ActivationCreatedEvent event, WorkingMemory workingMemory){
                ksession.fireAllRules();
            }
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
                workingMemory.fireAllRules();
            }
            public void activationCancelled(ActivationCancelledEvent event, WorkingMemory workingMemory){ }
            public void beforeActivationFired(BeforeActivationFiredEvent event, WorkingMemory workingMemory) { }
            public void afterActivationFired(AfterActivationFiredEvent event, WorkingMemory workingMemory) { }
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) { }
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) { }
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) { }
			public void agendaGroupPopped(AgendaGroupPoppedEvent event, WorkingMemory workingMemory) { }
			public void agendaGroupPushed(AgendaGroupPushedEvent event, WorkingMemory workingMemory) { }
        };
        //adding it is a bit dirty for the time being, but it works:
        ((StatefulKnowledgeSessionImpl) ksession).session.addEventListener(agendaEventListener);
        //END: REGISTER EVENT LISTENER TO FIRE RULES FOR BUSINESS RULE TASK TO WORK
        
    	//Register WorkItemManagers for all the generic tasks in the process
        HumanTaskMockHandler taskHandler1 = new HumanTaskMockHandler();
        TestAsyncWorkItemHandler taskHandler2 = new TestAsyncWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", taskHandler1);
        ksession.getWorkItemManager().registerWorkItemHandler("mySpecialTaskType", taskHandler2);
        
    	//Start the process using its id
    	Map<String, Object> variables = new HashMap<String, Object>();
    	variables.put("input", "222");
        ProcessInstance process = ksession.startProcess("com.plugtree.training.taskTypeVarietyProcess", variables);

        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        taskHandler1.completeWorkItem();
        
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());
        //we see the parameter in the last task
        Object param = taskHandler2.getWorkItemParameter("ruleExecution");
        Assert.assertNotNull(param);
        Assert.assertEquals("message", param);
        taskHandler2.complete();

        //after completing the last task, we finish the process
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, process.getState());
    }

    public static class TestAsyncWorkItemHandler implements WorkItemHandler {
    	
    	private WorkItemManager manager;
    	private WorkItem workItem;
    	
    	@Override
    	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    		//do nothing
    	}
    	
    	public Object getWorkItemParameter(String key) {
			return workItem.getParameter(key);
		}

		@Override
    	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
    		// Register work item to know when to continue
    		this.workItem = workItem;
    		// Register work item manager to continue operation internally
    		this.manager = manager;
    	}
    	
    	public void complete() {
    		this.manager.completeWorkItem(workItem.getId(), null);
    	}
    }
}
