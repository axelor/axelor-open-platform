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

/* global Slick: true */

"use strict";

var ui = angular.module('axelor.ui');

ui.controller('GridViewCtrl', GridViewCtrl);

ui.GridViewCtrl = GridViewCtrl;
ui.GridViewCtrl.$inject = ['$scope', '$element'];

function GridViewCtrl($scope, $element) {

  ui.DSViewCtrl('grid', $scope, $element);

  var ds = $scope._dataSource;
  var page = {};

  $scope.dataView = new Slick.Data.DataView();
  $scope.selection = [];

  ds.on('change', function(e, records, page){
    $scope.setItems(records, page);
  });

  var initialized = false;
  var reloadDotted = false;

  $scope.onShow = function(viewPromise) {

    if (!initialized) {

      viewPromise.then(function(){
        var view = $scope.schema,
          params = $scope._viewParams,
          sortBy = view.orderBy,
          pageNum = null;

        if (sortBy) {
          sortBy = sortBy.split(',');
        }
        if (params.options && params.options.mode === 'list') {
          pageNum = params.options.state;
          $scope._routeSearch = params.options.search;
        }

        reloadDotted = params.params && params.params['reload-dotted'];

        $scope.view = view;

        if (view.noFetch) return;

        var opts = ds._filter ? ds._filter : {
          _sortBy: sortBy,
          _pageNum: pageNum
        };

        $scope.filter(opts).then(function(){
          $scope.$broadcast('on:grid-selection-change', $scope.getContext(), true);
          $scope.updateRoute();
        });
      });

      initialized = true;
    } else {
      if (reloadDotted || ($scope._viewTypeLast && $scope._viewTypeLast !== 'grid')) {
        return $scope.reload().then(function() {
          $scope.updateRoute();
        });
      }
      var current = $scope.dataView.getItem(_.first($scope.selection));
      if (current && current.id) {
        $scope.dataView.updateItem(current.id, current);
      }
    }
  };

  $scope.getRouteOptions = function() {
    var pos = 1,
      args = [],
      query = {},
      params = $scope._viewParams;

    if (page && page.limit) {
      pos = (page.from / page.limit) + 1;
    } else if (params.options && params.options.mode === 'list') {
      pos = +(params.options.state);
    }

    pos = pos || 1;
    args = [pos];

    return {
      mode: 'list',
      args: args,
      query: query
    };
  };

  $scope._routeSearch = {};
  $scope.setRouteOptions = function(options) {
    var opts = options || {},
      pos = +(opts.state),
      current = (page.from / page.limit) + 1;

    pos = pos || 1;
    current = current || 1;

    $scope._routeSearch = opts.search;
    if (pos === current) {
      return $scope.updateRoute();
    }

    var params = $scope._viewParams;
    if (params.viewType !== "grid") {
      return $scope.show();
    }

    $scope.filter({
      _pageNum: pos
    });
  };

  $scope.getItem = function(index) {
    return $scope.dataView.getItem(index);
  };

  $scope.getItems = function() {
    return $scope.dataView.getItems();
  };

  $scope.setItems = function(items, pageInfo) {

    var dataView = $scope.dataView;
    var selection = $scope.selection || [];
    var selectionIds = dataView.mapRowsToIds(selection);
    var hasSelected = _.some(items, function (item) { return item.selected; });
    var allFetched = _.all(items, function (item) { return item.$fetched; });
    var syncSelection = function () {
      if (dataView.$syncSelection) {
        setTimeout(function(){
          if (hasSelected || allFetched) {
            dataView.$syncSelection();
          } else {
            dataView.$syncSelection(selection, selectionIds);
          }
        });
      }
    };

    //XXX: clear existing items (bug?)
    if (dataView.getLength()) {
      dataView.beginUpdate();
      dataView.setItems([]);
        dataView.endUpdate();
    }

    dataView.beginUpdate();
      dataView.setItems(items);
      dataView.endUpdate();

    if (pageInfo) {
        page = pageInfo;
    }

    var details = $scope.$details;
    if (details) {
      var onSyncDetails = function() {
        var record = details.record || {};
        var found = _.findWhere(items, { id: record.id });
        if (found) {
          found.selected = true;
        } else {
          details.edit(null);
        }
        syncSelection();
      }
      if (_.isEmpty(details.record)) {
        details.$timeout(onSyncDetails);
      } else {
        var removeOnSyncDetails = details.$on('on:edit', function() {
          onSyncDetails();
          removeOnSyncDetails();
        });
      }
    } else {
      syncSelection();
    }

    $scope.$broadcast('grid:adjust-columns');
  };

  $scope.attr = function (name) {
    if (!$scope.schema || $scope.schema[name] === undefined) {
      return true;
    }
    return $scope.schema[name];
  };

  $scope.canNew = function() {
    return $scope.hasButton('new');
  };

  $scope.canEdit = function() {
    return $scope.hasButton('edit') && ($scope.selection.length > 0 || $scope.dataView.getItemById(0));
  };

  $scope.canShowDetailsView = function () {
    var params = ($scope._viewParams || {}).params || {};
    return params['details-view'] && !axelor.device.mobile;
  };

  $scope.canShowSave = function () {
    if ($scope.$details) {
      return true;
    }
    return $scope.hasButton('save') && $scope.canEditInline();
  };

  $scope.canSave = function() {
    if ($scope.$details && $scope.$details.canSave()) {
      return true;
    }
    return $scope.hasButton('save') && this.dataView.canSave && this.dataView.canSave();
  };

  $scope.canDelete = function() {
    return $scope.hasButton('delete') && !$scope.canSave() && $scope.selection.length > 0;
  };

  $scope.canArchive = function() {
    return $scope.hasPermission('write')
      && $scope.hasButton('archive')
      && !$scope.canSave()
      && $scope.selection.length > 0;
  };

  $scope.canUnarchive = function() {
    return $scope.canArchive();
  };

  $scope.canEditInline = function() {
    return _.isFunction(this.dataView.canSave);
  };

  $scope.canMassUpdate = function () {
    // this permission is actually calculated from fields marked for mass update
    return $scope.hasPermission('massUpdate', false) || ($scope.schema && $scope.schema.canMassUpdate);
  };

  $scope.canExport = function() {
    return $scope.hasPermission('export');
  };

  $scope.selectFields = function() {
    var fields = _.map($scope.fields, function (field) {
      if (field.jsonField) {
        return field.name + '::' + (field.jsonType || 'text');
      }
      return field.name;
    });

    // consider target-name on o2o/m2o
    _.each(($scope.schema||{}).items, function (item) {
      var field = $scope.fields[item.name] || {};
      if (item.targetName && item.targetName !== field.targetName && _.endsWith(field.type, 'to-one')) {
        fields.push(item.name + '.' + item.targetName);
      }
    });

    return _.unique(fields);
  };

  $scope.filter = function(searchFilter) {

    var fields = $scope.selectFields(),
      options = {};

    function fixPage() {
      var promise = ds.fixPage();
      if (promise) {
        $scope.updateRoute();
        return promise;
      }
    }

    // if criteria is given search using it
    if (searchFilter.criteria || searchFilter._domains) {
      options = {
        filter: searchFilter,
        fields: fields
      };
      if (searchFilter.archived !== undefined) {
        options.archived = searchFilter.archived;
      }
      return ds.search(options).then(fixPage);
    }

    var filter =  {},
      sortBy, pageNum,
      domain = null,
      context = null,
      action = null,
      criteria = {
        operator: 'and'
      };

    for(var name in searchFilter) {
      var value = searchFilter[name];
      if (value !== '') filter[name] = value;
    }

    pageNum = +(filter._pageNum || 0);
    sortBy = filter._sortBy;
    domain = filter._domain;
    context = filter._context;
    action = filter._action;

    delete filter._pageNum;
    delete filter._sortBy;
    delete filter._domain;
    delete filter._context;
    delete filter._action;

    criteria.criteria = _.map(filter, function(value, key) {

      var field = $scope.fields[key] || _.findWhere(($scope.schema||{}).items, { name: key }) || {};
      var type = field.type || 'string';
      var operator = 'like';
      var origValue = value;
      var value2;

      //TODO: implement expression parser

      if (type === 'many-to-one' && !field.jsonField) {
        if (field.targetName) {
          key = key + '.' + field.targetName;
        } else {
          console.warn("Can't search on field: ", key);
        }
      }
      if (field.selection) {
        type = 'selection';
      }

      function stripOperator(val) {
        var match = /(<)(.*)(<)(.*)/.exec(val);
        if (match) {
          operator = 'between';
          value2 = match[2].trim();
          return match[4].trim();
        }
        match = /(<=?|>=?|=)(.*)/.exec(val);
        if (match) {
          operator = match[1];
          return match[2].trim();
        }
        return val;
      }

      function toMoment(val) {
        var format = 'MM/YYYY';
        if (/\d+\/\d+\/\d+/.test(val)) format = ui.dateFormat;
        if (/\d+\/\d+\/\d+\s+\d+:\d+/.test(val)) format = ui.dateTimeFormat;
        return val ? moment(val, format) : moment();
      }

      function toDate(val) {
        return val ? toMoment(val).format('YYYY-MM-DD') : val;
      }

      function toDateString(val) {
          return moment.utc(val, ui.dateFormat).toDate().toISOString().split("T")[0];
      }

      switch(type) {
        case 'integer':
        case 'long':
        case 'decimal':
          operator = '=';
          value = stripOperator(value);
          value = +(value) || 0;
          if (value2) value2 = +(value2) || 0;
          break;
        case 'boolean':
          operator = '=';
          value = !/f|n|false|no|0/.test(value);
          break;
        case 'date':
          operator = '=';
          value = stripOperator(value);
          if (value) value = toDateString(value);
          if (value2) value2 = toDateString(value2);
          break;
        case 'time':
          operator = '=';
          break;
        case 'datetime':
          operator = 'between';
          value = stripOperator(value);
          var val = toMoment(value);
          value = (operator == 'between' ? val.startOf('day') : val).toDate().toISOString();
          value2 = (operator == 'between' ? val.endOf('day') : val).toDate().toISOString();
          break;
        case 'enum':
        case 'selection':
          operator = '=';
          break;
      }

      // tag json fields
      if (field.jsonField) {
        key += '::' + (field.jsonType || 'text');
      }

      return {
        fieldName: key,
        operator: operator,
        value: value,
        value2: value2
      };
    });

    domain = domain || $scope._domain;
    context = _.extend({}, $scope._context, context);

    if (domain && $scope.getContext) {
      context = _.extend(context, $scope.getContext());
    }

    context._model = context._model || $scope._model;

    options = {
      filter: criteria,
      fields: fields,
      sortBy: sortBy,
      domain: domain,
      context: context,
      action: action
    };

    if (pageNum) {
      options.offset = (pageNum - 1 ) * ds._page.limit;
    }

    var advance = arguments.length > 1 ? arguments[1] : null;
    if (advance && advance.criteria && advance.criteria.length) {
      if (_.isEmpty(criteria.criteria)) {
        options.filter = advance;
      } else {
        criteria.criteria = [{
          operator: criteria.operator,
          criteria: criteria.criteria
        }, {
          operator: advance.operator,
          criteria: advance.criteria
        }];
        criteria.operator = "and";
      }
    }
    if (advance && advance.archived !== undefined) {
      options.archived = advance.archived;
    }
    return ds.search(options).then(fixPage);
  };

  $scope.pagerText = function() {
    if (page && page.from !== undefined) {
      if (page.total === 0) return null;
      return _t("{0} to {1} of {2}", page.from + 1, page.to, page.total);
    }
  };

  $scope.pagerIndex = function(fromSelection) {
    var index = page.index,
      record = null;
    if (fromSelection) {
      record = $scope.dataView.getItem(_.first($scope.selection));
      index = ds._data.indexOf(record);
    }
    return index;
  };

  $scope.onNext = function() {
    var fields = $scope.selectFields();
    ds.next(fields).then(function(){
      $scope.updateRoute();
    });
  };

  $scope.onPrev = function() {
    var fields = $scope.selectFields();
    ds.prev(fields).then(function(){
      $scope.updateRoute();
    });
  };

  $scope.onNew = function() {
    page.index = -1;
    if ($scope.$details) {
      $scope.$details.onNew();
      return;
    }
    $scope.switchTo('form', function(viewScope){
      $scope.ajaxStop(function(){
        $scope.$timeout(function(){
          viewScope.$broadcast('on:new');
        });
      });
    });
  };

  $scope.onEdit = function(force) {
    page.index = $scope.pagerIndex(true);
    $scope.switchTo('form', function (formScope) {
      formScope.__canForceEdit = force;
    });
  };

  $scope.$confirmMessage = _t("Do you really want to delete the selected record(s)?");
  $scope.$confirmArchiveMessage = _t("Do you really want to archive the selected record(s)?");
  $scope.$confirmUnarchiveMessage = _t("Do you really want to unarchive the selected record(s)?");

  $scope.onDelete = function() {
    var message = $scope.$confirmMessage;
    var message = _.isFunction(message) ? message() : message;

    axelor.dialogs.confirm(message, function(confirmed){

      if (!confirmed)
        return;

      var selected = _.map($scope.selection, function(index) {
        return $scope.dataView.getItem(index);
      });

      ds.removeAll(selected).success(function(records, page){
        if (records.length === 0 && page.total > 0) {
          $scope.onRefresh();
        }
      });
    });
  };

  $scope._doArchive = function (message, archive) {
    axelor.dialogs.confirm(message, function(confirmed) {
      if (!confirmed) {
        return;
      }

      var selected = _.map($scope.selection, function(index) {
        var item = $scope.dataView.getItem(index);
        return _.extend({}, _.pick(item, 'id', 'version'), { archived: archive });
      });

      ds.saveAll(selected).success(function() {
        $scope.onRefresh();
      });
    });
  };

  $scope.onArchive = function() {
    $scope._doArchive($scope.$confirmArchiveMessage, true);
  };

  $scope.onUnarchive = function() {
    $scope._doArchive($scope.$confirmUnarchiveMessage, false);
  };

  $scope.onRefresh = function() {
    if ($scope.$details) {
      $scope.$details.onNew().then(function () {
        $scope.reload();
      });
    } else {
      $scope.reload();
    }
  };

  $scope.isDirty = function () {
    if ($scope.$details && $scope.$details.isDirty) {
      return $scope.$details.isDirty();
    }
    return false;
  };

  $scope.confirmDirty = function(callback, cancelCallback) {
    if ($scope.$details && $scope.$details.confirmDirty) {
      return $scope.$details.confirmDirty(callback, cancelCallback);
    }
    return callback();
  };

  $scope.reload = function() {
    var fields = $scope.selectFields();
    return ds.search({
      fields: fields
    });
  };

  $scope.onSort = function(event, args) {
    var fields = $scope.selectFields();
    var sortBy = _.map(args.sortCols, function(column) {
      var field = column.sortCol.descriptor;
      var name = column.sortCol.field;
      if (field.jsonField) {
        if (field.type === 'many-to-one' && field.targetName) {
          name = name + "." + field.targetName;
        }
        name += '::' + ('integer,boolean,decimal'.indexOf(field.type) > -1 ? field.type : 'text');
      }
      var spec = column.sortAsc ? name : '-' + name;
      return spec;
    });
    ds.search({
      sortBy: sortBy,
      fields: fields
    });
  };

  $scope.onSelectionChanged = function(event, args) {
    var items = $scope.getItems();
    var selection = [];

    _.each(items, function (item) {
      item.selected = false;
    });
    _.each(args.rows, function(index) {
      var item = args.grid.getDataItem(index);
      if (item && item.id && item.id !== 0) {
        item.selected = true;
        selection.push(index);
      }
    });

    // update selected flags on record
    if ($scope.record && $scope.field) {
      var recordItems = $scope.record[$scope.field.name];
      if (recordItems && recordItems.length === items.length) {
        _.each(recordItems, function (recordItem, index) {
          recordItem.selected = items[index].selected;
        });
      }
    }

    $scope.selection = selection;
    $scope.$timeout(function () {
      $scope.$broadcast('on:grid-selection-change', $scope.getContext());
    });

    if ($scope.$details) {
      $scope.$details.selectionChanged(selection);
    }
  };

  $scope.onItemClick = function(event, args) {
    if (axelor.device.mobile) {
      $scope.$timeout(function () {
        $scope.onEdit();
      });
    }
  };

  $scope.onItemDblClick = function(event, args) {
    $scope.onEdit();
    $scope.$applyAsync();
  };

  var _getContext = $scope.getContext;
  $scope._isNestedGrid = !!_getContext;
  $scope.getContext = function() {

    // if nested grid then return parent's context
    if (_getContext) {
      return _getContext();
    }

    var dataView = $scope.dataView;
    var selected = _.map($scope.selection || [], function(index) {
      return dataView.getItem(index).id;
    });

    return selected.length ? { _ids: selected } : {};
  };

  $scope.getActionData = function() {
    // ignore if nested grid or has selected rows
    if (_getContext || !_.isEmpty($scope.selection)) {
      return false;
    }
    return _.extend({
      _domain: ds._lastDomain,
      _domainContext: ds._lastContext,
      _archived: ds._showArchived
    }, ds._filter);
  };

  $scope.onSave = function() {
    if ($scope.$details) {
      $scope.$details.onSave();
    }
    if ($scope.dataView.saveChanges) {
      $scope.dataView.saveChanges();
    }
  };

  $scope.onArchived = function(e) {
    var button = $(e.currentTarget);
    setTimeout(function(){
      var active = button.is('.active');
      var fields = $scope.selectFields();
      ds.search({
        fields: fields,
        archived: active
      });
    });
  };

  $scope.onExport = function (full) {
    var view = $scope.view || $scope.schema || {};
    if (!view.items) return;

    var names = _.pluck(view.items, 'name');
    var fields = full
      ? []
      : _.chain($scope.getVisibleCols())
         .map(function (col) { return (col.descriptor||{}).name; })
         .compact()
         .filter(function (name) { return names.indexOf(name) > -1; })
         .value();

    return ds.export_(fields).success(function(res) {
      var fileName = res.fileName;
      var filePath = 'ws/rest/' + $scope._model + '/export/' + fileName;
      if (ds._page.total > res.exportSize) {
        axelor.notify.alert(_t("{0} records exported.", res.exportSize), { title: _t('Warning!') });
      }
      ui.download(filePath, fileName);
    });
  };

  function focusFirst() {
    var index = _.first($scope.selection) || 0;
    var first = $scope.dataView.getItem(index);
    if (first) {
      $scope.dataView.$syncSelection([], [first.id], true);
    }
  }

  $scope.onHotKey = function (e, action) {
    if (action === "save" && $scope.canSave()) {
      $scope.onSave();
    }
    if (action === "refresh") {
      $scope.onRefresh();
    }
    if (action === "new") {
      $scope.onNew();
    }
    if (action === "edit") {
      if ($scope.canEdit()) {
        $scope.onEdit(true);
      } else {
        focusFirst();
      }
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

    $scope.$applyAsync();
    return false;
  };
}

ui.directive('uiViewGrid', function(){
  return {
    replace: true,
    template: '<div ui-slick-grid ui-widget-states></div>'
  };
});

ui.directive('uiViewDetails', ['DataSource', 'ViewService', function(DataSource, ViewService) {
  return {
    scope: {},
    controller: ['$scope', '$element', function ($scope, $element) {
      var parent = $scope.$parent;
      var params =  _.pick(parent._viewParams, ['views', 'model', 'domain', 'context', 'params']);

      var view = _.findWhere(params.views, { type: 'form' }) || { type: 'form' };
      if (params.params && _.isString(params.params['details-view'])) {
        view = { type: 'form', name: params.params['details-view']};
      }

      params.views = [view];
      $scope._viewParams = params;
      $scope._isDetailsForm = true;

      ui.ViewCtrl.call(this, $scope, DataSource, ViewService);

      // use same ds as grid
      $scope._dataSource = parent._dataSource;

      ui.FormViewCtrl.call(this, $scope, $element);

      parent.$parent.$details = $scope;

      var ds = $scope._dataSource;
      var noop = angular.noop;

      $scope.getRouteOptions = noop;
      $scope.setRouteOptions = noop;
      $scope.updateRoute = noop;
      $scope.$locationChangeCheck = noop;
      $scope.switchBack = noop;
      $scope.switchTo = noop;
      $scope.onHotKey = noop;

      $scope._dataSource = parent._dataSource;

      $scope.setEditable(true);
      $scope.show();

      function doEdit(index) {
        var found = ds.at(index);
        var record = $scope.record;
        if (record && found.id === record.id) return;
        $scope.doRead(found.id).success(function(record) {
          $scope.edit(record);
        });
      }

      $scope.selectionChanged = _.debounce(function () {
        var current = $scope.record || {};
        var first = parent.pagerIndex(true);
        if (first > -1) {
          doEdit(first);
        } else if (current.id > 0) {
          $scope.edit(null);
        }
      }, 300);

      $scope.$on("on:new", function(e) {
        var dataView = parent.dataView;
        if (dataView && dataView.$syncSelection) {
          dataView.$syncSelection([], [], true);
        }
      });

      $scope.$on("on:edit", function(e) {
        var record = $scope.record || {};
        var dataView = parent.dataView;
        if (dataView && record.id > 0) {
          var found = _.findWhere(dataView.getItems(), { id: record.id });
          if (found) {
            found.selected = true;
          }
        }
      });
    }],
    link: function (scope, element, attrs) {
      var overlay = $("<div class='slickgrid-overlay'>");
      scope.waitForActions(function () {
        element.parent().children('.slickgrid').append(overlay);
      });

      scope.$watch('$$dirty', function gridDirtyWatch(dirty) {
        overlay.toggle(dirty);
        if (scope.$parent.dataView && scope.$parent.dataView.$cancelEdit) {
          scope.$parent.dataView.$cancelEdit();
        }
      });

      scope.$on('$destroy', function () {
        overlay.remove();
      });
    },
    replace: true,
    templateUrl: "partials/views/details-form.html"
  };
}]);

ui.directive('uiPortletRefresh', ['NavService', function (NavService) {
  return function (scope, element) {
    if (!scope.onRefresh) return;

    var onRefresh = scope.onRefresh.bind(scope);
    var unwatch = false;
    var loading = false;

    scope.onRefresh = function () {
      var tab = NavService.getSelected();
      var type = (tab.params||{})['details-view'] ? scope.$parent._viewType : tab.viewType || tab.type;
      if (['dashboard', 'form'].indexOf(type) === -1) {
        if (unwatch) {
          unwatch();
          unwatch = null;
        }
        return;
      }

      if (unwatch || loading) {
        return;
      }

      unwatch =  scope.$watch(function portletVisibleWatch() {
        if (element.is(":hidden")) {
          return;
        }

        unwatch();
        unwatch = null;
        loading = true;

        scope.waitForActions(function () {
          scope.ajaxStop(function () {
            loading = false;
            onRefresh();
          });
        });
      });
    };
  };
}])

ui.directive('uiNestedGridActions', function () {
  return {
    scope: true,
    link: function (scope, element, attrs) {

      var _getContext = scope.getContext;

      scope.getContext = function () {
        var dataView = scope.dataView;
        var selected = _.map(scope.selection || [], function(index) {
          return dataView.getItem(index).id;
        });
        var context = {
          _parent: _getContext.call(scope),
          _ids: selected.length ? selected : undefined
        };
        return context;
      };

      scope.isHidden = function () {
        return false;
      };

      scope.isReadonlyExclusive = function () {
        return false;
      }
    },
    replace: true,
    template:
      "<div class='panel-related-actions'>" +
        "<div class='btn-group view-toolbar' ng-if='toolbar.length'>" +
          "<button ng-repeat='btn in toolbar | filter:{custom: true} | limitTo:3' class='btn' ui-tool-button='btn'>{{btn.title}}</button>" +
        "</div>" +
        "<div ui-menu-bar x-menus='[menubar[0]]' x-handler='this' class='view-menubar' ng-if='menubar.length'></div>" +
      "</div>"
  }
});

ui.directive('uiPortletGrid', function(){
  return {
    controller: ['$scope', '$element', 'ViewService', 'NavService', 'MenuService',
                 function($scope, $element, ViewService, NavService, MenuService) {

      GridViewCtrl.call(this, $scope, $element);

      var ds = $scope._dataSource;

      function doEdit(force) {
        var promise = MenuService.action($scope._viewAction, {
          context: $scope.getContext()
        });

        promise.success(function (result) {
          if (!result.data) return;
          var view = result.data[0].view;
          return doOpen(force, view);
        });
      }

      function doOpen(force, tab) {
        var index = $scope.pagerIndex(true);
        var record = ds.at(index);

        if ($scope._viewAction === "dms.file.children") {
          NavService.openTabByName("dms.file", {
            mode: "edit",
            state: record.id
          });
          return;
        }

        tab.viewType = "form";
        tab.recordId = record.id;
        tab.action = _.uniqueId('$act');
        tab.forceReadonly = $scope.isReadonly && $scope.isReadonly();

        if (tab.forceReadonly || $scope._isPopup || (($scope._viewParams || {}).params || {}).popup) {
          tab.$popupParent = $scope;
          tab.params = tab.params || {};
          _.defaults(tab.params, {
            'show-toolbar': false
          });
        }

        setTimeout(function(){
          NavService.openView(tab);
          $scope.$applyAsync();
          if (force) {
            $scope.waitForActions(function() {
              var scope = tab.$viewScope || ($scope.selectedTab || {}).$viewScope;
              if (scope && scope.onEdit) {
                scope.onEdit();
              }
            });
          }
        });
      }

      $scope.showPager = true;
      $scope.onEdit = doEdit;
      $scope.onItemDblClick = function(event, args) {
        doEdit(false);
      };

      $scope.$on("on:new", function(e) {
        ds._page.from = 0;
        $scope.onRefresh();
      });
      $scope.$on("on:edit", function(e) {
        ds._page.from = 0;
        $scope.onRefresh();
      });

      $scope.onRefresh = function () {
        $scope.filter({});
      };

      var _onShow = $scope.onShow;
      var _filter = $scope.filter;
      var _action = $scope._viewAction;
      var _field = $scope.field || {};

      $scope.onGridInit = function (grid, inst) {
        var editCol = _.findWhere(inst.cols, {id: '_edit_column'}) || {};
        editCol.descriptor = { hidden : true };
        $scope.$parent.$watch("isReadonly()", function (readonly) {
          if (inst.editable) {
            inst.readonly = readonly;
          }
          inst.showColumn('_edit_column', !readonly);
          editCol.descriptor = { hidden : readonly };
        });
      };

      $scope.filter = function (searchFilter) {
        var opts = _.extend({}, searchFilter, {
          _action: _action
        });
        var ds = $scope._dataSource;
        var view = $scope.schema || {};
        if (!opts._sortBy && !ds._sortBy && view.orderBy) {
          opts._sortBy = view.orderBy.split(',');
        }
        if ($scope._context && $scope.formPath && $scope.getContext) {
          opts._context = _.extend({id: null}, _.pick($scope.getContext(), _.keys($scope._context)));
          if ($scope._context._id) {
            opts._context._id = opts._context.id;
          }
        }
        return _filter.call($scope, opts);
      };

      $scope.onShow = function () {
        var scope = ($scope.selectedTab || {}).$viewScope;
        if (scope && scope.editRecord) {
          return;
        }
        return _onShow.apply($scope, arguments);
      };
    }],
    replace: true,
    template:
    '<div class="portlet-grid" ui-portlet-refresh>'+
      '<div ui-view-grid x-view="schema" x-on-init="onGridInit" x-data-view="dataView" x-editable="false" x-no-filter="{{noFilter}}" x-handler="this"></div>'+
    '</div>'
  };
});

ui.directive('uiTopHelp', function () {
  return {
    link: function (scope, element) {
      var unwatch = scope.$watch('schema', function gridSchemaWatch(view) {
        if (view) {
          unwatch();
        }
        if (view && view.help) {
          element.popover({
            html: true,
            title: view.title,
            content: view.help,
            placement: 'bottom',
            trigger: 'hover',
            delay: { show: 500, hide: 100 },
            container: 'body'
          });
        }
      });
    },
    replace: true,
    template:
      "<button ng-show='schema.help'>" +
        "<i class='fa fa-info'></i>" +
      "</button>"
  };
});

})();
