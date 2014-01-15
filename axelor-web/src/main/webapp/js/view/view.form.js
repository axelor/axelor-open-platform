/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
(function() {

var ui = angular.module('axelor.ui');

this.FormViewCtrl = FormViewCtrl;

FormViewCtrl.$inject = ['$scope', '$element'];
function FormViewCtrl($scope, $element) {

	DSViewCtrl('form', $scope, $element);

	var ds = $scope._dataSource;

	$scope.fields = {};
	$scope.fields_view = {};
	
	$scope.record = {};
	$scope.$$original = null;
	$scope.$$dirty = false;
	
	$scope.$events = {};
	
	/**
	 * Get field attributes.
	 * 
	 * @param field field name or field element
	 */
	$scope.getViewDef = function(field) {
		var name = id = field,
			elem = $(field),
			attrs = {};

		if (_.isObject(field)) {  // assume element
			name = elem.attr('x-field') || elem.attr('data-field') || elem.attr('name');
			id = elem.attr('x-for-widget') || elem.attr('id') || name;
		}
		
		attrs = _.extend({}, $scope.fields[name], $scope.fields_view[id]);

		return attrs;
	};

	$scope.doRead = function(id) {
		var params = {
			fields : _.pluck($scope.fields, 'name')
		};
		return ds.read(id, params);
	};

	function doEdit(id, dummy) {
		return $scope.doRead(id).success(function(record){
			if (dummy) {
				record = _.extend(dummy, record);
			}
			$scope.edit(record);
		});
	}
	
	var initialized = false;
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

		if (recordId) {
			return viewPromise.then(function(){
				doEdit(recordId);
				if (params.forceEdit) {
					$scope.setEditable();
				}
			});
		}
		
		if (!initialized && params.options && params.options.mode === "edit") {
			initialized = true;
			$scope._routeSearch = params.options.search;
			var recordId = +params.options.state;
			if (recordId > 0) {
				return viewPromise.then(function(){
					doEdit(recordId);
				});
			}
		}
		
		var page = ds.page(),
			record = null;
		
		if (page.index > -1) {
			record = ds.at(page.index);
		}
		viewPromise.then(function(){
			$scope.ajaxStop(function(){
				record = ($scope.record || {}).id ? $scope.record : record;
				if (record === undefined) {
					$scope.edit(null);
					return $scope.ajaxStop(function(){
						$scope.$broadcast("on:new");
					});
				}
				if (record && record.id)
					return doEdit(record.id);
				$scope.edit(record);
			});
		});
	};

	var editable = false;

	$scope.isEditable = function() {
		return editable;
	};

	$scope.setEditable = function() {
		editable = arguments.length === 1 ? _.first(arguments) : true;
	};
	
	$scope.getRouteOptions = function() {
		var rec = $scope.record,
			args = [];
		
		if (rec && rec.id > 0) {
			args.push(rec.id);
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

		$scope._routeSearch = opts.search;
		if (record.id == state) {
			return $scope.updateRoute();
		}
		
		var params = $scope._viewParams;
		if (params.viewType !== "form") {
			return $scope.show();
		}
		return state ? doEdit(state) : $scope.edit(null);
	};

	$scope.edit = function(record) {
		$scope.editRecord(record);
		$scope.updateRoute();
		$scope._viewPromise.then(function(){
			$scope.ajaxStop(function(){
				var handler = $scope.$events.onLoad,
					record = $scope.record;
				if (handler && !_.isEmpty(record)) {
					setTimeout(handler);
				}
			});
		});
	};
	
	$scope.editRecord = function(record) {
		$scope.$$original = record || {};
		$scope.$$dirty = false;
		$scope.record = angular.copy($scope.$$original);
		$scope.ajaxStop(function(){
			$scope.$broadcast("on:edit", $scope.record);
			$scope.$broadcast("on:record-change", $scope.record);
		});
	};
	
	function updateSelected(name, records) {
		var path = $scope.formPath ? $scope.formPath + '.' + name : name,
			child = $element.find('[x-path="' + path + '"]:first'),
			childScope = child.data('$scope');
		if (records == null || childScope == null)
			return records;
		
		var selected = childScope.selection || [],
			result = [];
		
		selected = _.map(selected, function(i){
			return childScope.dataView.getItem(i).id;
		});
		
		_.each(records, function(item){
			item = _.extend({}, item, {
				selected : _.contains(selected, item.id)
			});
			if (item.id <= 0) item.id = null;
			result.push(item);
		});
		
		return result;
	}
	
	$scope.getContext = function() {
		var context = _.extend({}, $scope._routeSearch, $scope.record);
		if ($scope.$parent && $scope.$parent.getContext) {
			context._parent = $scope.$parent.getContext();
		}
		
		_.each($scope.fields, function(field, name) {
			if (/-many$/.test(field.type)) {
				var items = updateSelected(field.name, context[name]);
				if (items) {
					context[name] = items;
				}
			}
		});

		var dummy = $scope.getDummyValues();
		_.each(dummy, function (value, name) {
			if (value && value.$updatedValues) {
				dummy[name] = value.$updatedValues;
			}
			if (name.indexOf('$') === 0) {
				dummy[name.substring(1)] = dummy[name];
			}
		});
		
		context = _.extend(context, dummy);
		context._model = ds._model;
		return context;
	};

	$scope.isDirty = function() {
		return $scope.$$dirty = !ds.equals($scope.record, $scope.$$original);
	};

	$scope.$watch("record", function(rec, old) {
		var view = $scope.schema;
		if (view && view.readonlyIf) {
			var readonly = axelor.$eval($scope, view.readonlyIf, rec);
			if (_.isFunction($scope.attr)) {
				$scope.attr('readonly', readonly);
			}
			editable = !readonly;
		}
		if (rec === old) {
			return $scope.$$dirty = false;
		}
		$scope.$broadcast("on:record-change", rec);
		return $scope.$$dirty = $scope.isDirty();
	}, true);

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
		return $scope.hasButton('save') && $scope.$$dirty && $scope.isValid();
	};

	$scope.canDelete = function() {
		return $scope.hasButton('delete');
	};
	
	$scope.canCopy = function() {
		return !$scope.isEditable() && $scope.hasButton('copy') && !$scope.$$dirty && ($scope.record || {}).id;
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
		$scope.confirmDirty(function(){
			$scope.edit(null);
			$scope.setEditable();
			$scope.$broadcast("on:new");
		});
	};
	
	$scope.onNewPromise = null;
	
	$scope.$on("on:new", function onNewHandler(event) {
		
		function afterVewLoaded() {
			
			var handler = $scope.$events.onNew;
			var last = $scope.$parent.onNewPromise || $scope.onNewPromise;
			
			function reset() {
				$scope.onNewPromise = null;
			}
			
			function handle() {
				var promise = handler();
				if (promise && promise.then) {
					promise.then(reset, reset);
					promise = promise.then(function () {
						if ($scope.isDirty()) {
							return $scope.editRecord($scope.record);
						}
					});
				}
				return promise;
			}

			$scope.setEditable();

			if (handler && $scope.record) {
				if (last) {
					return $scope.onNewPromise = last.then(handle);
				}
				$scope.onNewPromise = handle();
			}
		}
		
		$scope._viewPromise.then(function() {
			$scope.$timeout(afterVewLoaded);
		});
	});
	
	$scope.$on("on:nav-click", function(event, tab) {
		var record, context, checkVersion;
		if (event.defaultPrevented || tab.$viewScope !== $scope) {
			return;
		}
		event.preventDefault();
		context = tab.context || {};
		record = $scope.record || {};
		checkVersion = "" + context.__check_version;

		if (!record.id || checkVersion !== "true") {
			return;
		}
		
		ds.verify(record).success(function(res){
			if (res.status !== 0) {
				axelor.dialogs.confirm(
						_t("The record has been updated or delete by another action.") + "<br>" +
						_t("Would you like to reload the current record?"),
				function(confirmed){
					if (confirmed) {
						$scope.reload();
					}
				});
			}
		});
	});
	
	$scope.onEdit = function() {
		$scope.setEditable();
	};

	$scope.onCopy = function() {
		var record = $scope.record;
		ds.copy(record.id).success(function(record){
			$scope.edit(record);
			$scope.setEditable();
		});
	};
	
	$scope.getDummyValues = function() {
		if (!$scope.record) return {};
		var fields = _.keys($scope.fields);
		var extra = _.chain($scope.fields_view)
					  .filter(function(f){ return f.name && !_.contains(fields, f.name); })
					  .pluck('name')
					  .compact()
					  .value();
		return _.pick($scope.record, extra);
	};

	$scope.onSave = function() {
		
		var defer = $scope._defer();
		var event = $scope.$broadcast('on:before-save', $scope.record);
		var saveAction = $scope.$events.onSave;

		if (event.defaultPrevented) {
			if (event.error) {
				axelor.dialogs.error(event.error);
			}
			setTimeout(function() {
				defer.reject(event.error);
			});
			return defer.promise;
		}

		function doSave() {
			var dummy = $scope.getDummyValues();

			var values = ds.diff($scope.record, $scope.$$original);
			var promise = ds.save(values).success(function(record, page) {
				return doEdit(record.id, dummy);
			});

			promise.success(function(record) {
				defer.resolve(record);
			});
			promise.error(function(error) {
				defer.reject(error);
			});
		}

		$scope.applyLater(function() {
			$scope.ajaxStop(function() {
				if (!$scope.canSave()) {
					return defer.promise;
				}
				if (saveAction) {
					return saveAction().then(doSave);
				}
				return doSave();
			});
		});
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
			$scope.applyLater(callback);
		});
	};

	$scope.onBack = function() {
		var record = $scope.record || {};
		var editable = $scope.isEditable();

		if (record.id && editable) {
			$scope.setEditable(false);
			return;
		}

		$scope.switchTo("grid");
	};

	$scope.onRefresh = function() {
		$scope.confirmDirty($scope.reload);
	};

	$scope.reload = function() {
		var record = $scope.record;
		if (record && record.id) {
			return doEdit(record.id).success(function (rec) {
				var shared = ds.get(record.id);
				if (shared) {
					shared = _.extend(shared, rec);
				}
			});
		}
		$scope.edit(null);
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
	
	$scope.switchTo = function(type, callback) {
		$scope.confirmDirty(function() {
			$scope.setEditable(false);
			$scope.editRecord(null);
			__switchTo(type, callback);
		});
	};

	$scope.onSearch = function() {
		var e = $scope.$broadcast("cancel:grid-edit");
		if (e.defaultPrevented) {
			return;
		}

		$scope.switchTo("grid");
	};
	
	$scope.pagerText = function() {
		var page = ds.page(),
			record = $scope.record || {};
			
		if (page && page.from !== undefined) {
			if (page.total == 0 || page.index == -1 || !record.id) return null;
			return _t("{0} of {1}", (page.from + page.index + 1), page.total);
		}
	},
	
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
				if (record && record.id) doEdit(record.id);
			});
		});
	};
	
	$scope.onPrev = function() {
		$scope.confirmDirty(function() {
			ds.prevItem(function(record){
				if (record && record.id) doEdit(record.id);
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
				$scope.onEdit();
			}
			focusFirst();
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
		}
		
		$scope.applyLater();
		
		return false;
	};
};

ui.directive('uiViewForm', ['$compile', 'ViewService', function($compile, ViewService){
	
	function parse(scope, schema, fields) {
		
		var path = scope.formPath || "";
		
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
				
				var widget = this.widgetName,
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
				type = ui.getWidget(widget) || ui.getWidget(attrs.type) || attrs.type || 'string';

				if (_.isArray(attrs.selectionList) && !widget) {
					type = attrs.multiple ? 'multi-select' : 'select';
				}

				if (attrs.image ==  true) {
					type = "image";
				}
				if (type == 'label') { //TODO: allow <static> tag in xml view
					type = 'static';
				}

				attrs.serverType = attrs.type;
				attrs.type = type;
				
				item.attr('ui-' + type, '');
				item.attr('id', widgetId);
				
				scope.fields_view[widgetId] = attrs;

				//TODO: cover all attributes
				var _attrs = _.extend({}, attrs.attrs, this.attrs, widgetAttrs, {
						'name'			: attrs.name || this.name,
						'x-cols'		: this.cols,
						'x-colspan'		: this.colSpan,
						'x-rowspan'		: this.rowSpan,
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
				
				update(item, _attrs);

				// enable actions & conditional expressions
				item.attr('ui-actions', '');
				item.attr('ui-widget-states', '');

				if (type == 'button' || type == 'static') {
					item.html(this.title);
				}
				
				if (/button|group|tabs|tab|separator|spacer|static/.test(type)) {
					item.attr('x-show-title', false);
				}
			
				var items = this.items || this.pages;
				if (items) {
					process(items, item);
					if (type != 'tabs') {
						item.attr('ui-table-layout', '');
					}
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
		
		return elem;
	}
	
	return function(scope, element, attrs) {
		
		function getContent() {
			var info = {};
				record = scope.record || {};
			if (record.createdOn) {
				info.createdOn = moment(record.createdOn).format('DD/MM/YYYY HH:mm');
				info.createdBy = (scope.record.createdBy || {}).name;
			}
			if (record.updatedOn) {
				info.updatedOn = moment(record.updatedOn).format('DD/MM/YYYY HH:mm');
				info.updatedBy = (scope.record.updatedBy || {}).name;
			}
			var table = $("<table class='field-details'>");
			var tr;
			
			tr = $("<tr></tr>").appendTo(table);
			$("<th></th>").text(_t("Created On:")).appendTo(tr);
			$("<td></td>").text(info.createdOn).appendTo(tr);
			
			tr = $("<tr></tr>").appendTo(table);
			$("<th></th>").text(_t("Created By:")).appendTo(tr);
			$("<td></td>").text(info.createdBy).appendTo(tr);
			
			tr = $("<tr></tr>").appendTo(table);
			$("<th></th>").text(_t("Updated On:")).appendTo(tr);
			$("<td></td>").text(info.updatedOn).appendTo(tr);
			
			tr = $("<tr></tr>").appendTo(table);
			$("<th></th>").text(_t("Updated By:")).appendTo(tr);
			$("<td></td>").text(info.updatedBy).appendTo(tr);
			
			return table;
		}
		
		var logInfo = null;
		scope.onShowLog = function(e) {
			if (logInfo === null) {
				logInfo = $(e.delegateTarget).popover({
					title: _t('Update Log'),
					html: true,
					content: getContent,
					placement: "bottom",
					trigger: "manual",
					container: "body",
					delay: {show: 500, hide: 0}
				});
			}
			logInfo.popover('show');
		};
		
		scope.canShowAttachments = function() {
			return scope.canAttach() && (scope.record || {}).id;
		};
		
		scope.onShowAttachments = function(){
			var attachment = ViewService.compile('<div ui-attachment-popup></div>')(scope.$new());
			var popup = attachment.data('$scope');
			popup.show();
		};
		
		scope.hasAuditLog = function() {
			var record = scope.record || {};
			return record.createdOn || record.updatedOn || record.createBy || record.updatedBy;
		};

		scope.hasHelp = function() {
			var view = scope.schema;
			return view ? view.helpLink : false;
		};
		
		scope.hasWidth = function() {
			var view = scope.schema;
			return view && view.width;
		};

		scope.onShowHelp = function() {
			window.open(scope.schema.helpLink);
		};

		function onMouseDown(e) {
			if (!logInfo) return;
			if (logInfo.is(e.target) || logInfo.has(e.target).size() > 0) return;
			if (logInfo.data('popover').$tip.is(e.target) ||
			    logInfo.data('popover').$tip.has(e.target).size() > 0) return;
			closePopover();
		};

		function closePopover() {
			if (logInfo != null) {
				logInfo.popover('hide');
				logInfo.popover("destroy");
				logInfo = null;
			}
		}

		$(document).on('mousedown.loginfo', onMouseDown);

		scope.$on("$destroy", function() {
			closePopover();
			$(document).off('mousedown.loginfo', onMouseDown);
		});

		var unwatch = scope.$watch('schema.loaded', function(viewLoaded){

			if (!viewLoaded) return;
			
			unwatch();

			var schema = scope.schema;
			var form = parse(scope, schema, scope.fields);

			form = $compile(form)(scope);
			element.append(form);
			
			if (!scope._isPopup) {
				element.addClass('has-width');
			}
			
			if (schema.width) {
				if (schema.width === '100%' || schema.width === '*') {
					element.removeClass('has-width');
				}
				form.css({
					width: schema.width,
					minWidth: schema.minWidth,
					maxWidth: schema.maxWidth
				});
			}
			
			if (scope._viewResolver) {
				scope._viewResolver.resolve(schema, element);
				scope.$broadcast("adjust:dialog");
			}
		});
	};
}]);

}).call(this);
