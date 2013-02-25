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
	$scope.onShow = function(viewPromise) {
		
		if (!initialized) {
			
			viewPromise.then(function(){
				var view = $scope.schema,
					params = $scope._viewParams,
					sortBy = view.orderBy,
					pageNum = null;

				if (sortBy) {
					sortBy = sortBy.split('\\.');
				}
				if (params.options && params.options.mode === 'list') {
					pageNum = params.options.state;
					$scope._routeSearch = params.options.search;
				}

				$scope.view = view;
				$scope.filter({
					_sortBy: sortBy,
					_pageNum: pageNum
				}).then(function(){
					$scope.updateRoute();
				});
			});
			
			initialized = true;
		}
	};
	
	$scope.getRouteOptions = function() {
		var pos = null,
			args = [],
			query = {};

		if (page && page.limit) {
			pos = (page.from / page.limit) + 1;
			args.push(pos);
		}
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

	$scope.canEdit = function() {
		return $scope.canDelete();
	};
	
	$scope.canDelete = function() {
		return !$scope.canSave() && $scope.selection.length > 0;
	};

	$scope.filter = function(searchFilter) {
		
		var filter =  {}, sortBy, pageNum,
			domain = null,
			context = null,
			criteria = {
				operator: 'and'
			};

		var fields = _.pluck($scope.fields, 'name');
		for(var name in searchFilter) {
			var value = searchFilter[name];
			if (value !== '') filter[name] = value;
		}
		
		pageNum = +(filter._pageNum || 0);
		sortBy = filter._sortBy;
		domain = filter._domain;
		context = filter._context;
		
		delete filter._pageNum;
		delete filter._sortBy;
		delete filter._domain;
		delete filter._context;

		criteria.criteria = _.map(filter, function(value, key) {

			var field = $scope.fields[key] || {};
			var type = field.type || 'string';
			var operator = 'iContains';
			
			//TODO: implement expression parser
			
			if (type === 'many-to-one') {
				if (field.targetName) {
					key = key + '.' + field.targetName;
				} else {
					console.warn("Can't search on field: ", key);
				}
			}

			switch(type) {
				case 'integer':
				case 'long':
				case 'decimal':
					operator = 'iEquals';
					break;
				case 'boolean':
					operator = 'iEquals';
					value = !/f|n|false|no|0/.test(value);
					break;
				case 'date':
					operator = 'iEquals';
					value = moment(value, 'DD/MM/YYYY').format('YYYY-MM-DD'); //TODO: user date format
					break;
				case 'time':
					operator = 'iEquals';
					break;
				case 'datetime':
					operator = 'iEquals';
					value = moment(value, 'DD/MM/YYYY HH:mm').format(); //TODO: user datetime format
					break;
			}
			
			return {
				operator: operator,
				fieldName: key,
				value: value
			};
		});
		
		domain = domain || $scope._domain;
		if (domain && $scope.getContext) {
			context = _.extend({}, $scope._context, context, $scope.getContext());
		}
		
		var options = {
			filter: criteria,
			fields: fields,
			sortBy: sortBy,
			domain: domain,
			context: context
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
			viewScope.$broadcast("on:new");
		});
	};
	
	$scope.onEdit = function() {
		page.index = $scope.selection[0];
		$scope.switchTo('form');
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
		var fields = _.pluck($scope.fields, 'name');
		ds.search({
			fields: fields
		});
	};
	
	$scope.onSort = function(event, args) {

		var sortBy = [],
			fields = _.pluck($scope.fields, 'name');

		angular.forEach(args.sortCols, function(column){
			var name = column.sortCol.field;
			var spec = column.sortAsc ? name : '-' + name;
			sortBy.push(spec);
		});
		
		ds.search({
			sortBy: sortBy,
			fields: fields
		});
	};
	
	$scope.onSelectionChanged = function(event, args) {
		var selection = _.filter(args.rows, function(index) {
			var item = $scope.dataView.getItem(index);
			return item && item.id !== 0;
		});
		$scope.selection = selection;
		$scope.$apply();
	};
	
	$scope.onItemClick = function(event, args) {
		
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
	
	$scope.canEditInline = function() {
		return _.isFunction(this.dataView.canSave);
	};
	
	$scope.onSave = function() {
		this.dataView.saveChanges();
	};
	
	$scope.canSave = function() {
		return this.dataView.canSave && this.dataView.canSave();
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
}

angular.module('axelor.ui').directive('uiViewGrid', function(){
	return {
		replace: true,
		template: '<div ui-slick-grid></div>'
	};
});

angular.module('axelor.ui').directive('uiPortletGrid', function(){
	return {
		controller: ['$scope', '$element', function($scope, $element) {
			
			GridViewCtrl.call(this, $scope, $element);
			
			var ds = $scope._dataSource;
			
			$scope.onItemDblClick = function(event, args) {
				var selection = $scope.selection[0];
				var record = ds.at(selection);

				var tab = angular.copy($scope._viewParams);

				tab.viewType = "form";
				tab.recordId = record.id;
				tab.action = $scope._viewAction;

				setTimeout(function(){
					$scope.openTab(tab);
					$scope.$apply();
				});
			};
		}],
		template:
		'<div ui-view-grid x-view="schema" x-data-view="dataView" x-editable="false" x-no-filter="{{noFilter}}" x-handler="this">'+
			'<div ui-slick-grid></div>'+
		'</div>'
	};
});
