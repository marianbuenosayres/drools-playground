package com.plugtree.training.example.namedkiesession;

import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import static org.junit.Assert.assertEquals;

public class NamedKieSessionExampleTest {

    @Test
    public void testNamedKieSession() {
        KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();

        KieSession kSession = kContainer.newKieSession("ksession1");
        kSession.insert("Hello named session");
        int rulesExecuted = kSession.fireAllRules();

        assertEquals(1, rulesExecuted);
    }
}
