package com.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import org.drools.io.ResourceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import com.plugtree.training.enums.CustomerType;
import com.plugtree.training.enums.ShippingType;
import com.plugtree.training.model.Customer;
import com.plugtree.training.model.Order;

public class DSLExampleTest {

	private KieSession ksession;
	private List<Order> rejectedNational = new ArrayList<Order>();
	private List<Order> rejectedInternational = new ArrayList<Order>();
	private List<Order> priorityCustomer = new ArrayList<Order>();
    
	@Before
	public void setUp() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/custom-lang.dsl", ResourceFactory.newClassPathResource("rules/CustomLanguage.dsl"));
        kfs.write("src/main/resources/rules.dslr", ResourceFactory.newClassPathResource("rules/Rules.dslr"));
        KieBuilder kbuilder = ks.newKieBuilder(kfs);
        System.out.println("Compiling resources");
        kbuilder.buildAll();
		if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
			System.err.println(kbuilder.getResults());
			throw new IllegalArgumentException("Could not parse knowledge.");
		}
		KieContainer kcontainer = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
		ksession = kcontainer.newKieSession();
		// KieRuntimeLogger logger = ks.getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);
	}
	
    @Test
	public void testExecution() {

		Customer johnInternational = new Customer("John Z", CustomerType.INTERNATIONAL);
		Customer annaInternational = new Customer("Anna M", CustomerType.INTERNATIONAL);
		Customer maryInternational = new Customer("Mary C", CustomerType.NATIONAL);
		Customer jesseNational = new Customer("Jessy D", CustomerType.NATIONAL);

		Order internationalExpressOrder = new Order();
		internationalExpressOrder.setCustomer(johnInternational);
		internationalExpressOrder.setShipping(ShippingType.EXPRESS);
		internationalExpressOrder.setAmount(100);

		Order internationalUSPSOrderRejected = new Order();
		internationalUSPSOrderRejected.setCustomer(annaInternational);
		internationalUSPSOrderRejected.setShipping(ShippingType.USPS);
		internationalUSPSOrderRejected.setAmount(10);

		Order internationalUSPSOrderAccepted = new Order();
		internationalUSPSOrderAccepted.setCustomer(johnInternational);
		internationalUSPSOrderAccepted.setShipping(ShippingType.USPS);
		internationalUSPSOrderAccepted.setAmount(110);

		Order nationalStandardOrder = new Order();
		nationalStandardOrder.setCustomer(maryInternational);
		nationalStandardOrder.setShipping(ShippingType.STANDARD);
		nationalStandardOrder.setAmount(90.7f);

		Order nationalExpressOrder = new Order();
		nationalExpressOrder.setCustomer(jesseNational);
		nationalExpressOrder.setShipping(ShippingType.EXPRESS);
		nationalExpressOrder.setAmount(930);

		ksession.setGlobal("rejectedNational", rejectedNational);
		ksession.setGlobal("rejectedInternational", rejectedInternational);
		ksession.setGlobal("priorityCustomer", priorityCustomer);

		ksession.insert(internationalExpressOrder);
		ksession.insert(internationalUSPSOrderRejected);
		ksession.insert(internationalUSPSOrderAccepted);
		ksession.insert(nationalStandardOrder);
		ksession.insert(nationalExpressOrder);
		ksession.fireAllRules();

		Assert.assertEquals(1, rejectedNational.size());
		Assert.assertEquals(1, rejectedInternational.size());
		Assert.assertEquals(2, priorityCustomer.size());

	}

}
