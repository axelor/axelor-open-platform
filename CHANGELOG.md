## 5.0.4 (2018-07-10)

#### Bugs

* Fix form layout regressions
* Revert fix for conditional expressions on fields on editable grid

## 5.0.3 (2018-07-09)

#### Enhancements

* Ref-select widget should not allow editing record
* Panel header is now clickable if canCollapse is true
* Improve form layout
* Improve modern theme
* Improve kanban design/UX
* Add encryption support on large text fields
* Bring back LDAP and CAS integration
* Change `X-References` to `References` header in email message

#### Bugs

* Fix selection popup record ordering issue
* Fix conditional expressions on fields on editable grid
* Fix requiredIf condition issue
* Fix group maping from LDAP issue
* Fix advance search input issue
* Fix JavaEL expression issue
* Fix grid widget grouping issue on hidden column
* Fix NPE when trying to delete non-existent record
* Fix all day event issue in calendar view
* Fix resource leak when generating report pdf

## 5.0.2 (2018-06-20)

#### Enhancements

* Add support to disallow sub-select in domain filters with `domain.allow.sub-select = false`

#### Bugs

* Fix tag select widget issue on firefox
* Fix checkbox field in editable grid

## 5.0.1 (2018-06-18)

#### Bugs

* Fix editable grid cell focus issue
* Fix editable o2m item remove issue
* Fix widget attribute reset issue
* Fix kanban view missing values issue after card move
* Fix bulk update/delete issue with MySQL
* Fix conditional permissions with empty params value
* Fix conditional permissions not checked against database values

## 5.0.0 (2018-06-11)

#### New Features

* Encrypted field support

#### Enhancements

* Simplified access control rules
* Improve boolean widget readonly style
* Improve nav-select and boolean widget readonly style
* Remove unique constraints from User's name and email fields
* Add message stream widget to teams form
* Clear search value from advance filter when chaning field

#### Bugs

* Fix print.css
* Fix missing help icon on some widgets
* Fix groovy support
* Fix json fields validation issue
* Fix calendar view not fetching all events
* Fix kanban drag and drop issue on firefox
* Fix advance filter save issue with dotted fields
* Fix value enum log message
* Fix long command line issue on windows
* Fix context filter ignored when exporting data
* Fix file upload whitelist not checked with file fields
* Fix grid widget auto size issue
* Fix unable translate field value from unsaved records
* Fix validation error notification not shown from popup
* Fix search text validation issue on m2o field

## 5.0.0-rc5 (2018-05-14)

#### New Features

* Allow to export with single click
* Disable full export with `view.adv-search.export.full = false`
* Add support for file type whitelist & blacklist for upload

#### Bugs

* Fix non-imported incoming emails marked as seen issue
* Fix stream message mail subject issue
* Fix file attachment issue for stream message from popup composer
* Fix concurrent mail fetching issue of stream replies
* Fix extension view is include multiple time issue
* Fix route change issue from kanbank view
* Fix `file.upload.size` setting was not used whith DMS interface
* Refresh cards view after deleting a card to fix pagination issue
* Fix regression caused by search text validation on m2o

## 5.0.0-rc4 (2018-05-08)

#### Enhancements

* Updated translations

#### Security Fixes

* Fix file upload issue where file can be saved outside upload directory

#### Bugs

* Close mail inbox after fetching messages
* Fix tree view field mapping
* Fix group permission issue on menus
* Fix grid column alignment issue in popup
* Fix onNew event issue on popup editor
* Fix image widget regression
* Revert "Current user password should be required for changing users"

## 5.0.0-rc3 (2018-05-03)

#### Enhancements

* Prevent form view to switch when actions are in progress
* Improved maven publishing
* Use title as on grid button tooltip if help is not given
* Allow viewer template on relational fields without editor
* Improved new modern theme

#### Security Enhancements

* Add `session.cookie.secure` config option, can be used when app is served over https
* Do not show error stacktrace in production mode to prevent leaking sensitive details
* Current user password is required for creating or changing user records
* Reset session id to prevent session fixation

#### Security Fixes

* Fix user password hash in response
* Fix XSS vulnerability by sanitizing text values

#### Bugs

* Fix kanban column no records message dispay issue
* Fix $number helper is missing for viewer templates
* Fix code generation with gradle composite builds
* Fix m2o column reset issue with onNew action
* Fix dotted field issue in saved search filter
* Fix record delete issue with form only action-view
* Fix m2o validation issue when clearing search text

## 5.0.0-rc2 (2018-04-13)

#### New Features

* Add maven publish support in gradle plugin
* Added new clean theme "Modern"
* New setting `view.tabs.max` to control maximum number of tabs
* Set calendar date with `calendarDate` from action context
* Set calendar mode with `calendarMode` from action context
* Currency formatting using `x-currency="field.name"`
* Allow to provide custom css using `__config__.appStyle`
* Filter detail of grid view is now accessible from action context 

#### Enhancements

* Added adblocker detection (warns on login page if adblocker is detected)
* Added browser detection (warns on login page is using IE <= 11)
* Allow any action as home action if home attribute is true
* Optimize tooltip initialization
* Improve UI layout for better theme support
* Allow to show html view in popup
* Allow to manage view menus and menu items with field perms
* Only show technical popup to technical staff even in dev mode
* Optimize DMS view with huge file structure
* Support `customSearch` and `freeSearch` attributes to cards and kanban views
* Data export can be controlled with `data.export.max-size` and `data.export.fetch-size`
* Optimize grid widget rendering
* Optimize web ui by reducing DOM size
* Customize menu for custom models
* Thousand separator formatting for numeric fields
* Implemented versioned bulk update
* Custom fields in grid view are now added before buttons

#### Bugs

* Fix calendar view not using grid view filter
* Fix module uninstall issue
* Fix module install issue
* Fix selected row color issue in grid view when row is highlighted
* Fix encoding for CSV files
* Fix xml import eval attribute not supporting call actions
* Fix grid widget auto size issue with grouped data
* Fix XML source file processing on Windows
* Fix html widget style issues
* Fix issues with editable grid when all fields of the row are readonly
* Fix o2m/m2m field dropdown was not visible in editable grid
* Fix editable grid was not marking parent form dirty
* Fix mass update issues with null
* Fix navigation tabs icon and colors not updated properly
* Fix grid view reload with button action
* Fix placeholder issue on editor fields
* Fix `_model` key missing in context
* Fix translate icon on field without label
* Fix reference column formatting in tree view
* Fix view xsd having action-view attribute home in wrong place
* Fix o2m/m2m fields should always show archived records
* Fix m2o selection should not include archived records
* Fix duplicate row created on o2m when an action is using `response.setValues`
* Fix attachment file updates with DMS view
* Fix NPE caused by mail fetcher job
* Fix `freeSearch` with name field not working on grid view
* Fix various popup dialog layout issues
* Fix advance search not visible in view popup
* Fix memory leaks in web ui
* Fix parent reload from popup 
* Fix unarchive menu item not visible in form view
* Fix dotted fields in editable grid not updated if related m2o changes 
* Fix popup editor readonly issue
* Fix o2m editable grid sometime duplicates previous cell's value when creating new rows
* Fix time widget update issue in editable grid view
* Fix m2o field dropdown menu in editable grid
* Fix mass updatable field sometime not listed
* Fix menu overriding issue caused by wrong ordering
* Fix xml id is not utilized for menu and action definitions
* Fix context update issue caused by `response.setValues` call
* Fix value formatting issues in tree view
* Fix `nav-select` widget initialization issue
* Fix advance search field selection sorting
* Fix view tabs icon and colors not updated properly
* Fix translatable field value is sometime not translated

## 5.0.0-rc1 (2018-02-07)

#### New Features

* Migrate to Java8
* Migrate to Hibernate 5
* Migrate to java.time (drop joda.time)
* Use HikariCP as connection pool
* Oracle database support (12c)
* MySQL database support (5.7)
* Multi-Tenancy support
* Improved logging with logback
* Tomcat 8.5 and Servlet API 3.1
* Full-text search support using hibernate-search
* Sidebar menu search
* CSV export from dashlet/o2m/m2m
* Dynamic custom fields support
* Dynamic custom models support
* Contextual advance search for custom fields
* Context aware grid columns for custom fields
* Automatic form & grid views for custom models
* Master-Details support on main grid view
* Basic teams/tasks features
* JCache integration for hibernate L2-cache
* JavaScript scripting support using Nashorn
* Add new action-script action
* Add hot code change support using hotswap-agent (experimental)
* Add hot view xml changes (experimental)
* Add Intellij IDE support
* Improved Eclipse IDE support using buildship
* New embedded tomcat runner with hotswap and debugging support
* Add support to define help for views externally and overrides help defined in views
* Add SLF4J logger injection support
* Add enum type fields support
* Kotlin and Scala support

#### Enhancements

* Support for `join-table` on m2m fields
* Color support in stdout logging 
* Allow to override file upload directory structure
* Optimized code generation gradle task
* Allow to add message content with change tracking
* Re-implementation of context using proxy with seamless access to context values as well as database values
* Improve DMS ergonomics
* Allow to unarchive records
* Allow closing tabs with mouse middle click
* Re-implemented value translation feature
* Allow enhance base `Model` class with simple fields

#### Deprecations

* jdbc style positional parameters are deprecated, use JPA style positional parameters only

#### Breaking Changes

* Removed shell
* Mail groups are replaced with team (see basic teams feature)
* Method `Context#asType(Class)` returns proxy instance
* Changed scripting helper `__repo__.of()` to `__repo__()`
* Gradle tasks `init` and `migrate` are replaced with new `database` task

#### Breaking Schema Changes (from v4)

* `auth_permission.condition_value` column size changed from `255` to `1024`
* `mail_group` table dropped
* `mail_group_users` table dropped
* `mail_group_groups` table dropped
* `meta_module.depends` column dropped
* `meta_translation.message_key` column type changed from `text` to `varchar(1024)`
* `meta_translation.message_value` column type changed from `text` to `varchar(1024)`
