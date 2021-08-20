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

"use strict";

var ui = angular.module('axelor.ui');

/**
 * The String widget.
 */
ui.formInput('String', {
  css: 'string-item',

  init: function(scope) {
    var field = scope.field;
    var isReadonly = scope.isReadonly;
    var trKey = "$t:" + field.name;

    scope.isReadonly = function () {
      scope.$$readonlyOrig = isReadonly.apply(this, arguments);
      return (scope.record && scope.record[trKey]) || scope.$$readonlyOrig;
    };

    scope.format = function (value) {
      if ((scope.record && scope.record[trKey])) {
        return scope.record[trKey];
      }
      return value;
    };
  },

  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var field = scope.field,
      regex = field.pattern ? new RegExp(field.pattern, 'i') : null,
      minSize = +(field.minSize),
      maxSize = +(field.maxSize);

    scope.validate = function(value) {
      if (_.isEmpty(value)) {
        return true;
      }
      var length = value.length,
        valid = true;

      if (minSize) {
        valid = length >= minSize;
      }
      if(valid && maxSize) {
        valid = length <= maxSize;
      }
      if (valid && regex) {
        valid = regex.test(value);
      }

      return valid;
    };
  },

  template_readonly: '<input type="text" ng-show="text" tabindex="-1" readonly="readonly" class="display-text" value="{{text}}">'
});

/**
 * The Email input widget.
 */
ui.formInput('Email', {

  css: 'email-item',

  metaWidget: true,

  pattern: /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/,

  link: function(scope, element, attrs, model) {

    var pattern = this.pattern;

    scope.validate = function(value) {
      if(_.isEmpty(value)) {
        return true;
      }
      return pattern.test(value);
    };
  },

  template_editable: '<input type="email">',
  template_readonly: '<a target="_blank" ng-show="text" href="mailto:{{text}}">{{text}}</a>'
});

/**
 * The URL input widget.
 */
ui.formInput('Url', {
  css: 'url-item',
  metaWidget: true,
  template_editable: '<input type="url">',
  template_readonly: '<a target="_blank" ng-show="text" href="{{text}}">{{text}}</a>'
});

/**
 * The Text input widget.
 */
ui.formInput('Text', {
  css: 'text-item',
  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);
    var field = scope.field;

    if (element.is('textarea')) {
      element.attr('rows', parseInt(field.height) || 8);
    }

    var field = scope.field,
      regex = field.pattern ? new RegExp(field.pattern, 'i') : null,
      minSize = +(field.minSize),
      maxSize = +(field.maxSize);

    scope.validate = function(value) {
      if (_.isEmpty(value)) {
        return true;
      }
      var length = value.length,
        valid = true;

      if (minSize) {
        valid = length >= minSize;
      }
      if(valid && maxSize) {
        valid = length <= maxSize;
      }
      if (valid && regex) {
        valid = regex.test(value);
      }

      return valid;
    };

  },
  template_editable: '<textarea></textarea >',
  template_readonly: '<pre ng-show="text">{{text}}</pre>'
});

ui.formInput('TextInline', 'Text', {
  css: 'text-item-inline',
  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var field = scope.field;
    var picker = element;
    var input = picker.children('input');

    var container = null;
    var wrapper = $('<div class="slick-editor-dropdown textarea">').css("position", "absolute").hide();
    var textarea = $('<textarea>').appendTo(wrapper);

    scope.waitForActions(function() {
      container = element.parents('.ui-dialog-content,.view-container').first();
      wrapper.height(field.height || 175).appendTo(container);
    });

    var dropdownVisible = false;

    function adjust() {
      if (!wrapper.is(":visible"))
        return;
      if (axelor.device.small) {
        dropdownVisible = false;
        return wrapper.hide();
      }
      wrapper.position({
        my: "left top",
        at: "left top",
        of: picker,
        within: container
      })
      .zIndex(element.zIndex() + 1);
      wrapper.width(element.width());
      textarea.width("auto");
      textarea.css({
        "min-width": textarea.width()
      });
    }

    function onMouseDown(e) {
      if (element.is(':hidden')) {
        return;
      }
      var all = element.add(wrapper);
      var elem = $(e.target);
      if (all.is(elem) || all.has(elem).length > 0) return;
      if (elem.zIndex() > element.parents('.slick-form:first,.slickgrid:first').zIndex()) return;
      if (elem.parents(".ui-dialog:first").zIndex() > element.parents('.slickgrid:first').zIndex()) return;

      element.trigger('hide:slick-editor');
    }

    var canShowOnFocus = true;

    function showPopup(show, focusElem) {
      dropdownVisible = !!show;
      if (dropdownVisible) {
        $(document).on('mousedown', onMouseDown);
        textarea.val(scope.getValue());
        textarea.get(0).selectionEnd = 0;
        wrapper.show().css('display', 'flex');
        adjust();
        setTimeout(function () {
          textarea.focus();
        });
      } else {
        $(document).off('mousedown', onMouseDown);
        wrapper.hide();
        if (focusElem) {
          setTimeout(function () {
            focusElem.focus();
          });
        }
      }
    }

    element.on("hide:slick-editor", function(e) {
      showPopup(false);
    });

    input.on('focus', function () {
      if (canShowOnFocus) {
        showPopup(true);
      } else {
        canShowOnFocus = true;
      }
    });

    input.on('click', function () {
      showPopup(true);
    });

    input.on('keydown', function (e) {
      if (e.keyCode === 40 && e.ctrlKey) { // down key
        showPopup(true);
      }
    });

    textarea.on('blur', function () {
      scope.setValue(textarea.val(), true);
    });

    textarea.on('keydown', function (e) {
      if (e.keyCode === 9) { // tab key
        e.preventDefault();
        showPopup(false, navigateTabbable(e.shiftKey ? -1 : 1));
      }
    });

    function navigateTabbable(inc) {
      var tabbables = element.closest('.slick-form').find(':tabbable');
      var index = (tabbables.index(input) + inc + tabbables.length) % tabbables.length;
      return tabbables.eq(index);
    }

    scope.$watch(attrs.ngModel, function textModelWatch(value) {
      var firstLine = value && value.split(/\n/)[0];
      input.val(firstLine);
    });

    scope.$on("$destroy", function(e){
      wrapper.remove();
      $(document).off('mousedown', onMouseDown);
    });
  },
  template_editable:
      "<span>" +
        "<input type='text' readonly>" +
      "</span>"
});

ui.formInput('Password', 'String', {

  css: 'password-item',

  metaWidget: true,

  init: function(scope) {

    scope.password = function() {
      var value = this.getValue() || "";
      return _.str.repeat('*', value.length);
    };
  },
  template_readonly: '<input type="password" ng-show="text" tabindex="-1" readonly="readonly" class="display-text" value="{{password()}}"></input>',
  template_editable: '<input type="password" autocomplete="new-password">'
});

ui.directive('uiTextareaAutoSize', function () {

  return function (scope, element, attrs) {

    if (!element.is('textarea')) return;

    function resize() {
      var diff = element.outerHeight() - element.innerHeight();
      element.css('height', 'auto').css('height', element[0].scrollHeight + diff);
    }

    element.on('focus keyup input', resize);
    setTimeout(resize);
  };
});

})();
