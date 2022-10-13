## 1.0.0 (2020-01-10)

#### Features

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

#### Fixed

* Fix going into edit mode in editable grid with large horizontal scrolling
* Fix m2m field update issue

  <details>
  
  The m2m items, upon select/edit should not be fully populated as the record
   is already saved (similar to m2o).
  In controllers, make sure to return a compact map in m2m fields, ie a
  list of map with the records ids. Then, the view will fetch the records
  with all necessary fields by itself.
  
  </details>

#### Security

* Fix a bug
