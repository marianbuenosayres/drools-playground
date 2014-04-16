package org.jbpm.form.builder.ng.server.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jbpm.form.builder.ng.server.fb.FormServiceEntryPointImpl;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Path;

public class ImportFileServlet extends HttpServlet {

	private static final long serialVersionUID = 3250248130464581566L;
	private static final Log log = LogFactory.getLog(ImportFileServlet.class);
	
	private ServletFileUpload upload;
	
	@Inject
	@Named("ioStrategy")
	IOService ioService;

	@Override
	public void init() throws ServletException {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		
		// Configure a repository (to ensure a secure temp location is used)
		ServletContext servletContext = getServletConfig().getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);

		// Create a new file upload handler
		this.upload = new ServletFileUpload(factory);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
//		if (ServletFileUpload.isMultipartContent(req)) {
//			// Parse the request
//			try {
//				List<?> items = upload.parseRequest(req);
//				if (items.size() > 0) {
//					Object obj = items.iterator().next();
//					FileItem item = (FileItem) obj;
//					Path path = ioService.get(URI.create(FormServiceEntryPointImpl.FORMS_URL + "/" + item.getName()));
//					if (!ioService.exists(path)) {
//						ioService.createFile(path);
//					}
//					ioService.write(path, item.get());
//				}
//			} catch (FileUploadException e) {
//				log.error("Problem importing file", e);
//				throw new IOException("Problem importing file", e);
//			}
//		}
	}
}
