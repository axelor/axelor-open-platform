(function() {

var app = angular.module("axelor.app");

function UserCtrl($scope, $element, $location, DataSource, ViewService) {
	
	$scope._viewParams = {
		model: 'com.axelor.meta.db.MetaUser',
		viewType: 'form'
	};
	
	ViewCtrl($scope, DataSource, ViewService);
	FormViewCtrl($scope, $element);
	
	$scope.onClose = function() {
		$location.path('/');
	};

	$scope.setEditable();
	$scope.show();
}

app.controller("UserCtrl", ['$scope', '$element', '$location', 'DataSource', 'ViewService', UserCtrl]);

}).call(this);