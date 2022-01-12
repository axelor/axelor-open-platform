/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
(function() {

"use strict";

var ui = angular.module('axelor.ui');
var ds = angular.module('axelor.ds');

var equals = ds.equals,
  forEach = angular.forEach,
  isArray = angular.isArray,
  isObject = angular.isObject,
  isDate = angular.isDate;

function dummyEquals(a, b) {
  if (a === b) return true;
  if (a === null || b === null) return false;
  if (a !== a && b !== b) return true; // NaN === NaN
  var keys = _.keys(a).filter(function (k) { return k.indexOf('$') === 0; });
  if (keys.length === 0) {
    return true;
  }
  for (var i = 0; i < keys.length; i++) {
    var k = keys[i];
    if (!equals(a[k], b[k])) {
      return false;
    }
  }
  return true;
}

function updateValues(source, target, itemScope, formScope) {

  if (equals(source, target) && dummyEquals(source, target) && (!source || !source.$force)) {
    return;
  }

  // handle json records
  if (source && formScope && formScope._model === 'com.axelor.meta.db.MetaJsonRecord') {
    if (source.attrs || source.id) {
      source = source.id > 0
        ? _.pick(source, 'jsonModel', 'name', 'attrs', 'id', 'version')
        : _.pick(source, 'jsonModel', 'name', 'attrs');
    }

    var values = source.attrs ? _.extend({}, JSON.parse(source.attrs)) : source;
    var fix = function (rec) {
      if (!rec) return rec;
      if (_.isArray(rec)) return _.map(rec, fix);
      if (rec.id > 0 && (rec.version || rec.attrs)) {
        rec = _.pick(rec, 'id', 'name', 'selected');
        if (!rec.selected) delete rec.selected;
      }
      return rec;
    };

    _.each(values, function (v, k) {
      values[k] = fix(v);
    });

    // if called from form fields
    if (itemScope && itemScope.updateJsonValues) {
      return itemScope.updateJsonValues(values);
    }

    // onNew or onSave from main form
    var current = target && target.attrs ? JSON.parse(target.attrs) : {};
    if (source.attrs || !source.jsonModel) {
      source.attrs = JSON.stringify(_.extend({}, current, values));
    }

  } else if (itemScope && itemScope.updateJsonValues) {
    return itemScope.updateJsonValues(source);
  }

  function compact(value) {
    if (!value) return value;
    if (value.version === undefined) return value;
    if (!value.id) return value;
    var res = _.extend(value);
    res.$version = res.version;
    res.version = undefined;
    return res;
  }

  var changed = false;
  var selectedChanged = false;

  forEach(source, function(value, key) {
    var dest;
    var newValue = value;
    var oldValue = target[key];
    if (oldValue === newValue) {
      return;
    }
    if (isArray(value)) {
      dest = target[key] || [];
      newValue = _.map(value, function(item) {
        var found = _.find(dest, item.id > 0 ? function (v) {
          return v.id === item.id;
        } : function (v) {
          return equals(v, item);
        });
        if (_.has(item, "version") && item.id) item.$fetched = true;
        if (found) {
          var found_ = _.extend({}, found);
          var changed_ = updateValues(item, found_);
          changed = changed || changed_;
          var result = changed_ ? found_ : found;
          if (item.selected !== result.selected) {
            result.selected = item.selected;
            selectedChanged = true;
          }
          return result;
        } else if (item.selected) {
          selectedChanged = true;
        }
        return item;
      });
    } else if (isObject(value) && !isDate(value)) {
      dest = target[key] || {};
      if (dest.id === value.id) {
        if (_.isNumber(dest.version)) {
          dest = _.extend({}, dest);
          changed = updateValues(value, dest, itemScope, formScope) || changed;
        } else {
          dest.$updatedValues = value;
          if (formScope) {
            formScope.$broadcast('on:check-nested-values', value);
          }
        }
      } else {
        dest = compact(value);
      }
      newValue = dest;
    }

    if (!equals(oldValue, newValue)) {
      changed = true;
      target[key] = newValue;
    } else if (selectedChanged) {
      // Update even if only selected flags have changed
      target[key] = newValue;
    }
  });

  if (target) {
    if (changed) {
      target.$dirty = true;
    }

    if (itemScope && target.id > 0) {
      var item = _.findWhere(itemScope.items, { id: target.id });
      if (item && item.version > target.version) {
        target.version = item.version;
        if (item.$version !== undefined) {
          target.$version = item.$version;
        }
        _.extend(item, target);
      }
    }
  }

  return changed;
}

function handleError(scope, item, message) {

  if (!item) {
    return;
  }

  var ctrl = item.data('$ngModelController');
  if (!ctrl) {
    return;
  }

  if (ctrl.$doReset) {
    ctrl.$doReset();
  }

  if (!message) {
    ctrl.$doReset = null;
    return;
  }

  var e = $('<span class="error"></span>').text(message);
  var p = item.parent('.form-item');

  p.append(e);

  var clear = scope.$on('on:edit', function(){
    ctrl.$doReset();
  });

  function cleanUp(items) {
    var idx = items.indexOf(ctrl.$doReset);
    if (idx > -1) {
      items.splice(idx, 1);
    }
  }

  ctrl.$doReset = function(value) {

    cleanUp(ctrl.$viewChangeListeners);
    cleanUp(ctrl.$formatters);

    ctrl.$setValidity('invalid', true);
    ctrl.$doReset = null;

    e.remove();
    clear();

    return value;
  };

  if (!item.hasClass('readonly')) {
    ctrl.$setValidity('invalid', false);
  }
  ctrl.$viewChangeListeners.push(ctrl.$doReset);
  ctrl.$formatters.push(ctrl.$doReset);
}

function ActionHandler($scope, ViewService, options) {

  if (!options || !options.action)
    throw 'No action provided.';

  this.canSave = options.canSave;
  this.name = options.name;
  this.prompt = options.prompt;
  this.action = options.action;
  this.element = options.element || $();

  this.scope = $scope;
  this.ws = ViewService;
  this.viewType = $scope.viewType;
}

ActionHandler.prototype = {

  constructor: ActionHandler,

  onLoad : function() {
    return this.handle();
  },

  onNew: function() {
    return this.handle();
  },

  onSave: function() {
    var self = this;
    return this._fireBeforeSave().then(function() {
      return self.handle();
    });
  },

  onTabSelect: function(unblocked) {
    return this.onSelect.apply(this, arguments);
  },

  onSelect: function(unblocked) {
    var self = this;
    var blockUI = this._blockUI;
    if (unblocked) {
      this._blockUI = angular.noop;
    }
    function reset() {
      self._blockUI = blockUI;
    }
    var promise = this.handle();
    promise.then(reset, reset);
    return promise;
  },

  onClick: function(event) {
    var self = this;
    var withMoreAttrs = function (promise) {
      var actions = self.action.trim().split(/\s*,\s*/);
      if (actions.indexOf('save') > -1) {
        return promise.then(function() {
          return self._handleAction('com.axelor.meta.web.MetaController:moreAttrs');
        });
      }
      return promise;
    };

    var prompt = this._getPrompt();
    if (prompt) {
      var deferred = this.ws.defer(),
        promise = deferred.promise;
      axelor.dialogs.confirm(prompt, function(confirmed){
        if (confirmed) {
          self._fireBeforeSave().then(function() {
            withMoreAttrs(self.handle()).then(deferred.resolve, deferred.reject);
          });
        } else {
          self.scope.$timeout(deferred.reject);
        }
      }, {
        yesNo: false
      });
      return promise;
    }
    return this._fireBeforeSave().then(function() {
      return withMoreAttrs(self.handle());
    });
  },

  onChange: function(event) {
    return this.handle({ wait: 100 });
  },

  _getPrompt: function () {
    var prompt = this.prompt;
    var itemScope = this.element.scope();
    if (_.isFunction(itemScope.attr) && !this.element.is('[ui-slick-grid]')) {
      prompt = itemScope.attr('prompt') || prompt;
    }
    return _.isString(prompt) ? prompt : null;
  },

  _getContext: function() {
    var scope = this.scope;
    var context = scope.getContext ? scope.getContext() : scope.record;
    var viewParams = scope._viewParams || {};

    if (scope._isEditorScope && scope.handler) {
      viewParams = scope.handler._viewParams || viewParams;
    }

    context = _.extend({}, viewParams.context, context);
    if (context._model === undefined) {
      context._model = scope._model;
    }

    if (viewParams.viewType) context._viewType = viewParams.viewType;
    if (viewParams.views && viewParams.views.length) {
      context._views = _.map(viewParams.views, function (view) {
        if (view.type === context._viewType) context._viewName = view.name;
        return { type: view.type, name: view.name };
      });
    }

    // include button name as _signal (used by workflow engine)
    if (this.element.is("button,.button-item,li.action-item")) {
      context._signal = this.element.attr('name') || this.element.attr('x-name');
    }

    // include field name as source
    var source = this.element.attr('x-field') || this.element.attr('name') || this.element.attr('x-name');
    if (source) {
      context._source = source;
    }

    return context;
  },

  _getRootFormElement: function () {
    var formElement = $(this.element).parents('form[ui-form]:last');
    if (formElement.length === 0) {
      formElement = this._getFormElement();
    }
    return formElement;
  },

  _getFormElement: function () {

    var elem = $(this.element);
    var formElement = elem;

    if (formElement.is('form')) {
      return formElement;
    }

    formElement = elem.data('$editorForm') || elem.parents('form:first');
    if (!formElement || !formElement.get(0)) { // toolbar button
      formElement = this.element.parents('.form-view:first').find('form:first');
    }
    if (formElement.length === 0) {
      formElement = this.element;
    }
    return formElement;
  },

  handle: function(options) {
    var that = this;
    var action = this.action.trim();
    var deferred = this.ws.defer();

    var all = this.scope.$actionPromises || [];
    var pending = all.slice();
    var opts = _.extend({}, options);

    all.push(deferred.promise);

    this.scope.waitForActions(function () {
      var promise = that._handleAction(action);
      function done() {
        setTimeout(function () {
          var i = all.indexOf(deferred.promise);
          if (i > -1) {
            all.splice(i, 1);
          }
        }, 10);
      }
      promise.then(done, done);
      promise.then(deferred.resolve, deferred.reject);
    }, opts.wait || 10, pending);

    return deferred.promise;
  },

  _blockUI: function() {
    // block the entire ui (auto unblocks when actions are complete)
    _.delay(axelor.blockUI, 10);
  },

  _fireBeforeSave: function() {
    var scope = this._getRootFormElement().scope();
    var event = scope.$broadcast('on:before-save', scope.record);
    var deferred = this.ws.defer();

    if (event.defaultPrevented) {
      if (event.error) {
        axelor.dialogs.error(event.error);
      }
      setTimeout(function() {
        deferred.reject(event.error);
      });
    } else {
      scope.$timeout(function() {
        scope.ajaxStop(function() {
          deferred.resolve();
        }, 100);
      }, 50);
    }
    return deferred.promise;
  },

  _checkVersion: function() {
    var self = this;
    var scope = this.scope;
    var deferred = this.ws.defer();

    if (scope.checkVersion) {
      scope.checkVersion(function (verified) {
        if (verified) {
          return deferred.resolve();
        }
        axelor.dialogs.error(
            _t("The record has been updated or delete by another action."));
        deferred.reject();
      });
    } else {
      deferred.resolve();
    }

    return deferred.promise;
  },

  _handleNew: function() {
    var self = this;
    var scope = this.scope;
    var deferred = this.ws.defer();

    if (scope.onNew) {
      return scope.onNew();
    }
    if (scope.editRecord) {
      scope.editRecord(null);
      deferred.resolve();
    } else {
      deferred.reject();
    }

    return deferred.promise;
  },

  _handleSave: function(validateOnly) {
    if (validateOnly) {
      return this.__handleSave(validateOnly);
    }
    var self = this;
    var deferred = this.ws.defer();

    this._checkVersion().then(function () {
      self.__handleSave().then(deferred.resolve, deferred.reject);
    }, deferred.reject);

    return deferred.promise;
  },

  __handleSave: function(validateOnly) {
    var self = this;
    var scope = this.scope;
    var id = (scope.record||{}).id;
    var o2mPopup = scope._isPopup && (scope.$parent.field||{}).serverType === "one-to-many";
    if (o2mPopup && !validateOnly && this.name == 'onLoad' && (!id || id < 0)) {
      var deferred = this.ws.defer();
      var msg = _t("The {0}={1} event can't call 'save' action on unsaved o2m item.", this.name, this.action);
      deferred.reject(msg);
      console.error(msg);
      return deferred.promise;
    }
    return this._fireBeforeSave().then(function() {
      return self.__doHandleSave(validateOnly);
    });
  },

  __doHandleSave: function(validateOnly) {

    this._blockUI();

    // save should be done on root form scope only
    var rootForm = this._getRootFormElement();
    var scope = rootForm.is('[ui-view-grid]') ? this.scope : rootForm.scope();
    var deferred = this.ws.defer();

    if (scope.isValid && !scope.isValid()) {
      if (scope.showErrorNotice) {
        scope.showErrorNotice();
      } else {
        axelor.notify.error(_t('Please correct the invalid form values.'), {
          title: _t('Validation error')
        });
      }
      deferred.reject();
      return deferred.promise;
    }

    // Record may not be dirty but still have default values to save
    function isSavable() {
      if (scope.isDirty && scope.isDirty()) return true;
      return !(scope.record || {}).id && notEmpty(scope.record);
    }

    function notEmpty(record) {
      return _.some(record, function (value) {
        return _.isObject(value) ? notEmpty(value) : value !== undefined;
      });
    }

    if (validateOnly || !isSavable()) {
      deferred.resolve();
      return deferred.promise;
    }

    function doEdit(rec) {
      var params = scope._viewParams || {};
      scope.editRecord(rec);
      if (params.$viewScope) {
        params.$viewScope.updateRoute();
      }
      deferred.resolve();
    }

    function doSave(values) {
      var ds = scope._dataSource;
      ds.save(values).success(function(rec, page) {
        if (scope.doRead) {
          return scope.doRead(rec.id).success(doEdit);
        }
        return ds.read(rec.id).success(doEdit);
      });
    }

    var values = _.extend({ _original: scope.$$original }, scope.record);
    if (scope.onSave) {
      scope.onSave({
        values: values,
        callOnSave: false,
        wait: false
      }).then(deferred.resolve, deferred.reject);
    } else {
      doSave(values);
    }

    this._invalidateContext = true;
    return deferred.promise;
  },

  _closeView: function (scope) {
    if (scope.onOK) {
      return scope.onOK();
    }
    var tab = scope._viewParams || scope.selectedTab;
    if (scope.closeTab) {
      scope.closeTab(tab);
    } else if (scope.$parent) {
      this._closeView(scope.$parent);
    }
  },

  _isSameViewType: function () {
    return this.viewType === this.scope.viewType;
  },

  _handleAction: function(action) {

    this._blockUI();

    var self = this,
      scope = this.scope,
      context = this._getContext(),
      deferred = this.ws.defer();

    if (!this._isSameViewType()) {
      deferred.reject();
      return deferred.promise;
    }

    function resolveLater() {
      deferred.resolve();
      return deferred.promise;
    }

    function chain(items) {
      var first = _.first(items);
      if (first === undefined) {
        return resolveLater();
      }
      return self._handleSingle(first).then(function(pending) {
        if (_.isString(pending) && pending.trim().length) {
          return self._handleAction(pending);
        }

        var _deferred = self.ws.defer();
        scope.$timeout(function () {
          scope.ajaxStop(function() {
            _deferred.resolve();
          });
        });

        return _deferred.promise.then(function () {
          return chain(_.rest(items));
        });
      });
    }

    if (!action) {
      return resolveLater();
    }

    action = action.replace(/(^\s*,?\s*)|(\s*,?\s*$)/, '');

    var pattern = /,\s*(sync)\s*(,|$)/;
    if (pattern.test(action)) {
      var which = pattern.exec(action)[1];
      axelor.dialogs.error(_t('Invalid use of "{0}" action, must be the first action.', which));
      deferred.reject();
      return deferred.promise;
    }

    pattern = /(^sync\s*,\s*)|(^sync$)/;
    if (pattern.test(action)) {
      action = action.replace(pattern, '');
      return this._fireBeforeSave().then(function() {
        return self._handleAction(action);
      });
    }

    pattern = /(^|,)\s*(new)\s*,/;
    if (pattern.test(action)) {
      var which = pattern.exec(action)[2];
      axelor.dialogs.error(_t('Invalid use of "{0}" action, must be the last action.', which));
      deferred.reject();
      return deferred.promise;
    }

    pattern = /(^|,)\s*(close)\s*,/;
    if (pattern.test(action)) {
      axelor.dialogs.error(_t('Invalid use of "{0}" action, must be the last action.', pattern.exec(action)[2]));
      deferred.reject();
      return deferred.promise;
    }

    if (action === 'close') {
      this._closeView(scope);
      deferred.resolve();
      return deferred.promise;
    }

    if (action === 'new') {
      return this._handleNew();
    }

    if (action === 'validate') {
      return this._handleSave(true);
    }

    if (action === 'save') {
      return this._handleSave();
    }

    if (this._invalidateContext) {
      context = this._getContext();
      this._invalidateContext = false;
    }

    var model = context._model || scope._model;
    var data =  scope.getActionData ? scope.getActionData(context) : null;
    if (data && context._signal) {
      data._signal = context._signal;
    }

    var promise = this.ws.action(action, model, context, data).then(function(response){
      var resp = response.data,
        data = resp.data || [];
      if (resp.errors) {
        data.splice(0, 0, {
          errors: resp.errors
        });
      }
      return chain(data);
    });

    promise.then(deferred.resolve, deferred.reject);

    return deferred.promise;
  },

  _handleSingle: function(data) {

    var deferred = this.ws.defer();

    if (!data || data.length === 0) {
      deferred.resolve();
      return deferred.promise;
    }

    if (!this._isSameViewType()) {
      deferred.reject();
      return deferred.promise;
    }

    var self = this,
      scope = this.scope,
      formElement = this._getFormElement(),
      formScope = formElement.data('$scope') || scope,
      rootForm = this._getRootFormElement(),
      rootScope = rootForm.is('[ui-view-grid]') ? scope
        : rootForm.scope() || (scope.selectedTab || {}).$viewScope;

    function doReload(pending) {
      self._invalidateContext = true;
      var promise = _.isFunction(rootScope.reload) ? rootScope.reload() : scope.reload();
      if (promise) {
        promise.then(function(){
          deferred.resolve(pending);
        }, deferred.reject);
      } else {
        deferred.resolve(pending);
      }
      return deferred.promise;
    }

    if (data.exportFile) {
      var link = "ws/files/data-export/" + data.exportFile;
      ui.download(link, data.exportFile);
    }

    if (data.signal === 'refresh-app') {
      if(data.info) {
        axelor.dialogs.box(data.info.message, {
          onClose: function () {
            window.location.reload();
          }
        }, {
          title: data.info.title,
          confirmBtnTitle: data.info.confirmBtnTitle
        });
      } else {
        window.location.reload();
      }
      return deferred.promise;
    }
    if (data.signal === 'refresh-tab') {
      rootScope.waitForActions(function () {
        rootScope.reloadTab(rootScope.selectedTab);
      });
    }

    if(data.info) {
      axelor.dialogs.box(data.info.message, {
        onClose: function () {
          if (data.pending) {
            scope.$applyAsync(function(){
              if (data.reload) {
                return doReload(data.pending);
              }
              deferred.resolve(data.pending);
            });
          }
        },
        title: data.info.title,
        confirmBtnTitle: data.info.confirmBtnTitle
      });
      if (data.pending) {
        return deferred.promise;
      }
    }

    if(data.notify) {
      if (_.isArray(data.notify)) {
        _.each(data.notify, function(item) {
          axelor.notify.info(item.message, {
            title: item.title
          });
        });
      } else {
        axelor.notify.info(data.notify.message, {
          title: data.notify.title
        });
      }
    }

    if(data.error) {
      axelor.dialogs.error(data.error.message, function(){
        scope.$applyAsync(function(){
          if (data.error.action) {
            self._handleAction(data.error.action);
          }
          deferred.reject();
        });
      }, {
        title: data.error.title,
        confirmBtnTitle: data.error.confirmBtnTitle
      });
      return deferred.promise;
    }

    if (data.alert) {
      axelor.dialogs.confirm(data.alert.message, function(confirmed){
        scope.$applyAsync(function(){
          if (confirmed) {
            return deferred.resolve(data.pending);
          }
          if (data.alert.action) {
            self._handleAction(data.alert.action);
          }
          deferred.reject();
        });
      }, {
        title: data.alert.title || _t('Warning'),
        confirmBtnTitle: data.alert.confirmBtnTitle,
        cancelBtnTitle: data.alert.cancelBtnTitle,
        yesNo: false
      });

      return deferred.promise;
    }

    if (!_.isEmpty(data.errors)) {
      var hasError = false;
      _.each(data.errors, function(v, k){
        var item = (findItems(k) || $()).first();
        handleError(scope, item, v);
        if(v && v.length > 0) {
          hasError = true;
        }
      });
      if(hasError) {
        deferred.reject();
        return deferred.promise;
      }
    }

    if (data.values) {
      updateValues(data.values, scope.record, scope, formScope);
      if (scope.onChangeNotify) {
        scope.onChangeNotify(scope, data.values);
      }
      this._invalidateContext = true;
      axelor.$adjustSize();
    }

    if (data.reload) {
      return (function () {
        var promise = doReload(data.pending);
        if (data.view) {
          promise.then(function () {
            doOpenView(data.view);
          });
        }
        return promise;
      })();
    }

    if (data.validate || data.save) {
      var handleSave = function () {
        scope.$timeout(function () {
          self._handleSave(!!data.validate).then(function () {
            scope.ajaxStop(function () {
              deferred.resolve(data.pending);
            }, 100);
          }, deferred.reject);
        });
      };

      if (rootScope.afterGridEdit) {
        scope.$emit('on:before-save-action', rootScope.record);
        rootScope.afterGridEdit(handleSave);
      } else {
        handleSave();
      }

      return deferred.promise;
    }

    if (data['new']) {
      scope.$timeout(function () {
        self._handleNew().then(function(){
          scope.ajaxStop(function () {
            deferred.resolve(data.pending);
          }, 100);
        }, deferred.reject);
      });
      return deferred.promise;
    }

    if (data.signal) {
      formScope.$broadcast(data.signal, data['signal-data']);
    }

    function findItems(name) {

      var items = $();
      var toolbar;
      var containers;
      var formPaths = [formScope.formPath];
      var isSlickEditor;

      if (formElement.is('[ui-slick-editors]')) {
        containers = formElement.parent().add(formElement);
        isSlickEditor = true;
      } else if (formElement.parent().is('[ui-slick-editors],.slick-cell')) {
        containers = formElement.parent().parent().add(formElement);
        isSlickEditor = true;
      } else if (formElement.parent().is('[ui-panel-editor]')) {
        containers = formElement.parent().add(formElement).is('.m2o-editor-form,.o2m-editor-form') ? formElement : formElement.parents('[ui-form]:first').add(formElement);
      } else {
        containers = formElement;
        toolbar = formElement.parents('.form-view:first,.search-view:first')
          .find('.record-toolbar:first,.search-view-toolbar:first');
      }

      if (isSlickEditor) {
        var detailForm = formElement.parents('.form-item').first().children('.detail-form').first();
        if (detailForm.length) {
          containers = containers.add(detailForm);
          formPaths.push(detailForm.scope().formPath);
        }
      }

      _.each(formPaths, function (formPath) {
        if (formScope._model === 'com.axelor.meta.db.MetaJsonRecord') {
          formPath = formPath || 'attrs';
        }

        var current = containers.find('[x-path="' + (formPath ?  formPath + '.' + name : name) + '"]');
        if (current.length === 0 && formPath != 'attrs') {
          current = containers.find('[x-path="attrs.' + name + '"]');
        }

        items = items.add(current);
      });

      if (toolbar) {
        return toolbar.find('[name="' + name + '"],[x-name="' + name + '"]').add(items);
      }
      return items;
    }

    function setAttrs(item, itemAttrs, itemIndex) {

      var label = item.data('label'),
        itemScope = item.data('$scope'),
        hasValues = false,
        column;

      if (item.is('[ui-menu-item]')) {
        itemScope = item.isolateScope();
      }

      // handle o2m/m2m columns
      if (item.is('.slick-dummy-column')) {
        column = item.data('column');
        itemScope = item.parents('[x-path]:first,.portlet-grid').data('$scope');
        forEach(itemAttrs, function(value, attr){
          switch (attr) {
            case 'hidden':
              itemScope.showColumn(column.id, !value);
              break;
            case 'title':
              setTimeout(function(){
                itemScope.setColumnTitle(column.id, value);
              });
              break;
            case 'scale':
              var grid = item.parents('.slickgrid:first').data('grid');
              if (grid) {
                var found = _.findWhere(grid.getColumns(), {id: column.id});
                if (found) {
                  var descriptor = found.descriptor;
                  descriptor.widgetAttrs = _.extend(descriptor.widgetAttrs || {}, {scale: value});
                }
                itemScope.setItems(itemScope.getItems());
              }
          }
        });
        return;
      }

      //handle o2m/m2m title
      if(item.is('.one2many-item') || item.is('.many2many-item')){
        forEach(itemAttrs, function(value, attr){
          if (attr == 'title') {
            itemScope.title = value;
          }
        });
      }

      // handle notebook
      if (item.is('.tab-pane')) {
        forEach(itemAttrs, function(value, attr){
          if (attr == 'hidden') {
            itemScope.attr('hidden', value);
          }
          if (attr == 'title') {
            itemScope.title = value;
          }
        });
        return;
      }

      function isDotted() {
        var name = item.attr('x-field') || '';
        var dotted = name.indexOf('.') > -1;
        if (dotted) {
          itemAttrs.$hasDotted = true;
        }
        return dotted;
      }

      forEach(itemAttrs, function(value, attr){

        if ((attr === "value" || attr.indexOf('value:') === 0)) {
          hasValues = true;
          if (itemScope && itemScope.$setForceWatch) {
            itemScope.$setForceWatch(true);
          }
          if (isDotted()) return;
          if (itemAttrs.$hasDotted) {
            itemAttrs.$hasDotted = false;
          } else if (itemIndex > 0) {
            return;
          }
        }

        switch(attr) {
        case 'hidden':
          if (itemScope.field && itemScope.field.hideIf === "true") return;
        case 'required':
        case 'readonly':
        case 'collapse':
        case 'precision':
        case 'scale':
        case 'prompt':
        case 'css':
        case 'icon':
        case 'selection-in':
          itemScope.attr(attr, value);
          break;
        case 'title':
          (function () {
            var span = $(label).add(item).children('span[ui-help-popover]:first');
            if (span.length === 0) {
              span = label;
            }
            if (span && span.length > 0) {
              span.html(value);
            } else if (item.is('label')) {
              item.html(value);
            }
          })();
          itemScope.attr('title', value);
          break;
        case 'domain':
          if (itemScope.setDomain)
            itemScope.setDomain(value);
          break;
        case 'refresh':
          itemScope.waitForActions(function () {
            itemScope.$broadcast('on:attrs-change:refresh');
          }, 100);
          break;
        case 'url':
        case 'url:set':
          if (item.is('[ui-dashlet]')) {
            itemScope.attr('url', value);

            // Refresh if dashlet is already loaded
            var dashletScope = item.find('.dashlet').scope() || {};
            if (dashletScope.view && dashletScope.onRefresh) {
              dashletScope.onRefresh();
            }
          }
          break;
        case 'value':
        case 'value:set':
          if (itemScope.setValue) {
            itemScope.setValue(value);
          }
          break;
        case 'value:add':
          if (itemScope.fetchData && itemScope.select) {
            itemScope.fetchData(value, function(records){
              itemScope.select(records);
            });
          }
          break;
        case 'value:del':
          if (itemScope.removeItems) {
            itemScope.removeItems(value);
          }
          break;
        case 'active':
          if (itemScope.selectTab) {
            var tabName = (itemScope.field || {}).name;
            var selectedTab = _.find(itemScope.tabs, _.toBoolean(value)
              ? function (tab) {
              return tabName === (tab.field || {}).name;
            } : function (tab) {
              return tabName !== (tab.field || {}).name;
            });
            if (selectedTab) {
              itemScope.selectTab(selectedTab);
            }
          }
          break;
        case 'focus':
          if (value) {
            itemScope.focus();
          }
          break;
        }
      });

      if (hasValues && formScope.onChangeNotify) {
        formScope.onChangeNotify(formScope, formScope.record);
      }
    }

    forEach(data.attrs, function(itemAttrs, itemName) {
      var items = findItems(itemName);
      if (!items || items.length === 0) {
        // dashlet still not loaded ?
        if (itemName.indexOf('.') > -1) {
          var parentName = itemName.substring(0, itemName.indexOf('.'));
          var parentElem = findItems(parentName);
          if (parentElem.is('[ui-dashlet]')) {
            parentElem.scope().$$pendingAttrs = parentElem.scope().$$pendingAttrs || {};
            parentElem.scope().$$pendingAttrs[itemName.substring(itemName.indexOf('.')+1)] = itemAttrs;
          }
        }
        // self (form itself)
        if (itemName === 'self') {
          setAttrs(formElement, _.pick(itemAttrs, 'readonly'));
        }
        return;
      }
      items.each(function(i) {
        setAttrs($(this), itemAttrs, i);
      });
    });

    if (data.report) {
      return openReport(data);
    }

    function openReport(data) {
      var record = formScope.record || {};
      if (data.attached) {
        record.$attachments = (record.$attachments || 0) + 1;
        axelor.dialogs.confirm(_t('Report attached to current object. Would you like to download?'),
        function(confirmed) {
          scope.$applyAsync(function() {
            if (confirmed) {
              var url = "ws/rest/com.axelor.meta.db.MetaFile/" + data.attached.id + "/content/download";
              ui.download(url, data.attached.fileName);
              return deferred.resolve();
            }
            deferred.reject();
          });
        }, {
          title: _t('Download'),
          yesNo: false
        });
        return deferred.promise;
      }

      var url = "ws/files/report/" + data.reportLink + "?name=" + data.reportFile;
      var tab = {
        title: data.reportFile,
        resource: url,
        viewType: 'html'
      };

      if (axelor.device.mobile && data.reportFormat !== "html") {
        ui.download(url, data.reportFile);
      } else if (['pdf', 'html'].indexOf(data.reportFormat) > -1) {
        doOpenView(tab);
      } else {
        ui.download(url);
      }

      scope.$timeout(deferred.resolve);
      return deferred.promise;
    }

    function openTab(scope, tab) {
      if (scope.openTab) {
        scope.openTab(tab);
      } else if (scope.$parent) {
        openTab(scope.$parent, tab);
      }
    }

    function doOpenView(tab) {
      tab.action = _.uniqueId('$act');
      if (!tab.viewType)
        tab.viewType = 'grid';
      if (tab.viewType == 'grid' || tab.viewType == 'form')
        tab.model = tab.model || tab.resource;
      if (!tab.views) {
        tab.views = [{ type: tab.viewType }];
        if (tab.viewType === 'html') {
          angular.extend(tab.views[0], {
            resource: tab.resource,
            title: tab.title
          });
        }
      }
      if (tab.viewType === "html" && (tab.params||{}).download) {
        var view = _.findWhere(tab.views, { type: "html" });
        if (view) {
          var url = view.name || view.resource;
          var fileName = tab.params.fileName;
          ui.download(url, fileName);
          return scope.$applyAsync();
        }
      }
      if (tab.viewType === "html" && (tab.params||{}).target === "_blank") {
        var view = _.findWhere(tab.views, { type: "html" });
        if (view) {
          var url = view.name || view.resource;
          setTimeout(function () {
            window.open(url);
          });
          return scope.$applyAsync();
        }
      }
      if ((tab.params && tab.params.popup) || axelor.device.mobile) {
        tab.$popupParent = formScope;
      }
      openTab(scope, tab);
      scope.$applyAsync();
    }

    if (data.view) {
      doOpenView(data.view);
    }

    if (data.close || data.canClose) {
      this._closeView(scope);
    }

    deferred.resolve();

    return deferred.promise;
  }
};

ui.factory('ActionService', ['ViewService', function(ViewService) {

  function handler(scope, element, options) {
    var opts = _.extend({}, options, { element: element });
    return new ActionHandler(scope, ViewService, opts);
  }

  return {
    handler: handler
  };
}]);

var EVENTS = ['onClick', 'onChange', 'onSelect', 'onTabSelect', 'onNew', 'onLoad', 'onSave'];

ui.directive('uiActions', ['ViewService', function(ViewService) {

  function link(scope, element, attrs) {

    var props = _.isEmpty(scope.field) ? scope.schema : scope.field;
    if (!props) {
      return;
    }

    _.each(EVENTS, function(name){
      var action = props[name];
      if (!action) {
        return;
      }

      var handler = new ActionHandler(scope, ViewService, {
        name: name,
        element: element,
        action: action,
        canSave: props.canSave,
        prompt: props.prompt
      });
      scope.$events[name] = _.bind(handler[name], handler);
    });
  }

  return {
    link: function(scope, element, attrs) {
      scope.$evalAsync(function() {
        link(scope, element, attrs);
      });
    }
  };
}]);

ui.directive('uiActionClick', ['ViewService', function(ViewService) {
  return {
    link: function(scope, element, attrs) {
      var action = attrs.uiActionClick;
      var actionContext = attrs.uiActionContext;
      var actionScope = scope;

      if (actionContext) {
        var getContext = scope.getContext;
        actionScope = actionScope.$new();
        actionScope.getContext = function () {
          var context = getContext ? getContext.apply(scope, arguments) : {};
          var value = actionScope.$eval(actionContext);
          if (_.isArray(value) || _.isDate(value)) return context;
          if (_.isObject(value)) return _.extend({}, context, value);
          return context;
        };
      }

      scope.$evalAsync(function() {
        var handler = new ActionHandler(actionScope, ViewService, {
          element: element,
          action: action
        });
        element.on("click", function () {
          handler.handle();
          scope.$applyAsync();
        });
      });
    }
  };
}]);

})();
