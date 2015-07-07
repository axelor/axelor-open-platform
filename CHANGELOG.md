# 3.0.12 (2015-07-07)

## Bug fixes

- fix duplicate onload issue 
- fix dirty state handling with onNew having save
- fix unwanted collapse animation on panels
- fix popup/dialog issue
- fix save action issues in popup editor
- fix collapseIf on panel issue
- fix indexed action execution

## Improvements

- do not allow dragging maximized popup
- allow selector popup not resizable


# 3.0.11 (2015-07-02)

## Bug fixes

- fix onNew issue in calendar popup editor
- fix SelectProgress widget in grid
- fix duplicate read from popup
- fix editors inside panel-stack
- fix editor itemSpan
- fix o2m caching issue
- fix dropdown o2m/m2m in editable grid

## Improvements

- always force "save" action
- add view.form.check-version config option
- add collapse support for panels
- allow expressions with `can` attributes
- add x-show-titles attribute to editor tag
- add refresh button on grid view
- optimize popup editor window

# 3.0.10 (2015-06-19)

## Bug fixes

- fix tree view context issues
- fix x-bind issues (empty value)
- fix form view not in edit mode when opened from dashlet
- fix performance issue caused by distinct select

## Improvements

- lazy loading dashlet/portlet
- support for setting content id from main builder api
- support for buttons in tree view
- allow changing panel title with action-attrs
- don't show search option in m2o dropdown if all items are fetched

# 3.0.9 (2015-06-10)

## Bug fixes

- fix action handler issue (typo)
- fix search view overriding issue
- fix onChange event on toggle widget

## Improvements

- allow resizing popup editor on desktop
- use full width for dashboard on big screens
- don't include context details in i18n catalogs

# 3.0.8 (2015-06-01)

## Bug Fixes

- fix permission check for users without any group
- fix button widget readonly issue
- fix missing data demo value.
- fix action-attrs issue from editor fields
- fix schema definition
- fix button disable status issue
- fix transaction issue on exception
- fix some french translations
- fix print screen issues
- fix onload action issues
- fix mappedBy annotation

# 3.0.7 (2015-04-30)

## Bug Fixes

- fix wrong email sent date issue 
- fix file download issue
- fix binary widget download issue
- fix onChange issues on o2m fields
- fix recursion issue caused by an old fix

# 3.0.6 (2015-04-16)

## Bug Fixes

- fixed top-level editable grid save issue
- fixed editable grid update issue (IE only)
- fixed a regression (wrong change detection)
- fixed progress bar widget (IE)
- fixed advance search on o2m/m2m fields
- fixed decimal field issue 
- fixed unwrapped viewer template issue
- fixed selection-in on integer selection
- fixed textarea height issue
- fixed text widget issue in editable grid
- fixed grid widget cell formatting issue
- fixed hilite/css issue on text field
- fixed action-method conditional call issue
- fixed nested record update issue
- fixed dirty checking issue on html widget
- fixed parent context issue

## Improvements

- add special client side action 'validate'
- don't show m2o link if canView is false
- add some helper methods to ActionResponse
- hide text field on click outside the grid field
- throw exception if join field not found
- allow decimal scale/precision change on view
- allow password change from user preferences
- don't create demo user from data-init
- prevent certain operations on admin user/group



# 3.0.5 (2015-03-24)

## Bug Fixes

- fixed dependency order issue
- fixed editor context issue
- fixed wait for actions before onNew
- fixed response.view along with response.reload
- fixed meta scanner (search xml files only)
- fixed module loader clean up issues
- fixed duplicate views were not imported properly
- fixed advance search issue on datetime field 
- fixed context issue in tree view
- fixed context issue on portlet
- fixed selection issue in tree
- fixed onChange issue on view popup
- fixed I18nLoader encoding
- fixed onNew issue on closed popup editor
- fixed sequence when create records from the web application views
- fixed version check issue
- fixed multiline widget display in grid
- fixed html widget in grid : force multiline editor
- fixed image widget update issue
- fixed month format issue in chart
- fixed color issues in charts
- fixed build.gradle template for app stub
- fixed onClick handler issue when action has prompt
- fixed TagSelect widget issues
- fixed technical info popup
- fixed NavSelect rendering issue
- fixed	integer field handling with selection widget
- fixed SuggestBox canNew and x-create behavior
- fixed data-init issue
- fixed button in toolbar doesn't update title
- fixed duplicate records in search result
- fixed permission check on relational fields
- fixed file selection issue
- fixed wrong model name in help popover
- fixed m2o editor icons issue
- fixed undefined value error in viewer
- fixed attachment list not showing all records
- fixed grid portlet
- fixed wkf missing param
- fixed SmtpAccount NPE

## Improvements

- prevent double click on button
- don't allow init param from extending entity
- support onNew on o2m/m2o editor
- don't allow number width on panel forms
- improved editable grid
- support onNew support to grid widget
- add onNew support on panel-related
- disable hot keys on view popup
- minor code refactoring
- enable mass update button by default
- harmonize application default values
- update translation
- support for custom translation files
- support prompt message on item menu bar
- support for non-jta datasource
- reset form view scrolling
- show validation error for invalid grid fields
- add support for disable auto suggest
- remove hover effect from image button
- add missing title in MetaFile form view
- upgrade TinyMCE to 4.0.1

# 3.0.4 (2015-01-19)

## Bug Fixes

- fixed onSave/onChange issue
- fixed default values issue on editable grid
- fixed selection change issue on grid
- fixed exported file download issue
- fixed version info under eclipse
- fixed ImageSelect issue
- fixed m2o not showing name value
- fixed active cell not editing in editable grid
- fixed successive actions not updating context
- fixed pending action issue on reload request
- fixed inline o2m
- fixed permission issue
- fixed field editor validation issue
- fixed demo data
- fixed dashlet filter issue
- fixed image widget rendering issue
- fixed i18n extract issues
- fixed extra-code/extra-import merge issue
- fixed finder method generation issue
- fixed tree view empty parent node issue
- fixed route issue on record duplicate
- fixed editable grid cell value update and persistent row issues
- fixed cursor x-bind issue
- fixed missing import statements issue
- fixed generic Model.equals test 
- fixed automatic bi-direction association setup
- fixed print media css
- fixed child count in tree view
- fixed detached entity passed to persist
- fixed missing audit fields in context

## Improvements

- always generate code for extended entities
- update nvd3
- support for ADK version requirement
- document gradle tasks
- remove static groovy expression compilation
- improve m2o/m2m editor rendering
- allow reload from tab
- allow method as chart dataset provider
- allow prompt attribute from action-attrs
- improved help popover
- add option to save record from view popup
- add option to disable editor on o2m/m2m
- allow expr in search engine.
- update translation
- move 'selection' field definition into object definition.
- allow private setters in bean mapper

# 3.0.3 (2014-11-12)

## Bug Fixes

- fixed a regression that prevented duplicate view finding
- fixed menu user groups on view reset
- fixed menu rendering issue
- fixed i18n extract
- fixed RefSelect dummy widget issue
- fixed duplicate records in nested o2m
- fixed non-owning bi-directional o2o field issue
- fixed empty grid with canNew=false allows new record
- fixed image widget upload issue
- fixed binary widget download/update issue
- fixed tree view column title overflow
- fixed route issue on form view 
- fixed code editor empty issue
- fixed unnecessary dirty state from popup
- fixed missing attributes on panel-related
- fixed wrong context on m2o editor
- fixed o2m/m2m resize issue
- fixed help tooltip on prod mode
- fixed create default views

## Improvements

- add system information page
- add active user list
- allow i18n shell command per module
- provide default 'from' address for smtp server
- add replayTo to mail build 
- allow title on editor fields 
- show truncated button text as tooltip
- remove trailing comma in array and object literals
- allow editor as viewer
- improve placeholder style in form views
- improve MetaModel view

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

