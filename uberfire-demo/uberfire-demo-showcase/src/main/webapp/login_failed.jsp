<%
  request.getSession().invalidate();
  String redirectURL = request.getContextPath()  +"/org.jbpm.form.builder.ng.FormBuilderShowcase/FormBuilder.html?message=Login failed: Invalid UserName or Password";
  response.sendRedirect(redirectURL);
%>
