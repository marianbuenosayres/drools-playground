<%
  request.getSession().invalidate();
  String redirectURL = request.getContextPath()  +"/org.jbpm.form.builder.ng.FormBuilderShowcase/FormBuilder.html?message=Login failed: Not Authorized";
  response.sendRedirect(redirectURL);
%>
