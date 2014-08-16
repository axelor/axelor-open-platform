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
String loginSubmit = I18n.get("Login");

String loginUserName = I18n.get("User name");
String loginPassword = I18n.get("Password");

String pageTitle = I18n.get("Axelor");
%>
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style type="text/css">
body {
	padding-top: 40px;
	padding-bottom: 40px;
	background-color: #f5f5f5;
}

.form-signin {
	max-width: 300px;
	padding: 19px 29px 29px;
	margin: 0 auto 20px;

	border: 1px solid #e5e5e5;
	background-color: #fff;
	
	-webkit-border-radius: 5px;
  	   -moz-border-radius: 5px;
	        border-radius: 5px;

	-webkit-box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
	   -moz-box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
	        box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
}

.form-signin .form-signin-heading,
.form-signin .checkbox {
	margin-bottom: 10px;
}

.form-signin input[type="text"],
.form-signin input[type="password"] {
	font-size: 16px;
	height: auto;
	margin-bottom: 15px;
	padding: 7px 9px;
}

#footer {
	height: 60px;
}

.container .credit {
	margin: 20px 0;
	text-align: center;
}

.app-title {
	font-size: 32px;
	text-align: center;
}
</style>
<link href="lib/bootstrap/css/bootstrap.css" rel="stylesheet">
<style>
body {
	background-color: #f5f5f5;
}
</style>
</head>
<body>
	<div class="container">
		<div class="app-title">
			<h2 class="muted"><%= pageTitle %></h2>
		</div>
		<form class="form-signin" action="" method="POST">
			<h2 class="form-signin-heading"><%= loginTitle %></h2>
			<input type="text" class="input-block-level" placeholder="<%= loginUserName %>"
				tabindex="1" name="username">
			<input type="password" class="input-block-level" placeholder="<%= loginPassword %>"
				tabindex="2" name="password">
			<label class="checkbox"> <input type="checkbox"
				tabindex="3" value="rememberMe" name="rememberMe"> <%= loginRemember %>
			</label>
			<button tabindex="4" class="btn btn-large btn-primary" type="submit"><%= loginSubmit %></button>
		</form>
		<div id="footer">
			<div class="container">
				<p class="muted credit">&copy; Axelor. All Rights Reserved.</p>
			</div>
		</div>
	</div>
</body>
</html>
