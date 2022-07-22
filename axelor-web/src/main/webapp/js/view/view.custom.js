/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

      scope._requiresLazyLoading = true;
      var evalScope = axelor.$evalScope(scope);

      function render(template) {
        var elem = $('<span>' + template.trim() + '</span>');
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
    link: function (scope, element, attrs) {

      var cols = [];
      var sums = (scope.sums||'').split(',');
      var fields = {};
      var schema = scope.$parent.schema;

      function makeColumns(names) {
        cols = [];
        fields = {};
        _.each(names, function (name) {
          var field = _.findWhere(schema.items, { name: name }) || {};
          var col = _.extend({}, field, field.widgetAttrs, {
            name: name,
            title: field.title || field.autoTitle || _.humanize(name),
            type: field.selection ? 'selection' : field.serverType || field.type
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
            makeColumns(names);
          }
        });
      }

      scope.sums = sums;

      scope.format = function(value, name) {
        if (value === null || value === undefined) {
          return "";
        }

        var field = fields[name] || {};

        if (field.translatable) {
          return _t('value:' + value);
        }

        if (field.selection) {
          value = '' + value;
        }

        var formatter = ui.formatters[field.type];

        if (formatter) {
          return formatter(field, value);
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

      var activeSort = {
        key: null,
        descending: false
      };

      scope.onSort = function (col) {
        if (activeSort.key === col.name) {
          activeSort.descending = !activeSort.descending;
        } else {
          activeSort.key = col.name;
          activeSort.descending = false;
        }
        if (['integer', 'long', 'decimal'].indexOf(col.type) < 0) {
          scope.data = axelor.sortBy(scope.data, function (item) {
            return scope.format(item[activeSort.key], activeSort.key) || '';
          }, activeSort.descending);
        } else {
          scope.data = _.sortBy(scope.data, activeSort.descending ? function (item) {
            return -item[activeSort.key] || 0;
          } : function (item) {
            return +item[activeSort.key] || 0;
          });
        }
      }

      scope.sortIndicator = function (col) {
        if (activeSort.key !== col.name) return null;
        return 'slick-sort-indicator-' + (activeSort.descending ? 'desc' : 'asc');
      }

      var backgroundColor = $('body').css('background-color');
      var style = backgroundColor ? { backgroundColor: backgroundColor } : null;
      scope.headerStyle = scope.footerStyle = style;

      setTimeout(function () {
        element.parents('.dashlet:first').addClass("report-table");
      });
    },
    replace: true,
    template:
      "<table class='table table-striped'>" +
        "<thead ng-style='headerStyle'>" +
          "<tr>" +
            "<th ng-repeat='col in cols' ng-class='col.type'>{{col.title | t}}</th>" +
          "</tr>" +
        "</thead>" +
        "<tbody>" +
          "<tr ng-repeat='row in data'>" +
            "<td ng-repeat='col in cols' ng-class='col.type'>{{format(row[col.name], col.name)}}</td>" +
          "</tr>" +
        "</tbody>" +
        "<tfoot ng-if='sums.length' ng-style='footerStyle'>" +
          "<tr>" +
            "<td ng-repeat='col in cols' ng-class='col.type'>{{sum(col.name)}}</td>" +
          "</tr>" +
        "</tfoot>" +
      "</table>"
  };
});

})();
