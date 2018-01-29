/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

	ui.formatters = {

		"integer": function(field, value) {
			return value;
		},

		"decimal": function(field, value) {
			var scale = (field.widgetAttrs||{}).scale || field.scale || 2;
			var num = +(value);
			if ((value === null || value === undefined) && !field.defaultValue) {
				return value;
			}
			if (num === 0 || num) {
				return num.toFixed(scale);
			}
			return value;
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
			return value ? value[field.targetName] : "";
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

	ui.formatters.$fmt = function (scope, fieldName, fieldValue) {
		var value = arguments.length === 2 ? (scope.record || {})[fieldName] : fieldValue;
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
			return formatter(field, value);
		}
		return value;
	};

})();
