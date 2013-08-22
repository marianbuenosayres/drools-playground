package com.plugtree.training.example.cdiexample;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;

import javax.inject.Inject;

public class CDIExample {

    @Inject
    @KSession("ksession1")
    KieSession kSession;

    public int execute() {
        kSession.insert("Hello CDI Example!!");
        return kSession.fireAllRules();
    }

}
