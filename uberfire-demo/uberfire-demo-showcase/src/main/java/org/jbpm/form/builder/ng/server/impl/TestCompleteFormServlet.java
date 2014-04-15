package org.jbpm.form.builder.ng.server.impl;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class TestCompleteFormServlet extends HttpServlet {

	private static final long serialVersionUID = 3250248140464581566L;
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><h3>Method: ").append(req.getMethod()).append("</h3>");
		sb.append("<html><body><h3>Parameters:</h3>");
		sb.append("<table><tbody>");
		Enumeration<String> e1 = req.getParameterNames();
		while(e1.hasMoreElements()) {
			String key = e1.nextElement();
			String value = req.getParameter(key);
			sb.append("<tr><td>").append(key).append("</td><td>").append(value).append("</td></tr>");
		}
		sb.append("</tbody></table>");
		Enumeration<String> e2 = req.getAttributeNames();
		sb.append("<h3>Attributes:</h3>");
		sb.append("<table><tbody>");
		while(e2.hasMoreElements()) {
			String key = e2.nextElement();
			Object value = req.getAttribute(key);
			sb.append("<tr><td>").append(key).append("</td><td>").append(value).append("</td></tr>");
		}
		sb.append("</tbody></table></body></html>");
		IOUtils.write(sb.toString(), resp.getOutputStream());
	}
}
