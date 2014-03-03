/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
(function(){

var ui = angular.module('axelor.ui');

// configure datepicker
if (_t.calendar) {
	$.timepicker.setDefaults(_t.calendar);
	$.datepicker.setDefaults(_t.calendar);
}

// configure ui.mask
function createTwoDigitDefinition( maximum ) {
	return function( value ) {
		var number = parseInt( value, 10 );

		if (value === "" || /\D/.test(value) || _.isNaN(number)) {
			return false;
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

var _updateDatepicker = $.datepicker._updateDatepicker;
$.datepicker._updateDatepicker = function(inst) {
	if (!$.datepicker._noUpdate) {
		return _updateDatepicker.call($.datepicker, inst);
	}
};

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
				return isDate ? moment(value).startOf('day').format('YYYY-MM-DD') : value.toISOString();
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
		var props = scope.field;
		var isDate = this.isDate;
		
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
			}
		});
		input.on('grid:check', function () {
			updateModel();
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
			scope.applyLater();
		}

		scope.validate = function(value) {
			var minSize = props.minSize === 'now' ? moment() : props.minSize,
				maxSize = props.maxSize,
				input = moment(value),
				valid = true;

			if(isDate) {
				if(minSize) minSize = moment(minSize).startOf('day');
				if(maxSize) maxSize = moment(maxSize).startOf('day');
			}
			else {
				if(minSize) minSize = moment(minSize);
				if(maxSize) maxSize = moment(maxSize);
			}

			if(minSize) {
				if(!input) return false;
				valid = !input.isBefore(minSize) ;
			}
			if(valid && maxSize) {
				if(!input) return true;
				valid = !input.isAfter(maxSize) ;
			}

			return valid;
		};

		scope.$render_editable = function() {
			rendering = true;
			try {
				var value = scope.getText();
				if (value) {
					input.mask('value', value);
					try {
						$.datepicker._noUpdate = true;
						$.datepicker._setDateDatepicker(input[0], value);
					} finally {
						$.datepicker._noUpdate = false;
					}
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
	  	'<i class="fa fa-calendar"></i>'+
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
	
	init: function(scope) {
		this._super(scope);
		
		scope.parse = function(value) {
			return value;
		},

		scope.format = function(value) {
			return value;
		};
	},
	
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
		
		scope.validate = function(value) {
			return !value || /^(\d+:\d+)$/.test(value);
		};
		
		function updateModel() {
			var value = element.val() || '',
				oldValue = scope.getValue();

			value = scope.parse(value);
			
			if (angular.equals(value, oldValue)) {
				return;
			}
			
			scope.setValue(value, true);
			scope.applyLater();
		}
		
		scope.$render_editable = function() {
			var value = scope.getText();
			if (value) {
				element.mask('value', value);
			} else {
				element.mask('value', '');
			}
		};
	},
	template_editable: '<input type="text">'
});

ui.formInput('RelativeTime', 'DateTime', {
	
	init: function(scope) {
		this._super(scope);
		
		scope.isReadonly = function() {
			return true;
		};
		
		scope.format = function(value) {
			if (value) {
				return moment(value).fromNow();
			}
			return "";
		};
	}
});

function formatDuration(field, value) {
	if (!value || !_.isNumber(value)) {
		return value;
	}
	
	var h = Math.floor(value / 3600);
	var m = Math.floor((value % 3600) / 60);
	var s = Math.floor((value % 3600) % 60);
	
	h = _.str.pad(h, field.big ? 3 : 2, '0');
	m = _.str.pad(m, 2, '0');
	s = _.str.pad(s, 2, '0');

	var text = h + ':' + m;
	
	if (field.seconds) {
		text = text + ':' + s;
	}
	
	return text;
}

ui.formatDuration = formatDuration;
	
ui.formInput('Duration', 'Time', {
	
	mask: '99:mm',
	
	init: function(scope) {
		this._super(scope);
		
		var field = scope.field;
		var pattern = /^\d+:\d+(:\d+)?$/;

		scope.format = function(value) {
			return formatDuration(field, value);
		};
		
		scope.parse = function(value) {
			if (!value || !_.isString(value)) {
				return value;
			}
			if (!pattern.test(value)) {
				return null;
			}
			
			var parts = value.split(':'),
				first = +(parts[0]),
				next = +(parts[1]),
				last = +(parts[2] || 0);
			
			return (first * 60 * 60) + (next * 60) + last;
		};
	},
	
	link_editable: function(scope, element, attrs, model) {
		var field = scope.field || {},
			mask = this.mask;
		
		if (field.big) {
			mask = "999:mm";
		}
		if (field.seconds) {
			mask = mask + ":mm";
		}
		
		this.mask = mask;
		this._super.apply(this, arguments);
		
		scope.validate = function(value) {
			return !value || _.isNumber(value);
		};
	}
});

})(this);
