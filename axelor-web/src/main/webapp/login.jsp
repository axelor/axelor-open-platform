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
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@ page language="java" session="true" %>
<%@ page import="com.axelor.i18n.I18n" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%
String loginTitle = I18n.get("Please sign in");
String loginRemember = I18n.get("Remember me");
String loginSubmit = I18n.get("Log in");

String loginUserName = I18n.get("Username");
String loginPassword = I18n.get("Password");

int year = Calendar.getInstance().get(Calendar.YEAR);
String copyright = String.format("&copy; 2005 - %s Axelor. All Rights Reserved.", year);

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
    <link href="lib/bootstrap/css/bootstrap.css" rel="stylesheet">
    <link href="lib/bootstrap/css/bootstrap-responsive.css" rel="stylesheet">
    <link href="lib/font-awesome/css/font-awesome.css" rel="stylesheet">
    <link href="css/view.form.checkbox.css" rel="stylesheet">
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
      <div class="panel login-panel">
        <div class="panel-header panel-default">
          <img src="img/axelor.png" width="192px">
        </div>
        <div class="panel-body">
          <form id="login-form" action="" method="POST">
            <div class="form-fields">
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-envelope"></i></span>
                <input type="text" id="usernameId" name="username"  placeholder="<%= loginUserName %>">
              </div>
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-lock"></i></span>
                <input type="password" id="passwordId" name="password" placeholder="<%= loginPassword %>">
              </div>
              <label class="ibox">
                <input type="checkbox" value="rememberMe" name="rememberMe">
                <span class="box"></span>
                <span class="title"><%= loginRemember %></span>
              </label>
            </div>
            <div class="form-footer">
              <button class="btn btn-success" type="submit"><%= loginSubmit %></button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <footer class="container-fluid">
      <p class="credit small"><%= copyright %></p>
    </footer>
  </body>
</html>
