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

DashboardCtrl.$inject = ['$scope', '$element'];
function DashboardCtrl($scope, $element) {

  var view = $scope._views.dashboard;
  if (view.items) {
    $scope.$timeout(function () {
      $scope.parse(view);
    });
  } else {
    $scope.loadView('dashboard', view.name).success(function(fields, schema){
      $scope.parse(schema);
    });
  }

  $scope.$applyAsync(function(){
    if (view.deferred)
      view.deferred.resolve($scope);
  });

  $scope.show = function(promise) {
    $scope.updateRoute();
  };

  $scope.onShow = function() {

  };

  $scope.getContext = function() {
    return _.extend({}, $scope._context);
  };

  $scope.getRouteOptions = function() {
    return {
      mode: 'dashboard',
      args: []
    };
  };

  $scope.setRouteOptions = function(options) {
    if (!$scope.isNested) {
      $scope.updateRoute();
    }
  };

  $scope.parse = function(schema) {
    var items = angular.copy(schema.items || []);
    var row = [];

    items.forEach(function (item, i) {
      var span = item.colSpan || 6;

      item.$index = i;
      item.spanCss = {};
      item.spanCss['dashlet-cs' + span] = true;

      row.push(item);
    });

    $scope.schema = schema;
    $scope.row = row;
  };
}

ui.directive('uiViewDashboard', ['ViewService', function(ViewService) {

  return {
    controller: DashboardCtrl,
    link: function(scope, element, attrs) {

      scope.sortableOptions = {
        handle: ".dashlet-header",
        cancel: ".dashlet-buttons,input,textarea,button,select,option",
        items: ".dashlet",
        tolerance: "pointer",
        activate: function(e, ui) {
          var height = ui.helper.height();
          ui.placeholder.height(height);
        },
        deactivate: function(event, ui) {
          axelor.$adjustSize();
        },
        stop: function (event, ui) {
          var schema = scope.schema;
          var items = _.map(scope.row, function (item) {
            return schema.items[item.$index];
          });

          if (angular.equals(schema.items, items)) {
            return;
          }

          schema.items = items;
          return ViewService.save(schema);
        }
      };

      var unwatch = scope.$watch("schema", function dashboardSchemaWatch(schema) {
        if (!schema) {
          return;
        }
        unwatch();
        if (schema.css) {
          element.addClass(schema.css);
        }
      });
    },
    replace: true,
    transclude: true,
    template:
    "<div ui-sortable='sortableOptions' ng-model='row'>" +
      "<div class='dashlet' ng-class='dashlet.spanCss' ng-repeat='dashlet in row' ui-view-dashlet></div>" +
    "</div>"
  };
}]);

DashletCtrl.$inject = ['$scope', '$element', 'MenuService', 'DataSource', 'ViewService'];
function DashletCtrl($scope, $element, MenuService, DataSource, ViewService) {

  $scope.toolbar = null;
  $scope.menubar = null;

  var self = this;
  var init = _.once(function init() {

    ui.ViewCtrl.call(self, $scope, DataSource, ViewService);

    $scope.show = function() {

    };

    $scope.onShow = function() {

    };

    $scope.$on('on:attrs-change:refresh', function(e) {
      e.preventDefault();
      if ($scope.onRefresh) {
        $scope.onRefresh();
      }
    });

    $scope.$on('on:tab-reload', function(e) {
      if ($scope.onRefresh) {
        $scope.onRefresh();
      }
    });
  });

  $scope.initDashlet = function(dashlet, options) {

    var action = dashlet.action;
    if (!action) {
      return init();
    }

    MenuService.action(action, options).success(function(result){
      if (_.isEmpty(result.data)) {
        return;
      }
      var view = result.data[0].view;

      $scope._viewParams = view;
      $scope._viewAction = action;

      init();

      $scope.title = dashlet.title || view.title;
      if ($scope.attr) {
        $scope.title = $scope.attr('title') || $scope.title;
      }
      $scope.parseDashlet(dashlet, view);
    });
  };
}

ui.directive('uiViewDashlet', ['$compile', function($compile){
  return {
    scope: true,
    controller: DashletCtrl,
    link: function(scope, element, attrs) {

      var lazy = true;
      (function () {
        var counter = 0;
        return function checkLoading() {
          if (counter < 10 && element.parent().is(":hidden")) {
            counter++;
            return setTimeout(checkLoading, 100);
          }

          lazy = !element.parent().is(".dashlet-row");

          scope.waitForActions(function () {
            var unwatch = scope.$watch(function dashletInitWatch() {
              var dashlet = scope.dashlet;
              if (!dashlet) {
                return;
              }

              if (element.parent().is(":hidden")) {
                lazy = true;
                return;
              }

              unwatch();
              unwatch = null;

              var ctx;
              if (scope.getContext) {
                ctx = scope.getContext();
              }
              scope.initDashlet(dashlet, {
                context: ctx
              });
            });
          });
        };
      })()();

      scope.parseDashlet = _.once(function(dashlet, view) {
        var body = element.find('.dashlet-body:first');
        var header = element.find('.dashlet-header:first');
        var template = $('<div ui-portlet-' + view.viewType + '></div>');

        scope.noFilter = !dashlet.canSearch;

        template = $compile(template)(scope);
        body.append(template);

        if (dashlet.height) {
          setTimeout(function() {
            body.css("height", Math.max(0, dashlet.height - header.outerHeight()));
          });
        }
        if (dashlet.css) {
          element.addClass(dashlet.css);
        }
        if (view && view.viewType) {
          element.addClass(view.viewType);
        }

        element.removeClass('hidden');

        scope.show();

        // if lazy, load data
        if (scope.onRefresh && lazy) {
          scope.onRefresh();
        }
      });

      scope.showPager = false;
      scope.showRefresh = true;
      scope.showToggle = true;

      scope.collapsed = false;
      scope.collapsedIcon = "fa-chevron-up";
      scope.onDashletToggle = function(event) {
        var body = element.children('.dashlet-body');
        var action = scope.collapsed ? "show" : "hide";
        scope.collapsed = !scope.collapsed;
        scope.collapsedIcon = scope.collapsed ? "fa-chevron-down" : "fa-chevron-up";
        element.removeClass("collapsed");
        body[action]("blind", 200, function () {
          element.toggleClass("collapsed", !!scope.collapsed);
          if (body.css('display') !== 'none' && action === 'hide') {
            body.hide();
          }
          axelor.$adjustSize();
        });
      };

      scope.doNext = function() {
        if (this.canNext()) this.onNext();
      };

      scope.doPrev = function() {
        if (this.canPrev()) this.onPrev();
      };
    },
    replace: true,
    template:
      "<div class='dashlet hidden'>" +
        "<div class='dashlet-header'>" +
          "<ul class='dashlet-buttons' ng-if='showRefresh || canExport() || hasAction()'>" +
            "<li class='dropdown'>" +
              "<a href='' class='dropdown-toggle' data-toggle='dropdown'><i class='fa fa-gear'></i></a>" +
              "<ul class='dropdown-menu pull-right'>" +
                "<li ng-if='showRefresh'>" +
                  "<a href='' ng-click='onRefresh()' x-translate>Refresh</a>" +
                "</li>" +
                "<li ng-if='isLegendVisible !== undefined'>" +
                  "<a ng-if='isLegendVisible' href='' ng-click='toggleLegend()' x-translate>Hide legend</a>" +
                  "<a ng-if='!isLegendVisible' href='' ng-click='toggleLegend()' x-translate>Show legend</a>" +
                "</li>" +
                "<li ng-if='canExport()'>" +
                  "<a href='' ng-click='onExport()' x-translate>Export</a>" +
                "</li>" +
                "<li ng-if='hasAction()' class='divider'></li>" +
                "<li ng-if='hasAction()'>" +
                  "<a href='' ng-click='onAction()'>{{ (actionTitle || _t('Action')) }}</a>" +
                "</li>" +
              "</ul>" +
            "</li>" +
            "<li ng-if='showToggle'><a href='' ng-click='onDashletToggle()'><i class='fa' ng-class='collapsedIcon'></i></a></li>" +
          "</ul>" +
          "<div class='dashlet-pager' ng-if='showPager'>" +
            "<span class='dashlet-pager-text'>{{pagerText()}}</span>" +
            "<a href='' ng-click='doPrev()' ng-class='{disabled: !canPrev()}'><i class='fa fa-step-backward'></i></a>" +
            "<a href='' ng-click='doNext()' ng-class='{disabled: !canNext()}'><i class='fa fa-step-forward'></i></a>" +
          "</div>" +
          "<div class='dashlet-search' ng-if='showSearch'>" +
            "<div ui-filter-box x-handler='this'></div>" +
          "</div>" +
          "<div ui-nested-grid-actions ng-if='field.showBars'></div>" +
          "<div class='dashlet-title'><span ui-help-popover>{{title}}</span></div>" +
        "</div>" +
        "<div class='dashlet-body'></div>" +
      "</div>"
  };
}]);

})();
