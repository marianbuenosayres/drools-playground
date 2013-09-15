package com.plugtree.training.commands;

import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.executor.api.Command;
import org.kie.internal.executor.api.CommandContext;
import org.kie.internal.executor.api.ExecutionResults;

import com.plugtree.training.model.Requirement;

public class CompilationCommand implements Command {

	@Override
	public ExecutionResults execute(CommandContext context) throws Exception {
		WorkItem item = (WorkItem) context.getData("workItem");
		Requirement req = (Requirement) item.getParameter("compileReq");
		System.out.println("Compiling the process...");
		req.setCompiled(true);
		System.out.println("Compilation done");;
		ExecutionResults results = new ExecutionResults();
		results.setData("reqResult", req);
		return results;
	}
}
