/**
 * Application Module
 * 
 */
(function($, undefined){
	
var loadingElem = null,
	loadingTimer = null,
	loadingCounter = 0;

function updateLoadingCounter(val) {
	loadingCounter += val;
	if (loadingElem)
		loadingElem.children('span').text(_t('Loading') + ' (' + loadingCounter + ')...');
}

function onHttpStart(data, headersGetter) {

	updateLoadingCounter(1);
	
	if (loadingTimer) clearTimeout(loadingTimer);
	if (loadingCounter > 1) {
		return data;
	}
	
	if (loadingElem == null) {
		loadingElem = $('<div><span class="label label-important" style="padding: 8px 8px 4px 8px;">' + _t('Loading') + '...</span></div>')
			.css({
				position: 'fixed',
				top: 0,
				width: '100%',
				'text-align': 'center',
				'z-index': 2000
			}).appendTo('body');
	}
	loadingElem.show();
	return data;
}

function onHttpStop() {
	updateLoadingCounter(-1);
	loadingTimer = setTimeout(function(){
		if (loadingElem && loadingCounter === 0)
			loadingElem.fadeOut();
	}, 100);
}

angular.module('axelor.app', ['axelor.ds', 'axelor.ui', 'axelor.auth'])
	.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
		$routeProvider
		.when('/welcome', {
			action: 'welcome'
		})
		.when('/about', {
			action: 'about'
		})
		.when('/', {
			action: 'main'
		})
		.when('/ds/:resource', {
			action: 'main.tab',
			controller: TabCtrl,
			template: "<span><!-- dummy template --></span>"
		})
		.otherwise({
			redirectTo: '/welcome'
		});
	}])
	.config(['$httpProvider', function(provider) {
		provider.responseInterceptors.push('httpIndicator');
		provider.defaults.transformRequest.push(onHttpStart);
	}])
	.factory('httpIndicator', ['$rootScope', '$q', function($rootScope, $q){
		return function(promise) {
			return promise.then(function(response){
				onHttpStop();
				if (response.data && response.data.status === -1) {
					$rootScope.$broadcast('event:http-error', response.data);
					return $q.reject(response.data.message || response.data.string);
				}
				return response;
			}, function(error) {
				onHttpStop();
				$rootScope.$broadcast('event:http-error', error);
				return $q.reject(error);
			});
		};
	}])
	.filter('t', function(){
		return function(input) {
			var t = _t || angular.nop;
			return t(input);
		};
	})
	.directive('translate', function(){
		return function(scope, element, attrs) {
			var t = _t || angular.nop;
			setTimeout(function(){
				element.text(t(element.text()));
			});
		};
	});

})(jQuery);

AppCtrl.$inject = ['$scope', '$http', '$route', 'authService'];
function AppCtrl($scope, $http, $route, authService) {

	function getAppInfo(settings) {
		return {
			name: settings['application.name'],
			description: settings['application.description'],
			version: settings['application.version'],
			mode: settings['application.mode'],
			user: settings['user.name'],
			help: settings['help.location']
		};
	}

	function appInfo() {
		$http.get('ws/app/info').then(function(response){
			var settings = response.data;
			angular.extend($scope.app, getAppInfo(settings));
		});
	}

	// See index.jsp
	$scope.app = getAppInfo(__appSettings);
	
	var loginAttempts = 0;
	var loginWindow = null;
	var errorWindow = null;
	
	function showLogin(hide) {
		
		if (loginWindow == null) {
			loginWindow = $('#loginWindow')
			.attr('title', _t('Login'))
			.dialog({
				autoOpen: false,
				modal: true,
				position: "center",
				width: "auto",
				resizable: false,
				closeOnEscape: false,
				dialogClass: 'no-close',
				buttons: [{
					text: _t("Login"),
					click: function(){
						$scope.doLogin();
					}
				}]
			});
	
			$('#loginWindow input').keypress(function(event){
				if (event.keyCode === 13)
					$scope.doLogin();
			});
		}
		return loginWindow.dialog(hide ? 'close' : 'open').height('auto');
	}

	function showError(hide) {
		if (errorWindow == null) {
			errorWindow = $('#errorWindow')
			.attr('title', _t('Error'))
			.dialog({
				modal: true,
				position: "center",
				width: 480,
				resizable: false,
				close: function() {
					$scope.httpError = {};
					$scope.$apply();
				},
				buttons: [{
					text: _t("Show Details"),
					click: function(){
						$scope.onErrorWindowShow('stacktrace');
						$scope.$apply();
					}
				}, {
					text: _t("Close"),
					click: function() {
						errorWindow.dialog('close');
					}
				}]
			});
		}
		
		return errorWindow.dialog(hide ? 'close' : 'open').height('auto');
	}

	$scope.doLogin = function() {
		
		var data = {
			username: $('#loginWindow form input:first').val(),
			password: $('#loginWindow form input:last').val()
		};
		
		$http.post('login.jsp', data).then(function(response){
			authService.loginConfirmed();
			$('#loginWindow form input').val('');
			$('#loginWindow .alert').hide();
		});
	};
	
	$scope.$on('event:auth-loginRequired', function(event, status) {
		$('#loginWindow .alert').hide();
		showLogin();
		if (loginAttempts++ > 0)
			$('#loginWindow .alert.login-failed').show();
		if (status === 0 || status === 502)
	       $('#loginWindow .alert.login-offline').show();
		setTimeout(function(){
			$('#loginWindow input:first').focus();
		}, 300);
	});
	$scope.$on('event:auth-loginConfirmed', function() {
		showLogin(true);
		loginAttempts = 0;
		appInfo();
	});
	
	$scope.httpError = {};
	$scope.$on('event:http-error', function(event, data) {
		var message = _t("Internal Server Error"),
			report = data.data || data, stacktrace = null, cause = null, exception;
		
		if (report.stacktrace) {
			message = report.message || report.string;
			exception = report['class'] || '';
			
			if (exception.match(/(OptimisticLockException|StaleObjectStateException)/)) {
				message = "<b>" + _t('Concurrent updates error.') + '</b><br>' + message;
			}

			stacktrace = report.stacktrace;
			cause = report.cause;
		} else {
			stacktrace = report.replace(/.*<body>|<\/body>.*/g, '');
		}
		_.extend($scope.httpError, {
			message: message,
			stacktrace: stacktrace,
			cause: cause
		});
		showError();
	});
	$scope.onErrorWindowShow = function(what) {
		$scope.httpError.show = what;
	};
	
	$scope.$on('$routeChangeSuccess', function(event, current, prev) {

		var action = current.$route.action,
			path = action ? action.split('.') : null;

		if (path == null)
			return;

		$scope.routePath = path;
	});
	
	$scope.routePath = ["main"];
	$route.reload();
}

NavCtrl.$inject = ['$scope', '$rootScope', '$location', '$routeParams', 'MenuService'];
function NavCtrl($scope, $rootScope, $location, $routeParams, MenuService) {

	$scope.tabs = [];
	$scope.selectedTab = null;
	
	$scope.menuClick = function(event, record) {
		if (record.isFolder)
			return;
		$scope.openTabByName(record.action);
		$scope.$apply();
	};
	
	$scope.navClick = function(tab) {
		$scope.openTab(tab);
	};
	
	function findTab(key) {
		return _.find($scope.tabs, function(tab){
			return tab.action == key;
		});
	};
	
	$scope.openTab = function(tab) {
	
		var tabs = $scope.tabs;

		_.each(tabs, function(tab){
			tab.selected = false;
		});
		
		var found = findTab(tab.action);
		
		if (!found) {
			found = tab;
			tabs.push(tab);
		}
		
		found.selected = true;
		$scope.selectedTab = found;

		var resource = found.action;
		var path = '/ds/' + resource;
		
		// don't change location for views opened with button actions
		if (resource.indexOf('$act') != 0)
			$location.path(path);

		setTimeout(function(){
			$.event.trigger('adjust');
		});
	};
	
	$scope.openTabByName = function(name) {

		var tab = findTab(name);
		if (tab == null) {
			
			MenuService.action(name).success(function(result){

				if (!result.data) {
					return;
				}
				
				var view = result.data[0].view;
				
				if (view && view.viewType == 'html') {
					view.views = [{
						resource: view.resource,
						title: view.title,
						type: 'html'
					}];
				}
				
				tab = view;
				tab.action = name;
				$scope.openTab(tab);
			});
		}
		else
			$scope.openTab(tab);
	};
	
	function closeTab(tab) {

		var tabs = $scope.tabs,
			index = _.indexOf(tabs, tab);
		
		//TODO: garbage collection
		tabs.splice(index, 1);
		
		if (tab.selected) {
			if (index == tabs.length)
				index -= 1;
			_.each(tabs, function(tab){
				tab.selected = false;
			});
			var select = tabs[index];
			if (select) {
				select.selected = true;
				$scope.openTab(select);
			}
		}
	}
	
	$scope.closeTab = function(tab) {
		var viewScope = tab.$viewScope;
		if (viewScope && viewScope.confirmDirty) {
			viewScope.confirmDirty(function(){
				closeTab(tab);
			});
		} else {
			closeTab(tab);
		}
	};
	
	$scope.$watch('selectedTab.viewType', function(tab){
		if (tab == null)
			return;
		setTimeout(function(){
			$.event.trigger('adjustSize');
		}, 200);
	});
}

TabCtrl.$inject = ['$scope', '$rootScope', '$location', '$routeParams'];
function TabCtrl($scope, $rootScope, $location, $routeParams) {
	
	var resource = $routeParams['resource'];
    if (resource) {
        $scope.openTabByName(resource);
    }
}
