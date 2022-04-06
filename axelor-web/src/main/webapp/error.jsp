<%--

    Axelor Business Solutions

    Copyright (C) 2005-2022 Axelor (<http://axelor.com>).

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="x" uri="WEB-INF/axelor.tld" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" session="false" isErrorPage="true"%>
<%@ page import="java.util.function.Function"%>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="com.axelor.auth.pac4j.ClientListProvider" %>
<%@ page import="com.axelor.common.HtmlUtils" %>
<%@ page import="com.axelor.common.StringUtils"%>
<%@ page import="com.axelor.web.internal.AppInfo" %>
<%@ page import="com.axelor.i18n.I18n" %>
<%@ page import="com.axelor.inject.Beans" %>
<%@ page import="com.axelor.common.HtmlUtils" %>
<%@ page import="org.apache.shiro.authc.UnknownAccountException" %>
<%@ page import="org.pac4j.core.util.CommonHelper" %>
<%
String errorMsg;

if (exception instanceof UnknownAccountException) {
  errorMsg = /*$$(*/ "User doesn’t exist." /*)*/;
} else {
  errorMsg = /*$$(*/ "Authentication error" /*)*/;
}

if (!Beans.get(ClientListProvider.class).isExclusive()) {
  String url = CommonHelper.addParameter("login.jsp", "error", errorMsg);
  response.sendRedirect(url);
  return;
}

final ResourceBundle bundle = I18n.getBundle(request.getLocale());
Function<String, String> T = new Function<String, String>() {
  public String apply(String t) {
    try {
      return bundle.getString(t);
    } catch (Exception e) {
      return t;
    }
  }
};

AppInfo info = new AppInfo();

String loginHeader = "/login-header.jsp";
if (pageContext.getServletContext().getResource(loginHeader) == null) {
  loginHeader = null;
}

int year = Calendar.getInstance().get(Calendar.YEAR);
String copyright = String.format("&copy; 2005 - %s Axelor. All Rights Reserved.", year);

errorMsg = T.apply(errorMsg);
%>
<!DOCTYPE html>
<html lang="<%= info.getPageLang() %>">
<head>
  <title>Error</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <meta name="google" content="notranslate">
  <link rel="shortcut icon" href="ico/favicon.ico">
  <x:style src="css/application.login.css" />
  <x:script src="js/application.login.js" />
</head>
<body>
  <% if (loginHeader != null) { %>
  <jsp:include page="<%= loginHeader %>" />
  <% } %>

  <div class="container-fluid">
    <div class="panel login-panel">
      <div class="panel-header panel-default">
        <img src="img/axelor.png" width="192px" alt="Axelor">
      </div>
    </div>
    <div id="error-msg" class="alert alert-block alert-error text-center %>">
      <h4><%= HtmlUtils.escape(errorMsg) %></h4>
    </div>
  </div>

  <footer class="container-fluid">
    <p class="credit small"><%= copyright %></p>
  </footer>
</body>
</html>
