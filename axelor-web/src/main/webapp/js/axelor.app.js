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
	
	function hideLoading() {
		if (loadingTimer) {
			clearTimeout(loadingTimer);
			loadingTimer = null;
		}
		if (loadingCounter > 0) {
			return loadingTimer = _.delay(hideLoading, 300);
		}
		loadingTimer = _.delay(function () {
			loadingTimer = null;
			if (loadingElem) {
				loadingElem.fadeOut(100);
			}
		}, 100);
	}

	function onHttpStart(data, headersGetter) {
	
		updateLoadingCounter(1);
		
		if (loadingTimer) {
			clearTimeout(loadingTimer);
			loadingTimer = null;
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
		hideLoading();
	}

	// screen size detection
	Object.defineProperty(axelor, 'device', {
		enumerable: true,
		get: function () {
			var device = {
				small: false,
				large: false
			};
			device.large = $(window).width() > 768;
			device.small = !device.large;
			device.mobile = /Mobile|Android|iPhone|iPad|iPod|BlackBerry|Windows Phone/i.test(navigator.userAgent);
			device.webkit = /Webkit/i.test(navigator.userAgent);
			return device;
		}
	});

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
		evalScope.$iif = function(c, t, f) { return c ? t : f; };

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

	axelor.$adjustSize = _.debounce(function () {
		$.event.trigger('adjustSize');
	}, 100);

	var module = angular.module('axelor.app', ['axelor.ng', 'axelor.ds', 'axelor.ui', 'axelor.auth']);
	
	module.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
		var tabResource = {
			action: 'main.tab',
			controller: 'TabCtrl',
			template: "<span><!-- dummy template --></span>"
		};
		
		$routeProvider
		
		.when('/preferences', { action: 'preferences' })
		.when('/welcome', { action: 'welcome' })
		.when('/about', { action: 'about' })
		.when('/system', { action: 'system' })
		.when('/', { action: 'main' })

		.when('/ds/:resource', tabResource)
		.when('/ds/:resource/:mode', tabResource)
		.when('/ds/:resource/:mode/:state', tabResource)

		.otherwise({ redirectTo: '/' });
	}]);
	
	module.config(['$httpProvider', function(provider) {
		provider.responseInterceptors.push('httpIndicator');
		provider.defaults.transformRequest.push(onHttpStart);
	}]);
	
	module.factory('httpIndicator', ['$rootScope', '$q', function($rootScope, $q){
		
		var doc = $(document);
		var body = $('body');
		var blocker = $('<div class="blocker-overlay"></div>')
			.appendTo('body')
			.hide()
			.css({
				position: 'absolute',
				zIndex: 100000,
				width: '100%', height: '100%'
			});
		
		var spinner = $('<div class="blocker-wait"></div>')
			.append('<div class="blocker-spinner"><i class="fa fa-spinner fa-spin"></div>')
			.append('<div class="blocker-message">' + _t('Please wait...') + '</div>')
			.appendTo(blocker);

		var blocked = false;
		var blockedCounter = 0;
		var blockedTimer = null;
		var spinnerTimer = 0;

		function block(callback) {
			if (blocked) return true;
			if (blockedTimer) { clearTimeout(blockedTimer); blockedTimer = null; };
			if (loadingCounter > 0 || blockedCounter > 0) {
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
			if (blockedTimer) { clearTimeout(blockedTimer); blockedTimer = null; };
			if (loadingCounter > 0 || blockedCounter > 0 || loadingTimer) {
				spinnerTimer += 1;
				if (spinnerTimer > 300) {
					blocker.addClass('wait');
				}
				if (blockedCounter > 0) {
					blockedCounter = blockedCounter - 10;
				}
				return blockedTimer = _.delay(unblock, 10, callback);
			}
			doc.off("keydown.blockui mousedown.blockui");
			body.css("cursor", "");
			blocker.removeClass('wait').hide();
			spinnerTimer = 0;
			if (callback) {
				callback(blocked);
			}
			blocked = false;
		}

		axelor.blockUI = function(callback, minimum) {
			if (minimum && minimum > blockedCounter) {
				blockedCounter = Math.max(0, blockedCounter);
				blockedCounter = Math.max(minimum, blockedCounter);
			}
			return block(callback);
		};

		axelor.unblockUI = function() {
			return unblock();
		};

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
	}]);
	
	module.filter('t', function(){
		return function(input) {
			var t = _t || angular.nop;
			return t(input);
		};
	});
	
	module.directive('translate', function(){
		return function(scope, element, attrs) {
			var t = _t || angular.nop;
			setTimeout(function(){
				element.text(t(element.text()));
			});
		};
	});
	
	module.controller('AppCtrl', AppCtrl);
	
	AppCtrl.$inject = ['$rootScope', '$exceptionHandler', '$scope', '$http', '$route', 'authService'];
	function AppCtrl($rootScope, $exceptionHandler, $scope, $http, $route, authService) {
		
		function getAppInfo(settings) {
			
			var info = {
				name: settings['application.name'],
				description: settings['application.description'],
				version: settings['application.version'],
				author: settings['application.author'],
				copyright: settings['application.copyright'],
				home: settings['application.home'],
				help: settings['application.help'],
				mode: settings['application.mode'],
				sdk: settings['application.sdk'],
				user: settings['user.name'],
				login: settings['user.login'],
				homeAction: settings['user.action'],
				navigator: settings['user.navigator'],
				fileUploadSize: settings['file.upload.size']
			};

			if (settings['view.confirm.yes-no'] === true) {
				_.extend(axelor.dialogs.config, {
					yesNo: true
				});
			}
			
			return info;
		}
	
		function appInfo() {
			$http.get('ws/app/info').then(function(response){
				var settings = response.data;
				angular.extend($scope.app, getAppInfo(settings));
			});
		}
	
		// See index.jsp
		$scope.app = getAppInfo(__appSettings);
		$scope.$year = moment().year();
	
		var loginAttempts = 0;
		var loginWindow = null;
		var errorWindow = null;
		
		function showLogin(hide) {
			
			if (loginWindow == null) {
				loginWindow = $('#loginWindow')
				.attr('title', _t('Log in'))
				.dialog({
					dialogClass: 'no-close ui-dialog-responsive ui-dialog-small',
					autoOpen: false,
					modal: true,
					position: "center",
					width: "auto",
					resizable: false,
					closeOnEscape: false,
					zIndex: 100001,
					buttons: [{
						text: _t("Log in"),
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
					dialogClass: 'ui-dialog-responsive',
					draggable: true,
					resizable: false,
					closeOnEscape: true,
					modal: true,
					zIndex: 1100,
					width: 420,
					close: function() {
						$scope.httpError = {};
						$scope.$apply();
					},
					buttons: [{
						text: _t("Show Details"),
						'class': 'btn',
						click: function(){
							$scope.onErrorWindowShow('stacktrace');
							$scope.applyLater();
							axelor.$adjustSize();
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
			} else if (_.isString(report)) {
				stacktrace = report.replace(/.*<body>|<\/body>.*/g, '');
			} else {
				return; // no error report, so ignore
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

})(jQuery);
