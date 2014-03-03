<%--

    Copyright (c) 2012-2014 Axelor. All Rights Reserved.

    The contents of this file are subject to the Common Public
    Attribution License Version 1.0 (the "License"); you may not use
    this file except in compliance with the License. You may obtain a
    copy of the License at:

    http://license.axelor.com/.

    The License is based on the Mozilla Public License Version 1.1 but
    Sections 14 and 15 have been added to cover use of software over a
    computer network and provide for limited attribution for the
    Original Developer. In addition, Exhibit A has been modified to be
    consistent with Exhibit B.

    Software distributed under the License is distributed on an "AS IS"
    basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
    the License for the specific language governing rights and limitations
    under the License.

    The Original Code is part of "Axelor Business Suite", developed by
    Axelor exclusively.

    The Original Developer is the Initial Developer. The Initial Developer of
    the Original Code is Axelor.

    All portions of the code written by Axelor are
    Copyright (c) 2012-2014 Axelor. All Rights Reserved.

--%>
<%@ page language="java" session="true" %>
<%@ page import="com.axelor.app.AppSettings" %>
<%@ page import="com.axelor.web.internal.AppInfo" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.Locale"%>

<%
	AppSettings settings = AppSettings.get();

String appName = settings.get("application.name", "My App");
String appDesc = settings.get("application.description", null);
String appHome = settings.get("application.home", "");
String appLogo = settings.get("application.logo", "");
String appTheme = settings.get("application.theme", null);
String appMenu = settings.get("application.menu", "both");

String appTitle =  appName;

if (appDesc != null)
	appTitle = appName + " :: " + appDesc;

String appJS = AppInfo.getAppJS(getServletContext());
String appCss = AppInfo.getAppCSS(getServletContext());
String langJS = AppInfo.getLangJS(request, getServletContext());
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
  <link href="<%= appCss %>" rel="stylesheet">
  <%
  	if (appTheme != null) {
  %>
  <link href="css/<%= appTheme %>/theme.css" rel="stylesheet">
  <%
  	}
  %>
  <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
  <!--[if lt IE 9]>
    <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
  <![endif]-->

  <!-- Le fav and touch icons -->
  <link rel="shortcut icon" href="ico/favicon.ico">
  
  <script type="text/javascript">
	  var __appSettings = <%= AppInfo.asJson() %>
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
          <% if (!"left".equals(appMenu)) { %>
          <ul class="nav" nav-menu-bar></ul>
          <% } %>
          <ul class="nav nav-shortcuts pull-right">
            <li class="divider-vertical"></li>
            <li>
            	<a href="#/"><img src="img/home-icon.png"></a>
            </li>
            <li class="divider-vertical"></li>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                <img src="img/help-icon.png"> <b class="caret"></b>
              </a>
              <ul class="dropdown-menu">
                <li><a href="{{app.help}}" target="_blank"><span x-translate>Help</span></a></li>
                <li class="divider"></li>
                <li><a href="#/about"><i>{{app.name}} - v{{app.version}}</i></a></li>
                <li ng-show="app.sdk"><a href="http://axelor.com/" target="_blank"><i>Axelor Framework - v{{app.sdk}}</i></a></li>
              </ul>
            </li>
            <li class="divider-vertical"></li>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown">
              	<img src="img/user-icon.png"> <span>{{app.user}}</span> <b class="caret"></b>
              </a>
              <ul class="dropdown-menu">
                <li><a href="#/preferences"><i class="icon-cog"></i> <span x-translate>Preferences</span></a></li>
                <li class="divider"></li>
                <li><a href="logout"><i class="fa-power-off"></i> <span x-translate>Logout</span></a></li>
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
    <% if ("top".equals(appMenu)) { %>
	<div ng-show="routePath[0] == 'main'" ng-include src="'partials/main-nomenu.html'"></div>
    <% } else { %>
    <div ng-show="routePath[0] == 'main'" ng-include src="'partials/main.html'"></div>
    <% } %>
	<div ng-switch-when="about" ng-include src="'partials/about.html'"></div>
	<div ng-switch-when="welcome" ng-include src="'partials/welcome.html'"></div>
	<div ng-switch-when="preferences" ng-include src="'partials/preferences.html'"></div>
  </section>

  <!-- JavaScript at the bottom for fast page loading -->
  <script src="js/lib/i18n.js"></script>
  <script src="js/i18n/en.js"></script>
  <% if ( langJS != null) { %>
  <script src="<%= langJS %>"></script>
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
