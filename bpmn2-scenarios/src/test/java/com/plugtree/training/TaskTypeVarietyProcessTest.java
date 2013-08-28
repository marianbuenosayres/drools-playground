package com.plugtree.training;

import java.util.HashMap;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.WorkingMemory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.ActivationCancelledEvent;
import org.drools.event.ActivationCreatedEvent;
import org.drools.event.AfterActivationFiredEvent;
import org.drools.event.AgendaGroupPoppedEvent;
import org.drools.event.AgendaGroupPushedEvent;
import org.drools.event.BeforeActivationFiredEvent;
import org.drools.event.RuleFlowGroupActivatedEvent;
import org.drools.event.RuleFlowGroupDeactivatedEvent;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.junit.Assert;
import org.junit.Test;

import com.plugtree.training.handler.HumanTaskMockHandler;

public class TaskTypeVarietyProcessTest {

    private StatefulKnowledgeSession ksession;

    /**
     * Creates a ksession from a kbase containing process definition
     * @return 
     */
    public StatefulKnowledgeSession createKnowledgeSession() {
        //Create the kbuilder
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        //Add simpleProcess.bpmn to kbuilder
        kbuilder.add(new ClassPathResource("taskTypeVarietyProcess.bpmn2"), ResourceType.BPMN2);
        kbuilder.add(new ClassPathResource("taskTypeVarietyRules.drl"), ResourceType.DRL);
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
    public void taskTypeVarietyProcessTest(){
        this.ksession = this.createKnowledgeSession();
        
        //BEGIN: REGISTER EVENT LISTENER TO FIRE RULES FOR BUSINESS RULE TASK TO WORK
        final org.drools.event.AgendaEventListener agendaEventListener = new org.drools.event.AgendaEventListener() {
            public void activationCreated(ActivationCreatedEvent event, WorkingMemory workingMemory){
                ksession.fireAllRules();
            }
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
                workingMemory.fireAllRules();
            }
            public void activationCancelled(ActivationCancelledEvent event, WorkingMemory workingMemory){ }
            public void beforeActivationFired(BeforeActivationFiredEvent event, WorkingMemory workingMemory) { }
            public void afterActivationFired(AfterActivationFiredEvent event, WorkingMemory workingMemory) { }
            public void agendaGroupPopped(AgendaGroupPoppedEvent event, WorkingMemory workingMemory) { }
            public void agendaGroupPushed(AgendaGroupPushedEvent event, WorkingMemory workingMemory) { }
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) { }
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) { }
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) { }
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
