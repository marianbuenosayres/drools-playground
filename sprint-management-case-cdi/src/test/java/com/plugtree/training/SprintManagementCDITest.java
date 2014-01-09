package com.plugtree.training;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

import bitronix.tm.resource.jdbc.PoolingDataSource;

import com.plugtree.training.cdi.SprintMgmtApp;
import com.plugtree.training.model.Requirement;

public class SprintManagementCDITest {

	private PoolingDataSource ds;

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
    public void setUp() throws Exception {
		cleanupSingletonSessionId();
		this.ds = new PoolingDataSource();
		this.ds.setUniqueName("jdbc/testDS");
		this.ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		this.ds.setMaxPoolSize(10);
		this.ds.setAllowLocalTransactions(true);
		this.ds.getDriverProperties().setProperty("URL", "jdbc:h2:tasks;MVCC=true;DB_CLOSE_ON_EXIT=TRUE");
		this.ds.getDriverProperties().setProperty("user", "sa");
		this.ds.getDriverProperties().setProperty("password", "sasa");
		this.ds.init();
    }
	
	@After
	public void tearDown() throws Exception {
		if (this.ds != null) {
			this.ds.close();
		}
	}
	
	@Test
	public void testSmallSprintWithCDI() throws Exception {
		
        Weld w = new Weld();
        WeldContainer wc = w.initialize();

        SprintMgmtApp bean = wc.instance().select(SprintMgmtApp.class).get();
        
        bean.start();
        TaskService taskService = bean.getTaskService();
        
        Map<String, Object> params = new HashMap<String, Object>();
        List<Requirement> reqs = new ArrayList<Requirement>();
        reqs.add(new Requirement("Req 1 SIMPLE", "Do something"));
        reqs.add(new Requirement("Req 2 SIMPLE", "Do something else"));
        reqs.add(new Requirement("Req 3 URGENT", "And another thing"));
        params.put("reqs", reqs);
        ProcessInstance instance = bean.startProcess("sprintManagementProcess", params);
        //process will remain active until a reqsFinished signal is sent or a sprintClosed signal is sent
        Assert.assertNotNull(instance);
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
        
        //john is a developer, so he is potential owner of 3 tasks
        List<TaskSummary> johnTasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        Assert.assertNotNull(johnTasks);
        Assert.assertEquals(3, johnTasks.size());
        
        //lets make john claims and finishes the first task
        TaskSummary firstTask = johnTasks.iterator().next();
        taskService.claim(firstTask.getId(), "john");
        taskService.start(firstTask.getId(), "john");
        Task johnTask = taskService.getTaskById(firstTask.getId());
        Content content = taskService.getContentById(johnTask.getTaskData().getDocumentContentId());
        Map<?, ?> johnInput = (Map<?, ?>) ContentMarshallerHelper.unmarshall(content.getContent(), bean.getEnvironment());
        Requirement johnReq = (Requirement) johnInput.get("develReq");
        johnReq.setDeveloperId("john");
        Map<String, Object> johnResults = new HashMap<String, Object>();
        johnResults.put("reqResult", johnReq);
        taskService.complete(johnTask.getId(), "john", johnResults);
        
        //Then, jean and charles take the other two requirements
        List<TaskSummary> jeanTasks = taskService.getTasksAssignedAsPotentialOwner("jean", "en-UK");
        Assert.assertNotNull(jeanTasks);
        Assert.assertEquals(2, jeanTasks.size());
        
        TaskSummary secondTask = jeanTasks.iterator().next();
        taskService.claim(secondTask.getId(), "jean");
        taskService.start(secondTask.getId(), "jean");
        
        List<Status> ready = new ArrayList<Status>();
        ready.add(Status.Ready);
        List<TaskSummary> charlesTasks = taskService.getTasksAssignedAsPotentialOwnerByStatus("charles", ready, "en-UK");
        Assert.assertNotNull(charlesTasks);
        Assert.assertEquals(1, charlesTasks.size());
        
        TaskSummary thirdTask = charlesTasks.iterator().next();
        taskService.claim(thirdTask.getId(), "charles");
        taskService.start(thirdTask.getId(), "charles");
        
        //and they both modify the tasks
        Task jeanTask = taskService.getTaskById(secondTask.getId());
        Content content2 = taskService.getContentById(jeanTask.getTaskData().getDocumentContentId());
        Map<?, ?> jeanInput = (Map<?, ?>) ContentMarshallerHelper.unmarshall(content2.getContent(), bean.getEnvironment());
        Requirement jeanReq = (Requirement) jeanInput.get("develReq");
        jeanReq.setDeveloperId("jean");
        
        Task charlesTask = taskService.getTaskById(thirdTask.getId());
        Content content3 = taskService.getContentById(charlesTask.getTaskData().getDocumentContentId());
        Map<?, ?> charlesInput = (Map<?, ?>) ContentMarshallerHelper.unmarshall(content3.getContent(), bean.getEnvironment());
        Requirement charlesReq = (Requirement) charlesInput.get("develReq");
        charlesReq.setDeveloperId("charles");
        
        //and complete them
        Map<String, Object> jeanResults = new HashMap<String, Object>();
        jeanResults.put("reqResult", jeanReq);
        taskService.complete(jeanTask.getId(), "jean", jeanResults);

        Map<String, Object> charlesResults = new HashMap<String, Object>();
        charlesResults.put("reqResult", charlesReq);
        taskService.complete(charlesTask.getId(), "charles", charlesResults);

        //by now compilation and deployment are done for all three tasks, so testers like mary can see them
        List<TaskSummary> maryTasks = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
        Assert.assertNotNull(maryTasks);
        Assert.assertEquals(3, maryTasks.size());

        TaskSummary testTask1 = maryTasks.get(0);
        TaskSummary testTask2 = maryTasks.get(1);
        TaskSummary testTask3 = maryTasks.get(2);
        //mary takes two tasks and mark takes one
        taskService.claim(testTask1.getId(), "mary");
        taskService.claim(testTask2.getId(), "mary");
        taskService.claim(testTask3.getId(), "mark");
        
        //mary finds no bugs in the first task
        taskService.start(testTask1.getId(), "mary");
        Task maryTask1 = taskService.getTaskById(testTask1.getId());
        Content testContent1 = taskService.getContentById(maryTask1.getTaskData().getDocumentContentId());
        Map<?, ?> maryInput1 = (Map<?, ?>) ContentMarshallerHelper.unmarshall(testContent1.getContent(), bean.getEnvironment());
        Requirement maryReq1 = (Requirement) maryInput1.get("testReq");
        maryReq1.setTesterId("mary");
        maryReq1.setTested(true);
        Map<String, Object> testResults1 = new HashMap<String, Object>();
        testResults1.put("reqResult", maryReq1);
        taskService.complete(maryTask1.getId(), "mary", testResults1);
        
        //mark finds two bugs in the third task
        taskService.start(testTask3.getId(), "mark");
        Task markTask = taskService.getTaskById(testTask3.getId());
        Content testContent3 = taskService.getContentById(markTask.getTaskData().getDocumentContentId());
        Map<?, ?> markInput = (Map<?, ?>) ContentMarshallerHelper.unmarshall(testContent3.getContent(), bean.getEnvironment());
        Requirement markReq = (Requirement) markInput.get("testReq");
        markReq.addBug("Doesnt look good");
        markReq.addBug("Finds twice the same data");
        markReq.setTested(true);
        Map<String, Object> testResults3 = new HashMap<String, Object>();
        testResults3.put("reqResult", markReq);
        taskService.complete(markTask.getId(), "mark", testResults3);
        
        //mary finds a bug in the second task
        taskService.start(testTask2.getId(), "mary");
        Task maryTask2 = taskService.getTaskById(testTask2.getId());
        Content testContent2 = taskService.getContentById(maryTask2.getTaskData().getDocumentContentId());
        Map<?, ?> maryInput2 = (Map<?, ?>) ContentMarshallerHelper.unmarshall(testContent2.getContent(), bean.getEnvironment());
        Requirement maryReq2 = (Requirement) maryInput2.get("testReq");
        maryReq2.setTesterId("mary");
        maryReq2.addBug("Wont load");
        maryReq2.setTested(true);
        Map<String, Object> testResults2 = new HashMap<String, Object>();
        testResults2.put("reqResult", maryReq2);
        taskService.complete(maryTask2.getId(), "mary", testResults2);
        
        //Developers have 2 tasks to bugfix
        List<TaskSummary> bugfixTasks = taskService.getTasksAssignedAsPotentialOwner("jean", "en-UK");
        Assert.assertNotNull(bugfixTasks);
        Assert.assertEquals(2, bugfixTasks.size());
        
        //john is out on leave so jean and charles take one bugfix each
        TaskSummary bugTask1 = bugfixTasks.get(0);
        TaskSummary bugTask2 = bugfixTasks.get(1);
        taskService.claim(bugTask1.getId(), "jean");
        taskService.claim(bugTask2.getId(), "charles");
        
        //first task has two bugs, mary dismisses one and solves another
        taskService.start(bugTask1.getId(), "jean");
        Task jeanBugTask = taskService.getTaskById(bugTask1.getId());
        Content bugContent1 = taskService.getContentById(jeanBugTask.getTaskData().getDocumentContentId());
        Map<?, ?> jeanBugInput = (Map<?, ?>) ContentMarshallerHelper.unmarshall(bugContent1.getContent(), bean.getEnvironment());
        Requirement jeanBug = (Requirement) jeanBugInput.get("bugfixReq");
        jeanBug.resolveBug("Doesnt look good", "'Dont use IE6' by mary");
        jeanBug.resolveBug("Finds twice the same data", "'Solved by' mary");
        jeanBug.setTested(false);
        Map<String, Object> bugfixResults1 = new HashMap<String, Object>();
        bugfixResults1.put("reqResult", jeanBug);
        taskService.complete(jeanBugTask.getId(), "jean", bugfixResults1);
        
        //seconds task has one bugs, charles solves it
        taskService.start(bugTask2.getId(), "charles");
        Task charlesBugTask = taskService.getTaskById(bugTask2.getId());
        Content bugContent2 = taskService.getContentById(charlesBugTask.getTaskData().getDocumentContentId());
        Map<?, ?> charlesBugInput = (Map<?, ?>) ContentMarshallerHelper.unmarshall(bugContent2.getContent(), bean.getEnvironment());
        Requirement charlesBug = (Requirement) charlesBugInput.get("bugfixReq");
        charlesBug.resolveBug("Wont load", "'Solved by' charles");
        charlesBug.setTested(false);
        Map<String, Object> bugfixResults2 = new HashMap<String, Object>();
        bugfixResults2.put("reqResult", charlesBug);
        taskService.complete(charlesBugTask.getId(), "charles", bugfixResults2);
        
        //Now we're back at testing the two corrected tasks
        List<TaskSummary> fixedTasks = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
        Assert.assertNotNull(fixedTasks);
        Assert.assertEquals(2, fixedTasks.size());
        
        //mary takes one and mark another
        TaskSummary fixTask1 = fixedTasks.get(0);
        TaskSummary fixTask2 = fixedTasks.get(1);
        taskService.claim(fixTask1.getId(), "mary");
        taskService.claim(fixTask2.getId(), "mark");
        taskService.start(fixTask1.getId(), "mary");
        taskService.start(fixTask2.getId(), "mark");
        
        //They don't find any bugs in either requirement
        Task maryFixTask = taskService.getTaskById(fixTask1.getId());
        Content fixContent1 = taskService.getContentById(maryFixTask.getTaskData().getDocumentContentId());
        Map<?, ?> fixInput1 = (Map<?, ?>) ContentMarshallerHelper.unmarshall(fixContent1.getContent(), bean.getEnvironment());
        Requirement fixReq1 = (Requirement) fixInput1.get("testReq");
        fixReq1.setTested(true);
        fixReq1.setBugs(new ArrayList<String>());
        
        Task markFixTask = taskService.getTaskById(fixTask2.getId());
        Content fixContent2 = taskService.getContentById(markFixTask.getTaskData().getDocumentContentId());
        Map<?, ?> fixInput2 = (Map<?, ?>) ContentMarshallerHelper.unmarshall(fixContent2.getContent(), bean.getEnvironment());
        Requirement fixReq2 = (Requirement) fixInput2.get("testReq");
        fixReq2.setTested(true);
        fixReq2.setBugs(new ArrayList<String>());
        
        Map<String, Object> fixTestResults1 = new HashMap<String, Object>();
        fixTestResults1.put("reqResult", fixReq1);
        
        Map<String, Object> fixTestResults2 = new HashMap<String, Object>();
        fixTestResults2.put("reqResult", fixReq2);
        
        //they complete the testing task
        taskService.complete(fixTask1.getId(), "mary", fixTestResults1);
        taskService.complete(fixTask2.getId(), "mark", fixTestResults2);
        
        //After finishing all 3 requirements, sprint is completed, so sprintManagementProcess process should be completed as well
        ProcessInstance reloadedInstance = bean.getProcessInstance(instance.getId());
		Assert.assertNull(reloadedInstance);
        w.shutdown();
	}
}
