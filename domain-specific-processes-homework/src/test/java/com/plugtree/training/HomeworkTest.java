package com.plugtree.training;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;

public class HomeworkTest {

    private KieSession session;

    @Before
    public void setUp() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieContainer kc = ks.getKieClasspathContainer();
        this.session = kc.newKieSession("ksession1");
    }

    @After
    public void tearDown() {
    	this.session.dispose();
    }

    private void initSession(WorkItemHandler handlerGet, 
    		WorkItemHandler handlerBuy, WorkItemHandler handlerSell) {
        //register the same handler for all the Work Items present in the process.
    	if (handlerGet != null) {
    		this.session.getWorkItemManager().registerWorkItemHandler("GetShareValue", handlerGet);
    	}
    	if (handlerBuy != null) {
    		this.session.getWorkItemManager().registerWorkItemHandler("BuyShare", handlerBuy);
    	}
    	if (handlerSell != null) {
    		this.session.getWorkItemManager().registerWorkItemHandler("SellShare", handlerSell);
    	}
    }
    
    @Test
    public void doTestBuyPath() {
    	CommandDoneHandler handlerGet = new CommandDoneHandler();
    	CommandDoneHandler handlerBuy = new CommandDoneHandler();
    	CommandDoneHandler handlerSell = new CommandDoneHandler();
    	initSession(handlerGet, handlerBuy, handlerSell);
        
        //Start the process using its ID
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) session.startProcess("homework");
        
        //**************   Get Share Value   **************//
        //The process must be in the 'Get Share Value' task. Let's check the
        //input parameters received by the handler associated to that task.
        Assert.assertEquals("Get Share Value", processInstance.getNodeInstances().iterator().next().getNodeName());
        WorkItem item1 = handlerGet.getLastItem();
        Assert.assertNotNull(item1);
        
        //let's complete the task emulating the results of this task.
        Map<String,Object> taskResults = new HashMap<String, Object>();
        taskResults.put("value", 5);
		session.getWorkItemManager().completeWorkItem(item1.getId(), taskResults);
        
        //**************   Buy Share   **************//
        //Now we are at 'Buy Share' task. Let's check that the process
        //variable 'shareValue' has the value assigned from the previous task
        //and that we are at the proper task
        Assert.assertEquals(5, processInstance.getVariable("shareValue"));
        Assert.assertEquals("Buy Share", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        //let's complete the task (no results needed)
        WorkItem item2 = handlerBuy.getLastItem();
        Assert.assertNotNull(item2);
		session.getWorkItemManager().completeWorkItem(item2.getId(), null);
        
        //The process should be completed now. Let's check the state
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }
    
    @Test
    public void doTestSellPath() {
    	CommandDoneHandler handlerGet = new CommandDoneHandler();
    	CommandDoneHandler handlerBuy = new CommandDoneHandler();
    	CommandDoneHandler handlerSell = new CommandDoneHandler();
    	initSession(handlerGet, handlerBuy, handlerSell);
        
        //Start the process using its ID
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) session.startProcess("homework");
        
        //**************   Get Share Value   **************//
        //The process must be in the 'Get Share Value' task. Let's check the
        //input parameters received by the handler associated to that task.
        Assert.assertEquals("Get Share Value", processInstance.getNodeInstances().iterator().next().getNodeName());
        WorkItem item1 = handlerGet.getLastItem();
        Assert.assertNotNull(item1);
        
        //let's complete the task emulating the results of this task.
        Map<String,Object> taskResults = new HashMap<String, Object>();
        taskResults.put("value", 15);
		session.getWorkItemManager().completeWorkItem(item1.getId(), taskResults);
        
        //**************   Sell Share   **************//
        //Now we are at 'Sell Share' task. Let's check that the process
        //variable 'shareValue' has the value assigned from the previous task
        //and that we are at the proper task
        Assert.assertEquals(15, processInstance.getVariable("shareValue"));
        Assert.assertEquals("Sell Share", processInstance.getNodeInstances().iterator().next().getNodeName());
        
        //let's complete the task (no results needed)
        WorkItem item2 = handlerSell.getLastItem();
        Assert.assertNotNull(item2);
		session.getWorkItemManager().completeWorkItem(item2.getId(), null);
        
        //The process should be completed now. Let's check the state
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }
    
    @Test
    public void doTestNotifyPath() {
    	CommandDoneHandler handlerGet = new CommandDoneHandler();
    	//since it should go to the send alert task, we won't need to initialize
    	//the buy or sell share handlers. If it throws a WorkItemNotFoundException
    	//we know that we went a path we shouldn't have gone to 
    	initSession(handlerGet, null, null);
        
        //Start the process using its ID
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance) session.startProcess("homework");
        
        //**************   Get Share Value   **************//
        //The process must be in the 'Get Share Value' task. Let's check the
        //input parameters received by the handler associated to that task.
        Assert.assertEquals("Get Share Value", processInstance.getNodeInstances().iterator().next().getNodeName());
        WorkItem item1 = handlerGet.getLastItem();
        Assert.assertNotNull(item1);
        
        //let's complete the task emulating the results of this task.
        Map<String,Object> taskResults = new HashMap<String, Object>();
        taskResults.put("value", 30);
		session.getWorkItemManager().completeWorkItem(item1.getId(), taskResults);
        
        //**************   Send Alert   **************//
        //Now we must have completed the process through the 'Send Alert' script task
        //The variable 'shareValue' has the value assigned from the previous task
        Assert.assertEquals(30, processInstance.getVariable("shareValue"));
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }
}
