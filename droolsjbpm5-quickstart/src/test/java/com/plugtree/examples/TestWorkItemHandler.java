package com.plugtree.examples;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class TestWorkItemHandler implements WorkItemHandler {

	private WorkItem item;
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.item = workItem;
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.item = null;
	}
	
	public WorkItem getItem() {
		WorkItem workItem = item;
		item = null;
		return workItem;
	}

}
