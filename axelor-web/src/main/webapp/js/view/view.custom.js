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

  $scope.canExport = function() {
    return false;
  };

  $scope.onExport = function() {

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
      icon: '@',
      label: '@',
      value: '=',
      percent: '=',
      up: '=',
      tag: '=',
      tagCss: '='
    },
    link: function (scope, element, attrs) {

      scope.format = function (value) {
        var formatter = isNaN(+value) ? ui.formatters.string : ui.formatters.decimal;
        return formatter({}, value);
      };

      scope.percentStyle = function () {
        var type;
        if (scope.up == null) {
          type = 'info';
        } else {
          type = scope.up ? 'success' : 'error';
        }
        return 'text-' + type;
      };

      scope.percentLevelStyle = function () {
        if (scope.up == null) return null;
        return 'fa-level-' + (scope.up ? 'up' : 'down');
      };

      setTimeout(function () {
        element.parents('.dashlet:first').addClass("report-box");
        element.removeClass('hidden');
      });
    },
    replace: true,
    template:
      "<div class='report-data hidden'>" +
        "<i class='report-icon fa {{icon}}' ng-if='icon'/>" +
        "<div>" +
          "<h1>{{format(value)}}</h1>" +
          "<small>{{_t(label)}}</small>" +
          "<div class='font-bold pull-right' ng-class='percentStyle()' ng-show='percent'>" +
            "<span>{{percent | percent}}</span> <i class='fa' ng-class='percentLevelStyle()'/>" +
          "</div>" +
        "</div>" +
        "<div class='report-tags' ng-if='tag'><span class='label' ng-class='tagCss'>{{tag}}</span></div>" +
      "</div>"
  };
});

ui.directive('reportTable',  function() {
  return {
    link: function (scope, element, attrs) {

      var cols = [];
      var sums = attrs.sums ? attrs.sums.split(/\s*,\s*/) : [];
      var fields = {};
      var schema = scope.$parent.schema;

      function makeColumns(names) {
        cols = [];
        fields = {};
        _.each(names, function (name) {
          var field = _.findWhere(schema.items, { name: name }) || {};
          var col = _.extend({}, field, field.widgetAttrs, {
            name: name,
            title: field.title || field.autoTitle || _t(_.humanize(name)),
            type: field.serverType || 'string'
          });
          fields[name] = col;
          cols.push(col);
        });
        scope.cols = cols;
      }

      if (attrs.columns) {
        makeColumns((attrs.columns||'').split(/\s*,\s*/));
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

      scope.$parent.canExport = function() {
        return true;
      };

      scope.$parent.onExport = function() {
        var dataset = scope.data || [];

        var header = _.pluck(scope.cols, 'title');
        var content = "data:text/csv;charset=utf-8," + header.join(';') + '\n';

        dataset.forEach(function (item) {
          var row = _.pluck(scope.cols, 'name').map(function (key) {
            var val = item[key];
            if (val === undefined || val === null) {
              val = '';
            }
            return '"' + ('' + val).replace(/"/g, '""') + '"';
          });
          content += row.join(';') + '\n';
        });

        var name = (scope.$parent.title || 'export').toLowerCase();
        ui.download(encodeURI(content), _.underscored(name) + '.csv');
      };

      scope.sums = sums;

      scope.format = function(row, name) {
        var value = row[name];
        if (value == null) {
          return "";
        }

        var field = fields[name] || {};
        var type = scope.getType(field);
        var context = _.extend({}, row);

        if (field.translatable && type === 'string') {
          var trKey = 'value:' + value;
          var trValue = _t(trKey);
          if (trValue !== trKey) {
            context['$t:' + field.name] = trValue;
          }
        }

        var formatter = ui.gridFormatters[type] || ui.formatters[type];
        if (formatter) {
          return formatter(field, value, context);
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
        return scope.format({
          [name]: res
        }, name);
      };

      scope.getType = function (col) {
        return col.selection ? 'selection' : col.type;
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
            return scope.format(item, activeSort.key) || '';
          }, activeSort.descending);
        } else {
          scope.data = _.sortBy(scope.data, activeSort.descending ? function (item) {
            return -item[activeSort.key] || 0;
          } : function (item) {
            return +item[activeSort.key] || 0;
          });
        }
      };

      scope.sortIndicator = function (col) {
        if (activeSort.key !== col.name) return null;
        return 'slick-sort-indicator-' + (activeSort.descending ? 'desc' : 'asc');
      };

      var backgroundColor = $('body').css('background-color');
      var style = backgroundColor ? { backgroundColor: backgroundColor } : null;
      scope.headerStyle = scope.footerStyle = style;

      setTimeout(function () {
        element.parents('.dashlet:first').addClass("report-table");
      });
    },
    replace: true,
    template:
      "<table class='table table-striped readonly'>" +
        "<thead ng-style='headerStyle'>" +
          "<tr>" +
            "<th ng-repeat='col in cols' ng-class='getType(col)' ng-click='onSort(col)'>" +
              "<span>{{col.title}}</span>" +
              "<span class='slick-sort-indicator' ng-class='sortIndicator(col)'/>" +
            "</th>" +
          "</tr>" +
        "</thead>" +
        "<tbody>" +
          "<tr ng-repeat='row in data'>" +
            "<td ng-repeat='col in cols' ng-class='getType(col)' ng-bind-html='format(row, col.name)'/>" +
          "</tr>" +
        "</tbody>" +
        "<tfoot ng-if='sums.length > 0' ng-style='footerStyle'>" +
          "<tr>" +
            "<td ng-repeat='col in cols' ng-class='getType(col)'>{{sum(col.name)}}</td>" +
          "</tr>" +
        "</tfoot>" +
      "</table>"
  };
});

})();
