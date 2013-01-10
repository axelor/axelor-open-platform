(function(){

var ui = angular.module('axelor.ui');

ui.directive('uiHelpPopover', function() {
	
	function addRow(table, label, text, klass) {
		var tr = $('<tr></tr>').appendTo(table);
		if (label) {
			$('<th></th>').text(label + ':').appendTo(tr);
		}
		if (klass == null) {
			text = '<code>' + text + '</code>';
		}
		var td = $('<td></td>').html(text).addClass(klass).appendTo(tr);
		if (!label) {
			td.attr('colspan', 2);
		}
		return table;
	}
	
	function getHelp(scope, element, field, mode) {
		
		var text = field.help;
		var table = $('<table class="field-details"></table>');

		if (text) {
			text = text.replace(/\\n/g, '<br>');
			addRow(table, null, text, 'help-text');
		}
		
		if (mode != 'dev') {
			return table;
		}

		if (text) {
			addRow(table, null, '<hr noshade>', 'help-text');
		}
		
		var model = scope._model;
		if (model === field.target) {
			model = scope.$parent._model;
		}

		addRow(table, _t('Object'), model);
		addRow(table, _t('Field Name'), field.name);
		addRow(table, _t('Field Type'), field.serverType);
		
		if (field.type == 'text') {
			return table;
		}
		
		if (field.domain) {
			addRow(table, _t('Filter'), field.domain);
		}
		
		if (field.target) {
			addRow(table, _t('Reference'), field.target);
		}

		var value = scope.$eval('$$original.' + field.name);
		if (value && field.type === 'many-to-one') {
			value = value.id;
		}
		if (value && /-many$/.test(field.type)) {
			var length = value.length;
			value = _.first(value, 5);
			value = _.map(value, function(v){
				return v.id;
			});
			if (length > 5) {
				value.push('...');
			}
			value = value.join(', ');
		}

		addRow(table, _t('Orig. Value'), value);

		return table;
	}

	return function(scope, element, attrs) {
		var forWidget = attrs.forWidget || element.parents('[x-field]:first').attr('id');
		var field = scope.getViewDef(forWidget);
		if (field == null) {
			return;
		}
		var mode = scope.$eval('app.mode') || 'dev';
		if (!field.help && mode != 'dev') {
			return;
		}

		element.popover({
			html: true,
			delay: { show: 1000, hide: 100 },
			animate: true,
			trigger: 'hover',
			title: function() {
				return element.text();
			},
			content: function() {
				return getHelp(scope, element, field, mode);
			}
		});
	};
});

/**
 * The Label widget.
 *
 */
var LabelItem = {
	css: 'label-item',
	cellCss: 'form-label',
	transclude: true,
	template: '<label ui-help-popover ng-transclude></label>',
	link: function(scope, element, attrs, controller) {
			var field = scope.getViewDef(attrs.forWidget);
			if (field && field.required) {
				element.addClass('required');
			}
	}
};

/**
 * The Spacer widget.
 *
 */
var SpacerItem = {
	css: 'spacer-item',
	template: '<div>&nbsp;</div>'
};

/**
 * The Separator widget.
 *
 */
var SeparatorItem = {
	css: 'separator-item',
	showTitle: false,
	scope: {
		title: '@'
	},
	template: '<div><span style="padding-left: 4px;">{{title}}</span><hr style="margin: 4px 0;"></div>'
};

/**
 * The Static Text widget.
 *
 */
var StaticItem = {
	css: 'static-item',
	transclude: true,
	template: '<label ng-transclude></label>'
};

/**
 * The button widget.
 */
var ButtonItem = {
	css: 'button-item',
	transclude: true,
	template: '<button class="btn" type="button" ng-transclude></button>'
};

/**
 * The String widget.
 */
var StringItem = {
	css: 'string-item',
	template: '<input type="text">'
};

/**
 * The Email input widget.
 */
var EmailItem = {
	css: 'email-item',
	template: '<input type="email">'
};

/**
 * The Phone input widget.
 */
var PhoneItem = {
	css: 'phone-item',
	template: '<input type="tel">'
};

/**
 * The Integer input widget.
 */
var IntegerItem = {
	css: 'integer-item',
	require: '?ngModel',
	link: function(scope, element, attrs, model) {

		var props = scope.getViewDef(element),
			scale = props.scale || this.scale,
			format = "n" + (_.isUndefined(scale) ? 0 : scale);
		
		var options = {
			step: 1,
			numberFormat: format,
			spin: onSpin
		};
		
		model.$parsers.unshift(function(viewValue) {
            var isNumber = _.isNumber(viewValue) || isValid(viewValue);
            model.$setValidity('format', isNumber);
            return isNumber ? viewValue : undefined;
        });

		function isValid(text) {
			return _.isEmpty(text) || /^(-)?\d+(\.\d+)?$/.test(text);
		}
		
		function onSpin(event, ui) {
			
			var text = this.value,
				value = ui.value,
				orig = element.spinner('value'),
				parts, integer, decimal, min, max, dir = 0;

			event.preventDefault();
			
			if (!isValid(text)) {
				return false;
			}

			if (value < orig)
				dir = -1;
			if (value > orig)
				dir = 1;

			parts = text.split(/\./);
			integer = +parts[0];
			decimal = +parts[1];
			
			integer += dir;
			if (parts.length > 1) {
				value = integer + '.' + decimal;
			}
			
			value = +value;
			min = options.min;
			max = options.max;

			if (_.isNumber(min) && value < min)
				value = min;
			if (_.isNumber(max) && value > max)
				value = max;

			element.val(value);
			setTimeout(function(){
				scope.$apply(function(){
					model.$setViewValue(value);
				});
			});
		}

		if (props.minSize !== undefined)
			options.min = +props.minSize;
		if (props.maxSize !== undefined)
			options.max = +props.maxSize;

		setTimeout(function(){
			element.spinner(options);
			if (scope.isReadonly(element)) {
				element.spinner("disable");
			}
			element.on("on:attrs-change", function(event, data) {
				element.spinner(data.readonly ? "disable" : "enable");
			});
		});
	},
	template: '<input type="text">'
};

/**
 * The Decimal input widget.
 */
var DecimalItem = _.extend({}, IntegerItem, {
	css: 'decimal-item',
	step: 2
});

/**
 * The Boolean input widget.
 */
var BooleanItem = {
	css: 'boolean-item',
	template: '<input type="checkbox">'
};

// configure datepicket
if (_t.calendar) {
	$.timepicker.setDefaults(_t.calendar);
}

// configure ui.mask
function createTwoDigitDefinition( maximum ) {
	return function( value ) {
		var number = parseInt( value, 10 );

		if ( value === "" || /\D/.test( value ) ) {
			return;
		}

		// allow "0" if it is the only character in the value,
		// otherwise allow anything from 1 to maximum
		if ( !number && value.length === 2 ) {
			return;
		}

		// pad to 2 characters
		number = ( number < 10 ? "0" : "" ) + number;
		if ( number <= maximum ) {
			return number;
		}
	};
}

function yearsDefinition( value ) {
	var temp;

	// if the value is empty, or contains a non-digit, it is invalid
	if ( value === "" || /\D/.test( value ) ) {
		return false;
	}

	// convert 2 digit years to 4 digits, this allows us to type 80 <right>
	if ( value.length <= 2 ) {
		temp = parseInt( value, 10 );
		// before "32" we assume 2000's otherwise 1900's
		if ( temp < 10 ) {
			return "200" + temp;
		} else if ( temp < 32 ) {
			return "20" + temp;
		} else {
			return "19" + temp;
		}
	}
	if ( value.length === 3 ) {
		return "0"+value;
	}
	if ( value.length === 4 ) {
		return value;
	}
}

$.extend($.ui.mask.prototype.options.definitions, {
	"MM": createTwoDigitDefinition( 12 ),
	"DD": createTwoDigitDefinition( 31 ),
	"YYYY": yearsDefinition,
	"HH": createTwoDigitDefinition( 23 ),
	"mm": createTwoDigitDefinition( 59 )
});

/**
 * The DateTime input widget.
 */
var DateTimeItem = {

	css	: 'datetime-item',
	require: '?ngModel',
	
	format: 'DD/MM/YYYY HH:mm',
	mask: 'DD/MM/YYYY HH:mm',

	link: function(scope, element, attrs, controller) {
		
		var self = this;
		var input = element.children('input:first');
		var button = element.children('i:first');
		var options = {
			dateFormat: 'dd/mm/yy',
			showButtonsPanel: false,
			showTime: false,
			showOn: null,
			onSelect: function(dateText, inst) {
				input.mask('value', dateText);
				updateModel();
				if (!inst.timeDefined)
					input.datetimepicker('hide');
			}
		};

		if (this.isDate) {
			options.showTimepicker = false;
		}

		input.datetimepicker(options);
		input.mask({
			mask: this.mask
		});

		var changed = false;
		input.on('change', function(){
			changed = true;
		});
		input.on('blur', function(){
			if (changed) {
				updateModel();
			}
			changed = false;
		});
		input.on('keydown', function(e){
			if (e.altKey && e.keyCode === $.ui.keyCode.DOWN) {
				input.datetimepicker('show');
				e.preventDefault();
			}
			if (e.keyCode === $.ui.keyCode.ENTER && $(this).datepicker("widget").is(':visible')) {
				e.preventDefault();
			}
		});
		button.click(function(e, ui){
			if (scope.isReadonly(element)) {
				return;
			}
			input.datetimepicker('show');
		});

		function updateModel() {
			var value = input.datetimepicker('getDate'),
				onChange = element.data('$onChange');

			if (angular.isDate(value)) {
				value = self.isDate ? moment(value).sod().format('YYYY-MM-DD') : moment(value).format();
			}
			
			if (controller.$viewValue === value)
				return;

			scope.$apply(function(){
				controller.$setViewValue(value);
			});
			
			if (onChange) {
				onChange._handle();
			}
		}

		controller.$render = function () {
			var value = controller.$viewValue;
			if (value) {
				value = moment(value).format(self.format);
				input.mask('value', value);
				input.datetimepicker('setDate', value);
			} else {
				input.mask('value', '');
			}
		};
	},
	template:
	'<span class="picker-input">'+
	  '<input type="text" autocomplete="off">'+
	  '<i class="icon-calendar"></i>'+
	'</span>'
};

var DateItem = _.extend({}, DateTimeItem, {
	format: 'DD/MM/YYYY',
	mask: 'DD/MM/YYYY',
	isDate: true
});

var TimeItem = {
	css: 'time-item',
	mask: 'HH:mm',
	require: '?ngModel',
	link: function(scope, element, attrs, model) {
		
		element.mask({
			mask: this.mask
		});
		
		element.change(function(e, ui) {
			scope.$apply(function(){
				model.$setViewValue(element.val());
			});
		});
	},
	template: '<input type="text">'
};

/**
 * The Text input widget.
 */
var TextItem = {
	css: 'text-item',
	transclude: true,
	template: '<textarea rows="8" ng-transclude></textarea>'
};

var PasswordItem = {
	css: 'password-item',
	template: '<input type="password">'
};

var SelectItem = {
	css: 'select-item',
	cellCss: 'form-item select-item',
	require: '?ngModel',
	scope: true,
	link: function(scope, element, attrs, model) {

		var props = scope.getViewDef(element),
			multiple = props.multiple,
			input = element.children('input:first'),
			selection = [];
		
		if (_.isArray(props.selection)) {
			selection = props.selection;
		}
		
		var data = _.map(selection, function(item){
			return {
				key: item.value,
				value: item.title
			};
		});

		scope.showSelection = function() {
			input.autocomplete("search" , '');
		};
		
		function updateValue(value) {
			var onChange = element.data('$onChange');
			scope.$apply(function(){
				model.$setViewValue(value);
				if (onChange) {
					onChange._handle();
				}
			});
		}
		
		input.keydown(function(e){

			var KEY = $.ui.keyCode;

			switch(e.keyCode) {
			case KEY.DELETE:
			case KEY.BACKSPACE:
				updateValue('');
				input.val('');
			}
		});
		
		input.autocomplete({
			minLength: 0,
			source: data,
			focus: function(event, ui) {
				return false;
			},
			select: function(event, ui) {
				var val, terms;
				if (multiple) {
					val = model.$modelValue || [];
					terms = this.value || "";

					if (!_.isArray(val)) val = val.split(',');
					if (_.indexOf(val, ui.item.key) > -1)
						return false;

					val.push(ui.item.key);
					
					terms = terms.trim() === "" ? [] : terms.split(/,\s*/);
					terms.push(ui.item.value);
					
					this.value = terms.join(', ');
					updateValue(val);
				} else {
					this.value = ui.item.value;
					updateValue(ui.item.key);
				}
				return false;
			}
		});
		
		model.$render = function() {
			var val = model.$modelValue;
			if (val === null || _.isUndefined(val))
				return input.val('');

			if (props.serverType == "integer") {
				val = "" + val;
			}
			if (!_.isArray(val)) {
				val = [val];
			}
			
			var values = _.filter(data, function(item){
				return val.indexOf(item.key) > -1;
			});
			values = _.pluck(values, 'value');
			setTimeout(function(){
				input.val(multiple ? values.join(',') : _.first(values));
			});
		};
		attrs.$observe('disabled', function(value){
			input.autocomplete(value && 'disable' || 'enable');
		});
	},
	replace: true,
	template:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
	'</span>'
};

var SelectQueryItem = {
		css: 'select-item',
		cellCss: 'form-item select-item',
		require: '?ngModel',
		scope: true,
		link: function(scope, element, attrs, model) {
			
			var query = scope.$eval(attrs.query),
				input = element.children('input:first');

			scope.showSelection = function() {
				input.autocomplete("search" , '');
			};
			
			input.keydown(function(e){
				if (e.keyCode != 9)
					return false;
			});
			
			model.$render = function() {
				var value = model.$modelValue;
				input.val(value);
			};
			
			setTimeout(function(){
				input.autocomplete({
					minLength: 0,
					source: query,
					select: function(event, ui) {
						scope.$apply(function(){
							model.$setViewValue(ui.item.id);
						});
					}
				});
			});
		},
		replace: true,
		template:
		'<span class="picker-input">'+
			'<input type="text" autocomplete="off">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
		'</span>'
};

// register directives

var directives = {
	
	'uiLabel'		: LabelItem,
	'uiSpacer'		: SpacerItem,
	'uiSeparator'	: SeparatorItem,
	'uiStatic'		: StaticItem,
	'uiButton'		: ButtonItem,
	'uiSelect'		: SelectItem,
	'uiSelectQuery'	: SelectQueryItem,
	
	'uiString'	: StringItem,
	'uiEmail'	: EmailItem,
	'uiPhone'	: PhoneItem,
	'uiInteger'	: IntegerItem,
	'uiDecimal'	: DecimalItem,
	'uiBoolean'	: BooleanItem,
	'uiDatetime': DateTimeItem,
	'uiDate'	: DateItem,
	'uiTime'	: TimeItem,
	'uiText'	: TextItem,
	'uiPassword': PasswordItem
};

for(var name in directives) {
	ui.formDirective(name, directives[name]);
}

})(this);
