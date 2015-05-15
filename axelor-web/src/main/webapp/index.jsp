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
<%@ page import="com.axelor.app.AppSettings" %>
<%@ page import="com.axelor.web.internal.AppInfo" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.Locale"%>

<%
AppSettings settings = AppSettings.get();

String appName = settings.get("application.name", "My App");
String appDesc = settings.get("application.description", null);
String appHome = settings.get("application.home", "");
String appLogo = settings.get("application.logo", "img/axelor-logo.png");
String appTheme = settings.get("application.theme", null);
String appMenu = settings.get("application.menu", "both");

String appTitle =  appName;

if (appDesc != null) {
  appTitle = appName + " :: " + appDesc;
}

String appJS = AppInfo.getAppJS(getServletContext());
String appCss = AppInfo.getAppCSS(getServletContext());
String webkitCss = null;

if (AppInfo.isMobile(request) && AppInfo.isWebKit(request)) {
  webkitCss = "css/webkit.mobile.css";
}
%>
<!DOCTYPE html>
<html lang="en" ng-app="axelor.app" ng-controller="AppCtrl" ng-cloak>
  <head>
    <meta charset="utf-8">
    <title><%= appTitle %></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <meta name="description" content="<%= appDesc %>">
    <meta name="author" content="{{app.author}}">

    <!-- Le styles -->
    <link href="<%= appCss %>" rel="stylesheet">
    <% if (webkitCss != null) { %>
    <link href="<%= webkitCss %>" rel="stylesheet">
    <% } %>
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
      var __appSettings = <%= AppInfo.asJson() %>
    </script>
  </head>
  <body>

    <header class="header">
      <div class="navbar navbar-inverse navbar-fixed-top">
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
            <ul class="nav hidden-phone" nav-menu-bar></ul>
            <% } %>
            <ul class="nav nav-shortcuts pull-right">
              <li class="divider-vertical"></li>
              <li>
                <a href="#/" class="nav-link-home"><i class="fa fa-home"></i></a>
              </li>
              <li class="divider-vertical"></li>
              <li>
                <a href="" class="nav-link-mail"
                  ng-click="showMailBox()"><i class="fa fa-envelope"></i><sup
                    ng-show="unreadCount=$unreadMailCount()">{{unreadCount}}</sup></a>
              </li>
              <li class="divider-vertical"></li>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle nav-link-user" data-toggle="dropdown">
                  <img ng-src="{{app.userImage}}" width="20px"> <b class="caret"></b>
                </a>
                <ul class="dropdown-menu">
                  <li>
                    <a href="#/preferences">
                      <span class="nav-link-user-name">{{app.user}}</span>
                      <span class="nav-link-user-sub" x-translate>Preferences</span>
                    </a>
                  </li>
                  <li class="divider"></li>
                  <li><a href="#/about"></i><span x-translate>About</span></a></li>
                  <li><a href="logout"><span x-translate>Log out</span></a></li>
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
      <div class="fill-parent" ng-show="routePath[0] == 'main'" ng-include src="'partials/main-nomenu.html'"></div>
      <% } else { %>
      <div class="fill-parent" ng-show="routePath[0] == 'main'" ng-include src="'partials/main.html'"></div>
      <% } %>
      <div ng-switch-when="about" ng-include src="'partials/about.html'"></div>
      <div ng-switch-when="system" ng-include src="'partials/system.html'"></div>
      <div ng-switch-when="welcome" ng-include src="'partials/welcome.html'"></div>
      <div ng-switch-when="preferences" ng-include src="'partials/preferences.html'"></div>
    </section>

    <!-- JavaScript at the bottom for fast page loading -->
    <script src="js/lib/i18n.js"></script>
    <script src="ws/i18n/messages.js"></script>
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
