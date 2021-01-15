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

CustomViewCtrl.$inject = ['$scope', '$http', 'DataSource', 'ViewService'];
function CustomViewCtrl($scope, $http, DataSource, ViewService) {

  ui.ViewCtrl.call(this, $scope, DataSource, ViewService);

  var view = $scope._views.custom || {};
  var viewPromise = null;

  $scope.show = function() {
    if (!viewPromise) {
      viewPromise = $scope.loadView('custom', view.name);
      viewPromise.then(function(meta) {
        var schema = meta.view;
        $scope.schema = schema;
        $scope.schema.loaded = true;
      });
    }
    $scope.onShow(viewPromise);
  };

  $scope.onShow = function(viewPromise) {
    // it will be refreshed by dashlet
    if ($scope.dashlet) {
      return;
    }
    viewPromise.then(function () {
      $scope.onRefresh();
    });
  };

  $scope.getRouteOptions = function() {
    return {
      mode: 'custom'
    };
  };

  $scope.setRouteOptions = function(options) {
    var opts = options || {};
    if (opts.mode === "custom") {
      return $scope.updateRoute();
    }
    var params = $scope._viewParams;
    if (params.viewType !== "custom") {
      return $scope.show();
    }
  };

  $scope.getContext = function () {
    var context = $scope._context || {};
    if ($scope.$parent.getContext) {
      context = _.extend({}, $scope.$parent.getContext(), context);
    }
    return context;
  };

  $scope.onRefresh = function() {
    var context = _.extend({}, $scope.getContext(), { _domainAction: $scope._viewAction });
    var params = {
      data: context
    };
    return $http.post('ws/meta/custom/' + view.name, params).then(function(response) {
      var res = response.data;
      $scope.data = (res.data||{}).dataset;
    });
  };
}

var customDirective = ["$compile", function ($compile) {
  return {
    controller: CustomViewCtrl,
    link: function (scope, element, attrs, ctrl) {

      var evalScope = axelor.$evalScope(scope);

      function render(template) {
        var elem = $('<span>' + axelor.sanitize(template.trim()) + '</span>');
        if (elem.children().length === 1) {
          elem = elem.children().first();
        }
        if (scope.schema && scope.schema.css) {
          element.parents(".dashlet:first").addClass(scope.schema.css);
        }

        elem = $compile(elem)(evalScope);
        element.append(elem);
      }

      var unwatch = scope.$watch('schema.template', function customTemplateWatch(template) {
        if (template) {
          unwatch();
          render(template);
          scope.$on("on:new", function() { scope.onRefresh(); });
          scope.$on("on:edit", function() { scope.onRefresh(); });
        }
      });

      scope.showToggle = false;

      scope.$watch('data', function customDataWatch(data) {
        evalScope.data = data;
        evalScope.first = _.first(data);

        evalScope.$moment = function(d) { return moment(d); };
        evalScope.$number = function(d) { return +d; };
        evalScope.$image = function (fieldName, imageName) { return ui.formatters.$image(this, fieldName, imageName); };
        evalScope.$fmt = function (fieldName, fieldValue) {
          var args = [_.extend(this, {record: {first: this.first}}), fieldName];
          if (arguments.length > 1) {
            args.push(fieldValue);
          }
          return ui.formatters.$fmt.apply(null, args);
        };
      });
    }
  };
}];

ui.directive('uiCustomView', customDirective);
ui.directive('uiPortletCustom', customDirective);

// helper directives
ui.directive('reportBox', function() {
  return {
    scope: {
      value: '=',
      label: '@',
      percent: '=',
      up: '=',
      tag: '=',
      tagCss: '='
    },
    link: function (scope, element, attrs) {
      setTimeout(function () {
        element.parents('.dashlet:first')
          .addClass("report-box");
      });
    },
    replace: true,
    template:
      "<div class='report-box'>" +
        "<h1>{{value}}</h1>" +
        "<small>{{label}}</small>" +
        "<div class='font-bold text-info pull-right' ng-show='percent'>" +
          "<span>{{percent}}</span> <i class='fa fa-level-up'></i>" +
        "</div>" +
        "<div class='report-tags' ng-if='tag'><span class='label' ng-class='tagCss'>{{tag}}</span></div>" +
      "</div>"
  };
});

ui.directive('reportTable',  function() {
  return {
    scope: {
      data: '=',
      columns: '@',
      sums: '@'
    },
    link: function (scope, element, attrs) {

      var cols = [];
      var sums = (scope.sums||'').split(',');
      var fields = {};
      var schema = scope.$parent.$parent.schema;

      function makeColumns(names) {
        cols = [];
        fields = {};
        _.each(names, function (name) {
          var field = _.findWhere(schema.items, { name: name }) || {};
          var col = _.extend({}, field, field.widgetAttrs, {
            name: name,
            title: _.humanize(name)
          });
          fields[name] = col;
          cols.push(col);
        });
        scope.cols = cols;
      }

      if (scope.columns) {
        makeColumns((scope.columns||'').split(','));
      } else {
        var unwatch = scope.$watch('data', function reportDataWatch(data) {
          if (data) {
            unwatch();
            var first = _.first(data) || {};
            var names = _.keys(first).filter(function (name) { return name !== '$$hashKey'; });
            makeColumns(names.sort());
          }
        });
      }

      scope.sums = sums;

      scope.format = function(value, name) {
        if (value === null || value === undefined) {
          return "";
        }
        var field = fields[name];
        if (field && field.scale) {
          var val = +(value);
          if (_.isNumber(val)) {
            return val.toFixed(field.scale);
          }
        }
        return value;
      };

      scope.sum = function (name) {
        if (sums.indexOf(name) === -1) {
          return "";
        }
        var res = 0.0;
        _.each(scope.data, function (row) {
          var val = +(row[name]) || 0;
          res += val;
        });
        return scope.format(res, name);
      };

      setTimeout(function () {
        element.parents('.dashlet:first')
          .addClass("report-table");
      });
    },
    replace: true,
    template:
      "<table class='table table-striped'>" +
        "<thead>" +
          "<tr>" +
            "<th ng-repeat='col in cols'>{{col.title}}</th>" +
          "</tr>" +
        "</thead>" +
        "<tbody>" +
          "<tr ng-repeat='row in data'>" +
            "<td ng-repeat='col in cols'>{{format(row[col.name], col.name)}}</td>" +
          "</tr>" +
        "</tbody>" +
        "<tfoot ng-if='sums.length'>" +
          "<tr>" +
            "<td ng-repeat='col in cols'>{{sum(col.name)}}</td>" +
          "</tr>" +
        "</tfoot>" +
      "</table>"
  };
});

})();
