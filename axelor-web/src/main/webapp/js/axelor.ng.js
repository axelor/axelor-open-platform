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

			__custom__.ajaxStop = function ajaxStop(callback, context) {

				var $http = $injector.get('$http'),
					count = _.size($http.pendingRequests || []),
					wait = _.last(arguments);

				if (!wait || !_.isNumber(wait)) {
					wait = 10;
				}
				if (count > 0) {
					return _.delay(ajaxStop, wait, callback, context);
				}
				if (callback) {
					_.delay(callback, wait, context);
				}
			};

			__custom__.applyLater = function applyLater(func, wait) {
				var that = this;
				return setTimeout(function(){
			    	return that.$apply(func ||angular.noop);
			    }, wait);
			};
			
			__custom__.$new = function $new() {
				var inst = __super__.$new.apply(this, arguments);
				inst.$$watchChecker = this.$$watchChecker;
				inst.$$watchInitialized = false;
				inst.$$childCanWatch = true;
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

			angular.extend(__proto__, __custom__);
			angular.extend($rootScope, __custom__);

			$rootScope.$$watchChecker = null;
			
			return $rootScope;
		}]);
	}]);
	
}());