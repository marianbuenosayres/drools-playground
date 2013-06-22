package com.plugtree.hakssesion;

import java.util.Properties;

import javax.naming.Context;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.drools.core.SessionConfiguration;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.io.impl.ClassPathResource;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message.Level;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;

import bitronix.tm.resource.jdbc.PoolingDataSource;

import com.plugtree.haksession.HAKieSessionRegistry;
import com.plugtree.haksession.HAKieSessionService;
import com.plugtree.haksession.registries.JMSKieSessionRegistry;

public class HAKieSessionTest {

	private PoolingDataSource ds = new PoolingDataSource();
	private ActiveMQConnectionFactory factory;
	
	@Before
	public void setUp() throws Exception {
		
		 ds.setUniqueName("jdbc/testDS1");
	     //NON XA CONFIGS
	     ds.setClassName("org.h2.jdbcx.JdbcDataSource");
	     ds.setMaxPoolSize(3);
	     ds.setAllowLocalTransactions(true);
	     ds.getDriverProperties().put("user", "sa");
	     ds.getDriverProperties().put("password", "sasa");
	     ds.getDriverProperties().put("URL", "jdbc:h2:mem:mydb");
	     ds.init();
	     factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
	}
	
	@After
	public void tearDown() throws Exception {
		ds.close();
		factory = null;
		System.gc();
	}
	
	@Test
	public void testHAKieSession() throws Exception {
		
		Properties connProperties1 = new Properties();
		connProperties1.setProperty(JMSKieSessionRegistry.CONNECTION_FACTORY_JNDI, "my.jndi.name");
		connProperties1.setProperty(JMSKieSessionRegistry.FAIL_ON_ERROR, "true");
		connProperties1.setProperty(JMSKieSessionRegistry.NODE_IDENTIFIER, "node1");
		connProperties1.setProperty(JMSKieSessionRegistry.STORE_X_FIRINGS, "10");
		connProperties1.setProperty(JMSKieSessionRegistry.TOPIC_NAME, "ksessionTopic");
		connProperties1.setProperty(JMSKieSessionRegistry.HOW_MANY_NODES, "2");
		Context context1 = EasyMock.createMock(Context.class);
		EasyMock.expect(context1.lookup("my.jndi.name")).andReturn(factory);
		EasyMock.replay(context1);
		HAKieSessionRegistry registry1 = new JMSKieSessionRegistry(context1, connProperties1);
		EasyMock.verify(context1);
		
		Properties connProperties2 = new Properties();
		connProperties2.setProperty(JMSKieSessionRegistry.CONNECTION_FACTORY_JNDI, "my.jndi.name");
		connProperties2.setProperty(JMSKieSessionRegistry.FAIL_ON_ERROR, "true");
		connProperties2.setProperty(JMSKieSessionRegistry.NODE_IDENTIFIER, "node2");
		connProperties2.setProperty(JMSKieSessionRegistry.STORE_X_FIRINGS, "10");
		connProperties2.setProperty(JMSKieSessionRegistry.TOPIC_NAME, "ksessionTopic");
		connProperties2.setProperty(JMSKieSessionRegistry.HOW_MANY_NODES, "2");
		Context context2 = EasyMock.createMock(Context.class);
		EasyMock.expect(context2.lookup("my.jndi.name")).andReturn(factory);
		EasyMock.replay(context2);
		HAKieSessionRegistry registry2 = new JMSKieSessionRegistry(context2, connProperties2);
		EasyMock.verify(context2);
		
		KieServices ks = KieServices.Factory.get();
		KieRepository kr = ks.getRepository();
		ks.getKieClasspathContainer();
		KieFileSystem kfs = ks.newKieFileSystem();
		kfs.write(new ClassPathResource("rules/test-rules.drl"));
		KieBuilder kbuilder = ks.newKieBuilder(kfs);
		kbuilder.buildAll();
		if (kbuilder.getResults().hasMessages(Level.ERROR)) {
			throw new IllegalStateException("Problem reading DRL: " + kbuilder.getResults().toString());
		}
		KieContainer kcont = ks.newKieContainer(kr.getDefaultReleaseId());
		KieBase kbase = kcont.newKieBase(null);
		KieSessionConfiguration ksconf = SessionConfiguration.getDefaultInstance();
		Environment env = EnvironmentFactory.newEnvironment();
		
		KieSession ksession1 = HAKieSessionService.newHAKieSessionTransient(kbase, ksconf, env, registry1);
		KieSession ksession2 = HAKieSessionService.newHAKieSessionTransient(kbase, ksconf, env, registry2);
		
		CountRulesFired count1 = new CountRulesFired(ksession1);
		CountRulesFired count2 = new CountRulesFired(ksession2);
		
		String fact1 = "fact1";
		String fact2 = "fact2";
		
		ksession1.insert(fact1);
		ksession1.fireAllRules();
		ksession2.fireAllRules();

		Assert.assertEquals(1, count1.getRuleFiringCount() + count2.getRuleFiringCount());
		Assert.assertEquals(1, ksession1.getFactCount());
		Assert.assertEquals(1, ksession2.getFactCount());
		
		ksession2.insert(fact2);
		ksession2.fireAllRules();
		ksession1.fireAllRules();
		
		Assert.assertEquals(2, count1.getRuleFiringCount() + count2.getRuleFiringCount());
		Assert.assertEquals(2, ksession1.getFactCount());
		Assert.assertEquals(2, ksession2.getFactCount());
	}
	
	@Test
	public void testJPAHAKieSession() throws Exception {
		
		Properties connProperties1 = new Properties();
		connProperties1.setProperty(JMSKieSessionRegistry.CONNECTION_FACTORY_JNDI, "my.jndi.name");
		connProperties1.setProperty(JMSKieSessionRegistry.FAIL_ON_ERROR, "true");
		connProperties1.setProperty(JMSKieSessionRegistry.NODE_IDENTIFIER, "nodeA");
		connProperties1.setProperty(JMSKieSessionRegistry.STORE_X_FIRINGS, "10");
		connProperties1.setProperty(JMSKieSessionRegistry.TOPIC_NAME, "ksessionTopicPersistent");
		connProperties1.setProperty(JMSKieSessionRegistry.HOW_MANY_NODES, "2");
		Context context1 = EasyMock.createMock(Context.class);
		EasyMock.expect(context1.lookup("my.jndi.name")).andReturn(factory);
		EasyMock.replay(context1);
		HAKieSessionRegistry registry1 = new JMSKieSessionRegistry(context1, connProperties1);
		EasyMock.verify(context1);
		
		Properties connProperties2 = new Properties();
		connProperties2.setProperty(JMSKieSessionRegistry.CONNECTION_FACTORY_JNDI, "my.jndi.name");
		connProperties2.setProperty(JMSKieSessionRegistry.FAIL_ON_ERROR, "true");
		connProperties2.setProperty(JMSKieSessionRegistry.NODE_IDENTIFIER, "nodeB");
		connProperties2.setProperty(JMSKieSessionRegistry.STORE_X_FIRINGS, "10");
		connProperties2.setProperty(JMSKieSessionRegistry.TOPIC_NAME, "ksessionTopicPersistent");
		connProperties2.setProperty(JMSKieSessionRegistry.HOW_MANY_NODES, "2");
		Context context2 = EasyMock.createMock(Context.class);
		EasyMock.expect(context2.lookup("my.jndi.name")).andReturn(factory);
		EasyMock.replay(context2);
		HAKieSessionRegistry registry2 = new JMSKieSessionRegistry(context2, connProperties2);
		EasyMock.verify(context2);
		
		KieServices ks = KieServices.Factory.get();
		KieRepository kr = ks.getRepository();
		ks.getKieClasspathContainer();
		KieFileSystem kfs = ks.newKieFileSystem();
		kfs.write(new ClassPathResource("rules/test-rules.drl"));
		KieBuilder kbuilder = ks.newKieBuilder(kfs);
		kbuilder.buildAll();
		if (kbuilder.getResults().hasMessages(Level.ERROR)) {
			throw new IllegalStateException("Problem reading DRL: " + kbuilder.getResults().toString());
		}
		KieContainer kcont = ks.newKieContainer(kr.getDefaultReleaseId());
		KieBase kbase = kcont.newKieBase(null);
		KieSessionConfiguration ksconf = SessionConfiguration.getDefaultInstance();
		Environment env = EnvironmentFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa"));

		KieSession ksession1 = HAKieSessionService.newHAKieSessionPersistent(kbase, ksconf, env, registry1);
		CountRulesFired count1 = new CountRulesFired(ksession1);
		
		String fact1 = "fact1";
		String fact2 = "fact2";
		
		ksession1.insert(fact1);
		ksession1.fireAllRules();

		Assert.assertEquals(1, count1.getRuleFiringCount());
		Assert.assertEquals(1, ksession1.getFactHandles().size()); //getFactCount doesn't seem to work in CommandBasedStatefulKnowledgeSessions
		
		KieSession loadedKsession1 = HAKieSessionService.loadHAKieSessionPersistent(ksession1.getId(), kbase, ksconf, env, registry2);
		CountRulesFired count2 = new CountRulesFired(loadedKsession1);
		
		Assert.assertEquals(1, loadedKsession1.getFactHandles().size());
		
		loadedKsession1.insert(fact2);
		loadedKsession1.fireAllRules();
		
		Assert.assertEquals(2, count1.getRuleFiringCount() + count2.getRuleFiringCount());
		Assert.assertEquals(2, loadedKsession1.getFactHandles().size());
	}

}
