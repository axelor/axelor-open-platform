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

var ui = angular.module("axelor.ui");

var NestedForm = {
  scope: true,
  controller: [ '$scope', '$element', function($scope, $element) {

    ui.FormViewCtrl.call(this, $scope, $element);

    $scope.onShow = function(viewPromise) {

    };

    $scope.$$forceWatch = false;
    $scope.$$forceCounter = false;

    $scope.$setForceWatch = function () {
      $scope.$$forceWatch = true;
      $scope.$$forceCounter = true;
    };

    $scope.registerNested($scope);
    $scope.show();
  }],
  link: function(scope, element, attrs, ctrl) {

  },
  template: '<div ui-view-form x-handler="this"></div>'
};

ui.EmbeddedEditorCtrl = EmbeddedEditorCtrl;
ui.EmbeddedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function EmbeddedEditorCtrl($scope, $element, DataSource, ViewService) {

  var params = angular.copy($scope._viewParams);

  params.views = _.compact([params.summaryView || params.summaryViewDefault]);
  $scope._viewParams = params;

  ui.ViewCtrl($scope, DataSource, ViewService);
  ui.FormViewCtrl.call(this, $scope, $element);

  $scope.visible = false;
  $scope.onShow = function() {

  };

  var originalEdit = $scope.edit;

  function doEdit(record) {
    if (record && record.id > 0 && !record.$fetched) {
      $scope.doRead(record.id).success(function(record){
        originalEdit(record);
      });
    } else {
      originalEdit(record);
    }
  }

  function doClose() {
    if ($scope.isDetailView) {
      $scope.edit($scope.getSelectedRecord());
      return;
    }
    $scope.edit(null);
    $scope.waitForActions(function () {
      $scope.visible = false;
      $element.hide();
      $element.data('$rel').show();
    });
  }

  $scope.edit = function(record) {
    doEdit(record);
    $scope.setEditable(!$scope.$parent.$$readonly);
  };

  $scope.onClose = function() {
    $scope.onClear();
    doClose();
  };

  $scope.onOK = function() {
    if (!$scope.isValid()) {
      return;
    }
    var record = $scope.record;
    if (record) record.$fetched = true;

    var event = $scope.$broadcast('on:before-save', record);
    if (event.defaultPrevented) {
      if (event.error) {
        return axelor.dialogs.error(event.error);
      }
    }
    $scope.waitForActions(function () {
      $scope.select($scope.record);
      $scope.waitForActions(doClose);
    });
  };

  $scope.onAdd = function() {
    if (!$scope.isValid() || !$scope.record) {
      return;
    }

    var record = $scope.record;
    record.id = null;
    record.version = null;
    record.$version = null;

    $scope.onClear();

    function doSelect(rec) {
      if (rec) {
        $scope.select(rec);
      }
      return doEdit(rec);
    }

    if (!$scope.editorCanSave) {
      return doSelect(record);
    }

    $scope.onSave().then(function (rec) {
      doSelect(rec);
    });
  };

  $scope.onClear = function() {
    if ($scope.$parent.selection) {
      $scope.$parent.selection.length = 0;
    }
    doEdit(null);
  };

  $scope.canUpdate = function () {
    return $scope.record && $scope.record.id;
  };

  function loadSelected() {
    var record = $scope.getSelectedRecord();
    if ($scope.isDetailView) {
      $scope.edit(record);
    }
  }

  $scope.$on('grid:changed', function(event) {
    loadSelected();
  });

  $scope.$on('on:edit', function(event, record) {
    if ($scope.$parent.record === record) {
      $scope.waitForActions(loadSelected);
    }
  });

  $scope.$parent.$watch('isReadonly()', function nestedReadonlyWatch(readonly, old) {
    if (readonly === old) return;
    $scope.setEditable(!readonly);
  });

  $scope.show();
}

var EmbeddedEditor = {
  restrict: 'EA',
  css: 'nested-editor',
  scope: true,
  controller: EmbeddedEditorCtrl,
  link: function (scope, element, attrs) {
    setTimeout(function () {
      var prev = element.prev();
      if (prev.is("[ui-slick-grid]")) {
        element.zIndex(prev.zIndex() + 1);
      }
    });
  },
  template:
    '<fieldset class="form-item-group bordered-box" ui-show="visible">'+
      '<div ui-view-form x-handler="this"></div>'+
      '<div class="btn-toolbar pull-right">'+
        '<button type="button" class="btn btn btn-info" ng-click="onClose()" ng-show="isReadonly()"><span x-translate>Back</span></button> '+
        '<button type="button" class="btn btn-danger" ng-click="onClose()" ng-show="!isReadonly()"><span x-translate>Cancel</span></button> '+
        '<button type="button" class="btn btn-primary" ng-click="onAdd()" ng-show="!isReadonly() && !canUpdate()"><span x-translate>Add</span></button> '+
        '<button type="button" class="btn btn-primary" ng-click="onOK()" ng-show="!isReadonly() && canUpdate()"><span x-translate>OK</span></button>'+
      '</div>'+
    '</fieldset>'
};

ui.NestedEditorCtrl = NestedEditorCtrl;
ui.NestedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function NestedEditorCtrl($scope, $element, DataSource, ViewService) {

  var params = angular.copy($scope._viewParams);

  params.views = _.compact([params.summaryView || params.summaryViewDefault]);
  $scope._viewParams = params;

  ui.ManyToOneCtrl.call(this, $scope, $element, DataSource, ViewService);

  $scope.nested = null;
  $scope.registerNested = function(scope) {
    $scope.nested = scope;

    $scope.$watch("isReadonly()", function nestedReadonlyWatch(readonly) {
      scope.setEditable(!readonly);
    });
  };
}

var NestedEditor = {
  restrict: 'EA',
  css: 'nested-editor',
  require: '?ngModel',
  scope: true,
  controller: NestedEditorCtrl,
  link: function(scope, element, attrs, model) {

    function setValidity(nested, valid) {
      model.$setValidity('valid', nested.isValid());
      if (scope.setValidity) {
        scope.setValidity('valid', nested.isValid());
      }
    }

    var configure = _.once(function (nested) {

      //FIX: select on M2O doesn't apply to nested editor
      var unwatchId = scope.$watch(attrs.ngModel + '.id', function nestedRecordIdWatch(id, old){
        if (id === old) {
          return;
        }
        unwatchId();
        unwatchId = null;
        scope.$applyAsync();
      });

      var unwatchValid = nested.$watch('form.$valid', function nestedValidWatch(valid, old){
        if (valid === old) {
          return;
        }
        unwatchValid();
        unwatchValid = null;
        setValidity(nested, valid);
      });

      scope.$on("on:check-nested-values", function (e, value) {
        if (nested && value) {
          var val = scope.getValue() || {};
          if (val.$updatedValues === value) {
            _.extend(nested.record, value);
          }
        }
      });

      var parentAttrs = scope.$parent.field || {};
      if (parentAttrs.forceWatch) {
        nested.$$forceWatch = true;
      }
    });

    var unwatch = null;
    var original = null;

    function nestedEdit(record, fireOnLoad) {

      var nested = scope.nested;
      var counter = 0;

      if (!nested) return;
      if (unwatch) unwatch();

      original = angular.copy(record);

      unwatch = nested.$watch('record', function nestedRecordWatch(rec, old) {

        if (counter++ === 0 && !nested.$$forceCounter) {
          return;
        }

        var ds = nested._dataSource;
        var name = scope.field.name;

        // don't process default values
        if (ds.equals(rec, nested.defaultValues)) {
          return;
        }

        if (_.isEmpty(rec)) rec = null;
        if (_.isEmpty(old)) old = null;
        if (rec == old) {
          return;
        }
        if (rec) {
          rec.$dirty = !(rec.id > 0 && ds.equals(rec, original));
        }

        model.$setViewValue(rec);
        setValidity(nested, nested.isValid());
      }, true);

      return nested.edit(record, fireOnLoad);
    }

    scope.ngModel = model;
    scope.visible = false;

    scope.onClear = function() {
      scope.$parent.setValue(null, true);
      scope.$parent.$broadcast('on:new');
    };

    scope.onClose = function() {
      scope.$parent._isNestedOpen = false;
      scope.visible = false;
      element.hide();
    };

    scope.canClose = function() {
      return scope.canToggle() && scope.canSelect();
    };

    attrs.$observe('title', function(title){
      scope.title = title;
    });

    model.$render = function() {
      var nested = scope.nested,
        promise = nested._viewPromise,
        oldValue = model.$viewValue;

      function doRender() {
        var value = model.$viewValue;
        if (oldValue !== value) { // prevent unnecessary onLoad
          return;
        }
        if (!value || !value.id || value.$dirty) {
          return nestedEdit(value, false);
        }
        if (value.$fetched && (nested.record||{}).$fetched) return;
        return nested.doRead(value.id).success(function(record){
          record.$fetched = true;
          value.$fetched = true;
          return nestedEdit(_.extend({}, value, record));
        });
      }

      if (nested == null) {
        return;
      }

      promise.then(function() {
        configure(nested);
        nestedEdit(model.$viewValue, false);
        scope.waitForActions(doRender, 100);
      });
    };
  },
  template:
  '<fieldset class="form-item-group bordered-box" ui-show="visible">'+
    '<legend>'+
      '<span ng-bind-html="title"></span> '+
      '<span class="legend-toolbar" style="display: none;" ng-show="!isReadonly()">'+
        '<a href="" tabindex="-1" ng-click="onClear()" title="{{\'Clear\' | t}}" ng-show="canShowIcon(\'clear\')"><i class="fa fa-ban"></i></a> '+
        '<a href="" tabindex="-1" ng-click="onSelect()" title="{{\'Select\' | t}}" ng-show="canShowIcon(\'select\')"><i class="fa fa-search"></i></a> '+
        '<a href="" tabindex="-1" ng-click="onClose()" title="{{\'Close\' | t}}" ng-show="canClose()"><i class="fa fa-times-circle"></i></a>'+
      '</span>'+
    '</legend>'+
    '<div ui-nested-form></div>'+
  '</fieldset>'
};

ui.formDirective('uiNestedEditor', NestedEditor);
ui.formDirective('uiEmbeddedEditor', EmbeddedEditor);
ui.formDirective('uiNestedForm', NestedForm);

})();
