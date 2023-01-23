/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
      var languages = [ui.getPreferredLocale()];
      Array.prototype.push.apply(languages, window.navigator.languages);
      phoneInput.navigatorCountries = _.uniq(languages
        .map(function (language) { return language.split("-")[1]; })
        .filter(function (country) { return country; })
        .map(function (country) { return country.toLowerCase(); }))
        || phoneInput.getFallbackCountries();
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

  getOptions: function (iti) {
    return iti.options
      || _.find(iti, function (attr) { return (attr || {}).allowDropdown !== undefined })
      || {};
  },

  getDefaultOptions: function () {
    if (_.isEmpty(phoneInput.defaultOptions)) {
      var input = $("<input>").attr("type","hidden");
      input.appendTo("body");
      try {
        phoneInput.defaultOptions = phoneInput.getOptions(window.intlTelInput(input[0]));
        _.extend(phoneInput.defaultOptions, {validNumberTypes: []});
      } finally {
        input.remove();
      }
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
    if (input && input.is(":focus") && !input.hasClass("phone-input-readonly")
        || phoneInput.isValidNumber(iti, value)) {
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
    var country = phoneInput.getOptions(iti).initialCountry || "";
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
      if (/fixed|landline|fax/i.test(_.dasherize((scope.field || {}).name))) {
        options.placeholderNumberType = "FIXED_LINE";
      }
      phoneInput.applyOptions(options, scope.field.widgetAttrs);
      if (options.validNumberTypes) {
       var numberTypeMap = phoneInput.getNumberTypeMap();
        validNumberTypes = options.validNumberTypes.map(function (e) { return numberTypeMap[e] });
      }
      iti = window.intlTelInput(input[0], options);

      var flagsContainer = iti.flagsContainer
        || _.find(iti, function (attr) { return (attr || {}).className === "iti__flag-container" });
      $(flagsContainer).children("div").first().addClass("secondary-focus");
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

      if (!value) {
        return;
      }

      input.val(value);

      var options = {
        utilsScript: "lib/intl-tel-input/js/utils.js",
        initialCountry: phoneInput.getNavigatorCountry(),
        localizedCountries: phoneInput.getLocalizedCountries(),
        nationalMode: false,
        allowDropdown: false
      };
      phoneInput.applyOptions(options, scope.field.widgetAttrs);
      iti = window.intlTelInput(input[0], options);

      iti.promise.then(function () {
        var itiElem = element.find(".iti").first();

        adjustWidth = function () {
          span.text(input.val());
          itiElem.css("max-width", span.width() + 35);
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
  template_readonly: '<div ng-show="text"></span><input type="button" ng-click="onClick()" title="{{numberType}}" class="phone-input-readonly"><span class="hidden phone" style="white-space: nowrap"></div>'
});

})();
