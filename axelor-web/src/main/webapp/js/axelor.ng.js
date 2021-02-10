/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
        this.$timeout(function () {
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

      __custom__.$new = function $new() {
        var inst = __super__.$new.apply(this, arguments);

        inst.$$watchChecker = this.$$watchChecker;
        inst.$$watchInitialized = false;
        inst.$$childCanWatch = true;
        inst.$$shouldWatch = false;
        inst.$$popupStack = this.$$popupStack || (this.$$popupStack = []);
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

      var __super__$$apply = _.debounce(__super__.$apply, 100);

      __custom__.$apply = function $apply() {
        return arguments.length === 0
          ? __super__$$apply.apply(this)
          : __super__.$apply.apply(this, arguments);
      };

      __custom__.$applyNow = function $applyNow() {
        return __super__.$apply.apply(this, arguments);
      };

      angular.extend(__orig__, __custom__);
      angular.extend($rootScope, __custom__);

      $rootScope.$$watchChecker = null;

      return $rootScope;
    }]);
  }]);

})();
