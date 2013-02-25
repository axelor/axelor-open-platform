SearchViewCtrl.$inject = ['$scope', '$element', '$http', 'DataSource', 'ViewService', 'MenuService'];
function SearchViewCtrl($scope, $element, $http, DataSource, ViewService, MenuService) {
	
	var view = $scope._views['search'] || {};
	
	$scope._dataSource = DataSource.create('multi-search');
	
	setTimeout(function(){
		$scope.$apply(function(){
			if (view.deferred)
				view.deferred.resolve($scope);
		});
	});
	
	function fixFields(fields) {
		_.each(fields, function(field){
			if (field.type == 'reference')
				field.type = 'MANY_TO_ONE';
			
			if (field.type)
				field.type = field.type.toUpperCase();
			else
				field.type = 'STRING';
		});
		return fields;
	}
	
	$scope.show = function(viewPromise) {
		if (viewPromise == null) {
			viewPromise = $scope.loadView('search', view.name);
			viewPromise.success(function(fields, schema){
				$scope.initView(schema);
			});
		}
		
		$scope.onShow(viewPromise);
	};

	$scope.onShow = function() {
		
	};
	
	$scope.initView = function(schema) {
		
		var params = $scope._viewParams;
		
		$scope._searchFields = fixFields(schema.searchFields);
		$scope._resultFields = fixFields(schema.resultFields);

		$scope._searchView = schema;
		$scope.updateRoute();
		
		if (params.options && params.options.mode == "search") {
			$scope.setRouteOptions(params.options);
		}
	};
	
	$scope.getRouteOptions = function() {
		var args = [],
			query = $scope._routeSearch;

		return {
			mode: 'search',
			args: args,
			query: query
		};
	};

	$scope._routeSearch = null;
	$scope.setRouteOptions = function(options) {
		var opts = options || {},
			fields = $scope._searchFields || [],
			search = opts.search,
			record = {};

		if (!search || _.isEmpty(search)) {
			return;
		}

		if (angular.equals($scope._routeSearch, search)) {
			return $scope.updateRoute();
		}
		
		$scope._routeSearch = search;
		
		_.each(fields, function(field) {
			var value = search[field.name];
			if (value === undefined) {
				return;
			}
			if (field.target) {
				if (value) {
					record[field.name] = {id: +value};
					record[field.name][field.nameField] = value;
				}
			} else {
				record[field.name] = value;
			}
		});
		
		scopes.form.editRecord(record);
		$scope.doSearch();
	};
	
	var scopes = {};
	$scope._register = function(key, scope) {
		scopes[key] = scope;
	};
	
	$scope.doSearch = function() {
		var params = _.extend({}, scopes.form.record),
			empty =  _.chain(params).values().compact().isEmpty().value();
		if (empty)
			return $scope.doClear();

		var selected = (scopes.toolbar.record || {}).objectSelect;
		
		_.extend(params,{
			__name: view.name,
			__selected: _.isEmpty(selected) ? null : selected
		});

		var promise = $http.post('ws/search', {
			limit: view.limit || 80,
			data: params
		});
		
		promise.then(function(response){
			var res = response.data,
				records = res.data || [];
			
			// slickgrid expects unique `id` so generate them and store original one
			_.each(records, function(rec, i){
				rec._id = rec.id;
				rec.id = i + 1;
			});
			
			scopes.grid.setItems(records);
		});
		
	};
	
	$scope.doClear = function(all) {
		scopes.form.edit(null);
		scopes.grid.setItems([]);
		if (all) {
			scopes.toolbar.edit(null);
		}
	};
	
	$scope.doAction = function() {
		
		var action = scopes.toolbar.getMenuAction();
		if (action == null)
			return;
	
		var grid = scopes.grid,
			index = _.first(grid.selection),
			record = grid.dataView.getItem(index);
		
		action = action.action;
		record = _.extend({
			_action: action
		}, record);
		
		record.id = record._id;
		
		MenuService.action(action).success(function(result){

			if (!result.data) {
				return;
			}
			
			var view = result.data[0].view;
			
			tab = view;
			tab.action = _.uniqueId('$act');
			tab.viewType = 'form';
			
			tab.context = _.extend({}, tab.context, {
				_ref : record
			});
			
			$scope.openTab(tab);
		});
	};
}

SearchFormCtrl.$inject = ['$scope', '$element', 'ViewService'];
function SearchFormCtrl($scope, $element, ViewService) {
	
	FormViewCtrl.call(this, $scope, $element);
	$scope._register('form', $scope);
	
	$scope.$watch('_searchView', function(schema) {
		if (schema == null) return;
		var form = {
			title: 'Search',
			type: 'form',
			cols: 1,
			items: [{
				type: 'group',
				title: schema.title,
				items: schema.searchFields
			}]
		};

		var meta = { fields: schema.searchFields };
		ViewService.process(meta);
		
		$scope.fields = meta.fields;
		$scope.schema = form;
	});
}

SearchGridCtrl.$inject = ['$scope', '$element', 'ViewService'];
function SearchGridCtrl($scope, $element, ViewService) {
	
	GridViewCtrl.call(this, $scope, $element);
	$scope._register('grid', $scope);
	
	$scope.$watch('_searchView', function(schema) {
		if (schema == null) return;
		var view = {
			title: 'Search',
			type: 'grid',
			items: [{ name : '_modelTitle', title: 'Object' }].concat(schema.resultFields)
		};
		var meta = { fields: schema.resultFields };
		ViewService.process(meta);
		
		$scope.fields = meta.fields;
		$scope.schema = view;
	});
	
	$scope.onEdit = function() {

		var index = _.first(this.selection),
			records = this.dataView.getItems(),
			record = this.dataView.getItem(index),
			ids, domain, views;

		ids = _.chain(records).filter(function(rec){
				return rec._model == record._model;
			}).pluck('_id').value();

		domain = "self.id IN (" + ids.join(',') + ")";
		
		views = _.map(['form', 'grid'], function(type){
			var view = { type : type };
			var name = record["_" + type];
			if (name) view.name = name;
			return view;
		});

		var tab = {
			action: _.uniqueId('$act'),
			model: record._model,
			title: record._modelTitle,
			domain: domain,
			recordId: record._id,
			viewType: 'form',
			views: views
		};
		
		this.openTab(tab);
	};
	
	// disable sorting
	$scope.onSort = function() {
		
	};
}

SearchToolbarCtrl.$inject = ['$scope', '$element', '$http'];
function SearchToolbarCtrl($scope, $element, $http) {

	FormViewCtrl.call(this, $scope, $element);
	$scope._register('toolbar', $scope);
	
	var menus = {};
	
	function fetch(key, parent, request, response) {
		
		if (menus[key] != null) {
			return response(menus[key]);
		}

		var promise = $http.get('ws/search/menu', {
			params: {
				parent: parent
			}
		});
		promise.then(function(res){
			var data = res.data.data;
			data = _.map(data, function(item){
				return {
					id: item.name,
					action: item.action,
					value: item.title
				};
			});
			menus[key] = data;
			response(data);
		});
	}
	
	$scope.fetchRootMenus = function(request, response) {
		fetch('menuRoot', null, request, response);
	};
	
	$scope.fetchSubMenus = function(request, response) {
		fetch('menuSub', $scope.record.menuRoot, request, response);
	};
	
	$scope.fetchItemMenus = function(request, response) {
		fetch('menuItem', $scope.record.menuSub, request, response);
	};
	
	$scope.resetSelector = function(a, b) {
		_.each(arguments, function(name){
			if (_.isString(name)) {
				$scope.record[name] = null;
				menus[name] = null;
			}
		});
	};
	
	$scope.getMenuAction = function() {
		return _.find(menus.menuItem, function(item){
			return item.id === $scope.record.menuItem;
		});
	};

	$scope.$watch('_searchView', function(schema) {
		
		if (schema == null)
			return;
		
		$scope.fields = {
			'objectSelect' : {
				type : 'string',
				placeholder: _t('Search Objects'),
				multiple : true,
				selection : _.map(schema.selects, function(x) {
					return {
						value : x.model,
						title : x.title
					};
				})
			},
			'menuRoot' : {
				type : 'string',
				placeholder: _t('Action Category'),
				type: 'select-query',
				attrs: {
					query: 'fetchRootMenus',
					'ng-change': 'resetSelector("menuSub", "menuItem")'
				}
			},
			'menuSub' : {
				placeholder: _t('Action Sub-Category'),
				type: 'select-query',
				attrs: {
					query: 'fetchSubMenus',
					'ng-change': 'resetSelector($event, "menuItem")'
				}
			},
			'menuItem' : {
				placeholder: _t('Action'),
				type: 'select-query',
				attrs: {
					query: 'fetchItemMenus'
				}
			}
		};

		$scope.schema = {
			cols : 7,
			colWidths : '250px,auto,auto,150px,150px,150px,auto',
			type : 'form',
			items : [ {
				name : 'objectSelect',
				showTitle : false
			}, {
				type : 'button',
				title : _t('Search'),
				attrs: {
					'ng-click': 'doSearch()'
				}
			}, {
				type : 'button',
				title : _t('Clear'),
				attrs: {
					'ng-click': 'doClear(true)'
				}
			}, {
				name : 'menuRoot',
				showTitle : false
			}, {
				name : 'menuSub',
				showTitle : false
			}, {
				name : 'menuItem',
				showTitle : false
			}, {
				type : 'button',
				title : _t('Go'),
				attrs: {
					'ng-click': 'doAction()'
				}
			}, {
				type : 'spacer'
			} ]
		};
	});
}

angular.module('axelor.ui').directive('uiViewSearch', function(){
	return {
		controller: SearchViewCtrl,
		link: function(scope, element, attrs, ctrl) {
			
			element.on('keypress', '.search-view-form form:first', function(event){
				if (event.keyCode == 13 && $(event.target).is('input')){
					scope.doSearch();
				}
			});
			
			var grid = element.children('.search-view-grid');
			element.on('adjustSize', function(){
				if (!element.is(':visible'))
					return;
				grid.height(element.height() - grid.position().top);
			});
		}
	};
});

// ActionSelector (TODO: re-use search view toolbar)

ActionSelectorCtrl.$inject = ['$scope', '$element', '$attrs', '$http', 'MenuService'];
function ActionSelectorCtrl($scope, $element, $attrs, $http, MenuService) {

	FormViewCtrl.call(this, $scope, $element);
	var menus = {},
		category = $attrs.category;
	
	function fetch(key, request, response, params) {
		
		if (menus[key] != null) {
			return response(menus[key]);
		}

		var promise = $http.get('ws/search/menu', {
			params: params
		});
		promise.then(function(res){
			var data = res.data.data;
			data = _.map(data, function(item){
				return {
					id: item.name,
					action: item.action,
					value: item.title
				};
			});
			menus[key] = data;
			response(data);
		});
	}
	
	$scope.fetchRootMenus = function(request, response) {
		fetch('$menuRoot', request, response, {
			parent: '',
			category: category
		});
	};
	
	$scope.fetchSubMenus = function(request, response) {
		if (!$scope.record.$menuRoot) return;
		fetch('$menuSub', request, response, {
			parent: $scope.record.$menuRoot
		});
	};
	
	$scope.fetchItemMenus = function(request, response) {
		if (!$scope.record.$menuSub) return;
		fetch('$menuItem', request, response, {
			parent: $scope.record.$menuSub
		});
	};
	
	$scope.resetSelector = function(a, b) {
		_.each(arguments, function(name){
			if (_.isString(name)) {
				$scope.record[name] = null;
				menus[name] = null;
			}
		});
	};
	
	$scope.getMenuAction = function() {
		return _.find(menus.$menuItem, function(item){
			return item.id === $scope.record.$menuItem;
		});
	};
	
	$scope.doAction = function() {
		
		var action = $scope.getMenuAction();
		if (action == null)
			return;
	
		var context = $scope.$parent.getContext(),
			record;

		action = action.action;
		record = {
			id : context.id,
			_action: action,
			_model: context._model
		};

		MenuService.action(action).success(function(result){

			if (!result.data) {
				return;
			}
			
			var view = result.data[0].view;
			
			tab = view;
			tab.action = _.uniqueId('$act');
			tab.viewType = 'form';
			
			tab.context = _.extend({}, tab.context, {
				_ref : record
			});
			
			$scope.openTab(tab);
		});
	};

	$scope.fields = {
		'$menuRoot' : {
			type : 'string',
			placeholder: _t('Action Category'),
			type: 'select-query',
			attrs: {
				query: 'fetchRootMenus',
				'ng-change': 'resetSelector("$menuSub", "$menuItem")'
			}
		},
		'$menuSub' : {
			placeholder: _t('Action Sub-Category'),
			type: 'select-query',
			attrs: {
				query: 'fetchSubMenus',
				'ng-change': 'resetSelector($event, "$menuItem")'
			}
		},
		'$menuItem' : {
			placeholder: _t('Action'),
			type: 'select-query',
			attrs: {
				query: 'fetchItemMenus'
			}
		}
	};

	$scope.schema = {
		cols : 4,
		colWidths : '30%,30%,30%,10%',
		type : 'form',
		items : [ {
			name : '$menuRoot',
			showTitle : false
		}, {
			name : '$menuSub',
			showTitle : false
		}, {
			name : '$menuItem',
			showTitle : false
		}, {
			type : 'button',
			title : _t('Go'),
			attrs: {
				'ng-click': 'doAction()'
			}
		} ]
	};
}

angular.module('axelor.ui').directive('uiActionSelector', function(){
	return {
		scope: true,
		controller: ActionSelectorCtrl,
		template: '<div ui-view-form x-handler="this"></div>'
	};
});

