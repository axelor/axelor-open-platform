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

var ui = angular.module("axelor.ui");

function UserCtrl($scope, $element, $location, DataSource, ViewService) {

  $scope._viewParams = {
    model: 'com.axelor.auth.db.User',
    views: [{name: 'user-preferences-form', type: 'form'}],
    recordId: axelor.config['user.id']
  };

  ui.ViewCtrl($scope, DataSource, ViewService);
  ui.FormViewCtrl($scope, $element);

  $scope.onClose = function() {
    $scope.confirmDirty(doClose);
  };

  var __version = null;

  $scope.$watch('record.version', function recordVersionWatch(value) {
    if (value === null || value === undefined) return;
    if (__version !== null) return;
    __version = value;
  });

  function doClose() {
    if (!$scope.isDirty()) {
      var rec = $scope.record || {};
      axelor.config["user.action"] = rec.homeAction;
    }

    window.history.back();

    if (__version === ($scope.record || {}).version) {
      return;
    }

    setTimeout(function() {
      window.location.reload();
    }, 100);
  }

  $scope.isMidForm = function (elem) {
    return $element.find('form.mid-form').size();
  };

  $scope.setEditable();
  $scope.show();

  $scope.ajaxStop(function () {
    $scope.$applyAsync();
  });
}

function AboutCtrl($scope) {
  $scope.appName = axelor.config["application.name"];
  $scope.appDescription = axelor.config["application.description"];
  $scope.appVersion = axelor.config["application.version"];
  $scope.appVersionShort = $scope.appVersion.substring(0, $scope.appVersion.lastIndexOf('.'));
  $scope.appCopyright = axelor.config["application.copyright"];
  $scope.appSdk = axelor.config["application.sdk"];
  $scope.appSdkShort = $scope.appSdk.substring(0, $scope.appSdk.lastIndexOf('.'));
  $scope.appHome = axelor.config["application.home"];
  $scope.appHelp = axelor.config["application.help"];
  $scope.appYear = moment().year();
  $scope.technical = axelor.config['user.technical'];
}

function SystemCtrl($scope, $element, $location, $http) {
  if (!axelor.config['user.technical']) {
     window.location.hash = '/about';
     return;
  }

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

ui.controller("UserCtrl", ['$scope', '$element', '$location', 'DataSource', 'ViewService', UserCtrl]);
ui.controller("SystemCtrl", ['$scope', '$element', '$location', '$http', SystemCtrl]);
ui.controller("AboutCtrl", ['$scope', AboutCtrl]);

})();
