angular.module('axelor.ui').directive('uiSlickGrid', function(){
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
		link: function(scope, elem, attrs) {
			
			$(elem).addClass("slickgrid").hide();
			
			scope.$watch('view', function(view){
				
				if (view == null || scope.dataView == null) {
					return;
				}
				
				scope.fields_view = {};

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
				
				// used to keep track of columns by x-path
				function setDummyCols(cols) {
					var e = $('<div>').appendTo(elem).hide();
					_.each(cols, function(col, i) {
						$('<span class="slick-dummy-column">')
							.data('column', col)
							.attr('x-path', col.xpath)
							.appendTo(e);
					});
				}

				var cols = _.map(view.items, function(item){
					var field = scope.handler.fields[item.name],
						path = scope.handler.formPath;
					
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
				
				var checkboxSelector = null;
				if (scope.selector) {
					checkboxSelector = new Slick.CheckboxSelectColumn({
						cssClass: "slick-cell-checkboxsel"
					});
					
					cols.splice(0, 0, checkboxSelector.getColumnDefinition());
				}
				
				var options = {
					enableCellNavigation: true,
					enableColumnReorder: false,
					forceFitColumns: false,
					multiColumnSort: true,
					showHeaderRow: scope.noFilter != 'true'
				};
				
				$(elem).show();
				
				var gridHandler = scope.handler;
				var dataView = scope.dataView;
				var grid = new Slick.Grid($(elem), dataView, cols, options);
				
				elem.data('grid', grid);

				var visibleCols = _.pluck(cols, 'id');
				scope.handler.showColumn = function(name, show) {
					
					show = _.isUndefined(show) ? true : show;
					
					var visible = new Array(),
						current = new Array();
					
					_.each(cols, function(col){
						if (col.id != name && _.contains(visibleCols, col.id))
							return visible.push(col.id);
						if (col.id == name && show)
							return visible.push(name);
					});
					
					visibleCols = visible;
					current = _.filter(cols, function(col) {
						return _.contains(visible, col.id);
					});
					grid.setColumns(current);
					grid.getViewport().rightPx = 0;
					grid.resizeCanvas();
					grid.autosizeColumns();
				};
				
				scope.handler.resetColumns = function() {
					visibleCols = _.pluck(cols, 'id');
					grid.setColumns(cols);
					grid.getViewport().rightPx = 0;
					grid.resizeCanvas();
					grid.autosizeColumns();
				};
				
				scope.handler.setColumnTitle = function(name, title) {
					grid.updateColumnHeader(name, title);
				};

				grid.setSelectionModel(new Slick.RowSelectionModel());
				
				if (checkboxSelector) {
					grid.registerPlugin(checkboxSelector);
				}

				dataView.adjustSize = function() {
					if (!elem.is(':visible'))
						return;
					grid.getViewport().rightPx = 0;
					grid.resizeCanvas();
					grid.autosizeColumns();
					grid.invalidate();
				};
				
				elem.on('adjustSize', _.throttle(dataView.adjustSize, 300));
				
				grid.onCellChange.subscribe(function (e, args) {
	    			dataView.updateItem(args.item.id, args.item);
	  			});
				
				if (gridHandler.onSort)
					grid.onSort.subscribe(function(e, args){
						gridHandler.onSort(e, args);
					});
				
				if (gridHandler.onSelectionChanged)
					grid.onSelectedRowsChanged.subscribe(function(e, args){
						gridHandler.onSelectionChanged(e, args);
					});
				
				if (gridHandler.onItemDblClick)
					grid.onDblClick.subscribe(function(e, args){
						gridHandler.onItemDblClick(e, args);
					});
				
				if (gridHandler.onItemClick)
					grid.onClick.subscribe(function(e, args){
						gridHandler.onItemClick(e, args);
					});
	  			
	  			// wire up model events to drive the grid
				dataView.onRowCountChanged.subscribe(function (e, args) {
					grid.updateRowCount();
					grid.render();
				});
				
				dataView.onRowsChanged.subscribe(function (e, args) {
					grid.invalidateRows(args.rows);
					grid.render();
				});
				
				scope.$on('grid:selection-change', function(e, args) {
					if (dataView === args.data) {
						grid.setSelectedRows(args.selection || []);
						if (_.isEmpty(args.selection) && grid.getActiveCell())
						    grid.setActiveCell(null);
					}
				});
				
				if (scope.noFilter != 'true') {
					
					var filters = {};
					
					function makeCombo(input, selection) {
						
						var data = _.map(selection, function(item){
							return {
								key: item.value,
								value: item.title
							};
						});
						
						function update(item) {
							item = item || {};
							input.val(item.value || '');
							filters[input.data('columnId')] = item.key || '';
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
					
					// create filters
					_.each(cols, function(col){
						
						if (col.id === "_checkbox_selector")
							return;

						var header = grid.getHeaderRowColumn(col.id);
						var input = $('<input type="text">').data("columnId", col.id).appendTo(header);
						var field = col.descriptor || {};
						
						if (_.isArray(field.selection)) {
							makeCombo(input, field.selection);
						}
					});
	
					$(grid.getHeaderRow()).on('keyup', ':input', function(event){
						filters[$(this).data('columnId')] = $(this).val().trim();
						return true;
					});
					$(grid.getHeaderRow()).on('keypress', ':input', function(event){
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
					setDummyCols(cols);
					elem.trigger('adjustSize');
				});
				
				if (scope.$parent._viewResolver) {
					scope.$parent._viewResolver.resolve(view, elem);
				}
			});
		}
	};
});
