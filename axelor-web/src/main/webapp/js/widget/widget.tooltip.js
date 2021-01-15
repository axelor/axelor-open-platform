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

'use strict';

var ui = angular.module('axelor.ui');

ui.directive('uiTooltip', ['ViewService', function (ViewService) {
  return {
    scope: {
      selector: '@',
      placement: '@',
      getter: '&'
    },
    link: function (scope, element, attrs) {
      var ttElem = null;
      var ttTimer = null;
      var ttContent = null;

      function fetch(ds, record, tooltip) {

        if (tooltip.call) {
          var context = _.extend({ _model: ds._model }, record, tooltip.context);
          var promise = ViewService.action(tooltip.call, ds._model, context, null, {
            silent: true
          });
          return promise.then(function (response) {
            var res = response.data;
            var data = _.isArray(res.data) ? _.first(res.data) : res.data;
            return data;
          });
        }

        var id = record.id;
        var depends = (tooltip.depends || '').trim().split(/\s*,\s*/);

        if (depends.length === 0 || _.every(depends, function (x) { return _.has(record, x); })) {
          var deferred = ViewService.defer();
          deferred.resolve(record);
          return deferred.promise;
        }

        return ds.read(id, {
          fields: depends
        }).then(function (response) {
          var data = response.data;
          var res = _.first(data.data);
          return _.extend({}, record, res);
        });
      }

      function showTooltip(e, timer) {
        var meta = scope.getter({ $event: e }) || {};

        if (!meta.tooltip) return;
        if (!meta.tooltip.template) return;
        if (!meta.record) return;
        if (!meta.record.id || meta.record.id < 0) return;

        var ds = meta.dataSource._new(meta.dataSource._model);
        var tooltip = meta.tooltip;
        var record = meta.record;
        var promise = fetch(ds, record, tooltip);

        promise.then(function (rec) {
          // if canceled
          if (ttTimer !== timer) {
            return;
          }

          var template = tooltip.template;
          var evalScope = axelor.$evalScope(scope);

          evalScope.record = rec;

          if (template.indexOf('<') !== 0) {
            template = "<span>" + template + "</span>";
          }

          ttContent = ViewService.compile(template)(evalScope).hide().appendTo(element);
          ttTimer = setTimeout(function () {
            ttElem = $("<div class='field-tooltip'></div>").html(ttContent.html());
            ttElem.css({
              position: 'absolute',
            });
            ttElem.appendTo(document.body).position({
              my: "left bottom",
              at: "left+" + e.offsetX + " top",
              of: e.currentTarget
            }).addClass('open');
          });
        });
      }

      function hideTooltip() {
        if (ttContent) ttContent.remove();
        if (ttElem) ttElem.remove();
        if (ttTimer) clearTimeout(ttTimer);
        ttContent = null;
        ttElem = null;
        ttTimer = null;
      }

      function onMouseEnter(e) {
        clearTimeout(ttTimer);
        ttTimer = setTimeout(function () {
          showTooltip(e, ttTimer);
        }, 1000);
      }

      setTimeout(function () {
        var elem = element.parent();

        elem.on('mouseenter', scope.selector, onMouseEnter);
        elem.on('mouseleave', scope.selector, hideTooltip);

        scope.$on("$destroy", hideTooltip);
      });
    }
  };
}]);

})();
