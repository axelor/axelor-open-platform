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
(function () {

"use strict";

var ui = angular.module("axelor.ui");

EditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService', '$q'];

function EditorCtrl($scope, $element, DataSource, ViewService, $q) {

  var parent = $scope.$parent;

  $scope._viewParams = parent._viewParams;
  $scope.editorCanSave = parent.editorCanSave;
  $scope.editorCanReload = parent.editorCanReload;

  ui.ViewCtrl.call(this, $scope, DataSource, ViewService);
  ui.FormViewCtrl.call(this, $scope, $element);

  var closeCallback = null;
  var originalEdit = $scope.edit;
  var originalShow = $scope.show;

  var recordVersion = -1;
  var canClose = false;
  var isClosed = true;

  $scope.show = function(record, callback) {
    originalShow();
    if (_.isFunction(record)) {
      callback = record;
      record = null;
    }
    closeCallback = callback;
    isClosed = false;
    recordVersion = record ? record.version : -1;
    if (recordVersion === undefined && record) {
      recordVersion = record.$version;
    }
    this.edit(record);
  };

  function doEdit(record, fireOnLoad) {
    if (record && record.id > 0 && (!_.isNumber(record.version) || !record.$fetched)) {
      $scope.doRead(record.id).success(function(rec) {
        if (record.$dirty) {
          rec = _.extend({}, rec, record);
        }
        originalEdit(rec, fireOnLoad);
      });
    } else {
      originalEdit(record, fireOnLoad);
    }
    canClose = false;
  }

  var parentCanEditTarget = null;

  $scope.canEditTarget =  function () {
    if (parentCanEditTarget === null) {
      var parent = $scope.$parent;
      var func = parent.canEditTarget;
      while (parent && func === $scope.canEditTarget) {
        parent = parent.$parent;
        func = parent.canEditTarget;
      }
      parentCanEditTarget = func || angular.noop;
    }
    return parentCanEditTarget() !== false;
  };

  var isEditable = $scope.isEditable;
  $scope.isEditable = function () {
    var id = ($scope.record || {}).id;
    var perm = id > 0 ? 'write' : 'create';
    if (parent.isReadonly && parent.isReadonly()) return false;
    return $scope.hasPermission(perm)
      && (id > 0 ? $scope.canEditTarget() : true)
      && isEditable.call($scope);
  };

  var canEdit = $scope.canEdit;
  $scope.canEdit = function() {
    return $scope.canEditTarget() && canEdit.call($scope);
  };

  $scope.edit = function(record, fireOnLoad) {
    if (isClosed) return;
    $scope._viewPromise.then(function(){
      doEdit(record, fireOnLoad);
      $scope.setEditable(!$scope.$parent.$$readonly);
    });
  };

  function isChanged() {
    if ($scope.isDirty()) return true;
    var record = $scope.record || {};
    var version = record.version;
    return recordVersion !== version || record.$forceDirty;
  }

  function canOK() {
    if (isClosed) return false;
    return isChanged();
  }

  function onOK() {

    var record = $scope.record;

    function close(value, forceSelect) {
      if (value && (forceSelect || canOK())) {
        value.$fetched = true;
        value.selected = true;

        // add missing values
        _.chain(Object.keys(record))
          .filter(function(name) {
            return !_.startsWith(name, '$') && _.isObject(record[name]);
          })
          .each(function(name) {
            _.chain(Object.keys(record[name]))
              .filter(function(subName) {
                return !_.startsWith(subName, '$')
                  && subName !== 'version'
                  && _.isObject(value[name])
                  && value[name][subName] === undefined;
              })
              .each(function(subName) {
                value[name][subName] = record[name][subName];
              });
          });
        _.chain(value)
          .filter(function(val) { return val && val.$updatedValues; })
          .each(function(val) { _.extend(val, val.$updatedValues); });

        $scope.$parent.select(value);
      }
      canClose = true;
      $element.dialog('close');
      if ($scope.editorCanReload) {
        $scope.$parent.parentReload();
      }
      if (closeCallback && value) {
        closeCallback(value);
      }
      closeCallback = null;
      isClosed = true;
    }

    var event = $scope.$broadcast('on:before-save', record);
    if (event.defaultPrevented) {
      if (event.error) {
        axelor.dialogs.error(event.error);
      }
      return;
     }

    // wait for onChange actions
    $scope.waitForActions(function() {
      if ($scope.editorCanSave && isChanged()) {
        var recordId = record.id;
        if (record.id < 0)
          record.id = null;
        return $scope.onSave({force: true}).then(function(record, page) {
          // wait for onSave actions
          $scope.waitForActions(function(){
            record.$id = recordId;
            close(record, true);
          });
        });
      }
      if ($scope.isValid()) {
        close(record);
      } else if ($scope.showErrorNotice) {
        $scope.showErrorNotice();
      }
    }, 100);
  }

  $scope.onOK = function() {
    $scope.$timeout(onOK, 10);
  };

  $scope.onBeforeClose = function(event, ui) {

    if (canClose || !$scope.isDirty()) {
      $scope.$evalAsync(function () {
        $scope.edit(null, false);
      });
      return true;
    }

    event.preventDefault();

    $scope.confirmDirty(function(){
      canClose = true;
      $element.dialog('close');
    });
  };

  $scope.onHotKey = function (e, action) {

    if (action === "save") {
      $(e.target).blur().focus();
      $scope.onOK();
    }

    $scope.$applyAsync();

    return false;
  };
}

SelectorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function SelectorCtrl($scope, $element, DataSource, ViewService) {

  var parent = $scope.$parent;

  $scope._viewParams = parent._viewParams;
  $scope.getDomain = parent.getDomain;

  ui.ViewCtrl.call(this, $scope, DataSource, ViewService);
  ui.GridViewCtrl.call(this, $scope, $element);

  var searchLimit = (parent.field||{}).searchLimit || 0;
  if (searchLimit > 0) {
    $scope._dataSource._page.limit = searchLimit;
  }

  function doFilter() {
    $scope.filter($scope.getDomain());
  }

  var initialized = false;
  var origShow = $scope.show;
  $scope.show = function() {
    origShow();
    if (initialized) {
      doFilter();
    }
  };

  var _getContext = $scope.getContext;
  $scope.getContext = function() {
    // selector popup should return parent's context
    if ($scope.$parent && $scope.$parent.getContext) {
      return $scope.$parent.getContext();
    }
    return _getContext();
  };

  $scope.onItemClick = function(e, args) {
    $scope.$applyAsync($scope.onOK.bind($scope));
  };

  var origOnShow = $scope.onShow;
  $scope.onShow = function(viewPromise) {

    viewPromise.then(function(){
      var view = $scope.schema;
      var field = $scope.field || $scope.$parent.field;
      if (field) {
        view.orderBy = field.orderBy || view.orderBy;
      }
      $element.dialog('open');
      initialized = true;
      origOnShow(viewPromise);
    });
  };

  $scope.onOK = function() {

    var selection = _.map($scope.selection, function(index){
      return $scope.dataView.getItem(index);
    });

    if (!_.isEmpty(selection)) {
      $scope.$applyAsync(function () {
        $scope.$parent.select(selection);
        $scope.selection = [];
      });
    }

    $element.dialog('close');
  };

  $scope.onCreate = function () {
    $element.dialog('close');
    $scope.$parent.onNew();
  };

  $scope.canNew = function () {
    return $scope.hasPermission('create') && $scope.$parent.canNew();
  };
}

ui.directive('uiDialogSize', function() {

  return function (scope, element, attrs) {

    // use only with dialogs
    if (attrs.uiDialog === undefined && !element.hasClass('ui-dialog-content')) {
      return;
    }

    var loaded = false;
    var addMaximizeButton = _.once(function () {
      var elemDialog = element.parent();
      var elemTitle = elemDialog.find('.ui-dialog-title');
      var elemButton = $('<a href="#" class="ui-dialog-titlebar-max"><i class="fa fa-expand"></i></a>')
        .click(function (e) {
          scope.waitForActions(function () {
            $(this).children('i').toggleClass('fa-expand fa-compress');
            elemDialog.toggleClass('maximized');
            axelor.$adjustSize();
            setTimeout(function () {
              scope.$broadcast('grid:adjust-columns');
            }, 350);
          });
          return false;
      }).insertAfter(elemTitle);

      // remove maximized state on close
      element.on('dialogclose', function(e, ui) {
        elemTitle.parent().find('i.fa-compress').toggleClass('fa-expand fa-compress');
        elemDialog.removeClass('maximized');
      });
    });
    var addCollapseButton = _.once(function () {
      var elemDialog = element.parent();
      var elemTitle = elemDialog.find('.ui-dialog-title');
      $('<a href="#" class="ui-dialog-titlebar-collapse"><i class="fa fa-chevron-up"></i></a>')
      .click(function (e) {
        $(this).children('i').toggleClass('fa-chevron-up fa-chevron-down');
        elemDialog.toggleClass('collapsed');
        axelor.$adjustSize();
        return false;
      }).insertAfter(elemTitle);

      // remove maximized and collapsed states on close
      element.on('dialogclose', function(e, ui) {
        elemTitle.parent().find('i.fa-compress').toggleClass('fa-expand fa-compress');
        elemTitle.parent().find('i.fa-chevron-down').toggleClass('fa-chevron-down fa-chevron-up');
        elemDialog.removeClass('maximized collapsed');
      });
    });

    function doMaximize() {
      var field = scope.$parent.field || {};
      var params = (scope._viewParams || {}).params || {};

      var elemDialog = element.parent();
      var elemButton = elemDialog.find('.ui-dialog-titlebar-max');

      var maximize = params['popup.maximized']
        || field.popupMaximized === "all"
        || (field.popupMaximized === "editor" && element.is('[ui-editor-popup]'))
        || (field.popupMaximized === "selector" && element.is('[ui-selector-popup]'));

      if (maximize) {
        elemButton.click();
      }
    }

    function doAdjust() {
      element.dialog('open');
      element.scrollTop(0);
      setTimeout(doMaximize);
      setTimeout(doFocus);
      if (scope._afterPopupShow) {
        scope._afterPopupShow();
      }
    }

    function doShow() {
      addMaximizeButton();
      addCollapseButton();
      if (loaded) {
        return setTimeout(doAdjust);
      }
      loaded = true;
      scope.waitForActions(doAdjust);
    }

    function doFocus() {
      var container =  element.is('[ui-selector-popup]')
        ? element.find('.slick-headerrow')
        : element;
      var focusElem = container.find('input:tabbable');
      if (focusElem.length == 0) {
        focusElem = element.parent().find('.ui-dialog-buttonset').find(':tabbable');
      }
      if (focusElem[0]) {
        focusElem[0].focus();
      }

      //XXX: ui-dialog issue
      element.find('.slick-headerrow-column,.slickgrid,[ui-embedded-editor]').zIndex(element.zIndex());
      element.find('.record-toolbar .btn, .dropdown').zIndex(element.zIndex()+1);
    }

    // a flag used by evalScope to detect popup (see form.base.js)
    scope._isPopup = true;
    scope._doShow = function(viewPromise) {
      if (viewPromise && viewPromise.then) {
        viewPromise.then(doShow);
      } else {
        doShow();
      }
    };

    scope._setTitle = function (title) {
      if (title) {
        element.closest('.ui-dialog').find('.ui-dialog-title').text(title);
      }
    };

    scope.adjustSize = function() {
    };
  };
});

ui.directive('uiEditorPopup', function() {

  return {
    restrict: 'EA',
    controller: EditorCtrl,
    scope: {},
    link: function(scope, element, attrs) {

      scope.onShow = function(viewPromise) {
        scope._doShow(viewPromise);
      };

      scope.$watch('schema.title', function popupTitleWatch(title) {
        scope._setTitle(title);
      });

      element.scroll(function (e) {
        $(document).trigger('adjust:scroll', element);
      });

      var onNewHandler = scope.onNewHandler;
      scope.onNewHandler = function (event) {
        if (scope.isPopupOpen) {
          return onNewHandler.apply(scope, arguments);
        }
      };

      scope.isPopupOpen = true;
      setTimeout(function () {
        var isOpen = false;
        element.on('dialogclose', function (e) {
          isOpen = false;
          scope.waitForActions(function () {
            scope.isPopupOpen = isOpen;
            scope.$$popupStack.pop(1);
          }, 2000); // delay couple of seconds to that popup can cleanup
        });
        element.on('dialogopen', function (e) {
          scope.isPopupOpen = isOpen = true;
          scope.$$popupStack.push(1);
          scope.$applyAsync();
        });
      });
    },
    replace: true,
    template:
    '<div ui-dialog ui-dialog-size x-on-ok="onOK" x-on-before-close="onBeforeClose" ui-watch-if="isPopupOpen">'+
        '<div ui-view-form x-handler="this"></div>'+
    '</div>'
  };
});

ui.directive('uiSelectorPopup', function(){

  return {
    restrict: 'EA',
    controller: SelectorCtrl,
    scope: {
      selectMode: "@"
    },
    link: function(scope, element, attrs) {

      var onShow = scope.onShow;
      scope.onShow = function (viewPromise) {
        if (scope.clearFilters) {
          scope.clearFilters();
          scope.selection = [];
        }
        onShow(viewPromise);
        scope._doShow(viewPromise);
      };

      scope.$watch('schema.title', function popupTitleWatch(title){
        scope._setTitle(title);
      });

      var btnOK = null;

      function buttonState(count) {
        if (btnOK === null) {
          btnOK = element.siblings('.ui-dialog-buttonpane').find('.btn:last');
        }
        return btnOK.attr('disabled', !count || count <= 0);
      }

      scope.$watch('selection.length', buttonState);

      setTimeout(function(){
        var footer = element.closest('.ui-dialog').find('.ui-dialog-buttonpane'),
          header = element.closest('.ui-dialog').find('.ui-dialog-titlebar'),
          pager = element.find('.record-pager'),
          buttons = element.find('.ui-dialog-buttonset-left');
        header.find('.ui-dialog-title').after(pager);
        footer.prepend(buttons);
        footer.find('.button-ok').html(_t("Select"));
      });
    },
    replace: true,
    template:
    '<div ui-dialog ui-dialog-size x-on-ok="onOK">'+
        '<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-editable="false" x-selector="{{selectMode}}"></div>'+
        '<div ui-record-pager></div>'+
        '<div class="ui-dialog-buttonset-left pull-left" ng-show="canNew()">'+
          '<button class="btn" ng-click="onCreate()" x-translate>Create</button>'+
        '</div>'+
    '</div>'
  };
});

})();
