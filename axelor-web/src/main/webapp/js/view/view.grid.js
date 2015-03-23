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
GridViewCtrl.$inject = ['$scope', '$element'];
function GridViewCtrl($scope, $element) {

	DSViewCtrl('grid', $scope, $element);

	var ds = $scope._dataSource;
	var page = {};

	$scope.dataView = new Slick.Data.DataView();
	$scope.selection = [];
	
	ds.on('change', function(e, records, page){
		$scope.setItems(records, page);
	});

	var initialized = false;
	var reloadDotted = false;
	
	$scope.onShow = function(viewPromise) {

		if (!initialized) {
			
			viewPromise.then(function(){
				var view = $scope.schema,
					params = $scope._viewParams,
					sortBy = view.orderBy,
					pageNum = null;

				if (sortBy) {
					sortBy = sortBy.split(',');
				}
				if (params.options && params.options.mode === 'list') {
					pageNum = params.options.state;
					$scope._routeSearch = params.options.search;
				}

				reloadDotted = params.params && params.params['reload-dotted'];

				$scope.view = view;
				$scope.filter({
					_sortBy: sortBy,
					_pageNum: pageNum
				}).then(function(){
					$scope.$broadcast('on:grid-selection-change', $scope.getContext());
					$scope.updateRoute();
				});

				setTimeout(focusFilter);
			});
			
			initialized = true;
		} else {
			setTimeout(focusFilter);
			if (reloadDotted) {
				return $scope.reload().then(function() {
					$scope.updateRoute();
				});
			}
			var current = $scope.dataView.getItem(_.first($scope.selection));
			if (current && current.id) {
				$scope.dataView.updateItem(current.id, current);
			}
		}
	};
	
	$scope.getRouteOptions = function() {
		var pos = 1,
			args = [],
			query = {},
			params = $scope._viewParams;

		if (page && page.limit) {
			pos = (page.from / page.limit) + 1;
		} else if (params.options && params.options.mode === 'list') {
			pos = +(params.options.state);
		}

		pos = pos || 1;
		args = [pos];

		return {
			mode: 'list',
			args: args,
			query: query
		};
	};
	
	$scope._routeSearch = {};
	$scope.setRouteOptions = function(options) {
		var opts = options || {},
			pos = +(opts.state),
			current = (page.from / page.limit) + 1;

		pos = pos || 1;
		current = current || 1;

		$scope._routeSearch = opts.search;
		if (pos === current) {
			return $scope.updateRoute();
		}
		
		var params = $scope._viewParams;
		if (params.viewType !== "grid") {
			return $scope.show();
		}

		$scope.filter({
			_pageNum: pos
		});
	};

	$scope.getItem = function(index) {
		return $scope.dataView.getItem(index);
	};

	$scope.getItems = function() {
		return $scope.dataView.getItems();
	};

	$scope.setItems = function(items, pageInfo) {

		var dataView = $scope.dataView,
			selection = $scope.selection || [],
			selectionIds = dataView.mapRowsToIds(selection);
		
		//XXX: clear existing items (bug?)
		if (dataView.getLength()) {
			dataView.beginUpdate();
			dataView.setItems([]);
		    dataView.endUpdate();
		}

		dataView.beginUpdate();
	    dataView.setItems(items);
	    dataView.endUpdate();

		if (pageInfo) {
	    	page = pageInfo;
		}
		
		if (dataView.$syncSelection) {
			setTimeout(function(){
				dataView.$syncSelection(selection, selectionIds);
			});
		}
	};

	$scope.attr = function (name) {
		if (!$scope.schema || $scope.schema[name] === undefined) {
			return true;
		}
		return $scope.schema[name];
	};
	
	$scope.canNew = function() {
		return $scope.hasButton('new');
	};

	$scope.canEdit = function() {
		return $scope.hasButton('edit') && $scope.selection.length > 0;
	};
	
	$scope.canSave = function() {
		return $scope.hasButton('save') && this.dataView.canSave && this.dataView.canSave();
	};
	
	$scope.canDelete = function() {
		return $scope.hasButton('delete') && !$scope.canSave() && $scope.selection.length > 0;
	};
	
	$scope.canEditInline = function() {
		return _.isFunction(this.dataView.canSave);
	};
	
	$scope.canMassUpdate = function () {
		// this permission is actually calculated from fields marked for mass update
		return $scope.hasPermission('massUpdate', false);
	};

	$scope.filter = function(searchFilter) {

		var fields = _.pluck($scope.fields, 'name'),
			options = {};

		// if criteria is given search using it
		if (searchFilter.criteria || searchFilter._domains) {
			options = {
				filter: searchFilter,
				fields: fields
			};
			if (searchFilter.archived !== undefined) {
				options.archived = searchFilter.archived;
			}
			return ds.search(options);
		}

		var filter =  {},
			sortBy, pageNum,
			domain = null,
			context = null,
			action = undefined,
			criteria = {
				operator: 'and'
			};

		for(var name in searchFilter) {
			var value = searchFilter[name];
			if (value !== '') filter[name] = value;
		}
		
		pageNum = +(filter._pageNum || 0);
		sortBy = filter._sortBy;
		domain = filter._domain;
		context = filter._context;
		action = filter._action;
		
		delete filter._pageNum;
		delete filter._sortBy;
		delete filter._domain;
		delete filter._context;
		delete filter._action;

		criteria.criteria = _.map(filter, function(value, key) {

			var field = $scope.fields[key] || {};
			var type = field.type || 'string';
			var operator = 'like';
			var value2 = undefined;

			//TODO: implement expression parser
			
			if (type === 'many-to-one') {
				if (field.targetName) {
					key = key + '.' + field.targetName;
				} else {
					console.warn("Can't search on field: ", key);
				}
			}
			if (field.selection) {
				type = 'selection';
			}

			function stripOperator(val) {
				var match = /(<)(.*)(<)(.*)/.exec(val);
				if (match) {
					operator = 'between';
					value2 = match[2].trim();
					return match[4].trim();
				}
				match = /(<=?|>=?|=)(.*)/.exec(val);
				if (match) {
					operator = match[1];
					return match[2].trim();
				}
				return val;
			}

			function toMoment(val) {
				var format = 'MM/YYYY';
				if (/\d+\/\d+\/\d+/.test(val)) format = 'DD/MM/YYYY';
				if (/\d+\/\d+\/\d+\s+\d+:\d+/.test(val)) format = 'DD/MM/YYYY HH:mm';
				return val ? moment(val, format) : moment();
			}

			function toDate(val) {
				return val ? toMoment(val).format('YYYY-MM-DD') : val;
			}

			switch(type) {
				case 'integer':
				case 'long':
				case 'decimal':
					operator = '=';
					value = stripOperator(value);
					value = +(value) || 0;
					if (value2) value2 = +(value2) || 0;
					break;
				case 'boolean':
					operator = '=';
					value = !/f|n|false|no|0/.test(value);
					break;
				case 'date':
					operator = '=';
					value = stripOperator(value);
					value = toMoment(value);
					if (value) value = value.startOf('day').toDate().toISOString();
					value2 = toDate(value2);
					break;
				case 'time':
					operator = '=';
					break;
				case 'datetime':
					operator = 'between';
					value = stripOperator(value);
					var val = toMoment(value);
					value = (operator == 'between' ? val.startOf('day') : val).toDate().toISOString();
					value2 = (operator == 'between' ? val.endOf('day') : val).toDate().toISOString();
					break;
				case 'selection':
					operator = '=';
					break;
			}

			return {
				fieldName: key,
				operator: operator,
				value: value,
				value2: value2
			};
		});
		
		domain = domain || $scope._domain;
		if (domain && $scope.getContext) {
			context = _.extend({}, $scope._context, context, $scope.getContext());
		}
		
		options = {
			filter: criteria,
			fields: fields,
			sortBy: sortBy,
			domain: domain,
			context: context,
			action: action
		};

		if (pageNum) {
			options.offset = (pageNum - 1 ) * ds._page.limit;
		}

		return ds.search(options);
	};

	$scope.pagerText = function() {
		if (page && page.from !== undefined) {
			if (page.total == 0) return null;
			return _t("{0} to {1} of {2}", page.from + 1, page.to, page.total);
		}
	};
	
	$scope.pagerIndex = function(fromSelection) {
		var index = page.index,
			record = null;
		if (fromSelection) {
			record = $scope.dataView.getItem(_.first($scope.selection));
			index = ds._data.indexOf(record);
		}
		return index;
	};

	$scope.onNext = function() {
		var fields = _.pluck($scope.fields, 'name');
		ds.next(fields).then(function(){
			$scope.updateRoute();
		});
	};
	
	$scope.onPrev = function() {
		var fields = _.pluck($scope.fields, 'name');
		ds.prev(fields).then(function(){
			$scope.updateRoute();
		});
	};
	
	$scope.onNew = function() {
		page.index = -1;
		$scope.switchTo('form', function(viewScope){
			$scope.ajaxStop(function(){
				$scope.applyLater(function(){
					viewScope.$broadcast('on:new');
				});
			});
		});
	};
	
	$scope.onEdit = function(force) {
		page.index = $scope.pagerIndex(true);
		$scope.switchTo('form', function (formScope) {
			if (force && formScope.canEdit()) {
				formScope.onEdit();
			}
		});
	};

	$scope.onDelete = function() {
		
		axelor.dialogs.confirm(_t("Do you really want to delete the selected record(s)?"), function(confirmed){

			if (!confirmed)
				return;

			var selected = _.map($scope.selection, function(index) {
				return $scope.dataView.getItem(index);
			});

			ds.removeAll(selected).success(function(records, page){
				if (records.length == 0 && page.total > 0) {
					$scope.onRefresh();
				}
			});
		});
	};
	
	$scope.onRefresh = function() {
		return $scope.reload();
	};
	
	$scope.reload = function() {
		var fields = _.pluck($scope.fields, 'name');
		return ds.search({
			fields: fields
		});
	};

	$scope.onSort = function(event, args) {
		var fields = _.pluck($scope.fields, 'name');
		var sortBy = _.map(args.sortCols, function(column) {
			var name = column.sortCol.field;
			var spec = column.sortAsc ? name : '-' + name;
			return spec;
		});
		ds.search({
			sortBy: sortBy,
			fields: fields
		});
	};

	$scope.onSelectionChanged = function(event, args) {
		var items = $scope.getItems();
		var selection = [];

		_.each(items, function (item) {
			item.selected = false;
		});
		_.each(args.rows, function(index) {
			var item = args.grid.getDataItem(index);
			if (item && item.id !== 0) {
				item.selected = true;
				selection.push(index);
			}
		});

		$scope.selection = selection;
		$scope.applyLater(function () {
			$scope.$broadcast('on:grid-selection-change', $scope.getContext());
		});
	};
	
	$scope.onItemClick = function(event, args) {
		if (axelor.device.mobile) {
			$scope.$timeout(function () {
				$scope.onEdit();
			});
		}
	};
	
	$scope.onItemDblClick = function(event, args) {
		$scope.onEdit();
		$scope.$apply();
	};
	
	var _getContext = $scope.getContext;
	$scope.getContext = function() {

		// if nested grid then return parent's context
		if (_getContext) {
			return _getContext();
		}

		var dataView = $scope.dataView,
			selected = _.map($scope.selection, function(index) {
				return dataView.getItem(index);
			});
		return {
			'_ids': _.pluck(selected, "id")
		};
	};
	
	$scope.onSave = function() {
		this.dataView.saveChanges();
	};
	
	$scope.onArchived = function(e) {
		var button = $(e.currentTarget);
		setTimeout(function(){
			var active = button.is('.active');
			var fields = _.pluck($scope.fields, 'name');
			ds.search({
				fields: fields,
				archived: active
			});
		});
	};
	
	function focusFirst() {
		var index = _.first($scope.selection) || 0;
		var first = $scope.dataView.getItem(index);
		if (first) {
			$scope.dataView.$syncSelection([], [first.id], true);
		}
	}
	
	function focusFilter() {
		var filterBox = $('.filter-box .search-query:visible');
		if (filterBox.size()) {
			filterBox.focus().select();
		}
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
				$scope.onEdit(true);
			} else {
				focusFirst();
			}
		}
		if (action === "delete" && $scope.canDelete()) {
			$scope.onDelete();
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

		$scope.applyLater();
		return false;
	};
}

angular.module('axelor.ui').directive('uiViewGrid', function(){
	return {
		replace: true,
		template: '<div ui-slick-grid ui-widget-states></div>'
	};
});

angular.module('axelor.ui').directive('uiGridExport', function(){

	return {
		require: '^uiFilterBox',
		link: function(scope, element, attrs, ctrl) {
			var handler = ctrl.$scope.handler;
			if (!handler) {
				return;
			}
	
			function action(name) {
				var res = 'ws/rest/' + handler._model + '/export';
				return name ? res + '/' + name : res;
			}
	
			function fields() {
				return _.pluck(handler.view.items, 'name');
			}
	
			var ds = handler._dataSource;
			
			function onExport() {
				return ds.export_(fields()).success(function(res) {
	
					var filePath = action(res.fileName),
						fileName = res.fileName;
	
					var link = document.createElement('a');
	
					link.onclick = function(e) {
						document.body.removeChild(e.target);
					};
	
					link.href = filePath;
					link.download = fileName;
					link.innerHTML = fileName;
					link.style.display = "none";
	
					document.body.appendChild(link);
	
					link.click();
					
					axelor.notify.info(_t("Export in progress ..."));
				});
			};
			
			element.on('click', onExport);
		}
	};
});

angular.module('axelor.ui').directive('uiPortletGrid', function(){
	return {
		controller: ['$scope', '$element', 'ViewService', 'NavService', 'MenuService',
		             function($scope, $element, ViewService, NavService, MenuService) {

			GridViewCtrl.call(this, $scope, $element);
			
			var ds = $scope._dataSource;
			var counter = 0;
			
			function doEdit(force) {
				var promise = MenuService.action($scope._viewAction, {
					context: $scope.getContext()
				});

				promise.success(function (result) {
					if (!result.data) return;
					view = result.data[0].view;

					return doOpen(force, view);
				});
			}

			function doOpen(force, tab) {
				var index = $scope.pagerIndex(true);
				var record = ds.at(index);

				tab.viewType = "form";
				tab.recordId = record.id;
				tab.action = _.uniqueId('$act');

				if ($scope._isPopup) {
					tab.$popupParent = $scope;
					tab.params = tab.params || {};
					_.defaults(tab.params, {
						'show-toolbar': false
					});
				}

				setTimeout(function(){
					NavService.openView(tab);
					$scope.$apply();
					if (counter++ === 0) {
						return;
					}
					setTimeout(function() {
						var scope = ($scope.selectedTab || {}).$viewScope;
						if (scope && scope.editRecord) {
							scope.confirmDirty(function() {
								scope.doRead(record.id).success(function(record){
									scope.edit(record);
									if (force) {
										scope.onEdit();
									}
								});
							});
						}
					});
				});
			}

			$scope.showPager = true;
			$scope.onEdit = doEdit;
			$scope.onItemDblClick = function(event, args) {
				doEdit(false);
			};

			function doReload() {
				var tab = NavService.getSelected();
				var type = tab.viewType || tab.type;
				if (type !== 'grid') {
					$scope.waitForActions(function () {
						$scope.ajaxStop(function () {
							$scope.filter({});
						});
					});
				}
			}

			$scope.$on("on:new", function(e) {
				$scope.$timeout(doReload, 100);
			});
			$scope.$on("on:edit", function(e) {
				$scope.$timeout(doReload, 100);
			});

			$scope.onRefresh = function() {
				$scope.filter({});
			};
			
			var _onShow = $scope.onShow;
			var _filter = $scope.filter;
			var _action = $scope._viewAction;

			$scope.filter = function (searchFilter) {
				var opts = searchFilter || {};
				if ($scope.$parent._model) {
					opts = _.extend({}, opts, {
						_action: _action
					});
				}
				var ds = $scope._dataSource;
				var view = $scope.schema || {};
				if (!opts._sortBy && !ds._sortBy && view.orderBy) {
					opts._sortBy = view.orderBy.split(',');
				}
				return _filter.call($scope, opts);
			};
			
			$scope.onShow = function () {
				var scope = ($scope.selectedTab || {}).$viewScope;
				if (scope && scope.editRecord) {
					return;
				}
				return _onShow.apply($scope, arguments);
			};
		}],
		replace: true,
		template:
		'<div class="portlet-grid">'+
			'<div ui-view-grid x-view="schema" x-data-view="dataView" x-editable="false" x-no-filter="{{noFilter}}" x-handler="this"></div>'+
		'</div>'
	};
});
