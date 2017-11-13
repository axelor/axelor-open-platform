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
(function() {

'use strict';

var ui = angular.module('axelor.ui');

var OPERATORS = {
	"="		: _t("equals"),
	"!="	: _t("not equal"),
	">="	: _t("greater or equal"),
	"<="	: _t("less or equal"),
	">" 	: _t("greater than"),
	"<" 	: _t("less than"),

	"like" 		: _t("contains"),
	"notLike"	: _t("doesn't contain"),

	"between"		: _t("in range"),
	"notBetween"	: _t("not in range"),

	"isNull"	: _t("is null"),
	"notNull" 	: _t("is not null"),

	"true"		: _t("is true"),
	"false" 	: _t("is false")
};

var OPERATORS_BY_TYPE = {
	"enum"		: ["=", "!=", "isNull", "notNull"],
	"text"		: ["like", "notLike", "isNull", "notNull"],
	"string"	: ["=", "!=", "like", "notLike", "isNull", "notNull"],
	"integer"	: ["=", "!=", ">=", "<=", ">", "<", "between", "notBetween", "isNull", "notNull"],
	"boolean"	: ["true", "false"]
};

_.each(["long", "decimal", "date", "time", "datetime"], function(type) {
	OPERATORS_BY_TYPE[type] = OPERATORS_BY_TYPE.integer;
});

_.each(["one-to-one", "many-to-one", "one-to-many", "many-to-many"], function(type) {
	OPERATORS_BY_TYPE[type] = OPERATORS_BY_TYPE.text;
});

ui.directive('uiFilterItem', function() {

	return {
		replace: true,
		require: '^uiFilterForm',
		scope: {
			fields: "=",
			filter: "="
		},
		link: function(scope, element, attrs, form) {

			function getOperators() {

				if (element.is(':hidden')) {
					return;
				}

				var filter = scope.filter || {};
				if (filter.type === undefined || !filter.field) {
					return [];
				}

				var field = scope.fields[filter.field] || {};
				var operators = filter.selectionList
					? OPERATORS_BY_TYPE["enum"] || []
					: OPERATORS_BY_TYPE[filter.type] || [];

				if (field.target && !field.targetName) {
					operators = ["isNull", "notNull"];
				}

				return _.map(operators, function(name) {
					return {
						name: name,
						title: OPERATORS[name]
					};
				});
			}

			scope.remove = function(filter) {
				form.removeFilter(filter);
			};
			
			scope.canShowSelect = function () {
				return scope.filter && scope.filter.selectionList &&
					   scope.filter.operator && !(
					   scope.filter.operator == 'isNull' ||
					   scope.filter.operator == 'notNull');
			};

			scope.canShowInput = function() {
				return scope.filter && !scope.canShowSelect() &&
					   scope.filter.operator && !(
					   scope.filter.type == 'boolean' ||
					   scope.filter.operator == 'isNull' ||
					   scope.filter.operator == 'notNull');
			};

			scope.canShowRange = function() {
				return scope.filter && (
					   scope.filter.operator === 'between' ||
					   scope.filter.operator === 'notBetween');
			};
			
			scope.getSelection = function () {
				if (!scope.canShowSelect()) return [];
				var field = (scope.fields||{})[scope.filter.field] || {};
				return field.selectionList || [];
			};

			scope.onFieldChange = function() {
				var filter = scope.filter,
					field = scope.fields[filter.field] || {};

				filter.type = field.type || 'string';
				filter.selectionList = field.selectionList;

				if (field.type === 'many-to-one' || field.type === 'one-to-one') {
					filter.targetName = field.targetName;
				} else {
					filter.targetName = null;
				}
			};

			scope.onOperatorChange = function() {
				setTimeout(function() {
					scope.$parent.$parent.$parent.doAdjust();
				});
			};

			scope.$watch('filter.field', function (value, old) {
				scope.operators = getOperators();
			});

			scope.getOptions = function () {
				var all = [];
				var data = scope.$parent.contextData || {};
				var field = data.field || {};
				_.each(scope.options, function (item) {
					var name = field.name;
					if (name && item.name === name) {
						return;
					}
					if (item.contextField && !(item.contextField === name && item.contextFieldValue === data.value)) {
						return;
					}
					all.push(item);
				});
				return all;
			};

			var unwatch = scope.$watch('fields', function(fields, old) {
				if (_.isEmpty(fields)) return;
				unwatch();
				var options = _.values(fields);
				scope.options = _.sortBy(options, 'title');
			}, true);
		},
		template:
			"<div class='flex-layout'>" +
				"<div class='flex-row'>" +
					"<div class='flex-item filter-remove'>" +
						"<a href='' ng-click='remove(filter)'><i class='fa fa-times'></i></a>" +
					"</div>" +
					"<div class='flex-item filter-inputs'>" +
						"<span>" +
							"<select ng-model='filter.field' ng-options='v.name as v.title for v in getOptions()' ng-change='onFieldChange()' class='input-medium'></select> " +
						"</span>" +
						"<span>" +
							"<select ng-model='filter.operator' ng-options='o.name as o.title for o in operators' ng-change='onOperatorChange()' class='input-medium'></select> "+
						"</span>" +
						"<span ng-show='canShowSelect()'>" +
							"<select ng-model='filter.value' class='input=medium' ng-options='o.value as o.title for o in getSelection()'></select>" +
						"</span>" +
						"<span ng-show='canShowInput()'>" +
							"<input type='text' ui-filter-input ng-model='filter.value' class='input-medium'> " +
						"</span>" +
						"<span ng-show='canShowRange()'>" +
							"<input type='text' ui-filter-input ng-model='filter.value2' class='input-medium'> " +
						"</span>" +
					"</div>" +
				"</div>" +
			"</div>"
	};
});

ui.directive('uiFilterInput', function() {

	return {
		require: '^ngModel',

		link: function(scope, element, attrs, model) {

			var picker = null;
			var pattern = /^(\d{2}\/\d{2}\/\d{4})$/;
			var isopattern = /^(\d{4}-\d{2}-\d{2}T.*)$/;

			var options = {
				dateFormat: 'dd/mm/yy',
				showButtonsPanel: false,
				showTime: false,
				showOn: null,
				onSelect: function(dateText, inst) {
					var value = picker.datepicker('getDate');
					var isValue2 = _.str.endsWith(attrs.ngModel, 'value2');

					value = isValue2 ? moment(value).endOf('day').toDate() :
						               moment(value).startOf('day').toDate();

					model.$setViewValue(value.toISOString());
				}
			};

			model.$formatters.push(function(value) {
				if (_.isDate(value)) {
					value = moment(value).format('DD/MM/YYYY');
				}
				return value;
			});

			model.$parsers.push(function(value) {
				if (/^date/.test(scope.filter.type)) {
					if (isopattern.test(value)) {
						return value;
					} else if (pattern.test(value)) {
						var isValue2 = _.str.endsWith(attrs.ngModel, 'value2');
						return isValue2 ? moment(value, 'DD/MM/YYYY').endOf('day').toDate() :
										  moment(value, 'DD/MM/YYYY').startOf('day').toDate();
					}
					return null;
				}
				return value;
			});

			model.$parsers.push(function(value) {
				var type = scope.filter.type;
				if (!(type == 'date' || type == 'datetime') || isDate(value)) {
					return value;
				}
				return toMoment(value).toDate();
			});

			function isDate(value) {
				if (value === null || value === undefined) return true;
				if (_.isDate(value)) return true;
				if (/\d+-\d+-\d+T/.test(value)) return true;
			}

			function toMoment(value) {
				var format = null;
				if (/\d+\/\d+\/\d+/.test(value)) format = 'DD/MM/YYYY';
				if (/\d+\/\d+\/\d+\s+\d+:\d+/.test(value)) format = 'DD/MM/YYYY HH:mm';
				if (format === null) {
					return moment();
				}
				return moment(value, format);
			}

			element.focus(function(e) {
				var type = scope.filter.type;
				if (!(type == 'date' || type == 'datetime')) {
					return;
				}
				picker = picker || element.datepicker(options);
				picker.datepicker('show');
			});

			element.on('$destroy', function() {
				if (picker) {
					picker.datepicker('destroy');
					picker = null;
				}
			});
		}
	};
});

ui.directive('uiFilterContext', function () {

	return {
		scope: {
			fields: '=',
			context: '='
		},
		controller: ['$scope', function ($scope) {
			$scope.field = {
				name: 'contextValue',
				evalTarget: 'context.field.target',
				evalTargetName: 'context.field.targetName',
				evalValue: 'context.value',
				evalTitle: 'context.title'
			};

			$scope.getViewDef = function () {
				return $scope.field;
			};

			$scope.remove = function () {
				var context = {};
				var fields = $scope.contextFields || [];
				if (fields.length === 1) {
					context.field = fields[0];
				}
				$scope.context = context;
			};
			
			$scope.$watch('context.field.name', function (name) {
				if (!name) {
					$scope.remove();
				}
			});

			$scope.onFields = function (fields) {
				var contextFields = {};
				for (var item in fields) {
					var field = fields[item];
					var name = field.contextField;
					if (name && fields[name] && !contextFields[name]) {
						contextFields[name] = fields[name];
					}
				}
				$scope.contextFields = _.sortBy(_.values(contextFields), 'title');
				$scope.remove();
			};
		}],
		link: function (scope, element, attrs) {
			var unwatch = scope.$watch('fields', function (fields) {
				if (_.isEmpty(fields)) return;
				unwatch();
				scope.onFields(fields);
			}, true);
		},
		template:
			"<div class='flex-layout filter-context' ng-show='contextFields.length'>" +
				"<div class='flex-row'>" +
					"<div class='flex-item filter-remove'>" +
						"<a href='' ng-click='remove()'><i class='fa fa-times'></i></a>" +
					"</div>" +
					"<div class='flex-item filter-inputs'>" +
						"<span>" +
							"<select ng-model='context.field' ng-options='v as v.title for v in contextFields'></select>" +
						"</span>" +
						"<span>" +
							"<input ui-eval-ref-select ng-model='context.value' x-field='contextValue'>" +
						"</span>" +
					"</div>" +
				"</div>" +
			"</div>"
	};
});

FilterFormCtrl.$inject = ['$scope', '$element', 'ViewService'];
function FilterFormCtrl($scope, $element, ViewService) {

	this.doInit = function(model, viewItems) {
		var context = $scope.$parent.$parent._context || {};
		return ViewService
		.getFields(model, context.jsonModel)
		.success(function(fields, jsonFields) {

			var items = {};
			var nameField = null;
			var nameFields = [];

			_.each(fields, function(field, name) {
				if (field.name === 'id' || field.name === 'version' ||
					field.name === 'archived' || field.name === 'selected') return;
				if (field.type === 'binary' || field.large) return;
				if (field.nameColumn) {
					nameField = name;
				} else if (name === "name" || name === "code") {
					nameFields.push(name);
				}
				items[name] = field;
			});

			// include json fields
			_.each(jsonFields, function (fields, prefix) {
				_.each(fields, function (field, name) {
					if (field.type === 'many-to-many') return;
					var key = prefix + '.' + name;
					key += '::' + (field.jsonType || 'text');
					items[key] = _.extend({}, field, {
						name: key,
						title: (field.title || field.autoTitle) + " (" + items[prefix].title + ")"
					});
				});
				// don't search parent
				delete items[prefix];
			});

			nameField = nameField || _.first(nameFields);

			_.each(viewItems, function (item) {
				if (item.hidden) {
					delete items[item.name];
				} else {
					items[item.name] = item;
				}
			});

			$scope.fields = items;
			$scope.$parent.fields = $scope.fields;
			$scope.$parent.nameField = nameField || ($scope.fields.name ? 'name' : null);
		});
	};

	$scope.fields = {};
	$scope.filters = [{ $new: true }];
	$scope.operator = 'and';
	$scope.showArchived = false;

	var handler = $scope.$parent.handler;
	if (handler && handler._dataSource) {
		$scope.showArchived = handler._dataSource._showArchived;
	}

	$scope.addFilter = function(filter) {
		var last = _.last($scope.filters);
		if (last && !(last.field && last.operator)) return;
		$scope.filters.push(filter || { $new: true });
	};

	this.removeFilter = function(filter) {
		var index = $scope.filters.indexOf(filter);
		if (index > -1) {
			$scope.filters.splice(index, 1);
		}
		if ($scope.filters.length === 0) {
			$scope.addFilter();
		}
	};

	$scope.$on('on:select-custom', function(e, custom) {

		$scope.filters.length = 0;
		$scope.contextData = {};

		if (custom.$selected) {
			select(custom);
		} else {
			$scope.addFilter();
		}

		return $scope.applyFilter();
	});

	$scope.$on('on:select-domain', function(e, filter) {
		$scope.filters.length = 0;
		$scope.addFilter();
		return $scope.applyFilter();
	});

	$scope.$on('on:before-save', function(e, data) {
		var criteria = $scope.prepareFilter();
		if (data) {
			data.criteria = criteria;
		}
	});

	$scope.$on('on:clear-filter', function (e, options) {
		$scope.clearFilter(options);
	});

	function select(custom) {

		var criteria = custom.criteria;
		var filters = criteria.criteria;
		
		if (filters && filters.length === 2 && filters[1].criteria) {
			var first = _.first(filters);
			var last = _.last(filters);
			var name = first.fieldName.replace('.id', '');
			filters = last.criteria;
			$scope.contextData = {
				field: $scope.fields[name],
				value: first.value,
				title: first.title,
				saved: true
			};
		}

		$scope.operator = criteria.operator || 'and';

		_.each(filters, function(item) {
			
			var fieldName = item.fieldName || '';
			if (fieldName && fieldName.indexOf('.') > -1) {
				fieldName = fieldName.substring(0, fieldName.indexOf('.'));
			}

			var field = $scope.fields[fieldName] || {};
			var filter = {
				field: fieldName,
				value: item.value,
				value2: item.value2
			};

			filter.type = field.type || 'string';
			filter.operator = item.operator;

			if (item.operator === '=' && filter.value === true) {
				filter.operator = 'true';
			}
			if (filter.operator === '=' && filter.value === false) {
				filter.operator = 'false';
			}

			if (field.type === 'date' || field.type === 'datetime') {
				if (filter.value) {
					filter.value = moment(filter.value).toDate();
				}
				if (filter.value2) {
					filter.value2 = moment(filter.value2).toDate();
				}
			}
			
			if (filter.type == 'many-to-one' || field.type === 'one-to-one') {
				filter.targetName = field.targetName;
			}

			$scope.addFilter(filter);
		});
	}

	$scope.clearFilter = function(options) {
		$scope.filters.length = 0;
		$scope.showArchived = false;
		$scope.addFilter();
		$scope.contextData = {};

		if ($scope.$parent.onClear) {
			$scope.$parent.onClear();
		}

		var hide = options === true;
		var silent = !hide && options && options.silent;

		if (!silent) {
			$scope.applyFilter();
		}

		if ($scope.$parent && hide) {
			$scope.$parent.$broadcast('on:hide-menu');
		}
	};

	$scope.prepareFilter = function() {

		var criteria = {
			archived: $scope.showArchived,
			operator: $scope.operator,
			criteria: []
		};

		_.each($scope.filters, function(filter) {

			if (!filter.field || !filter.operator) {
				return;
			}

			var criterion = {
				fieldName: filter.field,
				operator: filter.operator,
				value: filter.value
			};

			if (filter.targetName && criterion.fieldName.indexOf(':') == -1 && (
					filter.operator !== 'isNull' ||
					filter.operator !== 'notNull')) {
				criterion.fieldName += '.' + filter.targetName;
			}
			if (/-many/.test(filter.type) && (
					filter.operator !== 'isNull' ||
					filter.operator !== 'notNull')) {
				criterion.fieldName += '.id';
			}

			if (criterion.operator == "true") {
				criterion.operator = "=";
				criterion.value = true;
			}
			if (criterion.operator == "false") {
				criterion = {
					operator: "or",
					criteria: [
					    {
					    	fieldName: filter.field,
						    operator: "=",
						    value: false
					    },
					    {
					    	fieldName: filter.field,
						    operator: "isNull"
					    }
					]
				};
			}

			if (criterion.operator == "between" || criterion.operator == "notBetween") {
				criterion.value2 = filter.value2;
			}

			if (filter.$new) {
				criterion.$new = true;
			}

			criteria.criteria.push(criterion);
		});
		
		var contextData = $scope.contextData || {};
		
		if (contextData.value && contextData.field && contextData.field.name) {
			var previous = criteria.criteria;
			var operator = criteria.operator;

			criteria.operator = "and";
			criteria.criteria = [{
				fieldName: contextData.field.name + ".id",
				operator: "=",
				value: contextData.value,
				title: contextData.title,
				$new: !contextData.saved
			}];

			if (previous && previous.length) {
				criteria.criteria.push({
					operator: operator,
					criteria: previous
				});
			}
		}

		return criteria;
	};

	$scope.applyFilter = function(hide) {
		var criteria = $scope.prepareFilter();
		if ($scope.$parent.onFilter) {
			$scope.$parent.onFilter(criteria);
		}
		if ($scope.$parent && hide) {
			$scope.$parent.$broadcast('on:hide-menu');
		}
		handler.$broadcast('on:advance-filter', criteria);
		handler.$broadcast('on:context-field-change', $scope.contextData);
	};
	
	$scope.canExport = function() {
		var handler = $scope.$parent.handler;
		if (handler && handler.hasPermission) {
			return handler.hasPermission('export');
		}
		return true;
	};

	$scope.onExport = function(full) {
		var handler = $scope.$parent.handler;
		if (handler && handler.onExport) {
			handler.onExport(full);
		}
	};
}

ui.directive('uiFilterForm', function() {

	return {
		replace: true,

		scope: {
			model: '=',
			onSearch: '&'
		},

		controller: FilterFormCtrl,

		link: function(scope, element, attrs, ctrl) {
			var unwatch = scope.$watch("$parent.viewItems", function (items) {
				if (items === undefined) return;
				unwatch();
				ctrl.doInit(scope.model, items);
			});
		},
		template:
		"<div class='filter-form'>" +
			"<div ui-filter-context fields='fields' context='contextData'></div>" +
			"<form class='filter-operator form-inline'>" +
				"<label class='radio inline'>" +
					"<input type='radio' name='operator' ng-model='operator' value='and' x-translate><span x-translate>and</span>" +
				"</label>" +
				"<label class='radio inline'>" +
					"<input type='radio' name='operator' ng-model='operator' value='or' x-translate><span x-translate>or</span>" +
				"</label>" +
				"<label class='checkbox inline show-archived'>" +
					"<input type='checkbox' ng-model='showArchived'><span x-translate>Show archived</span>" +
				"</label>" +
			"</form>" +
			"<div ng-repeat='filter in filters' ui-filter-item x-fields='fields' x-filter='filter'></div>" +
			"<div class='links'>"+
				"<a href='' ng-click='addFilter()' x-translate>Add filter</a>"+
				"<span class='divider'>|</span>"+
				"<a href='' ng-click='clearFilter(true)' x-translate>Clear</a></li>"+
				"<span class='divider' ng-if='canExport()'>|</span>"+
				"<a href='' ng-if='canExport()' ng-click='onExport()' x-translate>Export</a></li>"+
				"<span class='divider' ng-if='canExport()'>|</span>"+
				"<a href='' ng-if='canExport()' ng-click='onExport(true)' x-translate>Export full</a></li>"+
				"<span class='divider'>|</span>"+
				"<a href='' ng-click='applyFilter(true)' x-translate>Apply</a></li>"+
			"<div>"+
		"</div>"
	};
});

ui.directive('uiFilterBox', function() {

	return {
		scope: {
			handler: '='
		},
		controller: ['$scope', 'ViewService', 'DataSource', function($scope, ViewService, DataSource) {

			var handler = $scope.handler,
				params = (handler._viewParams || {}).params;

			var filterView = params ? params['search-filters'] : null;
			var filterDS = DataSource.create('com.axelor.meta.db.MetaFilter');

			this.$scope = $scope;

			$scope.model = handler._model;
			$scope.view = {};

			$scope.viewFilters = [];
			$scope.custFilters = [];

			$scope.canShare = axelor.config["view.adv-search.share"] !== 'false';

			if (filterView) {
				ViewService.getMetaDef($scope.model, {name: filterView, type: 'search-filters'})
				.success(function(fields, view) {
					var viewItems = _.map(view.items, function (item) {
						var field = fields[item.name] || {};
						return _.extend({}, field, item, { type: field.type });
					});
					$scope.view = view;
					$scope.viewItems = viewItems;
					$scope.viewFilters = angular.copy(view.filters);
				});
			} else {
				$scope.viewItems = [];
				filterView = 'act:' + (handler._viewParams || {}).action;
			}

			if (filterView) {
				filterDS.rpc('com.axelor.meta.web.MetaFilterController:findFilters', {
					model: 'com.axelor.meta.db.MetaFilter',
					context: {
						filterView: filterView
					}
				}).success(function(res) {
					_.each(res.data, function(item) {
						acceptCustom(item);
					});
				});
			}

			var current = {
				criteria: {},
				domains: [],
				customs: []
			};

			function acceptCustom(filter) {

				var custom = {
					title: filter.title,
					name: filter.name,
					shared: filter.shared,
					criteria: angular.fromJson(filter.filterCustom)
				};
				custom.selected = filter.filters ? filter.filters.split(/\s*,\s*/) : [];
				custom.selected = _.map(custom.selected, function(x) {
					return parseInt(x);
				});

				var found = _.findWhere($scope.custFilters, {name: custom.name});
				if (found) {
					_.extend(found, custom);
				} else {
					$scope.custFilters.push(custom);
				}

				return found ? found : custom;
			}

			$scope.selectFilter = function(filter, isCustom, live) {

				var selected = live ? !filter.$selected : filter.$selected;
				var selection = isCustom ? current.customs : current.domains;
				var applyAll = (handler.schema||{}).customSearch === false;

				if (live) {
					$scope.onClear();
				}

				filter.$selected = selected;

				var index = selection.indexOf(filter);
				if (selected) {
					selection.push(filter);
				}
				if (!selected && index > -1) {
					selection.splice(index, 1);
				}

				if (isCustom && (live || applyAll)) {
					$scope.hasCustSelected = filter.$selected && !applyAll;
					$scope.custName = filter.$selected ? filter.name : null;
					$scope.oldCustTitle = filter.$selected ? filter.title : '';
					$scope.custTitle = filter.$selected ? filter.title : '';
					$scope.custShared = filter.$selected ? filter.shared : false;
					return $scope.$broadcast('on:select-custom', filter, selection);
				}

				if (live || applyAll) {
					$scope.$broadcast('on:select-domain', filter);
				}
			};

			$scope.isSelected = function(filter) {
				return filter.$selected;
			};

			$scope.onRefresh = function() {
				if (this.custTerm) {
					return this.onFreeSearch();
				}
				handler.onRefresh();
			};

			$scope.hasFilters = function(which) {
				if (which === 1) {
					return this.viewFilters && this.viewFilters.length;
				}
				if (which === 2) {
					return this.custFilters && this.custFilters.length;
				}
				return (this.viewFilters && this.viewFilters.length) ||
					   (this.custFilters && this.custFilters.length);
			};

			$scope.canSaveNew = function() {
				if ($scope.hasCustSelected && $scope.oldCustTitle && $scope.custTitle) {
					return !angular.equals(_.underscored($scope.oldCustTitle), _.underscored($scope.custTitle));
				}
				return false;
			};

			$scope.onSave = function(saveAs) {

				var data = { criteria: null };
				$scope.$broadcast('on:before-save', data);

				var title = _.trim($scope.custTitle),
					name = $scope.custName || _.underscored(title);

				if (saveAs) {
					name = _.underscored(title);
				}

				var selected = [];

				_.each($scope.viewFilters, function(item, i) {
					if (item.$selected) selected.push(i);
				});

				var custom = data.criteria || {};

				custom = _.extend({
					operator: custom.operator,
					criteria: custom.criteria
				});

				var value = {
					name: name,
					title: title,
					shared: $scope.custShared,
					filters: selected.join(', '),
					filterView: filterView,
					filterCustom: angular.toJson(custom)
				};

				filterDS.rpc('com.axelor.meta.web.MetaFilterController:saveFilter', {
					model: 'com.axelor.meta.db.MetaFilter',
					context: value
				}).success(function(res) {
					var custom = acceptCustom(res.data);
					custom.$selected  = false;
					$scope.selectFilter(custom, true, true);
				});
			};

			$scope.onDelete = function() {

				var name = $scope.custName;
				if (!$scope.hasCustSelected || !name) {
					return;
				}

				function doDelete() {
					filterDS.rpc('com.axelor.meta.web.MetaFilterController:removeFilter', {
						model: 'com.axelor.meta.db.MetaFilter',
						context: {
							name: name,
							filterView: filterView
						}
					}).success(function(res) {
						var found = _.findWhere($scope.custFilters, {name: name});
						if (found) {
							$scope.custFilters.splice($scope.custFilters.indexOf(found), 1);
							$scope.hasCustSelected = false;
							$scope.custName = null;
							$scope.custTitle = null;
							$scope.custShared = false;
						}
						$scope.onFilter();
					});
				}

				axelor.dialogs.confirm(_t("Would you like to remove the filter?"), function(confirmed){
					if (confirmed) {
						doDelete();
					}
				});
			};

			$scope.onClear = function() {

				_.each($scope.viewFilters, function(d) { d.$selected = false; });
				_.each($scope.custFilters, function(d) { d.$selected = false; });

				current.domains.length = 0;
				current.customs.length = 0;

				$scope.hasCustSelected = false;
				$scope.custName = null;
				$scope.oldCustTitle = null;
				$scope.custTitle = null;
				$scope.custShared = false;
				$scope.custTerm = null;
				$scope.tagItems = [];

				if ($scope.handler && $scope.handler.clearFilters) {
					$scope.handler.clearFilters();
				}
			};

			$scope.tagItems = [];

			$scope.onFilter = function(criteria) {

				if (criteria) {
					current.criteria = criteria;
				} else {
					criteria = current.criteria;
				}

				var search = _.extend({}, criteria);
				if (!search.criteria) {
					search.operator = 'and';
					search.criteria = [];
				} else {
					search.criteria = _.clone(search.criteria);
				}
				
				if (arguments.length > 1 && _.isString(arguments[1])) {
					search._searchText = arguments[1];
				}

				var domains = [],
					customs = [];

				_.each(current.domains, function(domain) {
					domains.push(domain);
				});

				_.each(current.customs, function(custom) {
					if($scope.hasCustSelected) { return; }
					if (custom.criteria && custom.criteria.criteria) {
						customs.push({
							operator: custom.criteria.operator || 'and',
							criteria: custom.criteria.criteria
						});
					}
					_.each(custom.selected, function(i) {
						var domain = $scope.viewFilters[i];
						if (domains.indexOf(domain) == -1) {
							domains.push(domain);
						}
					});
				});

				if (customs.length > 0) {
					search.criteria.push({
						operator: criteria.operator || 'and',
						criteria: customs
					});
				}

				search._domains = domains;
				search.criteria = process(search.criteria);

				// process criteria for datetime fields, always use between operator
				function process(filter) {
					if (_.isArray(filter)) return _.map(filter, process);
					if (_.isArray(filter.criteria)) {
						filter.criteria = process(filter.criteria);
						return filter;
					}
					if (filter.operator != '=') return filter;
					if (($scope.fields[filter.fieldName]||{}).type != 'datetime') return filter;
					if (!(/\d+-\d+\d+T/.test(filter.value) || _.isDate(filter.value))) {
						return filter;
					}
					var v1 = moment(filter.value).startOf('day').toDate();
					var v2 = moment(filter.value).endOf('day').toDate();
					return _.extend({}, filter, {
						operator: 'between',
						value: v1,
						value2: v2
					});
				}
				
				function countCustom(criteria) {
					var n = _.filter(criteria, function (item) { return item.$new; }).length;
					if (criteria.length === 2 && criteria[1].criteria) {
						n += countCustom(criteria[1].criteria);
					}
					return n;
				}

				var tag = {};
				var all = _.chain([$scope.viewFilters, $scope.custFilters])
				   .flatten()
				   .filter(function (item) {
					  return item && item.$selected;
				   })
				   .pluck('title')
				   .value();

				var nCustom = countCustom((criteria||{}).criteria);
				if (nCustom > 0) {
					all.push(_t('Custom ({0})', nCustom));
				}

				if (all.length === 1) {
					tag.title = all[0];
				}
				if (all.length > 1) {
					tag.title = _t('Filters ({0})', all.length);
					tag.help = all.join(', ');
				}

				if (all.length === 0) {
					$scope.tagItems = [];
				} else {
					$scope.tagItems = [tag];
				}

				handler.filter(search);
			};

			$scope.onFreeSearch = function() {

				var filters = [],
					fields = {},
					nameField = this.nameField,
					text = this.custTerm,
					number = +(text);

				text = text ? text.trim() : null;

				if ((handler.schema || {}).freeSearch === 'all') {
					fields = _.extend({}, this.$parent.fields, this.fields);
				}
				
				if (!nameField) {
					nameField = (_.extend({}, this.$parent.fields, this.fields).name || {}).name;
				}

				if (nameField && text) {
					filters.push({
						fieldName: nameField,
						operator: 'like',
						value: text
					});
				}

				for(var name in fields) {

					if (name === nameField || !text) continue;

					var fieldName = null,
						operator = "like",
						value = text;

					var field = fields[name];

					switch (field.type) {
					case 'integer':
					case 'decimal':
						if (_.isNaN(number) || !text || !_.isNumber(number)) continue;
						if (field.type === 'integer' && (number > 2147483647 || number < -2147483648)) continue;
						fieldName = name;
						operator = '=';
						value = number;
						break;
					case 'text':
					case 'string':
						fieldName = name;
						break;
					case 'one-to-one':
					case 'many-to-one':
						if (field.jsonField) {
							fieldName = name;
						} else if (field.targetName) {
							fieldName = name + '.' + field.targetName;
						}
						break;
					case 'boolean':
						if (/^(t|f|y|n|true|false|yes|no)$/.test(text)) {
							fieldName = name;
							operator = '=';
							value = /^(t|y|true|yes)$/.test(text);
						}
						break;
					}

					if (!fieldName) continue;

					filters.push({
						fieldName: fieldName,
						operator: operator,
						value: value
					});
				}

				var criteria = {
					operator: 'or',
					criteria: filters
				};

				this.onFilter(criteria, text);
			};
		}],
		link: function(scope, element, attrs) {

			var menu = element.children('.filter-menu'),
				toggleButton = null;

			scope.onSearch = function(e) {
				if (menu && menu.is(':visible')) {
					hideMenu();
					return;
				}
				toggleButton = $(e.currentTarget);
				menu.zIndex(element.zIndex() + 1);
				menu.show();
				scope.doAdjust();

				$(document).on('mousedown.search-menu', onMouseDown);
				
				scope.$applyAsync(function () {
					scope.visible = true;
				});
			};

			scope.doAdjust = (function() {
				var opts = {
					my: "left top",
					at: "left bottom",
					of: element,
					collision: "fit"
				};
				if (element.hasClass('pull-right')) {
					opts.my = "right top";
					opts.at = "right bottom"
				}
				return function() {
					menu.position(opts);
				}
			}());

			scope.onClearFilter = function () {
				hideMenu();
				scope.visible = true;
				scope.$broadcast('on:clear-filter');
				scope.$timeout(function () {
					scope.visible = false;
				});
			};

			// append menu to body to fix overlaping issue
			scope.$timeout(function() {
				menu.zIndex(element.zIndex() + 1);
				menu.appendTo("body");
			});

			element.on('keydown.search-query', '.search-query', function(e) {
				if (e.keyCode === 13) { // enter
					scope.onFreeSearch();
				}
			});
			
			scope.$on('on:hide-menu', function () {
				hideMenu();
			});
			
			scope.$on('on:clear-filter-silent', function () {
				var visible = scope.visible;
				scope.visible = true;
				scope.$broadcast('on:clear-filter', { silent: true });
				scope.$timeout(function () {
					scope.visible = visible;
				});
			});

			function hideMenu() {
				$(document).off('mousedown.search-menu', onMouseDown);
				scope.$timeout(function () {
					scope.visible = false;
				});
				menu.hide();
			}

			function onMouseDown(e) {
				var all = $(menu).add(toggleButton);
				if (all.is(e.target) || all.has(e.target).size() > 0) {
					return;
				}
				if ($(e.target).zIndex() > $(menu).zIndex()) {
					return;
				}
				if(menu) {
					hideMenu();
				}
			}
			
			scope.hideMenu = hideMenu;

			scope.handler.$watch('schema.freeSearch', function (value, old) {
				if (value === 'none') {
					var input = element.find('input:first')
						.addClass('not-readonly')
						.attr('readonly', true)
						.click(scope.onSearch.bind(scope));
				}
			});

			element.on('$destroy', function() {
				$(document).on('mousedown.search-menu', onMouseDown);
				if (menu) {
					menu.remove();
					menu = null;
				}
			});
		},
		replace: true,
		template:
		"<div class='filter-box'>" +
			"<div class='tag-select picker-input search-query'>" +
			  "<ul>" +
				"<li class='tag-item label label-primary' ng-repeat='item in tagItems'>" +
					"<span class='tag-text' title='{{item.help}}'>{{item.title}}</span> " +
					"<i class='fa fa-times fa-small' ng-click='onClearFilter()'></i>" +
				"</li>" +
				"<li class='tag-selector' ng-show='!tagItems.length'>" +
					"<input type='text' autocomplete='off' ng-model='custTerm'>" +
				"</li>" +
			  "</ul>" +
			  "<span class='picker-icons'>" +
				"<i ng-click='onSearch($event)' class='fa fa-caret-down'></i>"+
				"<i ng-click='onRefresh()' class='fa fa-search'></i>" +
			  "</span>" +
			"</div>" +
			"<div class='filter-menu' ui-watch-if='visible'>" +
				"<span>" +
					"<strong x-translate>Advanced Search</strong>" +
					"<a href='' class='pull-right' ng-click='hideMenu()'><i class='fa fa-times'></i></a>" +
				"</span>" +
				"<hr>"+
				"<div class='filter-list'>" +
					"<dl ng-show='!hasFilters() && handler.schema.customSearch == false' style='display: hidden;'>" +
						"<dd><span x-translate>No filters available</span></dd>" +
					"</dl>" +
					"<dl ng-show='hasFilters(1)'>" +
						"<dt><i class='fa fa-floppy-o'></i><span x-translate> Filters</span></dt>" +
						"<dd ng-repeat='filter in viewFilters' class='checkbox'>" +
							"<input type='checkbox' " +
								"ng-model='filter.$selected' " +
								"ng-click='selectFilter(filter, false, false)' ng-disabled='hasCustSelected'> " +
							"<a href='' ng-click='selectFilter(filter, false, true)' ng-disabled='hasCustSelected'>{{filter.title}}</a>" +
						"</dd>" +
					"</dl>" +
					"<dl ng-show='hasFilters(2)'>" +
						"<dt><i class='fa fa-filter'></i><span x-translate> My Filters</span></dt>" +
						"<dd ng-repeat='filter in custFilters' class='checkbox'>" +
							"<input type='checkbox' " +
								"ng-model='filter.$selected' " +
								"ng-click='selectFilter(filter, true, false)' ng-disabled='hasCustSelected'> " +
							"<a href='' ng-click='selectFilter(filter, true, true)' ng-disabled='!filter.$selected && hasCustSelected'>{{filter.title}}</a>" +
						"</dd>" +
					"</dl>" +
				"</div>" +
				"<div ng-hide='handler.schema.customSearch == false'>" +
					"<hr ng-show='hasFilters()'>" +
					"<div ui-filter-form x-model='model'></div>" +
					"<hr>" +
					"<div class='form-inline'>" +
						"<div class='control-group'>" +
							"<input type='text' placeholder='{{\"Save filter as\" | t}}' ng-model='custTitle'> " +
							"<label class='checkbox' ng-if='canShare'>" +
								"<input type='checkbox' ng-model='custShared'><span x-translate>Share</span>" +
							"</label>" +
						"</div>" +
						"<button class='btn btn-small' ng-click='onSave()' ng-show='custTitle && !hasCustSelected'><span x-translate>Save</span></button> " +
						"<button class='btn btn-small' ng-click='onSave()' ng-show='custTitle && hasCustSelected'><span x-translate>Update</span></button> " +
						"<button class='btn btn-small' ng-click='onSave(true)' ng-show='canSaveNew()'><span x-translate>Save as</span></button> " +
						"<button class='btn btn-small' ng-click='onDelete()' ng-show='hasCustSelected'><span x-translate>Delete</span></button>" +
					"</div>" +
				"</div>" +
			"</div>" +
		"</div>"
	};
});

})();
