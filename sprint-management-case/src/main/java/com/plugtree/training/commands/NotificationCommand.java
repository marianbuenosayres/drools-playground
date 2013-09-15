package com.plugtree.training.commands;

import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.executor.api.Command;
import org.kie.internal.executor.api.CommandContext;
import org.kie.internal.executor.api.ExecutionResults;

import com.plugtree.training.model.Requirement;

public class NotificationCommand implements Command {

	@Override
	public ExecutionResults execute(CommandContext context) throws Exception {
		System.out.println(">>>Notification of failure on requirement");
		WorkItem item = (WorkItem) context.getData("workItem");
		Requirement req = (Requirement) item.getParameter("notifyReq");
		req.setCompiled(false);
		req.setTested(false);
		System.out.println(">>>>" + req);
		ExecutionResults results = new ExecutionResults();
		results.setData("reqResult", req);
		return results;
	}

}
