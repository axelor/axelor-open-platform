/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
(function() {

"use strict";

var ui = angular.module('axelor.ui');

ui.prepareContext = function(model, values, dummyValues, parentContext) {

  var context = _.extend({}, values);
  var IGNORE = ['$changed', '$editorModel', '$version', '$fetched', '$fetchedRelated'];

  function findDummy(item) {
    var dummy = {};
    if (item) {
      _.each(item, function (v, k) {
        if (k[0] === '$' && IGNORE.indexOf(k) === -1) {
          dummy[k.substring(1)] = v;
        }
      });
    }
    return dummy;
  }

  function compact(item) {
    if (!item || _.isNumber(item)) return item;
    if (item.id > 0 && item.version === undefined && !item.$dirty) {
      return _.extend({
        id: item.id,
        selected: item.selected,
        $version: item.$version
      }, findDummy(item));
    }
    item = _.extend({}, item);
    if (item.id <= 0) {
      item.id = null;
    }
    if (item.version === undefined && item.$version !== undefined) {
      item.version = item.$version;
    }
    return item;
  }

  var dummy = _.extend({}, dummyValues);
  _.each(dummy, function (value, name) {
    if (value && value.$updatedValues) {
      dummy[name] = value.$updatedValues;
    }
    if (name.indexOf('$') === 0) {
      dummy[name.substring(1)] = dummy[name];
    }
  });

  context = _.extend(context, dummy);
  context._model = model || context._model;
  context._parent = parentContext || context._parent;

  if (context.id <= 0) {
    context.id = null;
  }

  // use selected flag for o2m/m2m fields
  // see onSelectionChanged in o2m controller
  _.each(context.$many, function (getItems, name) {
    if (!getItems) {
      // no items selected
      context[name] = _.map(context[name], function (item) {
        if (item.selected) item = _.extend({}, item, {selected: false});
        return item;
      });
      return;
    }
    if (name.indexOf('$') === 0) name = name.substring(1);
    var items = getItems();
    var value = context[name] || [];
    if (items && items.length === value.length) {
      context[name] = _.map(items, compact);
    }
  });

  // compact o2m/m2m records
  _.each(context, function (value, name) {
    if (_.isArray(value)) {
      context[name] = _.map(value, compact);
    }
    // make sure to have proper selected flags in nested o2m/m2m values, see uiPanelEditor
    if (value && value.$editorModel) {

      context[name] = ui.prepareContext(value.$editorModel, value, findDummy(value));
    }
  });

  return context;
};

ui.controller('FormViewCtrl', FormViewCtrl);

ui.FormViewCtrl = FormViewCtrl;
ui.FormViewCtrl.$inject = ['$scope', '$element'];

function FormViewCtrl($scope, $element) {

  ui.DSViewCtrl('form', $scope, $element);

  var ds = $scope._dataSource;

  $scope.fields = {};
  $scope.fields_view = {};
  $scope.fields_related = {};

  $scope.record = {};
  $scope.$$original = null;
  $scope.$$dirty = false;
  $scope.$$dirtyGrids = [];

  $scope.$events = {};

  /**
   * Get field attributes.
   *
   * @param field field name or field element
   */
  $scope.getViewDef = function(field) {
    var id = field,
      name = field,
      elem = $(field),
      attrs = {};

    if (_.isObject(field)) {  // assume element
      name = elem.attr('x-field') || elem.attr('data-field') || elem.attr('name');
      id = elem.attr('x-for-widget') || elem.attr('id') || name;
    }

    attrs = _.extend({}, this.fields[name], this.fields_view[id]);

    return attrs;
  };

  $scope.doRead = function(id) {
    var params = {
      fields : _.pluck($scope.fields, 'name'),
      related: $scope.fields_related
    };
    var promise = ds.read(id, params);
    promise.success(function (record) {
      record.$fetched = true;
    });
    return promise;
  };

  function doEdit(id, dummy, fireOnLoad) {
    return $scope.doRead(id).success(function(record){
      if ($scope.viewType && $scope.viewType !== 'form') {
        $scope.edit(null);
        return;
      }
      if (dummy) {
        record = _.extend(dummy, record);
      }
      $scope.edit(record, fireOnLoad);
    });
  }

  var initialized = false;
  var routeId = null;
  $scope.onShow = function(viewPromise) {

    var params = this._viewParams;
    var context = params.context || {};
    var recordId = params.recordId || context._showRecord;

    if (params.recordId) {
      params.recordId = undefined;
    }

    if (context._showRecord) {
      context._showRecord = undefined;
    }

    $scope.$locationChangeCheck();
    $scope.$broadcast("on:form-show");

    if (recordId) {
      routeId = recordId;
      return viewPromise.then(function(){
        doEdit(recordId);
      });
    }

    if (!initialized && params.options && params.options.mode === "edit") {
      initialized = true;
      $scope._routeSearch = params.options.search;
      recordId = +params.options.state;
      if (recordId > 0) {
        routeId = recordId;
        return viewPromise.then(function(){
          doEdit(recordId);
        });
      }
    }

    var record = null;

    if (ds._record) {
      record = ds._record;
      delete ds._record;
    } else {
      var page = ds.page();

      if (page.index > -1) {
        record = ds.at(page.index);
      }
    }

    routeId = record && record.id > 0 ? record.id : null;

    viewPromise.then(function(){
      $scope.ajaxStop(function(){
        record = ($scope.record || {}).id ? $scope.record : record;
        if (record === undefined) {
          $scope.edit(null, false);
          return $scope.ajaxStop(function(){
            if (!$scope.record || !$scope.record.id) {
              $scope.$broadcast("on:new");
            }
          });
        }
        if (record && record.id)
          return doEdit(record.id);
        $scope.edit(record);
      });
    });
  };

  var editable = false;

  $scope.isForceEdit = function () {
    var params = this._viewParams || {};
    return params.forceEdit || (params.params && params.params.forceEdit);
  };

  $scope.isForceReadonly = function () {
    var params = this._viewParams || {};
    return params.forceReadonly || (params.params && params.params.forceReadonly);
  };

  $scope.isEditable = function() {
    return $scope.isForceEdit() || (editable && !$scope.isForceReadonly());
  };

  $scope.setEditable = function() {
    editable = arguments.length === 1 ? _.first(arguments) : true;
  };

  $scope.$$resetForm = function $$resetForm() {
    routeId = null;
    $scope.setEditable(false);
    $scope.editRecord(null);
  };

  var locationChangeOff = null;

  function locationChangeCheck() {
    if (locationChangeOff) {
      return;
    }

    var tab = $scope.selectedTab || {};
    var params = $scope._viewParams;

    if (tab !== params) {
      return;
    }

    locationChangeOff = $scope.$on("$locationChangeStart", function (event, newUrl, oldUrl) {
      // block navigation if popup is open
      var hasDialogs = $('body .ui-dialog:visible').length > 0;
      if (hasDialogs) {
        event.preventDefault();
        return;
      }

      var $location = $scope.$location;

      if (!$location || tab !== params || tab.$viewScope != $scope || !$scope.isDirty()) {
        $scope.$timeout(function () {
          if (params && params.viewType !== 'form') {
            $scope.$$resetForm();
          }
        });
        return;
      }

      var path = $location.path();
      var search = $location.search();

      // only handle /ds path changes
      if (path.indexOf("/ds/") !== 0 || oldUrl.indexOf("#/ds/") === -1) {
        return;
      }

      event.preventDefault();
      $scope.confirmDirty(function() {
        $scope.$$resetForm();
        $scope.$locationChangeOff();
        $location.path(path).search(search);
      }, function () {
        $scope.$locationChangeOff();
        locationChangeCheck();
      });
    });
  }

  $scope.$locationChangeOff = function () {
    if (locationChangeOff) {
      locationChangeOff();
      locationChangeOff = null;
    }
  };

  $scope.$locationChangeCheck = function () {
    $scope._viewPromise.then(function () {
      $scope.waitForActions(locationChangeCheck, 200);
    });
  };

  $scope.$on("$destroy", function () {
    $scope.$locationChangeOff();
  });

  $scope.getRouteOptions = function() {
    var rec = $scope.record,
      args = [];

    if (rec && rec.id > 0) {
      args.push(rec.id);
    } else if (routeId > 0) {
      args.push(routeId);
    }

    return {
      mode: 'edit',
      args: args,
      query: $scope._routeSearch
    };
  };

  $scope._routeSearch = {};
  $scope.setRouteOptions = function(options) {
    var opts = options || {},
      record = $scope.record || {},
      state = +opts.state || null;

    $scope.$locationChangeCheck();
    $scope._routeSearch = opts.search;
    if (record.id == state) {
      return $scope.updateRoute();
    }
    if (routeId === state && state) {
      return record.id && record.id !== state ? doEdit(state) : $scope.updateRoute();
    }

    var params = $scope._viewParams;
    if (params.viewType !== "form") {
      return $scope.show();
    }
    return state ? doEdit(state) : $scope.edit(null, false);
  };

  $scope.edit = function(record, fireOnLoad) {
    $scope.$$disableDirtyCheck = true;
    $scope.editRecord(record);
    $scope.updateRoute();
    $scope.$applyAsync(function () {
      $scope.$$disableDirtyCheck = false;
      if (fireOnLoad === false) return;
      $scope._viewPromise.then(function(){
        $scope.ajaxStop(function(){
          var handler = $scope.$events.onLoad,
            record = $scope.record;
          if (handler && !ds.equals({}, record) && record.id) {
            setTimeout(handler);
          }
        });
      });
    });
  };

  $scope.$$fixUndefined = function (orig, current) {
    _.keys(current).forEach(function (name) {
      var value = current[name];
      if (value == undefined && orig[name] === null) {
        current[name] = null;
      }
    });
  };

  $scope.editRecord = function(record) {
    $scope.$$original = angular.copy(record) || {};
    $scope.$$dirty = false;
    $scope.record = angular.copy($scope.$$original);
    $scope._viewPromise.then(function(){
      $scope.ajaxStop(function(){
        $scope.$$fixUndefined($scope.$$original, $scope.record);
        $scope.$broadcast("on:edit", $scope.record);
        $scope.$broadcast("on:record-change", $scope.record);
        if ($scope.__canForceEdit && $scope.canEdit()) {
          $scope.__canForceEdit = undefined;
          $scope.onEdit();
        }
      });
    });
  };

  $scope.getContextRecord = function() {
    return _.extend({}, $scope._routeSearch, $scope.record);
  };

  $scope.getContext = function() {
    var dummy = $scope.getDummyValues();
    var context = $scope.getContextRecord();
    if ($scope.$parent && $scope.$parent.getContext) {
      context._parent = $scope.$parent.getContext();
    } else {
      context = _.extend({}, $scope._viewParams.context, context);
    }
    if (!$scope.$hasPanels) {
      context._form = true;
    }
    $scope.$broadcast('on:update-context', context);
    return ui.prepareContext(ds._model, context, dummy);
  };

  $scope.$dirtyGrid = function (gridId, dirty) {
    var i = $scope.$$dirtyGrids.indexOf(gridId);
    if (dirty && i === -1) {
      $scope.$$dirtyGrids.push(gridId);
    } else if (!dirty && i > -1) {
      $scope.$$dirtyGrids.splice(i, 1);
    }
    return $scope.isDirty();
  };

  $scope.isDirty = function() {
    $scope.$$dirty = $scope.$$disableDirtyCheck ? false : !ds.equals($scope.record, $scope.$$original);
    return $scope.$$dirty;
  };

  $scope.$watch("record", function formRecordWatch(rec, old) {
    if (rec === old) {
      $scope.$$dirty = false;
      return;
    }
    $scope.$broadcast("on:record-change", rec);
    $scope.$$dirty = $scope.isDirty();
  }, true);

  $scope.$broadcastRecordChange = function () {
    $scope.$broadcast("on:record-change", $scope.record);
  };

  $scope.$on("on:record-change", function () {
    var view = $scope.schema;
    if (view && view.readonlyIf) {
      var readonly = axelor.$eval($scope, view.readonlyIf, _.extend({}, $scope._context, $scope.record));
      if (_.isFunction($scope.attr)) {
        $scope.attr('readonly', readonly);
      }
      editable = !readonly;
    }
  });

  $scope.isValid = function() {
    return $scope.form && $scope.form.$valid;
  };

  $scope.canNew = function() {
    return $scope.hasButton('new');
  };

  $scope.canEdit = function() {
    return $scope.hasButton('edit');
  };

  $scope.canSave = function() {
    return $scope.hasPermission('write') && $scope.$$dirty && $scope.isValid();
  };

  $scope.canDelete = function() {
    return $scope.hasButton('delete') && ($scope.record || {}).id > 0;
  };

  $scope.canArchive = function() {
    return $scope.hasPermission('write')
      && $scope.hasButton('archive')
      && ($scope.record || {}).id > 0
      && !$scope.$$dirty
      && !$scope.record.archived
      && $scope.isValid();
  };

  $scope.canUnarchive = function() {
    return $scope.hasPermission('write')
      && $scope.hasButton('archive')
      && ($scope.record || {}).id > 0
      && !$scope.$$dirty
      && $scope.record.archived
      && $scope.isValid();
  };

  $scope.canCopy = function() {
    return $scope.canNew() && $scope.hasButton('copy') && !$scope.$$dirty && ($scope.record || {}).id;
  };

  $scope.canAttach = function() {
    return $scope.hasButton('attach');
  };

  $scope.canCancel = function() {
    return $scope.$$dirty;
  };

  $scope.canBack = function() {
    return !$scope.$$dirty;
  };

  $scope.onNew = function() {
    var defer = $scope._defer();
    $scope.confirmDirty(function(){
      routeId = null;
      $scope.$locationChangeOff();
      $scope.edit(null, false);
      $scope.setEditable();
      $scope.$broadcast("on:new");
      $scope.$locationChangeCheck();
      defer.resolve();
    }, defer.reject);
    return defer.promise;
  };

  $scope.onNewPromise = null;
  $scope.defaultValues = null;

  $scope.onNewHandler = function onNewHandler(event) {

    routeId = null;

    function handleOnNew() {

      var handler = $scope.$events.onNew;
      var last = $scope.$parent.onNewPromise || $scope.onNewPromise;
      var params = ($scope._viewParams || {}).params || {};

      function reset() {
        $scope.onNewPromise = null;
      }

      function handle(defaults) {
        var promise = handler();
        if (promise && promise.then) {
          promise.then(reset, reset);
          promise = promise.then(function () {
            if ($scope.record && $scope.record.id > 0) return; // record may have been saved, see RM-13558
            if ($scope.isDirty()) {
              var rec = _.extend({}, defaults, $scope.record);
              var old = $scope.$$original;
              var res = $scope.editRecord(rec);
              if (rec) {
                rec._dirty = true;
              }
              if (!params['details-view']) {
                $scope.$$original = old;
              }
              return res;
            } else if (defaults) {
              $scope.editRecord(defaults);
            }
          });
        }
        return promise;
      }

      $scope.setEditable();

      if (handler && $scope.record) {
        if (last) {
          $scope.onNewPromise = last.then(handle);
          return;
        }
        $scope.onNewPromise = handle($scope.defaultValues);
      } else if ($scope.defaultValues) {
        $scope.editRecord($scope.defaultValues);
      }
    }

    function afterVewLoaded() {
      if ($scope.defaultValues === null) {
        var defaultValues = {};
        _.each($scope.fields, function (field, name) {
          if (field.defaultValue !== undefined) {
            defaultValues[name] = field.defaultValue;
          }
        });
        $scope.defaultValues = _.isEmpty(defaultValues) ? undefined : defaultValues;
      }

      // ensure correct date/datetime
      _.each($scope.fields, function (field, name) {
        if (field.defaultNow && $scope.defaultValues[name] !== undefined) {
          $scope.defaultValues[name] = field.type === 'date'
              ? moment().startOf('day').format('YYYY-MM-DD')
              : moment().toISOString();
        }
      });
      return handleOnNew();
    }

    $scope._viewPromise.then(function() {
      $scope.waitForActions(afterVewLoaded);
    });
  };

  $scope.$on("on:new", function (event) {
    $scope.onNewHandler(event);
  });

  $scope.$on("on:nav-click", function(event, tab) {
    var record, context, checkVersion;
    if (event.defaultPrevented || tab.$viewScope !== $scope) {
      return;
    }
    event.preventDefault();
    context = tab.context || {};
    record = $scope.record || {};
    checkVersion = "" + axelor.config["view.form.check-version"];
    if (context.__check_version !== undefined) {
      checkVersion = "" + context.__check_version;
    }

    if (!record.id || !(checkVersion === "true" || checkVersion === "silent")) {
      return;
    }

    return $scope.checkVersion(true, function (verified) {
      var params = $scope._viewParams;
      if (verified) {
        return;
      }
      if (checkVersion === "silent"
        && (!$scope.isDirty() || (params.params && params.params['show-confirm'] === false))) {
        return $scope.onRefresh();
      }

      axelor.dialogs.confirm(
          _t("The record has been updated or delete by another action.") + "<br>" +
          _t("Would you like to reload the current record?"),
      function(confirmed){
        if (confirmed) {
          $scope.onRefresh();
        }
      });
    });
  });

  $scope.checkVersion = function (callback) {
    var record = $scope.record || {};
    var done = callback;
    var graph = callback === true;
    if (graph) {
      done = arguments[1];
    }
    done = _.isFunction(done) ? done : angular.noop;

    function compact(rec) {
      var res = {
        id: rec.id,
        version: rec.version
      };
      if (res.version === undefined) {
        res.version = rec.$version;
      }
      if (graph) {
        _.each(rec, function(v, k) {
          if (!v) return;
          if (v.id) res[k] = compact(v);
          if (_.isArray(v)) res[k] = _.map(v, compact);
        });
      }
      return res;
    }

    if (!record.id) {
      return done(true);
    }

    return ds.verify(compact(record))
    .success(function(res) {
      done(res.status === 0);
    }).error(function (err) {
      done(false);
    });
  };

  $scope.onEdit = function() {
    $scope.setEditable();
  };

  $scope.onCopy = function() {
    var record = $scope.record;
    ds.copy(record.id).success(function(record){
      $scope.$locationChangeOff();
      routeId = null;
      $scope.edit(record);
      $scope.setEditable();
      $scope.$timeout(function () {
        record._dirty = true;
        $scope.$$original = {};
        locationChangeCheck();
      });
    });
  };

  $scope.getDummyValues = function() {
    if (!$scope.record) return {};
    var fields = _.keys($scope.fields);
    var extra = _.chain($scope.fields_view)
            .filter(function(f) { return f.name && !_.contains(fields, f.name); })
            .pluck('name')
            .compact()
            .value();

    if ($scope._model === 'com.axelor.auth.db.User') {
      extra = extra.filter(function (n) {
        return ['change', 'oldPassword', 'newPassword', 'chkPassword'].indexOf(n) === -1;
      });
    }

    return _.pick($scope.record, extra);
  };

  $scope._gridEditCount = 0;
  $scope._afterGridEditTasks = [];

  $scope.$on('on:grid-edit-start', function () {
    ++$scope._gridEditCount;
  });

  $scope.$on('on:grid-edit-end', function () {
    if (!--$scope._gridEditCount) {
      try {
        _.each($scope._afterGridEditTasks, function (task) {
          task();
        });
      } finally {
        $scope._afterGridEditTasks = [];
      }
    }
  });

  $scope.afterGridEdit = function (task) {
    if ($scope._gridEditCount) {
      $scope._afterGridEditTasks.push(task);
    } else {
      task();
    }
  };

  $scope.onSave = function(options) {

    var opts = _.extend({ fireOnLoad: true }, options);
    var defer = $scope._defer();
    var saveAction = $scope.$events.onSave;
    var fireOnLoad = opts.fireOnLoad;

    function fireBeforeSave() {
      var event = $scope.$broadcast('on:before-save', $scope.record);
      if (event.defaultPrevented) {
        if (event.error) {
          axelor.dialogs.error(event.error);
        }
        setTimeout(function() {
          defer.reject(event.error);
        });
        return false;
      }
      return true;
    }

    if (opts.callOnSave === false) {
      saveAction = null;
      fireOnLoad = false;
    }

    if (fireBeforeSave() === false) {
      return defer.promise;
    }

    function doSave() {
      var dummy = $scope.getDummyValues(),
        values = _.extend({}, $scope.record, opts.values),
        promise;

      values = ds.diff(values, $scope.$$original);
      promise = ds.save(values).success(function(record, page) {
        $scope.$$dirtyGrids.length = 0;
        return doEdit(record.id, dummy, fireOnLoad);
      });

      promise.success(function(record) {
        // update dotted fields with new values from form
        _.chain(Object.keys(record).concat(Object.keys($scope.fields)))
          .filter(function(name) { return name.indexOf('.') >= 0; })
          .uniq()
          .each(function(name) {
            var value = ui.findNested(values, name);
            if (value !== undefined) {
              record[name] = value;
            }
          });
        defer.resolve(record);
      });
      promise.error(function(error) {
        defer.reject(error);
      });
    }

    function waitForActions(callback) {
      if (opts.wait === false) {
        return callback();
      }
      return $scope.waitForActions(callback, 100);
    }

    function doOnSave() {
      if (!$scope.hasPermission('write') || !$scope.isValid()) {
        $scope.showErrorNotice();
        defer.reject();
        return defer.promise;
      }
      if (saveAction) {
        return saveAction().then(doSave);
      }
      // repeat on:before-save to ensure if any o2m/m2m is updated gets applied
      if (fireBeforeSave()) {
        waitForActions(doSave);
      }
    }

    $scope.afterGridEdit(function () { waitForActions(doOnSave); });

    return defer.promise;
  };

  $scope.confirmDirty = function(callback, cancelCallback) {
    var params = $scope._viewParams || {};
    if (!$scope.isDirty() || (params.params && params.params['show-confirm'] === false)) {
      return callback();
    }
    axelor.dialogs.confirm(_t("Current changes will be lost. Do you really want to proceed?"), function(confirmed){
      if (!confirmed) {
        if (cancelCallback) {
          cancelCallback();
        }
        return;
      }
      $scope.$applyAsync(callback);
    });
  };

  $scope.onDelete = function() {
    var record = $scope.record || {};
    if (!record.id  || record.id < 0) {
      return;
    }
    axelor.dialogs.confirm(_t("Do you really want to delete the selected record?"),
    function(confirmed){
      if (!confirmed) {
        return;
      }
      ds.removeAll([record]).success(function(records, page){
        if ($scope.switchBack() === false) {
          $scope.onNew();
        }
      });
    });
  };

  $scope.onArchive = function() {
    var record = $scope.record || {};
    if (!record.id  || record.id < 0) {
      return;
    }
    axelor.dialogs.confirm(_t("Do you really want to archive the selected record?"),
    function(confirmed) {
      if (!confirmed) {
        return;
      }
      var item = _.extend({}, record, { archived: true });
      ds.saveAll([item]).success(function() {
        $scope.switchBack();
      });
    });
  };

  $scope.onUnarchive = function() {
    var record = $scope.record || {};
    if (!record.id  || record.id < 0) {
      return;
    }
    axelor.dialogs.confirm(_t("Do you really want to unarchive the selected record?"),
    function(confirmed) {
      if (!confirmed) {
        return;
      }
      var item = _.extend({}, record, { archived: false });
      ds.saveAll([item]).success(function() {
        $scope.reload();
      });
    });
  };

  $scope.onBack = function() {
    var record = $scope.record || {};
    var editable = $scope.isEditable();
    $scope.$broadcast("cancel:grid-edit");
    if (record.id && editable && $scope.canEdit()) {
      $scope.setEditable(false);
      return;
    }

    $scope.switchBack();
  };

  $scope.onRefresh = function() {
    $scope.confirmDirty($scope.reload);
  };

  $scope.reload = function() {
    $scope.$broadcast("on:attrs-reset");
    var record = $scope.record;
    if (record && record.id) {
      return doEdit(record.id).success(function (rec) {
        var shared = ds.get(record.id);
        if (shared) {
          shared = _.extend(shared, rec);
        }
      });
    }
    $scope.edit(null, false);
    $scope.$broadcast("on:new");
  };

  $scope.onCancel = function() {
    var e = $scope.$broadcast("cancel:grid-edit");
    if (e.defaultPrevented) {
      return;
    }
    $scope.confirmDirty(function() {
      $scope.reload();
    });
  };

  var __switchTo = $scope.switchTo;

  $scope.switchBack = function () {
    var __switchBack = null;
        if($scope._viewTypeLast && $scope._viewTypeLast !== "form") {
            __switchBack = $scope._viewTypeLast;
        }
        if (__switchBack === null) {
            var views = ($scope._viewParams||{}).views || [];
            for (var i = 0 ; i < views.length; i++) {
                var view = views[i];
                if (view.type !== "form") {
                    __switchBack = view.type;
                    break;
                }
            }
        }
    if (__switchBack) {
      return $scope.switchTo(__switchBack);
    }
    return false;
  };

  $scope.switchTo = function(type, callback) {
    $scope.waitForActions(function () {
      $scope.confirmDirty(function() {
        $scope.setEditable(false);
        $scope.editRecord(null);
          __switchTo(type, function () {
            $scope.$locationChangeOff();
            if (callback) {
              callback();
            }
          });
      });
    });
  };

  $scope.onSearch = function() {
    var e = $scope.$broadcast("cancel:grid-edit");
    if (e.defaultPrevented) {
      return;
    }

    $scope.switchBack();
  };

  $scope.pagerText = function() {
    var page = ds.page(),
      record = $scope.record || {};

    if (page && page.from !== undefined) {
      if (page.total === 0 || page.index === -1 || !record.id) return null;
      return _t("{0} of {1}", (page.from + page.index + 1), page.total);
    }
  };

  $scope.canNext = function() {
    var page = ds.page();
    return (page.index < page.size - 1) || (page.from + page.index < page.total - 1);
  };

  $scope.canPrev = function() {
    var page = ds.page();
    return page.index > 0 || ds.canPrev();
  };

  $scope.onNext = function() {
    $scope.confirmDirty(function() {
      ds.nextItem(function(record){
        if (record && record.id) {
          $scope.$broadcast("on:attrs-reset");
          doEdit(record.id);
        }
      });
    });
  };

  $scope.onPrev = function() {
    $scope.confirmDirty(function() {
      ds.prevItem(function(record){
        if (record && record.id) {
          $scope.$broadcast("on:attrs-reset");
          doEdit(record.id);
        }
      });
    });
  };

  function focusFirst() {
    $scope._viewPromise.then(function() {
      setTimeout(function() {
        $element.find('form :input:visible').not('[readonly],[type=checkbox]').first().focus().select();
      });
    });
  }

  function showLog() {
    ds.read($scope.record.id, {
      fields: ['createdBy', 'createdOn', 'updatedBy', 'updatedOn']
    }).success(function (record) {
      showLogDialog(record);
    });
  }

  function showLogDialog(record) {

    function nameOf(user) {
      if (!user) {
        return "";
      }
      var name = axelor.config['user.nameField'] || 'name';
      return user[name] || "";
    }

    var info = {};
    if (record.createdOn) {
      info.createdOn = moment(record.createdOn).format(ui.dateTimeFormat);
      info.createdBy = nameOf(record.createdBy);
    }
    if (record.updatedOn) {
      info.updatedOn = moment(record.updatedOn).format(ui.dateTimeFormat);
      info.updatedBy = nameOf(record.updatedBy);
    }
    var table = $("<table class='table field-details'>");
    var tr;

    tr = $("<tr></tr>").appendTo(table);
    $("<th></th>").text(_t("Created By:")).appendTo(tr);
    $("<td></td>").text(info.createdBy).appendTo(tr);

    tr = $("<tr></tr>").appendTo(table);
    $("<th></th>").text(_t("Created On:")).appendTo(tr);
    $("<td></td>").text(info.createdOn).appendTo(tr);

    tr = $("<tr></tr>").appendTo(table);
    $("<th></th>").text(_t("Updated By:")).appendTo(tr);
    $("<td></td>").text(info.updatedBy).appendTo(tr);

    tr = $("<tr></tr>").appendTo(table);
    $("<th></th>").text(_t("Updated On:")).appendTo(tr);
    $("<td></td>").text(info.updatedOn).appendTo(tr);

    var text = $('<div>').append(table).html();

    axelor.dialogs.say(text);
  }

  $scope.toolmenu = [{
    isButton: true,
    items: [{
      title: _t('Refresh'),
      click: function(e) {
        $scope.onRefresh();
      }
    }, {
      title: _t('Delete'),
      active: function () {
        return $scope.canDelete();
      },
      click: function(e) {
        $scope.onDelete();
      }
    }, {
      title: _t('Duplicate'),
      active: function () {
        return $scope.canCopy();
      },
      click: function(e) {
        $scope.onCopy();
      }
    }, {
      visible: function () {
        return $scope.canArchive() || $scope.canUnarchive();
      },
    }, {
      title: _t('Archive'),
      active: function () {
        return $scope.canArchive();
      },
      visible: function () {
        return $scope.canArchive();
      },
      click: function(e) {
        $scope.onArchive();
      },
    }, {
      title: _t('Unarchive'),
      active: function () {
        return $scope.canUnarchive();
      },
      visible: function () {
        return $scope.canUnarchive();
      },
      click: function(e) {
        $scope.onUnarchive();
      },
    }, {
      visible: function () {
        return ($scope.record || {}).$processInstanceId;
      }
    }, {
      title: _t('Display process'),
      visible: function () {
        return ($scope.record || {}).$processInstanceId;
      },
      action: "wkf-instance-view-from-record"
    }, {
    }, {
      active: function () {
        return $scope.hasAuditLog();
      },
      title: _t('Last modified...'),
      click: showLog
    }]
  }];

  $scope.onHotKey = function (e, action) {

    if (action === "save") {
      if (!$scope.canSave()) {
        $scope.showErrorNotice();
      } else if ($scope.hasButton('save')) {
        $(e.target).blur().focus();
        $scope.onSave();
      }
    }
    if (action === "refresh") {
      $scope.onRefresh();
    }
    if (action === "new") {
      $scope.onNew();
    }
    if (action === "edit") {
      if ($scope.canEdit()) {
        $scope.onEdit();
      }
      focusFirst();
    }
    if (action === "delete" && $scope.canDelete()) {
      $scope.onDelete();
    }
    if (action === "select") {
      focusFirst();
    }
    if (action === "prev" && $scope.canPrev()) {
      $scope.onPrev();
    }
    if (action === "next" && $scope.canNext()) {
      $scope.onNext();
    }
    if (action === "search") {
      $scope.onBack();
      $scope.waitForActions(function () {
        var filterBox = $('.filter-box :input:visible');
        if (filterBox.length) {
          filterBox.focus().select();
          return false;
        }
      }, 300);
    }

    $scope.$applyAsync();

    return false;
  };

  $scope.$text = function (name) {
    var field = $scope.fields[name] || {},
      format = ui.formatters[field.type],
      record = $scope.record || {};
    if (format) {
      return format(field, record[name]);
    }
    return record[name];
  };

  $scope.$evalViewerExpr = axelor.$evalViewerExpr;
}

ui.formBuild = function (scope, schema, fields) {

  var path = scope.formPath || "";
  var hasPanels = false;
  var hasToolTipField = false;

  function update(e, attrs) {
    _.each(attrs, function(v, k) {
      if (_.isUndefined(v)) return;
      e.attr(k, v);
    });
  }

  function process(items, parent) {

    $(items).each(function(){

      if (this.type == 'break') {
        return $('<br>').appendTo(parent);
      }
      if (this.type == 'field') {
        delete this.type;
      }
      if (['panel', 'panel-json', 'panel-related'].indexOf(this.type) > -1) {
        scope.$hasPanels = hasPanels = true;
      }

      var widget = this.widget,
        widgetAttrs = {},
        attrs = {};

      _.extend(attrs, this.widgetAttrs);

      _.each(this.widgetAttrs, function(value, key) {
        widgetAttrs['x-' + key] = value;
      });

      var item = $('<div></div>').appendTo(parent),
        field = fields[this.name] || {},
        widgetId = _.uniqueId('_formWidget'),
        type = widget;

      attrs = angular.extend(attrs, field, this);
      widget = widget || attrs.widget;
      type = ui.getWidget(widget) ||
           ui.getWidget(attrs.type) ||
           ui.getWidget(attrs.serverType) ||
           attrs.type || attrs.serverType || 'string';

      if (_.isArray(attrs.selectionList) && !widget) {
        type = attrs.multiple ? 'multi-select' : 'select';
      }

      if (attrs.password) {
        type = 'password';
      }
      if (attrs.image) {
        type = "image";
      }
      if (type == 'label') {
        type = 'static-label';
      }

      if (attrs.type == 'panel-related') {
        type = 'panel-' + (field.type || attrs.serverType || type);
        if (attrs.items && attrs.items.length) {
          attrs.views = [{
            type: 'grid',
            title: attrs.title || field.title || field.autoTitle,
            items: attrs.items,
            fields: attrs.fields,
            canMove: attrs.canMove,
            orderBy: attrs.orderBy,
            editable: attrs.editable,
            editIcon: attrs.editIcon === undefined ? true : attrs.editIcon
          }];
        }
        this.items = attrs.items = null;
      }

      if ((attrs.editor || attrs.viewer) && attrs.target && type !== 'image') {
        type = 'inline-' + type;
      }

      attrs.serverType = field.serverType || attrs.serverType || attrs.type;
      attrs.type = type;

      item.attr('ui-' + type, '');
      item.attr('id', widgetId);

      if (parent.is('[ui-panel-tabs]')) {
        item.attr('ui-panel-tab', '');
        if (attrs.showTitle === undefined) {
          attrs.showTitle = false;
        }
        if (attrs.showFrame === undefined) {
          attrs.showFrame = false;
        }
      }

      scope.fields_view[widgetId] = attrs;

      //TODO: cover all attributes
      var _attrs = _.extend({}, attrs.attrs, this.attrs, widgetAttrs, {
          'name'			: attrs.name || this.name,
          'x-cols'		: this.cols,
          'x-colspan'		: this.colSpan || (type === 'help' ? 12 : undefined),
          'x-coloffset'	: this.colOffset,
          'x-rowspan'		: this.rowSpan,
          'x-sidebar'		: this.sidebar,
          'x-stacked'		: this.stacked,
          'x-flexbox'		: this.flexbox,
          'x-widths'		: this.colWidths,
          'x-field'		: this.name,
          'x-title'		: attrs.title
        });

      if (attrs.showTitle !== undefined) {
        attrs.showTitle = attrs.showTitle !== false;
        _attrs['x-show-title'] = attrs.showTitle;
      }

      if (attrs.required)
        _attrs['ng-required'] = true;
      if (attrs.readonly)
        _attrs['x-readonly'] = true;

      if (_attrs.name) {
        _attrs['x-path'] = path ? path + "." + _attrs.name : _attrs.name;
      }

      if (attrs.tooltip) {
        hasToolTipField = true;
        item.addClass('has-tooltip');
      }

      update(item, _attrs);

      // enable actions & conditional expressions
      item.attr('ui-actions', '');
      item.attr('ui-widget-states', '');

      if (type == 'button' || type == 'static-label') {
        item.html(this.title);
      }

      if (/button|group|tabs|tab|separator|spacer|static|static-label/.test(type)) {
        item.attr('x-show-title', false);
      }

      if (attrs.translatable && (attrs.serverType === 'string' || attrs.serverType === 'text')) {
        item.attr('ui-translate-icon', '');
      }

      var items = this.items || this.pages;
      if (items && this.type != 'panel-related') {
        process(items, item);
        if (type === 'panel') {
          item.attr('ui-panel-layout', '');
          item.attr('x-item-span', attrs.itemSpan);
        } else if (['tabs', 'panel-tabs', 'panel-stack', 'panel-related', 'panel-mail', 'button-group'].indexOf(type) == -1) {
          item.attr('ui-table-layout', '');
        }
      }
      if (type === 'group' && _.all(items, function (x){ return x.type === 'button'; })) {
        item.addClass('button-group');
      }
    });
    return parent;
  }

  var elem = $('<form name="form" ui-form-gate ui-form ui-table-layout ui-actions ui-widget-states></form>');
  elem.attr('x-cols', schema.cols)
    .attr('x-widths', schema.colWidths);

  if (schema.css) {
    elem.addClass(schema.css);
  }

  process(schema.items, elem);

  if (hasPanels) {
    elem.removeAttr('ui-table-layout').attr('ui-bar-layout', '');
  }

  if (hasToolTipField) {
    $("<div ui-tooltip selector='.form-item-container.has-tooltip' getter='getToolTip($event)'>").appendTo(elem);
  }

  return elem;
};

ui.directive('uiViewForm', ['$compile', 'ViewService', function($compile, ViewService){

  return function(scope, element, attrs) {

    scope.canShowAttachments = function() {
      return scope.canAttach() && (scope.record || {}).id;
    };

    scope.onShowAttachments = function(){
      var popup = ViewService.compile('<div ui-dms-popup></div>')(scope.$new(true));
      popup.isolateScope().showPopup(scope);
    };

    scope.hasAuditLog = function() {
      return scope.record && scope.record.id > -1;
    };

    scope.hasWidth = function() {
      var view = scope.schema;
      return view && view.width;
    };

    var translatted = null;

    scope.getToolTip = function (e) {
      var elem = $(e.currentTarget);
      var fieldScope = elem.scope();
      var field = fieldScope.field || {};
      if (!field.tooltip) {
        return;
      }

      var record = scope.record;
      var dataSource = scope._dataSource;

      if (field.serverType === 'many-to-one' || field.serverType === 'one-to-one') {
        record = fieldScope.getValue();
        dataSource = fieldScope._dataSource;
      }

      return {
        tooltip: field.tooltip,
        record: record,
        dataSource: dataSource
      };
    };

    scope.showErrorNotice = function () {
      var form = scope.form || $(element).data('$formController'),
        names;

      if (!form || form.$valid) {
        return;
      }

      var elems = element.find('[x-field].ng-invalid:not(fieldset)').filter(function() {
        var isInline = $(this).parents('.slickgrid,.nested-not-required').length > 0;
        if (isInline) {
          return false;
        }
        var elemScope = $(this).scope();
        if (elemScope.isHidden &&
          elemScope.isHidden()) {
          return false;
        }
        return true;
      });
      var items = elems.map(function () {
        return {
          name: $(this).attr('x-field'),
          title: $(this).attr('x-title')
        };
      });

      items = _.unique(_.compact(items), function (item) { return item.name; });

      if (items.length === 0) {
        return;
      }

      if (!translatted) {
        translatted = {};
        _.each(scope.fields_view, function (v, k) {
          if (v.name) {
            translatted[v.name] = v.title;
          }
        });
      }

      items = _.map(items, function(item) {
        var value = item.title;
        if (item.name) {
          value = translatted[item.name] || value;
        }
        return '<li>' + value + '</li>';
      });

      items = '<ul>' + items.join('') + '</ul>';

      axelor.notify.error(items, {
        title: _t("The following fields are invalid:")
      });
    };

    element.scroll(function (e) {
      $(document).trigger('adjust:scroll', element);
    });

    scope.$on("on:form-show", function () {
      setTimeout(function () {
        element.animate({ scrollTop: 0 }, 200);
      }, 300);
    });

    var unwatch = scope.$watch('schema.loaded', function formSchemaWatch(viewLoaded){

      if (!viewLoaded) return;

      unwatch();

      var preparing = true;

      scope.$watchChecker(function () {
        return !preparing;
      });

      var params = (scope._viewParams || {}).params || {};
      var schema = scope.schema;
      var form = ui.formBuild(scope, schema, scope.fields);

      form = $compile(form)(scope);

      var numFields = form.find('[x-field]').length;

      if (!scope._isPopup && !scope._isPanelForm) {
        element.addClass('has-width');
      }

      var width = schema.width || params.width;
      if (width && !(/^(large|mid|mini)$/.test(width))) {
        if (width === '100%' || width === '*') {
          element.removeClass('has-width');
        }
        form.css({
          width: width,
          minWidth: schema.minWidth || params.minWidth,
          maxWidth: schema.maxWidth || params.maxWidth
        });
        form.removeClass('large-form mid-form mini-form');
      }

      scope.$timeout(function () {
        element.append(form);
        preparing = false;
        if (scope._viewResolver) {
          scope._viewResolver.resolve(schema, element);
          scope.$broadcast("adjust:dialog");
        }
      }, Math.min(300, numFields * 2));
    });

    element.on('dblclick', '[x-field].readonly', function (e) {
      if (!scope.isEditable()) {
        element.prev('.record-toolbar')
          .find('button[ng-click="onEdit()"] i')
          .effect('pulsate', { times: 3 }, 600);
      }
    });

    scope.$on('refresh-tab', function () {
      scope.waitForActions(function () {
        scope.reloadTab(scope.selectedTab);
      });
    });
  };
}]);

ui.formWidget('uiWkfStatus', {
  link: function (scope, element, attrs) {

    scope.hasColorCode = function (item) {
      return item.color && item.color.startsWith('#');
    };

    scope.getColorClass = function (item) {
      return item.color ? 'bg-' + item.color : 'bg-blue';
    };
  },
  template:
    "<div class='panel wkf-status-container'>" +
      "<div class='panel-body'>" +
        "<ul class='wkf-status'>" +
          "<li ng-repeat='item in record.$wkfStatus track by item.name'>" +
            "<span class='badge' style='background-color: {{item.color}}' ng-if='hasColorCode(item)'>{{item.title}}</span>" +
            "<span class='badge' ng-class='getColorClass(item)' ng-if='!hasColorCode(item)'>{{item.title}}</span>" +
          "</li>" +
        "</ul>" +
      "</div>" +
    "</div>"
});

})();
