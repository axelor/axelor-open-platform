/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

ui.formInput('JsonField', 'String', {
	showTitle: false,
	link: function (scope, element, attrs, model) {
		var field = scope.field;
		var jsonFields = field.jsonFields || [];

		var defaultValues = {};
		var parentUnwatch = null;
		var selfUnwatch = null;

		scope.formPath = scope.formPath ? scope.formPath + "." + field.name : field.name;
		scope.record = {};

		function getDefaultValues() {
			jsonFields.forEach(function (item) {
				if (item.defaultValue === undefined) return;
				var value = item.defaultValue;
				switch(item.type) {
				case 'integer':
					value = +(value);
					break;
				case 'date':
				case 'datetime':
					value = value === 'now' ? new Date() : moment(value).toDate();
					break;
				}
				defaultValues[item.name] = value;
			});
			return angular.copy(defaultValues);
		}

		function unwatchParent() {
			if (parentUnwatch) {
				parentUnwatch();
				parentUnwatch = null;
			}
		}

		function unwatchSelf() {
			if (selfUnwatch) {
				selfUnwatch();
				selfUnwatch = null;
			}
		}

		function watchParent() {
			unwatchParent();
			parentUnwatch = scope.$watch('$parent.record.' + field.name, function (value, old) {
				if (value === old) return;
				onRender();
			});
		}

		function watchSelf() {
			unwatchSelf();
			selfUnwatch = scope.$watch('record', function (record, old) {
				if (record === old || angular.equals(record, defaultValues)) return;
				onUpdate();
			}, true);
		}

		function onUpdate() {
			var rec = null;
			_.each(scope.record, function (v, k) {
				if (k.indexOf('$') === 0 || v === null || v === undefined) return;
				if (rec === null) {
					rec = {};
				}
				rec[k] = v;
			});
			unwatchParent();
			if (scope.$parent.record[field.name] || rec) {
				scope.$parent.record[field.name] = rec ? angular.toJson(rec) : rec;
			}
			watchParent();
		}

		function onRender() {
			var value = scope.$parent.record[field.name];
			unwatchSelf();
			scope.record = value ? angular.fromJson(value) : getDefaultValues();
			watchSelf();
		}

		scope.$on('on:new', onRender);
		scope.$on('on:edit', function () {
			if (scope.viewType === 'form') onRender();
		});

		watchParent();
	}
});

})();
