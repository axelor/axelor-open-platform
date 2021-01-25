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

var regional = {
  monthNames: [
    _t('January'),
    _t('February'),
    _t('March'),
    _t('April'),
    _t('May'),
    _t('June'),
    _t('July'),
    _t('August'),
    _t('September'),
    _t('October'),
    _t('November'),
    _t('December')],

  monthNamesShort: [
    _t('Jan'),
    _t('Feb'),
    _t('Mar'),
    _t('Apr'),
    _t('May'),
    _t('Jun'),
    _t('Jul'),
    _t('Aug'),
    _t('Sep'),
    _t('Oct'),
    _t('Nov'),
    _t('Dec')],

  dayNames: [
    _t('Sunday'),
    _t('Monday'),
    _t('Tuesday'),
    _t('Wednesday'),
    _t('Thursday'),
    _t('Friday'),
    _t('Saturday')],

  dayNamesShort :	[_t('Sun'), _t('Mon'), _t('Tue'), _t('Wed'), _t('Thu'), _t('Fri'), _t('Sat')],
   dayNamesMin :	[_t('Su'), _t('Mo'), _t('Tu'), _t('We'), _t('Th'), _t('Fr'), _t('Sa')],
  weekHeader : 	_t('Wk'),
  hourText : 		_t('Hour'),
  minuteText : 	_t('Minute'),
  secondText : 	_t('Second'),
  currentText : 	_t('Now'),
  closeText :		_t('Done'),
  prevText : 		_t('Prev'),
  nextText : 		_t('Next'),
  firstDay: 		moment.localeData(ui.getBrowserLocale()).firstDayOfWeek()
};

// configure datepicker
$.timepicker.setDefaults(regional);
$.datepicker.setDefaults(regional);

// configure moment.js
moment.locale('en', {
  months: regional.monthNames,
  monthsShort: regional.monthNamesShort,
  weekdays: regional.dayNames,
  weekdaysShort: regional.dayNamesShort,
  weekdaysMin: regional.dayNamesMin,
  calendar : {
        sameDay : _t('[Today at] LT'),
        nextDay : _t('[Tomorrow at] LT'),
        nextWeek : _t('dddd [at] LT'),
        lastDay : _t('[Yesterday at] LT'),
        lastWeek : _t('[Last] dddd [at] LT'),
        sameElse : _t('L')
    },
  relativeTime : {
        future : _t("in %s"),
        past : _t("%s ago"),
        s : _t("a few seconds"),
        m : _t("a minute"),
        mm : _t("%d minutes"),
        h : _t("an hour"),
        hh : _t("%d hours"),
        d : _t("a day"),
        dd : _t("%d days"),
        M : _t("a month"),
        MM : _t("%d months"),
        y : _t("a year"),
        yy : _t("%d years")
    }
});

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

  temp = parseInt( value, 10 );

  // convert 2 digit years to 4 digits, this allows us to type 80 <right>
  if ( value.length <= 2 ) {
    // before "32" we assume 2000's otherwise 1900's
    if ( temp < 10 ) {
      return "200" + temp;
    } else if ( temp < 32 ) {
      return "20" + temp;
    } else {
      return "19" + temp;
    }
  }
  if ( temp === 0 ) {
    return "2000";
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

var _parseDate = $.datepicker.parseDate;
$.datepicker.parseDate = function (format, value, settings) {
  if (_.isString(value) && value.indexOf('_') > -1) {
    return;
  }
  return _parseDate.call($.datepicker, format, value, settings);
};

// suppress annoying logs
$.timepicker.log = function () {};

/**
 * The DateTime input widget.
 */
ui.formInput('DateTime', {

  css	: 'datetime-item',

  getFormat: function () { return ui.dateTimeFormat; },
  getMask: function () { return ui.dateTimeFormat; },

  widgets: ['Datetime'],

  init: function(scope) {

    var isDate = this.isDate;
    var that = this;

    scope.parse = function(value) {
      if (angular.isDate(value)) {
        return isDate ? moment(value).startOf('day').format('YYYY-MM-DD') : value.toISOString();
      }
      return value;
    };

    scope.format = function(value) {
      if (value) {
        return moment(value).format(that.getFormat());
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
    var isShowing = false;
    var lastValue = null;

    var options = {
      dateFormat: ui.dateFormat.toLowerCase().replace('yyyy', 'yy'),
      showButtonsPanel: false,
      showTime: false,
      showOn: null,
      beforeShow: function (e, ui) {
        lastValue = input.mask("value") || '';
        isShowing = true;
      },
      onClose: function (e, ui) {
        lastValue = null;
        isShowing = false;
      },
      onSelect: function(dateText, inst) {
        input.mask('value', dateText);
        updateModel();
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
      mask: this.getMask()
    });

    var changed = false;
    var rendering = false;

    input.on('change', function(e, ui){
      if (changed) return;
      if (isShowing) {
        changed = lastValue !== (input.mask("value") || '');
      } else {
        changed = !rendering;
      }
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
      } else if (e.keyCode === $.ui.keyCode.ENTER) {
        updateModel();
      } else if ($(this).datepicker("widget").is(':visible')) {
        e.stopPropagation();
        e.preventDefault();
        return false;
      } else if (e.keyCode === $.ui.keyCode.TAB) {
        if (changed) updateModel();
      }
    });
    button.click(function(e, ui){
      if (scope.isReadonly()) {
        return;
      }
      var mobile = axelor.device.mobile;
      if (mobile) {
        input.attr('disabled', 'disabled')
           .addClass('mobile-disabled-input');
      }
      input.datetimepicker('show');
      if (mobile) {
        input.removeAttr('disabled')
           .removeClass('mobile-disabled-input');
      }
    });

    scope.$onAdjust('size scroll', function () {
      if (isShowing) {
        input.datepicker('widget').hide();
        input.datetimepicker('hide');
      }
    });

    function updateModel() {
      var masked = input.mask("value") || '',
        value = input.datetimepicker('getDate') || null,
        oldValue = scope.getValue() || null;

      if (_.isEmpty(masked)) {
        value = null;
      }
      if (value && !input.mask("valid")) {
        model.$setViewValue(value); // force validation
        model.$render();
        scope.$applyAsync();
        return;
      }

      value = scope.parse(value);

      if (angular.equals(value, oldValue)) {
        return;
      }

      scope.setValue(value, true);
      scope.$applyAsync();
    }

    scope.validate = function(value) {
      var minSize = props.minSize === 'now' ? moment() : props.minSize,
        maxSize = props.maxSize,
        val = moment(value),
        valid = true;

      var masked = input.mask('value');
      if (masked && !input.mask("valid")) {
        return false;
      }

      if(isDate) {
        if(minSize) minSize = moment(minSize).startOf('day');
        if(maxSize) maxSize = moment(maxSize).startOf('day');
      }
      else {
        if(minSize) minSize = moment(minSize);
        if(maxSize) maxSize = moment(maxSize);
      }

      if(minSize) {
        if(!val) return false;
        valid = !val.isBefore(minSize) ;
      }
      if(valid && maxSize) {
        if(!val) return true;
        valid = !val.isAfter(maxSize) ;
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
            $.datepicker._setDateDatepicker(input[0], moment(scope.getValue()).toDate());
            input.mask('refresh');
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
  getFormat: function () { return ui.dateFormat; },
  getMask: function () { return ui.dateFormat; },
  isDate: true
});

ui.formInput('Time', 'DateTime', {

  css: 'time-item',
  mask: 'HH:mm',

  init: function(scope) {
    this._super(scope);

    scope.parse = function(value) {
      return value;
    };

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

    element.on('grid:check', function () {
      updateModel();
    });

    scope.validate = function(value) {
      return !value || /^(\d+:\d+)$/.test(value);
    };

    function updateModel() {
      var masked = element.mask("value") || '',
        value = element.val() || '',
        oldValue = scope.getValue() || '';

      if (value && !element.mask("valid")) {
        return model.$setViewValue(value); // force validation
      }
      if (_.isEmpty(masked)) {
        value = null;
      }

      value = scope.parse(value);

      if (angular.equals(value, oldValue)) {
        return;
      }

      scope.setValue(value, true);
      scope.$applyAsync();
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
  metaWidget: true,
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
  metaWidget: true,

  init: function(scope) {
    this._super(scope);

    var field = scope.field || {};
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
      mask = '99:mm';

    if (field.big) {
      mask = '9' + mask;
    }
    if (field.seconds) {
      mask += ':mm';
    }

    this.mask = mask;
    this._super.apply(this, arguments);

    scope.validate = function(value) {
      return !value || _.isNumber(value);
    };
  }
});

})();
