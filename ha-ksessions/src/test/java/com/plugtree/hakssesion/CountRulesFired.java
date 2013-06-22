package com.plugtree.hakssesion;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

public class CountRulesFired extends DefaultAgendaEventListener {

	private int ruleFiringCount = 0;

	public CountRulesFired(KieSession ksession) {
		ksession.addEventListener(this);
	}
	
	@Override
	public void afterMatchFired(AfterMatchFiredEvent event) {
		synchronized(this) {
			ruleFiringCount++;
		}
	}
	
	public int getRuleFiringCount() {
		return ruleFiringCount;
	}
}
