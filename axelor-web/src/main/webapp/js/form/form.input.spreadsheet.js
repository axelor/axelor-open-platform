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

/* global Handsontable: true */

"use strict";

var ui = angular.module('axelor.ui');

ui.formInput('Spreadsheet', {

  css: "spreadsheet-item",

  link: function (scope, element, attrs, model) {
    scope.prepareTemplate = true;

    var field = scope.field;
    var height = field.height || 580;

    var inst;

    scope.$timeout(function () {

      element.height(height).css({
        "position": "relative",
        "overflow": "hidden"
      });

      inst = new Handsontable(element[0], {
        colWidths: 60,
        rowHeaders: true,
        colHeaders: true,
        contextMenu: true,
        manualColumnResize: true,
        manualRowResize: true,
        afterChange: function (change, source) {
          if (source !== 'loadData') {
            update();
          }
        },
        afterCreateCol: update,
        afterCreateRow: update,
        afterRemoveCol: update,
        afterRemoveRow: update
      });
      model.$render();
    });

    element.resizable({
      handles: 's',
      resize: function () {
        if (inst) {
          inst.render();
        }
      }
    });

    function update() {
      if (!inst) { return; }
      var current = model.$viewValue;
      var value = compact(inst.getData());

      value = value ? JSON.stringify(value) : value;
      if (value === current) {
        return;
      }
      scope.setValue(value, true);
      scope.$applyAsync();
    }

    function compact(items) {
      var res = [];
      var i;

      for (i = 0; i < items.length; i++) {
        var item = items[i];
        if (Array.isArray(item)) {
          item = compact(item);
        }
        if (item === "" || item === null || item === undefined || item.length === 0) {
          continue;
        }
        res[i] = item;
      }

      var n = res.length;
      for (i = n - 1; i >= 0; i--) {
        if (res[i] !== null) {
          n = i+1;
          break;
        }
      }
      res = res.slice(0, n);
      return res.length ? res : null;
    }

    function fill(data) {
      var cols = 0;
      var rows = data.length;
      var i, row;

      for(i = 0; i < data.length; i++) {
        row = data[i] || (data[i] = []);
        cols = Math.max(row.length, cols);
      }

      cols = Math.max(50, cols);
      rows = Math.max(100, rows);

      for(i = 0; i < rows; i++) {
        row = data[i] || (data[i] = []);
        for (var j = 0; j < cols + 1; j++) {
          if (row[j] === undefined) {
            row[j] = null;
          }
        }
      }

      return data;
    }

    model.$render = function () {
      var value = null;
      try {
        value = JSON.parse(model.$viewValue) || null;
      } catch (e) {
      }
      if (inst) {
        value = fill(value || []);
        inst.loadData(value || null);
        setTimeout(function () {
          inst.render();
        }, 300);
      }
    };

    scope.$on("$destroy", function () {
      if (inst) {
        inst.destroy();
        inst = null;
      }
    });
  },
  template_editable: null,
  template_readonly: null,
  template:
    "<div></div>"
});

})();
