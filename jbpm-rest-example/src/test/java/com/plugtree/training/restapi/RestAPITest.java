package com.plugtree.training.restapi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteRestRuntimeFactory;
import org.kie.services.client.api.command.RemoteConfiguration;

@RunWith(Arquillian.class)
public class RestAPITest {

    @Deployment(testable=false)
    public static WebArchive createDeployment() throws IOException {
    	String s = File.separator;
    	String warFile = System.getProperty("user.home") + s + ".m2" + 
    		s + "repository" + s + "org" + s + "kie" + s + "kie-wb-distribution-wars" +
    		s + "6.0.1.Final" + s + "kie-wb-distribution-wars-6.0.1.Final-jboss-as7.war";
    	File destFile = new File(System.getProperty("java.io.tmpdir") + s + "kie-wb.war");
    	destFile.delete();
    	FileUtils.copyFile(new File(warFile), destFile);
    	WebArchive archive = ShrinkWrap.createFromZipFile(WebArchive.class, destFile);
    	return archive;
    }

    @Test
    @RunAsClient
    public void testRestAPI() throws Exception {

    	//Deploy the Kie JARs needed for the REST API objects
    	System.out.println(">>> About to deploy org.jbpm:HR:1.0 project");
    	ClientRequestFactory factory = RemoteConfiguration.
    		createAuthenticatingRequestFactory(
    			new URL("http://localhost:8080/kie-wb"), 
    			"krisv", "mypass", 5);
    	String baseRelUrl = "/rest/repositories/jbpm-playground/projects/HR/maven/";
    	ClientRequest request1 = factory.createRelativeRequest(baseRelUrl + "compile");
    	ClientResponse<?> response1 = request1.post();
    	Assert.assertEquals(Status.OK, response1.getResponseStatus());
    	System.out.println(">>> Project org.jbpm:HR:1.0 compiled");
    	ClientRequest request2 = factory.createRelativeRequest(baseRelUrl + "install");
    	ClientResponse<?> response2 = request2.post();
    	Assert.assertEquals(Status.OK, response2.getResponseStatus());
    	System.out.println(">>> Project org.jbpm:HR:1.0 installed");
    	ClientRequest request3 = factory.createRelativeRequest(baseRelUrl + "deploy");
    	ClientResponse<?> response3 = request3.post();
    	Assert.assertEquals(Status.OK, response3.getResponseStatus());
    	System.out.println(">>> Project org.jbpm:HR:1.0 deployed");
    	
    	//TODO deployment through REST API is not working. Code remains as demonstration
    	
        RuntimeEngine engine = new RemoteRestRuntimeFactory("org.jbpm:HR:1.0", 
                new URL("http://localhost:8080/kie-wb"),
                "krisv", "mypass")
            .newRuntimeEngine();
        engine.getKieSession().startProcess("org.jbpm.humantask");
        
        List<TaskSummary> tasks = engine.getTaskService().getTasksAssignedAsPotentialOwner("krisv", "en-UK");
        TaskSummary firstTask = tasks.iterator().next();
        engine.getTaskService().claim(firstTask.getId(), "krisv");
        engine.getTaskService().start(firstTask.getId(), "krisv");
        engine.getTaskService().complete(firstTask.getId(), "krisv", null);
        //TODO continue?? point is proven up to here
    }
}
