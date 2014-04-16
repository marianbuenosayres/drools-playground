<%
	String queryString = request.getQueryString();
    String redirectURL = "org.jbpm.form.builder.ng.FormBuilderShowcase/FormBuilder.html?"+(queryString==null?"":queryString);
    response.sendRedirect(redirectURL);
%>