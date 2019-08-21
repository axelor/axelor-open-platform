/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
(function () {

  "use strict";

  var ui = angular.module('axelor.ui');

  var currencySymbols = {
    en: '\u0024',
    fr: '\u20AC'
  };

  var thousandSeparator = {
    en: ',',
    fr: ' '
  };

  function addCurrency(value, symbol) {
    if (value && symbol) {
      var val = '' + value;
      if (axelor.config['user.lang'] === 'fr' ) {
        return val.endsWith(symbol) ? val : val + ' ' + symbol;
      }
      return val.startsWith(symbol) ? val : symbol + val;
    }
    return value;
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
        val = val[path[idx++]];
      }
      if (idx === path.length) {
        return val;
      }
    }
    return undefined;
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

  // override angular.js currency filter
  ui.filter('currency', function () {
    return addCurrency;
  });

  function formatNumber(field, value, scale) {
    var num = +(value);
    if ((value === null || value === undefined) && !field.defaultValue) {
      return value;
    }
    if (num === 0 || num) {
      var lang = axelor.config['user.lang'];
      var tsep = thousandSeparator[lang] || thousandSeparator.en;
      return _.numberFormat(num, scale, '.', tsep);
    }
    return value;
  }

  ui.findNested = findNested;
  ui.setNested = setNested;

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
      return value ? moment(value).format('DD/MM/YYYY') : "";
    },

    "time": function(field, value) {
      return value ? value : "";
    },

    "datetime": function(field, value) {
      return value ? moment(value).format('DD/MM/YYYY HH:mm') : "";
    },

    "many-to-one": function(field, value) {
      return value
        ? (field.targetName ? value[field.targetName] : (value.name || value.code || value.id || ""))
        : "";
    },

    "one-to-many": function(field, value) {
      return value ? '(' + value.length + ')' : "";
    },

    "many-to-many": function(field, value) {
      return value ? '(' + value.length + ')' : "";
    },

    "selection": function(field, value) {
      var cmp = field.type === "integer" ? function(a, b) { return a == b ; } : _.isEqual;
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
      return "ws/rest/" + model + "/" + n + "/" + imageName + "/download?image=true&v=" + v;
    }
    return "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
  };

  ui.formatters.$fmt = function (scope, fieldName, fieldValue, record) {
    var context = record || scope.record || {};
    var value = arguments.length === 2 ? context[fieldName] : fieldValue;
    if (value === undefined || value === null) {
      return "";
    }
    var field = findField(scope, fieldName);
    if (!field) {
      return value;
    }
    var type = field.selection ? "selection" : field.type;
    var formatter = ui.formatters[type];
    if (formatter) {
      return formatter(field, value, context);
    }
    return value;
  };

})();
