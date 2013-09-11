package com.plugtree.training;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Properties;

import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.UserGroupCallback;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class RuntimeManagerTest {

	private PoolingDataSource ds;
	private RuntimeManager manager;
	private UserGroupCallback userGroupCallback;

	private void cleanupSingletonSessionId() {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		if (tempDir.exists()) {
			String[] jbpmSerFiles = tempDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith("-jbpmSessionId.ser");
				}
			});
			for (String file : jbpmSerFiles) {
				new File(tempDir, file).delete();
			}
		}
	}
	
	@Before
	public void setup() {
		cleanupSingletonSessionId();
		this.ds = new PoolingDataSource();
		this.ds.setUniqueName("jdbc/testDS");
		this.ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		this.ds.setMaxPoolSize(10);
		this.ds.setAllowLocalTransactions(true);
		this.ds.getDriverProperties().setProperty("URL", "jdbc:h2:tasks;MVCC=true;DB_CLOSE_ON_EXIT=FALSE");
		this.ds.getDriverProperties().setProperty("user", "sa");
		this.ds.getDriverProperties().setProperty("password", "sasa");
		this.ds.init();
		
		Properties properties= new Properties();
        properties.setProperty("mary", "HR");
        properties.setProperty("john", "HR");
        userGroupCallback = new JBossUserGroupCallbackImpl(properties);

	}
	
	@After
    public void teardown() {
        if (manager != null) {
            manager.close();
        }
        if (ds != null) {
        	ds.close();
        }
    }
	
	@Test
    public void testSingletonRuntimeManager() {
        RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault()
                .userGroupCallback(userGroupCallback)
                .addAsset(ResourceFactory.newClassPathResource("sample.bpmn"), ResourceType.BPMN2)
                .get();
        
        manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(environment);        
        Assert.assertNotNull(manager);
        
        RuntimeEngine runtime = manager.getRuntimeEngine(EmptyContext.get());
        KieSession ksession = runtime.getKieSession();
        Assert.assertNotNull(ksession);       
        
        int sessionId = ksession.getId();
        Assert.assertTrue(sessionId == 1);
        
        runtime = manager.getRuntimeEngine(EmptyContext.get());
        ksession = runtime.getKieSession();        
        Assert.assertEquals(sessionId, ksession.getId());
        // dispose session that should not have affect on the session at all
        manager.disposeRuntimeEngine(runtime);
        
        ksession = manager.getRuntimeEngine(EmptyContext.get()).getKieSession();        
        Assert.assertEquals(sessionId, ksession.getId());
        
        // close manager which will close session maintained by the manager
        manager.close();
        
        runtime = manager.getRuntimeEngine(EmptyContext.get());
        Assert.assertNull(runtime);
    }
	
	@Test
    public void testPerRequestRuntimeManager() {
        RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault()
                .userGroupCallback(userGroupCallback)
                .addAsset(ResourceFactory.newClassPathResource("sample.bpmn"), ResourceType.BPMN2)
                .get();
        
        manager = RuntimeManagerFactory.Factory.get().newPerRequestRuntimeManager(environment);        
        Assert.assertNotNull(manager);
        
        RuntimeEngine runtime = manager.getRuntimeEngine(EmptyContext.get());
        KieSession ksession = runtime.getKieSession();
        Assert.assertNotNull(ksession);       
        
        int sessionId = ksession.getId();
        Assert.assertTrue(sessionId == 1);
        manager.disposeRuntimeEngine(runtime);
        
        runtime = manager.getRuntimeEngine(EmptyContext.get());
        ksession = runtime.getKieSession();    
        // session id should be 1+ previous session id
        Assert.assertEquals(sessionId+1, ksession.getId());
        sessionId = ksession.getId();
        manager.disposeRuntimeEngine(runtime);
        
        runtime = manager.getRuntimeEngine(EmptyContext.get());
        ksession = runtime.getKieSession();         
        // session id should be 1+ previous session id
        Assert.assertEquals(sessionId+1, ksession.getId());
        manager.disposeRuntimeEngine(runtime);       
        
        // when trying to access session after dispose 
        try {
            ksession.getId();
            Assert.fail("Should fail as session manager was closed and with that it's session");
        } catch (IllegalStateException e) {
        } catch (UndeclaredThrowableException e) {
        	Throwable rootCause = e.getCause();
            while (rootCause != null) {
                if (rootCause.getCause() != null){
                    rootCause = rootCause.getCause();
                } else {
                    break;
                }
            }
            if (!(rootCause instanceof IllegalStateException)){
                Assert.fail("Unexpected exception caught " + rootCause.getMessage());
            }
        }
    }
}
