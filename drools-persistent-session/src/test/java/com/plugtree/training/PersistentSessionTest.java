package com.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.drools.core.base.MapGlobalResolver;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

import com.plugtree.training.model.MyPersistentFact;

public class PersistentSessionTest {

	private PoolingDataSource ds;
	
	@Before
	public void setUp() throws Exception {
		this.ds = new PoolingDataSource();
		this.ds.setUniqueName("jdbc/testDS");
		this.ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		this.ds.setMaxPoolSize(3);
		this.ds.setAllowLocalTransactions(true);
		this.ds.getDriverProperties().setProperty("URL", "jdbc:h2:tasks;MVCC=true;DB_CLOSE_ON_EXIT=FALSE");
		this.ds.getDriverProperties().setProperty("user", "sa");
		this.ds.getDriverProperties().setProperty("password", "sasa");
		this.ds.init();
	}
	
	@After
	public void tearDown() throws Exception {
		if (this.ds != null) {
			this.ds.close();
		}
	}
	
	@Test
	public void testPersistentSession() throws Exception {

		//create a dynamic kiebase 
		KieServices ks = KieServices.Factory.get();
		KieFileSystem kfs = ks.newKieFileSystem();
		String rule = "package org.kie.test\n" +
        			  "global java.util.List list\n" +
        			  "rule rule1\n" + 
        			  "when\n" +
        			  "  Integer(intValue > 0)\n" +
        			  "then\n" +
        			  "  list.add( 1 );\n" +
        			  "end\n";
		kfs.write("src/main/resources/test-package/test.drl", rule);
		KieBuilder kbuilder = ks.newKieBuilder(kfs);
		kbuilder.buildAll();
		if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
			throw new IllegalArgumentException("Couldn't compile rules:" + kbuilder.getResults());
		}
		KieContainer kc = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
		KieBase kbase = kc.newKieBase(null);
		
		//create an enviroment with the entity manager factory
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.drools.persistence.jpa");
		Environment environment = EnvironmentFactory.newEnvironment();
		environment.set( EnvironmentName.ENTITY_MANAGER_FACTORY, emf );
        environment.set( EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager() );
        environment.set( EnvironmentName.GLOBALS, new MapGlobalResolver() );
        environment.set( EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, new ObjectMarshallingStrategy[]{
        		new TestObjectMarshallingStrategy(environment), //stores entities to the database 
        		new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT)
        });
        
        //create the persistent session
		KieSession ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null, environment);

		List<?> list = new ArrayList<Object>();
		
		ksession.setGlobal("list", list);
		
		ksession.insert(1);
		ksession.insert(2);
		
		ksession.fireAllRules();
		
		Assert.assertEquals(2, list.size());
		
		//reload the session
		KieSession ksession2 = JPAKnowledgeService.loadStatefulKnowledgeSession(ksession.getId(), kbase, null, environment);
		
		List<?> list2 = (List<?>) ksession2.getGlobal("list");
		
		Assert.assertEquals(2, list2.size());
		
		ksession2.insert(3);
		ksession2.insert(new MyPersistentFact("myData"));
		
		//check if MyPersistentFact is now in the database
		EntityManager em = emf.createEntityManager();
		List<?> queryResults = em.createQuery("from " + MyPersistentFact.class.getName() + " f where f.data = :name")
			.setParameter("name", "myData").getResultList();
		Assert.assertEquals(1, queryResults.size());
		
		ksession2.fireAllRules();
		
		Assert.assertEquals(3, list2.size());
	}
}
