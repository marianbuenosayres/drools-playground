package com.plugtree.training;

import org.kie.internal.executor.api.Command;
import org.kie.internal.executor.api.CommandContext;
import org.kie.internal.executor.api.ExecutionResults;

public class NotifySystemCommand implements Command {

	@Override
	public ExecutionResults execute(CommandContext context) throws Exception {
		System.out.println("Executing NotifySystemCommand...");
		ExecutionResults results = new ExecutionResults();
		results.setData("notified", "true");
		context.setData("deploymentId", "testDeploymentId");
		System.out.println("NotifySystemCommand executed");
		return results;
	}

}
