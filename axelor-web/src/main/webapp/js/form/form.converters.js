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
(function () {

  "use strict";

  var ui = angular.module('axelor.ui');

  var getFirstBrowserLanguage = function () {
    var nav = window.navigator,
      browserLanguagePropertyKeys = ['language', 'browserLanguage', 'systemLanguage', 'userLanguage'],
      i,
      language;

    // support for HTML 5.1 "navigator.languages"
    if (Array.isArray(nav.languages)) {
      for (i = 0; i < nav.languages.length; i++) {
        language = nav.languages[i];
        if (language && language.length) {
          return language;
        }
      }
    }

    // support for other well known properties in browsers
    for (i = 0; i < browserLanguagePropertyKeys.length; i++) {
      language = nav[browserLanguagePropertyKeys[i]];
      if (language && language.length) {
        return language;
      }
    }

    return null;
  };

  function getBrowserLocale() {
    return getFirstBrowserLanguage() || axelor.config['user.lang'] || 'en';
  }

  // Gets preferred locale based on user language and browser locale.
  function getPreferredLocale() {
    var userLanguage = (axelor.config["user.lang"] || "").replace("_", "-") || "";
    if (userLanguage.indexOf("-") >= 0) {
      return userLanguage;
    }
    return (
      _.find(window.navigator.languages, function (language) {
        return (
          !userLanguage || language.indexOf("-") >= 0 && language.split('-')[0] === userLanguage
        );
      }) || userLanguage || getBrowserLocale()
    );
  }

  function addCurrency(value, symbol) {
    if (value && symbol) {
      var val = '' + value;
      var lang = getPreferredLocale().split(/-|_/)[0];
      if (lang === 'fr') {
        return _.endsWith(val, symbol) ? val : val + ' ' + symbol;
      }
      return _.startsWith(val, symbol) ? val : symbol + val;
    }
    return value;
  }

  function canSetNested(record, name) {
    if (record && name && name in record) {
      return true;
    }
    if (name) {
      var path = name.split('.');
      var val = record || {};
      var idx = 0;
      while (idx < path.length - 1) {
        val = val[path[idx++]];
        if (!val) return false;
      }
    }
    return true;
  }

  function findNested(record, name) {
    if (record && name && name in record) {
      return record[name] === undefined ? null : record[name];
    }
    if (name) {
      var path = name.split('.');
      var val = record || {};
      var idx = 0;
      while (val && idx < path.length) {
        if (_.isString(val)) {
          val = fromJsonOrEmpty(val);
        }
        val = val[path[idx++]];
      }
      if (idx === path.length) {
        return val;
      }
    }
    return undefined;
  }

  function fromJsonOrEmpty(json) {
    try {
      return angular.fromJson(json);
    } catch (e) {
      return {};
    }
  }

  function setNested(record, name, value) {
    if (!record || !name) return record;
    var path = name.split('.');
    var nested = record;
    var idx = -1;
    while (++idx < path.length) {
      var key = path[idx];
      if (idx !== path.length - 1) {
        nested = nested[key] || (nested[key] = {});
      } else {
        nested[key] = value;
      }
    }
    return record;
  }

  function getNestedTrKey(nameField) {
    if (!nameField) {
      return undefined;
    }
    var names = nameField.split('.');
    names.push('$t:' + names.pop());
    return names.join('.');
  }

  // override angular.js currency filter
  ui.filter('currency', function() {
    return function(value, currency, fractionSize, currencyDisplay) {
      if (_.isBlank(value) || _.isBlank(currency)) {
        return value;
      }
      if (isNaN(value)) {
        return addCurrency(value, currency);
      }
      var options = {
        style : 'currency',
        currency : currency,
      };
      if (fractionSize !== undefined && fractionSize != null) {
        options.minimumFractionDigits = fractionSize;
        options.maximumFractionDigits = fractionSize;
      }
      if (currencyDisplay !== undefined && currencyDisplay != null) {
        options.currencyDisplay = currencyDisplay
      }
      try {
        return new Intl.NumberFormat(getPreferredLocale(), options).format(value);
      } catch (e) {
        // Fall back to adding currency symbol
        return addCurrency(value, currency);
      }
    };
  });

  // add percent filter
  ui.filter('percent', function() {
    return function(value, fractionSize) {
      if (_.isBlank(value)) {
        return value;
      }
      if (isNaN(value)) {
        return value + '%';
      }
      var options = {
        style : 'percent'
      };
      if (fractionSize !== undefined && fractionSize !== null) {
        options.minimumFractionDigits = fractionSize;
        options.maximumFractionDigits = fractionSize;
      } else {
        options.maximumFractionDigits = 1;
      }
      return new Intl.NumberFormat(getPreferredLocale(), options).format(value);
    };
  });

  // override angular.js number filter
  ui.filter('number', function() {
    return function(value, fractionSize) {
      return formatNumber(null, value, fractionSize);
    };
  });

  // override angular.js date filter
  ui.filter('date', function() {
    return function(value, format) {
      if (!value || !value.match(/\d{4,}\D\d{2}\D\d{2}/)) {
        return value;
      }
      if (format === undefined || format == null) {
        return value && value.length > 10 ? formatDateTime(null, value) : formatDate(value);
      }
      return moment(value).locale(getPreferredLocale()).format(format);
    };
  });

  function formatNumber(field, value, scale) {
    var num = +(value);
    if ((value === null || value === undefined) && (!field || !field.defaultValue)) {
      return value;
    }
    if (num === 0 || num) {
      return num.toLocaleString(getPreferredLocale(), {
        minimumFractionDigits: scale,
        maximumFractionDigits: scale
      });
    }
    return value;
  }

  function formatDate(value) {
    var format = arguments.length > 1 ? arguments[1] : ui.dateFormat;
    return value ? moment(value).format(format) : "";
  }

  function formatTime(field, value) {
    var format = arguments.length > 2 ? arguments[2] : ui.getTimeFormat(field);
    return value ? moment(value, [moment.ISO_8601, 'HH:mm:ss']).format(format) : "";
  }

  function formatDateTime(field, value) {
    var format = arguments.length > 2 ? arguments[2] : ui.getDateTimeFormat(field);
    return value ? moment(value).format(format) : "";
  }

  ui.findNested = findNested;
  ui.setNested = setNested;
  ui.canSetNested = canSetNested;
  ui.getNestedTrKey = getNestedTrKey;
  ui.getPreferredLocale = getPreferredLocale;

  var dateFormat = 'DD/MM/YYYY';
  $("body").on("app:config-fetched", function () {
    var mm = moment().clone();
    mm.locale(getPreferredLocale());
    dateFormat = (mm.localeData()._longDateFormat.L || dateFormat)
      .replace(/\u200f/g, '') // ar
      .replace(/YYYY年MMMD日/g, 'YYYY-MM-DD') // zh-tw
      .replace(/MMM/g, 'MM') // Don't support MMM
      .replace(/\bD\b/g, 'DD') // D -> DD
      .replace(/\bM\b/g, 'MM'); // M -> MM
  });

  Object.defineProperty(ui, 'dateFormat', {
    get: function () {
      return dateFormat;
    }
  });

  Object.defineProperty(ui, 'dateTimeFormat', {
    get: function () {
      return this.dateFormat + ' ' + ui.getTimeFormat();
    }
  });

  ui.getTimeFormat = function (field) {
    var format = "HH:mm";
    if ((field || {}).seconds) {
      format += ":ss";
    }
    return format;
  }

  ui.getDateTimeFormat = function (field) {
    return this.dateFormat + ' ' + ui.getTimeFormat(field);
  }

  ui.formatters = {

    "string": function(field, value, context) {
      if (field.translatable && value && context) {
        var key = '$t:' + field.name;
        return context[key] || value;
      }
      return value;
    },

    "integer": function(field, value) {
      return formatNumber(field, value);
    },

    "decimal": function(field, value, context) {
      var scale = (field.widgetAttrs||{}).scale || field.scale || 2;
      var currency = (field.widgetAttrs||{}).currency || field.currency;

      var text = formatNumber(field, value, scale);
      if (text && currency) {
        text = addCurrency(text, findNested(context, currency));
      }
      return text;
    },

    "boolean": function(field, value) {
      return value;
    },

    "duration": function(field, value) {
      return ui.formatDuration(field, value);
    },

    "date": function(field, value) {
      return formatDate(value);
    },

    "time": function(field, value) {
      return formatTime(field, value);
    },

    "datetime": function(field, value) {
      return formatDateTime(field, value);
    },

    "many-to-one": function(field, value) {
      if (!value) return "";
      if (!field.targetName) return value.name || value.code || value.id || "";
      var trKey = '$t:' + field.targetName;
      return value[trKey] || value[field.targetName];
    },

    "one-to-many": function(field, value) {
      return value ? '(' + value.length + ')' : "";
    },

    "many-to-many": function(field, value) {
      return value ? '(' + value.length + ')' : "";
    },

    "selection": function(field, value) {
      var cmp = ["integer", "long"].indexOf(field.type) < 0 ? _.isEqual : function(a, b) { return a == b ; };
      var res = _.find(field.selectionList, function(item){
        return cmp(item.value, value);
      }) || {};
      return res.title;
    }
  };

  ui.formatters["enum"] = ui.formatters.selection;

  function findField(scope, name) {
    if (scope.field && scope.field.target) {
      return ((scope.field.viewer||{}).fields||{})[name]
        || ((scope.field.editor||{}).fields||{})[name];
    }
    return (scope.viewItems || scope.fields || {})[name];
  }

  ui.formatters.$image = function (scope, fieldName, imageName) {
    var record = scope.record || {};
    var model = scope._model;

    if (fieldName) {
      var field = (scope.fields||{})[fieldName];
      if (field && field.target) {
        record = record[fieldName] || {};
        model = field.target;
      }
    }

    var v = record.version || record.$version || 0;
    var n = record.id;
    if (n > 0) {
      return "ws/rest/" + model + "/" + n + "/" + imageName + "/download?image=true&v=" + v
        + "&parentId=" + n + "&parentModel=" + model;
    }
    return "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
  };

  ui.formatters.$fmt = function (scope, fieldName, fieldValue, record) {
    var context = record || scope.record || {};
    var value = arguments.length === 2 ? findNested(context, fieldName) : fieldValue;
    if (value === undefined || value === null) {
      return "";
    }
    var field = findField(scope, fieldName) || scope.field;
    if (!field) {
      return value;
    }
    var type = field.selection ? "selection" : field.serverType || field.type;
    var formatter = ui.formatters[type];
    if (formatter) {
      return formatter(field, value, context);
    }
    return value;
  };

  function findForm(elem) {
    var formElement = elem;

    if (formElement.is('form')) {
      return formElement;
    }

    formElement = elem.data('$editorForm') || elem.parents('form:first');
    if (!formElement || !formElement.get(0)) { // toolbar button
      formElement = elem.parents('.form-view:first').find('form:first');
    }
    if (formElement.length === 0) {
      formElement = elem;
    }
    return formElement;
  }
  ui.findForm = findForm;

  // Returns a copy of list sorted in lexicographic order, running each value through iteratee
  function sortBy(list, iteratee, descending) {
    var locale = getPreferredLocale();
    var localeCompare = Intl.Collator(locale).compare;
    var result = (list || []).slice();

    function toKey(key) { return (key || '').toLocaleString(locale).toLocaleLowerCase(locale); }
    var getKey = _.isFunction(iteratee) ?
      function (value) { return toKey(iteratee(value)); } :
      function (value) { return toKey(value[iteratee]); };

    return result.sort(descending ? function (a, b) {
      return localeCompare(getKey(b), getKey(a));
    } : function (a, b) {
      return localeCompare(getKey(a), getKey(b));
    });
  }

  axelor.sortBy = sortBy;
})();
