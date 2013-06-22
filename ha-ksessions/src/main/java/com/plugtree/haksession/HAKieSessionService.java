package com.plugtree.haksession;

import org.kie.api.KieBase;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;

public class HAKieSessionService {

	private static HAKieStoreServices jpaProvider = new HAKieSessionServiceJpaImpl();
	private static HAKieStoreServices simpleProvider = new HAKieSessionServiceImpl();
	
	private HAKieSessionService() {
	}
	
	public static synchronized KieSession newHAKieSessionPersistent(
			KieBase kbase, KieSessionConfiguration ksconf, 
			Environment env, HAKieSessionRegistry registry) {
		jpaProvider.setRegistry(registry);
		return jpaProvider.newKieSession(kbase, ksconf, env);
	}

	public static synchronized KieSession loadHAKieSessionPersistent(int sessionId,
			KieBase kbase, KieSessionConfiguration ksconf, 
			Environment env, HAKieSessionRegistry registry) {
		jpaProvider.setRegistry(registry);
		return jpaProvider.loadKieSession(sessionId, kbase, ksconf, env);
	}
	
	public static synchronized KieSession newHAKieSessionTransient(
			KieBase kbase, KieSessionConfiguration ksconf, 
			Environment env, HAKieSessionRegistry registry) {
		simpleProvider.setRegistry(registry);
		return simpleProvider.newKieSession(kbase, ksconf, env);
	}
}
