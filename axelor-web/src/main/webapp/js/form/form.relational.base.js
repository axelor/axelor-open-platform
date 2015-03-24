/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
		params.summaryViewDefault = params.summaryView || views.form;
		
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

		if ($scope.editorCanReload && record && record.id) {
			var parent = $scope.$parent;
			if (parent && parent.canSave()) {
				var opts = {
					callOnSave: field.callOnSave
				};
				return parent.onSave(opts).then(function(){
					$scope.showPopupEditor(record);
				});
			}
		}
		return $scope.showPopupEditor(record);
	};

	$scope.showEditor = function(record) {
		var perm = record ? "read" : "create";
		if (perm === 'read' && !(record.id > 0)) {
			return _showEditor(record);
		}
		return $scope.isPermitted(perm, record, function(){
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
		var domain = ($scope.field||field).domain;
		var context = ($scope.field||field).context;
		if (domain !== undefined) $scope._domain = domain;
		if (context !== undefined) $scope._context = context;
	});
	
	$scope.setDomain = function(domain, context) {
		if (domain !== undefined) $scope._domain = domain;
		if (context !== undefined) $scope._context = context;
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
		
		function doFetch(view) {
			var sortBy = view.sortBy || view.orderBy;
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
		}

		if ($scope.isHidden()) {
			return doFetch($scope.view || {});
		}

		return $scope._viewPromise.then(function(view) {
			return doFetch(view || {});
		});
	};
	
	$scope.fetchSelection = function(request, response) {
		var fn = fetchSelection.bind(this);
		var onSelect = this.$events.onSelect;
		if (onSelect) {
			return onSelect(true).then(function() {
				return fn(request, response);
			});
		}
		return fn(request, response);
	};

	function fetchSelection(request, response) {

		var field = this.field;
		var nameField = field.targetName || 'id',
			fields = field.targetSearch || [],
			filter = {},
			limit = field.limit || 6,
			sortBy = field.orderBy;

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

		if (sortBy) {
			sortBy = sortBy.split(",");
		}

		var params = {
			filter: filter,
			fields: fields,
			archived: true,
			sortBy: sortBy,
			limit: limit
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
	
	$scope.createOnTheFly = function (term, popup, onSaveCallback) {

		var field = $scope.field;
		var targetFields = null;
		var requiredFields = (field.create||"").split(/,\s*/);
	
		function createItem(fields, term, popup) {
			var ds = $scope._dataSource,
				data = {}, missing = false;
	
			_.each(fields, function(field) {
				if (field.name === "name") return data["name"] = term;
				if (field.name === "code") return data["code"] = term;
				if (field.nameColumn) return data[field.name] = term;
				if (requiredFields.indexOf(field.name) > -1) {
					return data[field.name] = term;
				}
				if (field.required) {
					missing = true;
				}
			});
			if (popup || missing || _.isEmpty(data)) {
				return $scope.showPopupEditor(data);
			}
			return ds.save(data).success(onSaveCallback);
		}
		

		if (targetFields) {
			return createItem(targetFields, term, popup);
		}
		
		return $scope.loadView("form").success(function(fields, view){
			targetFields = fields;
			return createItem(fields, term, popup);
		});
	};
	
	$scope.attachTagEditor = function attachTagEditor(scope, element, attrs) {

		var field = scope.field;
		var input = null;
		
		if (!field.target) {
			return;
		}

		function onTagEdit(e, item) {

			var elem = $(e.target);
			var field = scope.field;
			var value = item[field.targetName];
			
			function onKeyDown(e) {

				// enter key
				if (e.keyCode === 13) {
					item[field.targetName] = input.val();
					saveAndSelect(item);
					hideEditor();
				}

				// escape
				if (e.keyCode === 27) {
					hideEditor();
				}
			}

			function hideEditor(forceSave) {
				
				$(document).off('mousedown.tag-editor');
				$(input).off('keydown.tag-editor').hide();
				
				if (forceSave && value !== input.val()) {
					item[field.targetName] = input.val();
					saveAndSelect(item);
				}
			}

			if (input === null) {
				input = $('<input class="tag-editor" type="text">').appendTo(element);
			}

			input.val(value)
				.width(element.width() - 6)
				.show().focus()
				.position({
					my: 'left top',
					at: 'left+3 top+3',
					of: element
				});

			$(input).on('keydown.tag-editor', onKeyDown);
			$(document).on('mousedown.tag-editor', function (e) {
				if (!input.is(e.target)) {
					hideEditor(true);
				}
			});
		}
		
		function saveAndSelect(record) {
        	var ds = scope._dataSource;
        	var data = _.extend({}, record, {
        		version: record.version || record.$version
        	});
        	ds.save(data).success(function (rec) {
        		scope.select(rec);
        	});
        }

		scope.onTagEdit = onTagEdit;
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

	var hasPermission = $scope.hasPermission;
	$scope.hasPermission = function(perm) {
		if (hasPermission && !hasPermission.apply($scope, arguments)) {
			return false;
		}
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