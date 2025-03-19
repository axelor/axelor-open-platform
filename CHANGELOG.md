## 7.3.3 (2025-03-12)

#### Fix

* Revert json field processing improvements

  <details>
  
  Previous version introduce json field processing improvements. Due to the nature of the change, this introduced 
  issue in json fields behavior. This has been reverted and will be re-added on further version.
  
  </details>


## 7.3.2 (2025-03-12)

#### Feature

* Add support to find value enums

  <details>
  
  A new `JpaScanner#findEnums` method added to find all value enums in the classpath.
  
  </details>

* Improve rendering phase

  <details>
  
  A change in an internal hook, by using `queueMicrotask` instead of `setTimeout` improve queue task execution. This 
  avoids unnecessary re-rendering if the callback updates state (mostly if used when fetching data).
  
  </details>

* Improve initial logo loading

  <details>
  
  Caught NPE when first loading logo seems to cause initial delay.
  
  </details>

* Improve json field processing

  <details>
  
  Contextual fields are filtered out in advance so that json fields aren't processing every time at rendering.
  
  </details>

#### Fix

* Prevent closing user home action tab
* Fix time widget format

  <details>
  
  Display formatted value of time widget in readonly. This will 
  take into account if the field should display the seconds.
  
  </details>

* Fix data export directory not multi-tenancy compatible

  <details>
  
  The data export directory is not multi-tenancy compatible. It should follow the multi-tenancy data upload 
  directory structure by including the tenantId as child path.
  
  </details>

* Fix auto save data in expandable tree-grid
* Fix add record in editable panel-dashlet grid

  <details>
  
  When we add new record in grid then if we try to add more subsequent record by clicking
  again on `+` button by expecting to commit current edited row and add new row. But it
  was adding/commiting duplicate records.
  
  </details>

* Fix MetaPermissions.canWrite permission check

  <details>
  
  MetaPermissions.canWrite should check CAN_WRITE instead of CAN_EXPORT
  
  </details>

* Revert back default step to 1 for decimal fields

  <details>
  
  On decimal fields, the default step was determined by the current scale of the field. If a field with scale set to 2, 
  default step would be 0.01. As it haven't been adopted, it reverts back previous behavior, with a default step to 1 
  for decimal fields. So it is no more allowed to have a step depending on the dynamic scale of the field.
  
  </details>

* Fix pass context params in action context
* Fix action on o2m/tree-grid setting dirty
* Hide attachment icon for custom form editor popup and not saved record

  <details>
  
  On not saved records in form editor popup, it shouldn't show the attachment icon.
  Same for some custom form editor popups like user preferences.
  
  </details>


## 7.3.1 (2025-02-05)

#### Feature

* Add support for OpenAPI scan include/exclude classes/packages

  <details>
  
  Introduced new configuration properties to include/exclude classes/packages from OpenAPI scanning with following 
  rules : class-level settings take precedence over package-level settings, exclusions take precedence over inclusions 
  and the longest matching rule applies for packages. 
  By default all classes are scanned (allow all, exclude some), except if some inclusions are defined (exclude all, 
  include some).
  
  Added properties: `application.openapi.scan.exclude.packages`, `application.openapi.scan.exclude.classes`, 
  `application.openapi.scan.packages`, `application.openapi.scan.classes`
  
  </details>

* Support application settings application.sign-in.logo-dark

  <details>
  
  Set a custom dark logo for the login page if require a different one from `application.logo-dark`.
  
  </details>

* Allow to overwrite entity `logUpdates` attribute

  <details>
  
  This allows to overwrite `logUpdates` attributes of entities. It can only enable 
  change tracking feature for an entity.
  
  </details>

* Display notification if the audit log isn't available for an entity

  <details>
  
  Entities with `logUpdates="false"` doesn't have change tracking enabled. So the 
  `created(On|By)` and `updated(On|By)` fields aren't available. When showing audit log popup,
  rely on `createdOn` field to check if change tracking is available for the entity. If not, 
  display a notification instead of showing a popup with empty data.
  
  </details>

* Support application settings application.logo-dark and application.icon-dark

  <details>
  
  `ws/public/app/logo` and `ws/public/app/logo` accept `mode` query parameter (`light` or `dark`).
  Application configuration `context.appLogo` and `context.appIcon` accept `String mode` parameter.
  Removed logo and icon from `ws/public/app/info`.
  
  </details>

#### Change

* Use theme-builder as form widget

  <details>
  
  Previously, theme-builder used in template with custom component support.
  Now theme-builder is implemented as form widget and can used in form-view directly.
  
  </details>

#### Fix

* Fix relational field target-name support in grid-view
* Fix save action handler in details-view
* Upgrade Swagger from 2.2.26 to 2.2.28

  <details>
  
  This fix API Swagger documentation error due to jackson version mismatch
  
  </details>

* Fix close dms popup on file open in tab

  <details>
  
  When dms popup view is opened through custom process action-view then
  on clicking on file to view in tab, it should close popup view (same as 
  attachment click behavior on form-view)
  
  </details>

* Fix validate action in form view
* Fix gap between line and summary-view in tree-grid
* Fix event injection from child constructor

  <details>
  
  When constructor injection is used, search for events in the injection point class hierarchy.
  
  </details>

* Fix pass action context to calendar view action
* Fix refresh record in details-view

## 7.3.0 (2025-01-14)

#### Feature

* Add canNew/canEdit/canDelete support for panel-dashlet

  <details>
  
  - canEdit param : canEdit="true" is already working for grid view,
  which signify editing of dashlet grid is allowed in readonly mode as well.
  canEdit="false" will disabled creation/editing of row in grid. 
  For other data views it will enable/disable edit option.
  
  - canNew/canDelete : it will enable/disable new/delete option for data view.
  
  </details>

* Add Barcode widget

  <details>
  
  This widget provides the ability to display a `string` value as a barcode in readonly mode.
  To customize the `Barcode`, use optional attributes `height`, `x-barcode-width`, `x-barcode-line-color`, 
  `x-barcode-display-value`, `x-barcode-background-color` and `x-barcode-format`.
  The barcode is only displayed if the value is accepted by the format.
  
  `Barcode` is also supported on viewers with the same attributes.
  
  </details>

* Add x-step to Integer and Decimal widgets

  <details>
  
  Add the ability to customize the increment and decrement amount using `x-step` attribute.
  Decimal and negative values are accepted. The default value is determined by the scale attribute if it exists, otherwise it is set to 1.
  
  </details>

* Add editable support in grid dashlet

  <details>
  
  The linked grid view should have `editable="true"` to allow edit records in
  grids. To disable this behavior, you have to pass `canEdit="false"`.
  
  </details>

* Improve mail message form

  <details>
  
  On MailMessage form, render recipient as SelectionTag. Allow to remove recipient 
  from Tag.
  
  When adding followers, validate recipients is filled, else it will display warning 
  notification. Without any recipients, nothing will be done. The process only send 
  message to the newest recipients.
  
  </details>

* Add QrCode widget

  <details>
  
  This widget provides the ability to display a `string` value as a QR Code in readonly mode.
  To customize the QrCode size in px, use `height` attribute with a number value. Default to 140.
  
  QrCode is also supported on viewers with attribute `value`.
  
  </details>

* Add support to hide view popup header and footer

  <details>
  
  Two new view params are added: `popup.show-header` and `popup.show-footer`.
  
  This is for advance use cases where we want to show `custom` views
  inside a popup with it's own header and footer.
  
  </details>

* Introduce help widget `variant` attribute

  <details>

  The help variant is extracted from the `css` attributes values. Now, help widget has a new attribute `variant` that
  can be used, Accepted values are `info`, `success`, `warning` and `error`. Default value is `info`.

  </details>

#### Change

* Upgrade Gradle from 8.7 to 8.11.1
* Upgrade backend dependencies

  <details>
  
  Here is the list of backend dependencies upgraded : 
  
  - Upgrade ASM from 9.7 to 9.7.1
  - Upgrade byte-buddy from 1.14.17 to 1.15.10
  - Upgrade commons-cli from 1.7.0 to 1.9.0
  - Upgrade commons-csv from 1.11.0 to 1.12.0
  - Upgrade commons-io from 2.16.1 to 2.18.0
  - Upgrade GraalJS from 22.0.0.2 to 22.3.5
  - Upgrade guava from 33.2.1-jre to 33.3.1-jre
  - Upgrade groovy from 3.0.22 to 3.0.23
  - Upgrade hazelcast from 5.3.7 to 5.3.8
  - Upgrade jackson from 2.17.1 to 2.18.2
  - Upgrade hsqldb JDBC from 2.7.3 to 2.7.4
  - Upgrade postgresql JDBC from 42.7.3 to 42.7.4
  - Upgrade jsoup from 1.17.2 to 1.18.3
  - Upgrade junit5 from 5.10.3 to 5.11.3
  - Upgrade junit-platform-launcher from 1.10.3 to 1.11.3
  - Upgrade pac4j from 5.7.5 to 5.7.7
  - Upgrade Quartz from 2.3.2 to 2.4.0
  - Upgrade Redisson/Hibernate 5.3.x+ from 3.29.0 to 3.38.1
  - Upgrade slf4j from 2.0.13 to 2.0.16
  - Upgrade snakeyaml from 2.2 to 2.3
  - Upgrade swagger-jaxrs2 from 2.2.22 to 2.2.26
  - Upgrade tomcat from 9.0.90 to 9.0.97
  - Upgrade undertow from 2.2.33.Final to 2.2.37.Final
  - Upgrade woodstox-core from 6.6.2 to 6.7.0
  - Upgrade xstream from 1.4.20 to 1.4.21
  
  </details>

#### Deprecate

* help widget `css` attribute deprecated

  <details>
  
  The help variant is extracted from the `css` attributes values. Now, help widget has a new attribute `variant` that
  can be used, `css` attribute usage to determinate the variant is deprecated.
  
  </details>

#### Fix

* Fix switch select widget selection issue
* Fix number increment/decrement when changing sign

  <details>
  
  On number fields, when changing sign, 0.5 was decremented to -1.5 instead of -0.5.
  
  </details>

* Add toolbar support in grid details view
* Fix lost focus on editable grid discard
* Translate boolean fields values in mail message email
* Fix gantt line alignment

  <details>
  
  The gantt line should be aligned to table task row.
  
  </details>

* Display personal name in recipient field of MailMessage form
* Fix task sequence support in gantt view
* Fix fetch editor nested related fields

  <details>
  
  When any nested field of editor field is defined in form then 
  it should fetched as editor field instead of form field.
  
  </details>

* Fix search emails address in recipient field in MailMessage form by adding current selected emails
* Format multi-select fields values in mail message
* Fix focused tab with showIf in panel-tabs

  <details>
  
  On a form view, when first tab of panel-tabs contains showIf/hideIf expression,
  so initially it will be hidden and staring focus will be on tab which doesn't contain
  showIf/hideIf expression.
  In this case, it should re-focus active tab to starting tab if starting tab is accessible.
  
  </details>


## 7.2.5 (2025-01-08)

#### Feature

* Allow TenantAware to not start transaction by default

  <details>
  
  Allow TenantAware to not start transaction by default.
  
  This is required when the thread task need to manage
  multiple transactions on it's own.
  
  </details>

#### Fix

* Fix downloading of DMS file with content instead of meta file

  <details>
  
  This fixes downloading of DMS file with string content, that are not associated with a meta file.
  
  </details>

* Fix default min/max size of json fields

  <details>
  
  Doesn't provide default min/max size to 0 for json fields. It is up to the user 
  to define the values.
  
  </details>

* Add support for Mayotte in phone widget
* Fix persist query string params in tab url

  <details>
  
  When app is opened through url let say search-view url like this {demoURL}/#/ds/sale.search/search?customer=Miss then
  query parameters should be persist with url and also when switching between tabs it should also
  restored query string url for that associated tab.
  
  </details>

* Fix use context for toolbar button for panel-dashlet

  <details>
  
  It should use context of dashlet action view to evaluate expression like showIf/hideIf/readonlyIf defined on toolbar button instead of tab action view context.
  
  </details>

* Fix X-Forwarded-Host that may contain port

  <details>
  
  X-Forwarded-Host may contain port number, so server name should strip out the port from X-Forwarded-Host.
  It also means that port can either come from X-Forwarded-Host or X-Forwarded-Port.
  If both X-Forwarded-Host and X-Forwarded-Port are present, port from X-Forwarded-Port will take precedence.
  
  </details>

* Fix search view error when using search-form
* Fix pass _ids in relational field grid
* Fix hidden/readonly attr on view toolbar button

  <details>
  
  When hidden="true" is set on menubar item or toolbar button it should hide the item or
  when readonly="true" is set on item, it should make item readonly.
  Currently hidden/readonly is only working when any of showIf/hideIf/readonlyIf is also defined on item.
  
  </details>

* Fix logout with form submission cases

  <details>
  
  When extending, some logout (ex, SAML central logout) is typically 
  not a redirection but returns a form that the front-end needs to submit.
  
  Fixed by using full page request for logout instead of AJAX.
  
  </details>

* Pass context to editable grid onNew action
* Fix skip passing fields in form record save
* Fix set action attrs on reference field
* Fix placeholder support in html widget
* Fix html widget sanitize issue

## 7.2.4 (2024-11-29)

#### Fix

* Fix SwitchSelect widget overflowing in vertical mode
* Fix untranslated buttons titles in view switcher toolbar
* Fix missing support of colors and shades config in charts

  <details>
  
  Colors and shades config support was not implemented.
  This also add missing pre-build set of colors : `material`, `roma`, `chartjs`, 
  `roma`, `macarons`. For example : `<config name="colors" value="roma" />`
  
  </details>

* Fix save record in popup editor for m2o/o2o/m2m(selection)

  <details>
  
  We can edit the record through popup editor, it will save record as following :
  - m2o/o2o/m2m(selection) : It will only save record when form is dirty
  - o2m/m2m(grid) : It will save record when any changes (also including non dirty dummy fields) in record
  
  </details>

* Fix exclude duplicate record in data store search
* Fix cache a view/fields when data is exist
* Show error when view is not found
* Allow to generate empty changelog release

  <details>
  
  If no changelog entries are found, there is no changelog release generated. This 
  is the default behavior. New properties `allowNoEntry` and `defaultContent` can 
  be used to determine if it is allowed to generate changelog without entries and 
  to specify the changelog release content (for example, `No notable changes`).
  
  </details>

* Fix Slider tooltip position

  <details>
  
  The position of the tooltip is now aligned with the slider thumb even after scrolling or zooming.
  
  </details>

* Add server-side view type mismatch check

  <details>
  
  When a view is requested, check if the requested view type matches.
  If not, an error message is logged and no view is returned.
  This prevents front-end from processing views with wrong type and causing unexpected errors.
  
  </details>

* Fix onSave action in grid details view
* Fix helper tooltip not accepting html elements
* Fix Quick Menu dynamic width

  <details>
  
  Quick menus no longer have a dynamic width so typing on text input is not flipping the menu anymore.
  
  </details>

* Fix rendering of object values in grid
* Small changes to refine UI
* Fix dirty record on MetaFile widgets

  <details>
  
  Some MetaFile widgets, ie Image/Drawing/BinaryLink shouldn't mark 
  the main record dirty when updating an existing associated file.
  
  </details>

* Fix grid resizing lags
* Enhance Gantt toolbar actions style
* Fix login popup after expired SSO profile

  <details>
  
  SSO profile may expire before the session expires, creating a situation where session exists
  with no valid profile. In that case, login popup could appear instead of being redirected to SSO.
  
  Now, log out subject when profile expires.
  
  </details>

#### Security

* Fix XSS vulnerability

## 7.2.3 (2024-11-13)

#### Fix

* Fix ImageSelect alignment in tree view
* Fix JS TypeError when action adds rows in one-to-many widget

  <details>
  
  Page would crash (error 500) in case an action adds rows in one-to-many widget.
  
  </details>


## 7.2.2 (2024-11-12)

#### Feature

* Expose Rating to be used in template

#### Fix

* Fix set action attrs on editor fields
* Fix handle integer value in selection widgets

  <details>
  
  When field type is integer/long in case of selection widgets then it should
  set value in record as integer.
  
  </details>

* Fix onChange not triggered on Rating widget
* Fix populate changes should reflect into collection grid
* Fix pass grid name/type in editable grid action context
* Fix o2m items data conflict issue

  <details>
  
  When data is reset through action-record or data is updated through popup
  then updated data should be merged properly to grid data.
  
  </details>

* Fix show `Display process` option in form toolbar menu
* Fix x-bind string case sensitive issue

  <details>
  
  When x-bind output of string field remains same for same field,
  it should reflect the output value instead of skipping value update.
  
  </details>

* Exclude cid field in default generated views
* Disable sorting when we add a row

  <details>
  
  After manual sort by column, added row could be at the top instead of the bottom.
  
  </details>

* Fix tag height in advance search causing toolbar visual effects
* Close popup when adding records in o2m edit mode
* Fix tree-grid empty summary view styles
* Fix handling 0 valued in selection widgets
* Fix Stepper clickable in read-only mode
* Fix missing i18n extract of report-box#label attribute
* Fix skip translations search for empty value
* Fix grid customization of fields mentioned several times

  <details>
  
  On a grid view definition, there may be several instances of the same field
  with different "if" conditions.
  
  When saving grid view customization, we need to retrieve all the fields with the same name,
  not just the first occurrence.
  
  </details>

* Fix orderBy for new rows after save/refresh on o2m grid

  <details>
  
  On o2m grid, order is normally preserved after search request.
  Now, search order is applied in case of form save/refresh.
  
  </details>

* Fix mark form dirty on editable m2m add
* Fix SwitchSelect widget not displaying many-to-one values
* Fix criteria on grid columns search
* Fix persistence issue during tracking of one-to-one field

  <details>
  
  Get new and old values without using context in audit tracker.
  
  </details>

* cid field shouldn't be copyable
* Fix close dropdown on tag click in tag-select
* Fix empty criteria in advanced search query

  <details>
  
  When field or operator is empty then that criteria should not send in search query.
  
  </details>

* Fix call onChange on m2m record change
* Fix pass view attributes in field action context
* Fix selection-in attribute support in mass update
* Fix child dialogs shown behind mass update and advanced search

## 7.2.1 (2024-10-17)

#### Feature

* Fix search bar value duplicated in other quick menus
* Always show tenant selection for non-hosts resolved tenants
* TagSelect x-color-field attribute is now compatible with hexadecimal color values
* Add /files/data-export?fileName and /files/report?link endpoints

  <details>
  
  Add files endpoints accepting filename as query param instead of path param.
  
  This ensures URIs are ASCII only, complying with Shiro InvalidRequestFilter.
  
  Files endpoints using filename as path param are kept for backward compatibility and may be removed
  in later versions.
  
  </details>

#### Change

* Re-enable Shiro global filters

  <details>
  
  Shiro global filters are re-enabled, now that our endpoints comply with
  Shiro InvalidRequestFilter (ASCII-only URIs).
  
  User endpoints also need to make sure they use ASCII-only characters in URI.
  
  </details>

#### Fix

* Fix version issue in editable m2m grid
* Fix ColorPicker popper to work even with invalid values
* Fix tenant selection at login when hosts are not specified

  <details>
  
  Session may exist even if user is not logged in.
  Tenant specified from login request should override any session tenant.
  
  </details>

* Fix data-description not translated on enum/selection
* Fix restore items state on save in form view
* Invalidate session when tenant becomes inactive
* Fix skip view dirty on editable m2m grid changes

#### Security

* Always rely on codes when fetching user from profile

  <details>
  
  Any extends of `com.axelor.auth.pac4j.AuthPac4jUserService.getUser` should takes 
  care to rely on fetch users by code only (instead of fetching by code and email).
  
  In case your application use SSO authentication, a carefully review is needed.
  As we now rely on users codes to retrieve users, make sure the users codes match 
  the user profile username or email provided by the identity provider (we rely on 
  pac4j user profile mapping for this). For example, OpenID Connect providers commonly 
  use `preferred_username` claim as username,but for others such as Azure OpenID Connect 
  provider, it will use the `upn` claim as username. As fallback is will use the `email` 
  claim as email. In case of existing users codes not matching identity providers username 
  or email, it will not retrieve them and users will not be able to log in. Manually change 
  will be needed, by updating users codes with their email for example.
  
  </details>


## 7.2.0 (2024-10-07)

#### Feature

* Add email widget support in grid view
* Add onDelete action support in panel-related

  <details>
  
  This action will be call when removing record in o2m/m2m grid.
  
  </details>

* Add summary view support in tree-grid
* Add error support on menus response

  <details>
  
  Beside notify and info response, this also add error response support on menus actions.
  
  </details>

* Support localization with IETF BCP 47 language tags and fall back to primary language subtags
* Add slider widget

  <details>
  
  This provides the ability to select a value within a range.
  It can be used on `integer` and `decimal` fields.
  
  </details>

* Add image field support in relational field

  <details>
  
  Now we can use x-image-field="fieldName" attribute to show image in
  m2o (selection) and m2m (tag-select) for both grid and form views.
  
  </details>

* Add onSave action support in editable grid view

  <details>
  
  This will support onSave for top level grid views and m2m collection grid only.
  
  </details>

* Add search support in image-select widget
* Add onCopy action support in form-view

  <details>
  
  This action will be execute after record is copied same as onLoad action in existing record.
  
  </details>

* Allow to configure client polling interval

  <details>
  
  Client poll menu tags each 10 seconds. On application with a large number of active users 
  and number of tags, this interval can lead to a number of performance issues.
  `application.polling-interval` property can be configured to define how often the client polls 
  in seconds. This isn't recommended to set a value lower than 10 seconds.
  
  </details>

* Set quick menus height to fix value
* Add stepper widget

  <details>
  
  This provides the ability to indicate progress through a multi-step process similarly to `NavSelect`.
  It can be used on `selection`, `enum` and `many-to-one` fields.
  
  </details>

* Improve advance search filters UI

  <details>
  
  Add search input to hide non matching filters. This will 
  provides better usage of filters in case many are displayed.
  
  </details>

* Add onCopy action support in panel-related

  <details>
  
  This action will be call after duplicating record in o2m/m2m grid,
  It will be used along with x-can-copy="true".
  
  </details>

* Add context in search view action

  <details>
  
  Now in search-view, when click on go button to execute the action,
  _searchContext is passed into action data context.
  
  Example : 
  _searchContext: {
  
    //All not null search fields
    code: 'A',
    product: {
     id: 1,
     name: 'P1',
     version: 1
    },
  
    //Selected search result ids group by model
    _results: [
      {
        model: 'com.axelor.contact.db.Contact'
        ids: [1, 2, 3]
      },
      {
       model: 'com.axelor.sale.db.Product'
       ids: [1, 2, 3]
      }
    ],
  
    //Context params: _view, _source, _action, ...
  }
  
  </details>

* Add react template support on Help widget
* Allow kanban onMove action to set values

  <details>
  
  In order to align with tree view node onMove action behavior,
  kanban onMove action values are now taken into account.
  
  </details>

* Add search-field support in dashboard

  <details>
  
  This allow to add search fields on top of the dashboard. Fields 
  can be filled when the dashboard loads with `onInit` event. Fields
  values will be add in context of all dashlets.
  
  </details>

* Enhance relative time widget display

  <details>
  
  Relative time widget now displays `Date` data fields in a more
  readable format: 'Today' if date is today, 'Tomorrow' if date is tomorrow,
  'Yesterday' if date is yesterday, 'dddd' (day name) if date is within next week,
  'Last dddd' if date is within last week, and as `DateTime` otherwise.
  
  Also provide support in grid/tree views as well as in formatter.
  
  </details>

* Add onDelete action support in data views

  <details>
  
  This will allow to define onDelete action support in grid, form, cards,
  kanban and calendar views.
  This will trigger actions before the delete process. Any errors or validations
  return during these actions should stop or/and suspend the execution.
  
  </details>

* Add shortcut to create new sub line in tree-grid

  <details>
  
  When line is in edit mode, we can create new sub-line through
  ctrl + enter to commit current row and add new sub line to it.
  
  </details>

* Add support to display help or title on grid header column

  <details>
  
  On grid header columns, the field help (fallback to column title) will be displayed 
  as tooltip on mouse over. This is convenient for column with long title but low width.
  
  </details>

* Save/Restore grid view state

  <details>
  
  When view is switched from grid, again back to grid then
  state should be restored.
  
  </details>

* Add color picker widget

  <details>
  
  This provides the ability to pick a color in a color picker popover for string data fields.
  Supports attributes `x-lite` to change the color picker to a basic color palette and `x-color-picker-show-alpha`.
  
  </details>

* Password reset functionality

  <details>
  
  Added built-in support for password reset functionality,
  allowing users to request a password reset link if they have forgotten their password.
  
  Available new properties:
  
  ```properties
  application.reset-password.enabled = true # (enabled by default)
  application.reset-password.max-age = 24 # (24 hours by default)
  ```
  
  </details>

* Add switch select widget

  <details>
  
  This provides the ability to pick a choice from a multiple-choice list.
  It can be used on `selection`, `enum` and `many-to-one` fields.
  It supports icons, `x-direction` and `x-labels` for hiding labels.
  
  </details>

* Add support to reset dummy field value on save

  <details>
  
  By default all dummy fields values are retain on save in form view,
  now with this option we can set x-reset-state="true" on dummy field 
  in order to reset it's value on save. By default it's false.
  
  </details>

* Implement tree-grid widget support for grid view

  <details>
  
  Add tree-grid widget support for grid view with some limitations that apply to first-level rows:
  
  - You can add a row to the bottom only, not between existing rows.
  - `Ctrl+Enter` to add subitem is not supported.
  
  </details>

* Provide ability to search/filter items in quick menus

  <details>
  
  When there are more than 10 items in quick menus, a search 
  input is display on top in order to search/filter the items.
  
  </details>

* Add support to display mail messages and followers on custom model
* Enhance toggle widget display in readonly
* Add views help link support

  <details>
  
  Add view help link support (based on `helpLink` attribute). This feature was present 
  on former version, but wasn't added during React migration.
  
  The help link button is now placed on end right side of the toolbar.
  
  </details>

* Add onMove node action support in tree-view

  <details>
  
  This action can be used to stop moving operation in tree view through sending errors or
  it can be useful to set some values before saving the node move modification.
  
  </details>

* Implement login customization

  <details>
  
  Add support for customizing the login page.
  
  application.sign-in.logo = url # absolute or relative url, to have a login logo different from `application.logo`
  application.sign-in.title = html # translatable sanitized html, shown after logo in form login panel
  application.sign-in.footer = html # translatable sanitized html, shown after form login panel
  
  application.sign-in.fields.username.show-title = true (default) | false
  application.sign-in.fields.username.title = translatable text # `Username` (default)
  application.sign-in.fields.username.placeholder = translatable text # default is empty
  application.sign-in.fields.username.icon = none (default) # Bootstrap or Material icon name or `none` to disable, shown as start adornment
  
  application.sign-in.fields.password.show-title = true (default) | false
  application.sign-in.fields.password.title = translatable text # `Password` (default)
  application.sign-in.fields.password.placeholder = translatable text # default is empty
  application.sign-in.fields.password.icon = none (default) # Bootstrap or Material icon name or `none` to disable, shown as start adornment
  
  application.sign-in.fields.tenant.show-title = true (default) | false
  application.sign-in.fields.tenant.title = translatable text # `Tenant` (default)
  
  # Extra buttons inside form login panel using custom button names
  application.sign-in.buttons.<button-name>.title = text
  application.sign-in.buttons.<button-name>.type = button (default) | link # use Button or Link component
  application.sign-in.buttons.<button-name>.variant = primary|secondary|success|danger|info|warning|light|dark # for button only
  application.sign-in.buttons.<button-name>.icon = icon_name # Bootstrap or Material icon name, shown before title
  application.sign-in.buttons.<button-name>.link = url # absolute or relative url with `:username` support
  application.sign-in.buttons.<button-name>.order = order # number relative to Login button (< 0 for before, >= 0 for after)
  
  # Use `submit` button name to customize Login submit button
  application.sign-in.buttons.submit.title = Sign in
  application.sign-in.buttons.submit.type = button
  application.sign-in.buttons.submit.variant = primary
  application.sign-in.buttons.submit.icon = none
  
  For translatable texts, you can add your translations to `custom_<language>.csv` files.
  
  </details>

* Add support for tracking custom fields
* Add icon, order, hidden and description support on enumeration.

#### Change

* Upgrade Guava from 33.2.0 to 33.2.1
* Upgrade Undertow from 2.2.32 to 2.2.33
* Login page layout changes

  <details>
  
  Login page has been reworked with the following changes:
  
  * Move tenant selection to top
  * Translatable copyright
  * Login button renamed to Sign in
  * Add "or" separator before SSO buttons
  * Remove unimplemented "Remember me" button"
  
  </details>

* Translations in CSV without values are not loaded anymore
* Upgrade Hsqldb from 2.7.2 to 2.7.3
* Upgrade Junit from 5.10.2 to 5.10.3
* Upgrade EclipseLink Moxy from 2.7.14 to 2.7.15
* Fix customize columns in grid view

  <details>
  
  When we do customize in grid view then 
  by default hidden columns should be exclude from the columns display list.
  
  </details>

* Update string validation pattern to be case sensitive
* Upgrade Pac4j from 5.7.4 to 5.7.5
* Use "Add subitem" as default tree field title
* Click on subline title to add new row
* User email now has unique constraint

  <details>
  
  You may execute this SQL statement for migration:
  
  ```sql
  UPDATE auth_user SET email = lower(email);
  ALTER TABLE auth_user ADD CONSTRAINT uk_auth_user_email UNIQUE (email);
  ```
  
  </details>

* Upgrade Tomcat from 9.0.88 to 9.0.90
* Upgrade Groovy from 3.0.21 to 3.0.22
* Upgrade Byte Buddy from 1.14.14 to 1.14.17

#### Remove

* Remove hibernate search support

  <details>
  
  We removed Hibernate search support (Full text search). The implementation was very basic and haven't
  been followed and improved since the initial implementation.
  
  Following properties aren't used anymore and can be deleted from properties file :
  
  * `hibernate.search.default.directory_provider`
  * `hibernate.search.default.indexBase`
  
  We are actively working on a new integration that will provide much more power and a better integration 
  in daily usage. Stay updated for the announcement in a future version.
  
  There is no replacement. Advance search feature can be used in order to achieve same goal.
  
  </details>

#### Fix

* Fix web services that can have request URI containing non-ASCII characters
* Fix commit new record on save in editable grid

  <details>
  
  When we create new edit row and commiting record directly through enter press,
  then it should commit row instead of discarding row.
  
  </details>

* Validate URL without escaping it on grid
* Fix formatting of enumerations in tracking messages
* Prevent going into edit mode when clicking on an URL on editable grid
* Fix add extra fields in customize dialog select
* Change expand icon in tree-grid widget

  <details>
  
  - Use `>>` for items which contains children items
  - Use `>` for items which doesn't have any children
  
  </details>

* Fix inline o2m widget
* Fix column sizing in tree-grid widget
* Fix delete confirmation on unsaved row in tree-grid
* Expose SLF4J API as Gradle api dependency
* Fix grid constraints on customize grid popup
* Fix eval expression on tree-grid collection widget

  <details>
  
  When we used showIf, hideIf, readonlyIf expression on tree-grid widget,
  so on sub line context evaluation parent context should be merged in order
  to evaluate those expression correctly.
  
  </details>

* Fix binary widget issues

  <details>
  
  - set form dirty on upload
  - uniform styles for binary and binary-link widgets
  - add title on button for native tooltip
  - check http code 200 on download and also remove image=true from url
  
  </details>

* Fix updating custom fields having roles
* Fix expand all in expandable grid widget
* Fix auto add new row in editable tree-grid
* Fix editor icons

  <details>
  
  Display edit icon only if `canEdit="true"` is explicitly defined in field (default m2o behavior).
  So the view icon will only be displayed if edit icon is not there.
  
  Also add tooltip on each button that describe the related action.
  
  </details>

* Fix preserve expandable state in collection field
* Improve expanded node behavior in tree-grid widget
* Fix action-attrs column attributes for tree-grid widget
* Fix hideLegend in chart view
* Fix show errors for custom fields
* Fix negative zero conversion

  <details>
  
  Decimal values between 0 and -1 do not become absolute.
  For example, -0.5 is no longer converted to 0.5
  
  </details>

* Fix sync values on refresh in collection grid
* Fix sync editable grid values with expandable form
* Fix use field name as default for x-tree-field in tree-grid
* Fix sync selection in duplicate collection field
* Fix duplicate widget ids that could cause RangeError
* Fix Rating widget on custom fields
* Fix translation and extraction of x-tree-field-title attribute
* Fix skip undirty check on collection line discard
* Fix o2m items version conflict when duplicating record

#### Security

* Ask to retype current password on change password page

  <details>
  
  Instead of passing current password in state after login, ask to 
  retype current password on change password page.
  
  </details>


## 7.1.5 (2024-08-08)

#### Change

* Always retain filters in grid view action context

  <details>
  
  On actions executed from grid views, we can fetch current filters applied 
  on the view using `request.getCriteria()`. it was initially only available if 
  there are no records selected. To be consistent, it should also be available 
  whatever records are selected or not. This shouldn't have any impact. Selected 
  records are available thought `_ids` in context, the current filter through 
  `request.getCriteria()`.
  
  </details>

* Model field preferred over custom field when setting value

  <details>
  
  When a custom field has same name of model field, action called from 
  form view was updating field in form but action called from json editor 
  was updating field in the json editor. This was creating confusion 
  depending on where the action was called. To uniformize behavior, form 
  field gets preference over custom field (if same name). This can be 
  breaking change, but use a custom field name same as the model field one 
  isn't recommended.
  
  </details>

#### Fix

* Fix title not displayed on custom collection fields
* Fix reload not triggered after notify if pending actions
* Improve reference field data for json fields
* Fix auto add new row in editable grid
* Fix prefer hideIf over showIf in expression evaluation

  <details>
  
  When widget defines both expression i.e. showIf and hideIf then 
  it will first eval hideIf expression, if it returns true then
  it is considered to be hidden true else it will take and eval result of showIf expression.
  
  </details>

* Fix set custom fields attributes

  <details>
  
  This fixes updating custom fields attributes in views. 
  
  Custom field that are part of the default `attrs` json field, attributes can be updated either without prefix 
  (`<attribute for="test" name="hidden" expr="eval: true"/>`) or without prefix 
  (`<attribute for="attrs.test" name="hidden" expr="eval: true"/>`), no matter where the action is triggered in the 
  view. This means that whether the action is triggered from a field event or a button in the main form or from a field 
  event or button inside a json field, it works same.
  
  For custom fields that are part of other json fields, attributes have to be updated with their respective prefix 
  (`<attribute for="myOtherJsonField.test" name="hidden" expr="eval: true"/>`) or if the action is executed inside the 
  json field, attributes can also be updated without prefix (`<attribute for="test" name="hidden" expr="eval: true"/>`).
  
  </details>

* Fix query domain on relational custom fields
* Fix call save only when record is changed in popup editor

  <details>
  
  When form contains dummy fields or x-dirty="false" items then when record is saved
  by clicking on ok, it should save record when those fields get changed regardless of
  form is not dirty.
  
  </details>

* Fix dirty issue for non-changed number value through action
* Fix js expressions and attributes priority

  <details>
  
  js expressions have the priority over attributes set with action-attrs.
  
  </details>

* Fix selection-in support for radio/checkbox select
* Fix hide columns through action-attrs in collection
* Fix ensure m2o value for json fields
* Fix kanban column title writing mode

  <details>
  
  When written vertically, multiline text should grow from right to left.
  
  </details>

* Fix details view should close on multiple selection of record

  <details>
  
  When multiple records are selected in grid view then
  details view should be not open and should be close if opened.
  
  </details>

* Fix canEdit/canView on TagSelect widget

#### Security

* Fix XSS vulnerability with message thread

## 7.1.4 (2024-07-18)

#### Fix

* Fix flashing issue on viewer in form view

  <details>
  
  Viewers are rendered when the form is ready, means that record is fetched. 
  This avoids flashing issue, especially with `Image` inside viewers.
  
  </details>

* Fix update custom fields

  <details>
  
  This fixes updating custom fields in views. 
  Custom field that are part of the default `attrs` json field can be updated either without prefix 
  (`<attribute for="test" name="value" expr="eval: "some""/>`) or without prefix 
  (`<attribute for="attrs.test" name="value" expr="eval: "some""/>`), no matter where the action is triggered in the 
  view. This means that whether the action is triggered from a field event or a button in the main form or from a field 
  event or button inside a json field, it works same. For custom fields that are part of other json fields, they have 
  to be updated with their respective prefix : `<attribute for="myOtherJsonField.test" name="value" expr="eval: "some""/>`.
  Both `action-attrs` and `action-record` are supported.
  
  </details>

* Fix grid view pagination

  <details>
  
  When we switch between grid to form and form back to grid, 
  first time prev/next was having no effect.
  
  </details>

* Fix search-fields panel frame in search view
* Fix set action attrs value with attribute
* Fix original value for json field in form view
* Fix popup should not open on click of expand in tree-grid/expandable
* Add expression attribute support in tree view button

  <details>
  
  Add support of readonly, hidden, hideIf, showIf, readonlyIf on tree-view button.
  
  </details>

* Fix entities updated/deleted in BeforeTransactionComplete observer

## 7.1.3 (2024-07-01)

#### Fix

* Fix padding in panel-tabs content

## 7.1.2 (2024-07-01)

#### Change

* Add application.home link in user dropdown

  <details>
  
  Starting from v7, the header logo doesn't rely on the `application.home` link anymore but on the user home 
  action if configured. `application.home` link is now added in the user dropdown menu as `Home page` item.
  
  </details>

#### Fix

* Fix reset some user dummy fields on save
* Fix zoom(scale) issue on mobile device
* Fix null operator on collection field in advance search
* Fix call search in search-view on tab refresh
* Fix disable dashboard customize for mobile
* Fix help widget display
* Fix selection search field in chart view
* Skip writing websocket IOException to log
* Fix collection master-details widget styles
* Add search-form support in search-view
* Fix remove on form image widget
* Fix dotted field lost on move in kanban view
* Fix restore filter on search in calendar view

  <details>
  
  In calendar view, when we select some user filters, then
  on change of month/week/day, it should be restored.
  
  </details>

* Improve tabs rendering

  <details>
  
  Instead of hiding tab on unselect or deactivate by seleting different tab,
  Then it will use visibility:hidden style instead of display: none.
  Visibility will keep container height/width, so no re-calculation of style
  on select or activate.
  
  </details>

* Fix handle error in action response
* Fix show/hide columns in tree grid
* Fix search view rendering performance issue
* Fix advance search bar position in toolbar

## 7.1.1 (2024-06-07)

#### Fix

* Implement client side sorting in grid
* Remove button on M2M only rely on canRemove attribute (not on the permission)
* Fix nav menu/tabs pre-build color names

  <details>
  
  This use pre-build color names hexadecimal code from `axelor-front` 
  instead of the ones defined in `axelor-ui`. Also provide sass variables 
  for all colors in order to re-use them.
  
  </details>

* Add missing widgets on custom fields widget selection
* Set AJAX resolver on all indirect clients

  <details>
  
  This fixes default SSO authentication request blocked
  because of CORS policy.
  
  </details>

* Fix calendar icon overlap on date
* Fix adding a row in editable grid with create permission only
* Check create permission in action if no id instead of write
* Merge response values from actions

  <details>
  
  When a group of actions return values, instead of using 
  the last action result, merge the values.
  
  </details>

* Fix export of custom fields on one-to-many and dashlet
* Fix processing view widgets (not recognized some widgets depending on view)
* Add missing transient annotation on cid field of enhanced Model

  <details>
  
  This fixes cid field not marked as transient when Model class is enhanced.
  It was notably breaking advanced search export all.
  
  </details>

* Fix o2m editor validation issue
* Fix fetch record after save in form view

## 7.1.0 (2024-05-28)

#### Feature

* Don't add CSRF token header/cookie for native clients
* Implement phone widget with react-international-phone

  <details>
  
  Widget is supported on form and grid view.
  The phone widget renders the field as a phone number link in readonly mode,
  and as a phone number input with a country/region selector in edit mode.
  
  Compared to implementation in previous front-end,
  `x-custom-placeholder` option is no longer supported.
  
  </details>

* Improve email pattern

  <details>
  
  Allow to use any printable characters in local part.
  Also remove upper limit on domain tld size.
  
  </details>

* Disabled autocapitalize, autocorrect and spellcheck on login fields
* Add view processors for processing views

  <details>
  
  View processors allow to programmatically add view widgets.
  They are automatically discovered on application startup,
  and they are executed in module resolution order,
  after finding view in `MetaService::findView`,
  which is used by `view` API endpoint.
  
  Example:
  
  ```java
  public class MyViewProcessor implements ViewProcessor {
  
    @Override
    public void process(AbstractView view) {
      // Do something to the `view`.
    }
  
  }
  ```
  
  </details>

* Allow send emails in edit mode
* Add prompt support on button link variant
* Support calendar event popover template

  <details>
  
  Example:
  
  ```xml
    <calendar name="sales-timeline" title="Sales Timeline" model="com.axelor.sale.db.Order" editable="true"
      eventStart="orderDate"
      eventStop="confirmDate"
      eventLength="8"
      colorBy="customer">
      <!-- All fields that should be fetched, ie. used in template -->
      <field name="name" />
      <field name="customer" />
      <field name="orderDate" />
      <field name="confirmDate" />
      <!-- The template that will be displayed in the event popover -->
      <template>
        <![CDATA[
        <>
         <ul>
           <li>{$fmt("customer")}</li>
           <li>{$fmt("orderDate")}</li>
           <li>{$fmt("confirmDate")}</li>
         </ul>
        </>
        ]]>
      </template>
    </calendar>
  ```
  
  </details>

* Add search support on collection field in grid view

  <details>
  
  This adds support to search if the target model as a target name available 
  (ie namecolumn). Multi values search is also supported using ` | ` separator.
  
  </details>

* Add attach files button in popup forms footer
* Add support to use domain with custom fields on level >= 1

  <details>
  
  This adds support to use following domain : `self.myM2O.attrs.myCustomFied = 'some'`. 
  It was restricted to first level only.
  
  </details>

* Add link action attrs support for button
* Add expandable grid support

  <details>
  
  Two types of expandable widgets are supported with collection field/grid view
  
  1.tree-grid (widget="tree-grid") (only supported on form collection field)
  
  ```xml
    <panel-related
      title="Items (Tree)"
      readonlyIf="confirmed"
      field="items"
      form-view="order-line-form"
      grid-view="order-line-grid"
      editable="true"
      onChange="com.axelor.sale.web.SaleOrderController:computeItems"
      widget="tree-grid"
      x-tree-field="items"
      x-tree-limit="2"
      x-tree-field-title="Add new item"
    >
      <field name="product" onChange="action-order-line-change-product"/>
      <field name="price" width="200" />
      <field name="quantity" width="150" />
    </panel-related>
  ```
  Options:
  
  - x-tree-field: used to define nested o2m field of order-line model (currently it's same as order object i.e. items)
  - x-tree-limit (optional): used to specify limit to support nested tree structure.
  - x-tree-field-title (optional): by default it uses main title for sub items heading (title will only display when item contains no-sub items)
  - x-expand-all: in case of tree-grid, it's enabled by default, it uses x-tree-field value as x-expand-all value. To disable it, we can pass `x-expand-all="false"`
  
  Note: onChange action will only work on top-level collection grid, while other actions defined like onNew/onLoad will work on nested editable grid line as well.
  
  2.expandable (widget="expandable")
  ```xml
  <panel-related
    title="Items (Expandable)"
    readonlyIf="confirmed"
    field="items"
    form-view="order-line-form"
    grid-view="order-line-grid"
    editable="true"
    onChange="com.axelor.sale.web.SaleOrderController:computeItems"
    widget="expandable"
    summary-view="order-line-nested"
    x-expand-all="items"
  /> 
  ```
  
  Options:
  
  - summary-view: used to define expandable form-view, if not specified then by default it will use form-view attribute.
  - x-expand-all: to enable expand all feature, you have to specify comma-separated list of nested expandable collection field if any.
  
  Notes:
  
  Actions like onChange, onNew, onLoad will work as per schema definition defined in order-form or order-line-nested form.
  Options like `x-tree-field`, `x-tree-limit`, `x-tree-field-title` have no impact in case of expandable widget.
  If `widget`/`summary-view` are defined in specified grid-view, then it will automatically be used in panel-related, we don't need it to specify.
  To disable it we can pass widget="one-to-many/many-to-many" depending on type of field.
  
  </details>

* Allow open urls in edit mode
* Don't store session for direct basic auth
* Add support to fetch graph with save and fetch requests

  <details>
  
  We can now pass `select` map to the `save` and `fetch` requests
  to return object graph. If `fields` and `related` options are also
  given, they will be merged with the graph.
  
  The format of the `select` map is as follow:
  
  ```json
  {
   ...
   "select": {
     "name": true,
     "customer": {
       "name": true
     },
     "items": {
       "product": {
         "name": true
       },
       "quantity": true
       "price": true
     }
   }
  }
  ```
  
  </details>

* Add scatter chart support
* Add area chart support
* Support hilites on calendar view

  <details>
  
  Support <hilite> elements on calendar view to define hiliting rules.
  
  The hilite attributes are:
    - if: boolean expression condition
    - styles: comma-separated list of styles: fill (default), outline, stripe, strike, fade
  
  </details>

* Add toggle password visibility feature

  <details>
  
  On password fields, this add a toggle icon at the end of the input 
  in order to effortlessly hides/reveals password for enhanced security 
  and convenience.
  
  </details>

* Add support to export collections fields

  <details>
  
  Collection fields can now be exported. This can be enabled using 
  `data.export.collections.enabled` property. The default separator 
  used is ` | `. This can be changed using `data.export.collections.separator` 
  property.
  
  </details>

* Add call button link in phone widget edit mode
* Add missing getters and setters for view element attributes

  <details>
  
  Add missing getters and setters for view element attributes
  in package `com.axelor.meta.schema.views`.
  
  </details>

* Add support to identify collection items from save/action response

  <details>
  
  The client can now set `cid` (collection id) to unsaved collection
  items to identify the same from the `save` and `action` responses.
  
  </details>

* Improve kanban UI/UX

  <details>
  
  Kanban columns can now be collapsed. An 'x-collapse-columns' property
  has been added, accepting a comma-separated list of column names that
  should be collapsed by default (reference fields not supported). This 
  comes with other visual changes such as pagination and minor fixes.
  
  </details>

* Add radar chart support
* Add drawing widget
* Add OpenAPI v3 specifications and Swagger UI

  <details>
  
  OpenAPI v3 specifications are available at `%URL%/ws/openapi`
  Swagger UI is available at `%URL%/#/api-documentation`
  
  </details>

#### Change

* Upgrade Jackson from 2.15.3 to 2.17.1
* Upgrade Guava from 32.1.3 to 33.2.0
* Upgrade Gradle from 7.5.1 to 8.7

  <details>
  
  This upgrade Gradle from 7.5.1 to 8.7.
  
  Upgrade the Gradle Wrapper to benefit from new features and improvements : 
  `./gradlew wrapper --gradle-version 8.7`
  
  This also include upgrade of Gradle plugins used.
  
  </details>

* Move view collaboration to Axelor Enterprise Edition
* Upgrade Undertow from 2.2.28 to 2.2.32
* Upgrade Woodstox from 6.5.1 to 6.6.2
* Load Monaco editor resources from local instead of from CDN
* Upgrade Redisson from 3.19.3 to 3.29.0
* Re-introduce multi-tenancy support

  <details>
  
  Clients without session support (e.g. basic auth) should provide `X-Tenant-ID` header
  with every requests to select a tenant.
  
  Clients with session support should send `X-Tenant-ID` header with login request.
  
  </details>

* Upgrade PostgreSQL JDBC from 42.7.2 to 42.7.3
* Upgrade ASM from 9.6 to 9.7
* Upgrade Apache Commons CLI from 1.6.0 to 1.7.0
* Upgrade Apache Commons CSV from 1.10.0 to 1.11.0
* Update fullcalendar from v6.1.9 to v6.1.11
* copyWebapp Gradle task excludes hidden files only

  <details>
  
  copyWebapp Gradle task used to exclude development files used by previous axelor-web front-end.
  Now, it excludes hidden files only.
  
  </details>

* Upgrade Junit5 from 5.10.1 to 5.10.2
* Upgrade Apache Commons IO from 2.15.1 to 2.16.1
* Upgrade Jsoup from 1.17.1 to 1.17.2
* Upgrade pac4j from 5.7.2 to 5.7.4
* Upgrade SLF4J from 2.0.9 to 2.0.13
* Move SSO authentications to Axelor Enterprise Edition
* Upgrade Apache Tika from 2.9.1 to 2.9.2
* Upgrade Tomcat from 9.0.84 to 9.0.88
* Upgrade Groovy from 3.0.20 to 3.0.21
* Upgrade Byte Buddy from 1.14.11 to 1.14.14
* Upgrade Hazelcast from 5.3.6 to 5.3.7
* Upgrade Infinispan from 13.0.21 to 13.0.22
* Upgrade Ldaptive from 2.2.0 to 2.3.2
* Upgrade Snakeyaml from 1.33 to 2.2
* Align rendering and styles between card and kanban views

  <details>
  
  This align rendering and styles between card and kanban views. 
  Mostly impact kanban views, some predefined styles as been removed 
  in react template (img, h4). See migration notes.
  
  </details>

#### Remove

* Remove ability to switch tenants after authentication

  <details>
  
  At the initial stage of the multi-tenant implementation, it was allowed 
  for a user to switch tenant. It was a convenient for testing and debugging.
  At this point it doesn't seems to good option anymore, due to security risk 
  and goal of multi-tenant. So the feature has been removed and no replacement 
  is expected. `db.%tenant%.roles` property has been removed.
  
  </details>

* Remove deprecated AppInfo in favor of InfoService
* Remove unused expandable attribute from grid view

#### Fix

* Fix search filter on relation field search

  <details>
  
  Search filter should exclude searching on `id` field.
  Even if it's mentioned in targetSearch, it should be excluded.
  
  </details>

* Fix mass update on selection fields
* Fix link button rendering styles
* Fix valueExpr support for custom field
* Fix update time in date-time widget
* Fix icon position on form button widget
* Fix calendar popover and mail message title locale time formatting
* Fix generateCode circular dependency on self
* Fix check edit permission in grid details view
* Fix refresh panel-dashlet on record navigation
* Fix reset tree-view on reload
* Fix exclude colorField for search criteria in tag-select widget
* Fix missing html support on form field title
* Fix trigger onChange on enter key in date picker
* Fix missing colOffset support on form field
* Fix relation field popup selection conflicts in popper

  <details>
  
  When relational field is searched using popup in advanced search
  or in mass update popper, the popper should remain open and should
  not affect any click event happens in selector popup wizard.
  
  </details>

* Fix technical information support on dashlet
* Exclude non string type fields from search fields
* Fix grid view search on limit change in pagination

  <details>
  
  When we have already search on some columns in grid view then
  if try to change limit in pagination(pager) then it is sending
  payload without searched criteria.
  
  </details>

* Fix set value on date field through action

  <details>
  
  When user cleared the value manually through backspace/delete
  After that if value is set through action then it should use/display
  that value in date/datetime input.
  
  </details>

* Fix update gantt taskEnd when it's defined
* Exclude archived M2O from selection

  <details>
  
  When M2O is used as a selection (`NavSelect` widget or in `Kanban#columnBy`), exclude archived records.
  
  </details>

* Fix select input text on focus attribute in form field

  <details>
  
  When focus is set through action-attrs, select input text.
  
  </details>

* Add missing customize feature on collection fields
* Allow saving new record if user has create but no write permission
* Disable progress when taskProgress is not defined in gantt view
* Fix parent support in form field tooltip
* Fix dirty form handling after grid button action

  <details>
  
  This fixes dirty form handling for both editable and non-editable grid.
  
  </details>

* Fix button colors in view toolbar
* Add missing tooltip on wkf widget
* Fix calendar year dropdown

  <details>
  
  This add support to scrollable year dropdown (+/- 50 years) 
  and display the upcoming/previous arrows.
  
  </details>

* Fix dotted field target-name in tag-select widget
* Fix fetch dotted fields in grid details view
* Fix dragging grid row with parent container scroll

  <details>
  
  If parent container has a scroll, it create dragging row issue : 
  the dragging element isn't at the right position. wheel scroll is 
  now disabled to avoid unexpected behavior.
  
  </details>

* Fix relation field multiple selection in advanced search
* Fix perms endpoint without id

  <details>
  
  If permission is found, access should be granted in case of no id.
  Without id, `AuthSecurity::isPermitted` had ids `[null]` and access was granted
  if permission filter result count happened to equal 1.
  
  </details>

#### Security

* Fix links to cross-origin destinations
* Fix checking permissions on export

  <details>
  
  When exporting data, it should check permissions on each fields. 
  For example, when exporting `currency.symbol`, it should first check 
  if user can export `currency` field in `Order` entity, then if user 
  can export `symbol` field on `Currency` entity.
  
  </details>


## 7.0.6 (2024-04-05)

#### Fix

* Fix pass related record on click action in chart
* Add missing hilite support in form view field
* Fix sorting in grid view
* Fix format decimal value in chart tooltip
* Fix view switch to form in calendar/cards view

  <details>
  
  When we switch to form through view toolbar from calendar/cards
  view then it should open first available record in form.
  If no records are available then it should skip navigation to 
  form view.
  
  </details>

* Fix pass search fields in action context in chart
* Add missing hilite support in cards view
* Fix prompt support in grid view button
* Fix view switch from grid to form in view toolbar

  <details>
  
  When we switch to form through view toolbar from grid view
  Then if any row is selected then it should open in form else
  open the first available record in grid.
  If no records are available then it should skip navigation to 
  form view.
  
  </details>


## 7.0.5 (2024-03-29)

#### Feature

* Add support to drop node to root in tree view

  <details>
  
  When tree view nodes are on same model, this allow to move 
  nodes to root, using draggable area in bottom of the view.
  
  </details>

#### Fix

* Fix action view params for panel-dashlet
* Fix issues in tree view

  <details>
  
  This fix several issues in tree view : 
  - reset tree data on refresh
  - several errors when moving nodes
  - save not triggered after d&d nodes
  
  </details>

* Fix button-group widget display
* Fix search grid on selector popup pagination
* Fix data views search after switch from calendar view
* Add missing icon support in panel
* Fix show truncated title as help in form button widget
* Fix validate required attr on collection fields
* Reduce collaboration avatar height

  <details>
  
  Reduce collaboration avatar height so that it does not increase toolbar height once shown.
  Also fix grouped avatars icon width.
  
  </details>

* Fix showing title or help on buttons mouseover in grid/tree view
* Fix groupBy support in collection grid field
* Fix reset data offset on grid header search
* Fix tag-select search issue
* Fix info-button content with dotted fields
* Fix kanban to form pagination

  <details>
  
  When click on card to open form from kanban column then
  it should allow to do pagination(prev/next) for that column
  set of records in form view.
  
  </details>

* Fix decimal form widget currency formatting

  <details>
  
  Formatting with `x-currency` was working on grid, but missing on decimal form widget.
  
  </details>

* Fix prevent propagation of click on tooltip content
* Fix show action condition errors in form view
* Fix button link
* Fix redundant search request on grid view pagination
* Fix call onChange action date input change

## 7.0.4 (2024-03-08)

#### Feature

* Add support to format `multi-select` widget
* In field help popover, display value for selection/enum fields.

#### Fix

* Fix grid open details view by default
* Fix widgetAttrs issue with custom field
* Fix resetting relational fields domain

  <details>
  
  Resetting domain on relational fields by returning `null`
  domain on onSelect event is not takes into account and
  existing domain is still used.
  
  </details>

* Reset grid offset on DMS search
* Always call onSelect on o2m when showing selector
* Fix create support in selector popup in relational field
* Fix invalid text widget style
* Fix grid group when groupBy having leading/trailing spaces
* Fix updating fields focus

  <details>
  
  Changing fields focus attrs hasn't any effect if refocus same field again.
  This also fix unfocusable M2O fields.
  
  </details>

* Fix m2o with nav-select widget display issue

  <details>
  
  When showIf/hideIf is specified with nav-select widget on m2o field
  Then it was displaying m2o selection along with nav-select.
  It should only display nav-select widget.
  
  </details>

* Fix ref-text should not allow create/view record
* Fix eval-ref-select to use suggest box behavior
* Fix how grid retrieve the field record value

  <details>
  
  This also fix wrong values displayed in grid for custom fields.
  
  </details>

* Fix mark form dirty on editor change
* Disabled http block overlay when login popup is displayed.
* Fix multi selection widgets values mapping

  <details>
  
  Make sure to map values from the previous concatenation
  that was using `Comma-separated + space`.
  
  </details>

* Don't add default json fields (`attrs.*`) if already present in grid view
* Fix unformatted calendar event titles
* Fix default values of json fields
* Fix single tab issue

  <details>
  
  When single tab is enabled, any tab is already opened then
  it should first close the current tab and open new tab as
  single tab only.
  
  </details>

* Fix RadioSelect/CheckboxSelect widget ui display

  <details>
  
  Don't display radio/checkbox select items onto one line
  but wrap them depending on available space (break into multiple lines).
  
  </details>

* Fix RadioSelect/CheckboxSelect widget in editable grid

  <details>
  
  In grid, RadioSelect/CheckboxSelect widget can't be used for obvious 
  display reason. When they are used, use default `selection` widget 
  for `RadioSelect` and `MultiSelect` widget for `CheckboxSelect`.
  
  </details>

* Improve multi-select widget in grid view

#### Security

* Upgrade PostgreSQL JDBC driver from 42.7.1 to 42.7.2

  <details>
  
  SQL injection is possible when using the non-default connection property 
  `preferQueryMode=simple` in combination with application code that has a 
  vulnerable SQL that negates a parameter value.
  
  See https://www.cve.org/CVERecord?id=CVE-2024-1597
  
  As we are using the default query mode, we aren't impacted. But administrators 
  can still change the JDBC URL themself to use the impacted query mode.
  
  </details>


## 7.0.3 (2024-02-14)

#### Fix

* Fix formatter support in collection template viewer
* Fix missing dotted fields in o2m grid if no parent fields

  <details>
  
  Issue happened when there is a dotted field without its parent,
  eg. "product.code" field, but no "product" field.
  
  </details>

* Fix set multiple action attrs on same field
* Display message if file(s) can't be downloaded
* Fix pagination settings not taken into account

  <details>
  
  `api.pagination.max-per-page` was taken into account on page text only.
  `api.pagination.default-per-page` was not taken into account.
  
  </details>

* Fix adding m2m item should mark form as dirty
* Check for errors on save action if not dirty
* Fix commit editable grid on ok in popup form
* Fix o2m grid flashing on items reset
* Fix custom field support in editable grid
* Return 404 http code when downloading files that don't exist
* Fix special operators support in advance search
* Fix confirm lost change when reloading browser tab
* Avoid grid re-rendering on auto-sizing
* Fix action attr value for reference fields
* Fix legacy template support for reference field
* Fix save request payload containing unwanted values

## 7.0.2 (2024-02-06)

#### Feature

* Improve dashlets/collections data loading

  <details>
  
  If a dashlet or a collection field isn't visible 
  (either hidden or in a non active panel-tab), data should't be fetched.
  Data should then fetched (if required) as soon as the widget is visible.
  
  </details>

* Improve dotted field rendering

  <details>
  
  To avoid flashing issue on dotted field change, dotted field 
  value is rendered and updated through state and state update
  will be defer during action execution.
  
  </details>

#### Fix

* Fix set records in collection editor
* Fix context for kanban view card template

  <details>
  
  Kanban card record should have preference over the action view context.
  Also improve context by removing unnecessary values like renderer etc.
  
  </details>

* Fix error when formatting m2o field value with missing context
* Fix id field value erased when setting values from actions

  <details>
  
  When setting values from actions (both action-attrs or action-record), 
  cause the id field value to be erased. In most cases, this shouldn't 
  be an issue but on new record (without id), and having id in context 
  cause the id in context do be erased.
  
  </details>

* Fix relational fields editor

  <details>
  
  Relational fields displayed in editor wasn't saved with main record due 
  to missing version fields when value set through action.
  Moreover, after the value as saved, the new version value wasn't sync, 
  so subsequent update triggerred concurrent update errors.
  
  </details>

* Trigger o2m search for records that have id and no version
* Fix update from value to no value on grid row

  <details>
  
  Issue happened in two cases on o2m grid.
  
  First one is when changing from a m2o record that has a translation
  to another one that has no translation. Translation from previous record was wrongly kept.
  
  Second one is when changing from a m2o record that has a dotted value
  to another one that has no dotted value. Dotted value from previous record was wrongly kept.
  
  </details>

* Fix o2m losing grid order after action setting grid rows

  <details>
  
  Action was receiving o2m items in fetched order, not grid order.
  
  </details>

* Fix checking dirty when closing o2m popup

  <details>
  
  Instead of comparing with previous row values to determine if the record is dirty,
  we now pass dirty state from popup record and editable row record.
  
  </details>

* Fix custom field change should mark form dirty
* Improve readonlyIf fields in editable grid

  <details>
  
  This avoids fields blinking when entering in editable grid 
  because they switch quickly from edit to read only mode.
  
  </details>

* Fix collection editor icons visibility depending on perms
* Fix grid crash error (`object is not extensible`)

  <details>
  
  When a grid has both a relational field (ie `product.category`) and 
  na associated dotted field (ie `product.category.code`), enter in editable 
  mode cause the grid widget to crash (`object is not extensible` error).
  
  </details>

* Fix *-to-one editor icons visibility depending on perms
* Fix support for dotted collection field

## 7.0.1 (2024-01-26)

#### Feature

* Improve o2m fetch request for edit popup

  <details>
  
  As soon as a row record is updated from popup view, reopening the popup 
  for the same row will not trigger another fetch request, as the record 
  is already fetched. This reduce number of requests.
  
  </details>

#### Fix

* Fix committing row on non-dirty O2M editable grid

  <details>
  
  If a O2M field with an editable grid is a non-dirty field,
  row changes were lost after committing. Need to compare record 
  equality instead of the _dirty flag (not relevant in that case).
  
  </details>

* Fix grid dotted field update

  <details>
  
  On existing row, after edit, dotted fields were reverted to previous value.
  Nested values were updated, but grid relies on dotted fields.
  
  </details>

* Fix grid hilite not using action view context
* Fix resetting o2m value when creating new record

  <details>
  
  Issue happened when opening an existing record in form view then clicking on create new record.
  O2M values from previous record were kept.
  
  </details>

* Fix reorder on non dirty o2m widget

  <details>
  
  Issue happens when o2m values are set through actions and those values are not persisted
  (either computed or id is set to null) and on initial re-ordering, grid gets empty.
  
  </details>


## 7.0.0 (2024-01-24)

#### Feature

* Support for Gantt dashlet
* Add support to use `TagSelect` widget in grid

  <details>
  
  This allow to use `TagSelect` widget in grid. It will display records as badge. Full list will be displayed on mouse over when records can't be displayed in the grid cell.
  `target-name` is at this time not supported.
  
  </details>

* Add support to filter mail messages

  <details>
  
  On the message stream widget, users can choose the messages types to display.
  
  Moreover, on the view definition, the `filter` attribute can be used to specify messages type
  to show by default : `all` (default), `comment`, `notification`.
  
  </details>

* Add `x-dirty` attribute to view fields

  <details>
  
  If the view field is marked as `x-dirty="false"`, the
  field value change will not mark current record dirty.
  
  The old dirty checking behavior of the `$` prefixed
  fields is now deprecated and will be removed during
  next major release.
  
  </details>

* Migrate to new front-end build on top of React

  <details>
  
  Drop current Angular front-end in favor of new front-end build on top of React.
  
  </details>

* Add support for `{user.dir}` variable in config file

  <details>
  
  The `{user.dir}` refers to the current working directory.
  It is useful during development where you have many different
  instances to test with.
  
  For example:
  
  ```properties
  data.upload.dir = {user.dir}/data/axelor
  ```
  
  will store upload files under `data/axelor` found under
  current working directory.
  
  </details>

* Add support of `canDelete` and `canNew` attribute to calendar views
* Improve `$fmt` script helper

  <details>
  
  The `$fmt` helper accepts an optional `props` param to pass custom field
  props (`currency`, `scale`, `second`, ...) : `$fmt("myField", { scale: 6 })`
  
  </details>

* Add changelog plugin

  <details>
  
  Provide a Gradle plugin to simplify changelog management.
  
  Each entry of the `CHANGELOG.md` file is generated from files in 
  the `changelogs/unreleased/` folder.
  
  To use the plugin, in your `build.gradle` :
  ```yaml
  apply plugin: com.axelor.gradle.support.ChangelogSupport
  
  changelog {
    version = "${project.version}"
    output.set(file("CHANGELOG.md"))
    inputPath.set(file("changelogs/unreleased"))
    types.set(["Feature", "Change", "Deprecate", "Remove", "Fix", "Security"])
    header.set("${version.get()} (${new Date().format("yyyy-MM-dd")})")
  }
  ```
  
  To generate the `CHANGELOG.md` with unreleased entries, run following Gradle task:
  `./gradlew generateChangelog`
  
  </details>

* Support view-param popup reload on dashlets

  <details>
  
  ```xml
  <view-param name="popup" value="true"/>
  ```
  
  Open dashlet edit in popup (instead of new tab).
  (This is currently for grid dashlet only. Other dashlet types always open popup.)
  After closing, dashlet has up-to-date data for the record that was opened and edited.
  (Search request may be performed by some dashlet types to achieve that result,
  but it's room for later improvement.)
  
  ```xml
  <view-param name="popup" value="reload"/>
  ```
  
  Reload current tab after closing popup from dashlet.
  If the tab is dirty before opening the popup,
  save confirmation dialog is shown before proceeding.
  
  </details>

* Add `auth.provider-default` configuration

  <details>
  
  Normally, form client is the default client used for authentication.
  When an authentication provider is defined as exclusive, form client is not loaded at all.
  
  Now, with `auth.provider-default` configuration, you can specify another non-exclusive client as default.
  
  Example:
  `auth.provider-default = google` // google auth provider is used by default
  
  You can specify another client via the `client_name` query parameter in login URL.
  
  Example:
  // Default client is used
  http://localhost:8080/open-platform-demo/
  
  // Specify another client
  http://localhost:8080/open-platform-demo/?client_name=form
  
  </details>

* Export and view MetaFields

  <details>
  
  Add x-can-export attributes on O2M metaFields on MetaModel form view
  
  </details>

* Allow canMove sequencing on any field specified by orderBy

  <details>
  
  With `canMove`, the field used for sequencing does not have to be a field named `sequence`
  anymore, but can be any field specified by `orderBy`.
  
  Sequencing is done on field specified by `orderBy`, and it must be only one integer field.
  If not specified, not sequencing is done.
  
  On top-level grid, `canMove` requires `orderBy`.
  
  </details>

* Add index to MetaTranslation on message field
* Support `action-menu` elements specific to a `search` view

  <details>
  
  `action-menu` elements can now be defined inside a `search` view
  to specify that they are specific to that view, and not available to all search views.
  
  </details>

* Add `application.icon` and `context.appIcon` settings

  <details>
  
  These settings allow to define an application icon used
  for website favicon and small logo.
  They can be used similarly to the `application.logo`
  and `context.appLogo` settings.
  The icon must be a multiple of 48px square
  for favicon compatibility with most browsers.
  
  </details>

* Add Rating widget

  <details>
  
  Rating widget can be used on Integer,Long and Decimal field. It provides ability to 
  collect measurable opinions/experiences/feedbacks/...
  
  Example : 
  ```xml
  <field name="note" type="Integer" widget="Rating"/>
  <field name="note" type="Integer" widget="Rating" x-rating-icon="heart"/>
  ```
  
  </details>

#### Change

* Upgrade Jackson from 2.13.4 to 2.15.3
* /ws/app/info endpoint moved to /ws/public/app/info

  <details>
  
  /ws/public/app/info either gives application login info
  or session info if the user is logged in.
  /ws/app/info is deprecated and will be dropped in a future release.
  
  </details>

* Upgrade Guava from 31.1 to 32.1.3
* Upgrade Undertow from 2.2.19 to 2.2.28
* Upgrade Woodstox from 6.3.1 to 6.5.1
* Improve flagging mail messages

  <details>
  
  The flag associated to the user and message is now retrieved on backend. This 
  avoids duplicated mail flags after reflagging messages. As part of the change, the 
  message is also required and indexes as been reviewed.
  
  Run following SQL script to adjust MailFlag table changes :
  ```
  ALTER TABLE mail_flags ALTER COLUMN MESSAGE SET NOT NULL;
  DROP INDEX mail_flags_message_idx;
  DROP INDEX mail_flags_user_id_idx;
  CREATE INDEX mail_flags_user_id_message_idx ON mail_flags (user_id, message);
  ```
  
  </details>

* Upgrade Hibernate ORM from 5.6.12 to 5.6.15
* Upgrade Redisson from 3.17.6 to 3.19.3
* Upgrade EhCache from 3.10.1 to 3.10.8
* Upgrade Flyway from 9.3.1 to 9.22.3
* Upgrade PostgreSQL JDBC from 42.5.0 to 42.7.1
* Upgrade Apache Shiro from 1.9.1 to 1.13.0
* Upgrade hsqldb from 2.7.0 to 2.7.2
* Upgrade logback from 1.2.11 to 1.3.14
* Refactor domain context of collection field search request

  <details>
  
  Restructured `_domainContext` for collection field search request:
  
  ```json
  {
      "_model",
      "_field",
      "_field_ids",
      "_parent": {
          "id",
          "_model"
      }
  }
  ```
  
  </details>

* Upgrade Jansi from 2.4.0 to 2.4.1
* Upgrade ASM from 9.3 to 9.6
* Upgrade Apache Commons CSV from 1.9.0 to 1.10.0
* CodeEditor `x-code-theme` is no more supported.

  <details>
  
  As part of the new v7 front-end built on top of React, `x-code-theme` is no more supported. 
  It will be re-added in a future version.
  
  </details>

* Use ref-select widget for User/Group homeAction

  <details>
  
  Remove special case for User/Group records where we added a dummy `__actionSelect` field.
  Instead, use ref-select widget on `homeAction`.
  
  </details>

* Upgrade Hibernate Validator from 6.2.4 to 6.2.5
* Upgrade MySQL JDBC from 8.0.30 to 8.0.33
* Upgrade Junit5 from 5.9.1 to 5.10.1
* Use Feature to discover studio module

  <details>
  
  Instead of checking using module name, use specific feature to discover if 
  studio module is used. This can be enabled using Studio feature.
  
  Example of how to enable the Studio feature from the module:
  
  ```java
  public class MyModule extends AxelorModule {
    @Override
    protected void configure() {
        AppSettings.get().enableFeature(AvailableAppFeatures.STUDIO)
    }
  }
  ```
  
  or using `features.studio = true` in `axelor-config.properties.
  
  </details>

* Upgrade Resteasy from 4.7.7 to 4.7.9
* The `view.grid.selection` property is now set to `checkbox` by default

  <details>
  
  Checkbox selection in grid is now enabled by default.
  
  Change to `view.grid.selection = none` to disable it globally.
  
  </details>

* Upgrade Gradle Spotless plugin from 6.11.0 to 6.13.0
* Upgrade slf4j from 1.7.36 to 2.0.9
* Upgrade Jsoup from 1.15.3 to 1.17.1
* Upgrade EclipseLink Moxy from 2.7.11 to 2.7.14
* Upgrade pac4j from 5.4.5 to 5.7.2
* Upgrade Caffeine from 3.1.1 to 3.1.6
* Upgrade XStream from 1.4.19 to 1.4.20
* Upgrade Apache Commons IO from 2.11.0 to 2.15.1
* collection editor without x-viewer no more used in readonly

  <details>
  
  For collection fields, if the editor isn't marked with `x-viewer="true"` and doesn't have any viewer defined, the editor is no more used in readonly mode. The default rendering behavior will be used instead (ie grid). This is more consistent with others fields behavior.
  
  </details>

* Upgrade Apache Commons CLI from 1.5.0 to 1.6.0
* Fix `x-show-bars` actions should not affect parent form

  <details>
  
  `x-show-bars` actions used to affect parent form.
  Now, they affect local grid context only.
  
  </details>

* Upgrade Apache Tika from 2.4.1 to 2.9.1
* Update shortcuts

  <details>
  
  Here are the changes of the shortcuts : 
  
  Changed :
  - delete current/selected record(s) : `Ctrl+Delete` instead of `Ctrl+D`
  - navigate to previous page/record : `Alt+Page Up` instead of `Ctrl+J`
  - navigate to next page/record : `Alt+Page Down` instead of `Ctrl+K`
  
  Added : 
  - duplicate current record : `Ctrl+D`
  
  </details>

* Upgrade embedded Tomcat from 9.0.65 to 9.0.84
* Upgrade Groovy from 3.0.13 to 3.0.20
* Upgrade ByteBuddy from 1.12.17 to 1.14.11
* Upgrade Hazelcast from 5.1.3 to 5.3.6
* Upgrade Hibernate Search from 5.11.10 to 5.11.12
* Upgrade Greenmail from 1.6.10 to 1.6.15
* Upgrade Unboundid LDAP SDK from 6.0.6 to 6.0.11
* Upgrade Infinispan from 13.0.11 to 13.0.21
* Upgrade Ldaptive from 2.1.1 to 2.2.0
* No longer encrypt plain passwords on startup

  <details>
  
  Due to historical reason, any users passwords stored in plain text in the database are encrypted on startup. 
  On databases having large number of users, it is unnecessary time consuming on startup.
  
  To reset a forgotten admin password, update or insert an active admin user with a temporary password 'admin123' 
  in database using the hashed password : 
  ```
  UPDATE auth_user set password = '$shiro1$SHA-512$1024$NE+wqQq/TmjZMvfI7ENh/g==$V4yPw8T64UQ6GfJfxYq2hLsVrBY8D1v+bktfOxGdt4b/9BthpWPNUy/CBk6V9iA0nHpzYzJFWO8v/tZFtES8CA==' where code = 'admin';
  ```
  
  Another way to generate passwords, is to use the Apache Command Line Hasher :
  ```
  $ (cd /tmp && curl -sSL -O https://repo1.maven.org/maven2/org/apache/shiro/tools/shiro-tools-hasher/1.11.0/shiro-tools-hasher-1.11.0-cli.jar)
  $ java -jar /tmp/shiro-tools-hasher-1.11.0-cli.jar --algorithm SHA-512 --iterations 500000 -p
  Password to hash:
  Password to hash (confirm):
  $shiro1$SHA-512$500000$pbUIjvJh1moFNc98vH+YbA==$Wtu3fIgNIL4ab9jWp6DyRa7vW5Zo33knW7JNV9KFJj08lal4WHBmVJSOHxJ0w+7SwlPvJ25O1QYNVb6wgmTHnA==
  ```
  
  Also, if you import users with passwords from any source, either password must be hashed or you can use helpers 
  methods to encrypt it :
  
  - `com.axelor.auth.AuthService.encrypt(java.lang.String)` : Encrypt the given password text.
  - `com.axelor.auth.AuthService.encrypt(com.axelor.auth.db.User)` : Encrypt the password of the given user.
  - `com.axelor.auth.AuthService.encrypt(java.lang.Object, java.util.Map)` : Adapter method to be used with csv/xml data
  import in order to encrypt the password of the given user.
  
  </details>

* Field names used by the create on the fly

  <details>
  
  The create on the fly feature control by the `x-create` attribute 
  is used to quickly create records and/or pre-filled values base on 
  the current search input.
  
  Previously, any fields named `code`, `name` and the namecolumn of 
  the object were filled by default. This is no more the cases: only 
  the field names provided in the `x-create` attribute will be.
  
  </details>

* Upgrade Snakeyaml from 1.32 to 1.33

#### Remove

* Remove `view.grid.editor-buttons` setting

  <details>
  
  The setting `view.grid.editor-buttons` has been removed. It was use 
  to show confirm/cancel buttons from grid row editor.
  
  </details>

* Remove `$context` variable in frontend eval expressions

  <details>
  
  This was never intended to be used.
  Stick to direct field access: `myField`, not `$context.myField`.
  
  </details>

* Remove DMS spreadsheet

  <details>
  
  DMS spreadsheet is not implemented in community version of new frontend.
  
  Enable or disable internal features using `AppSettings.enableFeature` or
  `AppSettings.disableFeature`.
  
  Example of how to enable the DMS spreadsheet feature from the module:
  
  ```java
  public class MyModule extends AxelorModule {
    @Override
    protected void configure() {
        AppSettings.get().enableFeature(AvailableAppFeatures.DMS_SPREADSHEET)
    }
  }
  ```
  
  or using `features.dms-spreadsheet = true` in `axelor-config.properties.
  
  </details>

* Remove `view.confirm-yes-no` setting

  <details>
  
  The setting `view.confirm-yes-no` has been removed. It was used 
  to show confirm dialog with yes/no buttons (else is Cancel/OK).
  
  </details>

* Drop custom style

  <details>
  
  Custom Style (provided from special context setting `context.appStyle`) 
  has been dropped. There is no replacement at this time.
  
  </details>

* Drop StaticResourceProvider

  <details>
  
  StaticResourceProvider, used to register custom css or js files, 
  has been dropped. There is no replacement at this time.
  
  </details>

* Remove CodeEditor `x-mode` support in favor of `x-code-syntax`
* Drop custom theme

  <details>
  
  Custom theme is no more supported and has been dropped. 
  There is no replacement at this time.
  
  </details>

* Drop extra partial JSP templates

  <details>
  
  Extra partial JSP templates support is no more supported and 
  has been dropped. There is no replacement at this time.
  
  </details>

* Remove `view.toolbar.show-titles` setting
* Remove top menus support

  <details>
  
  In this new version of the frontend, top menus aren't supported anymore.
  
  Run following SQL script to drop unnecessary columns :
  
  ```sql
  ALTER TABLE meta_menu DROP COLUMN top_menu ;
  ALTER TABLE meta_json_model DROP COLUMN menu_top;
  ```
  
  </details>

* Remove `view.menubar.location` setting

  <details>
  
  The setting `view.menubar.location` has been removed. It was used 
  to set menu style (left, top, both). There is no more support of top menus.
  
  </details>

#### Fix

* Fix bpm module discovery
* Fix destination of data errors in CSV data import

  <details>
  
  In a CSV data import, we can log the rows with an error in a new CSV file. They used to be logged in the XML config file.
  
  </details>

* Fix menus loading when menus that can't be added to N-ary Tree

  <details>
  
  When fetching the menus, if a menu is parent of itself or having wrong parent references, it load indefinitely. A log will be displayed in console for menus that can't be added to N-ary Tree.
  
  </details>

* Fix StackOverflowError in CSV data import

  <details>
  
  In a CSV data import, if we log the error rows in a file, a StackOverflowError was thrown when more than 2 CSV files have some errors.
  
  </details>

* Fix pending actions not launched after notify
* Fix error when retrieving menu

  <details>
  
  If an error occurs when evaluating menu conditions (ie script errors), it
  shouldn't fail the all process, but don't display the failed menus.
  
  </details>

* Fix tracking message value for datetime fields

  <details>
  
  For datetime field, the tracking value stored in database 
  should be in UTC so that it will be displayed depending 
  on user timezone.
  
  </details>

#### Security

* Fix search permission check on parent

  <details>
  
  In search request, need to check parent really has requested items.
  Otherwise, it is possible to specify unrelated parent and bypass permissions.
  
  </details>


## 6.1.5 (2023-08-16)

#### Changes

* Meta json field precision for decimal field is now 20 by default

#### Fixed

* Fix label field title reset
* Fix static widgets causing editor dirty
* Fix setting/resetting widgets title

## 6.1.4 (2023-06-23)

#### Features

* Preserve grid scroll position on form save/reload

#### Fixed

* Fix buttons actions in Tree views
* Fix on how application and module are determinate during gradle resolution

  <details>
  
  Due to the merge of `com.axelor.app` and `com.axelor.app-module` gradle
  plugins, it is now hard to determinate who is the module from the
  application. A module can be built itself, so it is seen as an
  application (when checking `project == project.getRootProject()`) and
  wrong plugins/dependencies/tasks are applied.
  
  To overcomes this, when a module need to be built standalone,
  `axelor.application = false` property can be added in `gradle.properties`. 
  This way, it will be seen as a module instead of an application.
  
  Better support will be added in a future version.
  
  </details>

* Fix going into edit mode in editable grid when clicking readonly cell
* Fix selection widget stealing focus after focusing another cell
* Readonly fields, included dot fields, shouldn't be focusable
* Fix search request when adjusting page boundary
* Fix onChange on Enter key in simple fields
* Fix lost dotted fields in grid when using master-detail widget
* Fix editable grid that don't wait for pending actions
* Fix deselected row after save triggered by previous row in editable grid
* Fix search box show/hide on cards view dashlet depending on dashlet `canSearch` attribute
* Fix onNew action on editor
* Don't create webapp folder in war
* Fix toolbar buttons display when same grid is displayed multiple times

#### Security

* Check for unauthorized users inside security filter directly

## 6.1.3 (2023-05-15)

#### Fixed

* Align script helper test expressions with Action behavior
* Fix attributes that need a test instead of an evaluation
* Fix action test condition when context proxy is used
* Do not try to generate binary download link on unsaved record

## 6.1.2 (2023-04-05)

#### Changes

* Improve resolution of AOP core dependencies

  <details>
  
  Use AOP version defined in root project. This avoids to use a version
  coming from transitive dependencies.
  
  For example, if a module is built and published using AOP version 6.1.2
  and the root project use AOP version 6.1.1, it will now use the AOP
  version of the project, ie 6.1.1 (instead of getting the AOP version of
  the transitive dependency of the module).
  
  </details>

#### Fixed

* Don't reload dashlet custom view when the widget is not visible
* Disable exporting on Kanban views
* Fix empty recipients list when posting message or adding followers
* Fix onChange triggered after grid edit cancel
* Fix missing "refresh", "new", "prev", and "next" keyboard shortcuts on cards and kanban views
* Fix moving record on top level grid
* Fix grid not editable depending on readonly/canEdit conditions
* Fix redefined User namecolumn in collaboration widget

  <details>
  
  When the namecolumn of the User entity is redefined,
  it was not taken into account in the collaboration widget.
  
  </details>

* Fix editable grid preventing save

  <details>
  
  On slow network and/or big grids, going in and out of grid edit
  may trigger duplicate grid edit events and mess with the counting
  of active editable grids. This could cause saving to fail.
  
  </details>

* Fix export on relational fields
* Fix spinner buttons triggering onChange inside editable grid
* Escape data when generating xml
* Don't allow to post message without body

#### Security

* Check `canNew` view attribute with "create new record" keyboard shortcut

## 6.1.1 (2023-02-06)

#### Fixed

* Fix dirty view when an editor contain a button
* Don't set default value on dotted fields of existing records

  <details>
  
  This fixes values mismatch of dotted fields having default values after saving new record
  from form view and switching back to grid.
  
  </details>

* Fix merging of namecolumn fields in code generator
* Fix multiple grouping on grid
* Fix reloading meta on systems having high number of cores
* Fix onnew popup actions called with delay
* Fix  hidden panels/buttons in editor when the record changes
* Fix details from view attrs reset when reloading from grid/tab
* Fix auth provider setting `exclusive`
* Forbid adding init params

  <details>
  
  When merging properties, it should be forbidden to add any init params,
  whether we're overriding fields or adding fields.
  
  </details>

* Don't warn about unchanged ref when merging entity props in code generator

  <details>
  
  `ref` may be specified as simple name or fully qualified name.
  When one is using simple name, compare by simple name only,
  as to avoid spurious warning.
  
  </details>

* Fix tab refresh with HTML dashlet
* Fix grid grouping with evaluated scale

  <details>
  
  Fix grid grouping when there are fields using scale evaluation (`x-scale="field"`).
  Use the maximum scale in the group for the formatted aggregation.
  
  </details>


## 6.1.0 (2022-11-03)

#### Changes

* Change code generator strategy to merge item attributes

  <details>
  
  Previously, the code generator replace the field definition by the new one. Now 
  the code generator will merge initial field attributes with overridable attributes.
  So, only necessarily attributes that need to be override should be defined in the 
  overwritten entity.
  
  This applies to both entities and enums.
  
  In the case of entity fields, there are a few restrictions:
    * Attributes that are not overridable:
      * `initParam`
      * `column`
      * `column2`
      * `ref`
      * `mappedBy`
      * `table`
      * `tz`
      * `json`
    * Attributes that are overridable with some conditions:
      * `large`: large field cannot become non-large
      * `transient` and `formula`: persisted field cannot become non-persisted
  
  </details>

* Remove `auth.provider.xx.absoluteUrlRequired` property
* Allow setting min/max to blank in order to remove the attribute in code generator
* Define maximum number of records per page

  <details>
  
  This change the default `api.pagination.max-per-page`, currently allowing unlimited 
  number of records per page, to 500.
  
  </details>

* Admins group can now customize views by default
* Disable sorting on grids having `canMove="true"`
* Let JpaFixture items persist error propagate

  <details>
  
  JpaFixture is used for tests.
  
  In case of data error, tests failed without the actual cause, and there is no point in continuing
  and have obscure errors happen on wrong data.
  
  </details>

* Support of `X-Forwarded-Context` header removed in favor of `X-Forwarded-Prefix`
* Reorder HTML widget buttons for consistency with Markdown widget
* Reorder toolbar icons for consistency with Markdown widget
* Move JpaFixture to axelor-test module
* Upgrade HSQL JDBC from 2.6.1 to 2.7.0
* Upgrade MySQL JDBC from 8.0.29 to 8.0.30
* Upgrade UnboundID LDAP SDK from 6.0.5 to 6.0.6
* Upgrade Junit from 5.8.2 to 5.9.1
* Upgrade Upgrade embedded Tomcat from 9.0.63 to 9.0.65
* Upgrade StringTemplate from 4.3.3 to 4.3.4
* Upgrade Greenmail from 1.6.9 to 1.6.10
* Upgrade Redisson from 3.17.3 to 3.17.6
* Upgrade Resteasy from 4.7.6 to 4.7.7
* Upgrade Groovy from 3.0.10 to 3.0.13
* Upgrade Flyway from 8.5.11 to 9.3.1
* Upgrade Hibernate Validator from 6.2.3 to 6.2.4
* Upgrade Woodstox from 6.2.8 to 6.3.1
* Upgrade Undertow from 2.2.17 to 2.2.19
* Upgrade EclipseLink MOXy from 2.7.10 to 2.7.11
* Upgrade Shiro from 1.9.0 to 1.9.1
* Upgrade Spotless from 6.5.1 to 6.11.0
* Upgrade Jsoup from 1.14.3 to 1.15.3
* Upgrade Pac4j from 5.4.3 to 5.4.5
* Upgrade Caffeine from 3.1.0 to 3.1.1
* Upgrade Hazelcast from 5.1.1 to 5.1.3
* Upgrade Jackson from 2.13.3 to 2.13.4
* Upgrade Infinispan from 13.0.10 to 13.0.11
* Upgrade PostgreSQL JDBC from 42.3.6 to 42.5.0
* Upgrade Tika from 2.3.0 to 2.4.1
* Upgrade Byte Buddy from 1.12.10 to 1.12.17
* Upgrade Ehcache from 3.10.0 to 3.10.1
* Upgrade Gradle from 7.4.2 to 7.5.1
* Upgrade Gradle Node Plugin from 3.2.1 to 3.4.0

#### Features

* Improve support of `X-Forwarded-*` headers

  <details>
  
  App now have full support for `X-Forwarded-*` headers and provides
  better usage of proxy management.
  
  Supported headers are: `X-Forwarded-Host`, `X-Forwarded-Port`, 
  `X-Forwarded-Proto`, `X-Forwarded-Prefix`, `X-Forwarded-For`.
  
  pac4j usage is now based on the current request. When redirecting urls, 
  the Location is now absolute and no more relative to current servlet path.
  This avoids custom proxy configuration to rewrite the location or cookie path.
  
  </details>

* Implement setting scale on grid column by action

  <details>
  
  `scale` attribute on a decimal field can be change on a grid column by an action.
  
  ```xml
  <action-attrs name="action-set-scale">
    <attribute for="items.price" name="scale" expr="eval: 10"/>
  </action-attrs>
  
  <form ...>
    ...
    <panel-related field="items">
      ...
      <field name="price"/>
    </panel-related>
    ...
  </form>
  ```
  
  </details>

* Displays the number of displayed and totaled items on the DMS list view
* Use colored letter on top right corner as placeholder user icon

  <details>
  
  This aligns user display with mail message and collaboration.
  
  </details>

* Implement Markdown widget using TOAST UI Editor with Code Syntax Highlight Plugin

  <details>
  
  Example:
  
  ```xml
  <field name="myTextField" widget="markdown"/>
  ```
  
  | Attribute           | Description                                                    |
  | ------------------- | -------------------------------------------------------------- |
  |`x-lite`             | Enable lite toolbar (defaults to `false`)                      |
  |`x-preview-style`    | Markdown editor's preview style: `tab` (default), `vertical`   |
  |`x-initial-edit-type`| Initial editor type: `markdown` (default), `wysiwyg`           |
  |`x-hide-mode-switch` | Whether to hide edit typo switch tab bar (defaults to `false`) |
  
  </details>

* Fall back to colored letter user image in mail message thread in case of permission failure
* Allow to get value of field with selection in string templates

  <details>
  
  For example, use `<SaleOrder.statusSelect.value>` to get the value of the selection.
  With `<SaleOrder.statusSelect>`, you still get the title of the selection.
  
  </details>

* Support x-field attribute with InfoButton widget to specify the bound field

  <details>
  
  Example:
  
  Use the `x-field` attribute on `info-button` widget to specify the bound field. When using `x-field`, 
  the button and the field are 2 distinct elements. Any attributes defined on that field will be used to 
  format the value. Moreover, this allows to change the button attributes without impact on the bound field.
  
  ```xml
  <panel>
    <button name="amountBtn" title="Amount" widget="info-button" x-field="totalAmount" onClick="my-action"/>
    <field name="amount" hidden="true"/>
  </panel>
  ```
  
  </details>

* Dynamically evaluate x-scale from context

  <details>
  
  On a decimal field, `x-scale` attribute accept an field name for a dynamic evaluation.
  Grid and form view are both supported.
  
  ```xml
  <field name="decimalField" widget="Decimal" x-scale="currency.decimalPlaces" x-precision="18"/>
  ```
  
  </details>

* Implement support to see users on same view in realtime

  <details>
  
  This allows to see users that are seeing/editing/updating the current opened record.
  
  Feature can be disabled with `view.collaboration.enabled` property. By default, it is enabled.
  On groups, there is a new boolean `canViewCollaboration` to determine whether members
  can view collaboration (`true` by default).
  
  </details>

* Introduce default number of items displayed per page config

  <details>
  
  Introduce new config `api.pagination.default-per-page` :
  
  ```
  # Define the default number of items per page
  api.pagination.default-per-page = 40
  ```
  
  This config is used in UI, especially in grid views, to define the default number of items 
  displayed per page. Default value is still 40 records.
  
  </details>

* Allow code generator to merge transient and multirelational fields

  <details>
  
  Previously it was not able to override any attributes of transients and collections fields.
  Now it is allow to change some of their attributes.
  
  </details>

* Implement client-side sorting of o2m/m2m grids

  <details>
  
  When there were some pending changes on a grid, sorting used to be disabled.
  This is no longer the case thanks to client-side sorting.
  Also transient/dummy fields can now be sorted.
  Only o2m/m2m grids can be sorted client-side.
  Other grid views that use pagination still send search request upon sorting.
  
  </details>

#### Fixed

* Fix truth value of action test expressions

  <details>
  
  When evaluating action test expressions, "expr" should have the same truth value as "!!expr".
  
  It was working with types boolean, integer, date, time, datetime, enum, references (any-to-one),
  but was failing with long, decimal, string, binary, collections (any-to-many).
  
  </details>

* Fix x-scale="0"
* Fix blank m2o in grid when there is no namecolumn

## 6.0.4 (2022-11-02)

#### Changes

* Upgrade Hibernate from 5.6.9 to 5.6.12

  <details>
  
  This fixes null mapped one-to-one field on an entity loaded from L2 cache.
  
  </details>

#### Features

* Update meta field labels and descriptions when restoring meta models

  <details>
  
  Update meta field labels so that they can be used in grid customization.
  
  </details>

* Improve details-view usage

  <details>
  
  In a details-view, the form view can be closed on demand or if there is no selected 
  line. When we delete a line in the grid, the record in the form is no more displayed.
  
  This also fix duplicated requests, missing onLoad/onNew calls in some conditions and 
  reset attrs states on form view.
  
  </details>

#### Fixed

* Preserve scroll position after saving form in details view
* Fix grouped grid display when already initialized
* Fix chart context evaluation when calling from action-view
* Fix page on grid customization popup when containing extra fields
* Make sure grids have committed before firing button action

  <details>
  
  Fixes application that can become unresponsive after clicking on a button on a form view
  while having uncommitted editable grids.
  
  </details>

* Fix record pager display issue in popup
* Fix missing cell css applied on item's parent
* Merge search-filter fields title with the model field title in grid customization popup
* Fix query fetching missing fields in TagSelect widget
* Fix delete metafile if target file is not found
* Fix dependency conflict between GraalJS and Birt with package "com.ibm.icu"
* Fix adding duplicate dotted field in grid customization

  <details>
  
  Fix adding duplicate dotted field in grid customization
  when it exists both in view and search filters.
  
  </details>

* Fix customize dashboard with drag & drop
* Fix wrong number of attachments displayed on record toolbar paper clip icon

## 6.0.3 (2022-09-28)

#### Features

* Add view action to help popover

#### Fixed

* Fix missing item context after creating M2M item from popup
* Check for `view.allow-customization` setting when saving customized view
* Fix save action on top grid preventing further actions
* Fix wrong css class applied on editable grid
* Fix custom fields on editable grid
* Fix restarting jobs after scheduler shutdown
* Fix model class resolution in Groovy expressions
* Fix refreshing html dashlet when record changes
* Fix non editable grid view customization popup
* Fix required field using TagSelect widget
* Skip default values of dotted field in editable grid
* Fix timezone issues with date adapter

  <details>
  
  When the server is running on UTC- timezone, date could be converted back by one day.
  
  </details>

* Fix conflicting `Order` title
* Fix saving boolean false filter

  <details>
  
  In the case of boolean fields with operator `false`,
  filter was transformed in order to check for null or false.
  But by doing that, original criteria was lost, breaking meta filter saving.
  Replaced client-side operators `true` and `false` by new virtual operators
  `$isTrue` and `$isFalse` which perform client-side transformation.
  
  </details>

* Fix ReferenceError with "<=" operator on custom date/datetime fields
* Fix sidebar toggle on window resize
* Fix client authentication using path based callback url

  <details>
  
  Some clients, ie AzureAd2Client, use path based callback url (/callback/AzureAd2Client) instead of default query based callback url (/callback?client_name=AzureAd2Client).
  
  </details>

* Fix missing TagSelect placeholder until we add and remove an item
* Fix auth properties that exist in client and configuration

  <details>
  
  When a property exists in the configuration and the client, try to set both.
  
  For example, `scope` exists in both `GenericOAuth20Client` and `OAuthConfiguration`.
  
  </details>

* Fix clipped field in Modern theme when using `css="large"` on WebKit-based browsers
* Apply client-side operators on export and masss update

  <details>
  
  Applying client-side operators was done on search only.
  Now, it is also done on exports and mass updates.
  
  </details>

* Fix setting $-prefixed dummy fields from onNew with default values

  <details>
  
  Issue happened when setting $-prefixed dummy fields from onNew
  and record has some default values.
  
  </details>

* Fix wrong selected rows after grid sorting
* Fix custom date/datetime criteria processing combined with column search
* Use access token to retrieve user profile picture from OpenID Connect

  <details>
  
  This fixes retrieving user profile picture from Azure Active Directory,
  where sending the access token is required.
  
  </details>

* Fix refreshing html view from tab right click
* Fix save action with only default values

  <details>
  
  This allow to save a record that contain default values (generally coming from domain definition) 
  when calling `save` action. This behavior will be same as the toolbar save button behavior.
  
  </details>

* Fix changing dashlet url from `action-attrs`
* Fix setting authentication map property

  <details>
  
  This fixes setting `GenericOAuth20Client.profileAttrs`.
  Issue happened when there was no getter.
  
  ```properties
  # profile attributes: map of key: type|tag
  # supported types: Integer, Boolean, Color, Gender, Locale, Long, URI, String (default)
  auth.provider.oauth.profile-attrs.age = Integer|age
  auth.provider.oauth.profile-attrs.is_admin = Boolean|is_admin
  ```
  
  </details>


## 6.0.2 (2022-08-03)

#### Features

* Improve report-box template in custom view

  <details>
  
  * Implement icon attribute
  * Format value
  * Translate label
  * Implement dynamic percent style
  * Implement dynamic percent level style
  * Fix tag positioning
  
  </details>

* Add config to define the maximum number of items displayed per page.

  <details>
  
  Introduce new config `api.pagination.max-per-page` :
  
  ```
  # Define the maximum number of items per page
  api.pagination.max-per-page = 1000
  ```
  
  This config is used globally in UI to limit and set the maximum number of items 
  displayed per page. -1 means unlimited (default value). This will block users who 
  try to get a high number of records. Fetch a too large dataset can result in 
  some high server side load.
  
  </details>

* Minor fields tracking UI improvements

  <details>
  
  Add minor fields tracking UI improvements : 
  
  - Use right arrow character instead of right angle quote
  - Display `None` instead of an empty value
  - On create event, don't generate tracking on empty field
  
  </details>

* Improve `report-table` custom view

  <details>
  
  * Translate titles
  * Use title attribute from fields
  * Preserve column declaration order from dataset if report-table columns
  attribute is not specified
  * Format data according to field type, including selection with translation
  * Support field translatable attribute
  * Sticky header and footer
  * Skip having to define data='data'
  * Ability to sort by columns
  * Ability to use widgets supported in grid views
  * Allow to export dataset from dashlet export button
  
  Example:
  ```xml
  <custom name="my-report-order-lines" title="Order lines">
    <field name="statusSelect" type="integer" selection="selection-order-status" title="Status"/>
    <field name="product" type="string" x-translatable="true"/>
    <field name="total" type="decimal" x-scale="2" />
    <dataset type="jpql" limit="10">
    <![CDATA[
    SELECT self.name AS name, self.statusSelect AS statusSelect,
           item.product.name as product, item.quantity * item.price AS total
    FROM Order self
    LEFT JOIN self.customer AS c
    LEFT JOIN self.items AS item
    WHERE c = :customer
    ORDER BY self.name
    ]]>
    </dataset>
    <template>
    <![CDATA[
    <report-table sums='total'></report-table>
    ]]>
    </template>
  </custom>
  ```
  
  </details>

#### Fixed

* Fix chart dashlet data export
* Fix unregistered request scope causing database task to fail

  <details>
  
  If application has any bound RequestScoped classes, it will cause the database task to fail
  to install AppModule with "No scope is bound to com.google.inject.servlet.RequestScoped." error.
  
  </details>

* Fix advanced search with custom any-to-many fields
* Fix html view height in popup
* Ignore forceEdit if user has no write permission
* Fix TypeError when user has insufficient permission to display a panel tab
* Fix duplicate data loading of kanban dashlet inside form view
* Fix context of kanban in dashlet

  <details>
  
  Apply the same context behavior as other dashlets,
  ie. merge action-view context with current record context.
  
  Instead of domain, use criteria (as it is done for calendars) for the `columnBy` filter,
  as to avoid conflict with current record context.
  
  </details>

* Fix reload action on cards and kanban view
* Fix grid JS error if action sent a list of IDs instead of a list of records
* Don’t update the number value on spin events

  <details>
  
  This fixes action iconsistencies between changing number value via manual input and spinner.
  
  </details>

* Take into account view-param limit on kanban views
* Fix spurious invalid fields notice when master detail is not shown
* Fix adding row on top editable grid when editor buttons are disabled
* Prevent popup from handling editable grid key down events

  <details>
  
  This fixes popup closing when pressing escape in editable grid.
  
  </details>

* Fix the view opened on mail thread creator link

  <details>
  
  The `user-info-form` view can be bypassed if we refresh the browser tab after 
  clicking on mail thread creator. The view is now opened in popup. If no view 
  is defined, nothing is opened (no more fallback on `user-form`).
  
  </details>

* Fix grid column search combined with predefined and custom search filters
* Don’t check for CSRF tokens with direct clients
* Don't reload dashlet calendar data when the widget is not visible
* Fix adding row on grouped editable grid
* Fix array of strings in context

  <details>
  
  For example:
  
  ```xml
    <context name="_myStrings" expr="eval: ['hello', 'world']"/>
  ```
  
  </details>

* Commit editable grid when closing popup without confirming

  <details>
  
  In a popup containing an editable grid, the line being edited was not committed if we directly
  close the popup via the "OK" button without clicking on "Confirm" in the editable grid.
  
  </details>

* Fix missing group on views and menus when loading
* Fix emptying a field using SingleSelect widget
* Fix uninitialized injector when running database task
* Fix broken translation popup
* Fix exporting selection field if multi-select widget is used

## 6.0.1 (2022-06-27)

#### Changes

* Upgrade embedded Tomcat from 9.0.62 to 9.0.63
* Upgrade Hibernate ORM from 5.6.8.Final to 5.6.9.Final
* Upgrade Byte Buddy from 1.12.9 to 1.12.10
* Upgrade Infinispan from 13.0.9 to 13.0.10
* Upgrade Redisson from 3.17.1 to 3.17.3
* Upgrade PostgreSQL JDBC from 42.3.4 to 42.3.6
* Upgrade Flyway from 8.5.10 to 8.5.11
* Upgrade Jackson from 2.13.2 to 2.13.3
* Upgrade UnboundID LDAP SDK from 6.0.4 to 6.0.5
* Rework mail message endpoints

  <details>
  
  Some mail message endpoints has been reworked to use dedicated path. This avoid to give permission 
  to `MailMessage` or `MailFlag` objects in order to access the mail menus and deal with messages and flags.
  `MailMessage` action-view is now always allowed in order to open mail views.
  
  </details>

* Upgrade GreenMail from 1.6.8 to 1.6.9

#### Features

* Update file name format for duplicate file name

  <details>
  
  Update format from `file (1).txt` to `file-1.txt`. This avoids 
  necessary space in file name.
  
  </details>

* Show configured application logo in about page
* Report duplicate view items at the end of the view loading process
* Sanitize uploaded filenames

  <details>
  
  Sanitize uploaded filenames :
  - Removes special characters that are illegal in filenames on certain operating systems
  - Replaces spaces and consecutive underscore with a single dash
  - Trims dot, dash and underscore from beginning and end of filename
  
  </details>

#### Fixed

* Don’t save meta filters with spurious `$new` attribute
* Fix saving filter with string field

  <details>
  
  In the case of string fields with operators `isNull` and `notNull`,
  filter was transformed in order to check for null or empty.
  But by doing that, original criteria was lost, breaking meta filter saving.
  
  Instead, use new virtual operators `$isEmpty` and `$notEmpty`
  which perform client-side transformation.
  
  </details>

* Fix MetaModule#application flag not filled
* Take column filters into account when refreshing dashlets
* Fix calendar dashlet refresh after refreshing/saving form view
* Fix mail thread avatar img display
* Fix downloaded filename from html action-view
* Fix uploading file if a file with the same name exist
* Fix missing name and color fields from selector with TagSelect widget
* Fix missing `_domainAction` from context of kanban in dashlet

  <details>
  
  The attribute `_domainAction` is needed to reevaluate the context server-side.
  
  </details>

* Fix ignored canNew attribute and permissions when pressing enter on last row of editable grid
* Fix deadlock when loading views of different types with same 'id'
* Fix meta file not found error
* Fix downloaded filename encoding in Content-Disposition header
* Clear dashlet filters when changing to another record
* Allow users to paste into password fields on change password form

  <details>
  
  Preventing password pasting undermines good security policy.
  https://web.dev/password-inputs-can-be-pasted-into/?utm_source=lighthouse&utm_medium=devtools
  
  </details>

* Fix duplicate data loading for grid dashlets on dashboards
* Fix closing of resources in CSV/XML importers
* Fix chart onAction and onClick having no context

  <details>
  
  onAction and onClick should have context just like onInit.
  
  </details>

* Fix keyboard shortcut popup content
* Fix grouping by extra custom field in grid view

  <details>
  
  Fix grouping by custom field defined on JSON field other than default `attrs`.
  
  </details>

* Fix too many redirects error in case of missing pac4j user profile
* Fix missing `_domainAction` from context of calendar in dashlet

  <details>
  
  The attribute `_domainAction` is needed to reevaluate the context server-side.
  
  </details>

* Fix hide issue on archive/attach/log grid built-in toolbar buttons

  <details>
  
  `hidden` attribute on built-in toolbar buttons wasn't taken into account for 
  archive/attach/log buttons.
  
  For example:
  ```xml
  <toolbar>
    <button name="log" onClick=" " hidden="true"/> <-- Hide audit log button -->
    <button name="attach" onClick=" " hidden="true"/> <-- Hide attachment button -->
    <button name="archive" onClick=" " hidden="true"/> <-- Hide archive/unarchive button -->
  </toolbar>
  ```
  
  </details>


## 6.0.0 (2022-05-20)

#### Changes

* Re-implement action-ws using http client
* Update join table columns names due to database reserved words conflicts

  <details>
  
  Due to reserved words added in recent versions of supported databases, change columns name `groups` and `menus` 
  to respectively `group_id` and `meta_menu_id` in the join tables name `meta_menu_groups` and `meta_view_groups`.
  
  Use these SQL statements to upgrade existing database :
  ```sql
  ALTER TABLE meta_menu_groups RENAME COLUMN groups TO group_id;
  ALTER TABLE meta_menu_groups RENAME COLUMN menus TO meta_menu_id;
  ALTER TABLE meta_view_groups RENAME COLUMN groups TO group_id;
  ALTER TABLE meta_view_groups RENAME COLUMN views TO meta_view_id;
  ```
  
  </details>

* Upgrade from Guice 4.2.2 to 5.1.0
* Remove `com.axelor.event.Priority` in favor of `javax.annotation.Priority`
* Not allowed to customize the search engine grid
* Upgrade to Hibernate Search 5.11.10.Final
* Search engine improvements

  <details>
  
  The search engine view has been improved. It allows to implement recursive filters for more flexibility on the search condition,
  but also:
  * Add support for `Enum` field type in search fields.
  * Allow to define `limit` on each `select`. It gets preference over `limit` attribute of `search`.
  * Add support for `enum` and `selection` (also autodetect `multi-select` values) for fields in result fields.
  * Add support for returning only distinct records (based on `id`) on each `select`.
  * Allow to add an `if` condition on `where`. It the value of the expression is false, the element is skipped.
  * Add support for hiliting on row as well as on field.
  * Add support for adding buttons in grid view.
  
  Breakings changes:
  * The `orderBy` fields should refere to field names of the object graph (and no more the `as` attribute).
  * When searching on a multi-valued field (O2M/M2M), it shouldn't need to be suffixed with [] anymore. For example,
  `items[].product` should now be `items.product`.
  
  </details>

* Improve groups and jobs `data-init`

  <details>
  
  The default `admins` group is now marked as technicalStaff. The default
  `mail.fetcher` job is no more active by default.
  
  </details>

* Upgrade to StringTemplate 4.3.3
* Format numbers, dates, and currencies preferably according to the user language

  <details>
  
  Formatting of numbers, dates, and currencies used to be based on browser locale only.
  Now, formatting is done preferably according to the user language.
  If the user language has no country information, we select the first browser locale
  that matches the user language.
  
  </details>

* Upgrade to Guava 31.1-jre
* Rename `application.properties` to `axelor-config.properties`

  <details>
  
  The internal configuration file `application.properties` is renamed to `axelor-config.properties`
  
  </details>

* Upgrade from JDK 8 to JDK 11

  <details>
  
  https://docs.oracle.com/en/java/javase/11/migrate/index.html#JSMIG-GUID-7744EF96-5899-4FB2-B34E-86D49B2E89B6
  
  </details>

* Upgrade to Spotless 6.5.1
* Use jcache in combination with Caffeine as second-level cache provider

  <details>
  
  Ehcache2 second-level cache is deprecated since Hibernate 5.3.
  Use `jcache` in combination with Caffeine by default as second-level cache.
  
  </details>

* Rework actions on chart menu

  <details>
  
  Adding buttons on chart menu was working using the following syntax :
  `<config name="onAction" value="some-action"/>`
  
  This syntax has been updated to the following :
  ```
  <actions>
  <action name="myBtn1" title="My action 1" action="some-action1"/>
  <action name="myBtn2" title="My action 2" action="some-action2"/>
  </actions>
  ```
  
  This allows to support more than 1 button on chart menu, but also
  provide a flexible usage of the feature.
  
  </details>

* Upgrade PostgreSQL Jdbc to 42.3.4

  <details>
  
  This add support new `SCRAM-SHA-256` password encryption method which is  
  now the default password encryption method is PostgreSQL 14.
  
  Jdbc also introduce a change about Timestamp rouding : 2018-06-03T23:59:59.999999999 is rounded to 
  2018-06-04 00:00:00 (previously it was rounded to 2022-03-08 23:59:59.999). This new behavior follow psql’s suit.
  Especially if you work with Datetime/Timestamp having `LocalTime.MAX` as time, make sure to adjust your query.
  
  </details>

* Adopt a better configurations naming

  <details>
  
  A lot of configurations names has been updated to have better 
  and uniform naming across all settings. Refer to migration notes.
  
  </details>

* Support indirect/direct basic auth client via setting `auth.local.basic-auth`

  <details>
  
  Indirect basic auth allows to log in on callback, while direct basic auth requires credentials in each request.
  
  ```properties
  # enable indirect and/or direct basic auth
  auth.local.basic-auth = indirect, direct
  ```
  
  `auth.local.basic.auth.enabled`, which was for direct basic auth only, is removed.
  
  </details>

* Upgrade to Hibernate 5.6.8.Final

  <details>
  
  Most notably, positional parameters for native queries are now one-based.
  
  </details>

* Upgrade to intl-tel-input 17.0.16
* Upgrade to Gradle 7.4.2
* Fire PreRequest and PostRequest events outside of transactions

  <details>
  
  `PreRequest`/`PostRequest` events are now fired outside of transactions.
  This fixes accessing the created/update records in a multithreaded process from `PostRequest` observers.
  However you can no longer rollback the request process in a `PostRequest` observer.
  
  </details>

* Upgrade to Apache Tomcat® 9.x
* Upgrade to pac4j 5.4.3
* Reimplement code generator in Java

  <details>
  
  Dropped old code generator written in Groovy in favor of a new code generator written in Java.
  
  </details>

* Upgrade JUnit4 from 4.12 to 4.13.2
* Upgrade from Groovy 2.4.10 to 3.0.10

  <details>
  
  Beware of breaking changes.
  See release note for more details :
  - https://groovy-lang.org/releasenotes/groovy-2.5.html
  - https://groovy-lang.org/releasenotes/groovy-2.6.html
  - https://groovy-lang.org/releasenotes/groovy-3.0.html
  
  </details>

* Merge `com.axelor.app` and `com.axelor.app-module` gradle plugins

  <details>
  
  Now there is only a single `com.axelor.app` plugin. All modules
  should use the app plugin only.
  
  Now all axelor modules are axelor apps.
  
  </details>

* Upgrade to Hibernate Validator 6.2.3.Final
* Default gantt view mode is now `month` instead of `week`

  <details>
  
  This can be changed on `<gantt>` view definition using the 
  `mode` attribute.
  
  </details>

* Upgrade to Shiro 1.9.0
* Replace nashorn script helper with GraalJS engine

  <details>
  
  The nashorn script engine is deprecated in JDK-11 and has some
  incompatible changes than JDK-8.
  
  The new implementation uses GraalJS which supports latest ECMAScript features.
  
  The collection helpers `listOf`, `setOf` and `mapOf` are removed as corresponding
  native JavaScript objects are passed with appropriate Java equivalent wrapper to the
  Java calls.
  
  </details>

* Rename method `getCodeOrEmail` to `getUserIdentifier` in `AuthPac4jProfileService`

  <details>
  
  Use a more generic method name, because users may override and use custom behavior.
  
  </details>

* Rework authentication implementation to use reflection and providers

  <details>
  
  Reflection is now used to configure authentication clients.
  The new syntax is `auth.provider.<providerName>.<configurationName>`.
  You may use any of the built-in providers: `google`, `facebook`, `github`, `azure`, `keycloak`,
  `apple`, `oauth`, `oidc`, `saml`, `cas`.
  Or you can configure any other clients supported by pac4j using your own custom provider name.
  You may even create and use your own custom authentication clients.
  
  </details>

* Upgrade to Quartz 2.3.2

#### Features

* Allow to customize notify/info/alert/error message modal/notification

  <details>
  
  This allow to add more customization on modal/notification : 
  - `notify` : allow to change the title of the notification
  - `info` : allow to change the title of the popup and the title of the confirm button
  - `alert` : allow to change the title of the popup and the title of the confirm and cancel button
  - `error` : allow to change the title of the popup and the title of the confirm button
  
  Example usage:
  
  ```xml
  <action-validate name="my-action">
    <notify message="A notification" title="My notif"/>
    <info message="This is an info" title="My info" confirm-btn-title="Got it"/>
    <alert message="This is an alert" title="My alert" confirm-btn-title="Got it" cancel-btn-title="Abort"/>
    <error message="This is an error" title="My error" confirm-btn-title="I understand" cancel-btn-title="Do something else"/>
  </action-validate>
  ```
  
  or with `ActionResponse`:
  
  ```java
    response.setNotify("A notification", "My notif");
    response.setInfo("This is an info", "My info", "Got it");
    response.setAlert("This is an alert", "My alert", "Got it");
    response.setError("This is an error", "My error", "I understand", "Do something else");
  ```
  
  </details>

* Parallelize generation of computed views
* Use Gradle Node Plugin to run npm tasks

  <details>
  
  No more need to install Node.js locally to build web resource bundles, aka `npm-build` task.
  
  If your project already depends on `gradle-node-plugin`, there will be conflicts between both. As the provided plugin 
  is only applied on root project, the best fit is to delegate the process to a subproject. That way, the subproject 
  can process and build your needs, and the root project can depends on that subproject and gets produced build.
  
  To use it, you need to Declare Node.js repository in `settings.gradle`, section `dependencyResolutionManagement` : 
  ```gradle
    // Declare the Node.js download repository
    ivy {
      name = "Node.js"
      setUrl("https://nodejs.org/dist/")
      patternLayout {
        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
      }
      metadataSources {
        artifact()
      }
      content {
        includeModule("org.nodejs", "node")
      }
    }
  ```
  
  See https://github.com/node-gradle/gradle-node-plugin/blob/3.1.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
  
  </details>

* Implement WebSocket support
* Add support to encrypt secrets in properties

  <details>
  
  Secrets defined in `axelor-config.properties` can be encrypted. 
  Value should be wrapped in `ENC()` to indicate that the value is
  encrypted : `db.default.password = ENC(<some_thing>)`
  
  To use encrypted secrets, `config.encryptor.password` properties
  should be added : this is the secret key used to encrypt/decrypt data.
  
  Others optional properties can be added to use custom encryption : 
  `config.encryptor.algorithm`, `config.encryptor.key-obtention-iterations`, 
  `config.encryptor.provider-name`, `config.encryptor.provider-class-name`, 
  `config.encryptor.salt-generator-classname`, `config.encryptor.iv-generator-classname`, 
  and `config.encryptor.string-output-type`.
  The default algorithm is `PBEWITHHMACSHA512ANDAES_256`.
  
  For convenience, a Gradle task `encryptText` have been added to generate 
  the encrypted value of a string :
  `./gradlew :encryptText --text="A secret to encode" --password="MySecureKey"`
  This will generate for you, the necessary properties and the 
  encrypted value to used inside `ENC()`.
  
  </details>

* Add application setting `auth.order` to set authentication provider order

  <details>
  
  Because of multisource application settings, settings order is undefined.
  Use `auth.order` if you require authentication providers to be displayed in a specific order
  on login page.
  
  </details>

* Add JpaRepository#findByIds(List<Long>) method to find multiple entities by their primary key
* Add support to MySQL 8
* Allow closing preference view manually with `canClose` or `close` action
* Add validation support for csv import

  <details>
  
  Two new attributes added to validate data being imported.
  
  - `check` - boolean expression
  - `check-message` - the validation message
  
  If `check` fails, the `check-message` or default validation message
  is shown and import will be terminated.
  
  </details>

* Add Gantt view attribute `mode`

  <details>
  
  Gantt view attribute `mode` allows to define the default view mode.
  It can be set to `year`, `month`, `week`, or `day`.
  
  </details>

* Add some predefinied css classes for dialogs and notify components
* Sanitize HTML where it is needed with DOMPurify

  <details>
  
  Instead of sanitizing all jQuery.html calls, use DOMPurify to sanitize HTML where it is needed only.
  This should reduce scripting load in the web client.
  
  </details>

* Perform accent-insensitive navigation search
* Refactoring menu and tags processing

  <details>
  
  This is a refactoring on how menus and tags are processing. The original way was complex and mixing tags 
  and menus rules in a single method. There is no changes on how it works.
  
  Now, menus and tags processing are separate. Moreover, instead of parsing each menu one by one (but also recursively 
  checking parent access), it builds a tree. The tree is a representation of the menus' hierarchy, where each menu have 
  a parent and some child's. Then the tree is traversed, checking each node access. When a node is allowed, it 
  continues traversing each child's. When a node is not allowed, it skips the node and all the child's.
  
  </details>

* Support YAML configuration format, env variables and system properties

  <details>
  
  Now support YAML format for internal configuration file :
  `axelor-config.properties` can be in YAML format (`yml` or `yaml` ext).
  It should only have a one internal configuration file (in properties
  or YAML format).
  
  External configuration file can be provided by using system properties
  (`axelor.config=<path_to_file>`) or using env variables
  (`AXELOR_CONFIG=<path_to_file>`). Same as the internal configuration
  file, it also supports YAML format.
  
  The internal configuration file is now optional. Final properties
  are built using internal configuration file + external configuration
  file + env variable + system properties. If variables are redefined,
  they will take preferences over the previous values.
  
  Configuration values can also be provided with system properties
  using `axelor.config.<key>=value` format. For example
  `db.default.user` becomes `axelor.config.db.default.user`.
  
  Configuration values can also be provided with environment variables
  using `AXELOR_CONFIG_<key>=value` format, where `<key>` is underscored
  uppercase equivalent of the configuration key. For example
  `db.default.user` becomes `AXELOR_CONFIG_DB_DEFAULT_USER`.
  
  </details>

* Add `QuickMenu` to allow running actions from default page

  <details>
  
  Example usage:
  
  ```java
  public class MyQuickMenu implements QuickMenuCreator {
  
    @Override
    public QuickMenu create() {
      final QuickMenu menu = new QuickMenu();
      menu.setTitle("My menu");
      menu.setOrder(0);
      menu.setShowingSelected(false);
      menu.setItems(
          List.of(
              new QuickMenu.Item("All projects", "project.all"),
              new QuickMenu.Item("All tasks", "project.task.all")));
      return menu;
    }
  }
  ```
  
  and register the QuickMenu in module configuration : 
  
  ```java
  public class MyModule extends AxelorModule {
  
    @Override
    protected void configure() {
      addQuickMenu(MyQuickMenu.class);
    }
  }
  ```
  
  </details>

* Improve index generation

  <details>
  
  We can now specify order on column names with `ASC` or `DESC`.
  Some examples:
  ```xml
    <index columns="code,name"/>
    <index columns="code ASC,name DESC"/>
    <index columns="code,name DESC"/>
  ```
  
  We can also specify whether the index is unique. 
  ```xml
    <index columns="code,name" unique="true"/>
  ```
  This will replace and makes `<unique-constraint ... />` obsolete in future.
  
  </details>

* Allow to enable/disable cache from `axelor-config.properties`

  <details>
  
  `javax.persistence.sharedCache.mode` property in `axelor-config.properties` can be used
  to overwrite the `shared-cache-mode` from `persistence.xml`. It allow to enable/disable
  the shared cache mode (ie second-level cache).
  
  </details>

* Add support to manage multiple matching notify in `action-validate`

#### Fixed

* Fix undefined when calling search engine with route params
* Fix secure cookie on login attempt or change tenant request
* Prevent concurrent meta restoring
* Fix usage of multiple matching `info` in `action-validate`

  <details>
  
  In `action-validate`, if there is multiple `info` matching,
  only the first will be displayed on UI.
  
  </details>

* Fix radio style for grid selector
* Fix MySQL exception reporting
* Use whole value comparisons in datetime searches instead of using `between` operator

  <details>
  
  PostgreSQL JDBC driver 42.2.3+ introduce a change on how Timestamp are rounded : 
  https://github.com/pgjdbc/pgjdbc/issues/1211
  
  For example, `2018-06-03T23:59:59.999999999` is rounded to `2018-06-04 00:00:00`
  Previously `2018-06-03T23:59:59.999999999` was rounded to `2022-03-08 23:59:59.999`
  
  If nanoseconds is greater than 999999500 , value is now rounded. This is how PostgreSQL works :
  ```
  $ insert into some_table(date) VALUES('2018-06-03 23:59:59.999999999');
           date            
  ----------------------------
  2018-06-04 00:00:00
  ```
  
  The `between` operator used when searching on date/dateTime in UI have been updated to use 
  `>=` and `<` operators instead.
  
  </details>

* Fix JAXP usage

  <details>
  
  Thread safe usage of JAXP API. Use `XMLUtils` to protect XML Parsers from XXE attacks 
  but also disable external entity processing.
  
  </details>

#### Removed

* Remove deprecated usage of `hashKey` and `hashAll` attributes

  <details>
  
  Use `equalsInclude` and `equalsIncludeAll` instead.
  
  </details>

* Remove deprecated `Query#fetchSteam` methods

  <details>
  
  Use `Query#fetchStream` instead.
  
  </details>

* Remove domain model lang attribute

  <details>
  
  Domain models are generated in Java only.
  
  </details>

* Remove dependency to OpenCSV
* Remove deprecated `ActionHandler(ActionRequest)` method

  <details>
  
  Use `ActionExecutor#newActionHandler(ActionRequest)` instead
  
  </details>

* Remove deprecated `Context#getParentContext` method

  <details>
  
  Use `Context#getParent` instead.
  
  </details>

* Remove deprecated `cachable` domain attribute

  <details>
  
  Use `cacheable` attribute instead.
  
  </details>

* Remove ModuleChanged event

  <details>
  
  Since dropping support of removable modules, this event is no more used.
  
  </details>

* Remove deprecated `LoginRedirectException`

  <details>
  
  Use `WebUtils.issueRedirect` instead.
  
  </details>

* Remove IDE app launcher support
* Remove `setFlash` in `ActionResponse` to be aligned with `action-validate#info`

  <details>
  
  Use `setInfo` instead.
  
  </details>

* Remove legacy form widgets `<notebook>`, `<break>`, `<group>`, `<portlet>`, and `<include>`

  <details>
  
  Form widgets `<notebook>`, `<break>`, `<group>`, `<portlet>`, and `<include>` are deprecated.
  `cols` and `colWidths` form attributes used for legacy form layout are also deprecated.
  Those have be removed. Use panel layout instead.
  
  </details>

* Drop removable module support

  <details>
  
  The feature is not used by any axelor apps and has many
  technical issues.
  
  Run following SQL script to drop unnecessary columns
  
  ```
  alter table meta_module drop column installed;
  alter table meta_module drop column removable;
  alter table meta_module drop column pending;
  ```
  
  </details>


## 5.4.13 (2022-03-11)

#### Features

* Improve MasterDetail usage

  <details>
  
  MasterDetail form is no more displayed by default, but only when a line is selected and we want to create new record from.
  Also, when we add a new record, cancel current edit or click on back button, MaterDetail form is hidden.
  Add a new button "Add and new" to allow pushing current record in grid and quickly create a new one.
  
  </details>

* Add support to use MasterDetail with editable grid

  <details>
  
  When MasterDetail is used with editable grid, the form is readonly 
  and record is now live sync with the changes made in editable grid.
  
  </details>

#### Fixed

* Fix downloading attachment of action-report
* Fix permission checking with charts and custom views

  <details>
  
  Should not check for permission on `MetaView` when executing actions from charts.
  `onInit` action on charts use current model if calling from `action-view` else,
  `ScriptBindings` is used, so that we still have a context.
  
  </details>

* Fix file name downloaded in notify box
* Prevent grid sorting when there are dirty rows
* Allow dashboard drag&drop according to view customization permission

#### Security

* Add model attribute to action-report

  <details>
  
  If a model is not specified on action-report, permission is checked against context.
  
  </details>

* Check context read perm on action-report in case of attachment

  <details>
  
  If action-report attach the generated report to current object, it 
  now check for read permission on that object.
  
  </details>


## 5.4.12 (2022-03-04)

#### Fixed

* Add missing hilite style CSS
* Add missing attributes when processing widgetAttrs

## 5.4.11 (2022-02-23)

#### Features

* Allow access to custom fields in StringTemplate via dotted notation

  <details>
  
  StringTemplate doesn't allow `$` character in expressions. This is the way we use to access custom field
  from context : `$extraAttrs.myCustomField`. Accessing custom fields need to be prefixed by the related
  json field name (ex: `<SaleOrder.extraAttrs.myField>`or `<SaleOrder.attrs.anotherField>`). For custom fields
  in default `attrs` field, it is not needed to use prefix (ex: `<SaleOrder.myField>` ). However, it is
  recommended to always use the related json field name as prefix.
  
  </details>

* Add `axelor.view.watch.delay` and `axelor.view.watch.kinds` VM arguments

  <details>
  
  Defaults are as follows (working on Eclipse/IntelliJ on Linux):
  
  ```
  -Daxelor.view.watch.kinds=ENTRY_CREATE
  -Daxelor.view.watch.delay=300
  ```
  
  `axelor.view.watch.kinds` is a list of comma-separated strings of the following values:
  `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`.
  
  `axelor.view.watch.delay` is the update delay in milliseconds.
  
  Varying development environments may require special configuration in
  order to have view hotswap working.
  
  For example, on Windows, only `ENTRY_MODIFY` events might be triggered
  in some cases. Moreover, there may be a big gap between those events,
  requiring to increase the update delay. So one might want to use:
  
  ```
  -Daxelor.view.watch.kinds=ENTRY_CREATE,ENTRY_MODIFY
  -Daxelor.view.watch.delay=4000
  ```
  
  </details>

#### Fixed

* Fix parsing field widgetAttrs to preserve type depending on attribute

  <details>
  
  Extra widget attributes doesn't takes into account the type of each attributes. 
  This result of some unexpected behaviors. For example, the `x-true-text="true"` 
  cause error on `BooleanSelect` widget.
  Depending on the attribute, the value is converting to the right type.
  
  </details>

* Preserve selected flags from action values

  <details>
  
  Preserve selected flags when using `response.setValue(field, collection)`
  with collection of maps without version field.
  
  </details>

* Sort advanced search fields in lexicographic order
* Fix view hotswap when running embedded Tomcat
* Fix JS error when having favorite menus with duplicate names

  <details>
  
  In "Organize favorites", in the user changes a menu link, they may then add another menu with the same name,
  causing AngularJS error tracking for unique names.
  
  </details>

* Fix panel `showTitle` attribute
* Translate toolbar menu customization shown to technical staff
* Fix back button when view-parem `forceEdit` is used
* Fix m2m field update upon select record

  <details>
  
  Populated the m2m items all data. Only omit the version
  field (not fully populated as the record is already saved)
  
  </details>

* Fix position of field error message to be always to bottom of the field
* Fix file upload when using filename pattern

  <details>
  
  Property `file.upload.filename.pattern` should be evaluated on each upload
  in order to evaluate `{year}`/`{month}` or `{day}` keys.
  Moreover, when using a custom filename pattern, `{name}` should be placed at
  the end of the pattern. If omitted, he is appended by default at the end.
  
  </details>

#### Security

* Fix sanitize to prevent XSS vulnerabilities

## 5.4.10 (2022-02-02)

#### Fixed

* Fix AuthSecurityWarner initialization on app startup

  <details>
  
  Resource might be loaded before Guice is initialized, which causes "Guice is not initialized." error.
  
  </details>



## 5.4.9 (2022-01-31)

#### Deprecated

* Deprecate legacy form layout in favor of panel layout

  <details>
  
  Form widgets `<notebook>`, `<break>`, `<group>`, `<portlet>`, and `<include>` are deprecated.
  `cols` and `colWidths` form attributes used for legacy form layout are also deprecated.
  Those will be removed in the next major version. Use panel layout instead.
  
  </details>


* Deprecate Groovy domain models

  <details>
  
  In next major release, domain models will be generated in Java only.
  
  </details>



#### Features

* Add application setting to disable permission check on actions

  <details>
  
  Use `application.disable.action.permission = true` to disable permission checking on actions.
  That setting severely breaks security. It should be used with extreme caution and as a last resort only.
  It will be removed in the future.
  
  </details>


* Add application setting to disable permission check on relational fields

  <details>
  
  Use `application.disable.relational.field.permission = true` to disable permission checking on relational fields.
  That setting severely breaks security. It should be used with extreme caution and as a last resort only.
  It will be removed in the future.
  
  </details>


* Better visibility in DMS file details
* Add setting "auth.saml.logout.request.signed"

#### Fixed

* Allow to select multiple records in o2m/m2m inside an editor
* Prevent sorting when any row is dirty in o2m/m2m grid

  <details>
  
  This fixes losing changes after sorting on a column.
  
  </details>


* Fix default value of Boolean custom fields
* Fix checking for create permission on add actions
* Fix static/tooltip/button-group widget in views extensions
* Translate "Use offline..." text in dms file details
* Fix view hotswap with IntelliJ IDEA
* Check for permission in suggest box create actions

## 5.4.8 (2022-01-03)

#### Fixed

* Fix going into edit mode in editable grid with large horizontal scrolling
* Don’t trigger popup’s onLoad upon closing

  <details>
  
  When a popup was closing when saving the record, onLoad actions was triggered.
  This is unnecessary as the popup will be closed in all cases. It was also
  causing errors because the actions results was applied on a closed popup/form.
  
  </details>


* Fix m2m field update issue

  <details>
  
  The m2m items, upon select/edit should not be fully populated as the record
  is already saved (similar to m2o).
  In controllers, make sure to return a compact map in m2m fields, ie a
  list of map with the records ids. Then, the view will fetch the records
  with all necessary fields by itself.
  
  </details>



## 5.4.7 (2021-12-17)

#### Changes

* Only show access type error for all users

  <details>
  
  Technical and non-technical users are now notified the same undetailed access type error message.
  Details are shown in the logs only.
  
  </details>



#### Features

* Add support to UTF-8 BOM file in CSV Importer and `CSVFile` helpers class
* Log technical message for AuthSecurityException

#### Fixed

* Do not test pattern on empty string
* Check for create permission when adding row in editable grid
* Fix web service `perms` ignoring `action` parameter when no `id` is specified
* Translate tree node Expand/Collapse tooltip
* Exclude auditable fields, `created(On|By)` and `updated(On|By)`, when copying record

#### Security

* Check for relational permissions recursively in save request
* Escape data to prevent XSS vulnerabilities in JSP pages
* Upgrade to SLF4J 1.7.32 and logback 1.2.9

  <details>
  
  Mitigate LOGBACK-1591
  - slf4j to 1.7.32
  - logback to 1.2.9
  - jansi to 1.18
  
  See:
  - https://jira.qos.ch/browse/LOGBACK-1591
  - http://slf4j.org/log4shell.html
  - http://mailman.qos.ch/pipermail/announce/2021/000163.html
  
  </details>



## 5.4.6 (2021-11-30)

#### Changes

* Improve editable grid shortcuts to confirm/cancel edit

  <details>
  
  We can now use `ENTER` key to confirm current edit and `ESCAPE` key to cancel.
  
  </details>


#### Features

* Add `view.grid.editor.buttons` application configuration
* Add `canEdit` attribute to `panel-dashlet`

  <details>
  
  The new `canEdit` attribute on `panel-dashlet` will control whatever we can edit
  dashlet records even if the dashlet is in readonly. By default, when a `panel-dashlet`
  is in readonly (or main view is readonly mode or top container element is readonly),
  the record will be opened in popup. We can't edit the records. If editable, the record
  will be opened in a new tab, in readonly mode. We can then switch to edit mode to edit the record.
  
  Few more improvements :
  * Show file icon in grid dashlet when in readonly mode
  * Show pencil icon in grid dashlet when editable
  * Fix ignored dashlet attributes in dashboard
  
  </details>


#### Fixed

* Fix login failure event

  <details>
  
  Fire login failure event with form client, LDAP, and Basic Auth.
  
  Can't be triggered with other indirect clients like OAuth 2.0, OpenID
  Connect, SAML 2.0, etc.
  
  </details>

* Fix memory leak in session listener
* Fix horizontal scrolling in editable grid
* Fix field sometimes blank on initialization with CodeEditor widget
* Check for permission on messages requests
* Fix `commons.codec` dependency conflict when using SAML
* Check for read permission on action-record changing dummy fields
* Translate grid selection checkbox tooltip
* Fix readonly dashlet with forceEdit should open readonly popup in readonly mode
* Fix Gantt view sometimes blank on initialization
* Fix resetting of shared custom views
* Fix keyboard shortcuts on Mac-like devices

  <details>
  
  When Macintosh is detected, use specific keyboard shortcuts.
  Macintosh keyboards don’t have AltGr nor Insert keys.
  
  </details>

* Fix concurrent update error when moving tasks in Gantt view
* Check for permission on followers requests
* Fix NPE with null password to `AuthService#passwordMatchesPattern()`
* Fix blocked loading of custom translations
* Fix ignored regex on custom field
* Check for permission on attachments requests
* Fix dashlet with popup should open editable popup in editable mode

## 5.4.5 (2021-10-21)

#### Changes

* Add view customization permission on Group and `view.customization` application configuration

  <details>

  All users used to be allowed to customize views, and only admins could share.
  Now, view customization permission is checked on Group, and can be set to "Not allowed" (default),
  "Can customize", or "Can share".
  `view.customization` application configuration defaults to `true`.
  If set to `false`, Customize menu and custom views are disabled.

  </details>


#### Fixed

* Fix null value with BooleanRadio when switching from a record to another
* Fix permission check on action targeting a different model from current record

## 5.4.4 (2021-10-01)

#### Fixed

* Check for permission on action model if defined
* Fix infinite recursive popup grid loading
* Fix alignment of meta JSON field using contextField
* Fix JS error when M2O field has no namecolumn
* Add scrollbar in DMS Permission dialog box
* Fix missing ID on created records from TagSelect widget
* Fix detaching of one-to-one from non-owning side

#### Security

* Prevent script execution from custom stylesheet

## 5.4.3 (2021-09-08)

#### Changes

* Serialize Time with seconds

  <details>
  
  Time is now serialized with formatter "HH:mm:ss".
  Seconds are still not displayed by default in client (need to use `x-seconds="true"`).
  
  </details>

* Require manual closing of datetime picker
* Disable translation joins on queries by default

  <details>
  
  Add Query.translate() and Filter.translate() to enable translation joins.

  REST API now accepts parameter `translate` to enable translation joins
  on criteria and for ordering, but never for domains.

  Web client always uses the `translate` parameter.
  
  </details>


#### Features

* Clarify title/placeholder of user password fields

  <details>
  
  Clarify title/placeholder of user password fields in form view and change password page.

  * Change title depending on existing or new user
  * Add help popovers
  * Add password pattern help using `user.password.pattern.title` (customized via translation)
  
  </details>

* Use select control type on year, month, and time in datetime widget
* Add support for related parameters in domain expressions

  <details>
  
  Character `$` is used to access related fields in domain expression parameters.
  This can be used to access other JSON fields. Example:

  ```
  "self.product = :$extraAttrs$myCustomProduct"
  ```
  
  </details>

* Add view-param `grid-width` to set width of grid in details-view

  <details>
  
  Example:

  ```xml
  <action-view name="sale.orders" model="com.axelor.sale.db.Order" title="Sale Orders">
    <view type="grid" name="order-grid" />
    <view type="form" name="order-form" />
    <view-param name="details-view" value="true" />
    <view-param name="grid-width" value="25%" />
  </action-view>
  ```
  
  </details>

* Add multi-tenant aware thread implementation

  <details>
  
  The `com.axelor.db.tenants.TenantAware` is a custom `Thread` implementation
  that keeps track of current tenant and runs the thread in transaction with
  current tenant in the context.
  
  </details>

* Add setter methods for all attributes in XMLInput/XMLBind CSVInput/CSVBind
* Support adaptive date/time/datetime quick search in grid view
* Add support for CSV/XML data import of custom fields

  <details>
  
  Example:

  ```xml
  <!-- for CSV data -->
  <input file="some-data.csv" ...>
    <bind column="some" to="$attrs.some" />
  </input>

  <!-- for XML data -->
  <input file="some-data.xml" ...>
    <bind node="some" to="$attrs.some" />
  </input>
  ```

  For custom model records, you can specify `json-model` in order to automatically bind jsonModel,
  handle search domain, and set namecolumn.

  CSV import example:

  ```xml
  <input file="data.csv" json-model="ElectricityBillSubscription"
      search="json_extract_text(self.attrs, 'name') = :name">
    <bind column="name" to="$attrs.name" />
    <bind column="startDate" to="$attrs.startDate" adapter="LocalDate" />
    <bind column="endDate" to="$attrs.endDate" adapter="LocalDate" />

    <bind to="$attrs.billSubscription"
        search="json_extract_text(self.attrs, 'name') = :billName">
      <bind column="billName" to="$attrs.name" />
    </bind>
  </input>
  ```

  ```csv
  name,startDate,endDate,billName
  "Bill Jan2021","01/01/2021","31/01/2021","Test01"
  "Bill Feb2021","01/02/2021","28/02/2021","Test02"
  ```

  XML import example:

  ```xml
  <bind node="electricityBillSubscription" json-model="ElectricityBillSubscription" update="true"
      search="json_extract_text(self.attrs, 'name') = :_electricityBillSubscriptionName">
    <bind node="name" to="_electricityBillSubscriptionName" />
    <bind node="name" to="$attrs.name" />
    <bind node="startDate" to="$attrs.startDate" adapter="LocalDate" />
    <bind node="endDate" to="$attrs.endDate" adapter="LocalDate" />

    <bind node="billSubscription" to="$attrs.billSubscription" update="true"
        search="json_extract_text(self.attrs, 'name') = :_billSubscriptionName">
      <bind node="@name" to="_billSubscriptionName" />
      <bind node="@name" to="$attrs.name" />
    </bind>
  </bind>
  ```

  ```xml
  <electricityBillSubscription>
    <name>Bill Jan2021</name>
    <startDate>01/01/2021</startDate>
    <endDate>31/01/2021</endDate>
    <billSubscription name="Test01"/>
  </electricityBillSubscription>
  ```
  
  </details>

* Support `x-seconds` attribute on DateTime and Time widgets
* Add ui-action-context attribute to use with ui-action-click

  <details>
  
  The `ui-action-context="scopeVariable"` can be used with `ui-action-click` in custom view
  template to provide extra details in action context.
  
  </details>

* Expose $scope.openTab as axelor.$openTab

  <details>
  
  The api can be useful for some apps loading in iframe.
  
  </details>

* Improve the indicator and delay the wait cursor

  <details>
  
  - delay showing of loading indicator by 1 second
  - delay wait cursor till the wait spinner appears
  
  </details>

* Add support for view auto-reloading

  <details>
  
  Use `view-param` named `auto-reload`, with value specified in seconds. Example:

  ```xml
    <view-param name="auto-reload" value="30" />
  ```

  Auto-reloading is paused when tab is not selected or when record is in edit mode.
  
  </details>

* Add JSON field meta info on all views

#### Fixed

* Fix CSS of invalid BinaryLink
* Fix value update action after save in one-to-many/many-to-many editor
* Fix dirty form when there are custom multirelational fields
* Fix grid search on custom fields
* Fix grid customization when view has duplicate fields
* Fix duplicate computed views after upgrade
* Fix `ActionResponse.setReload` and `refresh-tab` signal from editor
* Fix NavSelect widget with M2O record having an empty name
* Fix attribute `x-currency` not taken into account
* Fix user notifications for followers added by email address
* Fix dashlet search triggering buttons
* Log errors when persisting JpaFixture items
* Exclude UI custom fields from exports
* Fix missing handling of LocalTime in JpaFixture::load

  <details>
  
  When loading fixture in tests with LocalTime fields, the JpaFixture throw
  a exception because LocalTime hasn't appropriate constructor.

  ```
  Caused by: org.yaml.snakeyaml.error.YAMLException: No single argument constructor found for class java.time.LocalTime : null
  ```
  
  </details>

* Wait for actions before confirming line on editable grid
* Fix unsupported regular expression in WebKit causing empty grid
* Fix currency filter with symbols other than $, €, ¥, £
* Use number input type on number fields
* Fix text value escape on M2O names
* Fix multi-tenant datasource configuration
* Fix generated code when setting `default="null"` on nullable boolean
* Fix broken free search
* Fix stuck login dialog after wrong attempt
* Fix recursive child grid view in popup
* Fix readonly input in mass update form when grid has readonlyIf in fields
* Fix grid export IndexOutOfBoundsException with collection columns and translatable order by
* Fix ignored grid orderBy when applying default search filters
* Fix empty PDF tab with latest Chrome
* Fix readonly button on grid
* Fix mass applying same permission to children DMS files
* Fix free search with custom fields
* Fix button showIf/hideIf on editable grid
* Fix unnecessary fetch from field editor
* Fix missing "contains" search with MultiSelect widget
* Fix dirty checking issue with relational fields

  <details>
  
  When some o2m has an onChange action that updates parent record with
  `setValues(instance)` the o2m items were wrongly marked as dirty if
  the o2m doesn't have named column.
  
  </details>

* Fix multi-tenant datasource configuration
* Preserve view-param showArchived when clearing filters
* Fix uploading of meta files in form view
* Fix Binary/BinaryLink in editable grid

#### Security

* Check for permissions when calling actions
* Check for permissions on dotted fields

  <details>
  
  Check for permissions on dotted fields:

  * Filter permitted dotted fields in fetch request
  * Filter permitted dotted fields in search request
  * Filter permitted fields in ActionRequest.setValue
  * Check for create/write permissions on related fields in save request
  * Check for write permissions on editable grid
  * Hide dotted fields if user has no read permissions

  Users are still permitted to read dotted fields’ id, version, and namecolumn
  regardless of permissions.
  
  </details>


## 5.4.2 (2021-04-15)

#### Changes

* Default setting `data.export.encoding` to UTF-8

  <details>
  
  When UTF-8 is used, BOM is added so that Excel recognizes the encoding.
  
  </details>

* Remove jcenter and prevent dependency confusion risk
* Migrate from opencsv to commons-csv
* Format exported dates and times with client locale

  <details>
  
  Setting `data.export.locale` to set a fixed locale for all exports.
  Setting `data.export.separator` defaults to ';'.
  Only numbers (integer, long, decimal) were formatted using client locale.
  Dates, times, and datetimes now also use client locale.
  Setting `date.format` is removed.
  
  </details>


#### Features

* Improve grid customization

  <details>
  
  <ul>
    <li>Show translated field titles besides field names</li>
    <li>Allow to save column widths</li>
    <li>Add removed fields as hidden as to preserve expressions</li>
    <li>Add reset customization button</li>
    <li>Don’t delete customized grids when clearing cache</li>
    <li>List fields from search filters in selector</li>
    <li>Remove move icons in selector</li>
  </ul>
  
  </details>

* Add action to select a `panel` in `panel-tabs`

  <details>
  
  Example:

  ```xml
    <form ...>
     ...
     <panel-tabs>
      <panel title="One" name="t1"></panel>
      <panel title="Two" name="t2"></panel>
     </panel-tabs>
    </form>

    <action-attrs ...>
      <attribute name="active" for="t1" expr="true" />
    </action-attrs>
  ```
  
  </details>

* Apply translations on translatable fields and enums for data exports

  <details>
  
  Only headers and selections were translated.
  Translatable fields and enums are now also translated.
  
  </details>

* Add "auth.ldap.user.dn.format" and "auth.ldap.user.username.attribute" configurations
* Add `view-param` named `default-search-filters` to apply filters by default

  <details>
  
  Example:

  ```xml
    <view-param name="search-filters" value="filter-sales"/>
    <view-param name="default-search-filters" value="confirmed,highValue"/>
  ```

  Filters named `confirmed` and `highValue` will be applied by default.
  For that usage, use new attribute `name` for each `filter` in `search-filters`.
  
  </details>

* Use thin scrollbars on grids inside form views
* Add workflow status help

  <details>
  
  Example:

  ```java
  public void onFetch(
      @Observes @Named(RequestEvent.FETCH) @EntityType(Order.class) PostRequest event) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> values = (Map<String, Object>) event.getResponse().getItem(0);
    if (values != null) {
      List<Map<String, Object>> status = new ArrayList<>();
      status.add(
          ImmutableMap.of(
              "name", "s1", "title", I18n.get("Status 1"), "color", "red", "help", "Some help…"));
      values.put("$wkfStatus", status);
    }
  }
  ```
  
  </details>


#### Fixed

* Fix NPE when loading extension views without corresponding base views
* Fix "cn" used as ID attribute with LDAP
* Fix Unexpected character '`' when running npm-build
* Fix evaluation of filter `if` and `if-module` attributes
* Fix grid column widths when popup is opened as maximized
* Fix attrs reset for bpmn use case
* Fix looped view loading when grid has o2m/m2m fields referencing themselves
* Fix duplicate onChange call with BooleanRadio widget
* Merge menus into mobile toolbar
* Fix fetching LDAP attributes with Active Directory
* Fix row height when using Image widget in editable grid
* Fix missing ID in context after 'save' action in multirelational editor
* Apply view attributes to new records as well
* Fix "Cannot change session ID" exception when using basic auth on non-secure requests
* Fix search query causing dirty form
* Fix Phone widget readonly width adjustment
* Fix wrong query string append with dynamic URL in HTML view

## 5.4.1 (2021-02-10)

#### Changes

* Add `x-show-bars` attribute to `panel-related` and `panel-dashlet` (disabled by default)

#### Features

* Add relative dates and current user/group criteria to advanced search
* Add support for defining Column attributes `insertable` and `updatable`
* Add tel link on grid view when using Phone widget
* Add support for aggregation with Duration widget
* Add flag dropdown to Phone widget

#### Fixed

* Fix requiredIf with some widgets (BinaryLink, CodeEditor…)
* Fix setting null on nullable number field with min/max attributes
* Preserve per module web resource inclusion order
* Fix sidebar="false" panel attribute evaluation
* Fix URL widget in grid view
* Fix grid field hilite overwritten by grid view hilite
* Fix parent toolbar/menubar displayed in dashlet
* Fix search engine action menu selection
* Group M2O/O2O by ID in grid, but show namecolumn
* Set "SameSite=None" attribute on cookies to allow CORS requests
* Set first day of week according to browser locale
* Fix modern theme button alignment in O2M editor

#### Security

* Fix HTML sanitization

## 5.4.0 (2020-12-18)

#### Changes

* `equalsInclude` attribute marks field to be included in equality test, instead of `hashKey`
* Improve keyboard shortcuts

  <details>
  
  The keyboard shortcuts Ctrl+F and Ctrl+G were conflicting with browser shortcuts,
  they are now changed to Alt+F and Alt+G respectively. Also fixed some broken shortcuts.

  Also added a dialog to show keyboard shortcuts which can be shown from the top-right corner menu.
  
  </details>

* Add support for toolbar and menubar on dashlet and o2m/m2m widgets

  <details>
  
  For the toolbar, only first 3 buttons will be visible.
  For the menubar, only first menu will be visible.
  
  </details>


#### Deprecated

* Deprecate `LoginRedirectException` in favor of `WebUtils.issueRedirect`
* Deprecate `hashKey` and `hashAll` in favor of `equalsInclude` and `equalsIncludeAll`

#### Features

* Add support for separators in editors
* Add menu item in form view to show workflow
* Add support for x-show-icons on panel-related
* Add `refresh-tab` response signal
* Add helper api to easily open html tabs
* Add support for defining view attrs externally
* Improve Context api to allow overriding proxy usage
* Add support searching on translatable fields
* Override/add filters: currency, percent, number, date
* Implement grid view customization

  <details>
  
  We can now customize the top-level grid view with a customize column menu and
  selecting/removing/reordering fields.

  The customized view can be shared to all users by admins.
  
  </details>

* Add support for more hilite colors

  <details>
  
  We can now use more named colors with hilite besides the hilite styles.
  
  </details>

* Alert when DMS files and exported files are not found
* Implement `json_set` function

  <details>
  
  The function can be used to update json values using JPQL.

  For example:

  ```sql
  UPDATE
    Product p
  SET
    p.attrs = json_set(p.attrs, 'seller.name', '"Some NAME"')
  WHERE
    json_extract(p.attrs, 'seller', 'id') = 1
  ```
  
  </details>

* Add before transaction complete event

  <details>
  
  The event is for internal purpose only. We may promote it as public api in some future version.
  The event is fired with total number of records updated/deleted in current transaction.
  
  </details>

* Numeric and Decimal format support based on browser language
* Date and DateTime format support based on browser language
* Add support for `help` attribute to panel widgets
* Add support for field tooltip templates
* Support CSS on grid buttons
* Add `mail.smtp.from` application setting
* Add support to show workflow status on top of the form
* Charts decimal/date format support based on browser language
* Add current view type and view names in action context
* Add reset search terms icon to advance search box
* Add support for maximized popups for relational fields

  <details>
  
  We can specify `x-popup-maximized="all|editor|selector" to specify whether to show
  the `editor`, `selector` or both popups in maximized state.
  
  </details>


#### Fixed

* Fix random mail sender test failure
* Fix save action in editable grid
* Fix onChange issue on html widget
* Fix select image button text in HTML widget
* Fix `LoginRedirectException`
* Fix js error when clicking on html view tab
* Fix Response#setValue with $-prefixed dummy field
* Fix PDF visibility in DMS when changing tab content with Chrome
* Fix image-select widget issues
* Fix updating of selected flags for JavaScript expressions
* Fix dotted target-name on TagSelect and many-to-one fields
* Prevent sorting/searching on transient/dummy field in grid
* Reset attributes when navigating and refreshing records from form view
* Fix image height in *-to-many editor
* Prevent searching on relational field without namecolumn in grid
* Fix OK/Cancel button order on editable grid and master-detail
* Fix needless recomputing of views
* Fix visibility evaluation of field using master-detail widget
* Fix empty selection in grid when action initialized selection state
* Fix $fmt with dotted fields and custom views
* Remove unsupported grid button attributes from autocompletion
* Add missing Indian momentjs locales
* Fix o2m/m2m field button bar display
* Fix `target-name` attribute issue on m2o field of grid view
* Fix save action in master-detail
* Apply view extensions by matching name and groups
* Fix NPE when finding computed field dependencies
* Fix translation of dotted target names
* Fix touchpad click to confirm on a popup
* Fix custom field value expression doesn't make form dirty
* Fix Thai momentjs locale
* Fix rendering of buttons and action execution on editable grids
* Fix TypeError when several editable grids have the same dummy fields
* Add missing English momentjs locales
* Fix reference names in json fields

  <details>
  
  When name field of the reference record is changed, the name value saved in json field
  is updated to reflect the updated name.
  
  </details>

* Fix custom fields support in string templates
* Fix editable row reset to old value issue
* Fix context issue with chart click actions

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

## 5.3.6 (2020-10-14)

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
