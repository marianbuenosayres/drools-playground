<%
    String queryString = request.getQueryString();
    String redirectURL = request.getContextPath()  +"/com.plugtree.training.showcase.UberfireDemoShowcase/demo.html?"+(queryString==null?"":queryString);
    response.sendRedirect(redirectURL);
%>
