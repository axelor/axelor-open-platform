<%@ page language="java" session="true"%>
<%@ page import="com.axelor.web.AppSettings"%>
<%@ page contentType="text/html; charset=UTF-8"%>

<%
	AppSettings settings = AppSettings.get();

	String appName = settings.get("application.name", "My App");
	String appLogo = settings.get("application.logo", "");
	String appHome = settings.get("application.home", "");
	String appDesc = settings.get("application.description", null);
	String appVersion = settings.get("application.version");

	String appTitle = appName;
	if (appDesc != null && !"".equals(appDesc.trim())) {
		appTitle = appName + " :: " + appDesc;
	}
%>
<html>
<head>
<link href="lib/bootstrap/css/bootstrap.css" rel="stylesheet">
<style type="text/css">

body {
	padding-top: 60px;
	padding-bottom: 40px;
	background-color: white;
}

.navbar-inverse .navbar-inner {
	background: #F5F5F5;
	border-color: #3c3c3d;
	-webkit-box-shadow: none;
	   -moz-box-shadow: none;
	        box-shadow: none;
	filter: none;
}

.navbar-inverse .nav .active > a {
	color: white;
	background-color: #53881f;
}

.navbar-inverse .nav > li > a:hover {
	color: black;
}

.navbar-inverse .nav .active > a:hover {
	color: black;
	background-color: #53881f;
}

.navbar-inverse .brand {
	color: black;
}

.navbar-inverse .brand:hover {
	color: black;
}

.form-signin {
	max-width: 300px;
	padding: 19px 29px 29px;
	margin: 0 auto 20px;

	border: 1px solid #53881f;
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
	margin: 20px 0;
	height: 60px;
}

.container .credit {
	text-align: center;
}

.app-title {
	height: auto;
	font-size: 32px;
	text-align: center;
	margin: 15px 0 20px;
}

.app-title .muted {
	color: #53881f;
}
</style>
<title><%=appTitle%></title>
<link rel="shortcut icon" href="ico/favicon.ico">
</head>
<body>
	<div class="navbar navbar-inverse navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container">
				<button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
					<span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
				</button>
				<% if (appLogo == null || "".equals(appLogo)) { %>
				<a class="brand" href="<%= appHome %>" style="font-size: 25px;"><%= appName %></a>
				<% } else { %>
				<a class="brand" href="<%= appHome %>" style="padding: 3px;">
				  <img src="<%= appLogo %>" style="height: 34px;">
				</a>
				<% } %>
				<div class="nav-collapse collapse pull-right">
					<ul class="nav">
						<li class="active"><a href="#">Home</a></li>
						<li><a href="#about">About</a></li>
						<li><a href="#contact">Contact</a></li>
					</ul>
				</div>
				<!--/.nav-collapse -->
			</div>
		</div>
	</div>

	<div class="container">
		<div class="app-title">
			<img src="img/axelor.png" style="height: 70px; margin-bottom: 10px">
			<h2 class="muted"><%=appName%></h2>
		</div>
		<form class="form-signin" action="" method="POST">
			<h2 class="form-signin-heading">Please sign in</h2>
			<input type="text" class="input-block-level" placeholder="User name"
				tabindex="1" name="username">
			<input type="password" class="input-block-level" placeholder="Password"
				tabindex="2" name="password">
			<label class="checkbox"> <input type="checkbox"
				tabindex="3" value="rememberMe" name="rememberMe"> Remember me
			</label>
			<button tabindex="4" class="btn btn-large btn-success" type="submit">Login</button>
		</form>
		<div id="footer">
			<div class="container">
				<div class="muted credit">Version: <%=appVersion%></div>
				<p class="muted credit">&copy; Axelor. All Rights Reserved.</p>
			</div>
		</div>
	</div>
</body>
</html>
