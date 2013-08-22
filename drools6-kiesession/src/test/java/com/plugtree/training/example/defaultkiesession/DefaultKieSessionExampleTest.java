package com.plugtree.training.example.defaultkiesession;

import java.io.IOException;
import java.io.File;

import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import static org.junit.Assert.assertEquals;

public class DefaultKieSessionExampleTest {

    @Test
    public void testKieSession() {
        KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();

        KieSession kSession = kContainer.newKieSession();
        kSession.insert("Hello");
        int rulesExecuted = kSession.fireAllRules();

        assertEquals(1, rulesExecuted);
    }

    @Test
    public void testKieSessionFromFile() {
        String currentFolder = null;
        try {
            currentFolder = new File(".").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File rootFolder = null;
        if (currentFolder.endsWith("drools-playground")) {
            rootFolder = new File("drools6-kiesession");
        } else {
            rootFolder = new File(".");
        }

        KieServices ks = KieServices.Factory.get();
        KieBuilder kieBuilder = ks.newKieBuilder(rootFolder).buildAll();

        KieSession kSession = ks.newKieContainer(kieBuilder.getKieModule().getReleaseId()).newKieSession();
        kSession.insert("Hello Again");
        int rulesExecuted = kSession.fireAllRules();

        assertEquals(1, rulesExecuted);

    }
}
