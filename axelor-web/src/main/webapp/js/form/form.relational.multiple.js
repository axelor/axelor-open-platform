/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

var ui = angular.module("axelor.ui");

ui.OneToManyCtrl = OneToManyCtrl;
ui.OneToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function OneToManyCtrl($scope, $element, DataSource, ViewService, initCallback) {

  ui.RefFieldCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
    ui.GridViewCtrl.call(this, $scope, $element);
    $scope.editorCanSave = false;
    $scope.selectEnable = false;
    $scope.toolbar = null;
    $scope.menubar = null;
    $scope.selectMode = "multi";
    if (initCallback) {
      initCallback();
    }
  });

  var detailView = null;

  $scope.createDetailView = function() {
    var es;
    if (detailView == null) {
      detailView = $('<div ui-embedded-editor class="detail-form"></div>').attr('x-title', $element.attr('x-title'));
      detailView = ViewService.compile(detailView)($scope);
      detailView.data('$rel', $());
      es = detailView.data('$scope');
      es.setDetailView();
      $element.after(detailView);

      es.$on('on:before-save-action', function () {
        es.$parent.select(_.extend(es.record, {selected: true}));
      });
    } else {
      es = detailView.data('$scope');
    }
  };

  $scope.select = function(value) {

    // if items are same, no need to set values
    if ($scope._dataSource.equals(value, $scope.getValue())) {
      return;
    }

    var items = _.chain([value]).flatten(true).compact().value();
    var records = _.map($scope.getItems(), _.clone);

    if ($scope.editorCanSave) {
      items = items.map(function (item) {
        var version = item.version || item.$version || 0;
        var result = _.omit(item, 'version');
        return _.extend(result, {
          $version: version,
          $fetched: false
        });
      });
    }

    // update dotted fields from nested fields
    if (value && !Array.isArray(value)) {
      Object.keys($scope.fields)
        .filter(function(name) { return name.indexOf('.') >= 0; })
        .forEach(function(name) {
          var first = items[0];
          delete first[name];
          var val = ui.findNested(first, name);
          if (val !== undefined) {
            first[name] = val;
          }
        });
    }

    _.each(records, function(item) {
      item.selected = false;
    });

    _.each($scope.itemsPending, function (item) {
      var find = _.find(records, function(rec) {
        if (rec.id && rec.id == item.id) {
          return true;
        }
        var a = _.omit(item, 'id', 'version');
        var b = _.omit(rec, 'id', 'version');
        return $scope._dataSource.equals(a, b);
      });
      if (!find) {
        records.push(item);
      }
    });

    _.each(items, function(item){
      item = _.clone(item);
      var find = _.find(records, function(rec){
        var id1 = rec.id || rec.$id;
        var id2 = item.id || item.$id;
        if (id1 < 0 && id2 > 0) id2 = item.$id || item.id;
        return id1 && id1 === id2;
      });

      if (find) {
        _.extend(find, item);
      } else {
        records.push(item);
      }
    });

    records = $scope.$$ensureIds(records);

    _.each(records, function(rec){
      if (rec.id <= 0) rec.id = null;
    });

    if ($scope.dataView.$resequence) {
      $scope.dataView.$resequence(records);
    }

    var callOnChange = $scope.dataView.$isResequencing !== true;

    $scope.itemsPending = records;
    $scope.setValue(records, callOnChange);
    $scope.$timeout(function() {
      $scope.$broadcast('grid:changed');
    });
  };

  var _setItems = $scope.setItems;
  $scope.setItems = function(items) {
    _setItems(items);
    $scope.itemsPending = [];
    if (detailView !== null) {
      if (items === null || _.isEmpty(items)) {
        detailView.hide();
      } else {
        detailView.show();
      }
    }
  };

  $scope.removeItems = function(items, fireOnChange) {
    var all, ids, records;

    if (_.isEmpty(items)) return;

    all = _.isArray(items) ? items : [items];

    ids = _.map(all, function(item) {
      return _.isNumber(item) ? item : item.id;
    });

    records = _.filter($scope.getItems(), function(item) {
      return ids.indexOf(item.id) === -1;
    });

    $scope.setValue(records, fireOnChange);
    $scope.$applyAsync();
  };

  $scope.removeSelected = function(selection) {
    var selected, items;
    if (_.isEmpty(selection)) return;
    selected = _.map(selection, function (i) {
      return $scope.dataView.getItem(i).id;
    });
    items = _.filter($scope.getItems(), function (item) {
      return selected.indexOf(item.id) === -1;
    });
    // remove selected from data view
    _.each(selected, function (id) {
      if (id && id > -1) $scope.dataView.deleteItem(id);
    });
    $scope.dataView.$setSelection([]);
    $scope.setValue(items, true);
    $scope.$applyAsync();
  };

  $scope.canEditTarget = function () {
    return $scope.canEdit() && $scope.attr('canEdit') !== false;
  };

  $scope.$on('on:grid-edit-start', function() {
    $scope._editorVisible = true;
  });

  $scope.$on('on:grid-edit-end', function() {
    $scope._editorVisible = false;
  });

  $scope.canShowEdit = function () {
    if ($scope._editorVisible) return false;
    var selected = $scope.selection.length ? $scope.selection[0] : null;
    return selected !== null && $scope.canEdit();
  };

  $scope.canShowView = function () {
    if ($scope._editorVisible || $scope.canShowEdit()) return false;
    var selected = $scope.selection.length ? $scope.selection[0] : null;
    return selected !== null && $scope.canView();
  };

  $scope.canEdit = function () {
    return $scope.attr('canEdit') !== false && $scope.canView();
  };

  var _canRemove = $scope.canRemove;
  $scope.canRemove = function () {
    var selected = $scope.selection.length ? $scope.selection[0] : null;
    return _canRemove() && selected !== null;
  };

  $scope.canCopy = function () {
    if (!$scope.field || !($scope.field.widgetAttrs||{}).canCopy) return false;
    if (!$scope.canNew()) return false;
    if (!$scope.selection || $scope.selection.length !== 1) return false;
    var record = $scope.dataView.getItem(_.first($scope.selection));
    return !!record;
  };

  $scope.onCopy = function() {
    var ds = $scope._dataSource;
    var index = _.first($scope.selection);
    var item = $scope.dataView.getItem(index);
    var doSelect = function (record) {
      if (!record.id) $scope.$$ensureIds([record]);
      $scope.select([record]);
      $scope.$timeout(function () {
        $scope.dataView.$setSelection([$scope.dataView.getLength() - 1], true);
      }, 100);
    };
    if (item && item.id > 0) {
      ds.copy(item.id).success(doSelect);
    } else if (item) {
      item = angular.copy(item);
      item.id = undefined;
      item.$id = undefined;
      doSelect(item);
    }
  };

  $scope.onEdit = function() {
    var selected = $scope.selection.length ? $scope.selection[0] : null;
    if (selected !== null) {
      var record = $scope.dataView.getItem(selected);
      $scope.showEditor(record);
    }
  };

  $scope.onRemove = function() {
    if (this.isReadonly()) {
      return;
    }
    axelor.dialogs.confirm(_t("Do you really want to delete the selected record(s)?"), function(confirmed){
      if (confirmed && $scope.selection && $scope.selection.length)
        $scope.removeSelected($scope.selection);
    });
  };

  $scope.viewCanCopy = function () {
    return this.hasPermission("create") && !this.isDisabled() && !this.isReadonly() && this.canCopy();
  };

  $scope.viewCanExport = function () {
    if (!$scope.field || !($scope.field.widgetAttrs||{}).canExport) return false;
    var ids = _.compact(_.pluck($scope.getValue(), 'id'));
    if (ids.length === 0
        || _.some($scope.getValue(), function (item) { return (item || {}).$dirty; })) {
      return false;
    }
    return this.hasPermission("export") && this.canExport();
  };

  $scope.getSelectedRecord = function() {
    var selected = _.first($scope.selection || []);
    if (_.isUndefined(selected))
      return null;
    return $scope.dataView.getItem(selected);
  };

  var _onSelectionChanged = $scope.onSelectionChanged;
  $scope.onSelectionChanged = function(e, args) {
    _onSelectionChanged(e, args);

    var field = $scope.field,
      record = $scope.record || {},
      selection = $scope.selection || [];

    record.$many = record.$many || (record.$many = {});
    record.$many[field.name] = selection.length ? $scope.getItems : null;

    if (detailView === null) {
      return;
    }

    $scope.$timeout(function() {
      var dvs = detailView.scope();
      var rec = $scope.getSelectedRecord();
      if ($scope.isHidden()) {
        detailView.hide();
      } else {
        detailView.show();
      }
      if (rec == null) {
        dvs.edit(rec);
        return;
      }
      if (!dvs.record || ((dvs.record.id || 0) !== (rec.id || 0))) {
        if (dvs.record && (dvs.record.id === 0 || dvs.record.id === null) && rec.id < 0) {
          // Same record; was just assigned a negative id
          dvs.edit(rec, false);
        } else {
          dvs.edit(rec);
        }
      }
    });
  };

  $scope.onItemDblClick = function(event, args) {
    if($scope.canView()){
      $scope.onEdit();
      $scope.$applyAsync();
    }
  };

  (function (scope) {

    var dummyId = 0;

    function ensureIds(records, clone) {
      var items = [];
      angular.forEach(records, function(record){
        var item = clone ? angular.copy(record, {}) : (record || {});
        if (!item.id) {
          item.id = item.$id || (item.$id = --dummyId);
        }
        items.push(item);
      });
      return items;
    }

    function fetchData() {
      var items = scope.getValue();
      return scope.fetchData(items, function(records){
        scope.setItems(ensureIds(records, true));
      });
    }

    scope.$$ensureIds = ensureIds;
    scope.$$fetchData = fetchData;

  })($scope);

  $scope.reload = function() {
    return $scope.$$fetchData();
  };

  $scope.filter = function() {

  };

  $scope.onSort = function(event, args) {

    //TODO: implement client side sorting (prevent losing O2M changes).
    if ($scope.isDirty() && !$scope.editorCanSave) {
      return;
    }

    var records = $scope.getItems();
    if (records == null || records.length === 0)
      return;

    for (var i = 0; i < records.length; i++) {
      var item = records[i];
      if (item.$dirty || !item.id || item.id <= 0) {
        return;
      }
    }

    var sortBy = [];

    angular.forEach(args.sortCols, function(column){
      var field = column.sortCol.descriptor;
      var name = column.sortCol.field;
      if (field.jsonField) {
        if (field.type === 'many-to-one' && field.targetName) {
          name = name + "." + field.targetName;
        }
        name += '::' + ('integer,boolean,decimal'.indexOf(field.type) > -1 ? field.type : 'text');
      }
      var spec = column.sortAsc ? name : '-' + name;
      sortBy.push(spec);
    });

    var ids = _.pluck(records, 'id');
    var criterion = {
      'fieldName': 'id',
      'operator': 'in',
      'value': ids
    };

    var fields = $scope.selectFields();
    var filter = {
      operator: 'and',
      criteria: [criterion]
    };

    var context = _.pick($scope.getContext(), ['id', '_model']);
    context._field = $scope.field.name;
    context._field_ids = ids;

    $scope.selection = [];
    $scope._dataSource.search({
      filter: filter,
      fields: fields,
      sortBy: sortBy,
      archived: true,
      limit: -1,
      domain: null,
      context: context
    });
  };

  $scope.onShow = function(viewPromise) {

  };

  $scope.show();
}

ui.ManyToManyCtrl = ManyToManyCtrl;
ui.ManyToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function ManyToManyCtrl($scope, $element, DataSource, ViewService) {

  OneToManyCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
    $scope.editorCanSave = true;
    $scope.selectEnable = true;
  });

  var _setValue = $scope.setValue;
  $scope.setValue = function(value, trigger) {
    var compact = _.map(value, function(item) {
      return {
        id: item.id,
        $version: item.version
      };
    });
    _setValue(compact, trigger);
  };
}

ui.formInput('OneToMany', {

  css: 'one2many-item',

  transclude: true,

  showTitle: false,

  collapseIfEmpty: true,

  controller: OneToManyCtrl,

  link: function(scope, element, attrs, model) {

    scope.ngModel = model;
    scope.title = attrs.title;

    scope.formPath = scope.formPath ? scope.formPath + "." + attrs.name : attrs.name;

    var doRenderUnwatch = null;
    var doViewPromised = false;

    var validate = model.$validators.valid || function () { return true; };
    model.$validators.valid = function(modelValue, viewValue) {
      if (scope.isRequired() && _.isEmpty(viewValue)) return false;
      return validate.call(model.$validators, viewValue);
    };

    if (scope.field.requiredIf) {
      scope.$watch("attr('required')", function o2mRequiredWatch(required) {
        if (required !== undefined) {
          model.$validate();
        }
      });
    }
    if (scope.field.validIf) {
      scope.$watch("attr('valid')", function o2mValidWatch(valid) {
        if (valid !== undefined) {
          model.$setValidity('invalid', valid);
        }
      });
    }

    function doRender() {
      if (doRenderUnwatch) {
        return;
      }
      doRenderUnwatch = scope.$watch(function o2mFetchWatch() {
        if (!isVisible() || !doViewPromised) {
          return;
        }
        doRenderUnwatch();
        doRenderUnwatch = null;
          scope.$$fetchData();
        });
    }

    function isVisible() {
      return !element.is(':hidden');
    }

    scope._viewPromise.then(function () {
      doViewPromised = true;
      if (doRenderUnwatch) {
        doRenderUnwatch();
        doRenderUnwatch = null;
        doRender();
      }
      });

    model.$render = doRender;

    var adjustHeight = false;
    var adjustSize = (function() {
      var rowSize = +(scope.field.rowHeight) || 26;
      var	minSize = 56;
      var elem = element;
      if (elem.is('.panel-related')) {
        elem = element.children('.panel-body');
        minSize = 28;
      } else if (elem.is('.stackbar')) {
        elem = element.children('.grid-container');
        minSize = 50;
      } else if (scope.$hasPanels) {
        minSize += 28;
      }
      if (elem.is('.picker-input')) {
        elem = null;
      }

      var inc = 0,
        height = +(scope.field.height) || 10;
      var maxSize = (rowSize * height) + minSize;

      var unwatch = scope.$watch('schema', function (schema) {
        if (schema) {
          unwatch();
          unwatch = null;
          if (schema.rowHeight && schema.rowHeight !== rowSize) {
            rowSize = schema.rowHeight;
            maxSize = (rowSize * height) + minSize;
          }
        }
      });

      return function(value) {
        inc = arguments[1] || inc;
        var count = Math.max(_.size(value), scope.dataView.getLength()) + inc, height = minSize;
        if (count > 0) {
          height = (rowSize * count) + (minSize + rowSize);
        }
        if (elem && adjustHeight) {
          elem.css('min-height', count ? Math.min(height, maxSize) : (minSize + 26));
        }
        axelor.$adjustSize();
      };
    })();

    var collapseIfEmpty = this.collapseIfEmpty;
    scope.$watch(attrs.ngModel, function o2mAdjustWatch(value){
      if (!value) {
        // clear data view
        scope.dataView.setItems([]);
      }
      if (collapseIfEmpty) {
        adjustSize(value);
      }
    });

    function deleteItemsById(id) {
      var items = scope.dataView.getItems() || [];
      while (items.length > 0) {
        var item = _.findWhere(items, {id: id});
        var index = _.indexOf(items, item);
        if (index === -1) {
          break;
        }
        items.splice(index, 1);
      }
      return items;
    }

    scope.onGridInit = function(grid, inst) {
      var editIcon = scope.canView() || (!scope.isReadonly() && scope.canEdit());
      var editable = inst.editable && !axelor.device.mobile;

      adjustHeight = true;

      if (editable) {
        element.addClass('inline-editable');
        scope.$on('on:new', function(event){
          var items = deleteItemsById(0);
          if (items.length === 0) {
            scope.dataView.setItems([]);
            grid.setSelectedRows([]);
          }
        });
        scope.$watch("isReadonly()", function o2mReadonlyWatch(readonly) {
          inst.readonly = readonly;
          var _editIcon = scope.canView() || (!readonly && scope.canEdit());
          if (_editIcon != editIcon) {
            inst.showColumn('_edit_column', editIcon = _editIcon);
          }
        });

        adjustSize(scope.getValue(), 1);
      } else {
        adjustSize(scope.getValue());
      }

      inst.showColumn('_edit_column', editIcon);

      grid.onAddNewRow.subscribe(function (e, args) {
        var items = scope.getValue() || [];
        var rows = grid.getDataLength();
        adjustSize(items, rows - items.length + 1);
      });

      scope.dataView.onRowCountChanged.subscribe(function (e, args) {
        adjustSize();
      });
    };

    scope.onGridBeforeSave = function(records) {
      if (!scope.editorCanSave) {
        deleteItemsById(0);
        scope.select(records);
        return false;
      }
      return true;
    };

    scope.onGridAfterSave = function(records, args) {
      if (scope.editorCanSave) {
        scope.select(records);
      }
    };

    scope.isDisabled = function() {
      return this.isReadonly();
    };

    var field = scope.field;
    if (field.widget === 'master-detail') {
      setTimeout(function(){
        scope.createDetailView();
      });
    }

    scope.$watch("attr('title')", function o2mTitleWatch(title){
      scope.title = title;
    });
  },

  template_editable: null,

  template_readonly: null,

  template:
  "<div class='stackbar' ng-class='{noEdit: canView() && !canEdit()}'>" +
  "<div class='navbar'>" +
      "<div class='navbar-inner'>" +
          "<div class='container-fluid'>" +
              "<span class='brand' href='' ui-help-popover ng-bind-html='title'></span>" +
          "<span class='icons-bar dropdown pull-right' ng-show='viewCanCopy() || viewCanExport()'>" +
          "<a href='' class='dropdown-toggle' data-toggle='dropdown'><i class='fa fa-caret-down'></i></a>" +
          "<ul class='dropdown-menu'>" +
            "<li ng-show='viewCanCopy()'><a href='' ng-click='onCopy()' x-translate>Duplicate</a></li>" +
            "<li ng-show='viewCanExport()'><a href='' ng-click='onExport()' x-translate>Export</a></li>" +
          "</ul>" +
        "</span>" +
              "<span class='icons-bar pull-right' ng-show='!isReadonly()'>" +
          "<a href='' ng-click='onSelect()' ng-show='hasPermission(\"read\") && canShowIcon(\"select\") && !isDisabled() && canSelect()'>" +
            "<i class='fa fa-search'></i><span x-translate>Select</span>" +
          "</a>" +
          "<a href='' ng-click='onNew($event)' ng-show='hasPermission(\"create\") && canShowIcon(\"new\") && !isDisabled() && canNew()'>" +
            "<i class='fa fa-plus'></i><span x-translate>New</span>" +
          "</a>" +
          "<a href='' ng-click='onEdit()' ng-show='hasPermission(\"read\") && canShowIcon(\"edit\") && canShowEdit()'>" +
            "<i class='fa fa-pencil'></i><span x-translate>Edit</span>" +
          "</a>" +
          "<a href='' ng-click='onEdit()' ng-show='hasPermission(\"read\") && canShowIcon(\"view\") && canShowView()'>" +
            "<i class='fa fa-file-text-o'></i><span x-translate>Show</span>" +
          "</a>" +
          "<a href='' ng-click='onRemove()' ng-show='hasPermission(\"read\") && canShowIcon(\"remove\") && !isDisabled() && canRemove()'>" +
            "<i class='fa fa-remove'></i><span x-translate>Remove</span>" +
          "</a>" +
                  "<i ng-click='onCopy()' ng-show='hasPermission(\"create\") && !isDisabled() && canCopy()' title='{{\"Duplicate\" | t}}' class='fa fa-files-o'></i>" +
              "</span>" +
          "</div>" +
      "</div>" +
  "</div>" +
  "<div class='grid-container'>" +
    "<div ui-view-grid " +
      "x-view='schema' " +
      "x-data-view='dataView' " +
      "x-handler='this' " +
      "x-no-filter='true' " +
      "x-on-init='onGridInit' " +
      "x-on-before-save='onGridBeforeSave' " +
      "x-on-after-save='onGridAfterSave' " +
      "></div>" +
  "</div>" +
  "</div>"
});

ui.formInput('ManyToMany', 'OneToMany', {

  css	: 'many2many-item',

  controller: ManyToManyCtrl
});

var panelRelatedTemplate =
"<div class='panel panel-related' ng-class='{noEdit: canView() && !canEdit()}'>" +
  "<div class='panel-header'>" +
    "<div class='panel-title'><span ui-help-popover ng-bind-html='title'></span></div>" +
    "<div ui-nested-grid-actions ng-if='field.showBars'></div>" +
    "<div class='icons-bar' ng-show='!isReadonly()'>" +
      "<a href='' ng-click='onSelect()' ng-show='hasPermission(\"read\") && canShowIcon(\"select\") && !isDisabled() && canSelect()'>" +
        "<i class='fa fa-search'></i><span x-translate>Select</span>" +
      "</a>" +
      "<a href='' ng-click='onNew($event)' ng-show='hasPermission(\"create\") && canShowIcon(\"new\") && !isDisabled() && canNew()'>" +
        "<i class='fa fa-plus'></i><span x-translate>New</span>" +
      "</a>" +
      "<a href='' ng-click='onEdit()' ng-show='hasPermission(\"read\") && canShowIcon(\"edit\") && canShowEdit()'>" +
        "<i class='fa fa-pencil'></i><span x-translate>Edit</span>" +
      "</a>" +
      "<a href='' ng-click='onEdit()' ng-show='hasPermission(\"read\") && canShowIcon(\"view\") && canShowView()'>" +
        "<i class='fa fa-file-text-o'></i><span x-translate>Show</span>" +
      "</a>" +
      "<a href='' ng-click='onRemove()' ng-show='hasPermission(\"read\") && canShowIcon(\"remove\") && !isDisabled() && canRemove()'>" +
        "<i class='fa fa-remove'></i><span x-translate>Remove</span>" +
      "</a>" +
    "</div>" +
    "<div class='icons-bar dropdown' ng-show='viewCanCopy() || viewCanExport()'>" +
      "<a href='' class='dropdown-toggle' data-toggle='dropdown'><i class='fa fa-caret-down'></i></a>" +
      "<ul class='dropdown-menu'>" +
        "<li ng-show='viewCanCopy()'><a href='' ng-click='onCopy()' x-translate>Duplicate</a></li>" +
        "<li ng-show='viewCanExport()'><a href='' ng-click='onExport()' x-translate>Export</a></li>" +
      "</ul>" +
    "</div>" +
  "</div>" +
  "<div class='panel-body panel-layout'>" +
    "<div ui-view-grid " +
      "x-view='schema' " +
      "x-data-view='dataView' " +
      "x-handler='this' " +
      "x-no-filter='true' " +
      "x-on-init='onGridInit' " +
      "x-on-before-save='onGridBeforeSave' " +
      "x-on-after-save='onGridAfterSave'></div>" +
  "</div>" +
"</div>";

ui.formInput('PanelOneToMany', 'OneToMany', {
  template_editable: null,
  template_readonly: null,
  template: panelRelatedTemplate
});

ui.formInput('PanelManyToMany', 'ManyToMany', {
  template_editable: null,
  template_readonly: null,
  template: panelRelatedTemplate
});

ui.formInput('TagSelect', 'ManyToMany', 'MultiSelect', {

  css	: 'many2many-tags',

  showTitle: true,

  init: function(scope) {
    this._super(scope);

    var nameField = scope.field.targetName || 'id';

    scope.parse = function(value) {
      return value;
    };

    scope.formatItem = function(item) {
      if (item && scope._items && item.id in scope._items) {
        item = scope._items[item.id];
      }
      if (!item) return item;
      var key = nameField;
      var trKey = '$t:' + key;
      if (trKey in item) key = trKey;
      return ui.findNested(item, key);
    };

    scope.getItems = function() {
      return _.pluck(this.getSelection(), "value");
    };

    var _select = scope.select;
    scope.select = function (value) {
      var res = _select.apply(scope, arguments);
      scope.itemsPending = [];
      return res;
    };

    scope.handleClick = function(e, item) {
      if (scope.field['tag-edit'] && scope.onTagEdit && !scope.isReadonly()) {
        return scope.onTagEdit(e, item);
      }
      scope.showEditor(item);
    };

    scope.isRequired = function() {
      return scope.attr("required") && _.isEmpty(scope.text);
    };

    scope.validate = function (viewValue) {
      return !scope.isRequired() || !_.isEmpty(viewValue);
    };
  },

  link: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var validate = model.$validators.valid || function () { return true; };
    model.$validators.valid = function(modelValue, viewValue) {
      if (scope.isRequired() && _.isEmpty(viewValue)) return false;
      return validate.call(model.$validators, viewValue);
    };

    var field = scope.field;
    // special case for json fields
    if (field.jsonField) {
      var nameField = scope.field.targetName;
      var ds = scope._dataSource._new(scope._dataSource._model);
      scope.$watch(attrs.ngModel, function m2mValueWatch(value, old) {
        scope._items = null;
        if (value && value.length && value[0][nameField] === undefined) {
          ds.search({
            fields: ['id', nameField],
            domain: "id in :ids",
            context: { ids: _.pluck(value, 'id') }
          }).success(function (records) {
            scope._items = _.reduce(records, function(memo, item) {
              memo[item.id] = item;
              return memo;
            }, {});
            model.$render();
          });
        }
      }, true);
    }

    var superRender = model.$render;
    model.$render = function () {
      superRender.apply(model, arguments);
      var extraFields = [scope.field.targetName, scope.field.colorField]
        .filter(function (name) { return name; });
      if (_.isEmpty(extraFields)) return;
      var items = scope.getItems();
      var missing = _.filter(items, function (item) {
        return !_.isEmpty(_.difference(extraFields, Object.keys(item)));
      });
      if (_.isEmpty(missing)) return;

      var ids = _.pluck(missing, 'id');
      var context = _.pick(scope.getContext(), ['id', '_model']);
      context._field = scope.field.name;
      context._field_ids = ids;
      scope._dataSource.search({
        fields: extraFields,
        filter: {
          operator: 'and',
          criteria: [{
            fieldName: 'id',
            operator: 'in',
            value: ids
          }]
        },
        archived: true,
        limit: -1,
        domain: null,
        context: context,
      }).success(function (records) {
        _.each(records, function (record) {
          var item = _.findWhere(items, {id: record.id});
          if (item) {
            _.extend(item, _.pick(record, extraFields));
          }
        });
        scope.format(items);
      });
    }

  },

  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var input = this.findInput(element);
    var field = scope.field;

    function create(term, popup) {
      scope.createOnTheFly(term, popup, function (record) {
        scope.select(record);
        setTimeout(function() {
          input.focus();
        });
      });
    }

    scope.loadSelection = function(request, response) {

      if (!scope.canSelect()) {
        return response([]);
      }

      this.fetchSelection(request, function(items, page) {
        var term = request.term;
        var text = '<strong><em>' + term + '</em></strong>';
        var canSelect = scope.canSelect() && (items.length < page.total || (request.term && items.length === 0));
        if (field.create && term && scope.canNew() && scope.hasPermission("create")) {
          items.push({
            label : _t('Create "{0}" and select...', text),
            click : function() { create(term); }
          });
          items.push({
            label : _t('Create "{0}"...', text),
            click : function() { create(term, true); }
          });
        }
        if (canSelect) {
          items.push({
            label : _t("Search more..."),
            click : function() { scope.showSelector(); }
          });
        }
        if ((field.create === undefined || (field.create && !term))
            && scope.canNew() && scope.hasPermission("create")) {
          items.push({
            label: _t("Create..."),
            click: function() { scope.showPopupEditor(); }
          });
        }
        response(items);
      });
    };

    scope.matchValues = function(a, b) {
      if (a === b) return true;
      if (!a) return false;
      if (!b) return false;
      return a.id === b.id;
    };

    var _setValue = scope.setValue;
    scope.setValue = function(value, fireOnChange) {
      var items = _.map(value, function(item) {
        if (item.version === undefined) {
          return item;
        }
        var ver = item.version;
        var val = _.omit(item, "version");
        val.$version = ver;
        if (val.$id === undefined) {
          delete val.$id;
        }
        return val;
      });
      items = _.isEmpty(items) ? null : items;
      return _setValue.call(this, items, fireOnChange);
    };

    var _handleSelect = scope.handleSelect;
    scope.handleSelect = function(e, ui) {
      if (ui.item.click) {
        setTimeout(function(){
          input.val("");
        });
        ui.item.click.call(scope);
        return scope.$applyAsync();
      }
      return _handleSelect.apply(this, arguments);
    };

    var _removeItem = scope.removeItem;
    scope.removeItem = function(e, ui) {
      if (scope.attr('canRemove') === false) return;
      _removeItem.apply(this, arguments);
    };

    if (scope.field && scope.field['tag-edit']) {
      scope.attachTagEditor(scope, element, attrs);
    }
  }
});

ui.InlineOneToManyCtrl = InlineOneToManyCtrl;
ui.InlineOneToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function InlineOneToManyCtrl($scope, $element, DataSource, ViewService) {

  var field = $scope.field || $scope.getViewDef($element);

  $scope.$on("ds:saved", function (e, scope) {
    var updatedItems = ((scope._data || [])[(scope._page || {}).index] || {})[field.name] || [];
    for (var i = 0; i < updatedItems.length; ++i) {
      var updatedItem = updatedItems[i] || {};
      var item = $scope.items[i] || {};
      if (!item.id || item.id < 0 && updatedItem.id > 0) {
        item.id = updatedItem.id;
      }
    }
  });

  var params = {
    model: field.target
  };

  if (field.editor) {
    params.views = [{
      type: 'grid',
      items: field.editor.items
    }];
  }

  $scope._viewParams = params;

  OneToManyCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
    $scope.editorCanSave = false;
    $scope.selectEnable = false;
  });
}

// used in panel form
ui.formInput('InlineOneToMany', 'OneToMany', {

  showTitle: true,

  controller: InlineOneToManyCtrl,

  link: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    scope.onGridInit = function() {};
    scope.items = [];

    var showOnNew = (scope.field.editor||{}).showOnNew !== false;
    var unwatch = null;

    function canAdd() {
      return scope.hasPermission("write") && !scope.isDisabled() && scope.canNew();
    }

    model.$render = function () {
      if (unwatch) {
        unwatch();
        unwatch = null;
      }
      scope.items = model.$viewValue;
      if ((scope.items && scope.items.length > 0) || scope.$$readonly) {
        return;
      }
      scope.items = showOnNew && canAdd() ? [{}] : [];
      unwatch = scope.$watch('items[0]', function o2mFirstItemWatch(item, old) {
        if (!item) return;
        if (item.$changed) {
          unwatch();
          model.$setViewValue(scope.items);
        }
        if (!scope._dataSource.equals(item, old)) {
          item.$changed = true;
        }
      }, true);
    };

    function isEmpty(record) {
      if (!record || _.isEmpty(record)) return true;
      var values = _.filter(record, function (value, name) {
        return !(/[\$_]/.test(name) || value === null || value === undefined);
      });
      return values.length === 0;
    }

    function itemsChanged() {
      var items = scope.items;
      if (items && items.length > 0) {
        var changed = false;
        var values = _.filter(items, function (item) {
          if (item.$changed) {
            changed = true;
          }
          return !isEmpty(item);
        });
        if (changed && values.length) {
          model.$setViewValue(values);
        }
      }
    }

    scope.$watch('items', itemsChanged, true);
    scope.$itemsChanged = itemsChanged;

    scope.$watch('$$readonly', function o2mReadonlyWatch(readonly, old) {
      if (readonly === undefined) return;
      var items = model.$viewValue;
      if (_.isEmpty(items)) {
        scope.items = (showOnNew && canAdd() && !readonly) ? [{}] : items;
      }
    });

    scope.$copy = function (record) {
      return angular.copy(record);
    };

    scope.addItem = function () {
      var items = scope.items || (scope.items = []);
      var item = _.last(items);
      if (items && items.length && isEmpty(item)) {
        return;
      }
      if (canAdd()) {
        items.push(showOnNew && !items.length ? {} : { $changed: true });
      }
    };

    scope.removeItem = function (index) {
      var items = scope.items;
      items.splice(index, 1);
      var values = _.filter(items, function (item) {
        return !isEmpty(item);
      });
      if (items.length === 0 && showOnNew) {
        scope.addItem();
      }
      model.$setViewValue(values);
    };

    scope.canRemove = function () {
      return scope.attr('canRemove') !== false;
    };

    scope.setValidity = function (key, value, record) {
      if (arguments.length === 3) {
        record.$valid = value;
        value = _.all(scope.items, function (x) { return x.$valid; });
      }
      model.$setValidity(key, value);
    };

    scope.setExclusive = function (name, record) {
      _.each(scope.items, function (item) {
        if (record !== item) {
          item[name] = false;
        }
      });
    };
  },

  template_readonly:function (scope) {
    var field = scope.field;
    var tmpl = (field.viewer || {}).template;
    if (!tmpl && field.editor && (field.editor.viewer || !field.targetName)) {
      tmpl = '<div class="o2m-editor-form" ui-panel-editor></div>';
    }
    if (!tmpl && field.targetName) {
      tmpl = '{{record.' + field.targetName + '}}';
    }
    tmpl = tmpl || '{{record.id}}';
    return "<div class='o2m-list'>" +
    "<div class='o2m-list-row' ng-class-even=\"'even'\" ng-repeat='record in items' ng-init='$$original = $copy(record)'>" + tmpl + "</div>" +
    "</div>";
  },

  template_editable: function (scope) {
    return "<div class='o2m-list'>" +
      "<div class='o2m-list-row' ng-class-even=\"'even'\" ng-repeat='record in items' ng-init='$$original = $copy(record)'>" +
        "<div class='o2m-editor-form' ui-panel-editor></div>" +
        "<span class='o2m-list-remove' ng-show='hasPermission(\"remove\") && !isDisabled() && canRemove()'>" +
          "<a tabindex='-1' href='' ng-click='removeItem($index)' title='{{\"Remove\" | t}}'><i class='fa fa-times'></i></a>" +
        "</span>" +
      "</div>" +
      "<div class='o2m-list-row o2m-list-add' ng-show='hasPermission(\"create\") && !isDisabled() && canNew()'>" +
        "<a tabindex='-1' href='' ng-click='addItem()'  title='{{\"Add\" | t}}'><i class='fa fa-plus'></i></a>" +
      "</div>" +
    "</div>";
  },
  template: null
});

//used in panel form
ui.formInput('InlineManyToMany', 'InlineOneToMany', {

});

// used in editable grid
ui.formInput('OneToManyInline', 'OneToMany', {

  css	: 'one2many-inline',

  collapseIfEmpty : false,

  link: function(scope, element, attrs, model) {

    this._super.apply(this, arguments);

    scope.onSort = function() {

    };

    var field = scope.field;
    var picker = element.children('.picker-input');
    var input = picker.children('input');
    var grid = picker.children('[ui-slick-grid]');

    var container = null;
    var wrapper = $('<div class="slick-editor-dropdown"></div>')
      .css("position", "absolute")
      .hide();

    var render = model.$render,
      renderPending = false;
    model.$render = function() {
      if (wrapper.is(":visible")) {
        renderPending = false;
        render();
        grid.trigger('adjust:size');
      } else {
        renderPending = true;
      }
    };

    scope.waitForActions(function(){
      container = element.parents('.ui-dialog-content,.view-container').first();
      grid.height(175).appendTo(wrapper);
      wrapper.height(175).appendTo(container);
    });

    function adjust() {
      if (!wrapper.is(":visible"))
        return;
      if (axelor.device.small) {
        dropdownVisible = false;
        return wrapper.hide();
      }
      wrapper.position({
        my: "left top",
        at: "left bottom",
        of: picker,
        within: container
      })
      .zIndex(element.zIndex() + 1)
      .width(element.width());
    }

    var dropdownVisible = false;
    scope.onDropdown = function () {
      dropdownVisible = !dropdownVisible;
      if (!dropdownVisible) {
        wrapper.hide();
        return;
      }
      if (renderPending) {
        renderPending = false;
        render();
        setTimeout(function () {
          axelor.$adjustSize();
        });
      }
      wrapper.show();
      adjust();
    };

    scope.canDropdown = function () {
      return !axelor.device.small;
    };

    scope.canShowAdd = function () {
      return dropdownVisible && scope.canEdit();
    };

    scope.canShowRemove = function () {
      return dropdownVisible && scope.canRemove() && !_.isEmpty(scope.selection);
    };

    element.on("hide:slick-editor", function(e){
      dropdownVisible = false;
      wrapper.hide();
    });

    scope.$onAdjust(adjust, 300);

    input.on('keydown', function (e) {
      if (e.keyCode === 40 && e.ctrlKey && !dropdownVisible) {
        scope.onDropdown();
      }
    });

    input.on('keydown', function (e) {
      if (e.keyCode === 9) { // tab key
        element.trigger('hide:slick-editor');
      }
    });

    function hidePopup(e) {
      if (element.is(':hidden')) {
        return;
      }
      var all = element.add(wrapper);
      var elem = $(e.target);
      if (all.is(elem) || all.has(elem).length > 0) return;
      if (elem.zIndex() > element.parents('.slick-form:first,.slickgrid:first').zIndex()) return;
      if (elem.parents(".ui-dialog:first").zIndex() > element.parents('.slickgrid:first').zIndex()) return;

      element.trigger('hide:slick-editor');
    }

    $(document).on('mousedown.mini-grid', hidePopup);

    scope.$watch(attrs.ngModel, function o2mModelWatch(value) {
      var text = "";
      if (value && value.length)
        text = "(" + value.length + ")";
      input.val(text);
    });

    scope.$watch('schema.loaded', function o2mSchemaWatch(viewLoaded) {
      var schema = scope.schema;
      if (schema && scope.attr('canEdit') === false) {
        schema.editIcon = false;
      }
    });

    scope.$on("$destroy", function(e){
      wrapper.remove();
      $(document).off('mousedown.mini-grid', hidePopup);
    });

    scope.canEdit = function () {
      return scope.hasPermission('create') && !scope.isReadonly() && scope.attr('canEdit') !== false;
    };

    scope.canRemove = function() {
      return scope.hasPermission('create') && !scope.isReadonly() && scope.attr('canEdit') !== false;
    };
  },

  template_editable: null,

  template_readonly: null,

  template:
  '<span class="form-item-container">'+
  '<span class="picker-input picker-icons-2">'+
    '<input type="text" readonly>'+
    '<span class="picker-icons">'+
      '<i class="fa fa-plus" ng-click="onSelect()" ng-show="canShowAdd()" title="{{\'Select\' | t}}"></i>'+
      '<i class="fa fa-minus" ng-click="onRemove()" ng-show="canShowRemove()" title="{{\'Select\' | t}}"></i>'+
      '<i class="fa fa-caret-down" ng-show="canDropdown()" ng-click="onDropdown()" title="{{\'Show\' | t}}"></i>'+
    '</span>'+
    '<div ui-view-grid ' +
      'x-view="schema" '+
      'x-data-view="dataView" '+
      'x-handler="this" '+
      'x-no-filter="true" '+
      'x-on-init="onGridInit" '+
      'x-on-before-save="onGridBeforeSave" '+
      'x-on-after-save="onGridAfterSave" '+
      '></div>'+
  '</span>'+
  '</span>'
});

ui.formInput('ManyToManyInline', 'OneToManyInline', {

  css	: 'many2many-inline',

  controller: ManyToManyCtrl,

  link: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);
  }
});

})();
