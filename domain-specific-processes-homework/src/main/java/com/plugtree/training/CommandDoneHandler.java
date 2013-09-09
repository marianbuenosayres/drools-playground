package com.plugtree.training;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class CommandDoneHandler implements WorkItemHandler {

	private WorkItem lastItem;

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.lastItem = workItem;
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.lastItem = null;
	}
	
	public WorkItem getLastItem() {
		WorkItem result = lastItem;
		lastItem = null;
		return result;
	}
}
