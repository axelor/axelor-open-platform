/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

var equals = angular.equals,
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
				var found = _.find(dest, function(v){
					return item.id && v.id === item.id;
				});
				if (_.has(item, "version") && item.id) item.$fetched = true;
				if (found) {
					var found_ = _.extend({}, found);
					var changed_ = updateValues(item, found_);
					changed = changed || changed_;
					return changed_ ? found_ : found;
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
		}
	});

	if (target && changed) {
		target.$dirty = true;
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

	if (item.children(':first').is(':input,.input-append,.picker-input')) {
		p.append(e);
	} else {
		p.prepend(e);
	}

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
		var prompt = this._getPrompt();
		if (prompt) {
			var deferred = this.ws.defer(),
				promise = deferred.promise;
			axelor.dialogs.confirm(prompt, function(confirmed){
				if (confirmed) {
					self._fireBeforeSave().then(function() {
						self.handle().then(deferred.resolve, deferred.reject);
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
			return self.handle();
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
		var scope = this.scope,
			context = scope.getContext ? scope.getContext() : scope.record,
			viewParams = scope._viewParams || {};
		
		context = _.extend({}, viewParams.context, context);

		// include button name as _signal (used by workflow engine)
		if (this.element.is("button,a.button-item,li.action-item")) {
			context._signal = this.element.attr('name') || this.element.attr('x-name');
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
		_.delay(axelor.blockUI, 100);
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
		if (validateOnly || (scope.isDirty && !scope.isDirty())) {
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

	_handleAction: function(action) {

		this._blockUI();
		
		var self = this,
			scope = this.scope,
			context = this._getContext(),
			deferred = this.ws.defer();

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
		var promise = this.ws.action(action, model, context).then(function(response){
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

		var self = this,
			scope = this.scope,
			formElement = this._getFormElement(),
			formScope = formElement.data('$scope') || scope,
			rootForm = this._getRootFormElement(),
			rootScope = rootForm.is('[ui-view-grid]') ? scope : rootForm.scope();

		function doReload(pending) {
			self._invalidateContext = true;
			var promise = rootScope.reload();
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
			(function () {
				var link = "ws/files/data-export/" + data.exportFile;
				var frame = $('<iframe>').appendTo('body').hide();
				frame.attr("src", link);
				setTimeout(function(){
					frame.attr("src", "");
					frame.remove();
					frame = null;
				}, 5000);
			})();
		}
		
		if (data.signal === 'refresh-app') {
			if(data.flash || data.info) {
				axelor.dialogs.box(data.flash || data.info, {
					onClose: function () {
						window.location.reload();
					}
				});
			} else {
				window.location.reload();
			}
			return deferred.promise;
		}

		if(data.flash || data.info) {
			axelor.dialogs.box(data.flash || data.info, {
				onClose: function () {
					if (data.pending) {
						scope.applyLater(function(){
							if (data.reload) {
								return doReload(data.pending);
							}
							deferred.resolve(data.pending);
						});
					}
				}
			});
			if (data.pending) {
				return deferred.promise;
			}
		}

		if(data.notify) {
			axelor.notify.info(data.notify);
		}

		if(data.error) {
			axelor.dialogs.error(data.error, function(){
				scope.applyLater(function(){
					if (data.action) {
						self._handleAction(data.action);
					}
					deferred.reject();
				});
			});
			return deferred.promise;
		}
		
		if (data.alert) {
			axelor.dialogs.confirm(data.alert, function(confirmed){
				scope.applyLater(function(){
					if (confirmed) {
						return deferred.resolve(data.pending);
					}
					if (data.action) {
						self._handleAction(data.action);
					}
					deferred.reject();
				});
			}, {
				title: _t('Warning'),
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
			scope.$timeout(function () {
				self._handleSave(!!data.validate).then(function(){
					scope.ajaxStop(function () {
						deferred.resolve(data.pending);
					}, 100);
				}, deferred.reject);
			});
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

			var items;
			var toolbar;
			var containers;

			if (formElement.is('[ui-slick-editors]')) {
				containers = formElement.parent().add(formElement);
			} else if (formElement.parent().is('[ui-slick-editors],.slick-cell')) {
				containers = formElement.parent().parent().add(formElement);
			} else if (formElement.parent().is('[ui-panel-editor]')) {
				containers = formElement.parent().add(formElement).is('.m2o-editor-form,.o2m-editor-form') ? formElement : formElement.parents('[ui-form]:first').add(formElement);
			} else {
				containers = formElement;
				toolbar = formElement.parents('.form-view:first,.search-view:first')
					.find('.record-toolbar:first,.search-view-toolbar:first');
			}

			items = containers.find('[x-path="' + (formScope.formPath ?  formScope.formPath + '.' + name : name) + '"]');
			if (toolbar) {
				return toolbar.find('[name="' + name + '"]').add(items);
			}
			return items;
		}
		
		function setAttrs(item, itemAttrs, itemIndex) {
			
			var label = item.data('label'),
				itemScope = item.data('$scope'),
				hasValues = false,
				column;

			// handle o2m/m2m columns
			if (item.is('.slick-dummy-column')) {
				column = item.data('column');
				itemScope = item.parents('[x-path]:first,.portlet-grid').data('$scope');
				forEach(itemAttrs, function(value, attr){
					if (attr == 'hidden')
						itemScope.showColumn(column.id, !value);
					if (attr == 'title')
						setTimeout(function(){
							itemScope.setColumnTitle(column.id, value);
						});
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
					if (itemScope.$setForceWatch) {
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
					itemScope.attr(attr, value);
					break;
				case 'title':
					(function () {
						var span = $(label).add(item).children('span[ui-help-popover]:first');
						if (span.size() === 0) {
							span = label;
						}
						if (span && span.size() > 0) {
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
					if (item.is('[ui-portlet]')) {
						item.find('iframe:first').attr('src', value);
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
				}
			});

			if (hasValues && formScope.onChangeNotify) {
				formScope.onChangeNotify(formScope, formScope.record);
			}
		}
		
		forEach(data.attrs, function(itemAttrs, itemName) {
			var items = findItems(itemName);
			if (!items || items.length === 0) {
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
					scope.applyLater(function() {
						if (confirmed) {
							var url = "ws/rest/com.axelor.meta.db.MetaFile/" + data.attached.id + "/content/download";
							ui.download(url);
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
			if (tab.viewType == 'form' || tab.viewType == 'grid') {
				var views = _.groupBy(tab.views, 'type');
				if (!views.grid) tab.views.push({type: 'grid'});
				if (!views.form) tab.views.push({type: 'form'});
			}
			if (tab.viewType === "html" && (tab.params||{}).download) {
				var view = _.findWhere(tab.views, { type: "html" });
				if (view) {
					var url = view.name || view.resource;
					var fileName = tab.params.fileName || "true";
					ui.download(url, fileName);
					return scope.applyLater();
				}
			}
			if (tab.viewType === "html" && (tab.params||{}).target === "_blank") {
				var view = _.findWhere(tab.views, { type: "html" });
				if (view) {
					var url = view.name || view.resource;
					setTimeout(function () {
						window.open(url);
					});
					return scope.applyLater();
				}
			}
			if ((tab.params && tab.params.popup) || axelor.device.mobile) {
				tab.$popupParent = formScope;
			}
			openTab(scope, tab);
			scope.applyLater();
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
			scope.$evalAsync(function() {
				var handler = new ActionHandler(scope, ViewService, {
					element: element,
					action: action
				});
				element.on("click", function () {
					handler.handle();
					scope.applyLater();
				});
			});
		}
	};
}]);

})();
