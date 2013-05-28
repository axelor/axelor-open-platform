(function(){

var ui = angular.module('axelor.ui');

/**
 * The Integer input widget.
 */
ui.formInput('Integer', {

	css: 'integer-item',
	
	link: function(scope, element, attrs, model) {
		
		var props = scope.field,
			precision = props.precision || 18,
			scale = props.scale || 2;
		
		var isDecimal = this.isDecimal,
			pattern = isDecimal ? /^(-)?\d+(\.\d+)?$/ : /^\s*-?[0-9]*\s*$/;
		
		scope.isNumber = function(value) {
			return _.isEmpty(value) || _.isNumber(value) || pattern.test(value);
		};

		scope.validate = scope.isValid = function(value) {
			var valid = scope.isNumber(value);
            if (valid && _.isString(value)) {
            	var parts = value.split(/\./),
            		integer = parts[0] || "",
            		decimal = parts[1] || "";
            	valid = (integer.length <= precision - scale) && (decimal.length <= scale);
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
		element.on("spinchange", function(e) {
			updateModel(element.val(), true);
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

/**
 * The Decimal input widget.
 */
ui.formInput('Decimal', 'Integer', {
	css: 'decimal-item',
	isDecimal: true
});

/**
 * The Progress widget.
 * 
 */
ui.formInput('Progress', 'Integer', {
	
	css: 'progress-item',
	cellCss: 'form-item progress-item',
	
	link_readonly: function(scope, element, attrs, model) {
		
		scope.$watch("getValue()", function(value, old) {
			var width = value || 0;
			var css = "progress-striped";
			
			if (width < 50) {
				css += " progress-danger active";
			} else if (width < 100) {
				css += " progress-warning active";
			} else {
				css += " progress-success";
			}
			
			scope.css = css;
			scope.width = width;
		});
	},
	
	template_readonly:
	'<div class="progress {{css}}">'+
	  '<div class="bar" style="width: {{width}}%;"></div>'+
	'</div>'
});

})(this);
