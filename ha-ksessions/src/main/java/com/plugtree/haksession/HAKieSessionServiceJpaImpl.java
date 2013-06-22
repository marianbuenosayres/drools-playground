package com.plugtree.haksession;

import java.util.Properties;

import org.drools.core.SessionConfiguration;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.core.process.instance.WorkItemManagerFactory;
import org.drools.core.reteoo.ReteooWorkingMemory;
import org.drools.persistence.jpa.processinstance.JPAWorkItemManagerFactory;
import org.kie.api.KieBase;
import org.kie.api.runtime.CommandExecutor;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimerJobFactoryOption;

public class HAKieSessionServiceJpaImpl implements HAKieStoreServices {

	private HAKieSessionRegistry registry;
	
    private Class< ? extends CommandExecutor>               commandServiceClass;
    private Class< ? extends WorkItemManagerFactory>        workItemManagerFactoryClass;

    private Properties                                      configProps = new Properties();

	public HAKieSessionServiceJpaImpl() {
        setDefaultImplementations();
	}

	protected void setDefaultImplementations() {
        setCommandServiceClass( DecorableSingleSessionCommandService.class );
        setProcessInstanceManagerFactoryClass( "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory" );
        setWorkItemManagerFactoryClass( JPAWorkItemManagerFactory.class );
        setProcessSignalManagerFactoryClass( "org.jbpm.persistence.processinstance.JPASignalManagerFactory" );
    }

    public void setCommandServiceClass(Class< ? extends CommandExecutor> commandServiceClass) {
        if ( commandServiceClass != null ) {
            this.commandServiceClass = commandServiceClass;
            configProps.put( "drools.commandService",
                             commandServiceClass.getName() );
        }
    }

    public Class< ? extends CommandExecutor> getCommandServiceClass() {
        return commandServiceClass;
    }

    public void setProcessInstanceManagerFactoryClass(String processInstanceManagerFactoryClass) {
        configProps.put( "drools.processInstanceManagerFactory",
                         processInstanceManagerFactoryClass );
    }

    public void setWorkItemManagerFactoryClass(Class< ? extends WorkItemManagerFactory> workItemManagerFactoryClass) {
        if ( workItemManagerFactoryClass != null ) {
            this.workItemManagerFactoryClass = workItemManagerFactoryClass;
            configProps.put( "drools.workItemManagerFactory",
                             workItemManagerFactoryClass.getName() );
        }
    }

    public Class< ? extends WorkItemManagerFactory> getWorkItemManagerFactoryClass() {
        return workItemManagerFactoryClass;
    }

    public void setProcessSignalManagerFactoryClass(String processSignalManagerFactoryClass) {
        configProps.put( "drools.processSignalManagerFactory",
                         processSignalManagerFactoryClass );
    }
	
	public void setRegistry(HAKieSessionRegistry registry) {
		this.registry = registry;
	}
	
	protected HAKieSessionRegistry getRegistry() {
		return registry;
	}

    private KieSessionConfiguration mergeConfig(KieSessionConfiguration configuration) {
        ((SessionConfiguration) configuration).addDefaultProperties(configProps);
        configuration.setOption(TimerJobFactoryOption.get("jpa"));
        return configuration;
    }

	@Override
	public KieSession newKieSession(KieBase kbase,
			KieSessionConfiguration ksconf, Environment env) {
		DecorableSingleSessionCommandService commandService = new DecorableSingleSessionCommandService(
				kbase, mergeConfig(ksconf), env) {
			@Override
			protected KieSession decorateKieSession(KieSession kieSession) {
				StatefulKnowledgeSessionImpl kieSessionImpl = (StatefulKnowledgeSessionImpl) kieSession;
				ReteooWorkingMemory session = (ReteooWorkingMemory) kieSessionImpl.session;
				return new HAKieSession(session, getRegistry(), false);
			}
		};
		return new CommandBasedStatefulKnowledgeSession(commandService);
	}

	public KieSession loadKieSession(int sessionId, KieBase kbase,
			KieSessionConfiguration ksconf, Environment env) {
		DecorableSingleSessionCommandService commandService = new DecorableSingleSessionCommandService(
				sessionId, kbase, mergeConfig(ksconf), env) {
			@Override
			protected KieSession decorateKieSession(KieSession kieSession) {
				StatefulKnowledgeSessionImpl kieSessionImpl = (StatefulKnowledgeSessionImpl) kieSession;
				ReteooWorkingMemory session = (ReteooWorkingMemory) kieSessionImpl.session;
				return new HAKieSession(session, getRegistry(), false);
			}
		};
		return new CommandBasedStatefulKnowledgeSession(commandService);
	}
}
