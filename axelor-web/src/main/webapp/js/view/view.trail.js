(function(){

var ui = angular.module('axelor.ui');

this.FormListCtrl = FormListCtrl;
this.FormListCtrl.$inject = ["$scope", "$element", "$compile", "DataSource", "ViewService"];

function FormListCtrl($scope, $element, $compile, DataSource, ViewService) {
	
	DSViewCtrl('form', $scope, $element);

	$scope._viewParams.$viewScope = $scope;
	
	var view = $scope._views['form'];
	setTimeout(function(){
		$scope.$apply(function(){
			if (view.deferred)
				view.deferred.resolve($scope);
		});
	});
	
	var ds = $scope._dataSource;
	var params = $scope._viewParams.params || {};
	var sortBy = params['trail-order'] && params['trail-order'].split(/\s*,\s*/);

	ds.on('change', function(e, records, page){
		$scope.records = records;
	});

	$scope.onShow = function(viewPromise) {
		viewPromise.then(function() {
			$scope.updateRoute();
			if ($scope.records === undefined)
				$scope.onReload();
		});
	};

	$scope.getRouteOptions = function() {
		return {
			mode: "trail"
		};
	};
	
	$scope.setRouteOptions = function(options) {
		$scope.updateRoute();
	};
	
	$scope.onNew = function() {
		this._canCreate = true;
	};
	
	$scope.onCancel = function() {
		this._canCreate = false;
	};
	
	$scope.onReload = function() {
		this._canCreate = false;
		ds.search({
			sortBy: sortBy
		}).success(function(records, page) {
		});
	};
	
	$scope.canCreate = function() {
		return this._canCreate;
	};
	
	$scope.show();
}

ui.directive('uiFormList', function() {

	return {
		
		controller: FormListCtrl,
		
		link: function(scope, element, attrs) {

		},
		
		template: '<div class="trail-list">'+
			'<div ng-repeat="item in records" ui-trail-form></div>'+
		'</div>'
	};
});

TrailFormCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function TrailFormCtrl($scope, $element, DataSource, ViewService) {
	
	var params = $scope.$parent._viewParams;
	var view = _.find(params.views, function(view) {
		return view.type === 'form';
	});
	
	if ($scope._editForm) {
		view = {
			type: view.type,
			name: $scope._editForm
		};
	}

	params = _.extend({}, {
		'title': params.title,
		'model': params.model,
		'domain': params.domain,
		'context': params.context,
		'viewType': 'form',
		'views': [view],
		'params': params.params
	});

	$scope._viewParams = params;

	ViewCtrl.call(this, $scope, DataSource, ViewService);
	FormViewCtrl.call(this, $scope, $element);
	
	// trail forms are by default editable
	$scope.setEditable(true);
	
	$scope.updateRoute = function(options) {
		
	};
	
	$scope.confirmDirty = function(fn) {
		return fn();
	};
	
	$scope.onShow = function(viewPromise) {
		
		viewPromise.then(function() {
			$scope.edit($scope.$parent.item);
		});
	};
	
	$scope.$on("on:new", function() {
		var trailScope = $scope.$parent.$parent;
		if (trailScope) {
			trailScope.onReload();
		}
	});
	
	$scope.show();
}

function trailWidth(scope, element) {
	var params = scope._viewParams.params || {};
	var width = params['trail-width'];
	if (width) {
		element.width(width);
	}
}

ui.directive("uiTrailForm", function() {

	return {
		scope: {},
		replace: true,
		controller: TrailFormCtrl,
		link: function(scope, element, attrs) {
			trailWidth(scope, element);
		},
		template:
		'<div ui-view-form x-handler="this" class="trail-form"></div>'
	};
});

TrailEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function TrailEditorCtrl($scope, $element, DataSource, ViewService) {
	
	var params = _.clone($scope.$parent._viewParams);
	if (params.params && params.params['trail-edit-form']) {
		$scope._editForm = params.params['trail-edit-form'];
	}
	
	TrailFormCtrl.apply(this, arguments);
}

ui.directive("uiTrailEditor", function() {

	return {
		scope: {},
		controller: TrailEditorCtrl,
		link: function(scope, element, attrs) {
			trailWidth(scope, element);
		},
		template:
		'<div ui-trail-form x-handler="this" class="trail-editor"></div>'
	};
});

}).call(this);
