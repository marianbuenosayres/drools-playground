package org.jbpm.form.builder.ng.server.impl;

import java.io.IOException;
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

public class ViewFormServlet extends HttpServlet {

	private static final long serialVersionUID = 3250248140464581566L;
	
	@Inject
	@Named("ioStrategy")
	IOService ioService;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
//		String uuid = req.getParameter("uuid");
//		String html = "";
//		if (uuid != null && !"".equals(uuid)) {
//			Path path = ioService.get(URI.create(FormServiceEntryPointImpl.TEMP_URL + "/" + uuid));
//			if (ioService.exists(path)) {
//				html = ioService.readAllString(path);
//			} else {
//				html = "Path with UUID " + uuid + "not generated";
//			}
//		} else {
//			html = "uuid parameter must be passed to URL";
//		}
//		resp.getWriter().println(html);
	}
}
