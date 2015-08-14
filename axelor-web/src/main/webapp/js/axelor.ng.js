/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
(function () {

	var module = angular.module('axelor.ng', []);

	module.config(['$provide', function($provide) {

		$provide.decorator('$rootScope', ['$delegate', '$exceptionHandler', '$injector', function ($rootScope, $exceptionHandler, $injector) {
			
			var __proto__ = Object.getPrototypeOf($rootScope),
				__super__ = {},
				__custom__ = {};

			for (var name in __proto__) {
				if (angular.isFunction(__proto__[name])) {
					__super__[name] = __proto__[name];
				}
			}

			var $q = null,
				$http = null,
				$timeout = null;
			
			__custom__.ajaxStop = function ajaxStop(callback, context) {
				var count, wait;

				if ($http === null) {
					$http = $injector.get('$http');
				}
				
				count = _.size($http.pendingRequests || []);
				wait = _.last(arguments) || 10;

				if (_.isNumber(context)) {
					context = undefined;
				}
				
				if (count > 0) {
					return _.delay(ajaxStop.bind(this), wait, callback, context);
				}
				if (_.isFunction(callback)) {
					return this.$timeout(callback.bind(context), wait);
				}
			};

			__custom__.$actionPromises = [];
			__custom__.waitForActions = function waitForActions(callback, wait) {
				if ($q === null) {
					$q = $injector.get('$q');
				}
				var that = this;
				var waitFor = wait || 10;
				this.$timeout(function () {
					// wait for any pending ajax requests
					that.ajaxStop(function () {
						var all = that.$actionPromises;
						// wait for actions
						$q.all(all).then(function () {
							// if new actions are executed, wait for them
							if (that.$actionPromises.length) {
								return _.delay(waitForActions.bind(that), 10, callback);
							}
							if (callback) {
								callback();
							}
						}, callback);
					});
				}, waitFor);
			};

			__custom__.applyLater = function applyLater(func, wait) {
				return this.$timeout(func ||angular.noop, wait);
			};

			__custom__.$callWhen = function (predicate, callback, wait) {
				var count = wait || 100;

				function later() {
					if (count-- === 0 || (_.isFunction(predicate) && predicate())) {
						return callback();
					}
					return _.delay(later, count);
				}

				this.$timeout(later, wait);
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
			
			__custom__.$new = function $new() {
				var inst = __super__.$new.apply(this, arguments);
				inst.$$watchChecker = this.$$watchChecker;
				inst.$$watchInitialized = false;
				inst.$$childCanWatch = true;
				inst.$$shouldWatch = false;
				return inst;
			};
			
			// make sure to patch $rootScope.$digest with
			// if ((!current.$$canWatch || current.$$canWatch(current)) && (watchers = current.$$watchers)) {
			//   ...
			// }

			__custom__.$$canWatch = function () {
				if (!this.$$watchInitialized || !this.$$watchChecker) {
					return this.$$watchInitialized = true;
				}
				if (this.$$shouldWatch === true) {
					return true;
				}
				var parent = this.$parent || {};
				if (parent.$$childCanWatch !== undefined && !parent.$$childCanWatch) {
					return false;
				}
				return this.$$childCanWatch = this.$$watchChecker(this);
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
			
			function debouncedApply(wait) {
				return _.debounce(__super__.$apply, wait);
			}

			var $apply1 = _.debounce(__super__.$apply);
			var $apply2 = _.debounce(__super__.$apply, 100);

			var $debouncedApply = _.debounce(function $debouncedApply() {
				if ($http === null) {
					$http = $injector.get('$http');
				}
				if ($http.pendingRequests.length) {
					return $apply2.call(this);
				}
				return $apply1.call(this);
			});

			__custom__.$apply = function $apply() {
				if (arguments.length === 0) {
					return $debouncedApply.call(this);
				}
				return __super__.$apply.apply(this, arguments);
			};

			__custom__.$applyNow = function $applyNow() {
				return __super__.$apply.apply(this, arguments);
			};

			angular.extend(__proto__, __custom__);
			angular.extend($rootScope, __custom__);

			$rootScope.$$watchChecker = null;
			
			return $rootScope;
		}]);
	}]);
	
}());