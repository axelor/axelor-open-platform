## 5.3.8 (2021-02-10)

#### Features

* Add support for defining Column attributes `insertable` and `updatable`

#### Fixed

* Fix requiredIf with some widgets (BinaryLink, CodeEditor…)
* Set "SameSite=None" attribute on cookies to allow CORS requests

#### Security

* Fix HTML sanitization in suggest box and tree view

## 5.3.7 (2020-12-18)

#### Features

* Add index to MetaHelp on model, language, view fields

#### Fixed

* Prevent usage of proxies in search request
* Fix o2m/m2m sorting issue caused by permissions
* Fix missing CSRF header upon successful login request
* Fix searching for o2o field on the non-owning side
* Fix MenuItem tag display with null/empty value
* Save decimal value with applied x-scale from view
* Fix translation of dotted fields on references in grid view
* Fix one-to-many editor validation issue
* Fix misalignment on grid group row
* Fix required field validation on contextual custom fields

## 5.3.5 (2020-10-14)

#### Features

* Go into edit on focus for multiline string in editable grid
* Add index to MailMessage on relatedModel, relatedId fields

#### Fixed

* Fix change tracking translation issue
* Hide edit in grid icons bar while editing row
* Set authentication request character encoding to UTF-8
* Fix customize dashboard with drag & drop
* Fix Context#asType invalid type error message
* Fix boolean field focus issue in editable grid
* Fix timezone issue with start/end time in calendar view popup
* Fix timezone issue with start/end of calendar view
* Fix single-select widget on integer field
* Fix readonly rendering of inline HTML widget
* Fix readonly rendering of large string in editable grid
* Fix setting of o2m/m2m field in editable grid from action
* Fix PostAction event issue for action-record
* Fix buttons not shown when using x-selector="checkbox"
* Ignore hiding of fields in editable grid
* Update dotted fields in grid from form dotted fields
* Fix onChange not triggered when setting date to null
* Fix rare emptied row on save issue in editable grid
* Fix multi-month calendar events missing for intermediate months

## 5.3.5 (2020-08-17)

#### Features

* Add support for help on panel-dashlet
* Add support for per grid widget checkbox selector

  <details>
  Now we can use `x-selector="checkbox"` on grid view or panel-related to
  show checkbox selection.
  </details>

* Fix gantt view pagination issue

#### Fixed

* Fix switching to another tenant
* Fix dotted fields disappearing after edition on top-level editable grids
* Fix black and white tag colors
* Fix empty form after saving new records in details view
* Fix downloading of meta files if parent has collections of meta files
* Fix missing read permission on user/group for DMS
* Fetch target names for tag-select widget
* Fix view extension replace on menubar/toolbar/panel-mail
* Fix action method call with arguments having ':'
* Fix charts with zero in series
* Fix emptied field not present in grid after opening popup form
* Fix truncated title and vertical alignment of tag-select with modern theme
* Fix view watching from mixed location kinds
* Fix nullable boolean radio displayed as false
* Fix search null or empty string field values
* Fix grid toolbar button js expression issue
* Fix onNew on editable grid row
* Fix column menu show/hide item in grid view
* Fix wrong positional parameter resolution from query binder
* Fix mail base64 image max line (RFC2045)
* Fix dotted field translation in grid view
* Fix NavSelect and MenuBar size adjust issue

#### Security

* Validate external input for tree view

## 5.3.4 (2020-06-29)

#### Fixed

* Fix mass update on grid view because of dotted fields
* Restrict system page to technical staff
* Fix the button _signal in context
* Fix query building with empty logical filters
* Formatting the restoring meta execution time in ISO time
* Fix NPE caused by response exception handler
* Fix missing delimiter in advanced search when no export permission
* Fix input left padding with ImageSelect widget when element is not shown immediately after edit mode
* Consider permissions and showArchived view-param for tag-count
* Fix meta file link with no parent

## 5.3.3 (2020-06-05)

#### Features

* Add support for font config for birt reports
* Support axis titles on charts
* Support cards, kanban, and calendar views on dashlets
* Add support to use m2o fields for kanban columns
* Allow to show additional details with ref-text widget
* Add single-select and improve multi-select widget with color support
* Add support for popup editor on cards and kanban views

  <details>
  A new attribute `edit-window` is added with the following values:

  * `self` - show editor in same tab
  * `popup` - show popup editor
  * `popup-new` - show popup editor for new records only
  </details>

#### Fixed

* Fix unnecessary requests for name values
* Fix validation error in editable grid
* Fix "Invalid or non-matching id" when committing edit in grid
* Fix meta loading deadlock when creating groups
* Fix group creation when generating computed views
* Fix custom view refresh issue in dashlet
* Fix embedded tomcat runner
* Preserve column order in data exported from charts
* Fix text value escape in grid widget
* Fix details-view with grouped grid not working
* Fix path resolution in view watcher under Windows
* Fix CSRF token cookie when using SSO
* Allow deletion of one-to-one on the non-owning side and change of owner
* Allow inline edit with grouped grid
* Fix no file in response when observing exports
* Fix view watcher on application's resources
* Don't show fallback characters instead of icons on slow network
* Fix editable grid issue in details-view
* Fix setting of owner of one-to-one
* Fix checkbox alignment in editable grid

## 5.3.2 (2020-04-16)

#### Features

* Limit parallel meta loading to maximumPoolSize
* Add support for providing webapp resources from modules
* Add support for registering static web resources

#### Fixed

* Fix backward-compatible authentication via login.jsp
* Fix compile classpath issue
* Fix context issue with js expressions on m2o fields
* Fix spurious onLoad execution from calendar and kanban views
* Fix new line confirmation in editable grid after ActionResponse#setValues
* Align kanban hilite colors with grid hilite colors
* Fix view watching with IntelliJ IDEA
* Wait for actions when committing changes in editable grid
* Fix embedded tomcat run task issue
* Fix large text field in editable grid widget
* Use permission filter in order to count attachments
* Context from grid row editor should use row record

## 5.3.1 (2020-04-15)

#### Breaking Changes

* In order to migrate User activateOn and expiresOn fields, use these SQL statements:
  
    ```sql
    ALTER TABLE auth_user ALTER COLUMN activate_on TYPE timestamp;
    ALTER TABLE auth_user ALTER COLUMN expires_on TYPE timestamp;
    UPDATE meta_field SET type_name = 'LocalDateTime' FROM meta_model WHERE meta_field.meta_model = meta_model.id AND meta_model.name = 'User' AND meta_field.name IN ('activateOn', 'expiresOn');
    ```

#### Changes

* Change User activateOn and expiresOn to datetime type

#### Features

* Check for active user on every pre-request

#### Fixed

* Fix CSS of calendar bubble content
* Fix menu title wrapping with tag
* Fix combining selection simple filters with custom filters
* Don't show kanban popover with empty content
* Fix NavSelect widget on integer selection
* Fix extension insert before in declaration order
* Fix Overview panel randomly still present despite having custom panel as first element
* Do not allow deleting tasks when scheduler is running
* Fix tomcat 8.5.51 issue caused by javax.el service discovery
* Fix unknown tracked field detection when using inheritance
* Fix target-name on dotted field after selection from grid
* Fix same duration widget mask being applied to subsequent fields

## 5.3.0 (2020-01-24)

#### Changes

* Remove licenseCheck from check dependencies
* Upgrade to Gradle 5.6.4
* Run license task on src files only
* Migrate LDAP to pac4j
* Refactor grid widget to improve inline edit experience
* Make button onClick attribute required
* Upgrade to Spotless 3.24.3
* Improve hotswap-agent support
* Upgrade to pac4j 3.8.3
* Upgrade to Guava 28.1

#### Features

* Increase custom field conditions limit to 512 characters
* Add canNew, canEdit, and canDelete to cards and kanban views
* Set default logger config for pac4j package
* Centralize properties from application.properties
* Display `conditionToCheck` and `moduleToCheck` on meta menu view
* Add Request#getUser() method that returns current session user
* Log request data at trace level
* Watch for view updates
* Improve display of forced password change
* Sort mail messages by most recent reply if any
* Only update visible tags
* Parallelize loading of models, views, and i18n with rollback per module
* Add tel link and pattern to phone widget
* Add direct basic authentication client
* Add position "inside" for view extensions
* Add support for groupBy on custom models
* Add hideIf and showIf support to grid buttons
* Add responseBindingType to SAML configuration
* Ability to toggle chart legends and ellipsed labels with tooltip
* Add hideLegend config to charts
* Add support for NavSelect widget on many-to-one fields
* Scan for css/js files to minify
* Trigger onChange on Enter key in simple fields

#### Fixed

* Fix 'x-' prefixed extra attributes on custom fields
* Fix Query#update with null value
* Re-throw exception on Birt report generation exception
* Fix JNDI data source not working
* Fix currency formatting with IE11
* Do not set WWW-Authenticate response header when request has no basic auth header
* Fix scope syncing on charts
* Fix inline-checkbox widget title wrapping issue
* Use styled checkbox for grid row selector
* Block the UI as early as possible on action call
* Fix Query#update when query has ORDER BY clause
* Fix Gantt view scrolling
* Fix untranslated namecolumn when fetching missing values
* Fix m2o field in grid not showing translated value
* Fix dashlet refresh issue
* Update menu tags using `tag-count`
* Fix tag-select widget's search field width issue
* Fix popup editor issue when a tab is opened from it
* Fix widget name check inconsistencies
* Allow empty panel inside a panel
* Fix migration of existing views to extension views
* Fix Query#update with several fields
* Fix archived records not displayed when simple filter and advanced filter are applied
* Fix redirection to originally requested URL
* Fix dashlet refresh issue
* Remove deprecated api usage from Logger injection support
* Fix wrong redirection to favicon.ico
* Fix table layout on field editors
* Fix month variable replacement in app settings value
* Fix multi-tenancy with pac4j
* Fix tests
* Fix error popup with empty message not showing up in prod mode
* Remove showTitle from panel-related
* Fix query filtering on collections and using order by
* Fix duplicate m2m item issue
* Fix dotted fields setting unwanted intermediate records
* Silent requests should not hide loading indicator
* Fix ajax login should re-execute pending requests
* Fix target-name of a custom field in grid view when visibleInGrid is not used
* Fix issue caused by empty string value on decimal field
* Fix untranslated namecolumn value in TagSelect
* Fix collection fields with editor validation issue
* Fix relative style/script source in JSP
* Fix popup editor issue when a tab is opened from it
* Fix selecting a m2o on editable grid when server is slow
* Fix SAML postLogoutURL for webapps deployed at root
* Fix DMS file being automatically downloaded on form view

#### Security

* Fix XSS vulnerability with html widget
* Add CSRF protection using pac4j CSRF authorizer

## 5.2.1 (2019-09-19)

#### Features

* Add `generateChangelog` gradle task to generate final CHANGELOG from unreleased entries.
* Log tracking of unknown fields during code generation

#### Fixed

* Fix distinct query issue when search is done on o2m/m2m
* Prohibited usage of unsupported xml attributes in grid fields.
* Invalid meta and translations when restore is done

#### Security

* Fix security issue in criteria filter, Query and json function

## 5.2.0 (2019-09-16)

#### Enhancements

* Upgrade to Shiro 1.4.1
* Add support for SAML2
* Add support for OpenID Connect
* Log authentication failure
* Add support for sidebar panels with custom models
* Add support for customer field type spacer
* Add "auth." prefix to authentication-related configurations
* Implement CAS via pac4j
* Use view.menubar.location instead of application.menu
* Add support for more CAS client types
* Improved custom field views
* Do not generate default value if default="" is given
* Add support for OAuth
* Allow to set target-name attribute on custom model fields
* Use woodstox StAX API for data import
* Improve tracking message formatting
* Add support for toolbar and menubar in kanban view
* Prohibited usage of some unsupported editor attributes
* Add domain field name validation for few more reserved names
* Add x-big and x-seconds attributes
* Configure XStream security framework
* Add scale attribute to chart series
* Clear the persistence context after job is executed
* Add /ws/public/\*\* as anonymous rest endpoints
* Use icon if user profile image is not set
* Have boolean-radio behave like radio-select
* Restrict x-direction values to "horizontal" and "vertical"
* Improve Meta Scheduler views and usage
* Add x-accept support to specify file type filters

#### Bugs

* Skip linked bindings when finding observers
* Fix json field ordering issue
* Fix unable to open form from grid dashlet
* Fix calendar view color issue
* Fix BinaryLink & Image widgets with custom json models
* Fix advanced search on transient fields
* Fix showIf expression on custom o2m field issue
* Fix button in custom model grid
* Fix readonlyIf on button of custom model grid
* Fix downloading of meta files in JSON fields
* Fix EntityHelper#hashCode inconsistent with generated entities
* Fix toolbar buttons remains highlighted on view switch
* Center mail message avast image
* Fix grid selected rows (exclude group rows)
* Fix display time on calendar
* Fix hilite expression parsing issue
* Fix pagination issue caused by use of query cache
* Fix pagination issue when searching on collection fields
* Fix file name encoding when upload DMSFile
* Fix random view if view by name not found
* Fix initParam with field override
* Fix img-button css
* Fix dotted fields issue
* Fix image widget reload issue
* Do not show concurrent updates error on missing reference
* Fix html widget empty value issue
* Exclude archived records from tag-count
* Fix empty PDF tab with Chrome
* Extract title attribute in extensions for i18n
* Fix kanban view tooltip placement issue
* Fix redirect issue with https proxy
* Fix nested editor issue with canSelect=false
* Fix ImageSelect widget regression
* Fix translation value of translatable m2o name field is not reflected
* Fix old-style view extensions when base view has panel-mail
* Fix advanced search state sharing on card and grid views
* Fix if condition on help element of grid view not working
* Fix change tracking emails having null values
* Fix csv import on collection fields
* Fix xml import on collection fields
* Allow non csv column in local context values
* Fix xml import on collection fields

#### Breaking Changes

* View lookup: if a view with a specified name is not found,
  no view is now returned, instead of returning another unpredictable view.
* All authentication-related configurations are now prefixed with "auth.".
  For instance, previous "cas.\*" configurations are now named "auth.cas.\*".
* `x-direction` attribute (used with `boolean-radio` and `radio-select` widgets)
  is now restricted to either "horizontal" or "vertical".

## 5.1.0 (2019-06-28)

Check the `5.1.0-rc1` and `5.1.0-rc2` Changelog for complete list of changes.

#### Enhancements

* Only log when non-existing field is referenced in expressions (breaking change)
* Properly handle expression errors
* Prevent calling arbitrary methods with action (breaking change)
* Add app startup/shutdown events

#### Bugs

* Fix value assignment in EL expression
* Fix duplicate results in number of attachments

#### Breaking Changes

* Calling arbitrary methods from action-method or with `call:` is not allowed.

  All such methods should be annotated with `@CallMethod` annotation (`com.axelor.meta.CallMethod`).
  Use following shell command to find all the method calls in your code base:

  ```
  $ grep -P "(expr)(\s*=\s*)(\"call:([^\"]+\([^\"]+)\")" -r * -oh --include="*.xml" \
    | cut -d\" -f2 \
    | sed -E 's|call:\s*||g' | cut -d\( -f1 | sort -u
  ```

* Scripting expressions in xml actions are now not silent on errors.

  All errors during expression evaluation except missing attribute error are propagated to the user
  so evaluation of such expressions will fail and ultimately actions too.

* DMS permissions (requires manual intervention)

  DMS permissions are created and removed recursively for all children documents/folders.
  The `perm.dms.file.__parent__` permission is no longer used. Also, DMS permissions are readonly
  once created, but can be removed.

  Remove DMS file related permissions (permissions with a name starting with `perm.dms.file`).
  Those permissions will be recreated and updated after adding new DMS permissions.
  Remove and add back DMS permissions on your documents from the DMS view.

* The `context.appLogo` method should now use helper `MetaFiles#getDownloadLink` instead of
  returning `MetaFile`.

* On ManyToOne fields, `canEdit` attribute is `false` by default now.

* The `freeSearch="name"` to search on name field should be changed to `freeSearch="actualNameField"`

* The `cachable` attribute on `entity` definition is now deprecated and replaced with `cacheable`.

  ```
  $ find -iregex ".*domains.*.xml" | xargs -l sed -i 's|cachable="|cacheable="|g'
  ```

## 5.0.16 (2019-06-28)

#### Enhancements

* Add support for base64 encoded images with mail builder api
* Set Monday as first day of week in calendar view for French locale
* Improve date formatting in calendar view for French locale

#### Bugs

* Fix calendar view layout issues
* Fix dms permissions preventing attachments
* Fix grid row selection issue when deleting o2m/m2m items
* Fix wrong context with grid button after 'save' action
* Fix fetch request data serialization issue caused by rollbacked transaction
* Fix js expressions with dummy not evaluated inside field editors
* Fix named width styles not working
* Fix double escaping of html chars in grid widget
* Fix grid rendering issue caused by page change from form view
* Do not fetch archived records in tree view
* Fix x-can-copy issue (unable to copy if parent is now saved)
* Fix NestedEditor issue when name field is missing

## 5.0.15 (2019-05-31)

#### Enhancements

* Upgrade to hotswap-agent 1.3.0
* Improve help popover

#### Bugs

* Fix deprecated nested editor issue (for legacy use cases)
* Fix tracking message formatting issue
* Fix lost changes issue with child grid
* Fix change tracking clean up issue (transaction rollback should discard tracking)
* Fix o2m list editor layout (IE11 issue)
* Fix placeholder color (IE11 issue)
* Fix no scrollbar in popup editor (IE11 issue)
* Patch jquery for possible XSS vulnerability (jquery/jquery#2432)
* Patch jquery for CVE-2019-11358
* Fix boolean-radio widget on chrome
* Fix file handle not closed issues

## 5.1.0-rc2 (03-06-2019)

#### Bugs

* Fix lost changes issue with child grid
* Fix use of "member of" in domain expression
* Fix duplicate DMS file results when several DMS permissions are matched
* Fix hiding of error message div with IE

#### Enhancements

* Upgrade to hotswap-agent 1.3.0
* Set Monday as first day of week for French locale
* Improve date formatting in calendar view with locale fr
* Check for permission when downloading files
* Validate decimal's scale and precision attributes

## 5.1.0-rc1 (22-04-2019)

#### New Features

* New event system similar to CDI 2.0 event api
* Support for JPA event listeners
* Complete re-write of view extensions

#### Enhancements

* Major refactoring in auth api
* Improved xml view handling
* Prevent initial data fetch in grid view with `x-no-fetch="true"`
* Support for custom action on chart dashlet with `onAction` attribute
* Improved user preferences view
* Add support for forcing user to change password
* Logout user if password is changed
* Add ability to specify free search fields
* Improve DMS loading performance
* Add menubar to cards view
* Support for sharable DMS file URLs
* Delete attachments when record is deleted
* Check for DMS permissions when making zip
* Give and remove DMS permissions recursively
* Add image and pdf preview into DMS file form
* Only generate menu for custom models if menu title is provided
* Support for settings form width for custom model
* Support for setting column sequence for custom model
* Support for 'orderBy' on custom models
* Support for label fields in custom model
* Support for field permission on custom model and custom fields
* Support for font-awesome icons in image-select widget

#### Bugs

* Fix tag-select widget issue with custom fields
* Fix @RequestScoped services in unit tests
* Fix date search in grid view when user is in different time zone
* Fix I18n message bundle cache causing wrong bundle update
* Fix DMS file rename issue
* Fix @RequestScoped services when used with quartz scheduler

## 5.0.14 (2019-04-17)

#### Enhancements

* Add support for changing selection-in attribute with action-attrs
* Hide attachment icon when we can't attach a file

#### Bugs

* Fix panel-tabs visibility issue
* Fix auto fill parent field on new record on gantt view
* Fix track message textbox not clearing on new record
* Fix Column filters not applied with advance search
* Fix unexpected dirty record warning when navigating form records
* Fix popover width
* Fix o2m permission issues
* Fix onLoad issue caused by json fields
* Fix empty value in html widget with firefox
* Fix dotted field not loading
* Fix translated text gets escaped

## 5.0.13 (2019-03-22)

#### Enhancements

* Use fixed width columns in kanban view
* Add support to hide kanban columns using view-param
* Add support for multiline widget attribute to custom fields

#### Bugs

* Fix editable grid updating wrong record
* Fix i18n message extractor updating catelogs with wrong translations
* Fix required field in editor causing infinite fetch requests
* Fix NPE when using quartz job context
* Fix @RequestScoped services in unit tests
* Fix @RequestScoped services when used with quartz scheduler
* Fix data export issue from panel-dashlet
* Fix menu search
* Fix advance search issue with contains/not contains filter

## 5.0.12 (2019-01-31)

#### Enhancements

* Show exception message in prod mode (but no stacktrace)
* Add support for summary popup on kanban cards
* Add support for preventing initial data fetch in grid view (x-no-fetch="false")
* Remove kanban view restriction of max 6 columns

#### Bugs

* Fix DMS file rename issue when file name contains single quote
* Fix RefSelect sometime doesn't use configured views
* Fix kanban view scrolling
* Fix xml view validation issue

## 5.0.11 (2019-01-15)

#### Bugs

* Fix panel-tabs visibility issue
* Fix selected row flag reset issue
* Fix pagination issue in dms view

## 5.0.10 (2018-12-21)

#### Bugs

* Fix typo in Query#fetchStream methods
* Fix boolean-radio widget issue
* Check for parent to determine if a widget is hidden (#33)
* Fix I18nBundle initialization issues
* Fix idle in transaction when using quartz
* Fix pending data import of action menus not resolved 

## 5.0.9 (2018-11-28)

#### Bugs

* Fix record copy api
* Fix `requiredIf` is not applied if used with `showIf`
* Fix filter input focus issue in grid dashlet
* Fix required field clear issue in editable grid
* Fix dialog overlay opacity
* Fix column sizing issue in popup

#### Enhancements

* Allow duplicating unsaved row in o2m/m2m
* Support for `sortable` attribute on grid view
* Support for `sortable` attribute on grid view fields

## 5.0.8 (2018-11-06)

#### Bugs

* Fix view popup is now opening if first view is grid
* Fix technical info popup doesn't show value of m2o fields on o2m editor
* Fix grid column size issue in popup
* Fix server error dialog from popup is not visible
* Fix grid widget row selection issue
* Fix context update issue with panel-dashlet
* Fix invalid session error on system info page
* Fix MS-Edge issue
* Fix integer value formatting in track messages
* Fix grouped grid alignment issue in modern theme
* Fix m2o field validation issue
* Fix dirty record issue on copy
* Fix number widget increment issue
* Fix button focus style issues
* Fix calendar view doesn't use predefined filters
* Fix page size input/button alignment
* Fix translation extract issue
* Fix default values on custom models
* Fix default values in custom forms
* Fix unnecessary scrollbar in mailbox view
* Fix filter input focus issue in grid dashlet

#### Enhancements

* Allow to open popup in maximized state (use `popup.maximized` view param)
* Bring back `view.confirm.yes-no` config
* Add MultiSelect widget support in grid view
* Refresh kanban view when moving card fails
* Use special user form view from message link (user-info-form)
* Improve upload progress popup
* Improve sidebar menu UX

## 5.0.7 (2018-10-05)

#### Bugs

* Fix regression caused by RM-13705

## 5.0.6 (2018-09-14)

#### Bugs

* Fix ZonedDateTime adapter
* Fix encrypted field migration
* Fix dummy values from field editor missing in context
* Fix dummy fields issues in relational field editor
* Fix homeAction field in User m2o editor
* Fix full send in message details
* Fix dotted fields in field editor causing form dirty
* Fix onNew with save action
* Fix sidebar style conflict issue with field editor
* Fix contextual custom field with hidden=true
* Fix xml view hot reload
* Fix onSave/onLoad actions on custom models
* Fix view toolbar visibility on side change
* Fix translation popup for multiline text fields
* Fix unnecessary fetch request for dummy fields
* Fix scroll position issue on grid view when switching views

#### Enhancements

* Order followers with their namecolum
* Open the Inbox mail view instead of the unread mail view
* Prevent sorting on dummy fields
* Improve modern theme
* Terminate pending actions if view is switched
* Reset form view when switched over with browser back action
* Do not load record in form view if view is switched
* Do not add grid/form views when opening view with action

#### Others

* Adopt new style guide (google java format, two spaces for indentation)

## 5.0.5 (2018-08-03)

#### Bugs

* Fix advance search popup not hiding on navbar click
* Fix issue with custom filter sharing
* Fix translations
* Fix selection widget issue when value has html escape values
* Fix validation issue on date widget
* Fix form layout issue
* Fix extra scrollbar with html view
* Fix route change issue with html view
* Fix tab refresh issue on tree view
* Fix class path scanner issue with duplicate classes from bootstrap loader

#### Enhancements

* Add support for domain filter blacklist pattern

  ```
  domain.blacklist.pattern = (\\(\\s*SELECT\\s+)|some_function
  ```

  The old `domain.allow.sub-select` settings is removed in favor of this one.

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
