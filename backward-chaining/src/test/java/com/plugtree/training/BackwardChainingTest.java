package com.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Message.Level;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;

import com.plugtree.training.model.Location;

public class BackwardChainingTest {

	private List<String> firedRules = new ArrayList<String>();
	
	@Test
	public void testBackwardChaining() {
		KieServices ks = KieServices.Factory.get();
		KieFileSystem kfs = ks.newKieFileSystem();
		kfs.write(ks.getResources().newClassPathResource("rules/queries.drl"));
		KieBuilder kbuilder = ks.newKieBuilder(kfs).buildAll();
		if (kbuilder.getResults().getMessages(Level.ERROR).size() > 0) {
			for (Message msg : kbuilder.getResults().getMessages(Level.ERROR)) {
				System.out.println(msg);
			}
			throw new IllegalArgumentException("Couldn't parse knowledge");
		}
		KieContainer kcontainer = ks.newKieContainer(
				kbuilder.getKieModule().getReleaseId());
		KieBase kbase = kcontainer.getKieBase();
		KieSession ksession = kbase.newKieSession();
		
		//We add an AgendaEventListener to keep track of fired rules.
        ksession.addEventListener(new DefaultAgendaEventListener(){
            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                firedRules.add(event.getMatch().getRule().getName());
            }
        });
		
		ksession.insert(new Location("House", "Office"));
		ksession.insert(new Location("House", "Kitchen"));
		ksession.insert(new Location("Kitchen", "Knife"));
		ksession.insert(new Location("Kitchen", "Cheese"));
		ksession.insert(new Location("Office", "Desk"));
		ksession.insert(new Location("Office", "Chair"));
		ksession.insert(new Location("Desk", "Computer"));
		ksession.insert(new Location("Desk", "Pencil"));
		
		QueryResults results1 = ksession.getQueryResults("itContains", 
				new Object[] {"House", "Pencil"});
		Assert.assertTrue(results1.size() == 1);
		
		QueryResults results2 = ksession.getQueryResults("itContains", 
				new Object[] {"Desk", "Cheese"});
		Assert.assertTrue(results2.size() == 0);
		
		ksession.insert("go");
		ksession.fireAllRules();

		Assert.assertEquals(1,firedRules.size());
		Assert.assertTrue(firedRules.contains("go"));

		firedRules.clear();

		ksession.insert("go1");
		ksession.fireAllRules();

		Assert.assertEquals(2,firedRules.size());
		Assert.assertTrue(firedRules.contains("go"));
		Assert.assertTrue(firedRules.contains("go1"));
	}
}
