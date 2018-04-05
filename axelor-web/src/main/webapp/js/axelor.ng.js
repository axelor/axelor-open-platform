/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

	"use strict";
	
	// create global axelor namespace exists
	window.axelor = window.axelor || {};
	window.axelor.config = {};

	var module = angular.module('axelor.ng', []);

	module.config(['$provide', function($provide) {

		$provide.decorator('$rootScope', ['$delegate', '$exceptionHandler', '$injector', function ($rootScope, $exceptionHandler, $injector) {
			
			var __orig__ = Object.getPrototypeOf($rootScope),
				__super__ = {},
				__custom__ = {};

			for (var name in __orig__) {
				if (angular.isFunction(__orig__[name])) {
					__super__[name] = __orig__[name];
				}
			}

			var $q = null,
				$http = null,
				$timeout = null;
			
			var ajaxStopQueue = [];
			var ajaxStopPending = null;
			
			function processAjaxStopQueue(scope) {

				function doCall(params) {
					var cb = params[0];
					var ctx = params[1];
					var wait = params[2];
					if (wait) {
						setTimeout(function () { scope.$applyAsync(cb.bind(ctx)); }, wait);
					} else {
						scope.$applyAsync(cb.bind(ctx));
					}
				}

				if (ajaxStopPending) {
					clearTimeout(ajaxStopPending);
					ajaxStopPending = null;
				}

				if ($http === null) {
					$http = $injector.get('$http');
				}

				while (($http.pendingRequests || []).length === 0 && ajaxStopQueue.length > 0) {
					doCall(ajaxStopQueue.shift());
				}

				if (ajaxStopQueue.length > 0) {
					ajaxStopPending = _.delay(processAjaxStopQueue, 100, scope);
				}
			}

			__custom__.ajaxStop = function ajaxStop(callback, context, wait) {
				if (_.isNumber(context)) {
					wait = context;
					context = undefined;
				}
				if (_.isFunction(callback)) {
					ajaxStopQueue.push([callback, context, wait]);
				}
				if (!ajaxStopPending) {
					processAjaxStopQueue(this);
				}
			};

			// expose _t() to use in template
			__custom__._t = _t;

			__custom__.$actionPromises = [];
			__custom__.waitForActions = function waitForActions(callback, wait) {
				if ($q === null) {
					$q = $injector.get('$q');
				}
				var that = this;
				var args = arguments;
				var waitFor = wait || 10;
				setTimeout(function () {
					// wait for any pending ajax requests
					that.ajaxStop(function () {
						var all = args.length === 3 ? args[2] : that.$actionPromises;
						// wait for actions
						$q.all(all).then(function () {
							// if new actions are executed, wait for them
							if (args.length !== 3 && that.$actionPromises.length) {
								return _.delay(waitForActions.bind(that), 10, callback);
							}
							if (callback) {
								callback();
							}
						}, callback);
					});
				}, waitFor);
			};

			__custom__.$callWhen = function (predicate, callback, wait) {
				var count = wait || 100;
				var that = this;

				function later() {
					if (count-- === 0 || (_.isFunction(predicate) && predicate())) {
						return that.$applyAsync(callback);
					}
					return _.delay(later, count);
				}

				setTimeout(later, wait);
			};

			__custom__.$timeout = function(func, wait, invokeApply) {
				if ($timeout === null) {
					$timeout = $injector.get('$timeout');
				}
				if (arguments.length === 0) {
					return $timeout();
				}
				return $timeout.apply(null, arguments);
			};

			__custom__.$onAdjust = function (events, handler, wait) {
				var names = events;
				if (_.isFunction(names)) {
					wait = handler;
					handler = names;
					names = 'adjust:size';
				} else {
					names = names.replace(/(\w+)/g, 'adjust:$1');
				}

				var func = wait ? _.debounce(handler, wait) : handler;

				$(document).on(names, func);
				this.$on('$destroy', function () {
					$(document).off(names, func);
				});
			};
			
			__custom__.$$onSuspend = function () {
				if (this.$$watchersSuspended === undefined && this.$$watchers) { 
					this.$$watchersSuspended = this.$$watchers;
					this.$$watchersCount = 0;
					this.$$watchers = this.$$watchersSuspended.filter(function (w) {
						return w.fn.uiAttachWatch;
					});
				}
			};

			__custom__.$$onResume = function () {
				if (this.$$watchersSuspended) {
					this.$$watchers = this.$$watchersSuspended;
					this.$$watchersCount = this.$$watchersSuspended.length;
					this.$$watchersSuspended = undefined;
					this.$timeout(function () { this.$broadcast('dom:attach'); }.bind(this), 100);
				}
			};

			__custom__.$new = function $new() {
				var inst = __super__.$new.apply(this, arguments);

				inst.$$watchChecker = this.$$watchChecker;
				inst.$$watchInitialized = false;
				inst.$$childCanWatch = true;
				inst.$$shouldWatch = false;
				
				var onSuspend = inst.$$onSuspend.bind(inst);
				var onResume = inst.$$onResume.bind(inst);

//				inst.$on('dom:detach', onSuspend);
//				inst.$on('dom:attach', onResume);

				return inst;
			};
			
			// make sure to patch $rootScope.$digest with
			// if ((!current.$$canWatch || current.$$canWatch(current)) && (watchers = current.$$watchers)) {
			//   ...
			// }

			__custom__.$$canWatch = function () {
				if (!this.$$watchInitialized || !this.$$watchChecker) {
					this.$$watchInitialized = true;
					return true;
				}
				if (this.$$shouldWatch === true) {
					return true;
				}
				var parent = this.$parent || {};
				if (parent.$$childCanWatch !== undefined && !parent.$$childCanWatch) {
					return false;
				}
				this.$$childCanWatch = this.$$watchChecker(this);
				return this.$$childCanWatch;
			};

			__custom__.$watchChecker = function (checker) {

				var self = this,
					previous = this.$$watchChecker;

				if (this.$$watchChecker === null) {
					this.$$watchChecker = checker;
				} else {
					this.$$watchChecker = function() {
						return previous(self) && checker(self);
					};
				}
			};

			angular.extend(__orig__, __custom__);
			angular.extend($rootScope, __custom__);

			$rootScope.$$watchChecker = null;
			
			return $rootScope;
		}]);
	}]);
	
})();
