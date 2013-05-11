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
		if (value && field.type === "string") {
			var length = value.length;
			value = _.first(value, 50);
			if (length > 50) {
				value.push('...');
			}
			value = value.join('');
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
			placement: function() {
				var coord = $(element.get(0)).offset(),
					viewport = {height: innerHeight, width: window.innerWidth};
				if(viewport.height < (coord.top + 100))
					return 'top';
				if(coord.left > (viewport.width / 2))
					return 'left';
				return 'right';
			},
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
		
		scope.$watch("isReadonly()", function(readonly){
			element.toggleClass("readonly", readonly);
		});
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
	
	init: function(scope, element, attrs, model) {

		scope.validate = function(value) {
			if(_.isEmpty(value))
				return true;
			
			var reg = /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
			return reg.test(value);
		};
	},
	
	template_editable: '<input type="email">',
	template_readonly: '<a target="_blank" href="mailto:{{text}}">{{text}}</a>'
});

/**
 * The URL input widget.
 */
ui.formInput('Url', {
	css: 'url-item',
	template_editable: '<input type="url">',
	template_readonly: '<a target="_blank" href="{{text}}">{{text}}</a>'
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
			if (scope.isReadonly()) {
				element.spinner("disable");
			}
			element.on("on:attrs-change", function(event, data) {
				element.spinner(data.readonly ? "disable" : "enable");
			});
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
	
	widgets: ['Datetime'],

	init: function(scope) {

		var isDate = this.isDate,
			format = this.format;
		
		scope.parse = function(value) {
			if (angular.isDate(value)) {
				return isDate ? moment(value).sod().format('YYYY-MM-DD') : value.toISOString();
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
			if (scope.isReadonly()) {
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
	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var field = scope.field,
			textarea = element.get(0);

		textarea.rows = field.height || 8;

		//Firefox add one more line
		if ($.browser.mozilla){
			textarea.rows -= 1;
		}
    },
	template_editable: '<textarea></textarea >',
	template_readonly: '<pre>{{text}}</pre>'
});

ui.formInput('Password', {
	
	css: 'password-item',
	
	init: function(scope) {

		scope.format = function(value) {
			if (!value || !this.isReadonly()) {
				return value;
			}
			return _.str.repeat('*', value.length);
		};
	},
	
	template_editable: '<input type="password">'
});

ui.formWidget('BaseSelect', {

	findInput: function(element) {
		return element.find('input:first');
	},

	init: function(scope) {
		
		scope.loadSelection = function(request, response) {

		};

		scope.parse = function(value) {
			return value;
		};

		scope.format = function(value) {
			return value;
		};
	},

	link_editable: function (scope, element, attrs, model) {

		var input = this.findInput(element);

		scope.showSelection = function(e) {
			if (scope.isReadonly()) {
				return;
			}
			input.autocomplete("search" , '');
			input.focus();
		};

		scope.handleDelete = function(e) {

		};

		scope.handleSelect = function(e, ui) {
			
		};
		
		scope.handleClose = function(e, ui) {
			
		};
		
		scope.handleOpen = function(e, ui) {
			
		};

		input.autocomplete({
			
			minLength: 0,
			
			source: function(request, response) {
				scope.loadSelection(request, response);
			},

			focus: function(event, ui) {
				return false;
			},
			
			select: function(event, ui) {
				var ret = scope.handleSelect(event, ui);
				if (ret !== undefined) {
					return ret;
				}
				return false;
			},
			
			open: function(event, ui) {
				scope.handleOpen(event, ui);
			},
			
			close: function(event, ui) {
				scope.handleClose(event, ui);
			}
		})
		.focus(function() {
			element.addClass('focus');
		})
		.blur(function() {
			element.removeClass('focus');
		})
		.keydown(function(e) {
			var KEY = $.ui.keyCode;

			switch(e.keyCode) {
			case KEY.DELETE:
			case KEY.BACKSPACE:
				scope.handleDelete(e);
			}
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

ui.formInput('Select', 'BaseSelect', {

	css: 'select-item',
	cellCss: 'form-item select-item',

	init: function(scope) {
		
		this._super(scope);

		var field = scope.field,
			selection = field.selection || [],
			selectionMap = {};
		
		if (_.isArray(field.selection)) {
			selection = field.selection;
		}

		var data = _.map(selection, function(item) {
			var value = "" + item.value;
			selectionMap[value] = item.title;
			return {
				value: value,
				label: item.title || value
			};
		});

		scope.loadSelection = function(request, response) {
			var items = _.filter(data, function(item) {
				var label = item.label || "",
					term = request.term || "";
				return label.toLowerCase().indexOf(term.toLowerCase()) > -1;
			});
			response(items);
		};

		scope.parse = function(value) {
			if (!value || _.isString(value)) return value;
			return value.value;
		};

		scope.format = function(value) {
			if (!value) return value;
			if (_.isString(value)) {
				return selectionMap["" + value] || value;
			}
			return value.label;
		};
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		
		var input = this.findInput(element);
		
		function update(value) {
			scope.setValue(value, true);
			setTimeout(function(){
				scope.$apply();
			});
		}

		scope.handleDelete = function(e) {
			if (e.keyCode === 46) { // DELETE
				update('');
			}
		};

		scope.handleSelect = function(e, ui) {
			update(ui.item);
		};

		scope.$render_editable = function() {
			input.val(this.getText());
		};
	}
});

ui.formInput('MultiSelect', 'Select', {

	css: 'multi-select-item',
	cellCss: 'form-item multi-select-item',
	
	init: function(scope) {
		this._super(scope);

		var __parse = scope.parse;
		var __format = scope.format;
		
		scope.parse = function(value) {
			if (_.isArray(value)) {
				return value.join(', ');
			}
			return __parse(value);
		};

		scope.format = function(value) {
			var items = value,
				values = [];
			if (!value) {
				scope.items = [];
				return value;
			}
			if (!_.isArray(items)) items = items.split(/,\s*/);
			values = _.map(items, function(item) {
				return {
					value: item,
					title: __format(item)
				};
			});
			scope.items = values;
			return _.pluck(values, 'title').join(', ');
		};
		
		scope.getItems = function() {
			return this.items;
		};
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var input = this.findInput(element);

		input.focus(function() {
			scaleInput();
		}).blur(function() {
			scaleInput(50);
			input.val('');
		});

		function scaleInput(width) {
			if (width) {
				return input.width(width);
			}
			var w = element.innerWidth();
				e = element.find('.tag-item:last'),
				p = e.position();
			if (p) {
				w = w - (p.left + e.outerWidth());
			}
			input.width(w - 24);
		}

		function update(value) {
			scope.setValue(value, true);
			setTimeout(function(){
				scope.$apply();
			});
		}
		
		scope.removeItem = function(item) {
			var items = this.getItems(),
				value = _.isString(item) ? item : item.value;

			items = _.chain(items)
					 .pluck('value')
					 .filter(function(v) {
						 return value !== v;
					 })
					 .value();

			update(items);
		};
		
		var __showSelection = scope.showSelection;
		scope.showSelection = function(e) {
			if (e && $(e.srcElement).is('li,i')) {
				return;
			}
			return __showSelection(e);
		};

		scope.handleDelete = function(e) {
			if (input.val()) {
				return;
			}
			var items = this.getItems();
			this.removeItem(_.last(items));
		};
		
		scope.handleSelect = function(e, ui) {
			var items = this.getItems(),
				values = _.pluck(items, 'value');
			if (_.indexOf(values, ui.item.value) > -1)
				return false;

			values.push(ui.item.value);
			update(values);
			scaleInput(50);
		};

		scope.handleOpen = function(e, ui) {
			input.data('autocomplete')
				 .menu
				 .element
				 .position({
					 my: "left top",
					 at: "left bottom",
					 of: element
				 })
				 .width(element.width() - 4);
		};
		
		scope.$render_editable = function() {
			return input.val('');
		};
	},
	template_editable:
	'<div class="tag-select picker-input">'+
	  '<ul ng-click="showSelection($event)">'+
		'<li class="tag-item label label-info" ng-repeat="item in items">{{item.title}} <i class="icon-remove icon-small" ng-click="removeItem(item)"></i></li>'+
		'<li class="tag-selector"><input type="text" autocomplete="off"></li>'+
	  '</ul>'+
	  '<span class="picker-icons">'+
	  	'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
	  '</span>'+
	'</div>',
	template_readonly:
	'<div class="tag-select">'+
		'<span class="label label-info" ng-repeat="item in items">{{item.title}}</span>'+
	'</div>'
});

ui.formInput('SelectQuery', 'Select', {

	link_editable: function(scope, element, attrs, model) {
		
		this._super.apply(this, arguments);

		var current = {};
		
		scope.format = function(value) {
			if (!value) return "";
			if (_.isString(value)) {
				return current.label || value;
			}
			current = value;
			return value.label;
		};

		var query = scope.$eval(attrs.query);
	
		scope.loadSelection = function(request, response) {
			return query(request, response);
		};
	}
});

ui.formInput('RadioSelect', {
	
	css: "radio-select",
	
	link: function(scope, element, attrs, model) {
		
		var field = scope.field,
			selection = [];
	
		if (_.isArray(field.selection)) {
			selection = field.selection;
		}
		scope.selection = selection;

		element.on("change", ":input", function(e) {
			scope.setValue($(e.target).val(), true);
			scope.$apply();
		});
	},
	template_editable: null,
	template_readonly: null,
	template:
	'<ul ng-class="{ readonly: isReadonly() }">'+
		'<li ng-repeat="select in selection">'+
		'<label>'+
			'<input type="radio" name="radio_{{$parent.$id}}" value="{{select.value}}"'+
			' ng-disabled="isReadonly()"'+
			' ng-checked="getValue() == select.value"> {{select.title}}'+
		'</label>'+
		'</li>'+
	'</ul>'
});

ui.formInput('NavSelect', {
	
	css: "nav-select",
	
	link: function(scope, element, attrs, model) {
		
		var field = scope.field,
			selection = [];
	
		if (_.isArray(field.selection)) {
			selection = field.selection;
		}
		scope.selection = selection;
		
		scope.onSelect = function(select) {
			this.setValue(select.value, true);
		};

	},
	template_editable: null,
	template_readonly: null,
	template:
	'<div class="nav-select">'+
	'<ul class="steps">'+
		'<li ng-repeat="select in selection" ng-class="{ active: getValue() == select.value }">'+
			'<a href="" ng-click="onSelect(select)">{{select.title}}</a>'+
		'</li>'+
		'<li></li>'+
	'</ul>'+
	'</div>'
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
		var image = element.children('img:first');
		
		scope.$render_readonly = function() {
			var content = model.$viewValue || null;
			if (content == null) {
				image.get(0).src = BLANK;
			}
			image.get(0).src = content;
		};
	},
	template_editable:
	'<div style="min-width: 140px;">' +
		'<input type="file" accept="image/*">' +
		'<img class="img-polaroid" style="width: 140px; height: 140px; display: inline-block;">' +
		'<div class="btn-group">' +
			'<button ng-click="doSelect()" class="btn" type="button"><i class="icon-upload-alt"></i></button>' +
			'<button ng-click="doSave()" class="btn" type="button"><i class="icon-download-alt"></i></button>' +
			'<button ng-click="doRemove()" class="btn" type="button"><i class="icon-remove"></i></button>' +
		'</div>' +
	'</div>',
	
	template_readonly:
	'<div style="min-width: 140px;">'+
		'<img class="img-polaroid" style="width: 140px; height: 140px;">'+
	'</div>'
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
