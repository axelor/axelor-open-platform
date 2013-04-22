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
		if (value && field.type === "password") {
			value = _.str.repeat('*', value.length);
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
ui.formItem('Label', {

	css: 'label-item',
	cellCss: 'form-label',

	transclude: true,
	
	link: function(scope, element, attrs) {
		var field = scope.field;
		if (field && field.required) {
			element.addClass('required');
		}
	},

	template: '<label ui-help-popover ng-transclude></label>'
});

/**
 * The Spacer widget.
 *
 */
ui.formItem('Spacer', {
	css: 'spacer-item',
	template: '<div>&nbsp;</div>'
});

/**
 * The Separator widget.
 *
 */
ui.formItem('Separator', {
	css: 'separator-item',
	showTitle: false,
	scope: {
		title: '@'
	},
	template: '<div><span style="padding-left: 4px;">{{title}}</span><hr style="margin: 4px 0;"></div>'
});

/**
 * The Static Text widget.
 *
 */
ui.formItem('Static', {
	css: 'static-item',
	transclude: true,
	template: '<label ng-transclude></label>'
});

/**
 * The button widget.
 */
ui.formItem('Button', {
	css: 'button-item',
	transclude: true,
	link: function(scope, element, attrs, model) {

		element.on("click", function(e) {
			scope.fireAction("onClick");
		});
	},
	template: '<button class="btn" type="button" ng-transclude></button>'
});

/**
 * The String widget.
 */
ui.formInput('String', {
	css: 'string-item'
});

/**
 * The Email input widget.
 */
ui.formInput('Email', {
	css: 'email-item',
	template_editable: '<input type="email">',
	template_readonly: '<a target="_blank" href="mailto:{{text}}">{{text}}</a>'
});

/**
 * The Phone input widget.
 */
ui.formInput('Phone', {
	css: 'phone-item',
	template_editable: '<input type="tel">'
});

/**
 * The Integer input widget.
 */
ui.formInput('Integer', {

	css: 'integer-item',
	
	link_editable: function(scope, element, attrs, model) {

		var props = scope.field,
			precision = props.precision || 18,
			scale = props.scale || 2;
		
		var options = {
			step: 1,
			spin: onSpin,
			change: function( event, ui ) {
				updateModel(element.val(), true);
			}
		};
		
		var isDecimal = this.isDecimal,
			pattern = isDecimal ? /^(-)?\d+(\.\d+)?$/ : /^\s*-?[0-9]*\s*$/;

		function isNumber(value) {
			return _.isEmpty(value) || _.isNumber(value) || pattern.test(value);
		}

		function isValid(value) {
			var valid = isNumber(value);
            if (valid && _.isString(value)) {
            	var parts = value.split(/\./),
            		integer = parts[0] || "",
            		decimal = parts[1] || "";
            	valid = (integer.length <= precision - scale) && (decimal.length <= scale);
            }
            return valid;
		}

		function format(value) {
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
		}
		
		function updateModel(value, handle) {
			var onChange = scope.$events.onChange;

			if (!isNumber(value)) {
				return;
            }

			value = format(value);
			
			element.val(value);
			scope.setValue(value);
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
			
			if (!isNumber(text)) {
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
		
		scope.validate = function(value) {
			return isValid(value);
		};

		scope.format = function(value) {
			return format(value);
		};

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
			scope.$elem_editable = element.parent();
			model.$render = function() {
				var value = model.$viewValue;
				if (value) {
					value = format(value);
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
 * The Boolean input widget.
 */
ui.formInput('Boolean', {
	
	css: 'boolean-item',
	
	cellCss: 'form-item boolean-item',
	
	link_editable: function(scope, element, attrs, model) {

		var onChange = scope.$events.onChange || angular.noop;
		
		scope.$render_editable = function() {
			element[0].checked = scope.parse(model.$viewValue);
		};
		
		element.click(function(){
			scope.setValue(this.checked);
			setTimeout(function(){
				scope.$apply();
				setTimeout(onChange);
			});
		});
	},
	template_editable: '<input type="checkbox">',
	template_readonly: '<input type="checkbox" disabled="disabled" ng-checked="text">'
});

// configure datepicket
if (_t.calendar) {
	$.timepicker.setDefaults(_t.calendar);
}

// configure ui.mask
function createTwoDigitDefinition( maximum ) {
	return function( value ) {
		var number = parseInt( value, 10 );

		if (value === "" || /\D/.test(value) || _.isNaN(number)) {
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

// datepicker keyboad navigation hack
var _doKeyDown = $.datepicker._doKeyDown;
$.extend($.datepicker, {
	_doKeyDown: function(event) {
		var inst = $.datepicker._getInst(event.target),
			handled = false;
		inst._keyEvent = true;
		if ($.datepicker._datepickerShowing) {
			switch (event.keyCode) {
			case 36: // home
				$.datepicker._gotoToday(event.target);
				handled = true;
				break;
			case 37: // left
				$.datepicker._adjustDate(event.target, -1, "D");
				handled = true;
				break;
			case 38: // up
				$.datepicker._adjustDate(event.target, -7, "D");
				handled = true;
				break;
			case 39: // right
				$.datepicker._adjustDate(event.target, +1, "D");
				handled = true;
				break;
			case 40: // down
				$.datepicker._adjustDate(event.target, +7, "D");
				handled = true;
				break;
			}
			if (handled) {
				event.ctrlKey = true;
			}
		} else if (event.keyCode === 36 && event.ctrlKey) { // display the date picker on ctrl+home
			$.datepicker._showDatepicker(this);
			handled = true;
		}
		if (handled) {
			event.preventDefault();
			event.stopPropagation();
		} else {
			_doKeyDown(event);
		}
	}
});

/**
 * The DateTime input widget.
 */
ui.formInput('DateTime', {

	css	: 'datetime-item',
	
	format: 'DD/MM/YYYY HH:mm',
	mask: 'DD/MM/YYYY HH:mm',

	init: function(scope) {

		var isDate = this.isDate,
			format = this.format;
		
		scope.parse = function(value) {
			if (angular.isDate(value)) {
				isDate ? moment(value).sod().format('YYYY-MM-DD') : value.toISOString();
			}
			return value;
		},

		scope.format = function(value) {
			if (value) {
				return moment(value).format(format);
			}
			return value;
		};
	},

	link_editable: function(scope, element, attrs, model) {
		var input = element.children('input:first');
		var button = element.find('i:first');
		var onChange = scope.$events.onChange;
		
		var options = {
			dateFormat: 'dd/mm/yy',
			showButtonsPanel: false,
			showTime: false,
			showOn: null,
			onSelect: function(dateText, inst) {
				input.mask('value', dateText);
				updateModel();
				if (changed && onChange) {
					setTimeout(onChange);
				}
				if (!inst.timeDefined) {
					input.datetimepicker('hide');
					setTimeout(function(){
						input.focus().select();
					});
				}
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
		var rendering = false;

		input.on('change', function(e, ui){
			changed = !rendering;
		});
		input.on('blur', function() {
			if (changed) {
				changed = false;
				updateModel();
				if (scope.$events.onChange) {
					setTimeout(scope.$events.onChange);
				}
			}
		});
		input.on('keydown', function(e){

			if (e.isDefaultPrevented()) {
				return;
			}

			if (e.keyCode === $.ui.keyCode.DOWN) {
				input.datetimepicker('show');
				e.stopPropagation();
				e.preventDefault();
				return false;
			}
			if (e.keyCode === $.ui.keyCode.ENTER && $(this).datepicker("widget").is(':visible')) {
				e.stopPropagation();
				e.preventDefault();
				return false;
			}
			if (e.keyCode === $.ui.keyCode.ENTER || e.keyCode === $.ui.keyCode.TAB) {
				if (changed) updateModel();
			}
		});
		button.click(function(e, ui){
			if (scope.isReadonly(element)) {
				return;
			}
			input.datetimepicker('show');
		});

		function updateModel() {
			var masked = input.mask("value") || '',
				value = input.datetimepicker('getDate'),
				oldValue = scope.getValue();

			if (_.isEmpty(masked)) {
				value = null;
			}

			value = scope.parse(value);
			
			if (angular.equals(value, oldValue)) {
				return;
			}
			
			scope.setValue(value, true);
			setTimeout(function(){
				scope.$apply();
			});
		}

		scope.$render_editable = function() {
			rendering = true;
			try {
				var value = scope.getText();
				if (value) {
					input.mask('value', value);
					input.datetimepicker('setDate', value);
				} else {
					input.mask('value', '');
				}
			} finally {
				rendering = false;
			}
		};
	},
	template_editable:
	'<span class="picker-input">'+
	  '<input type="text" autocomplete="off">'+
	  '<span class="picker-icons">'+
	  	'<i class="icon-calendar"></i>'+
	  '</span>'+
	'</span>'
});

ui.formInput('Date', 'DateTime', {
	format: 'DD/MM/YYYY',
	mask: 'DD/MM/YYYY',
	isDate: true
});

ui.formInput('Time', 'DateTime', {

	css: 'time-item',
	mask: 'HH:mm',
	
	link_editable: function(scope, element, attrs, model) {
		
		element.mask({
			mask: this.mask
		});
		
		element.change(function(e, ui) {
			updateModel();
		});
		
		element.on('keydown', function(e){
			if (e.isDefaultPrevented()) {
				return;
			}
			if (e.keyCode === $.ui.keyCode.ENTER || e.keyCode === $.ui.keyCode.TAB) {
				updateModel();
			}
		});
		
		function updateModel() {
			var value = element.val();
			if (model.$viewValue === value)
				return;
			scope.$apply(function(){
				model.$setViewValue(element.val());
			});
		}
	},
	template_editable: '<input type="text">'
});

/**
 * The Text input widget.
 */
ui.formInput('Text', {
	css: 'text-item',
	template_editable: '<textarea rows="8"></textarea>'
});

ui.formInput('Password', {
	
	css: 'password-item',
	
	format: function(scope, value, editable) {
		if (!value || editable) {
			return value;
		}
		return _.str.repeat('*', value.length);
	},
	
	template_editable: '<input type="password">'
});

ui.formInput('Select', {

	css: 'select-item',
	cellCss: 'form-item select-item',

	link_editable: function(scope, element, attrs, model) {

		var props = scope.field,
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
			if (scope.isReadonly(element)) {
				return;
			}
			input.autocomplete("search" , '');
		};
		
		function updateValue(value) {
			var onChange = scope.$evetns.onChange;
			scope.$apply(function(){
				this.setValue(value);
				if (onChange) {
					setTimeout(onChange);
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

		scope.$render_editable = function() {
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
	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
	'</span>'
});

ui.formInput('SelectQuery', {
	
	css: 'select-item',
	cellCss: 'form-item select-item',

	link_editable: function(scope, element, attrs, model) {
		
		var query = scope.$eval(attrs.query),
			input = element.children('input:first');

		scope.showSelection = function() {
			input.autocomplete("search" , '');
		};
		
		input.keydown(function(e){
			if (e.keyCode != 9)
				return false;
		});
		
		scope.$render_editable = function() {
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
	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
	'</span>'
});

ui.formInput('Image', {
	
	css: 'image-item',
	cellCss: 'form-item image-item',
	
	BLANK: "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",

	link_editable: function(scope, element, attrs, model) {
		
		var input = element.children('input:first');
		var image = element.children('img:first');
		var buttons = element.children('.btn-group');
		var BLANK = this.BLANK;
		
		input.add(buttons).hide();
		image.add(buttons).hover(function(){
			buttons.show();
		}, function() {
			buttons.hide();
		});

		scope.doSelect = function() {
			input.click();
		};
		
		scope.doSave = function() {
			var content = image.get(0).src;
			if (content) {
				window.open(content);
			}
		};
		
		scope.doRemove = function() {
			image.get(0).src = BLANK;
			image.get(0).src = null;
			input.val(null);
			update(null);
		};
		
		input.change(function(e, ui) {
			var file = input.get(0).files[0];
			var reader = new FileReader();
			
			reader.onload = function(e){
				var content = e.target.result;
				image.get(0).src = content;
				update(content);
			};

			reader.readAsDataURL(file);
		});
		
		function update(value) {
			setTimeout(function(){
				model.$setViewValue(value);
				scope.$apply();
			});
		}

		scope.$render_editable = function() {
			var content = model.$viewValue || null;
			if (content == null) {
				image.get(0).src = BLANK;
			}
			image.get(0).src = content;
		};
	},
	link_readonly: function(scope, element, attrs, model) {

		var BLANK = this.BLANK;

		scope.$render_readonly = function() {
			var content = model.$viewValue || null;
			if (content == null) {
				element.get(0).src = BLANK;
			}
			element.get(0).src = content;
		};
	},
	template_editable:
	'<div>' +
		'<input type="file" accept="image/*">' +
		'<img class="img-polaroid" style="width: 140px; height: 140px;">' +
		'<div class="btn-group">' +
			'<button ng-click="doSelect()" class="btn" type="button"><i class="icon-upload-alt"></i></button>' +
			'<button ng-click="doSave()" class="btn" type="button"><i class="icon-download-alt"></i></button>' +
			'<button ng-click="doRemove()" class="btn" type="button"><i class="icon-remove"></i></button>' +
		'</div>' +
	'</div>',
	
	template_readonly: '<img class="img-polaroid" style="width: 140px; height: 140px;">'
});

ui.formInput('Binary', {
	
	css: 'file-item',
	cellCss: 'form-item file-item',
	
	link_editable: function(scope, element, attrs, model) {
		
		var input = element.children('input:first').hide();
		var frame = element.children("iframe").hide();

		scope.doSelect = function() {
			input.click();
		};
		
		scope.doSave = function() {
			var record = scope.record,
				model = scope._model,
				field = element.attr('x-field');
			var url = "ws/rest/" + model + "/" + record.id + "/" + field + "/download";
			frame.attr("src", url);
			setTimeout(function(){
				frame.attr("src", "");
			},100);
		};
		
		scope.doRemove = function() {
			var record = scope.record;
			input.val(null);
			model.$setViewValue(null);
			record.$upload = null;
		};

		input.change(function(e) {
			var file = input.get(0).files[0];
			var record = scope.record;
			if (file) {
				record.$upload = {
					field: element.attr('x-field'),
					file: file
				};
				//Update file and mine just in case of new record
				if(!record.id && scope._model == 'com.axelor.meta.db.MetaFile'){
					record.fileName = file.name;
					record.mine = file.type;
				}
				record.size = file.size;
				setTimeout(function(){
					model.$setViewValue(0); // mark form for save
					scope.$apply();
				});
			}
		});
		
	},
	template_editable:
	'<div>' +
		'<iframe></iframe>' +
		'<input type="file">' +
		'<div class="btn-group">' +
			'<button ng-click="doSelect()" class="btn" type="button"><i class="icon-upload-alt"></i></button>' +
			'<button ng-click="doSave()" class="btn" type="button"><i class="icon-download-alt"></i></button>' +
			'<button ng-click="doRemove()" class="btn" type="button"><i class="icon-remove"></i></button>' +
		'</div>' +
	'</div>'
});

})(this);
