package com.plugtree.training.commands;

import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.executor.api.Command;
import org.kie.internal.executor.api.CommandContext;
import org.kie.internal.executor.api.ExecutionResults;

import com.plugtree.training.model.Requirement;

public class DeploymentCommand implements Command {

	@Override
	public ExecutionResults execute(CommandContext context) throws Exception {
		WorkItem item = (WorkItem) context.getData("workItem");
		Requirement req = (Requirement) item.getParameter("deployReq");
		System.out.println("Deploying the process...");
		req.setDeployed(true);
		System.out.println("Deployment done");;
		ExecutionResults results = new ExecutionResults();
		results.setData("reqResult", req);
		return results;
	}
}
