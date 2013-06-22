package com.plugtree.haksession;

import java.util.List;

import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.core.reteoo.ReteooWorkingMemory;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.WorkingMemoryEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.Match;

public class HAKieSession extends StatefulKnowledgeSessionImpl implements KieSession {

	private final AgendaFilter haFilter;
	private final HAKieSessionRegistry registry;
	
	public HAKieSession(ReteooWorkingMemory session, HAKieSessionRegistry registry, boolean shareWMChangesThroughRegistry) {
		super(session);
		this.registry = registry;
		this.registry.setKieSession(this);
		this.haFilter = new AgendaFilter() {
			@Override
			public boolean accept(Match match) {
				List<Object> facts = match.getObjects();
				String ruleName = match.getRule().getName();
				return !HAKieSession.this.registry.hasFiredRuleForObjects(ruleName, facts);
			}
		};
		this.addEventListener(new DefaultAgendaEventListener() {
			@Override
			public void beforeMatchFired(BeforeMatchFiredEvent event) {
				List<Object> facts = event.getMatch().getObjects();
				String ruleName = event.getMatch().getRule().getName();
				HAKieSession.this.registry.ruleFiredForObjects(ruleName, facts);
			}
		});
		if (shareWMChangesThroughRegistry) {
			addEventListener(new WorkingMemoryEventListener() {
				@Override
				public void objectUpdated(ObjectUpdatedEvent event) {
					Object oldFact = event.getOldObject();
					Object newFact = event.getObject();
					HAKieSession.this.registry.factUpdated(oldFact, newFact);
				}
				
				@Override
				public void objectInserted(ObjectInsertedEvent event) {
					Object fact = event.getObject();
					HAKieSession.this.registry.factInserted(fact);
				}
				
				@Override
				public void objectDeleted(ObjectDeletedEvent event) {
					Object fact = event.getOldObject();
					HAKieSession.this.registry.factRetracted(fact);
				}
			});
		}
	}
	
	@Override
	public int fireAllRules(final AgendaFilter agendaFilter) {
		return super.fireAllRules(new AgendaFilter() {
			@Override
			public boolean accept(Match match) {
				return agendaFilter.accept(match) && haFilter.accept(match);
			}
		});
	}

	@Override
	public int fireAllRules(final AgendaFilter agendaFilter, int max) {
		return super.fireAllRules(new AgendaFilter() {
			@Override
			public boolean accept(Match match) {
				return agendaFilter.accept(match) && haFilter.accept(match);
			}
		}, max);
	}
	
	@Override
	public void fireUntilHalt() {
		super.fireUntilHalt(haFilter);
	}
	
	@Override
	public void fireUntilHalt(final AgendaFilter agendaFilter) {
		super.fireUntilHalt(new AgendaFilter() {
			@Override
			public boolean accept(Match match) {
				return agendaFilter.accept(match) && haFilter.accept(match);
			}
		});
	}
	
	@Override
	public int fireAllRules() {
		return super.fireAllRules(haFilter);
	}
	
	@Override
	public int fireAllRules(int max) {
		return super.fireAllRules(haFilter, max);
	}
}
