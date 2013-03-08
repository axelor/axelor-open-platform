(function(){

var ui = angular.module('axelor.ui');

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
		if (!record && $scope.isReadonly($element)) {
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
		if ($scope.isReadonly($element)) {
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
		
		var onSelect = $element.data('$onSelect');
		if (onSelect) {
			onSelect.handle().then(function(){
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
	
	$scope.setValue = function(value, fireOnChange) {
		var model = $scope.ngModel,
			onChange = $element.data('$onChange');
		setTimeout(function(){
			$scope.$apply(function(){
				model.$setViewValue(value);
				model.$render();
				if (onChange && fireOnChange) {
					onChange.handle();
				}
			});
		});
	};

	$scope.getValue = function() {
		var model = $scope.ngModel;
		if (model != null)
			return model.$viewValue;
		return null;
	};
	
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

			var ds = $scope._dataSource;
			var sortBy = view.orderBy;
			
			if (sortBy) {
				sortBy = sortBy.split(",");
			}
			
			return ds.search({
				filter: filter,
				fields: fields,
				sortBy: ds._sortBy || sortBy,
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

ManyToOneCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function ManyToOneCtrl($scope, $element, DataSource, ViewService) {

	RefFieldCtrl.call(this, $scope, $element, DataSource, ViewService);

	var ds = $scope._dataSource,
		field = $scope.getViewDef($element),
		nameField = field.targetName || field.nameField || 'id';

	$scope.createNestedEditor = function() {
		
		var embedded = $('<div ui-nested-editor></div>')
			.attr('ng-model', $element.attr('ng-model'))
			.attr('name', $element.attr('name'))
			.attr('x-title', $element.attr('x-title'))
			.attr('x-path', $element.attr('x-path'));

		embedded = ViewService.compile(embedded)($scope);
		embedded.hide();
		
		var colspan = $element.parents("form.dynamic-form:first").attr('x-cols') || 4,
			cell = $('<td class="form-item"></td>').attr('colspan', colspan).append(embedded),
			row = $('<tr></tr>').append(cell);
		
		row.insertAfter($element.parents("tr:first"));
		
		return embedded;
	};
	
	$scope.select = function(value) {
		
		if (_.isArray(value)) {
			value = _.first(value);
		}
		
		var record = value;

		// fetch '.' names
		var path = $element.attr('x-path');
		var relatives = $element.parents().find('[x-field][x-path^="'+path+'."]').map(
				function(){
					return $(this).attr('x-path').replace(path+'.','');
				}).get();
		
		relatives = _.unique(relatives);
		if (relatives.length > 0 && value && value.id) {
			return ds.read(value.id, {
				fields: relatives
			}).success(function(rec){
				var record = { 'id' : value.id };
				record[nameField] = rec[nameField];
				_.each(relatives, function(name) {
					var prefix = name.split('.')[0];
					record[prefix] = rec[prefix];
				});
				$scope.setValue(record, true);
			});
		}
		// end fetch '.' names

		if (value && value.id) {
			record = { 'id' : value.id };
			record[nameField] = value[nameField];
			if (nameField && _.isUndefined(value[nameField])) {
				return ds.details(value.id).success(function(rec){
					$scope.setValue(rec, true);
				});
			}
		}
		
		$scope.setValue(record, true);
	};
	
	$scope.onEdit = function() {
		var record = $scope.getValue();
		$scope.showEditor(record);
	};
	
	$scope.onSummary = function() {
		var record = $scope.getValue();
		if (record && record.id) {
			return ds.read(record.id).success(function(record){
				$scope.showNestedEditor(record);
			});
		}
		$scope.showNestedEditor(record);
	};
}

OneToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function OneToManyCtrl($scope, $element, DataSource, ViewService, initCallback) {

	RefFieldCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
		GridViewCtrl.call(this, $scope, $element);
		$scope.editorCanSave = false;
		if (initCallback) {
			initCallback();
		}
	});
	
	var embedded = null,
		detailView = null;

	$scope.createNestedEditor = function() {

		embedded = $('<div ui-embedded-editor class="inline-form"></div>');
		embedded.attr('x-title', $element.attr('x-title'));
		embedded = ViewService.compile(embedded)($scope);
		embedded.hide();
		
		$element.append(embedded);
		embedded.data('$rel', $element.children('.slickgrid:first').children('.slick-viewport'));

		return embedded;
	};
	
	var _showNestedEditor = $scope.showNestedEditor;
	$scope.showNestedEditor = function(record) {
		_showNestedEditor(record);
		if (embedded) {
			embedded.data('$rel').hide();
			embedded.data('$scope').edit(record);
		}
		return embedded;
	};
	
	$scope.showDetailView = function() {
		if (detailView == null) {
			detailView = $('<div ui-embedded-editor class="detail-form"></div>').attr('x-title', $element.attr('x-title'));
			detailView = ViewService.compile(detailView)($scope);
			detailView.data('$rel', $());
			detailView.data('$scope').isDetailView = true;
			$element.after(detailView);
		}
		detailView.show();
	};
	
	$scope.select = function(value) {
		var items = value,
			records;
	
		if (!_.isArray(value)) {
			items = [value];
		}

		records = _.map($scope.dataView.getItems(), function(item){
			return _.clone(item);
		});

		_.each(items, function(item){
			item = _.clone(item);
			var find = _.find(records, function(rec){
				return rec.id && rec.id == item.id;
			});
			
			if (find)
				_.extend(find, item);
			else
				records.push(item);
		});
		
		_.each(records, function(rec){
			if (rec.id <= 0) rec.id = null;
		});
		
		$scope.setValue(records, true);
		setTimeout(function(){
			$scope.$broadcast('grid:changed');
		});
	};
	
	var _setItems = $scope.setItems;
	$scope.setItems = function(items) {
		_setItems(items);
		if (embedded !== null) {
			embedded.data('$scope').onClose();
		}
		if (detailView !== null)
		if (items === null || _.isEmpty(items))
			detailView.hide();
		else
			detailView.show();
	};
	
	$scope.removeItems = function(selection) {

		if (!selection || selection.length == 0)
			return;

		var dataView = $scope.dataView;
	
		var items = dataView.getItems();
		var records = [];
	
		for(var i = 0 ; i < items.length ; i++) {
			if (selection.indexOf(i) > -1)
				continue;
			records.push(items[i]);
		}

		$scope.setValue(records, true);
	};
	
	$scope.onEdit = function() {
		var selected = $scope.selection.length ? $scope.selection[0] : null;
		if (selected !== null) {
			var record = $scope.dataView.getItem(selected);
			$scope.showEditor(record);
		}
	};
	
	$scope.onRemove = function() {
		if ($scope.isReadonly($element)) {
			return;
		}
		axelor.dialogs.confirm(_t("Do you really want to delete the selected record(s)?"), function(confirmed){
			if (confirmed && $scope.selection && $scope.selection.length)
				$scope.removeItems($scope.selection);
		});
	};
	
	$scope.onSummary = function() {
		var selected = $scope.getSelectedRecord();
		if (selected) {
			$scope.showNestedEditor(selected);
		}
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
		if (detailView === null)
			return;
		setTimeout(function(){
			detailView.show();
			detailView.data('$scope').edit($scope.getSelectedRecord());
		});
	};
	
	$scope.filter = function() {
		
	};
	
	$scope.onSort = function(event, args) {
		
		//TODO: implement client side sorting (prevent losing O2M changes).
		if ($scope.isDirty() && !$scope.editorCanSave) {
			return;
		}

		var records = $scope.dataView.getItems();
		if (records == null || records.length == 0)
			return;

		var sortBy = [];
		
		angular.forEach(args.sortCols, function(column){
			var name = column.sortCol.field;
			var spec = column.sortAsc ? name : '-' + name;
			sortBy.push(spec);
		});
		
		var ids = _.pluck(records, 'id');
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
		
		$scope.selection = [];
		$scope._dataSource.search({
			filter: filter,
			fields: fields,
			sortBy: sortBy,
			archived: true,
			limit: -1,
			domain: null,
			context: null
		});
	};
	
	$scope.onShow = function(viewPromise) {

	};
	
	$scope.show();
}

ManyToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function ManyToManyCtrl($scope, $element, DataSource, ViewService) {

	OneToManyCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
		$scope.editorCanSave = true;
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

var ManyToOneItem = {
	css	: 'many2one-item',
	require: '?ngModel',
	scope: true,
	controller: ManyToOneCtrl,
	link: function(scope, element, attrs, model) {
		
		scope.ngModel = model;
		
		var field = scope.getViewDef(element),
			input = element.children('input:first'),
			nameField = field.targetName || field.nameField || 'id';
		
		scope.formPath = scope.formPath ? scope.formPath + "." + field.name : field.name;
		
		input.keydown(function(e){
			var handled = false;
			if (e.keyCode == 113) { // F2
				if (e.shiftKey) {
					scope.onNew();
				} else {
					scope.onEdit();
				}
				handled = true;
			}
			if (e.keyCode == 114) { // F3
				scope.onSelect();
				handled = true;
			}
			if (e.keyCode == 46) { // DELETE
				scope.select(null);
				handled = true;
			}
			if (!handled) {
				return;
			}
			setTimeout(function(){
				scope.$apply();
			});
			e.preventDefault();
			e.stopPropagation();
			return false;
		});
		
		model.$render = function() {
			var value = model.$viewValue;
			if (value) {
				value = value[nameField];
			}
			input.val(value);
		};

		var embedded = null;
		var readonly = false;
		if (field.widget == 'NestedEditor') {
			setTimeout(function(){
				embedded = scope.showNestedEditor();
				if (readonly) {
					scope.setReadonly(embedded, readonly);
				}
			});
		}

		attrs.$observe('readonly', function(value) {
			readonly = value;
			if (embedded) {
				scope.setReadonly(embedded, readonly);
			}
		});
		
		scope.setValidity = function(key, value) {
			model.$setValidity(key, value);
		};
		
		function search(request, response) {
			var fields = field.targetSearch || [],
				filter = {}, ds = scope._dataSource;

			fields = ["id", nameField].concat(fields);
			fields = _.chain(fields).compact().unique().value();

			_.each(fields, function(name){
				if (name !== "id" && request.term) {
					filter[name] = request.term;
				}
			});
			
			var domain = scope._domain,
				context = scope._context;

			if (domain && scope.getContext) {
				context = _.extend({}, context, scope.getContext());
			}

			var params = {
				filter: filter,
				fields: fields,
				archived: true,
				limit: 10
			};
			
			if (domain) {
				params.domain = domain;
				params.context = context;
			}

			ds.search(params).success(function(records, page){
				response(records);
			});
		}
		
		var onSelectFired = false;
		input.autocomplete({
			source: function(request, response) {
				var onSelect = element.data('$onSelect');
				if (onSelect && !onSelectFired) {
					onSelect.handle().then(function(){
						search(request, response);
					});
					onSelectFired = true;
				}
				else search(request, response);
			},
			focus: function(event, ui) {
				return false;
			},
			select: function(event, ui) {
				scope.select(ui.item);
				onSelectFired = false;
				return false;
			}
		}).data("autocomplete")._renderItem = function( ul, item ) {
			var label = item[nameField] || item.name || item.code || item.id;
			return $("<li><a>" + label  + "</a></li>")
				.data("item.autocomplete", item)
				.appendTo(ul);
		};
		
		var canSelect = _.isUndefined(field.canSelect) ? true : field.canSelect;
		setTimeout(function(){
			if (!canSelect) scope.setHidden(element);
		});
		
		if ((scope._viewParams || {}).summaryView) {
			element.removeClass('picker-icons-3').addClass('picker-icons-4');
		}

		scope.isDisabled = function() {
			return scope.isReadonly(element);
		};
	},
	template:
	'<div class="picker-input picker-icons-3">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-eye-open" ng-click="onSummary()" ng-show="hasPermission(\'read\') && _viewParams.summaryView"></i>'+
			'<i class="icon-pencil" ng-click="onEdit()" ng-show="hasPermission(\'read\')" title="{{\'Edit\' | t}}"></i>'+
			'<i class="icon-plus" ng-click="onNew()" ng-show="hasPermission(\'write\') && !isDisabled()" title="{{\'New\' | t}}"></i>'+
			'<i class="icon-search" ng-click="onSelect()" ng-show="hasPermission(\'read\') && !isDisabled()" title="{{\'Select\' | t}}"></i>'+
		'</span>'+
   '</div>'
};

var SuggestBox = _.extend({}, ManyToOneItem, {
	_linkOrig : ManyToOneItem.link,
	link: function(scope, element, attrs, model) {
		this._linkOrig(scope, element, attrs, model);
		var input = element.children(':input:first');
		input.autocomplete("option" , {
			minLength: 0
		});
		scope.showSelection = function() {
			if (scope.isReadonly(element)) {
				return;
			}
			input.autocomplete("search" , '');
		};
	},
	template:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
   '</span>'
});

var OneToManyItem = {
		css: 'one2many-item',
		transclude: true,
		showTitle: false,
		require: '?ngModel',
		scope: true,
		collapseIfEmpty: true,
		controller: OneToManyCtrl,
		link: function(scope, element, attrs, model) {
			
			scope.ngModel = model;
			scope.title = attrs.title;
			
			scope.formPath = scope.formPath ? scope.formPath + "." + attrs.name : attrs.name;
			
			var dummyId = 0;
			var adjusted = false;
			
			function ensureIds(records) {
				var items = [];
				angular.forEach(records, function(record){
					var item = angular.copy(record, {});
					if (item.id == null)
						item.id = --dummyId;
					items.push(item);
				});
				return items;
			};

			model.$render = function() {
				var items = scope.getValue();
				scope._viewPromise.then(function(){
					scope.fetchData(items, function(records){
						records =  ensureIds(records);
						scope.setItems(records);
						setTimeout(function(){
							if (adjusted) {
								return;
							}
							adjusted = true;
							if (scope.$popup) {
								scope.$popup.$broadcast('adjust:dialog');
							}
						});
					});
				});
			};
			
			if (this.collapseIfEmpty) {
				scope.$watch(attrs.ngModel, function(value){
					var minHeight = _.isEmpty(value) ? '' : 230;
					element.css('min-height', minHeight);
					if (minHeight) {
						$.event.trigger('adjustSize');
					}
				});
			}

			scope.onGridInit = function(grid) {
				var editable = grid.getOptions().editable;
				if (editable) {
					element.addClass('inline-editable');
					scope.$on('on:new', function(event){
						if (scope.dataView.getItemById(0)) {
							scope.dataView.deleteItem(0);
						}
						grid.setOptions({enableAddRow: true});
					});
				}
				
				element.on("on:attrs-change", function(event, data) {
					if (!editable || !data) return;
					grid.setOptions({editable: !data.readonly });
				});
				
				if (!(scope._viewParams || {}).summaryView) {
					return;
				}
				var col = {
					id: '_summary',
					name: '',
					sortable: false,
					resizable: false,
					width: 16,
					formatter: function(row, cell, value, columnDef, dataContext) {
						return '<i class="icon-caret-right" style="display: inline-block; cursor: pointer; padding: 1px 4px;"></i>';
					}
				};
				
				var cols = grid.getColumns();
				cols.splice(0, 0, col);
				
				grid.setColumns(cols);
				grid.onClick.subscribe(function(e, args) {
					if ($(e.target).is('.icon-caret-right'))
						setTimeout(function(){
							scope.onSummary();
						});
				});
			};
			
			scope.onGridBeforeSave = function(records) {
				if (!scope.editorCanSave) {
					if (scope.dataView.getItemById(0)) {
						scope.dataView.deleteItem(0);
					}
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
				return scope.isReadonly(element);
			};
			
			var field = scope.getViewDef(element);
			if (field.widget === 'MasterDetail') {
				setTimeout(function(){
					scope.showDetailView();
				});
			}
		},
		template:
		'<div class="stackbar">'+
		'<div class="navbar">'+
			'<div class="navbar-inner">'+
				'<div class="container-fluid">'+
					'<span class="brand" href="" ui-help-popover ng-bind-html-unsafe="title"></span>'+
					'<span class="icons-bar pull-right">'+
						'<i ng-click="onSelect()" ng-show="hasPermission(\'read\') && !isDisabled()" title="{{\'Select\' | t}}" class="icon-search"></i>'+
						'<i ng-click="onNew()" ng-show="hasPermission(\'write\') && !isDisabled()" title="{{\'New\' | t}}" class="icon-plus"></i>'+
						'<i ng-click="onEdit()" ng-show="hasPermission(\'read\')" title="{{\'Edit\' | t}}" class="icon-pencil"></i>'+
						'<i ng-click="onRemove()" ng-show="hasPermission(\'remove\') && !isDisabled()" title="{{\'Remove\' | t}}" class="icon-minus"></i>'+
					'</span>'+
				'</div>'+
			'</div>'+
		'</div>'+
		'<div ui-view-grid ' +
			'x-view="schema" '+
			'x-data-view="dataView" '+
			'x-handler="this" '+
			'x-no-filter="true" '+
			'x-on-init="onGridInit" '+
			'x-on-before-save="onGridBeforeSave" '+
			'x-on-after-save="onGridAfterSave" '+
			'></div>'+
		'</div>'
};

var ManyToManyItem = _.extend({}, OneToManyItem, {
	css	: 'many2many-item',
	controller: ManyToManyCtrl
});

var OneToManyInline = _.extend({}, OneToManyItem, {
	css	: 'one2many-inline',
	requires: '?ngModel',
	collapseIfEmpty : false,
	scope: true,
	link: function(scope, element, attrs, model) {
		OneToManyItem.link.apply(this, arguments);
		
		scope.onSort = function() {
			
		};
		
		var input = element.children('input');
		var grid = element.children('[ui-slick-grid]');
		
		var wrapper = $('<div class="slick-editor-dropdown"></div>')
			.css("position", "absolute")
			.hide();

		var render = model.$render,
			renderPending = false;
		model.$render = function() {
			if (wrapper.is(":visible")) {
				renderPending = false;
				render();
				grid.trigger('adjustSize');
			} else {
				renderPending = true;
			}
		};
		
		setTimeout(function(){
			var container = element.parents('.view-container');
			grid.height(175).appendTo(wrapper);
			wrapper.height(175).appendTo(container);
		});
		
		function adjust() {
			if (!wrapper.is(":visible"))
				return;
			wrapper.position({
				my: "left top",
				at: "left bottom",
				of: element,
				within: "#container"
			})
			.zIndex(element.zIndex() + 1)
			.width(element.width());
		}
		
		element.on("show:slick-editor", function(e){
			if (renderPending) {
				renderPending = false;
				render();
			}
			wrapper.show();
			adjust();
		});

		element.on("hide:slick-editor", function(e){
			wrapper.hide();
		});
		
		element.on("adjustSize", _.debounce(adjust, 300));
		
		scope.$watch(attrs.ngModel, function(value) {
			var text = "";
			if (value && value.length)
				text = "(" + value.length + ")";
			input.val(text);
		});
		
		scope.$on("$destroy", function(e){
			wrapper.remove();
		});
	},
	template:
	'<span class="picker-input picker-icons-2" style="position: absolute;">'+
		'<input type="text" readonly>'+
		'<span class="picker-icons">'+
			'<i class="icon-plus" ng-click="onNew()" ng-show="hasPermission(\'create\')" title="{{\'Select\' | t}}"></i>'+
			'<i class="icon-minus" ng-click="onRemove()" ng-show="hasPermission(\'remove\')" title="{{\'Select\' | t}}"></i>'+
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
	'</span>'
});

var ManyToManyInline = _.extend({}, OneToManyInline, {
	css	: 'many2many-inline',
	link: function(scope, element, attrs, model) {
		OneToManyInline.link.apply(this, arguments);
		scope.onNew = scope.onSelect;
	},
	controller: ManyToManyCtrl
});

var NestedForm = {
	scope: true,
	controller: [ '$scope', '$element', function($scope, $element) {
		
		FormViewCtrl.call(this, $scope, $element);
		
		$scope.onShow = function(viewPromise) {
			
		};
		
		$scope.registerNested($scope);
		$scope.show();
	}],
	link: function(scope, element, attrs, ctrl) {

	},
	template: '<div ui-view-form x-handler="this"></div>'
};

EmbeddedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function EmbeddedEditorCtrl($scope, $element, DataSource, ViewService) {
	
	var params = angular.copy($scope._viewParams);
	
	params.views = _.compact([params.summaryView]);
	$scope._viewParams = params;

	ViewCtrl($scope, DataSource, ViewService);
	FormViewCtrl.call(this, $scope, $element);

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
	};
	
	function doClose() {
		if ($scope.isDetailView) {
			$scope.edit($scope.getSelectedRecord());
			return;
		}
		$element.hide();
		$element.data('$rel').show();
	};
	
	$scope.edit = function(record) {
		doEdit(record);
	};

	$scope.onClose = function() {
		doClose();
	};
	
	$scope.onOK = function() {
		if (!$scope.isValid()) {
			return;
		}
		var record = $scope.record;
		if (record) record.$fetched = true;
		$scope.select(record);
		setTimeout(doClose);
	};
	
	$scope.onClear = function() {
		$scope.record = {};
	};
	
	$scope.$on('grid:changed', function(event) {
		var record = $scope.getSelectedRecord();
		if ($scope.isDetailView) {
			$scope.edit(record);
		}
	});
	
	$scope.show();
}

var EmbeddedEditor = {
		restrict: 'EA',
		css: 'nested-editor',
		scope: true,
		controller: EmbeddedEditorCtrl,
		template:
			'<fieldset class="form-item-group bordered-box">'+
				'<div ui-view-form x-handler="this"></div>'+
				'<div class="btn-toolbar pull-right">'+
					'<button type="button" class="btn btn-danger" ng-click="onClose()">Cancel</button> '+
					'<button type="button" class="btn btn-primary" ng-click="onOK()">OK</button>'+
				'</div>'+
			'</fieldset>'
};

NestedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function NestedEditorCtrl($scope, $element, DataSource, ViewService) {
	
	var params = $scope._viewParams;
	
	params.views = _.compact([params.summaryView]);
	$scope._viewParams = params;
	
	ManyToOneCtrl.call(this, $scope, $element, DataSource, ViewService);
	
	$scope.nested = null;
	$scope.registerNested = function(scope) {
		$scope.nested = scope;
	};
}

var NestedEditor = {
	restrict: 'EA',
	css: 'nested-editor',
	require: '?ngModel',
	scope: true,
	controller: NestedEditorCtrl,
	link: function(scope, element, attrs, model) {
		
		var configured = false,
			updateFlag = true;
		
		function setValidity(nested, valid) {
			setTimeout(function(){
				model.$setValidity('valid', nested.isValid());
				if (scope.setValidity) {
					scope.setValidity('valid', nested.isValid());
				}
				scope.$apply();
			});
		}
		
		function configure(nested) {
			
			//FIX: select on M2O doesn't apply to nested editor
			scope.$watch(attrs.ngModel + '.id', function(){
				setTimeout(function(){
					nested.$apply();
				});
			});

			nested.$watch('form.$valid', function(valid){
				setValidity(nested, valid);
			});
			nested.$watch('record', function(rec, old){
				if (updateFlag && rec != old) {
					if (_.isEmpty(rec)) {
						rec = null;
					} else {
						rec.$dirty = true;
					}
					if (rec) {
						model.$setViewValue(rec);
					}
				}
				updateFlag = true;
				setValidity(nested, nested.isValid());
			}, true);
		}
		
		scope.ngModel = model;
		
		scope.onClear = function() {
			model.$setViewValue(null);
			model.$render();
		};

		scope.onClose = function() {
			element.hide();
		};

		attrs.$observe('title', function(title){
			scope.title = title;
		});
		
		model.$render = function() {
			var nested = scope.nested,
				promise = nested._viewPromise,
				value = model.$viewValue;

			if (nested == null)
				return;
			
			if (!configured) {
				configured = true;
				promise.then(function(){
					configure(nested);
				});
			}
			if (value == null || !value.id || value.$dirty) {
				return nested.edit(value);
			}
			
			promise.then(function(){
				nested.doRead(value.id).success(function(record){
					updateFlag = false;
					nested.edit(record);
				});
			});
		};
	},
	template:
		'<fieldset class="form-item-group bordered-box">'+
			'<legend>'+
				'<span ng-bind-html-unsafe="title"></span> '+
				'<span class="legend-toolbar">'+
					'<a href="" tabindex="-1" ng-click="onClear()" title="{{\'Clear\' | t}}"><i class="icon-ban-circle"></i></a> '+
					'<a href="" tabindex="-1" ng-click="onSelect()" title="{{\'Select\' | t}}"><i class="icon-search"></i></a> '+
					'<a href="" tabindex="-1" ng-click="onClose()" title="{{\'Close\' | t}}"><i class="icon-remove-sign"></i></a>'+
				'</span>'+
			'</legend>'+
			'<div ui-nested-form></div>'+
		'</fieldset>'
};

//register directives
ui.formDirective('uiManyToOne', ManyToOneItem);
ui.formDirective('uiOneToMany', OneToManyItem);
ui.formDirective('uiManyToMany', ManyToManyItem);
ui.formDirective('uiNestedEditor', NestedEditor);
ui.formDirective('uiEmbeddedEditor', EmbeddedEditor);
ui.formDirective('uiNestedForm', NestedForm);
ui.formDirective('uiSuggestBox', SuggestBox);

ui.formDirective('uiOneToManyInline', OneToManyInline);
ui.formDirective('uiManyToManyInline', ManyToManyInline);

}).call(this);