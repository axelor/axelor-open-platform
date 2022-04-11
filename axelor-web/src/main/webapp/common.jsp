<%--

    Axelor Business Solutions

    Copyright (C) 2005-2022 Axelor (<http://axelor.com>).

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
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.function.Function"%>
<%@ page import="com.axelor.i18n.I18n" %>
<%@ page import="com.axelor.web.internal.AppInfo" %>
<%@ page import="com.axelor.app.AppSettings" %>
<%@ page import="com.axelor.app.AvailableAppSettings" %>
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

AppInfo appInfo = new AppInfo();
AppSettings appSettings = AppSettings.get();

String pageLang = appInfo.getPageLang();

String appLogo = appSettings.get(AvailableAppSettings.APPLICATION_LOGO, "img/axelor.png");
String appCopyright = appSettings.get(AvailableAppSettings.APPLICATION_COPYRIGHT,
  "&copy; 2005–{year} Axelor. All Rights Reserved.");

%>
