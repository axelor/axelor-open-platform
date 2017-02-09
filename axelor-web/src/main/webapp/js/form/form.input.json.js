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
		var jsonFix = {};

		var defaultValues = {};
		var parentUnwatch = null;
		var selfUnwatch = null;

		scope.formPath = scope.formPath ? scope.formPath + "." + field.name : field.name;
		scope.record = {};
		
		jsonFields.forEach(function (item) {
			if (item.target === 'com.axelor.meta.db.MetaJsonRecord' &&
				item.targetName && item.targetName.indexOf('attrs.') === 0) {
				jsonFix[item.name] = function (v) {
					if (v) {
						v[item.targetName.substring(6)] = v[item.targetName];
					}
					return v;
				}
			}
		});

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
		
		function format(name, value) {
			var func = jsonFix[name];
			return func ? func(value) : value;
		}

		function onUpdate() {
			var rec = null;
			_.each(scope.record, function (v, k) {
				if (k.indexOf('$') === 0 || v === null || v === undefined) return;
				if (_.isArray(v)) {
					if (v.length == 0) return;
					v = v.map(function (x) {
						return x.id ? { id: x.id } : x;
					});
				}
				if (rec === null) {
					rec = {};
				}
				rec[k] = format(k, v);
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

		// hide parent panel if no jsonFields defined
		scope.$evalAsync(function () {
			var parent = scope.$parent.field || {};
			if (parent.type === 'panel' && _.size(parent.items) === 1 && _.isEmpty(field.jsonFields)) {
				scope.$parent.attr('hidden', true);
			}
		});
	}
});

ui.formInput('JsonRaw', 'String', {
	showTitle: false,
	link: function (scope, element, attrs, model) {
		
		scope.placeHolderKey = _t('name');
		scope.placeHolderVal = _t('value');

		scope.items = [];

		scope.onAdd = function () {
			var last = _.last(scope.items);
			if (last && !(_.trim(last.name) && _.trim(last.value))) return;
			scope.items.push({});
		};

		scope.onRemove = function (index) {
			if (scope.items.length > 0) {
				scope.items.splice(index, 1);
			}
		};

		var unwatch = null;

		function doWatch () {
			if (unwatch) {
				unwatch();
			}
			unwatch = scope.$watch('items', function (items, old) {
				if (items === old) return;
				var record = null;
				_.each(items, function (item) {
					if (!_.trim(item.name) || !_.trim(item.value)) return;
					if (record === null) {
						record = {};
					}
					record[item.name] = item.value;
				});
				model.$setViewValue(record ? angular.toJson(record) : null);
			}, true);
		}

		model.$render = function () {
			var value = model.$viewValue;
			if (value) {
				value = angular.fromJson(value);
			} else {
				value = {};
			}
			scope.items = _.map(_.keys(value), function (name) {
				return { name: name, value: value[name] || '' };
			});
			doWatch();
		};
	},
	template_readonly:
		"<div class='json-editor'>" +
			"<table class='form-layout'>" +
				"<tr ng-repeat='(i, item) in items'>" +
					"<td class='form-label'>" +
						"<strong class='display-text'>{{item.name}}</strong>:" +
					"</td>" +
					"<td class='form-item'>" +
						"<span class='display-text'>{{item.value}}</span>" +
					"</td>" +
				"</tr>" +
			"</table>" +
		"</div>",
	template_editable:
		"<div class='json-editor'>" +
			"<table class='form-layout'>" +
				"<tr ng-repeat='(i, item) in items'>" +
					"<td class='form-item'><span class='form-item-container'>" +
						"<input type='text' placeholder='{{placeHolderKey}}' ng-model='item.name'></span>" +
					"</td>" +
					"<td class='form-item'><span class='form-item-container'>" +
						"<input type='text' placeholder='{{placeHolderVal}}' ng-model='item.value'></span>" +
					"</td>" +
					"<td><a href='' ng-click='onRemove(i)'><i class='fa fa-minus'></i></a></td>" +
				"</tr>" +
			"</table>" +
			"<a href='' ng-click='onAdd()'><i class='fa fa-plus'></i></a>" +
		"</div>"
});

ui.formInput('JsonRefSelect', {

	css: 'multi-object-select',

	controller: ['$scope', 'ViewService', function($scope, ViewService) {

		$scope.createElement = function(id, name, selectionList) {

			var elemGroup = $('<div ui-group ui-table-layout cols="2" x-widths="150,*"></div>');
			var elemSelect = $('<input ui-select showTitle="false">')
				.attr("name", name + "$model")
				.attr("x-for-widget", id)
				.attr("ng-model", "record." + name + ".model");

			var elemSelects = $('<div></div>').attr('ng-switch', "record." + name + ".model");
			var elemItems = _.map(selectionList, function(s) {
				return $('<input ui-json-ref-item ng-switch-when="' + s.value +'">')
					.attr('ng-model', 'record.' + name)
					.attr('name', name)
					.attr('x-target', s.value);
			});

			elemGroup
				.append($('<div></div>').append(elemSelect))
				.append(elemSelects.append(elemItems));

			return ViewService.compile(elemGroup)($scope);
		};
	}],

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var name = scope.field.name;
		var selectionList = scope.field.selectionList;

		scope.fieldsCache = {};

		scope.refFireEvent = function (name) {
			var handler = scope.$events[name];
			if (handler) {
				return handler();
			}
		};

		var elem = scope.createElement(element.attr('id'), name, selectionList);
		setTimeout(function() {
			element.append(elem);
		});

		scope.$watch("record." + name + ".model", function (value, old) {
			if (value === old || old === undefined) return;
			if (scope.record && scope.record[name]) {
				scope.record[name] = _.pick(scope.record[name], 'model');
				if (!scope.record[name].model) {
					delete scope.record[name];
				}
			}
		});
	},
	template_editable: null,
	template_readonly: null
});

ui.formInput('JsonRefItem', 'ManyToOne', {

	showTitle: false,

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		if (scope.field.targetName) {
			return this._link.apply(this, arguments);
		}

		var self = this;
		var target = element.attr('x-target');
		var data = (_.findWhere(scope.$parent.field.selectionList, {value: target})||{}).data || {};
		
		function doLink(fields) {
			var name = false,
				search = [];

			_.each(fields, function(f) {
				if (f.nameColumn) name = f.name;
				if (f.name === "name") search.push("name");
				if (f.name === "code") search.push("code");
			});

			if (!name && _.contains(search, "name")) {
				name = "name";
			}

			_.extend(scope.field, {
				target: scope._model,
				targetName: name,
				targetSearch: search,
				domain: data.domain
			});

			self._link(scope, element, attrs, model);
		}

		if (scope.fieldsCache[scope._model]) {
			doLink(scope.fieldsCache[scope._model]);
		} else {
			scope.loadFields().success(function (fields) {
				scope.fieldsCache[scope._model] = fields;
				doLink(fields);
			});
		}
	},

	_link: function(scope, element, attrs, model) {
		var name = element.attr('name');
		
		scope.getValue = function () {
			return scope.record[name];
		}
		
		var __setValue = scope.setValue;
		
		scope.setValue = function (value) {
			var val = _.pick(scope.record[name], 'model');
			val = _.extend(val, value);
			__setValue.call(scope, val);
		}

		function doSelect() {
			var value = (scope.record || {})[name];
			scope.select(value);
		}

		scope.$watch("record", doSelect);
	}
});

})();
