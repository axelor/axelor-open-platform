<%--

    Axelor Business Solutions

    Copyright (C) 2012-2014 Axelor (<http://axelor.com>).

    This program is free software: you can redistribute it and/or  modify
    it under the terms of the GNU Affero General Public License, version 3,
    as published by the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page language="java" session="true" %>
<%@ page import="com.axelor.i18n.I18n" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%
String loginTitle = I18n.get("Please sign in");
String loginRemember = I18n.get("Remember me");
String loginSubmit = I18n.get("Log in");

String loginUserName = I18n.get("User name");
String loginPassword = I18n.get("Password");

String loginHeader = "login-header.jsp";
if (pageContext.getServletContext().getResource(loginHeader) == null) {
  loginHeader = null;
}
%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style type="text/css">
      body {
        padding-top: 60px;
      }
    </style>
    <link href="lib/bootstrap/css/bootstrap.css" rel="stylesheet">
    <link href="lib/font-awesome/css/font-awesome.css" rel="stylesheet">
    <link href="css/colors.css" rel="stylesheet">
    <link href="css/login.css" rel="stylesheet">
    <script type="text/javascript" src="lib/jquery.ui/js/jquery.js"></script>
    <script type="text/javascript" src="lib/bootstrap/js/bootstrap.js"></script>
  </head>
  <body>

    <% if (loginHeader != null) { %>
    <jsp:include page="<%= loginHeader %>" />
    <% } %>

    <div class="container-fluid">
      <div class="row-fluid">
        <div class="panel login-panel span4 offset4">
          <div class="panel-header panel-default">
            <span class="panel-title"><%= loginTitle %></span>
          </div>
          <div class="panel-body">
            <form id="login-form" action="" method="POST">
              <label for="usernameId"><%= loginUserName %></label>
              <input type="text" class="input-block-level" id="usernameId" name="username">
              <label for="passwordId"><%= loginPassword %></label>
              <input type="password" class="input-block-level" id="passwordId" name="password">
              <label class="checkbox"> <input type="checkbox" value="rememberMe" name="rememberMe"> <%= loginRemember %>
              </label>
              <button class="btn btn-primary" type="submit"><%= loginSubmit %></button>
            </form>
          </div>
        </div>
      </div>
    </div>

    <footer class="container-fluid">
      <p class="credit small">&copy; 2014 <a href="http://www.axelor.com">Axelor</a>. All Rights Reserved.</p>
    </footer>
  </body>
</html>
