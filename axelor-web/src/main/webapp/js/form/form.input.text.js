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
 * The Phone input widget.
 */
var phoneInput = {
  localizedCountries: {},
  navigatorCountries: [],
  fallbackCountries: [],
  numberTypes: [],
  numberTypeMap: {},
  validationErrors: [],
  defaultOptions: {},
  autoCountry: "",

  getLocalizedCountries: function () {
    if (_.isEmpty(phoneInput.localizedCountries)) {
      window.intlTelInputGlobals.getCountryData().forEach(function (country) {
        var match = country.name.match(/(.+)\s+\((.+)\)$/);
        var name;
        if (match) {
          name = match[1];
          if (_.endsWith(match[2], ")")) {
            name = name.match(/(.*)\s+\(/)[1];
          }
        } else {
          name = country.name;
        }
        phoneInput.localizedCountries[country.iso2] = _t(name);
      });
    }
    return phoneInput.localizedCountries;
  },

  getNavigatorCountries: function () {
    if (!phoneInput.navigatorCountries.length) {
      phoneInput.navigatorCountries = _.uniq(window.navigator.languages
        .map(function (language) { return language.split("-")[1]; })
        .filter(function (language) { return language; })) || [phoneInput.getFallbackCountries()[0]]
    }
    return phoneInput.navigatorCountries;
  },

  getNavigatorCountry: function () {
    return phoneInput.getNavigatorCountries()[0];
  },

  getFallbackCountries: function () {
    if (!phoneInput.fallbackCountries.length) {
      phoneInput.fallbackCountries = ["us", "fr"];
    }
    return phoneInput.fallbackCountries;
  },

  getNumberTypes: function () {
    if (!phoneInput.numberTypes.length) {
      phoneInput.numberTypes = Object.values(phoneInput.getNumberTypeMap());
    }
    return phoneInput.numberTypes;
  },

  getNumberTypeMap: function () {
    if (_.isEmpty(phoneInput.numberTypeMap)) {
      phoneInput.numberTypeMap = {
        "FIXED_LINE": _t("Fixed line"),
        "MOBILE": _t("Mobile"),
        "FIXED_LINE_OR_MOBILE": _t("Fixed line or mobile"),
        "TOLL_FREE": _t("Toll free"),
        "PREMIUM_RATE": _t("Premium rate"),
        "SHARED_COST": _t("Shared cost"),
        "VOIP": _t("VoIP"),
        "PERSONAL_NUMBER": _t("Personal number"),
        "PAGER": _t("Pager"),
        "UAN": _t("UAN"),
        "VOICEMAIL": _t("Voicemail")
      };
    }
    return phoneInput.numberTypeMap;
  },

  getValidationErrors: function () {
    if (!phoneInput.validationErrors.length) {
      phoneInput.validationErrors = [
        _t("Invalid number"),
        _t("Invalid country code"),
        _t("Too short"),
        _t("Too long"),
        _t("Invalid number"),
        _t("Invalid length")
      ];
    }
    return phoneInput.validationErrors;
  },

  getDefaultOptions: function () {
    if (_.isEmpty(phoneInput.defaultOptions)) {
      var input = $("<input>").attr("type","hidden");
      input.appendTo("body");
      phoneInput.defaultOptions = window.intlTelInput(input[0]).options;
      _.extend(phoneInput.defaultOptions, {validNumberTypes: []});
      input.remove();
    }
    return phoneInput.defaultOptions;
  },

  setNumberType: function (scope, iti) {
    scope.numberType = !phoneInput.isEmptyNumber(iti) ? phoneInput.getNumberTypes()[iti.getNumberType()] || _t("Unknown") : "";
  },

  isValidNumber: function (iti, value) {
    return window.intlTelInputGlobals.isValidNumber(value || iti.getNumber(),
      iti.getSelectedCountryData().iso2);
  },

  isEmptyNumber: function (iti, value) {
    var val = (value || iti.getNumber() || "").trim();
    return !val || val === "+" || val === "+" + (iti.getSelectedCountryData() || {}).dialCode
  },

  detectCountry: function (iti, value, input) {
    if (input && input.is(":focus") || phoneInput.isValidNumber(iti)) {
      return;
    }

    // Detect from placeholder.
    if (!value && input) {
      var placeholder = input.attr("x-placeholder");
      if (placeholder) {
        iti.setNumber(placeholder);
        iti.setNumber("");
        return;
      }
    }

    var countries = _.uniq(phoneInput.getNavigatorCountries().concat(phoneInput.getFallbackCountries()));
    var validCountry = countries.find(function (country) {
      return window.intlTelInputGlobals.isValidNumber(value, country);
    });

    if (validCountry) {
      iti.setCountry(validCountry);
      iti.setNumber(value);
    } else if (input) {
      input.val(value);
    }
  },

  setInitialCountry: function (iti) {
    var country = iti.options.initialCountry;
    if (country === "auto") {
      country = phoneInput.autoCountry || phoneInput.getNavigatorCountry();
    }
    iti.setCountry(country);
  },

  applyOptions: function (options, attrs) {
    if (!_.isEmpty(attrs)) {
      function fixJson(value) {
        value = value.trim();
        if (!_.startsWith(value, "{")) {
          value = "{" + value + "}";
        }
        return value.replace(/(?:['"])?(\w+)(?:['"])?:\s*(?:['"])?([^'"]+)(?:['"])?/g, '"$1": "$2"');
      }

      var specials = {
        customPlaceholder: function (value) {
          var customPlaceholders = JSON.parse(fixJson(value));
          options.customPlaceholder = function (selectedCountryPlaceholder, selectedCountryData) {
            return customPlaceholders[selectedCountryData.iso2] || selectedCountryPlaceholder;
          };
        },
        initialCountry: function (value) {
          if (value === "auto") {
            if (window.intlTelInputGlobals.geoIpLookup) {
              options.geoIpLookup = function (success) {
                var successHook = function (country) {
                  phoneInput.autoCountry = country || phoneInput.getNavigatorCountry();
                  success(phoneInput.autoCountry);
                }
                window.intlTelInputGlobals.geoIpLookup(successHook);
              };
            } else {
              console.error(_.sprintf('(Phone widget) '
                + 'initialCountry="%s" requires intlTelInputGlobals.geoIpLookup.', value));
            }
          }
          options.initialCountry = value;
        }
      }

      var defaults = phoneInput.getDefaultOptions();
      _.each(attrs, function (value, key) {
        var special = specials[key];
        if (special) {
          special(value);
          return;
        }

        var initial = defaults[key];
        if (_.isArray(initial)) {
          if (_.isString(value)) {
            try {
              value = JSON.parse(value);
            } catch (e) {
            }
            if (!_.isArray(value)) {
              if (value.indexOf(" ") >= 0) {
                value = value.split(/\s+/);
              } else {
                value = [value];
              }
            }
          }
        } else if (_.isObject(initial)) {
          if (_.isString(value)) {
            try {
              value = JSON.parse(fixJson(value));
            } catch (e) {
            }
          }
        } else if (_.isBoolean(initial)) {
          value = _.toBoolean(value);
        }
        options[key] = value;
      });
    }

    if (!_.isEmpty(options.onlyCountries) && options.initialCountry) {
      var initialCountry = options.initialCountry.toLowerCase();
      if (!_.chain(options.onlyCountries)
          .find(function (e) { return e.toLowerCase() === initialCountry }).value()) {
        options.initialCountry = options.onlyCountries[0];
      }
    }

    if (!options.customPlaceholder && window.intlTelInputGlobals.customPlaceholder) {
      options.customPlaceholder = window.intlTelInputGlobals.customPlaceholder;
    }
  }
};

window.intlTelInputGlobals.isValidNumber = function (value, country) {
  return !window.intlTelInputUtils || window.intlTelInputUtils.isValidNumber(value, country);
}

ui.formInput('Phone', 'String', {
  css: 'phone-item',
  init: function (scope) {
    this._super.apply(this, arguments);

    scope.onClick = function() {
      window.open("tel:" + scope.getValue());
    }
  },
  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);
    var iti = null;
    var input = element.find("input").first();
    var validNumberTypes = [];

    if (scope.field.pattern) {
      input.attr("pattern", scope.field.pattern);
    }

    if (scope.field.placeholder) {
      input.attr("placeholder", scope.field.placeholder);
      input.attr("x-placeholder", scope.field.placeholder);
    }

    function validateNumber(initial) {
      var empty = phoneInput.isEmptyNumber(iti);
      if (empty || phoneInput.isValidNumber(iti)) {
        var validationError = null;
        if (!empty && !_.isEmpty(validNumberTypes) && validNumberTypes.indexOf(scope.numberType) < 0) {
          validationError = _t("Invalid number type");
        }
        scope.validationError = validationError;
      } else {
        var errors = phoneInput.getValidationErrors();
        scope.validationError = errors[iti.getValidationError()] || errors[0];
      }
      if (!initial) {
        model.$setViewValue(iti.getNumber() || null);
      }
    }

    // Automatically format
    input.on("change keyup blur", function () {
      var value = input.val();
      if (phoneInput.isValidNumber(iti, value)) {
        iti.setNumber(value || "");
      }
      phoneInput.setNumberType(scope, iti);
      validateNumber();
      scope.$applyAsync();
    });

    // onChange
    input.on("change", function () {
      var value = iti ? iti.getNumber() : input.val();
      scope.setValue(value || null, true);
    });

    var countryChange = false;
    input.on("open:countrydropdown", function () {
      countryChange = true;
    });
    input.on("countrychange", function () {
      if (!countryChange) return;
      countryChange = false
      phoneInput.setNumberType(scope, iti);
      validateNumber();
      scope.$applyAsync();
    });

    var validationElement = element.find(".phone-validation");
    var validationType = (window.intlTelInputGlobals.validationType || "WARNING").toUpperCase();
    switch (validationType) {
      case "HIDDEN":
        validationElement.addClass("hidden");
        break;
      case "WARNING":
        validationElement.addClass("hilite-warning-text");
        break;
      case "ERROR":
        validationElement.addClass("hilite-error-text");
        scope.$watch("validationError", function (validationError) {
          model.$setValidity("invalid", !validationError);
        })
        break;
      default:
        console.error(_.sprintf("(Phone widget) Unknown phone validation type: %s", validationType));
    }

    scope.$render_editable = function () {
      var value = scope.getValue();

      if (iti) {
        if (!input.is(":focus")) {
          phoneInput.setInitialCountry(iti);
          iti.setNumber(value || "");
        } else if (phoneInput.isValidNumber(iti, value)) {
          iti.setNumber(value || "");
        }
        phoneInput.detectCountry(iti, value, input);
        phoneInput.setNumberType(scope, iti);
        validateNumber(true);
        return;
      }

      input.val(value);

      var options = {
        utilsScript: "lib/intl-tel-input/js/utils.js",
        initialCountry: phoneInput.getNavigatorCountry(),
        preferredCountries: phoneInput.getNavigatorCountries(),
        localizedCountries: phoneInput.getLocalizedCountries(),
        nationalMode: false
      };
      phoneInput.applyOptions(options, scope.field.widgetAttrs);
      if (options.validNumberTypes) {
       var numberTypeMap = phoneInput.getNumberTypeMap();
        validNumberTypes = options.validNumberTypes.map(function (e) { return numberTypeMap[e] });
      }
      iti = window.intlTelInput(input[0], options);

      $(iti.flagsContainer).children("div").first().addClass("secondary-focus");
      iti.promise.then(function () {
        phoneInput.detectCountry(iti, value, input);
        phoneInput.setNumberType(scope, iti);
        validateNumber(true);
        element.find(".iti__country-list").addClass("dropdown");
      });
    };
  },
  link_readonly: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);
    var iti = null;
    var input = element.find("input").first();
    var span = element.find("span.phone").first();
    var adjustWidth = angular.noop;

    scope.$render_readonly = function () {
      var value = scope.getValue();

      if (iti) {
        phoneInput.setInitialCountry(iti);
        iti.setNumber(value || "");
        phoneInput.detectCountry(iti, value, input);
        phoneInput.setNumberType(scope, iti);
        adjustWidth();
        return;
      }

      input.val(value);

      var options = {
        utilsScript: "lib/intl-tel-input/js/utils.js",
        initialCountry: phoneInput.getNavigatorCountry(),
        nationalMode: false,
        allowDropdown: false
      };
      phoneInput.applyOptions(options, scope.field.widgetAttrs);
      iti = window.intlTelInput(input[0], options);

      iti.promise.then(function () {
        var itiElem = element.find(".iti").first();

        adjustWidth = function () {
          span.text(input.val());
          itiElem.width(span.width() + 32);
        }

        scope.isVisible = function () {
          return itiElem.is(":visible");
        }

        scope.$watch("isVisible()", function (visible) {
          if (visible) {
            adjustWidth();
          }
        });

        phoneInput.detectCountry(iti, value, input);
        phoneInput.setNumberType(scope, iti);
        adjustWidth();
      });
    };

    // Editor as viewer
    if (scope.getValue()) {
      scope.$render_readonly();
    }
  },
  template_editable: '<div class="input"><input type="tel" title="{{numberType}}"><span class="phone-validation" ng-show="validationError">{{validationError}}</span></div>',
  template_readonly: '<div ng-show="text"></span><input type="button" ng-click="onClick()" title="{{numberType}}" class="phone-input-readonly"><span class="hidden phone"></div>'
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
