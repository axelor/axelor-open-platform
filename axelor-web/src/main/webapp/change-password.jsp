<%--

    Axelor Business Solutions

    Copyright (C) 2005-2019 Axelor (<http://axelor.com>).

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
<%@ taglib prefix="x" uri="WEB-INF/axelor.tld" %>
<%@ page language="java" session="true" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" session="true" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.function.Function"%>
<%@ page import="com.axelor.i18n.I18n" %>
<%@ page import="com.axelor.auth.AuthService" %>
<%@ page import="com.axelor.app.AppSettings" %>
<%@ page import="com.axelor.auth.pac4j.AuthPac4jModule" %>
<%@ page import="org.pac4j.http.client.indirect.FormClient" %>
<%

Function<String, String> T = new Function<String, String>() {
  public String apply(String t) {
    try {
      return I18n.get(t);
    } catch (Exception e) {
      return t;
    }
  }
};

AuthService authService = AuthService.getInstance();

String passwordPattern = authService.getPasswordPattern();
String passwordPatternTitle = authService.getPasswordPatternTitle();
String newPasswordMustBeDifferent = T.apply("New password must be different.");
String confirmPasswordMismatch = T.apply("Confirm password doesn't match.");

String errorMsg = T.apply(request.getParameter(FormClient.ERROR_PARAMETER));

if (errorMsg == null) {
  errorMsg = T.apply("Please change your password.");
}

String confirmSubmit = T.apply("Change password");

String loginUserName = T.apply("Username");
String loginPassword = T.apply("Current password");
String newPassword = T.apply("New password");
String confirmPassword = T.apply("Confirm password");

String warningBrowser = T.apply("Update your browser!");
String warningAdblock = T.apply("Adblocker detected!");
String warningAdblock2 = T.apply("Please disable the adblocker as it may slow down the application.");

int year = Calendar.getInstance().get(Calendar.YEAR);
String copyright = String.format("&copy; 2005 - %s Axelor. All Rights Reserved.", year);

String loginHeader = "/login-header.jsp";
if (pageContext.getServletContext().getResource(loginHeader) == null) {
  loginHeader = null;
}

@SuppressWarnings("all")
Map<String, String> tenants = (Map) session.getAttribute("tenantMap");
String tenantId = (String) session.getAttribute("tenantId");

AppSettings settings = AppSettings.get();
String callbackUrl = AuthPac4jModule.getCallbackUrl();

%>
<!DOCTYPE html>
<html>
  <head>
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
          <img src="img/axelor.png" width="192px">
        </div>

        <div class="alert alert-block alert-error text-center">
					<h4><%= errorMsg %></h4>
				</div>

			  <div class="panel-body">
          <form id="login-form" action="<%=callbackUrl%>" method="POST">
            <div class="form-fields">
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-envelope"></i></span>
                <input type="text" id="usernameId" name="username" placeholder="<%= loginUserName %>"
                  required="required" value="<%= request.getParameter("username") %>" readonly="readonly">
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
                  required="required" pattern="<%= passwordPattern %>" title="<%= passwordPatternTitle %>"
                  oninput="checkPasswordInputs()">
              </div>
              <div class="input-prepend">
                <span class="add-on"><i class="fa fa-lock"></i></span>
                <input type="password" id="confirmPasswordId" name="confirmPassword" placeholder="<%= confirmPassword %>"
                  required="required"
                  oninput="checkPasswordInputs()">
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
      <div id="br-warning" class="alert alert-block alert-error hidden">
	  	<h4><%= warningBrowser %></h4>
	  	<ul>
	  		<li>Chrome</li>
	  		<li>Firefox</li>
	  		<li>Safari</li>
	  		<li>IE >= 11</li>
	  	</ul>
	  </div>
	  <div id="ad-warning" class="alert hidden">
	  	<h4><%= warningAdblock %></h4>
	  	<%= warningAdblock2 %>
	  </div>
    </div>

    <footer class="container-fluid">
      <p class="credit small"><%= copyright %></p>
    </footer>

    <div id="adblock"></div>

    <script type="text/javascript">
    $(function () {
	    if (axelor.browser.msie && !axelor.browser.rv) {
	    	$('#br-warning').removeClass('hidden');
	    }
	    if ($('#adblock') === undefined || $('#adblock').is(':hidden')) {
	    	$('#ad-warning').removeClass('hidden');
	    }
    });

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
