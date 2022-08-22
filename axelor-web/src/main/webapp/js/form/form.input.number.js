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

/* jshint validthis: true */

"use strict";

var ui = angular.module('axelor.ui');

/**
 * The Numeric input widget.
 */
ui.formInput('Number', {

  css: 'integer-item',

  widgets: ['Integer', 'Long', 'Decimal'],

  template_readonly: '<span class="display-text">{{localeValue()}}</span>',
  template_editable: '<input type="number">',

  link: function(scope, element, attrs, model) {

    var props = scope.field,
      minSize = +props.minSize,
      maxSize = +props.maxSize;

    scope.isDecimal = function () {
      return props.serverType === "decimal" || props.widget === "decimal";
    };

    var isDecimal = scope.isDecimal(),
      pattern = isDecimal ? /^(-)?\d+(\.\d+)?$/ : /^\s*-?[0-9]*\s*$/;

    scope.scale = function () {
      var scale = [scope.attr('scale'), (props.widgetAttrs || {}).scale, props.scale]
        .find(function (val) { return val !== undefined && val !== null; });
      if (_.isString(scale)) {
        var context = scope.getContext();
        _.chain(scope.fields).map(function (field) {
          return (field || {}).name;
        }).filter(function (fieldName) {
          return _.contains(fieldName, '.');
        }).each(function (fieldName) {
          var val = context[fieldName];
          if (val !== undefined) {
            ui.setNested(context, fieldName, val);
          }
        });
        var jsonField = _.find((scope.handler || {}).fields, function (field) {
          return field.json;
        });
        if (jsonField && context[jsonField.name] !== undefined) {
          context[jsonField.name] = angular.fromJson(context[jsonField.name]);
        }
        scale = scope.$eval(scale, context);
      }
      return _.isNumber(scale) ? scale : 2;
    };

    function precision() {
      var value = scope.attr('precision');
      if (value) {
        return value;
      }
      if ((props.widgetAttrs||{}).precision) {
        return props.widgetAttrs.precision;
      }
      return props.precision || 18;
    }

    scope.isNumber = function(value) {
      return _.isEmpty(value) || _.isNumber(value) || pattern.test(value);
    };

    function isEmpty(value) {
      return _.isEmpty(value) && !_.isNumber(value);
    }

    scope.validate = scope.isValid = function(value) {
      var valid = scope.isNumber(value);
      if (valid && isDecimal && _.isString(value)) {
        value = scope.format(value);
        valid = _.string.trim(value, '-').length - 1 <= precision();
        value = +value;
      }

      if (valid && (minSize || minSize === 0)) {
        valid = value >= minSize || scope.field.nullable && isEmpty(value);
      }
      if (valid && (maxSize || maxSize === 0)) {
        valid = value <= maxSize || scope.field.nullable && isEmpty(value);
      }

      return valid;
    };

    scope.localeValue = function localeValue() {
      var value = scope.getValue();
      var field = isDecimal ? _.extend({}, scope.field, {
        scale: scope.scale(),
        precision: precision()
      }) : scope.field;
      return isDecimal
        ? ui.formatters.decimal(field, value, scope.record)
        : ui.formatters.integer(field, value);
    };

    scope.format = function format(value) {
      if (isDecimal && _.isString(value) && value.trim().length > 0) {
        return parseFloat(value).toFixed(scope.scale());
      }
      return value;
    };

    scope.parse = function(value) {
      if (scope.field.nullable && isEmpty(value)) return null;
      if (_.isString(value)) value = +(value);
      if (_.isNaN(value)) value = null;
      if (_.isNumber(value) && isDecimal) value = value.toFixed(scope.scale());
      return value;
    };

    scope.$on("on:attrs-changed", function (e, attr) {
      if (attr.name === 'scale' || attr.name === 'precision') {
        model.$render();
      }
    });
  },

  link_editable: function(scope, element, attrs, model) {

    var props = scope.field;

    var options = {
      step: 1
    };
    var keyPattern = scope.isDecimal() ? /[\d-.]/ : /[\d-]/;

    element.on("spin", onSpin);
    element.on("spinchange", function(e, row) {
      updateModel(element.val());
      element.focus();
    });
    element.on("grid:check", function(e, row) {
      updateModel(element.val());
    });
    element.on("keydown", function(e, row) {
      if (!axelor.browser.chrome && !e.ctrlKey && e.key.length === 1 && !e.key.match(keyPattern)) {
        e.preventDefault();
        return;
      }
      if (e.keyCode === $.ui.keyCode.ENTER) {
        updateModel(element.val());
      }
    });
    element.on("blur", function () {
      var value = element.val();
      if (value === "") {
        element.val(value);
      }
    });

    var pendingChange = false;

    function handleChange(changed) {
      var onChange = scope.$events.onChange;
      if (onChange && (changed || pendingChange)) {
        pendingChange = false;
        setTimeout(onChange);
      }
    }

    function equals(a, b) {
      if (a === b) return true;
      if (angular.equals(a, b)) return true;
      if (a === "" && b === undefined) return true;
      if (b === "" && a === undefined) return true;
      if (a === undefined || b === undefined) return false;
      if (a === null || b === null) return false;
      if (!scope.isNumber(a) || !scope.isNumber(b)) return false;
      a = a === "" ? a : ((+a) || 0);
      b = b === "" ? b : ((+b) || 0);
      return a === b;
    }

    function updateModel(value, handle) {
      if (!scope.isNumber(value)) {
        return model.$setViewValue(value); // force validation
      }
      var val = scope.parse(value);
      var old = scope.getValue();
      var text = scope.format(value);

      element.val(text);

      if (equals(val, old)) {
        element.val(val);
        return handleChange();
      }

      scope.setValue(val);
      scope.$applyAsync();

      pendingChange = true;

      if (handle !== false) {
        handleChange();
      }
    }

    function onSpin(event, ui) {

      var text = this.value,
        value = ui.value,
        orig = element.spinner('value'),
        parts, integer, decimal, min, max, dir = 0;

      event.preventDefault();

      if (!scope.isNumber(text)) {
        return false;
      }

      if (value < orig)
        dir = -1;
      if (value > orig)
        dir = 1;

      parts = text.split(/\./);
      integer = +parts[0];
      decimal = parts[1];

      integer += dir;
      if (parts.length > 1) {
        value = integer + '.' + decimal;
      }

      min = options.min;
      max = options.max;

      if (_.isNumber(min) && value < min)
        value = min;
      if (_.isNumber(max) && value > max)
        value = max;

      element.val(value);
    }

    if (props.minSize !== undefined)
      options.min = +props.minSize;
    if (props.maxSize !== undefined)
      options.max = +props.maxSize;

    setTimeout(function(){
      element.spinner(options);
      scope.$elem_editable = element.parent();
      model.$render = function() {
        var value = model.$viewValue;
        if (value) {
          value = scope.format(value);
        }
        element.val(value);
        scope.initValue(value);
      };
      model.$render();
      scope.$watch(scope.scale, function (scale) {
        element.attr("step", _.sprintf("%." + scale + "f", 1 / Math.pow(10, scale)));
        model.$render();
      });
    });
  }
});

// fix spinner repeat issue
var oldRepeat = $.ui.spinner.prototype._repeat;
$.ui.spinner.prototype._repeat = function () {
  if (this.element.scope().isReadonly()) {
    return this._stop();
  }
  return oldRepeat.apply(this, arguments);
};

})();
