<%
  request.getSession().invalidate();
  String redirectURL = request.getContextPath()  +"/com.plugtree.training.showcase.UberfireDemoShowcase/demo.html?message=Login failed: Not Authorized";
  response.sendRedirect(redirectURL);
%>
