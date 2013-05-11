(function(){

var ui = angular.module('axelor.ui');

ui.RefFieldCtrl = RefFieldCtrl;

function RefFieldCtrl($scope, $element, DataSource, ViewService, initCallback) {

	var field = $scope.getViewDef($element),
		params = {
			model: field.target,
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
	
	$scope.showNestedEditor = function(record) {
		if (!params.summaryView) {
			return;
		}
		if (embedded === null) {
			embedded = $scope.createNestedEditor();
		}
		if (embedded !== null) {
			embedded.toggle();
		}
		return embedded;
	};
	
	$scope.showPopupEditor = function(record) {
		if (!record && this.isReadonly()) {
			return;
		}
		if (editor == null) {
			editor = ViewService.compile('<div ui-editor-popup></div>')($scope.$new());
			editor.data('$target', $element);
		}
		
		var popup = editor.data('$scope');
		popup.show();
		popup.edit(record);
		if (record == null) {
			popup.$broadcast("on:new");
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
				selector = ViewService.compile('<div ui-selector-popup></div>')($scope.$new());
				selector.data('$target', $element);
			}
			var popup = selector.data('$scope');
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

	var fetchDS = (function() {
		var ds = $scope._dataSource;
		var fds = DataSource.create(ds._model, {
			domain: ds._domain,
			context: ds._context
		});
		return fds;
	})();

	$scope.fetchData = function(value, success) {
		
		var records = $.makeArray(value),
			ids = _.chain(records).filter(function(item){
				return _.isNumber(item.id) && item.id > 0 &&
					   _.isUndefined(item.version) &&
					   _.isUndefined(item.$fetched);
			}).pluck('id').value();
		
		if (ids.length == 0) {
			return success(value);
		}
		
		var criterion = {
			'fieldName': 'id',
			'operator': 'inSet',
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
			
			return fetchDS.search({
				filter: filter,
				fields: fields,
				sortBy: fetchDS._sortBy || sortBy,
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
	
		fetchDS.search(params).success(function(records, page){
			var items = _.map(records, function(record) {
				return {
					label: record[nameField],
					value: record
				};
			});
			response(items);
		});
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