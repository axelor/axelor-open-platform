<%--

    Axelor Business Solutions

    Copyright (C) 2005-2018 Axelor (<http://axelor.com>).

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
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" session="true" %>
<%@ taglib prefix="x" uri="WEB-INF/axelor.tld" %>
<%@ page import="com.axelor.app.AppSettings" %>
<%@ page import="com.axelor.web.internal.AppInfo" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.stream.Collectors" %>
<%
AppSettings settings = AppSettings.get();
AppInfo info = new AppInfo();

String appName = settings.get("application.name", "My App");
String appDesc = settings.get("application.description", null);
String appHome = settings.get("application.home", "#/");
String appLogo = info.getLogo();
String appStyle = info.getStyle();
String appAuthor = settings.get("application.author", "");
String appTheme = info.getTheme();
String appMenu = settings.get("application.menu", "both");

String appTitle =  appName;

if (appDesc != null) {
  appTitle = appName + " :: " + appDesc;
}

String extraHead = "/index-head.jsp";
String extraFoot = "/index-foot.jsp";
String extraButtons = "/index-nav-buttons.jsp";

if (pageContext.getServletContext().getResource(extraHead) == null) {
	extraHead = null;
}
if (pageContext.getServletContext().getResource(extraFoot) == null) {
	extraFoot = null;
}
if (pageContext.getServletContext().getResource(extraButtons) == null) {
	extraButtons = null;
}

@SuppressWarnings("all")
Map<String, String> tenantMap = (Map) session.getAttribute("tenantMap");
String tenantId = (String) session.getAttribute("tenantId");

%>
<!DOCTYPE html>
<html lang="<%= info.getPageLang() %>" ng-app="axelor.app" ng-controller="AppCtrl" ng-cloak>
  <head>
    <meta charset="utf-8">
    <title><%= appTitle %></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <meta name="google" content="notranslate">
    <meta name="description" content="<%= appDesc %>">
    <meta name="author" content="<%= appAuthor %>">

    <!-- Le styles -->
    <x:style src="css/application.css" />
    <% if (appTheme != null) { %>
    <link href="css/<%= appTheme %>/theme.css" rel="stylesheet">
    <% } %>
    <% if (appStyle != null) { %>
    <style>
    <%= appStyle %>
    </style>
    <% } %>

    <style>
    header .nav > li {
      /* fix wierd dots in MS-Edge */
      list-style:none !important;
    }
    </style>

    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="ico/favicon.ico">
    <% if (extraHead != null) { %> <jsp:include page="<%= extraHead %>" /> <% } %>
  </head>
  <body>

    <header class="header">
      <div class="navbar navbar-fixed-top">
        <div class="navbar-inner">
          <div class="container-fluid">
            <ul class="nav hidden" id="offcanvas-toggle">
              <li>
                <a href=""><i class="fa fa-bars"></i></a>
              </li>
              <li class="divider-vertical"></li>
            </ul>
            <% if (appLogo == null || "".equals(appLogo)) { %>
            <a class="brand" href="<%= appHome %>"><%= appName %></a>
            <% } else { %>
            <a class="brand-logo" href="<%= appHome %>">
              <img src="<%= appLogo %>">
            </a>
            <% } %>
            <% if (!"left".equals(appMenu)) { %>
            <ul class="nav hidden-phone" data-nav-menu-bar></ul>
            <% } %>
            <ul class="nav nav-shortcuts pull-right">
              <li class="divider-vertical"></li>
              <li>
                <a href="#/" class="nav-link-home"><i class="fa fa-home"></i></a>
              </li>
              <li class="divider-vertical"></li>
              <li class="dropdown">
                <a href="javascript:" class="dropdown-toggle" data-toggle="dropdown">
                  <i class="fa fa-star"></i>
                </a>
                <ul class="dropdown-menu" nav-menu-fav></ul>
              </li>
              <li class="divider-vertical"></li>
              <li>
                <a href="" class="nav-link-mail"
                  ng-click="showMailBox()"><i class="fa fa-envelope"></i><sup
                    ng-show="unreadCount=$unreadMailCount()">{{unreadCount}}</sup></a>
              </li>
              <li class="divider-vertical"></li>
              <li nav-menu-tasks></li>
              <li class="divider-vertical"></li>
              <li class="dropdown">
                <a href="javascript:" class="dropdown-toggle nav-link-user" data-toggle="dropdown">
                  <img ng-src="{{ $user.image }}" width="20px"> <b class="caret"></b>
                </a>
                <ul class="dropdown-menu">
                  <li>
                    <a href="#/preferences">
                      <span class="nav-link-user-name">{{ $user.name }}</span>
                      <span class="nav-link-user-sub" x-translate>Preferences</span>
                    </a>
                  </li>
                  <% if (tenantMap != null && tenantMap.size() > 1) { %>
                  <li class="divider"></li>
	              <li>
	                <a href=""><strong><%= tenantMap.get(tenantId) %></strong></a>
	              </li>
                  <li class="dropdown-submenu">
                  	<a tabIndex="-1" href="" x-translate>More...</a>
                  	<ul class="dropdown-menu">
                  	<% for (String key : tenantMap.keySet()) { %>
                  	<% if (!key.equals(tenantId)) { %>
                  	  <li><a href="login.jsp?tenant=<%= key %>"><%= tenantMap.get(key) %></a></li>
                  	<% } %>
                  	<% } %>
                  	</ul>
                  </li>
                  <% } %>
                  <li class="divider"></li>
                  <li><a href="#/about"><span x-translate>About</span></a></li>
                  <li><a href="logout"><span x-translate>Log out</span></a></li>
                </ul>
              </li>
            </ul>
            <% if (extraButtons != null) { %> <jsp:include page="<%= extraButtons %>" /> <% } %>
          </div>
        </div>
      </div>
    </header>

    <div ng-include x-src="'partials/login-window.html'"></div>
    <div ng-include x-src="'partials/error-window.html'"></div>

    <section role="main" id="container" ng-switch x-on="routePath[0]">
      <% if ("top".equals(appMenu)) { %>
      <div class="fill-parent" ng-show="routePath[0] == 'main'" ng-include x-src="'partials/main-nomenu.html'"></div>
      <% } else { %>
      <div class="fill-parent" ng-show="routePath[0] == 'main'" ng-include x-src="'partials/main.html'"></div>
      <% } %>
      <div ng-switch-when="about"><div ng-include x-src="'partials/about.html'"></div></div>
      <div ng-switch-when="system"><div ng-include x-src="'partials/system.html'"></div></div>
      <div ng-switch-when="welcome"><div ng-include x-src="'partials/welcome.html'"></div></div>
      <div ng-switch-when="preferences"><div ng-include x-src="'partials/preferences.html'"></div></div>
    </section>

    <!-- JavaScript at the bottom for fast page loading -->
    <script src="js/messages.js"></script>
    <x:script src="js/application.js"/>
    <% if (extraFoot != null) { %> <jsp:include page="<%= extraFoot %>" /> <% } %>
  </body>
</html>
