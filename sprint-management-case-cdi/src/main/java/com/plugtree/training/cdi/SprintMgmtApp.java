package com.plugtree.training.cdi;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

import org.drools.core.WorkingMemory;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.common.AbstractWorkingMemory;
import org.drools.core.event.DefaultAgendaEventListener;
import org.drools.core.event.RuleFlowGroupActivatedEvent;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.persistence.SingleSessionCommandService;
import org.jboss.seam.transaction.DefaultSeamTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.SingletonRuntimeManager;
import org.jbpm.runtime.manager.impl.factory.JPASessionFactory;
import org.jbpm.runtime.manager.impl.factory.LocalTaskServiceFactory;
import org.jbpm.services.task.wih.NonManagedLocalHTWorkItemHandler;
import org.kie.api.KieBase;
import org.kie.api.cdi.KBase;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.internal.deployment.DeploymentService;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.context.EmptyContext;

import bitronix.tm.TransactionManagerServices;

import com.plugtree.training.ReqsCompletedListener;
import com.plugtree.training.handlers.CompilationWorkItemHandler;
import com.plugtree.training.handlers.DeploymentWorkItemHandler;
import com.plugtree.training.handlers.NotificationWorkItemHandler;

@ApplicationScoped
public class SprintMgmtApp {

	@Inject
	@KBase("sprint")
	private KieBase kbase;
	private SingletonRuntimeManager runtimeManager;
	private ReqsCompletedListener listener = new ReqsCompletedListener();
	private EntityManagerFactory emf = null;
	
	@Produces @PersistenceUnit(unitName="org.jbpm.persistence.jpa")
	public synchronized EntityManagerFactory getEmf() {
		if (this.emf == null) {
			this.emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		}
		return this.emf;
	}

	@Produces
	public DeploymentService getDeploymentService() {
		return new KModuleDeploymentService();
	}
	
	@Produces
	public SeamTransaction getTransaction() {
		return new DefaultSeamTransaction();
	}
	
	@Produces 
	public EntityManager getEm() {
		return getEmf().createEntityManager();
	}
	
	public void onClose(@Disposes EntityManager em) {
		em.close();
	}

	public TaskService getTaskService() {
		return runtimeManager.getRuntimeEngine(null).getTaskService();
	}

	public void start() {
		RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault().
				entityManagerFactory(getEmf()).
				addEnvironmentEntry(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager()).
				userGroupCallback(new CDIUserGroupCallback()).
				knowledgeBase(kbase).get();
		this.runtimeManager = new SingletonRuntimeManager(environment, 
				new JPASessionFactory(environment), 
				new LocalTaskServiceFactory(environment),
				"default-singleton");
		this.runtimeManager.init();
        initRuntime(runtimeManager.getRuntimeEngine(EmptyContext.get()).getKieSession());
	}
	
	public void initRuntime(KieSession ksession) {
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new NonManagedLocalHTWorkItemHandler(ksession, getTaskService()));
        ksession.getWorkItemManager().registerWorkItemHandler("compilation", new CompilationWorkItemHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("deployment", new DeploymentWorkItemHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("notification", new NotificationWorkItemHandler());
		ksession.addEventListener(this.listener);
		CommandBasedStatefulKnowledgeSession cmdKsession = (CommandBasedStatefulKnowledgeSession) ksession;
		SingleSessionCommandService sscs = (SingleSessionCommandService) cmdKsession.getCommandService();
		StatefulKnowledgeSessionImpl realSession = (StatefulKnowledgeSessionImpl) sscs.getKieSession();
		AbstractWorkingMemory wm = (AbstractWorkingMemory) realSession.session;
		wm.addEventListener(new DefaultAgendaEventListener() {
			@Override
			public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
				workingMemory.fireAllRules();
			}
		});
		//ksession.addEventListener(AuditLoggerFactory.newJPAInstance(getEmf(), getEnvironment()));
	}
	
	private Environment env = null;

	public synchronized Environment getEnvironment() {
		if (this.env == null) {
			this.env = EnvironmentFactory.newEnvironment();
			this.env.set("IS_JTA_TRANSACTION", Boolean.FALSE);
			this.env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, getEmf());
		}
		return this.env;
	}

	public ProcessInstance startProcess(String processId, Map<String, Object> params) {
		return runtimeManager.getRuntimeEngine(null).getKieSession().startProcess(processId, params);
	}

	public ProcessInstance getProcessInstance(long processInstanceId) {
		return runtimeManager.getRuntimeEngine(null).getKieSession().getProcessInstance(processInstanceId);
	}
}
