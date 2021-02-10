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
/**
 * @license HTTP Auth Interceptor Module for AngularJS
 * (c) 2012 Witold Szczerba
 * License: MIT
 */
(function () {

"use strict";

angular.module('axelor.auth', []).provider('authService', function() {
  /**
   * Holds all the requests which failed due to 401 response,
   * so they can be re-requested in future, once login is completed.
   */
  var buffer = [];

  /**
   * Required by HTTP interceptor.
   * Function is attached to provider to be invisible for regular users of this service.
   */
  this.pushToBuffer = function(config, deferred) {
    buffer.push({
      config: config,
      deferred: deferred
    });
  };

  this.$get = ['$rootScope','$injector', function($rootScope, $injector) {
    var $http; //initialized later because of circular dependency problem
    function retry(config, deferred) {
      setTimeout(axelor.blockUI);
      $http = $http || $injector.get('$http');
      $http(config).then(function(response) {
        deferred.resolve(response);
      });
    }
    function retryAll() {
      for (var i = 0; i < buffer.length; ++i) {
        retry(buffer[i].config, buffer[i].deferred);
      }
      buffer = [];
    }
    return {
      loginConfirmed: function() {
        $rootScope.$broadcast('event:auth-loginConfirmed');
        retryAll();
      }
    };
  }];
})

/**
 * $http interceptor.
 * On 401 response - it stores the request and broadcasts 'event:angular-auth-loginRequired'.
 */
.config(['$httpProvider', 'authServiceProvider', function($httpProvider, authServiceProvider) {

  $httpProvider.interceptors.push(['$rootScope', '$q', function($rootScope, $q) {
    return {
      responseError: function error(response) {
        if (response.status === 401 && axelor.config['auth.central.client']) {
            // redirect to central login page
            window.location.href = './?client_name=' + axelor.config['auth.central.client']
              + "&hash_location=" + encodeURIComponent(window.location.hash);
            return $q.reject(response);
        }
        if (response.status === 401 || response.status === 502 || (response.status === 0 && !response.data)) {
          var deferred = $q.defer();
          authServiceProvider.pushToBuffer(response.config, deferred);
          if (!response.config.silent) {
            $rootScope.$broadcast('event:auth-loginRequired', response.status);
          }
          return deferred.promise;
        }
        // redirect to the CAS login page
        if (response.status === 302 || response.status === 307) {
          window.location.reload();
        }
        // otherwise
        return $q.reject(response);
      }
    };
  }]);
}]);

})();
