package com.plugtree.training.handler;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class HumanTaskMockHandler implements WorkItemHandler {

    private WorkItemManager workItemManager;
    private Long workItemId;

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        this.workItemId = workItem.getId();
        this.workItemManager = manager;
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    }

    public void completeWorkItem() {
        this.workItemManager.completeWorkItem(workItemId, null);
    }

}
