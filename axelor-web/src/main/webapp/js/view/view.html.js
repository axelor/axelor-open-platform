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

var ui = angular.module('axelor.ui');

ui.HtmlViewCtrl = HtmlViewCtrl;
ui.HtmlViewCtrl.$inject = ['$scope', '$element', '$sce', '$interpolate'];

function HtmlViewCtrl($scope, $element, $sce, $interpolate) {

  var views = $scope._views;
  var stamp = -1;

  $scope.view = views.html;

  $scope.getContext = function () {
    var params = $scope._viewParams || {};
    var parent = $scope.$parent;
    return _.extend({}, params.context, parent.getContext ? parent.getContext() : {});
  };

  $scope.getURL = function getURL() {
    var view = $scope.view;
    if (view) {
      var url = view.name || view.resource;
      if (stamp > 0) {
        var q = url.lastIndexOf('?');
        if (q > -1) {
          url += "&t" + stamp;
        } else {
          url += "?t" + stamp;
        }
      }
      if (url && url.indexOf('{{') > -1) {
        url = $interpolate(url)($scope.getContext());
      }
      return $sce.trustAsResourceUrl(url);
    }
    return null;
  };

  $scope.show = function() {
    $scope.updateRoute();
  };

  $scope.onRefresh = function () {
    if (stamp > -1) {
      stamp = new Date().getTime();
    } else {
      stamp = 0;
    }
  };

  $scope.getRouteOptions = function() {
    return {
      mode: "html"
    };
  };

  $scope.setRouteOptions = function(options) {
    $scope.updateRoute();
  };

  if ($scope._viewParams) {
    $scope._viewParams.$viewScope = $scope;
    $scope.show();
  }

  $scope.$applyAsync(function() {
    if ($scope.view.deferred) {
      $scope.view.deferred.resolve($scope);
    }
  });
}

var directiveFn = function(){
  return {
    controller: HtmlViewCtrl,
    replace: true,
    link: function (scope, element) {
      setTimeout(function () {
        element.parents('[ui-attach]').each(function () {
          $(this).scope().keepAttached = true;
        });
      }, 100);

      // XXX: chrome 76 issue? See RM-20400
      if (axelor.browser.chrome) {
        scope.$on('on:nav-click', function (e, tab) {
          if (tab.$viewScope !== scope) return;
          var iframe = element.find('iframe')[0];
          var embed = iframe.contentDocument ? iframe.contentDocument.body.firstChild : null;
          if (embed && embed.id === 'plugin') {
            embed.height = '101%';
            setTimeout(function () {
              embed.height = '100%';
            });
          }
        });
      }
    },
    template:
    '<div class="iframe-container">'+
      '<iframe ng-src="{{getURL()}}" frameborder="0" scrolling="auto"></iframe>'+
    '</div>'
  };
};

ui.directive('uiViewHtml', directiveFn);
ui.directive('uiPortletHtml', directiveFn);

})();
