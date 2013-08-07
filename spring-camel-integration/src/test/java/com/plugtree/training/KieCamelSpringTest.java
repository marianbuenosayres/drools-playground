package com.plugtree.training;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.junit.Test;
import org.kie.spring.InternalKieSpringUtils;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

import com.plugtree.training.model.Person;

public class KieCamelSpringTest extends CamelSpringTestSupport {

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return (AbstractXmlApplicationContext) InternalKieSpringUtils.getSpringContext(
				new ReleaseIdImpl("com.plugtree.training", "spring-camel-integration", "0001"), 
				getClass().getResource("/cxf-rs-spring.xml"));
	}

	@Test
	public void testInvocation() throws Exception {
		String cmd = "";
        cmd += "<batch-execution lookup=\"ksession1\">\n";
        cmd += "  <insert out-identifier=\"mariano\">\n";
        cmd += "      <com.plugtree.training.model.Person>\n";
        cmd += "         <name>john</name>\n";
        cmd += "      </com.plugtree.training.model.Person>\n";
        cmd += "   </insert>\n";
        cmd += "   <fire-all-rules/>\n";
        cmd += "</batch-execution>\n";

        Exchange exchange = this.createExchangeWithBody(cmd);
        
        ProducerTemplate producer = this.context.createProducerTemplate();
        producer.setDefaultEndpointUri("direct://http");
        producer.send(exchange);
        
        Object obj = this.applicationContext.getBean("myPeopleList");
        assertNotNull(obj);
        assertTrue(obj instanceof List);
        List<?> list = (List<?>) obj;
        assertFalse(list.isEmpty());
        assertEquals(list.size(), 1);
        Object subObj = list.iterator().next();
        assertNotNull(subObj);
        assertTrue(subObj instanceof Person);
        Person person = (Person) subObj;
        assertTrue(person.getName().equals("john"));
	}
	
}
