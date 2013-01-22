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
		
		var tabResource = {
			action: 'main.tab',
			controller: TabCtrl,
			template: "<span><!-- dummy template --></span>"
		};
		
		$routeProvider
		
		.when('/welcome', { action: 'welcome' })
		.when('/about', { action: 'about' })
		.when('/', { action: 'main' })

		.when('/ds/:resource', tabResource)
		.when('/ds/:resource/:mode', tabResource)
		.when('/ds/:resource/:mode/:state', tabResource)

		.otherwise({ redirectTo: '/' });
	}])
	.config(['$httpProvider', function(provider) {
		provider.responseInterceptors.push('httpIndicator');
		provider.defaults.transformRequest.push(onHttpStart);
	}])
	.factory('httpIndicator', ['$rootScope', '$q', function($rootScope, $q){
		
		var doc = $(document);
		var body = $('body');
		var blocker = $('<div>').appendTo('body').hide()
			.css({
				position: 'absolute',
				zIndex: 100000,
				width: '100%', height: '100%'
			});

		var blocked = false;
		
		function block() {
			if (blocked) return true;
			if (loadingCounter > 0) {
				blocked = true;
				doc.on("keydown.blockui mousedown.blockui", function(e) {
					if ($('#loginWindow').is(':visible')) {
						return;
					}
					e.preventDefault();
					e.stopPropagation();
				});
				body.css("cursor", "wait");
				blocker.show();
				unblock();
			}
			return blocked;
		}

		function unblock() {
			if (loadingCounter > 0) {
				return _.delay(unblock, 10);
			}
			doc.off("keydown.blockui mousedown.blockui");
			body.css("cursor", "");
			blocker.hide();
			blocked = false;
		}

		axelor.blockUI = function() {
			return block();
		};

		axelor.unblockUI = function() {
			return unblock();
		};

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

AppCtrl.$inject = ['$rootScope', '$scope', '$http', '$route', 'authService'];
function AppCtrl($rootScope, $scope, $http, $route, authService) {

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
				zIndex: 100001,
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

		var route = current.$route,
			path = route && route.action ? route.action.split('.') : null;

		if (path == null)
			return;

		$scope.routePath = path;
	});
	
	$scope.routePath = ["main"];
	$route.reload();
}

NavCtrl.$inject = ['$scope', '$rootScope', '$location', '$q', 'MenuService'];
function NavCtrl($scope, $rootScope, $location, $q, MenuService) {

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
	
	$scope.$on("on:update-route", update);

	function update(event) {
		
		var tab = $scope.selectedTab,
			scope = event.targetScope;

		if (!tab || !tab.action || scope !== tab.$viewScope || !scope.getRouteOptions) {
			return;
		}
		if (tab.action.indexOf('$act') > -1) {
			return;
		}

		var path = tab.action,
			opts = scope.getRouteOptions(),
			mode = opts.mode,
			args = opts.args;

		path = "/ds/" + path + "/" + mode;
		args = _.filter(args, function(arg) {
			return _.isNumber(args) || arg;
		});

		if (args.length) {
			path += "/" + args.join("/");
		}

		if ($location.$$path !== path) {
			$location.path(path);
			$location.search(opts.query);
		}
	}

	function findTab(key) {
		return _.find($scope.tabs, function(tab){
			return tab.action == key;
		});
	};
	
	var VIEW_TYPES = {
		'list' : 'grid',
		'edit' : 'form'
	};
	
	$scope.openTab = function(tab, options) {
		
		var tabs = $scope.tabs,
			found = findTab(tab.action);

		options = options || tab.options;
		
		if (options && options.mode) {
			tab.viewType = VIEW_TYPES[options.mode] || options.mode;
		}
		
		tab.options = options;

		if (!found) {
			found = tab;
			tabs.push(tab);
		}
		
		_.each(tabs, function(tab) { tab.selected = false; });

		found.selected = true;
		$scope.selectedTab = found;
		
		if (options && tab.$viewScope) {
			var view = tab.$viewScope._views[tab.viewType],
				promise = view ? view.deferred.promise : null;
			if (promise) {
				promise.then(function(viewScope) {
					viewScope.setRouteOptions(options);
				});
			}
		}
		
		setTimeout(function(){
			$.event.trigger('adjust');
		});
	};
	
	$scope.openTabByName = function(name, options) {

		var tab = findTab(name);
		if (tab) {
			return $scope.openTab(tab, options);
		}

		MenuService.action(name).success(function(result){

			if (!result.data) {
				return;
			}

			var view = result.data[0].view;
			
			if (view && (view.type || view.viewType) == 'html') {
				view.views = [{
					resource: view.resource,
					title: view.title,
					type: 'html'
				}];
			}

			tab = view;
			tab.action = name;
			
			$scope.openTab(tab, options);
		});
	};

	function closeTab(tab) {

		var tabs = $scope.tabs,
			index = _.indexOf(tabs, tab);

		// remove tab
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
	
	$scope.$watch('selectedTab.viewType', function(viewType){
		if (viewType) {
			setTimeout(function(){
				$.event.trigger('adjustSize');
			});
		}
	});
}

TabCtrl.$inject = ['$scope', '$location', '$routeParams'];
function TabCtrl($scope, $location, $routeParams) {
	
	var params = _.clone($routeParams),
		search = _.clone($location.$$search);

	var resource = params.resource,
		state = params.state,
		mode = params.mode;

	if (resource) {
        $scope.openTabByName(resource, {
        	mode: mode,
        	state: state,
        	search: search
        });
    }
}
