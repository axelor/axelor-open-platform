/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
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
}

function onHttpStart(data, headersGetter) {

	updateLoadingCounter(1);
	
	if (loadingTimer) clearTimeout(loadingTimer);
	if (loadingCounter > 1) {
		return data;
	}
	
	if (loadingElem == null) {
		loadingElem = $('<div><span class="label label-important loading-counter">' + _t('Loading') + '...</span></div>')
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
			controller: 'TabCtrl',
			template: "<span><!-- dummy template --></span>"
		};
		
		$routeProvider
		
		.when('/preferences', { action: 'preferences' })
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
		
		function block(callback) {
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
			}
			unblock(callback);
			return blocked;
		}

		function unblock(callback) {
			if (loadingCounter > 0) {
				return _.delay(unblock, 10, callback);
			}
			doc.off("keydown.blockui mousedown.blockui");
			body.css("cursor", "");
			blocker.hide();
			if (callback) {
				callback(blocked);
			}
			blocked = false;
		}

		axelor.blockUI = function() {
			return block(arguments[0]);
		};

		axelor.unblockUI = function() {
			return unblock();
		};
		
		function ajaxStop(callback, context) {
			var wait = _.last(arguments);
			if (!wait || !_.isNumber(wait)) {
				wait = 10;
			}
			if (loadingCounter > 0) {
				return _.delay(ajaxStop, wait, callback, context);
			}
			if (callback) {
				_.delay(callback, wait, context);
			}
		};
		
		function applyLater(wait, func) {
			var that = this,
				args = _.rest(arguments, _.isFunction(func) ? 2: 1);

			if (_.isFunction(wait)) {
				func = wait;
				wait = 0;
			}
			
			func = func || angular.noop;
			
			return setTimeout(function(){
		    	return that.$apply(function() {
		    		return func.apply(null, args);
		    	});
		    }, wait || 0);
		}

		var proto = Object.getPrototypeOf($rootScope);
		_.extend(proto, {
			ajaxStop: ajaxStop,
			applyLater: applyLater
		});

		return function(promise) {
			return promise.then(function(response){
				onHttpStop();
				if (response.data && response.data.status === -1) {
					$rootScope.$broadcast('event:http-error', response.data);
					return $q.reject(response);
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

	// some helpers
	
	axelor.$eval = function (scope, expr, context) {
		if (!scope || !expr) {
			return null;
		}

		var evalScope = scope.$new(true);
		
		function isValid(name) {
			if (!name) {
				if (_.isFunction(scope.isValid)) {
					return scope.isValid();
				}
				return scope.isValid === undefined || scope.isValid;
			}
		
			var ctrl = scope.form;
			if (ctrl) {
				ctrl = ctrl[name];
			}
			if (ctrl) {
				return ctrl.$valid;
			}
			return true;
		}

		evalScope.$get = function(n) {
			var context = this.$context || {};
			if (context.hasOwnProperty(n)) {
				return context[n];
			}
			return evalScope.$eval(n, context);
		};
		evalScope.$moment = function(d) { return moment(d); };		// moment.js helper
		evalScope.$number = function(d) { return +d; };				// number helper
		evalScope.$popup = function() { return scope._isPopup; };	// popup detect

		evalScope.$contains = function(iter, item) {
			if (iter && iter.indexOf)
				return iter.indexOf(item) > -1;
			return false;
		};
		
		evalScope.$readonly = scope.isReadonly ? _.bind(scope.isReadonly, scope) : angular.noop;
		evalScope.$required = scope.isRequired ? _.bind(scope.isRequired, scope) : angular.noop;

		evalScope.$valid = function(name) {
			return isValid(scope, name);
		};

		evalScope.$invalid = function(name) {
			return !isValid(scope, name);
		};
		
		try {
			evalScope.$context = context;
			return evalScope.$eval(expr, context);
		} finally {
			evalScope.$destroy();
			evalScope = null;
		}
	};

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
			login: settings['user.login'],
			homeAction: settings['user.action'],
			navigator: settings['user.navigator'],
			help: settings['help.location'],
			sdk: settings['sdk.version'],
			fileMaxSize: settings['file.max.size']
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
					'class': 'btn btn-primary',
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
					'class': 'btn',
					click: function(){
						$scope.onErrorWindowShow('stacktrace');
						$scope.$apply();
					}
				}, {
					text: _t("Close"),
					'class': 'btn btn-primary',
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
