package com.plugtree.training.example.cdiexample;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CDIExampleTest {

    @Test
    public void testGo() {
        Weld w = new Weld();
        WeldContainer wc = w.initialize();

        CDIExample bean = wc.instance().select(CDIExample.class).get();
        int rulesExecuted = bean.execute();

        assertEquals(1, rulesExecuted);

        w.shutdown();
    }
}
