(function(undefined){

var ui = angular.module('axelor.ui');

function formatter(row, cell, value, columnDef, dataContext) {
	
	if (_.isNull(value) || _.isUndefined(value)) return value;
	
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
		cols.unshift(selectColumn.getColumnDefinition());
	}

	var options = {
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
	dataView.adjustSize = onAdjustSize;
	element.on('adjustSize', _.throttle(onAdjustSize, 300));

	// register grid event handlers
	this.subscribe(grid.onSort, this.onSort);
	this.subscribe(grid.onSelectedRowsChanged, this.onSelectionChanged);
	this.subscribe(grid.onClick, this.onItemClick);
	this.subscribe(grid.onDblClick, this.onItemDblClick);
	
	// register dataView event handlers
	this.subscribe(dataView.onRowCountChanged, this.onRowCountChanged);
	this.subscribe(dataView.onRowsChanged, this.onRowsChanged);
	
	scope.$on('grid:selection-change', function(e, args) {
		if (dataView === args.data) {
			grid.setSelectedRows(args.selection || []);
			if (_.isEmpty(args.selection) && grid.getActiveCell())
			    grid.setActiveCell(null);
		}
	});
	
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

Grid.prototype.onCellChange = function(event, args) {
	
};

Grid.prototype.onSelectionChanged = function(event, args) {
	if (this.handler.onSelectionChanged)
		this.handler.onSelectionChanged(event, args);
};

Grid.prototype.onSort = function(event, args) {
	if (this.handler.onSort)
		this.handler.onSort(event, args);
};

Grid.prototype.onItemClick = function(event, args) {
	if (this.handler.onItemClick)
		this.handler.onItemClick(event, args);
};

Grid.prototype.onItemDblClick = function(event, args) {
	if (this.handler.onItemDblClick)
		this.handler.onItemDblClick(event, args);
};

Grid.prototype.onRowCountChanged = function(event, args) {
	this.grid.updateRowCount();
	this.grid.render();
};

Grid.prototype.onRowsChanged = function(event, args) {
	this.grid.invalidateRows(args.rows);
	this.grid.render();
};

ui.directive('uiSlickGrid', function() {
	return {
		restrict: 'EA',
		replace: false,
		scope: {
			'view'		: '=',
			'dataView'	: '=',
			'handler'	: '=',
			'selector'	: '@',
			'noFilter'	: '@',
			'onInit'	: '&'
		},
		link: function(scope, element, attrs) {

			var grid = null;

			element.addClass('slickgrid').hide();
			scope.$watch("view", function(view){
				
				if (grid || view == null || scope.dataView == null) {
					return;
				}
				
				grid = new Grid(scope, element, attrs);
			});
		}
	};
});

})();