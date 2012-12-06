(function(){

var ui = angular.module('axelor.ui');

/**
 * The Label widget.
 *
 */
var LabelItem = {
	css: 'label-item',
	cellCss: 'form-label',
	transclude: true,
	template: '<label ng-transclude></label>',
	link: function(scope, element, attrs, controller) {
			var field = scope.getViewDef(attrs.forWidget);
			if (field && field.required) {
				element.addClass('required');
			}
			if (field && field.help) {
				element.tooltip({
					html: true,
					delay: { show: 1000, hide: 100 },
					title: field.help
				});
			}
	}
};

/**
 * The Spacer widget.
 *
 */
var SpacerItem = {
	css: 'spacer-item',
	template: '<div></div>'
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
	template: '<input type="number">'
};

/**
 * The Decimal input widget.
 */
var DecimalItem = {
	css: 'decimal-item',
	require: '?ngModel',
	
	link: function(scope, element, attrs, controller) {
		controller.$parsers.unshift(function(viewValue) {
			var isNumber = _.isEmpty(viewValue) || _.isNumber(viewValue) || /^(-)?\d+(\.\d+)?$/.test(viewValue);
			controller.$setValidity('format', isNumber);
			return isNumber ? viewValue : undefined;
		});
	},
	template: '<input type="text">'
};

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

/**
 * The DateTime input widget.
 */
var DateTimeItem = {

	css	: 'datetime-item',
	require: '?ngModel',
	
	format: 'DD/MM/YYYY HH:mm',
	mask: '39/19/9999 29:69',

	link: function(scope, element, attrs, controller) {
		
		var self = this;
		var input = element.children('input:first');
		var button = element.children('button:first');
		var options = {
			dateFormat: 'dd/mm/yy',
			showButtonsPanel: false,
			showTime: false,
			showOn: null,
			onSelect: function(dateText, inst) {
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
			if (e.keyCode == 40) // DOWN
				input.datetimepicker('show');
		});
		button.click(function(e, ui){
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
				input.val(value);
				input.datetimepicker('setDate', value);
			} else {
				input.val("");
			}
		};
	},
	template:
		'<div class="input-append">'+
	    	'<input type="text" autocomplete="off">'+
	    	'<button class="btn" type="button" tabindex="-1">'+
	    		'<i class="icon-calendar"></i>'+
	    	'</button>'+
	    '</div>'
};

var DateItem = _.extend({}, DateTimeItem, {
	format: 'DD/MM/YYYY',
	mask: '39/19/9999',
	isDate: true
});

var TimeItem = {
	css: 'time-item',
	mask: '29:69',
	require: '?ngModel',
	link: function(scope, element, attrs, model) {
		
		element.mask({
			mask: this.mask
		});
		
		element.change(function(e, ui) {
			setTimeout(function(){
				scope.$apply(function(){
					model.$setViewValue(element.val());
				});
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
	'<div class="input-append">'+
		'<input type="text" autocomplete="off">'+
		'<button class="btn" type="button" tabindex="-1" ng-click="showSelection()"><i class="icon-caret-down"></i></button>'+
	'</div>'
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
		'<div class="input-append">'+
			'<input type="text" autocomplete="off">'+
			'<button class="btn" type="button" tabindex="-1" ng-click="showSelection()"><i class="icon-caret-down"></i></button>'+
		'</div>'
};

// define input masks (number rules)
_.each([0,1,2,3,4,5,6,7,8,9], function(n){
	$.ui.mask.definitions[""+n] = "[0-" + n + "]";
});

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
