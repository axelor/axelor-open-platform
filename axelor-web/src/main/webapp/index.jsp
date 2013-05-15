<%@ page language="java" session="true" %>
<%@ page import="com.axelor.web.AppSettings" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.Locale "%>

<%

AppSettings settings = AppSettings.get();

String appName = settings.get("application.name", "My App");
String appDesc = settings.get("application.description", null);
String appHome = settings.get("application.home", "");
String appLogo = settings.get("application.logo", "");
String appTheme = settings.get("application.theme", null);

String appTitle =  appName;

if (appDesc != null)
	appTitle = appName + " :: " + appDesc;

String localeJS = AppSettings.getLocaleJS(request,getServletContext());
String appJS = AppSettings.getAppJS(getServletContext());

%>
<!DOCTYPE html>
<html lang="en" ng-app="axelor.app" ng-controller="AppCtrl" ng-cloak>
<head>
  <meta charset="utf-8">
  <title><%= appTitle %></title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="description" content="<%= appDesc %>">
  <meta name="author" content="{{app.author}}">

  <!-- Le styles -->
  <link href="css/application.css" rel="stylesheet">
  <% if (appTheme != null) { %>
  <link href="css/<%= appTheme %>/theme.css" rel="stylesheet">
  <% } %>
  <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
  <!--[if lt IE 9]>
    <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
  <![endif]-->

  <!-- Le fav and touch icons -->
  <link rel="shortcut icon" href="ico/favicon.ico">
  
  <script type="text/javascript">
	  var __appSettings = <%= settings.toJSON() %>
  </script>
</head>
<body>

  <header class="header">
    <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <% if (appLogo == null || "".equals(appLogo)) { %>
          <a class="brand" href="<%= appHome %>"><%= appName %></a>
          <% } else { %>
          <a class="brand-logo" href="<%= appHome %>">
            <img src="<%= appLogo %>">
          </a>
          <% } %>
          <ul class="nav pull-right">
            <li class="divider-vertical"></li>
            <li><a href="#/"><img src="img/home-icon.png"> <span x-translate>Home</span></a></li>
            <li class="divider-vertical"></li>
            <li><a href="{{app.help}}" target="_blank"><img src="img/help-icon.png"> <span x-translate>Help</span></a></li>
            <li class="divider-vertical"></li>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown">
              	<img src="img/about-icon.png"> <span x-translate>About</span> <b class="caret"></b>
              </a>
              <ul class="dropdown-menu">
                <li><a href="#/about"><i>{{app.name}} - v{{app.version}}</i></a></li>
                <li ng-show="app.sdk"><a href="http://axelor.com/"><i>Axelor Framework - v{{app.sdk}}</i></a></li>
              </ul>
            </li>
            <li class="divider-vertical"></li>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown">
              	<img src="img/user-icon.png"> <span>{{app.user}}</span> <b class="caret"></b>
              </a>
              <ul class="dropdown-menu">
                <li><a href="#" x-translate>Profile</a></li>
                <li class="divider"></li>
                <li><a href="logout" x-translate>Sign Out</a></li>
              </ul>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </header>
  
  <div ng-include src="'partials/login-window.html'"></div>
  <div ng-include src="'partials/error-window.html'"></div>
  
  <section role="main" id="container" ng-switch on="routePath[0]">
	<div ng-show="routePath[0] == 'main'" ng-include src="'partials/main.html'"></div>
	<div ng-switch-when="about" ng-include src="'partials/about.html'"></div>
	<div ng-switch-when="welcome" ng-include src="'partials/welcome.html'"></div>
  </section>

  <!-- JavaScript at the bottom for fast page loading -->
  <script src="js/lib/i18n.js"></script>
  <script src="js/i18n/en.js"></script>
  <% if (localeJS != null) { %>
  <script src="js/i18n/<%= localeJS %>.js"></script>
  <% } %>
  <script src="<%= appJS %>"></script>
  <!-- trigger adjustSize event on window resize -->  
  <script type="text/javascript">
  	$(function(){
  		$(window).resize(function(event){
  			if (!event.isTrigger)
  				$.event.trigger('adjustSize');
  		});
  	});
  </script>
</body>
</html>
