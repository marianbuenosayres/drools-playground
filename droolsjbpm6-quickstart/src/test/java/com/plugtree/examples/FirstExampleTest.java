package com.plugtree.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.drools.core.io.impl.ClassPathResource;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;

import com.plugtree.examples.model.Person;
import com.plugtree.examples.model.Pet;

public class FirstExampleTest {


    /**
     * Cat on a Tree Test
     */
    @Test
    public void catOnATreeTest() {
    	
        // Create a file system to add knowledge to
    	KieServices ks = KieServices.Factory.get();
    	KieRepository kr = ks.getRepository();
    	KieFileSystem kfs = ks.newKieFileSystem();
        // Add our knowledge
		kfs.write(new ClassPathResource("rules.drl"));
		kfs.write(new ClassPathResource("process.bpmn2"));
        // Create the Knowledge Builder
		KieBuilder kbuilder = ks.newKieBuilder(kfs);
		kbuilder.buildAll();
        //Check for errors during the compilation of the rules
		Results results = kbuilder.getResults();
		List<Message> errors = results.getMessages(Message.Level.ERROR);
        if (errors.size() > 0) {
            for (Message error : errors) {
                System.err.println(error);
            }
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        // Create the Knowledge Base
        KieContainer kcont = ks.newKieContainer(kr.getDefaultReleaseId());
		KieBase kbase = kcont.newKieBase(null);
        // Create the StatefulSession using the Knowledge Base that contains
        // the compiled rules
		KieSession ksession = kbase.newKieSession();
        
        // Add a Work Item Handler for managing the process instance human interaction
        TestWorkItemHandler handler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", handler);

        //Register the session as a global, to allow it to start a process instance from the rules
        ksession.setGlobal("processRuntime", ksession);
        
        // We can add a runtime logger to understand what is going on inside the
        // Engine
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);

        // Create a Person
        Person person = new Person("Joe");
        // Create a Pet
        Pet pet = new Pet("mittens", "on a limb", Pet.PetType.CAT);
        // Set the Pet to the Person
        person.setPet(pet);

        // Now we will insert the Pet and the Person into the KnowledgeSession
        ksession.insert(pet);
        ksession.insert(person);

        // We will fire all the rules that were activated
        ksession.fireAllRules();
        
        //The process should have started
        WorkItem item = handler.getItem();
        Assert.assertNotNull(item);
        ProcessInstance processInstance = ksession.getProcessInstance(item.getProcessInstanceId());
        Assert.assertNotNull(processInstance);
        
        //Continue the process with the pet position
        Map<String, Object> taskResults = new HashMap<String, Object>();
        taskResults.put("petPosition", "on the street");
        ksession.getWorkItemManager().completeWorkItem(item.getId(), taskResults);
        
        //make sure the process is completed
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
        
        Assert.assertEquals(pet.getPosition(), "on the street");
        
        //Dispose the knowledge session
        ksession.dispose();

    }
}
