package com.plugtree.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.process.WorkflowProcessInstance;

import com.plugtree.training.model.Requirement;

public class ReqsCompletedListener extends DefaultProcessEventListener {

	private final Map<Long, List<Requirement>> reqMap = new HashMap<Long, List<Requirement>>();
	
	public ReqsCompletedListener() {
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		System.out.println(">>>>Process started: " + event.getProcessInstance().getProcessId() + " " + event.getProcessInstance().getId());
		WorkflowProcessInstance instance = (WorkflowProcessInstance) event.getProcessInstance();
		long instanceId = instance.getId();
		String process = instance.getProcessId();
		if ("sprintManagementProcess".equals(process)) {
			List<Requirement> reqs = (List<Requirement>) instance.getVariable("reqs");
			reqMap.put(instanceId, new ArrayList<Requirement>(reqs));
		}
	}
	
	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		WorkflowProcessInstance instance = (WorkflowProcessInstance) event.getProcessInstance();
		Requirement req = (Requirement) instance.getVariable("req");
		synchronized (this) {
			if (req != null) {
				for (Map.Entry<Long,List<Requirement>> entry: reqMap.entrySet()) {
					List<Requirement> possibleReqs = entry.getValue();
					Requirement flag = null;
					for (Requirement subReq : possibleReqs) {
						if (subReq.getId() == req.getId()) {
							flag = subReq;
							break;
						}
					}
					if (flag != null) {
						possibleReqs.remove(flag);
						reqMap.put(entry.getKey(), possibleReqs);
						if (possibleReqs.isEmpty()) {
							event.getKieRuntime().signalEvent("reqsFinished", new Object());
						}
					}
				}
			}
		}
	}
}
