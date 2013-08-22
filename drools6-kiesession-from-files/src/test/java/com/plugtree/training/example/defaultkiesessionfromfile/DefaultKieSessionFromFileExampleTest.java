package com.plugtree.training.example.defaultkiesessionfromfile;

import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class DefaultKieSessionFromFileExampleTest {

    @Test
    public void testKieSessionFromExternalProject() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieRepository kr = ks.getRepository();

        KieModule kModule = kr.addKieModule(ks.getResources().newFileSystemResource(getFile("drools6-kiesession")));
        KieContainer kContainer = ks.newKieContainer(kModule.getReleaseId());
        KieSession kSession = kContainer.newKieSession();

        kSession.insert("Hello for the third time?");
        int rulesExecuted = kSession.fireAllRules();

        assertEquals(1, rulesExecuted);
    }

    public static File getFile(String exampleName) throws Exception {
        File folder = new File(new File(".").getCanonicalPath());
        File exampleFolder = null;
        while (folder != null) {
            exampleFolder = new File(folder, exampleName);
            if (exampleFolder.exists()) {
                break;
            }
            exampleFolder = null;
            folder = folder.getParentFile();
        }

        if (exampleFolder != null) {
            File targetFolder = new File(exampleFolder, "target");
            if (!targetFolder.exists()) {
                throw new RuntimeException("The target folder does not exist, please build project " + exampleName + " first");
            }

            for (String str : targetFolder.list()) {
                if (str.startsWith(exampleName) && !str.endsWith("-sources.jar") && !str.endsWith("-tests.jar")) {
                    return new File(targetFolder, str);
                }
            }
        }

        throw new RuntimeException("The target jar does not exist, please build project " + exampleName + " first");
    }

}
