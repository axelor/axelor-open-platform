(function(undefined){

var ui = angular.module('axelor.ui');

function formatter(row, cell, value, columnDef, dataContext) {
	
	if (value === null || value === undefined) {
		return "";
	}
	if (_.isString(value) && value.trim() === "")
		return "";
	if (_.isObject(value) && _.isEmpty(value))
		return "";

	var field = columnDef.descriptor || {};
	
	if (_.isArray(field.selection)) {
		var cmp = field.type === "integer" ? function(a, b) { return a == b ; } : _.isEqual;
		var res = _.find(field.selection, function(item){
			return cmp(item.value, value);
		}) || {};
		return res.title;
	}
	
	switch(field.type) {
	case 'many-to-one':
		return value[field.targetName];
	case 'one-to-many':
	case 'many-to-many':
		return '(' + value.length + ')';
	case 'date':
		return moment(value, 'YYYY-MM-DD').format('DD/MM/YYYY');
	case 'datetime':
		return moment(value, 'YYYY-MM-DD HH:mm').format('DD/MM/YYYY HH:mm');
	case 'boolean':
		return value ? '<i class="icon-ok"></i>' : "";
	}

	return value;
}

//used to keep track of columns by x-path
function setDummyCols(element, cols) {
	var e = $('<div>').appendTo(element).hide();
	_.each(cols, function(col, i) {
		$('<span class="slick-dummy-column">')
			.data('column', col)
			.attr('x-path', col.xpath)
			.appendTo(e);
	});
}

function makeFilterCombo(input, selection, callback) {

	var data = _.map(selection, function(item){
		return {
			key: item.value,
			value: item.title
		};
	});
	
	function update(item) {
		var filter = {};
		item = item || {};
		input.val(item.value || '');
		filter[input.data('columnId')] = item.key || '';
		callback(filter);
	}

	input.autocomplete({
		minLength: 0,
		source: data,
		focus: function(event, ui) {
			return false;
		},
		select: function(event, ui) {
			update(ui.item);
			return false;
		}
	}).keyup(function(e){
		return false;
	}).keydown(function(e){
		switch(e.keyCode) {
		case 8:		// backspace
		case 46:	// delete
			update(null);
		}
	}).focus(function(){
		input.autocomplete("search", "");
	});
}

var Editor = function(args) {
	
	var element;
	var column = args.column;
	var scope;

	var form = $(args.container)
		.parents('[ui-slick-grid]:first')
		.find('[ui-slick-editors]:first');
	
	element = form.find('[x-field="'+ column.field +'"]');
	scope = form.data('$scope');
	
	if (!element.parent().is('td.form-item'))
		element = element.parent();
	element.data('$parent', element.parent());

	this.init = function() {
		element.appendTo(args.container);
		if (element.data('keydown.nav') == null) {
			element.data('keydown.nav', true);
			element.bind("keydown.nav", function (e) {
				if (e.keyCode === $.ui.keyCode.LEFT || e.keyCode === $.ui.keyCode.RIGHT) {
					e.stopImmediatePropagation();
				}
			});
		}
		if (args.item && args.item.id > 0)
			element.hide();
		this.focus();
	};
	
	this.destroy = function() {
		element.appendTo(element.data('$parent') || form)
			   .removeData('$parent');
		element.trigger("hide:slick-editor");
	};
	
	this.position = function(pos) {
		element.trigger("show:slick-editor");
		setTimeout(function(){
			element.trigger("show:slick-editor");
			scope.$apply();
		});
	};

	this.focus = function() {
		if (element.is(':input'))
			return element.focus().select();
		element.find(':input:first').focus().select();
	};

	this.loadValue = function(item) {
		var that = this,
			record = scope.record || {},
			current = item || { id: 0 },
			updated = false;
			
		if (record.id !== current.id || record.version !== current.version) {
			scope.editRecord(current);
		} else {
			record[column.field] = current[column.field];
			updated = true;
		}
		setTimeout(function(){
			if (updated) {
				scope.$apply();
			}
			element.show();
			that.focus();
		});
	};
	
	this.serializeValue = function() {
		var record = scope.record || {};
		var value = record[column.field];
		return value === undefined ? "" : value;
	};
	
	this.applyValue = function(item, state) {
		item[column.field] = state;
		item.$dirty = true;
		if (item.id === undefined) {
			args.grid.onCellChange.notify(args.grid.getActiveCell());
		}
	};
	
	this.isValueChanged = function() {
		var record = scope.record || {},
			current = args.item || { id: 0 };
		
		var v1 = record[column.field],
			v2 = current[column.field];

		return !angular.equals(v1, v2);
	};
	
	this.validate = function() {
		return {
			valid: !element.hasClass('ng-invalid'),
			msg: null
        };
	};
	
	this.init();
};

var Factory = {
	
	getEditor : function(col) {
		var field = col.descriptor || {};
		if (field.readonly) {
			return null;
		}
		if (field.type == 'binary') {
			return null;
		}
		if (col.editor) {
			return col.editor;
		}
		return col.editor = Editor;
	}
};

var Grid = function(scope, element, attrs) {
	this.scope = scope;
	this.element = element;
	this.attrs = attrs;
	this.handler = scope.handler;
	this.showFilters = !scope.$eval('noFilter');
	this.grid = this.parse(scope.view);
};

Grid.prototype.parse = function(view) {

	var scope = this.scope,
		handler = scope.handler,
		dataView = scope.dataView,
		element = this.element;

	scope.fields_view = {};

	var cols = _.map(view.items, function(item){
		var field = handler.fields[item.name],
			path = handler.formPath;

		field = _.extend({}, item, field);
		scope.fields_view[item.name] = field;
		path = path ? path + '.' + item.name : item.name;

		return {
			name: item.title || item.name,
			id: item.name,
			field: item.name,
			descriptor: field,
			formatter: formatter,
			sortable: true,
			xpath: path
		};
	});

	// create checkbox column
	var selectColumn = null;
	if (scope.selector) {
		selectColumn = new Slick.CheckboxSelectColumn({
			cssClass: "slick-cell-checkboxsel"
		});
		
		cols.unshift(_.extend(selectColumn.getColumnDefinition(), {
			headerCssClass: "slick-cell-checkboxsel"
		}));
	}

	var options = {
		editable: view.editable,
		editorFactory:  Factory,
		enableCellNavigation: true,
		enableColumnReorder: false,
		forceFitColumns: false,
		multiColumnSort: true,
		showHeaderRow: this.showFilters
	};

	this.cols = cols;
	this.grid = grid = new Slick.Grid(element, dataView, cols, options);
	
	element.show();
	element.data('grid', grid);
	
	grid.setSelectionModel(new Slick.RowSelectionModel());
	if (selectColumn) {
		grid.registerPlugin(selectColumn);
	}

	var onAdjustSize = _.bind(this.adjustSize, this);
	dataView.$adjustSize = onAdjustSize;
	element.on('adjustSize', _.throttle(onAdjustSize, 300));

	dataView.$syncSelection = function(old, oldIds) {
		var selection = dataView.mapIdsToRows(oldIds);
		grid.setSelectedRows(selection);
		if (selection.length === 0 && !grid.getEditorLock().isActive()) {
			grid.setActiveCell(null);
        }
	};
	
	// register grid event handlers
	this.subscribe(grid.onSort, this.onSort);
	this.subscribe(grid.onSelectedRowsChanged, this.onSelectionChanged);
	this.subscribe(grid.onClick, this.onItemClick);
	this.subscribe(grid.onDblClick, this.onItemDblClick);
	
	this.subscribe(grid.onKeyDown, this.onKeyDown);
	this.subscribe(grid.onCellChange, this.onCellChange);
	this.subscribe(grid.onAddNewRow, this.onAddNewRow);
	this.subscribe(grid.onBeforeEditCell, this.onBeforeEditCell);
	
	// register dataView event handlers
	this.subscribe(dataView.onRowCountChanged, this.onRowCountChanged);
	this.subscribe(dataView.onRowsChanged, this.onRowsChanged);
	
	// delegate some methods to handler scope
	//TODO: this spoils the handler scope, find some better way
	handler.showColumn = _.bind(this.showColumn, this);
	handler.resetColumns = _.bind(this.resetColumns, this);
	handler.setColumnTitle = _.bind(this.setColumnTitle, this);
	
	// set filters
	if (this.showFilters) {
		var filters = {};
		var filtersRow = $(grid.getHeaderRow());
		
		_.each(cols, function(col){
			if (!col.xpath) {
				return;
			}

			var header = grid.getHeaderRowColumn(col.id);
			var input = $('<input type="text">').data("columnId", col.id).appendTo(header);
			var field = col.descriptor || {};
			
			if (_.isArray(field.selection)) {
				makeFilterCombo(input, field.selection, function(filter){
					_.extend(filters, filter);
				});
			}
		});

		filtersRow.on('keyup', ':input', function(event){
			filters[$(this).data('columnId')] = $(this).val().trim();
			return true;
		});
		filtersRow.on('keypress', ':input', function(event){
			if (event.keyCode === 13) {
				scope.handler.filter(filters);
				return false;
			}
			return true;
		});
	}
	
	var onInit = scope.onInit();
	if (_.isFunction(onInit)) {
		onInit(grid);
	}
	
	setTimeout(function(){
		setDummyCols(element, cols);
		element.trigger('adjustSize');
	});

	if (scope.$parent._viewResolver) {
		scope.$parent._viewResolver.resolve(view, element);
	}

	return grid;
};

Grid.prototype.subscribe = function(event, handler) {
	event.subscribe(_.bind(handler, this));
};

Grid.prototype.adjustSize = function() {
	var grid = this.grid;
	if (!grid || !this.element.is(':visible'))
		return;
	grid.getViewport().rightPx = 0;
	grid.resizeCanvas();
	grid.autosizeColumns();
	if (!grid.getEditorLock().isActive())
		grid.invalidate();
};

Grid.prototype.showColumn = function(name, show) {
	
	var that = this,
		grid = this.grid,
		cols = this.cols;
	
	if (this.visibleCols == null) {
		this.visibleCols = _.pluck(cols, 'id');
	}
	
	show = _.isUndefined(show) ? true : show;
	
	var visible = new Array(),
		current = new Array();
	
	_.each(cols, function(col){
		if (col.id != name && _.contains(that.visibleCols, col.id))
			return visible.push(col.id);
		if (col.id == name && show)
			return visible.push(name);
	});
	
	this.visibleCols = visible;
	current = _.filter(cols, function(col) {
		return _.contains(visible, col.id);
	});
	grid.setColumns(current);
	grid.getViewport().rightPx = 0;
	grid.resizeCanvas();
	grid.autosizeColumns();
};

Grid.prototype.resetColumns = function() {
	var grid = this.grid,
		cols = this.cols;
	
	this.visibleCols = _.pluck(cols, 'id');
	
	grid.setColumns(cols);
	grid.getViewport().rightPx = 0;
	grid.resizeCanvas();
	grid.autosizeColumns();
};

Grid.prototype.setColumnTitle = function(name, title) {
	this.grid.updateColumnHeader(name, title);
};

Grid.prototype.onBeforeEditCell = function(event, args) {
	if (!args.item) {
		this.editorScope.editRecord(null);
	}
};

Grid.prototype.onKeyDown = function(e, args) {
	var grid = this.grid,
		lock = grid.getEditorLock();

	if (e.isDefaultPrevented()){
		e.stopImmediatePropagation();
		return false;
	}
	
	if (!lock.isActive()) {
		return;
	}

	// prevent arrow key navigation
	if (e.which == 38 || e.which == 40 || e.which == 37 || e.which == 39) {
		e.stopImmediatePropagation();
		return false;
	}

	function commit(row, cell) {
		if (lock.commitCurrentEdit()) {
			grid.setActiveCell(args.row, cell);
			grid.editActiveCell();
			return true;
		}
		return false;
	}

	var handled = false;
	if (e.which === $.ui.keyCode.TAB) {
		var cell = null;
		if (e.shiftKey) {
			cell = this.findPrevEditable(args.row, args.cell);
		} else {
			cell = this.findNextEditable(args.row, args.cell);
		}
		if (cell !== null) {
			commit(args.row, cell);
			handled = true;
		}
	}

	if (e.which === $.ui.keyCode.ENTER) {
		if (e.ctrlKey) {
			if (!this.saveChanges(args)) {
				this.focusInvalidCell(args);
			}
		} else {
			if (!lock.commitCurrentEdit()) {
				this.focusInvalidCell(args);
			}
			grid.focus();
		}
		handled = true;
	}

	if (e.which === $.ui.keyCode.ESCAPE) {
		grid.focus();
	}
	
	if (handled) {
		e.stopImmediatePropagation();
		return false;
	}
};

Grid.prototype.isCellEditable = function(cell) {
	var cols = this.grid.getColumns();
	if (cell === null)
		return false;
	var field = (cols[cell] || {}).descriptor || {};
	return !field.readonly;
};

Grid.prototype.findNextEditable = function(row, posX) {
	var grid = this.grid,
		cols = grid.getColumns(),
		cell = posX + 1;
	while (cell < cols.length) {
		if (this.isCellEditable(cell)) {
			return cell;
		}
		cell += 1;
	}
	cell = 0;
	while (cell < posX) {
		if (this.isCellEditable(cell)) {
			return cell;
		}
		cell += 1;
	}
	return null;
};

Grid.prototype.findPrevEditable = function(row, posX) {
	var grid = this.grid,
		cols = grid.getColumns(),
		cell = posX - 1;
	while (cell > -1) {
		if (this.isCellEditable(cell)) {
			return cell;
		}
		cell -= 1;
	}
	cell = cols.length - 1;
	while (cell > posX) {
		if (this.isCellEditable(cell)) {
			return cell;
		}
		cell -= 1;
	}
	return null;
};

Grid.prototype.saveChanges = function(args) {
	
	if (args == null) {
		args = _.extend({ row: 0, cell: 0 }, this.grid.getActiveCell());
		args.item = this.grid.getDataItem(args.row);
	}

	var grid = this.grid,
		lock = grid.getEditorLock(),
		canSave = this.editorScope.isValid();
	
	if (lock.isActive()) {
		canSave = canSave && lock.commitCurrentEdit();
	}

	if (!canSave) {
		return false;
	}

	var self = this,
		data = this.scope.dataView,
		item = data.getItem(args.row);

	var ds = this.handler._dataSource,
		records = [];

	records = _.map(data.getItems(), function(rec) {
		var res = {};
		for(var key in rec) {
			var val = rec[key];
			if (_.isString(val) && val.trim() === "")
				val = null;
			res[key] = val;
		}
		if (res.id === 0) {
			res.id = null;
		}
		return res;
	});
	
	function focus() {
		
		grid.setActiveCell(args.row, args.cell);
		grid.focus();
		
		if (item.id === 0) {
			grid.setOptions({
				enableAddRow: true
			});
			var nextCell = self.findNextEditable(args.row + 1, -1);
			if (nextCell !== null) {
				grid.updateRowCount();
				grid.render();
				grid.setActiveCell(args.row + 1, nextCell);
				grid.editActiveCell();
			}
		}
	}

	var onBeforeSave = this.scope.onBeforeSave(),
		onAfterSave = this.scope.onAfterSave();

	if (onBeforeSave && onBeforeSave(records) === false) {
		return setTimeout(focus);
	}

	return ds.saveAll(records).success(function(records, page) {
		if (data.getItemById(0)) {
			data.deleteItem(0);
		}
		if (onAfterSave) {
			onAfterSave(records, page);
		}
		setTimeout(focus);
	});
};

Grid.prototype.isDirty = function(row) {
	var grid = this.grid;
	
	if (row === null || row === undefined) {
		var n = 0;
		while (n < grid.getDataLength()) {
			var item = grid.getDataItem(n);
			if (item && item.$dirty) {
				return true;
			}
			n ++;
		}
	} else {
		var item = grid.getDataItem(row);
		if (item && item.$dirty) {
			return true;
		}
	}
	return false;
};

Grid.prototype.markDirty = function(row, field) {
	
	var grid = this.grid,
		hash = grid.getCellCssStyles("highlight") || {},
		items = hash[row] || {};
	
	items[field] = "dirty";
	hash[row] = items;

	grid.setCellCssStyles("highlight", hash);
	grid.invalidateAllRows();
	grid.render();
};

Grid.prototype.clearDirty = function(row) {
	var grid = this.grid,
		hash = grid.getCellCssStyles("highlight") || {};

	if (row === null || row === undefined) {
		hash = {};
	} else {
		delete hash[row];
	}
	
	grid.setCellCssStyles("highlight", hash);
	grid.invalidateAllRows();
	grid.render();
};

Grid.prototype.focusInvalidCell = function(args) {
	var grid = this.grid,
		formCtrl = this.editorForm.children('form').data('$formController'),
		error = formCtrl.$error || {};
	
	for(var name in error) {
		var errors = error[name] || [];
		if (errors.length) {
			var name = errors[0].$name,
				cell = grid.getColumnIndex(name);
			if (cell > -1) {
				grid.setActiveCell(args.row, cell);
				grid.editActiveCell();
				break;
			}
		}
	}
};

Grid.prototype.onAddNewRow = function(event, args) {
	var scope = this.scope,
		grid = this.grid,
		dataView = scope.dataView,
		item = args.item;

	if (!item.id) {
		item.id = 0;
		grid.invalidateRow(dataView.length);
		dataView.addItem(item);
	    
		grid.setOptions({
			enableAddRow: false
		});
	    
		grid.updateRowCount();
	    grid.render();
	}
};

Grid.prototype.setEditors = function(form, formScope) {
	var grid = this.grid,
		data = this.scope.dataView,
		element = this.element;

	grid.setOptions({
		editable: true,
		asyncEditorLoading: false,
		enableAddRow: true,
		editorLock: new Slick.EditorLock()
	});
	
	form.prependTo(element).hide();

	// delegate isDirty to the dataView
	var that = this;
	data.canSave = function() {
		return that.editorScope.isValid() && that.isDirty();
	};
	data.saveChanges = _.bind(this.saveChanges, this);
	
	this.editorForm = form;
	this.editorScope = formScope;
	this.editable = true;
};

Grid.prototype.onSelectionChanged = function(event, args) {
	if (this.handler.onSelectionChanged)
		this.handler.onSelectionChanged(event, args);
};

Grid.prototype.onCellChange = function(event, args) {
	var grid = this.grid,
		cols = grid.getColumns(),
		name = cols[args.cell].field;
	
	this.markDirty(args.row, name);
};

Grid.prototype.onSort = function(event, args) {
	if (this.handler.onSort)
		this.handler.onSort(event, args);
};

Grid.prototype.onItemClick = function(event, args) {
	if (this.editable) {
		return;
	}
	if (this.handler.onItemClick)
		this.handler.onItemClick(event, args);
};

Grid.prototype.onItemDblClick = function(event, args) {
	if (this.editable) {
		return;
	}
	if (this.handler.onItemDblClick)
		this.handler.onItemDblClick(event, args);
};

Grid.prototype.onRowCountChanged = function(event, args) {
	this.grid.updateRowCount();
	this.grid.render();
};

Grid.prototype.onRowsChanged = function(event, args) {
	var grid = this.grid,
		data = this.scope.dataView;
	
	if(this.editable && !data.getItemById(0)) {
		grid.setOptions({
			enableAddRow: true
		});
	}
	
	if (!this.isDirty()) {
		this.clearDirty();
	}
	grid.invalidateRows(args.rows);
	grid.render();
};

ui.directive('uiSlickEditors', function() {
	
	return {
		restrict: 'EA',
		replace: true,
		controller: ['$scope', '$element', 'DataSource', 'ViewService', function($scope, $element, DataSource, ViewService) {
			ViewCtrl($scope, DataSource, ViewService);
			FormViewCtrl.call(this, $scope, $element);
			$scope.onShow = function(viewPromise) {
				
			};
			
			$scope.show();
		}],
		link: function(scope, element, attrs) {
			
		},
		template: '<div ui-view-form x-handler="true"></div>'
	};
});

ui.directive('uiSlickGrid', ['ViewService', function(ViewService) {
	
	var types = {
		'one-to-many' : 'one-to-many-inline',
		'many-to-many' : 'many-to-many-inline'
	};

	function makeForm(scope, model, items, fields) {

		fields = fields || {};
		items = _.map(items, function(item) {
			var field = fields[item.name],
				type = types[field.type];
			
			var params = _.extend({}, item, { noLabel: true });
			if (type) {
				params.type = type;
			}
			return params;
		});

		var schema = {
			cols: items.length,
			colWidths: '=',
			viewType : 'form',
			items: items,
		};

		scope._viewParams = {
			model: model,
			views: [schema]
		};

		return ViewService.compile('<div ui-slick-editors></div>')(scope);
	};
	
	return {
		restrict: 'EA',
		replace: false,
		scope: {
			'view'		: '=',
			'dataView'	: '=',
			'handler'	: '=',
			'selector'	: '@',
			'noFilter'	: '@',
			'onInit'	: '&',
			'onBeforeSave'	: '&',
			'onAfterSave'	: '&'
		},
		link: function(scope, element, attrs) {

			var grid = null,
				handler = scope.handler;

			element.addClass('slickgrid').hide();
			scope.$watch("view", function(view) {
				
				if (grid || view == null || scope.dataView == null) {
					return;
				}
				
				if (handler._readOnly) {
					view.editable = false;
				}

				grid = new Grid(scope, element, attrs, ViewService);
				if (view.editable) {
					var child = scope.$new();
					var form = makeForm(child, handler._model, view.items, handler.fields);
					grid.setEditors(form, child);
				}
			});
		}
	};
}]);

})();