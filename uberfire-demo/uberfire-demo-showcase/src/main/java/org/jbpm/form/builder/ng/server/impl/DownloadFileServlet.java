package org.jbpm.form.builder.ng.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.jbpm.form.builder.ng.server.fb.FormServiceEntryPointImpl;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Path;

public class DownloadFileServlet extends HttpServlet {

	private static final long serialVersionUID = 3250248140464581566L;
	
	//private static final String FORMS_URL = FormServiceEntryPointImpl.FORMS_URL;
	
	@Inject
	@Named("ioStrategy")
	IOService ioService;
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
//		String fileName = req.getParameter("fileName");
//		Path path = ioService.get(URI.create(FORMS_URL + "/" + fileName));
//		byte[] content = ioService.readAllBytes(path);
//        // Make sure to show the download dialog
//		resp.setContentType("application/octet-stream");
//        resp.setHeader("Content-disposition","attachment; filename=" + fileName);
//		OutputStream out = resp.getOutputStream();
//		out.write(content);
//		out.flush();
	}
}
