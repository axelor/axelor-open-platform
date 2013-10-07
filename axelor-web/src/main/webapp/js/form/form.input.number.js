/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
(function(){

var ui = angular.module('axelor.ui');

/**
 * The Numeric input widget.
 */
ui.formInput('Number', {

	css: 'integer-item',
	
	widgets: ['Integer', 'Long', 'Decimal'],
	
	link: function(scope, element, attrs, model) {
		
		var props = scope.field,
			precision = props.precision || 18,
			scale = props.scale || 2;
		
		var isDecimal = props.serverType === "decimal",
			pattern = isDecimal ? /^(-)?\d+(\.\d+)?$/ : /^\s*-?[0-9]*\s*$/;
		
		scope.isNumber = function(value) {
			return _.isEmpty(value) || _.isNumber(value) || pattern.test(value);
		};

		scope.validate = scope.isValid = function(value) {
			var valid = scope.isNumber(value),
				minSize = +props.minSize,
				maxSize = +props.maxSize;
            if (valid && _.isString(value)) {
            	var parts = value.split(/\./),
            		integer = parts[0] || "",
            		decimal = parts[1] || "";
            	valid = (integer.length <= precision - scale) && (decimal.length <= scale);
            	value = +value;
            }

            if (valid && minSize) {
				valid = value >= minSize;
			}
			if (valid && maxSize) {
				valid = value <= maxSize;
			}

        	return valid;
		};

		scope.format = function format(value) {
			if (isDecimal && _.isString(value)) {
				var parts = value.split(/\./),
					integer = parts[0] || "",
					decimal = parts[1] || "",
					negative = integer.indexOf("-") === 0;

				integer = "" + (+integer); // remove leading zero if any
				if (negative && integer.indexOf("-") !== 0) {
				    integer = "-" + integer;
				}

				if (decimal.length <= scale) {
					return integer + '.' + _.string.rpad(decimal, scale, '0');
				}
				decimal = (+decimal.slice(0, scale)) + Math.round("." + decimal.slice(scale));
				decimal = _.string.pad(decimal, scale, '0');
				return integer + '.' + decimal;
			}
			return value;
		};
		
		scope.parse = function(value) {
			if (isDecimal) return value;
			if (value && _.isString(value)) return +value;
			return value;
		};
	},
	
	link_editable: function(scope, element, attrs, model) {

		var props = scope.field;
		
		var options = {
			step: 1
		};

		element.on("spin", onSpin);
		element.on("spinchange", function(e, row) {
			var val = row[props.name],
				handle = val !== undefined && val !== scope.getValue();
			updateModel(element.val(), handle);
		});
		
		function updateModel(value, handle) {
			var onChange = scope.$events.onChange;

			if (!scope.isNumber(value)) {
				return;
            }

			var text = scope.format(value);
			var val = scope.parse(value);

			element.val(text);
			scope.setValue(val);

			setTimeout(function(){
				scope.$apply();
			});

			if (onChange && handle) {
				setTimeout(onChange);
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

			updateModel(value, false);
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
		});
	}
});

})(this);
