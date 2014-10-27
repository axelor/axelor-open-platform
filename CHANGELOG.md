# 3.0.2 (2014-10-14)

Bug fix release.

## Bug Fixes

- fixed gradlew.bat not deploying axelor-gradle (windows)
- fixed dropdown menu z-index issue
- fixed o2o formatting issue (grid view)
- fixed o2m/m2m grid resize issue
- fixed editable grid related issues

## Improvements

- support i18n extract for js/html sources
- csv based i18n support for web client
- support for installing modules by default (RM-2576)

# 3.0.1 (2014-10-08)

Bug fix release.

## Bug Fixes

- fixed some tree view related issues
- fixed F2/F3 permission issue
- fixed pending action issue for method actions
- fixed editor field selection
- fixed module install/uninstall issues
- fixed action-condition errors issue
- fixed MetaFiles.getPath returns wrong path
- fixed image widget height issue
- fixed print.css
- fixed error & login dialogs (make responsive)
- fixed height issues on various widgets
- fixed rollbackOnly transaction in xml import (catch the root cause)

# Improvements

- make button width 100%
- re-designed about page
- removed obsolete code
- do not attach panels in form view

# 3.0.0 (2014-09-23)

Fully responsive mobile ready views, gradle based build system and much more.

## Features

- migrated to gradle build system
- axelor cli utility (command line tool)
- fully responsive mobile ready views
- new responsive dashboard view (replaces portal view)
- custom editor/viewer support for new responsive form view
- removed nested editor support (use custom editor feature)
- implemented repository pattern
- generate pojo only domain class
- new customizable login page

# 2.1.0 (2014-06-27)

New translation system, security api, templates and mail support.

## Features

- re-implementation of security api
- custom sequence generator
- support for custom guice module to configure services
- new reflection api to search for resource in java classpath
- new mail send/recieve mails
- new template api (with multiple template engine support)
- integrated quartz scheduler
- on-the-fly record creation for m2o/m2m widgets
- implemented notification popup
- re-implemented code editor using CodeMirror widget
- support for external application.properties
- re-implemented translation support
- implemented translation extraction utilities

# 2.0.0 (2014-01-15)

Complete re-write of web client and major changes on server framework.

## Features

- angular.js + bootstrap + jquery based web ui
- inline editable grid widget (SlickGrid)
- major refactoring of data import api
- major refactoring of data persistence api
- workflow & bpmn support
- native sql formula field support on domain objects
- generate collection field helpers in domain class
- generate equals, hashCode and toString methods for domain class
- generate finder methods in domain class
- support for archived records
- support for hubernate L2 cache (ehcache)
- action-group to execute actions in a group
- binary & image field support
- csv based translation support
- support for document management
- optimized groovy script evaluation
- implemented readonly form view support
- implemented calendar view
- implemented chart view
- implemented tree view
- dynamic widget attributes
- embedded portlet widget for form view
- MultiSelect & TagSelect widgets
- RadioSelect & NavSelect widgets
- RefSelect widget (two part reference selection)
- module install/uninstall support
- view override support (duplicate views with same name)
- implemented html widget (TinyMCE)
- implemented advance search with custom & saved filters
- implemented mass update feature
- domain object inheritance & aggregation support
- grid row/cell highlight support
- keyboard shortcuts

# 1.0.0 (2013-07-17)

First stable release

## Features

- xml domain object definitions
- xml view definitions
- xml actions
- xml data import
- csv data import
- simple security api
- smartclient based web ui
- hibernate + guice + tomcat

# 0.9.x (2012-2013)

Initial prototype releases

