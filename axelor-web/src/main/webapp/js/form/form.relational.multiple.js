/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

ui.OneToManyCtrl = OneToManyCtrl;
ui.OneToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function OneToManyCtrl($scope, $element, DataSource, ViewService, initCallback) {

	ui.RefFieldCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
		ui.GridViewCtrl.call(this, $scope, $element);
		$scope.editorCanSave = false;
		$scope.selectEnable = false;
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
	$scope.showNestedEditor = function(show, record) {
		_showNestedEditor(show, record);
		if (embedded) {
			embedded.data('$rel').hide();
			var formScope = embedded.scope();
			formScope._viewPromise.then(function () {
				formScope.edit(record);
			});
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
		var es = detailView.data('$scope');
		detailView.toggle(es.visible = !es.visible);
	};
	
	$scope.select = function(value) {

		// if items are same, no need to set values
		if ($scope._dataSource.equals(value, $scope.getValue())) {
			return;
		}

		var items = _.chain([value]).flatten(true).compact().value();
		var records = _.map($scope.getItems(), _.clone);

		_.each($scope.itemsPending, function (item) {
			var find = _.find(records, function(rec) {
				if (rec.id && rec.id == item.id) {
					return true;
				}
				var a = _.omit(item, 'id', 'version');
				var b = _.omit(rec, 'id', 'version');
				return $scope._dataSource.equals(a, b);
			});
			if (!find) {
				records.push(item);
			}
		});

		_.each(items, function(item){
			item = _.clone(item);
			var find = _.find(records, function(rec){
				return rec.id && rec.id == item.id;
			});
			
			if (find) {
				_.extend(find, item);
			} else {
				records.push(item);
			}
		});
		
		_.each(records, function(rec){
			if (rec.id <= 0) rec.id = null;
		});
		
		if ($scope.dataView.$resequence) {
			$scope.dataView.$resequence(records);
		}

		var callOnChange = $scope.dataView.$isResequencing !== true;

		$scope.itemsPending = records;
		$scope.setValue(records, callOnChange);
		$scope.applyLater(function(){
			$scope.$broadcast('grid:changed');
		});
	};
	
	var _setItems = $scope.setItems;
	$scope.setItems = function(items) {
		_setItems(items);
		$scope.itemsPending = [];
		if (embedded !== null) {
			embedded.data('$scope').onClose();
		}
		if (detailView !== null)
		if (items === null || _.isEmpty(items))
			detailView.hide();
		else
			detailView.show();
	};
	
	$scope.removeItems = function(items, fireOnChange) {
		var all, ids, records;
		
		if (_.isEmpty(items)) return;
		
		all = _.isArray(items) ? items : [items];
		
		ids = _.map(all, function(item) {
			return _.isNumber(item) ? item : item.id;
		});

		records = _.filter($scope.getItems(), function(item) {
			return ids.indexOf(item.id) === -1;
		});
		
		$scope.setValue(records, fireOnChange);
		$scope.applyLater();
	};

	$scope.removeSelected = function(selection) {
		var selected, items;
		if (_.isEmpty(selection)) return;
		selected = _.map(selection, function (i) {
			return $scope.dataView.getItem(i).id;
		});
		items = _.filter($scope.getItems(), function (item) {
			return selected.indexOf(item.id) === -1;
		});
		// remove selected from data view
		_.each(selected, function (id) {
			$scope.dataView.deleteItem(id);
		});
		$scope.setValue(items, true);
		$scope.applyLater();
	};

	$scope.canEditTarget = function () {
		return $scope.canEdit() && $scope.attr('canEdit') !== false;
	};

	$scope.canShowEdit = function () {
		var selected = $scope.selection.length ? $scope.selection[0] : null;
		return selected !== null && $scope.canView();
	};
	
	$scope.canEdit = function () {
		return $scope.attr('canEdit') !== false && $scope.canView();
	};
	
	var _canRemove = $scope.canRemove;
	$scope.canRemove = function () {
		var selected = $scope.selection.length ? $scope.selection[0] : null;
		return _canRemove() && selected !== null;
	};

	$scope.canCopy = function () {
		if (!$scope.field || !($scope.field.widgetAttrs||{}).canCopy) return false;
		if (!$scope.canNew()) return false;
		if (!$scope.selection || $scope.selection.length !== 1) return false;
		var record = $scope.dataView.getItem(_.first($scope.selection));
		return record && record.id > -1;
	};

	$scope.onCopy = function() {
		var ds = $scope._dataSource;
		var index = _.first($scope.selection);
		var item = $scope.dataView.getItem(index);
		if (item && item.id > 0) {
			ds.copy(item.id).success(function(record) {
				$scope.select([record]);
			});
		}
	};

	$scope.onEdit = function() {
		var selected = $scope.selection.length ? $scope.selection[0] : null;
		if (selected !== null) {
			var record = $scope.dataView.getItem(selected);
			$scope.showEditor(record);
		}
	};
	
	$scope.onRemove = function() {
		if (this.isReadonly()) {
			return;
		}
		axelor.dialogs.confirm(_t("Do you really want to delete the selected record(s)?"), function(confirmed){
			if (confirmed && $scope.selection && $scope.selection.length)
				$scope.removeSelected($scope.selection);
		});
	};
	
	$scope.onSummary = function() {
		var selected = $scope.getSelectedRecord();
		if (selected) {
			$scope.showNestedEditor(true, selected);
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

		var field = $scope.field,
			record = $scope.record || {},
			selection = $scope.selection || [];

		record.$many = record.$many || (record.$many = {});
		record.$many[field.name] = selection.length ? $scope.getItems : null;

		if (detailView === null) {
			return;
		}

		$scope.$timeout(function() {
			var dvs = detailView.scope();
			var rec = $scope.getSelectedRecord() || {};
			detailView.show();
			if (!dvs.record || (dvs.record.id !== rec.id)) {
				dvs.edit(rec);
			}
		});
	};
	
	$scope.onItemDblClick = function(event, args) {
		if($scope.canView()){
			$scope.onEdit();
			$scope.$apply();
		}
	};

	(function (scope) {

		var dummyId = 0;

		function ensureIds(records) {
			var items = [];
			angular.forEach(records, function(record){
				var item = angular.copy(record, {});
				if (item.id == null)
					item.id = --dummyId;
				items.push(item);
			});
			return items;
		}

		function fetchData() {
			var items = scope.getValue();
			return scope.fetchData(items, function(records){
				records =  ensureIds(records);
				scope.setItems(records);
			});
		}

		scope.$$fetchData = fetchData;

	})($scope);

	$scope.reload = function() {
		return $scope.$$fetchData();
	};

	$scope.filter = function() {
		
	};
	
	$scope.onSort = function(event, args) {
		
		//TODO: implement client side sorting (prevent losing O2M changes).
		if ($scope.isDirty() && !$scope.editorCanSave) {
			return;
		}

		var records = $scope.getItems();
		if (records == null || records.length === 0)
			return;

		for (var i = 0; i < records.length; i++) {
			var item = records[i];
			if (!item.id || item.id <= 0) {
				return;
			}
		}

		var sortBy = [];
		
		angular.forEach(args.sortCols, function(column){
			var name = column.sortCol.field;
			var spec = column.sortAsc ? name : '-' + name;
			sortBy.push(spec);
		});
		
		var ids = _.pluck(records, 'id');
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

ui.ManyToManyCtrl = ManyToManyCtrl;
ui.ManyToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function ManyToManyCtrl($scope, $element, DataSource, ViewService) {

	OneToManyCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
		$scope.editorCanSave = true;
		$scope.selectEnable = true;
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

ui.formInput('OneToMany', {
	
	css: 'one2many-item',
	
	transclude: true,
	
	showTitle: false,
	
	collapseIfEmpty: true,
	
	controller: OneToManyCtrl,
	
	link: function(scope, element, attrs, model) {

		scope.ngModel = model;
		scope.title = attrs.title;
		
		scope.formPath = scope.formPath ? scope.formPath + "." + attrs.name : attrs.name;
		
		var doRenderUnwatch = null;
		var doViewPromised = false;

		function doRender() {
			if (doRenderUnwatch) {
				return;
			}
			doRenderUnwatch = scope.$watch(function () {
				if (!isVisible() || !doViewPromised) {
					return;
				}
				doRenderUnwatch();
				doRenderUnwatch = null;
					scope.$$fetchData();
				});
		}
		
		function isVisible() {
			return !element.is(':hidden');
		}

		scope._viewPromise.then(function () {
			doViewPromised = true;
			if (doRenderUnwatch) {
				doRenderUnwatch();
				doRenderUnwatch = null;
				doRender();
			}
			});

		model.$render = doRender;

		var adjustHeight = false;
		var adjustSize = (function() {
			var rowSize = 26;
			var	minSize = 56;
			var elem = element;
			if (elem.is('.panel-related')) {
				elem = element.children('.panel-body');
				minSize = 28;
			} else if (scope.$hasPanels) {
				minSize += 28;
			}
			if (elem.is('.picker-input')) {
				elem = null;
			}

			var inc = 0;
			var maxSize = (rowSize * 10) + minSize;

			return function(value) {
				inc = arguments[1] || inc;
				var count = _.size(value) + inc, height = minSize;
				if (count > 0) {
					height = (rowSize * count) + (minSize + rowSize);
				}
				if (elem && adjustHeight) {
					elem.css('min-height', Math.min(height, maxSize));
				}
				axelor.$adjustSize();
			};
		})();

		var collapseIfEmpty = this.collapseIfEmpty;
		scope.$watch(attrs.ngModel, function(value){
			if (!value) {
				// clear data view
				scope.dataView.setItems([]);
			}
			if (collapseIfEmpty) {
				adjustSize(value);
			}
		});

		function deleteItemsById(id) {
			var items = scope.dataView.getItems() || [];
			while (items.length > 0) {
				var item = _.findWhere(items, {id: id});
				var index = _.indexOf(items, item);
				if (index === -1) {
					break;
				}
				items.splice(index, 1);
			}
			return items;
		}

		scope.onGridInit = function(grid, inst) {
			var editIcon = scope.canView() || (!scope.isReadonly() && scope.canEdit());
			var editable = grid.getOptions().editable && !axelor.device.mobile;

			adjustHeight = true;

			if (editable) {
				element.addClass('inline-editable');
				scope.$on('on:new', function(event){
					var items = deleteItemsById(0);
					if (items.length === 0) {
						scope.dataView.setItems([]);
						grid.setSelectedRows([]);
					}
				});
				scope.$watch("isReadonly()", function(readonly) {
					grid.setOptions({
						editable: !readonly && scope.canEdit()
					});
					
					var _editIcon = scope.canView() || (!readonly && scope.canEdit());
					if (_editIcon != editIcon) {
						inst.showColumn('_edit_column', editIcon = _editIcon);
					}
				});
				
				adjustSize(scope.getValue(), 1);
			} else {
				adjustSize(scope.getValue());
			}

			inst.showColumn('_edit_column', editIcon);

			grid.onAddNewRow.subscribe(function (e, args) {
				var items = scope.getValue() || [];
				var rows = grid.getDataLength();
				adjustSize(items, rows - items.length + 1);
			});

			if (!(scope._viewParams || {}).summaryView || scope.field.widget === "MasterDetail") {
				return;
			}
			var col = {
				id: '_summary',
				name: '<span>&nbsp;</span>',
				sortable: false,
				resizable: false,
				width: 16,
				formatter: function(row, cell, value, columnDef, dataContext) {
					return '<i class="fa fa-caret-right" style="display: inline-block; cursor: pointer; padding: 4px 10px;"></i>';
				}
			};
			
			var cols = grid.getColumns();
			cols.splice(0, 0, col);
			
			grid.setColumns(cols);
			grid.onClick.subscribe(function(e, args) {
				if ($(e.target).is('.fa-caret-right'))
					scope.$timeout(function(){
						grid.setSelectedRows([args.row]);
						grid.setActiveCell(args.row, args.cell);
						scope.$timeout(function () {
							scope.onSummary();
						});
					});
			});
		};
		
		scope.onGridBeforeSave = function(records) {
			if (!scope.editorCanSave) {
				deleteItemsById(0);
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
			return this.isReadonly();
		};

		var field = scope.field;
		if (field.widget === 'MasterDetail') {
			setTimeout(function(){
				scope.showDetailView();
			});
		}

		attrs.$observe('title', function(title){
			scope.title = title;
		});
	},
	
	template_editable: null,
	
	template_readonly: null,
	
	template:
	'<div class="stackbar">'+
	'<div class="navbar">'+
		'<div class="navbar-inner">'+
			'<div class="container-fluid">'+
				'<span class="brand" href="" ui-help-popover ng-bind-html="title"></span>'+
				'<span class="icons-bar pull-right" ng-show="!isReadonly()">'+
					'<i ng-click="onEdit()" ng-show="hasPermission(\'read\') && canShowEdit()" title="{{\'Edit\' | t}}" class="fa fa-pencil"></i>'+
					'<i ng-click="onNew()" ng-show="hasPermission(\'write\') && !isDisabled() && canNew()" title="{{\'New\' | t}}" class="fa fa-plus"></i>'+
					'<i ng-click="onRemove()" ng-show="hasPermission(\'remove\') && !isDisabled() && canRemove()" title="{{\'Remove\' | t}}" class="fa fa-minus"></i>'+
					'<i ng-click="onSelect()" ng-show="hasPermission(\'read\') && !isDisabled() && canSelect()" title="{{\'Select\' | t}}" class="fa fa-search"></i>'+
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
});

ui.formInput('ManyToMany', 'OneToMany', {
	
	css	: 'many2many-item',
	
	controller: ManyToManyCtrl
});

var panelRelatedTemplate = 
"<div class='panel panel-related'>" +
	"<div class='panel-header'>" +
		"<div class='icons-bar pull-right' ng-show='!isReadonly()'>" +
			"<i ng-click='onEdit()' ng-show='hasPermission(\"read\") && canShowEdit()' title='{{\"Edit\" | t}}' class='fa fa-pencil'></i>" +
			"<i ng-click='onNew()' ng-show='hasPermission(\"create\") && !isDisabled() && canNew()' title='{{\"New\" | t}}' class='fa fa-plus'></i>" +
			"<i ng-click='onCopy()' ng-show='hasPermission(\"create\") && !isDisabled() && canCopy()' title='{{\"Duplicate\" | t}}' class='fa fa-files-o'></i>" +
			"<i ng-click='onRemove()' ng-show='hasPermission(\"read\") && !isDisabled() && canRemove()' title='{{\"Remove\" | t}}' class='fa fa-minus'></i>" +
			"<i ng-click='onSelect()' ng-show='hasPermission(\"read\") && !isDisabled() && canSelect()' title='{{\"Select\" | t}}' class='fa fa-search'></i>" +
		"</div>" +
		"<div class='panel-title'><span ui-help-popover ng-bind-html='title'></span></div>" +
	"</div>" +
	"<div class='panel-body panel-layout'>" +
		"<div ui-view-grid " +
			"x-view='schema' " +
			"x-data-view='dataView' " +
			"x-handler='this' " +
			"x-no-filter='true' " +
			"x-on-init='onGridInit' " +
			"x-on-before-save='onGridBeforeSave' " +
			"x-on-after-save='onGridAfterSave'></div>" +
	"</div>" +
"</div>";

ui.formInput('PanelOneToMany', 'OneToMany', {
	template_editable: null,
	template_readonly: null,
	template: panelRelatedTemplate
});

ui.formInput('PanelManyToMany', 'ManyToMany', {
	template_editable: null,
	template_readonly: null,
	template: panelRelatedTemplate
});

ui.formInput('TagSelect', 'ManyToMany', 'MultiSelect', {

	css	: 'many2many-tags',

	showTitle: true,
	
	init: function(scope) {
		this._super(scope);

		var nameField = scope.field.targetName || 'id';
		
		scope.parse = function(value) {
			return value;
		};

		scope.formatItem = function(item) {
			return item ? item[nameField] : item;
		};

		scope.getItems = function() {
			return _.pluck(this.getSelection(), "value");
		};
		
		var _select = scope.select;
		scope.select = function (value) {
			var res = _select.apply(scope, arguments);
			scope.itemsPending = [];
			return res;
		};

		scope.handleClick = function(e, item) {
			if (scope.field['tag-edit'] && scope.onTagEdit && !scope.isReadonly()) {
				return scope.onTagEdit(e, item);
			}
        	scope.showEditor(item);
        };
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		
		var input = this.findInput(element);
		var field = scope.field;

        function create(term, popup) {
			scope.createOnTheFly(term, popup, function (record) {
				scope.select(record);
				setTimeout(function() {
					input.focus();
				});
			});
		}

        scope.loadSelection = function(request, response) {

			if (!scope.canSelect()) {
				return response([]);
			}

			this.fetchSelection(request, function(items, page) {
				var term = request.term;
				var canSelect = scope.canSelect() && (items.length < page.total || (request.term && items.length === 0));
				if (field.create && term && scope.canNew()) {
					items.push({
						label : _t('Create "{0}" and select...', '<strong><em>' + term + '</em></strong>'),
						click : function() { create(term); }
					});
					items.push({
						label : _t('Create "{0}"...', '<strong><em>' + term + '</em></strong>'),
						click : function() { create(term, true); }
					});
				}
				if (canSelect) {
					items.push({
						label : _t("Search more..."),
						click : function() { scope.showSelector(); }
					});
				}
				if ((field.create === undefined || (field.create && !term)) && scope.canNew()) {
					items.push({
						label: _t("Create..."),
						click: function() { scope.showPopupEditor(); }
					});
				}
				response(items);
			});
		};

		scope.matchValues = function(a, b) {
			if (a === b) return true;
			if (!a) return false;
			if (!b) return false;
			return a.id === b.id;
		};

		var _setValue = scope.setValue;
		scope.setValue = function(value, fireOnChange) {
			var items = _.map(value, function(item) {
				if (item.version === undefined) {
					return item;
				}
				var ver = item.version;
				var val = _.omit(item, "version");
				val.$version = ver;
				return val;
			});
			items = _.isEmpty(items) ? null : items;
			return _setValue.call(this, items, fireOnChange);
		};
		
		var _handleSelect = scope.handleSelect;
		scope.handleSelect = function(e, ui) {
			if (ui.item.click) {
				setTimeout(function(){
					input.val("");
				});
				ui.item.click.call(scope);
				return scope.applyLater();
			}
			return _handleSelect.apply(this, arguments);
		};

		var _removeItem = scope.removeItem;
		scope.removeItem = function(e, ui) {
			if (scope.attr('canRemove') === false) return;
			_removeItem.apply(this, arguments);
		};

		if (scope.field && scope.field['tag-edit']) {
			scope.attachTagEditor(scope, element, attrs);
		}
	}
});

ui.InlineOneToManyCtrl = InlineOneToManyCtrl;
ui.InlineOneToManyCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function InlineOneToManyCtrl($scope, $element, DataSource, ViewService) {

	var field = $scope.field || $scope.getViewDef($element);
	var params = {
		model: field.target
	};

	if (!field.editor) {
		throw "No editor defined.";
	}

	params.views = [{
		type: 'grid',
		items: field.editor.items
	}];

	$scope._viewParams = params;

	OneToManyCtrl.call(this, $scope, $element, DataSource, ViewService, function(){
		$scope.editorCanSave = false;
		$scope.selectEnable = false;
	});
}

// used in panel form
ui.formInput('InlineOneToMany', 'OneToMany', {

	showTitle: true,

	controller: InlineOneToManyCtrl,

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		scope.onGridInit = function() {};
		scope.items = [];

		var showOnNew = (scope.field.editor||{}).showOnNew !== false;
		var unwatch = null;

		function canAdd() {
			return scope.hasPermission("write") && !scope.isDisabled() && scope.canNew();
		}

		model.$render = function () {
			if (unwatch) {
				unwatch();
				unwatch = null;
			}
			scope.items = model.$viewValue;
			if ((scope.items && scope.items.length > 0) || scope.$$readonly) {
				return;
			}
			scope.items = showOnNew && canAdd() ? [{}] : [];
			unwatch = scope.$watch('items[0]', function (item, old) {
				if (!item) return;
				if (item.$changed) {
					unwatch();
					model.$setViewValue(scope.items);
				}
				item.$changed = true;
			}, true);
		};
		
		function isEmpty(record) {
			if (!record || _.isEmpty(record)) return true;
			var values = _.filter(record, function (value, name) {
				return !(/[\$_]/.test(name) || value === null || value === undefined);
			});
			return values.length === 0;
		}

		scope.$watch('items', function (items, old) {
			if (!items || items.length === 0) return;
			var changed = false;
			var values = _.filter(items, function (item) {
				if (item.$changed) {
					changed = true;
				}
				return !isEmpty(item);
			});
			if (changed) {
				model.$setViewValue(values);
			}
		}, true);

		scope.$watch('$$readonly', function (readonly, old) {
			if (readonly === undefined) return;
			var items = model.$viewValue;
			if (_.isEmpty(items)) {
				scope.items = (showOnNew && canAdd() && !readonly) ? [{}] : items;
			}
		});

		scope.addItem = function () {
			var items = scope.items;
			var item = _.last(items);
			if (items && items.length && isEmpty(item)) {
				return;
			}
			if (canAdd()) {
				items.push({});
			}
		};

		scope.removeItem = function (index) {
			var items = scope.items;
			items.splice(index, 1);
			var values = _.filter(items, function (item) {
				return !isEmpty(item);
			});
			if (items.length === 0) {
				scope.addItem();
			}
			model.$setViewValue(values);
		};

		scope.canRemove = function () {
			return scope.attr('canRemove') !== false;
		}

		scope.setValidity = function (key, value) {
			model.$setValidity(key, value);
		};

		scope.setExclusive = function (name, record) {
			_.each(scope.items, function (item) {
				if (record !== item) {
					item[name] = false;
				}
			});
		};
	},

	template_readonly:function (scope) {
		var field = scope.field;
		var tmpl = field.viewer;
		if (!tmpl && field.editor && (field.editor.viewer || !field.targetName)) {
			tmpl = '<div class="o2m-editor-form" ui-panel-editor></div>';
		}
		if (!tmpl && field.targetName) {
			tmpl = '{{record.' + field.targetName + '}}';
		}
		tmpl = tmpl || '{{record.id}}';
		return "<div class='o2m-list'>" +
		"<div class='o2m-list-row' ng-class-even=\"'even'\" ng-repeat='record in items'>" + tmpl + "</div>" +
		"</div>";
	},

	template_editable: function (scope) {
		return "<div class='o2m-list'>" +
			"<div class='o2m-list-row' ng-class-even=\"'even'\" ng-repeat='record in items'>" +
				"<div class='o2m-editor-form' ui-panel-editor></div>" +
				"<span class='o2m-list-remove' ng-show='hasPermission(\"remove\") && !isDisabled() && canRemove()'>" +
					"<a tabindex='-1' href='' ng-click='removeItem($index)' title='{{\"Remove\" | t}}'><i class='fa fa-times'></i></a>" +
				"</span>" +
			"</div>" +
			"<div class='o2m-list-row o2m-list-add' ng-show='hasPermission(\"write\") && !isDisabled() && canNew()'>" +
				"<a tabindex='-1' href='' ng-click='addItem()'  title='{{\"Add\" | t}}'><i class='fa fa-plus'></i></a>" +
			"</div>" +
		"</div>";
	},
	template: null
});

//used in panel form
ui.formInput('InlineManyToMany', 'InlineOneToMany', {

});

// used in editable grid
ui.formInput('OneToManyInline', 'OneToMany', {

	css	: 'one2many-inline',

	collapseIfEmpty : false,
	
	link: function(scope, element, attrs, model) {
		
		this._super.apply(this, arguments);

		scope.onSort = function() {
			
		};
		
		var field = scope.field;
		var input = element.children('input');
		var grid = element.children('[ui-slick-grid]');
		
		var container = null;
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
			container = element.parents('.ui-dialog-content,.view-container').first();
			grid.height(175).appendTo(wrapper);
			wrapper.height(175).appendTo(container);
		});
		
		function adjust() {
			if (!wrapper.is(":visible"))
				return;
			if (axelor.device.small) {
				dropdownVisible = false;
				return wrapper.hide();
			}
			wrapper.position({
				my: "left top",
				at: "left bottom",
				of: element,
				within: container
			})
			.zIndex(element.zIndex() + 1)
			.width(element.width());
		}
		
		var dropdownVisible = false;
		scope.onDropdown = function () {
			dropdownVisible = !dropdownVisible;
			if (!dropdownVisible) {
				return wrapper.hide();
			}
			if (renderPending) {
				renderPending = false;
				render();
				setTimeout(function () {
					axelor.$adjustSize();
				});
			}
			wrapper.show();
			adjust();
		};

		scope.canDropdown = function () {
			return !axelor.device.small;
		};

		scope.canShowAdd = function () {
			return dropdownVisible && scope.canEdit();
		};

		scope.canShowRemove = function () {
			return dropdownVisible && scope.canRemove() && !_.isEmpty(scope.selection);
		};

		element.on("hide:slick-editor", function(e){
			dropdownVisible = false;
			wrapper.hide();
		});
		
		element.on("adjustSize", _.debounce(adjust, 300));
		
		input.on('keydown', function (e) {
			if (e.keyCode === 40 && e.ctrlKey && !dropdownVisible) {
				scope.onDropdown();
			}
		});

		function hidePopup(e) {
			if (element.is(':hidden')) {
				return;
			}
			var all = element.add(wrapper);
			var elem = $(e.target);
			if (all.is(elem) || all.has(elem).size() > 0) return;
			if (elem.zIndex() > element.parents('.slickgrid:first').zIndex()) return;
			if (elem.parents(".ui-dialog:first").zIndex() > element.parents('.slickgrid:first').zIndex()) return;

			element.trigger('close:slick-editor');
		}
		
		$(document).on('mousedown.mini-grid', hidePopup);
		
		scope.$watch(attrs.ngModel, function(value) {
			var text = "";
			if (value && value.length)
				text = "(" + value.length + ")";
			input.val(text);
		});
		
		scope.$watch('schema.loaded', function(viewLoaded) {
			var schema = scope.schema;
			if (schema && scope.attr('canEdit') === false) {
				schema.editIcon = false;
			}
		});
		
		scope.$on("$destroy", function(e){
			wrapper.remove();
			$(document).off('mousedown.mini-grid', hidePopup);
		});

		scope.canEdit = function () {
			return scope.hasPermission('create') && !scope.isReadonly() && scope.attr('canEdit') !== false;
		};

		scope.canRemove = function() {
			return scope.hasPermission('create') && !scope.isReadonly() && scope.attr('canEdit') !== false;
		};
	},
	
	template_editable: null,
	
	template_readonly: null,
	
	template:
	'<span class="picker-input picker-icons-2" style="position: absolute;">'+
		'<input type="text" readonly>'+
		'<span class="picker-icons">'+
			'<i class="fa fa-plus" ng-click="onSelect()" ng-show="canShowAdd()" title="{{\'Select\' | t}}"></i>'+
			'<i class="fa fa-minus" ng-click="onRemove()" ng-show="canShowRemove()" title="{{\'Select\' | t}}"></i>'+
			'<i class="fa fa-caret-down" ng-show="canDropdown()" ng-click="onDropdown()" title="{{\'Show\' | t}}"></i>'+
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

ui.formInput('ManyToManyInline', 'OneToManyInline', {
	
	css	: 'many2many-inline',
	
	controller: ManyToManyCtrl,
	
	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
	}
});

})();
