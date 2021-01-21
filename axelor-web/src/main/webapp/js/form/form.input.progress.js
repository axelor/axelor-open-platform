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

ui.ProgressMixin = {

  css: 'progress-item',
  cellCss: 'form-item progress-item',
  metaWidget: true,

  link_readonly: function(scope, element, attrs, model) {

    var field = scope.field || {},
      that = this;

    scope.$watch("getValue()", function progressValueWatch(value, old) {
      var props = that.compute(field, value);
      scope.cssClasses = 'progress ' + props.css;
      scope.styles = {
        width: props.width + '%'
      };
      scope.css = props.css;
      scope.width = props.width;
    });
  },

  compute: function(field, value) {

    var max = +(field.max) || 100,
      min = +(field.min) || 0;

    var colors = [
      ["r", 24],	// 00 - 24 (red)
      ["y", 49],	// 25 - 49 (yellow)
      ["b", 74],  // 50 - 74 (blue)
      ["g", 100]  // 75 - 100 (green)
    ];

    if (field.colors) {
      colors = _.chain(field.colors.split(/,/)).invoke('split', /:/).value() || [];
    }

    colors.reverse();

    var styles = {
      "r": "progress-danger",
      "y": "progress-warning",
      "b": "progress-primary",
      "g": "progress-success"
    };

    var width = +(value) || 0;
    var css = "progress-striped";

    width = (width * 100) / (max - min);
    width = Math.min(Math.round(width), 100);

    var color = "";
    for(var i = 0 ; i < colors.length; i++) {
      var c = colors[i][0];
      var v = +colors[i][1];
      if (width <= v) {
        color = styles[c] || "";
      }
    }

    css += " " + color;
    if (width < 100) {
      css += " " + "active";
    }

    return {
      css: css,
      width: width
    };
  },

  template_readonly:
  '<div ng-class="cssClasses">'+
    '<div class="bar" ng-style="styles"></div>'+
  '</div>'
};

/**
 * The Progress widget with integer/decimal input.
 *
 */
ui.formInput('Progress', 'Integer', _.extend({}, ui.ProgressMixin));

/**
 * The Progress widget with selection input.
 *
 */
ui.formInput('SelectProgress', 'Select', _.extend({}, ui.ProgressMixin));

})();
