/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
(function () {

  "use strict";

  var ui = angular.module('axelor.ui');

  ui.directive('navQuickMenuBar', function () {

    return {
      replace: true,
      controller: ['$scope', '$element', 'MenuService', 'ActionService', 'NavService',
          function ($scope, $element, MenuService, ActionService, NavService) {
        $scope.menus = [];

        this.update = function () {
          MenuService.quick().then(function (response) {
            var data = (response.data || {}).data;
            $scope.menus = _.chain(data)
              .filter(function (menu) { return !_.isEmpty((menu || {}).items); })
              .sortBy(function (menu) { return menu.order; })
              .value();
          });
        }

        this.ActionService = ActionService;
        this.NavService = NavService;
      }],
      scope: true,

      link: function (scope, element, attrs, ctrl) {
        ctrl.update();
      },

      template:
        "<ul class='nav nav-menu-bar quick-menu-bar'>" +
          "<li class='nav-menu dropdown'ng-repeat='menu in menus'>" +
            "<a href='javascript:' class='dropdown-toggle' data-toggle='dropdown'>" +
              "<span ng-bind-html='menu.title'></span> " +
              "<b class='caret'></b>" +
            "</a>" +
            "<ul nav-quick-menu='menu'></ul>" +
          "</li>" +
        "</ul>"
    };
  });

  ui.directive('navQuickMenu', function () {

    return {
      replace: true,
      require: '^navQuickMenuBar',
      scope: {
        menu: '=navQuickMenu'
      },

      template:
        "<ul class='dropdown-menu quick-menu'>" +
          "<li ng-repeat='item in menu.items' nav-quick-menu-item='item'>" +
        "</ul>"
    };
  });

  ui.directive('navQuickMenuItem', function () {

    return {
      replace: true,
      require: '^navQuickMenuBar',
      scope: {
        item: '=navQuickMenuItem'
      },

      link: function (scope, element, attrs, ctrl) {
        scope.getContext = function () {
          return _.extend({}, scope.item.context, {
            _model: scope.item.model || 'com.axelor.meta.db.MetaAction' });
        }

        var action = scope.item.action;

        if (action) {
          var handler = ctrl.ActionService.handler(scope, element, { action: action });

          element.on("click auxclick", function (e) {
            var foundTab = _.find(ctrl.NavService.getTabs(), function (tab) {
              return tab.action === action });

            if (foundTab) {
              return ctrl.NavService.openTab(foundTab, undefined, e);
            }

            return handler.onClick(e).then(function (response) {
              ctrl.update();
            });
          });
        }
      },

      template:
        "<li class='quick-menu-item'>" +
          "<a href='javascript:' class='ibox round'>" +
            "<input type='radio' ng-checked='{{item.selected}}'>" +
            "<div class='box' ng-if='$parent.menu.showingSelected'></div>" +
            "<span class='title' ng-bind-html='item.title'></span>" +
          "</a>" +
        "</li>"
    };
  });

})();
