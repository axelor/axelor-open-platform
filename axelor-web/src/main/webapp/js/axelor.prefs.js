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
		$scope.confirmDirty(doClose);
	};
	
	function doClose() {
		if (!$scope.isDirty()) {
			var app = $scope.app || {};
			var rec = $scope.record || {};
			var act = rec.action || {};
	
			if (app.homeAction !== act.name) {
				app.homeAction = act.name;
			}
		}
		$location.path('/');
	};

	$scope.setEditable();
	$scope.show();
}

app.controller("UserCtrl", ['$scope', '$element', '$location', 'DataSource', 'ViewService', UserCtrl]);

}).call(this);