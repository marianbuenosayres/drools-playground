package com.plugtree.haksession;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.drools.core.SessionConfiguration;
import org.drools.core.common.AbstractRuleBase;
import org.drools.core.event.RuleBaseEventListener;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.core.reteoo.ReteooStatefulSession;
import org.drools.core.reteoo.ReteooWorkingMemory.WorkingMemoryReteAssertAction;
import org.kie.api.KieBase;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;

public class HAKieSessionServiceImpl implements HAKieStoreServices {

	//weak storage of sessions;
	private static Map<Integer, WeakReference<KieSession>> sessions = new HashMap<Integer, WeakReference<KieSession>>();
	
	private HAKieSessionRegistry registry;
	
	public HAKieSessionServiceImpl() {
	}
	
	public void setRegistry(HAKieSessionRegistry registry) {
		this.registry = registry;
	}
	
	protected HAKieSessionRegistry getRegistry() {
		return registry;
	}
	
	public KieSession newKieSession(KieBase kbase,
			KieSessionConfiguration ksconf, Environment env) {
		if ( ksconf == null ) {
			ksconf = SessionConfiguration.getDefaultInstance();
        }
        if ( env == null ) {
            env = EnvironmentFactory.newEnvironment();
        }
    	KnowledgeBaseImpl kbaseImpl = (KnowledgeBaseImpl) kbase;
    	AbstractRuleBase ruleBase = (AbstractRuleBase) kbaseImpl.getRuleBase();
        SessionConfiguration sessionConfig = (SessionConfiguration) ksconf;
        ruleBase.readLock();
        int id = ruleBase.nextWorkingMemoryCounter();
        KieSession retval = null;
        try {
            ReteooStatefulSession session = new ReteooStatefulSession( id, ruleBase, sessionConfig, env );
            retval = new HAKieSession(session, registry, true);
            sessions.put(id, new WeakReference<KieSession>(retval));

            if ( sessionConfig.isKeepReference() ) {
                ruleBase.addStatefulSession( session );
                for (Object listener : session.getRuleBaseUpdateListeners()) {
                    ruleBase.addEventListener((RuleBaseEventListener) listener);
                }
            }

            session.queueWorkingMemoryAction( new WorkingMemoryReteAssertAction( session.getInitialFactHandle(),
                                                                                 false,
                                                                                 true,
                                                                                 null,
                                                                                 null ) );
            return retval;
        } finally {
            ruleBase.readUnlock();
        }
	}

	public KieSession loadKieSession(int id, KieBase kbase,
			KieSessionConfiguration configuration, Environment environment) {
		throw new UnsupportedOperationException("Transient ksessions not persisted through ID");
	}

}
