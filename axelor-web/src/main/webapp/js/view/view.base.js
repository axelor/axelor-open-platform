ViewCtrl.$inject = ['$scope', 'DataSource', 'ViewService'];
function ViewCtrl($scope, DataSource, ViewService) {

	if ($scope._viewParams == null) {
		$scope._viewParams = $scope.selectedTab;
	}

	if ($scope._viewParams == null) {
		throw "View parameters are not provided.";
	}

	var params = $scope._viewParams;

	$scope._views = ViewService.accept(params);
	$scope._viewType = params.viewType;

	$scope._model = params.model;
	$scope._fields = {};

	$scope._dataSource = null;
	$scope._domain = params.domain;
	$scope._context = params.context;
	
	if (params.model) {
		$scope._dataSource = DataSource.create(params.model, params);
	}
	
	$scope._defer = function() {
		return ViewService.defer();
	};

	$scope.loadView = function(viewType, viewName) {
		var view = $scope._views[viewType];
		if (view == null) {
			view = {
				type: viewType,
				name: viewName
			};
		}
		return ViewService.getMetaDef($scope._model, view);
	};

	$scope.switchTo = function(viewType) {

		var view = $scope._views[viewType],
			callback = arguments.length > 1 ? arguments[1] : null;
		if (view == null) {
			return;
		}
		
		var promise = view.deferred.promise;
		promise.then(function(viewScope){

			if (viewScope == null) {
				return;
			}

			$scope._viewType = viewType;
			$scope._viewParams.viewType = viewType; //XXX: remove
			viewScope.show();
			
			// store viewScope (for NavCtrl usages, dirty check)
			$scope._viewParams.$viewScope = viewScope;

			if (callback) {
				callback(viewScope);
			}
		});
	};
	
	if (!params.action) {
		return;
	}

	// show single or default record if specified
	var context = params.context || {};
	if (context._showSingle || context._showRecord) {
		var ds = DataSource.create(params.model, params);
		
		function doEdit(id) {
			$scope.switchTo('form', function(scope){
				scope._viewPromise.then(function(){
					scope.doRead(id).success(function(record){
						scope.edit(record);
					});
				});
			});
		}
		
		if (context._showRecord > 0) {
			return doEdit(context._showRecord);
		}

		return ds.search({
			offset: 0,
			limit: 2,
			fields: ["id"]
		}).success(function(records, page){
			if (page.total === 1 && records.length === 1) {
				return doEdit(records[0].id);
			}
			return $scope.switchTo($scope._viewType || 'grid');
		});
	}
	
	// switch to the the current viewType
	$scope.switchTo($scope._viewType || 'grid');
}

/**
 * Base controller for DataSource views. This controller should not be used
 * directly but actual controller should inherit from it.
 * 
 */
function DSViewCtrl(type, $scope, $element) {

	if (type == null) {
		throw "No view type provided.";
	}
	if ($scope._dataSource == null) {
		throw "DataSource is not provided.";
	}
	
	$scope._viewResolver = $scope._defer();
	$scope._viewPromise = $scope._viewResolver.promise;

	var ds = $scope._dataSource;
	var view = $scope._views[type] || {};
	var viewPromise = null;
	var hiddenButtons = {};

	$scope.fields = {};
	$scope.schema = null;
	
	setTimeout(function(){
		$scope.$apply(function(){
			if (view.deferred)
				view.deferred.resolve($scope);
		});
	});

	$scope.show = function() {
		if (viewPromise == null) {
			viewPromise = $scope.loadView(type, view.name);
			viewPromise.success(function(fields, schema){
				var toolbar = [];
				_.each(schema.toolbar, function(button){
					if (/new|edit|save|delete|copy|cancel|refresh|search/.test(button.name))
						return hiddenButtons[button.name] = button.hidden;
					toolbar.push(button);
				});

				$scope.fields = fields;
				$scope.schema = schema;
				$scope.toolbar = toolbar;
			});
		}
		
		$scope.onShow(viewPromise);
	};
	
	$scope.onShow = function(promise) {
		
	};

	$scope.canNext = function() {
		return ds && ds.canNext();
	};

	$scope.canPrev = function() {
		return ds && ds.canPrev();
	};
	
	$scope.hasButton = function(name) {
		if (_(hiddenButtons).has(name))
			return !hiddenButtons[name];
		return name !== 'copy';
	};
}
