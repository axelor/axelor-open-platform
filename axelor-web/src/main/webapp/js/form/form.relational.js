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
			views[view.viewType] = view;
		});
		
		if (field.gridView) {
			views.grid = {
				type: 'grid',
				name: field.gridView
			};
		}
		if (field.formView) {
			views.form = {
				type: 'form',
				name: field.formView
			};
		}
		
		if (field.summaryView === "" || field.summaryView === "true") {
			params.summaryView = views.form;
		} else if (field.summaryView) {
			params.summaryView = {
				type: "form",
				name: field.summaryView
			};
		}

		params.views = _.compact([views.grid, views.form]);
		$scope._viewParams = params;
	}
	
	ViewCtrl($scope, DataSource, ViewService);
	
	$scope.ngModel = null;
	$scope.editorCanSave = true;

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
	};
	
	$scope.showPopupEditor = function(record) {
		if (editor == null) {
			editor = ViewService.compile('<div ui-editor-popup></div>')($scope.$new());
		}
		
		var popup = editor.data('$scope');
		popup.show();
		popup.edit(record);
		if (record == null) {
			popup.$broadcast("on:new");
		}
	};
	
	$scope.showEditor = function(record) {
		
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
		
		return $scope.showPopupEditor(record);
	};
	
	$scope.showSelector = function() {
		
		function doShow() {
			if (selector == null) {
				selector = ViewService.compile('<div ui-selector-popup></div>')($scope.$new());
			}
			var popup = selector.data('$scope');
			popup.show();
		}
		
		var onSelect = $element.data('$onSelect');
		if (onSelect) {
			onSelect._handle().then(function(){
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
					onChange._handle();
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
				return _.isNumber(item.id) && item.id > 0 && _.isUndefined(item.version);
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
		
		var ds = $scope._dataSource;
		var view = $scope._views ? $scope._views['grid'] || {} : {};
		var sortBy = view.orderBy;
		
		if (sortBy) {
			sortBy = sortBy.split("\\.");
		}
		
		var promise = ds.search({
			filter: filter,
			fields: fields,
			sortBy: ds._sortBy || sortBy,
			limit: -1,
			domain: null
		});
		promise.success(function(records, page){
			success(records, page);
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
			.attr('title', $element.attr('x-title'));

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
					return $(this).attr('name').replace(path+'.','');
				}).get();
		
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
		embedded.attr('title', $element.attr('x-title'));
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
	};
	
	$scope.showDetailView = function() {
		if (detailView == null) {
			detailView = $('<div ui-embedded-editor class="detail-form"></div>').attr('title', $element.attr('x-title'));
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

			var KEY = $.ui.keyCode;

			switch(e.keyCode) {
			case KEY.DELETE:
			//case KEY.BACKSPACE:
				scope.select(null);
			}
		});
		
		model.$render = function() {
			var value = model.$viewValue;
			if (value) {
				value = value[nameField];
			}
			input.val(value);
		};
		
		if (field.widget == 'NestedEditor') {
			setTimeout(function(){
				scope.showNestedEditor();
			});
		}
		
		// fix padding
		if (scope.summaryView == null) {
			input.css('padding-right', 84);
		}
		
		scope.setValidity = function(key, value) {
			model.$setValidity(key, value);
		};
		
		function search(request, response) {
			var fields = field.targetSearch || [],
				filter = {}, ds = scope._dataSource;

			fields = ["id", nameField].concat(fields);
			fields = _.chain(fields).compact().unique().value();

			_.each(fields, function(name){
				if (name !== "id") {
					filter[name] = request.term;
				}
			});

			ds.search({
				filter: filter,
				fields: fields,
				limit: 10
			}).success(function(records, page){
				response(records);
			});
		}
		
		var onSelectFired = false;
		input.autocomplete({
			source: function(request, response) {
				var onSelect = element.data('$onSelect');
				if (onSelect && !onSelectFired) {
					onSelect._handle().then(function(){
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
	},
	template:
	'<div class="input-append">'+
		'<input type="text" autocomplete="off">'+
		'<button class="btn" type="button" tabindex="-1" ng-click="onSummary()" ng-show="_viewParams.summaryView"><i class="icon-eye-open"></i></button>'+
		'<button class="btn" type="button" tabindex="-1" ng-click="onEdit()"><i class="icon-pencil"></i></button>'+
		'<button class="btn" type="button" tabindex="-1" ng-click="onNew()"><i class="icon-plus"></i></button>'+
		'<button class="btn" type="button" tabindex="-1" ng-click="onSelect()"><i class="icon-search"></i></button>'+
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
			input.autocomplete("search" , '');
		};
	},
	template:
	'<div class="input-append">'+
		'<input type="text" autocomplete="off">'+
		'<button class="btn" type="button" tabindex="-1" ng-click="showSelection()"><i class="icon-caret-down"></i></button>'+
   '</div>'
});

var OneToManyItem = {
		css: 'one2many-item',
		transclude: true,
		showTitle: false,
		require: '?ngModel',
		scope: true,
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
			
			scope.$watch(attrs.ngModel, function(value){
				element.css('min-height', value && value.length ? 200 : '');
			});
			
			scope.onGridInit = function(grid) {
				if (!scope._viewParams.summaryView)
					return;

				var el = element.find('.slickgrid:first'),
					grid = el.data('grid');

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
					'<span class="brand" href="" ng-bind-html-unsafe="title"></span>'+
					'<div class="btn-group pull-right">'+
						'<a href="" tabindex="-1" ng-click="onSelect()"><i class="icon-search"></i></a>'+
						'<a href="" tabindex="-1" ng-click="onNew()"><i class="icon-plus"></i></a>'+
						'<a href="" tabindex="-1" ng-click="onEdit()"><i class="icon-pencil"></i></a>'+
						'<a href="" tabindex="-1" ng-click="onRemove()"><i class="icon-minus"></i></a>'+
					'</div>'+
				'</div>'+
			'</div>'+
		'</div>'+
		'<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-no-filter="true" x-on-init="onGridInit"></div>'+
		'</div>'
};

var ManyToManyItem = _.extend({}, OneToManyItem, {
	css	: 'many2many-item',
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
		
		function configure(nested) {
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
				setTimeout(function(){
					model.$setValidity('valid', nested.isValid());
					if (scope.setValidity) {
						scope.setValidity('valid', nested.isValid());
					}
					scope.$apply();
				});
			}, true);
		}
		
		scope.ngModel = model;
		
		scope.onClear = function() {
			model.$viewValue = null;
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
				ds = scope._dataSource,
				value = model.$viewValue;

			if (nested == null)
				return;
			
			if (!configured) {
				configured = true;
				nested._viewPromise.then(function(){
					configure(nested);
				});
			}
			if (value == null || !value.id || value.$dirty) {
				return nested.edit(value);
			}
			
			ds.read(value.id).success(function(record){
				updateFlag = false;
				nested.edit(record);
			});
		};
	},
	template:
		'<fieldset class="form-item-group bordered-box">'+
			'<legend>'+
				'<span ng-bind-html-unsafe="title"></span> '+
				'<span class="legend-toolbar">'+
					'<a href="" tabindex="-1" ng-click="onClear()"><i class="icon-ban-circle"></i></a> '+
					'<a href="" tabindex="-1" ng-click="onSelect()"><i class="icon-search"></i></a> '+
					'<a href="" tabindex="-1" ng-click="onClose()"><i class="icon-remove-sign"></i></a>'+
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

}).call(this);