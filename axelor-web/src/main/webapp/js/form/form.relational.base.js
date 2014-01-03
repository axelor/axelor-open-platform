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
(function(){

var ui = angular.module('axelor.ui');

ui.RefFieldCtrl = RefFieldCtrl;

function RefFieldCtrl($scope, $element, DataSource, ViewService, initCallback) {

	var field = $scope.getViewDef($element),
		params = {
			model: field.target || $element.attr('x-target'),
			views: field.views || {},
			domain: field.domain,
			context: field.context
		},
		views = {};
	
	if (!$element.is('fieldset')) {
		
		_.each(field.views, function(view){
			views[view.type] = view;
		});
		
		var formView = null,
			gridView = null,
			summaryView = null;

		if (field.summaryView === "" || field.summaryView === "true") {
			summaryView = views.form;
		}

		if (field.gridView) {
			gridView = {
				type: 'grid',
				name: field.gridView
			};
		}
		if (field.formView) {
			formView = {
				type: 'form',
				name: field.formView
			};
		}

		if (field.summaryView === "" || field.summaryView === "true") {
			summaryView = views.form || formView || { type: 'form' };
		} else if (field.summaryView) {
			summaryView = {
				type: "form",
				name: field.summaryView
			};
		}
		
		views.form = formView || views.form;
		views.grid = gridView || views.grid;
		params.summaryView = angular.copy(summaryView);
		
		params.views = _.compact([views.grid, views.form]);
		$scope._viewParams = params;
	}
	
	ViewCtrl($scope, DataSource, ViewService);
	
	$scope.ngModel = null;
	$scope.editorCanSave = true;
	$scope.editorCanReload = field.canReload;

	if (initCallback) {
		initCallback.call(this);
	}
	
	var editor = null;
	var selector = null;
	var embedded = null;
	
	$scope.createNestedEditor = function() {
		return null;
	};

	/**
	 * Show/Hide the nested editor according to the show parameter, if
	 * undefined then toggle.
	 *
	 */
	$scope.showNestedEditor = function showNestedEditor(show) {
		if (!params.summaryView) {
			return;
		}
		if (embedded === null) {
			embedded = $scope.createNestedEditor();
		}
		var es = embedded.data('$scope');
		if (es !== null) {
			es.visible = (show === undefined ? !es.visible : show);
			embedded.toggle(es.visible);
		}
		return embedded;
	};
	
	$scope.showPopupEditor = function(record) {
		if (!record && this.isReadonly()) {
			return;
		}
		if (editor == null) {
			editor = ViewService.compile('<div ui-editor-popup></div>')($scope);
			editor.data('$target', $element);
		}
		
		var popup = editor.data('$scope');
		popup.show(record);
		if (record == null) {
			popup.ajaxStop(function() {
				popup.$broadcast("on:new");
				popup.applyLater();
			});
		}
	};
	
	function _showEditor(record) {
		
		if (field.editWindow === "blank") {
			var tab = {
				action: _.uniqueId('$act'),
				title: field.title,
				model: field.target,
				recordId: record ? record.id : null,
				views: [{
					type: 'form',
					name: field.formView
				}, {
					type: 'grid',
					name: field.gridView
				}]
			};
				
			return $scope.openTab(tab);
		}
		
		//TODO: handle other modes

		if ($scope.editorCanReload && record && record.id) {
			var parent = $scope.$parent;
			if (parent && parent.canSave()) {
				return parent.onSave().then(function(){
					$scope.showPopupEditor(record);
				});
			}
		}
		return $scope.showPopupEditor(record);
	};

	$scope.showEditor = function(record) {
		var perm = record ? "read" : "create";
		$scope.isPermitted(perm, record, function(){
			_showEditor(record);
		});
	};
	
	$scope.parentReload = function() {
		var parent = $scope.$parent;
		if (parent) {
			parent.reload();
		}
	};

	$scope.showSelector = function() {
		if (this.isReadonly()) {
			return;
		}
		function doShow() {
			if (selector == null) {
				selector = $('<div ui-selector-popup></div>').attr('x-select-mode', $scope.selectMode || "multi");
				selector = ViewService.compile(selector)($scope);
				selector.data('$target', $element);
			}
			var popup = selector.data('$scope');
			popup._domain = $scope._domain; // make sure that popup uses my domain (#1233)
			popup.show();
		}
		
		var onSelect = this.$events.onSelect;
		if (onSelect) {
			onSelect().then(function(){
				doShow();
			});
		} else {
			doShow();
		}
	};
	
	$scope.$on("on:edit", function(record){
		$scope._domain = field.domain;
		$scope._context = field.context;
	});
	
	$scope.setDomain = function(domain, context) {
		$scope._domain = domain;
		$scope._context = context;
	};
	
	$scope.getDomain = function() {
		return {
			_domain: $scope._domain,
			_context: $scope._context
		};
	};

	var fetchDS = (function () {
		var fds = null;
		return function () {
			if (fds) return fds;
			var ds = $scope._dataSource;
			return fds = DataSource.create(ds._model, {
				domain: ds._domain,
				context: ds._context
			});
		};
	})();

	$scope.fetchData = function(value, success) {
		
		var records = $.makeArray(value),
			ids = [];

		_.each(records, function(item) {
			if (_.isNumber(item)) {
				return ids.push(item);
			}
			if (_.isNumber(item.id) && item.id > 0 &&
				_.isUndefined(item.version) &&
				_.isUndefined(item.$fetched)) {
				return ids.push(item.id);
			}
		});

		if (ids.length == 0) {
			return success(value);
		}
		
		var criterion = {
			'fieldName': 'id',
			'operator': 'in',
			'value': ids
		};

		var fields = _.pluck($scope.fields, 'name');
		var filter = {
			operator: 'and',
			criteria: [criterion]
		};
		
		return $scope._viewPromise.then(function(view) {

			var sortBy = view.orderBy;
			
			if (sortBy) {
				sortBy = sortBy.split(",");
			}
			
			return fetchDS().search({
				filter: filter,
				fields: fields,
				sortBy: fetchDS()._sortBy || sortBy,
				archived: true,
				limit: -1,
				domain: null
			}).success(function(records, page){
				// only edited records should have version property
				var items = _.map(records, function(item){
					item.$version = item.version;
					item.$fetched = false;
					delete item.version;
					return item;
				});
				success(items, page);
			});
		
		});
	};
	
	$scope.fetchSelection = function(request, response) {
		var fn = fetchSelection.bind(this);
		var onSelect = this.$events.onSelect;
		if (onSelect) {
			return onSelect().then(function() {
				return fn(request, response);
			});
		}
		return fn(request, response);
	};

	function fetchSelection(request, response) {

		var field = this.field;
		var nameField = field.targetName || 'id',
			fields = field.targetSearch || [],
			filter = {};

		fields = ["id", nameField].concat(fields);
		fields = _.chain(fields).compact().unique().value();

		_.each(fields, function(name){
			if (name !== "id" && request.term) {
				filter[name] = request.term;
			}
		});
		
		var domain = this._domain,
			context = this._context;
	
		if (domain && this.getContext) {
			context = _.extend({}, context, this.getContext());
		}
	
		var params = {
			filter: filter,
			fields: fields,
			archived: true,
			limit: 6
		};

		if (domain) {
			params.domain = domain;
			params.context = context;
		}
	
		fetchDS().search(params).success(function(records, page){
			var items = _.map(records, function(record) {
				return {
					label: record[nameField],
					value: record
				};
			});
			response(items, page);
		});
	};

	$scope.canSelect = function() {
		if (field.canSelect !== undefined) return field.canSelect;
		if ($scope.selectEnable !== undefined) return $scope.selectEnable;
		return true;
	};

	$scope.canNew = function() {
		return _.isUndefined(field.canNew) ? true : field.canNew;
	};
	
	$scope.canEdit = function() {
		return !$scope.isReadonly();
	};

	$scope.canView = function() {
		return _.isUndefined(field.canView) ? true : field.canView;
	};

	$scope.canRemove = function() {
		return _.isUndefined(field.canRemove) ? true : field.canRemove;
	};

	$scope.select = function(value) {
		
	};

	$scope.onNew = function() {
		$scope.showEditor(null);
	};

	$scope.onEdit = function() {
		
	};
	
	$scope.onSelect = function() {
		$scope.showSelector();
	};
	
	$scope.onRemove = function() {
		
	};

	$scope.hasPermission = function(perm) {
		if (!field.perms) return true;
		var perms = field.perms;
		var permitted = perms[perm];
		if (!permitted) {
			return false;
		}
		return true;
	};
}

}).call(this);