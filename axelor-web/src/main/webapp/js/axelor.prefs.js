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
(function() {

var app = angular.module("axelor.app");

function UserCtrl($scope, $element, $location, DataSource, ViewService) {
	
	$scope._viewParams = {
		model: 'com.axelor.auth.db.User',
		views: [{name: 'user-preferences-form', type: 'form'}]
	};
	
	ViewCtrl($scope, DataSource, ViewService);
	FormViewCtrl($scope, $element);
	
	$scope.onClose = function() {
		$scope.confirmDirty(doClose);
	};

	var __version = null;
	var __getContext = $scope.getContext;
	
	$scope.getContext = function() {
		var context = __getContext.apply($scope, arguments) || {};
		if (!context.code) {
			context.code = __appSettings['user.login'];
		}
		return context;
	};
	
	$scope.$watch('record.version', function (value) {
		if (value === null || value === undefined) return;
		if (__version !== null) return 
		__version = value;
	});
	
	function doClose() {
		if (!$scope.isDirty()) {
			var app = $scope.app || {};
			var rec = $scope.record || {};
			app.homeAction = rec.homeAction;
		}
		
		window.history.back();
		
		if (__version === ($scope.record || {}).version) {
			return;
		}

		setTimeout(function() {
			window.location.reload();
		}, 100);
	};

	$scope.setEditable();
	$scope.show();
	
	$scope.ajaxStop(function () {
		$scope.applyLater();
	});
}

function SystemCtrl($scope, $element, $location, $http) {

	var promise = null;

	$scope.onRefresh = function () {
		if (promise) {
			return;
		}
		promise = $http.get("ws/app/sysinfo").then(function (res) {
			var info = res.data;
			_.each(info.users, function (item) {
				item.loginTime = moment(item.loginTime).format('L LT');
				item.accessTime = moment(item.accessTime).format('L LT');
			});
			$scope.info = info;
			promise = null;
		});
		return promise;
	};

	$scope.onClose = function () {
		window.history.back();
	};

	$scope.onRefresh();
}

app.controller("UserCtrl", ['$scope', '$element', '$location', 'DataSource', 'ViewService', UserCtrl]);
app.controller("SystemCtrl", ['$scope', '$element', '$location', '$http', SystemCtrl]);

}).call(this);