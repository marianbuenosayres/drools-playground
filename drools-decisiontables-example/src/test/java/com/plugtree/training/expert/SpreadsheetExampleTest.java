package com.plugtree.training.expert;

import org.drools.decisiontable.InputType;
import org.drools.decisiontable.SpreadsheetCompiler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import com.plugtree.training.enums.CreditStatus;
import com.plugtree.training.model.Job;
import com.plugtree.training.model.Person;

public class SpreadsheetExampleTest  {

    private KieSession ksession;

    @Before
    public void setUp() throws Exception {
    	KieServices ks = KieServices.Factory.get();
    	
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/Credit-Rules.xls", ResourceFactory.newClassPathResource("rules/CreditRules.xls"));

        //See the generated rules from the spreadsheet
        SpreadsheetCompiler compiler = new SpreadsheetCompiler();
        String drl = compiler.compile("/rules/CreditRules.xls", InputType.XLS);
        System.out.println("DRL String:"+drl);

        KieBuilder kbuilder = ks.newKieBuilder(kfs);
        System.out.println("Compiling rules");
        kbuilder.buildAll();
        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            System.err.println(kbuilder.getResults());
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        
        KieContainer kcontainer = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
        ksession = kcontainer.newKieSession();
        //KieRuntimeLogger logger = ks.getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);
    }
    @Test
    public void testExecution() {
        Person p1 = new Person(20);
        p1.setJob(new Job(2, 3000f));
        Person p2 = new Person(18);
        p2.setJob(new Job(0, 5000f));
        Person p3 = new Person(47);
        p3.setJob(new Job(10, 4500f));
        ksession.insert(p1);
        ksession.insert(p2);
        ksession.insert(p3);
        ksession.fireAllRules();

        Assert.assertEquals(CreditStatus.ACCEPTED, p1.getCredit().getStatus());
        Assert.assertEquals(CreditStatus.REJECTED, p2.getCredit().getStatus());
        Assert.assertEquals(CreditStatus.REJECTED, p2.getCredit().getStatus());
        
        ksession.dispose();

    }
}
