/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
			var scale = field.scale || 2,
				num = +(value);
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

})();
