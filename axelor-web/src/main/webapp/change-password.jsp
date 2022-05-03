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
<%@ page language="java" session="true" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.axelor.inject.Beans" %>
<%@ page import="com.axelor.auth.AuthService" %>
<%@ page import="com.axelor.auth.pac4j.AuthPac4jInfo" %>
<%@ page import="org.pac4j.http.client.indirect.FormClient" %>
<%@ page import="com.axelor.common.HtmlUtils" %>
<%@include file='common.jsp'%>
<%

AuthService authService = AuthService.getInstance();

String newPasswordMustBeDifferent = T.apply("New password must be different.");
String confirmPasswordMismatch = T.apply("Confirm password doesn't match.");

String errorMsg = T.apply(request.getParameter(FormClient.ERROR_PARAMETER));
String username = request.getParameter("username");

if (errorMsg == null) {
  errorMsg = T.apply("Please change your password.");
}

String confirmSubmit = T.apply("Change password");

String loginUserName = T.apply("Username");
String loginPassword = T.apply("Current password");
String newPassword = T.apply("New password");
String confirmPassword = T.apply("Confirm new password");
String passwordPattern = authService.getPasswordPattern();
String passwordPatternTitle = authService.getPasswordPatternTitle();

@SuppressWarnings("all")
Map<String, String> tenants = (Map) session.getAttribute("tenantMap");
String tenantId = (String) session.getAttribute("tenantId");

AuthPac4jInfo authPac4jInfo = Beans.get(AuthPac4jInfo.class);
String callbackUrl = authPac4jInfo.getCallbackUrl();
%>
<!DOCTYPE html>
<html lang="<%= pageLang %>">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <meta name="google" content="notranslate">
    <link rel="shortcut icon" href="ico/favicon.ico">
    <x:style src="css/application.login.css" />
    <x:script src="js/application.login.js" />
  </head>
  <body>

    <div class="container-fluid">
      <div class="panel login-panel">
        <div class="panel-header panel-default">
          <img src="<%= appLogo %>" width="192px">
        </div>

        <div class="alert alert-block alert-error text-center">
          <h4><%= HtmlUtils.escape(errorMsg) %></h4>
        </div>

        <div class="panel-body">
          <form id="login-form" action="<%=callbackUrl%>" method="POST">
            <div class="form-fields">
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-envelope"></i></span>
                <input type="text" id="usernameId" name="username" placeholder="<%= loginUserName %>"
                  required="required"
                  <%= username != null ? "value=\"" + HtmlUtils.escapeAttribute(username) + "\" readonly=\"readonly\"" : "autofocus=\"autofocus\"" %>>
              </div>
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-lock"></i></span>
                <input type="password" id="passwordId" name="password" placeholder="<%= loginPassword %>"
                  required="required" autofocus="autofocus"
                  oninput="checkPasswordInputs()">
              </div>
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-lock"></i></span>
                <input type="password" id="newPasswordId" name="newPassword" placeholder="<%= newPassword %>"
                  required="required"
                  oninput="checkPasswordInputs()"
                  pattern="<%= passwordPattern %>"
                  title="<%= passwordPatternTitle %>">
              </div>
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-lock"></i></span>
                <input type="password" id="confirmPasswordId" name="confirmPassword" placeholder="<%= confirmPassword %>"
                  required="required"
                  oninput="checkPasswordInputs()">
              </div>
              <div id="password-title" class="alert alert-block alert-info text-center">
                <h4><%= passwordPatternTitle %></h4>
              </div>
              <% if (tenants != null && tenants.size() > 1) { %>
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-database"></i></span>
                <select name="tenantId">
                <% for (String key : tenants.keySet()) { %>
                  <option value="<%= key %>" <%= (key.equals(tenantId) ? "selected" : "") %>><%= tenants.get(key) %></option>
                <% } %>
                </select>
              </div>
              <% } %>
            </div>
            <div class="form-footer">
              <button class="btn btn-primary" type="submit"><%= confirmSubmit %></button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <footer class="container-fluid">
      <p class="credit small"><%= appCopyright %></p>
    </footer>

    <script type="text/javascript">
    function checkPasswordInputs() {
      var passwordElem = document.getElementById("passwordId");
      var newPasswordElem = document.getElementById("newPasswordId");
      var confirmPasswordElem = document.getElementById("confirmPasswordId");
      newPasswordElem.setCustomValidity(passwordElem.value === newPasswordElem.value
          ? "<%= newPasswordMustBeDifferent %>" : "");
      confirmPasswordElem.setCustomValidity(newPasswordElem.value !== confirmPasswordElem.value
          ? "<%= confirmPasswordMismatch %>" : "");
    }

    $("#confirmPasswordId").bind("copy paste", function(e) {
      e.preventDefault();
    });
    </script>
  </body>
</html>
