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
	"string"	: ["=", "!=", "like", "notLike", "isNull", "notNull"],
	"integer"	: ["=", "!=", ">=", "<=", ">", "<", "between", "notBetween", "isNull", "notNull"],
	"boolean"	: ["true", "false"]
};

_.each(["long", "decimal", "date", "time", "datetime"], function(type) {
	OPERATORS_BY_TYPE[type] = OPERATORS_BY_TYPE.integer;
});

_.each(["text", "many-to-one", "one-to-many", "many-to-many"], function(type) {
	OPERATORS_BY_TYPE[type] = OPERATORS_BY_TYPE.string;
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
			
			scope.getOperators = function() {
				
				if (element.is(':hidden')) {
					return;
				}
				
				var filter = scope.filter || {};
				if (filter.type === undefined) {
					return [];
				}

				return _.map(OPERATORS_BY_TYPE[filter.type], function(name) {
					return {
						name: name,
						title: OPERATORS[name]
					};
				});
			};
			
			scope.remove = function(filter) {
				form.removeFilter(filter);
			};
			
			scope.canShowInput = function() {
				return scope.filter &&
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
			
			scope.onFieldChange = function() {
				var filter = scope.filter,
					field = scope.fields[filter.field] || {};
				filter.type = field.type || 'string';
			};
		},
		template:
		"<div class='form-inline' style='margin-bottom: 5px;'>" +
			"<select ng-model='filter.field' ng-options='v.name as v.title for (k, v) in fields' ng-change='onFieldChange()' class='input-medium'></select> " +
			"<select ng-model='filter.operator' ng-options='o.name as o.title for o in getOperators()' class='input-medium'></select> "+
			"<input type='text' ui-filter-input ng-model='filter.value' ng-show='canShowInput()' class='input-medium'> " +
			"<input type='text' ui-filter-input ng-model='filter.value2' ng-show='canShowRange()' class='input-medium'> " +
			"<a href='' ng-click='remove(filter)'><i class='icon icon-remove'></i></a>" +
		"</div>"
	};
});

ui.directive('uiFilterInput', function() {
	
	return {
		require: '^ngModel',

		link: function(scope, element, attrs, model) {
		
			var picker = null;
	
			var options = {
				dateFormat: 'dd/mm/yy',
				showButtonsPanel: false,
				showTime: false,
				showOn: null,
				onSelect: function(dateText, inst) {
					var value = picker.datepicker('getDate');
					model.$setViewValue(value.toISOString());
				}
			};
	
			element.focus(function(e) {
				var type = scope.filter.type;
				if (!(type == 'date' || type == 'datetime')) {
					return
				}
				if (picker == null) {
					picker = element.datepicker(options);
				}
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

FilterFormCtrl.$inject = ['$scope', '$element', 'ViewService'];
function FilterFormCtrl($scope, $element, ViewService) {
	
	this.doInit = function(model) {
		return ViewService
		.getFields(model)
		.success(function(fields) {
			_.each(fields, function(field, name) {
				if (field.name === 'id' || field.name === 'version' ||
					field.name === 'archived' || field.name === 'selected') return;
				if (field.name === 'createdOn' || field.name === 'updatedOn') return;
				if (field.name === 'createdBy' || field.name === 'updatedBy') return;
				if (field.type === 'binary' || field.large) return;
				$scope.fields[name] = field;
			});
		});
	};

	$scope.fields = {};
	$scope.filters = [{}];
	$scope.operator = 'or';

	$scope.addFilter = function() {
		$scope.filters.push({});
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

	$scope.clearFilter = function() {
		$scope.filters.length = 0;
		$scope.addFilter();
		$scope.applyFilter();
	};

	$scope.applyFilter = function() {
		
		var criteria = {
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
			
			criteria.criteria.push(criterion);
		});
		
		if ($scope.$parent.onFilter) {
			$scope.$parent.onFilter(criteria);
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
			
			ctrl.doInit(scope.model);
		},
		template:
		"<div class='filter-form'>" +
			"<form class='filter-operator form-inline'>" +
				"<label class='radio inline'>" +
					"<input type='radio' name='operator' ng-model='operator' value='or'> or" +
				"</label>" +
				"<label class='radio inline'>" +
					"<input type='radio' name='operator' ng-model='operator' value='and'> and" +
				"</label>" +
			"</form>" +
			"<div ng-repeat='filter in filters' ui-filter-item x-fields='fields' x-filter='filter'></div>" +
			"<div class='links'>"+
				"<a href='' ng-click='addFilter()' x-translate>Add filter</a>"+
				"<span class='divider'>|</span>"+
				"<a href='' ng-click='clearFilter()' x-translate>Clear</a></li>"+
				"<span class='divider'>|</span>"+
				"<a href='' ng-click='applyFilter()' x-translate>Apply</a></li>"+
			"<div>"+
		"</div>"
	};
});

ui.directive('uiFilterMenu', function() {

	return {
		scope: {
			handler: '='
		},
		controller: ['$scope', 'ViewService', function($scope, ViewService) {
			
			var handler = $scope.handler,
				params = (handler._viewParams || {}).params;

			var filterView = {
				name: params['search-filters'],
				type: 'search-filters'
			};

			$scope.model = handler._model;
			$scope.view = {};

			$scope.viewFilters = [];
			$scope.custFilters = [];
			
			if (filterView) {
				ViewService.getMetaDef($scope.model, filterView).success(function(fields, view) {
					$scope.view = view;
					$scope.viewFilters = angular.copy(view.filters);
				});
			}
			
			var current = {
				criteria: {},
				domains: []
			};

			$scope.toggleFilter = function(filter) {
				var domains = current.domains;
				var i = domains.indexOf(filter);
				
				filter.$selected = !filter.$selected;
				if (filter.$selected) {
					domains.push(filter);
				} else if (i > -1) {
					domains.splice(i, 1);
				}
				$scope.onFilter();
			};

			$scope.isSelected = function(filter) {
				return filter.$selected;
			};
			
			$scope.onRefresh = function() {
				handler.onRefresh();
			};

			$scope.onFilter = function(criteria) {
				
				if (criteria) {
					current.criteria = criteria;
				} else {
					criteria = current.criteria;
				}
				
				var search = _.extend(criteria, {
					_domains: current.domains
				});
				
				if (search.criteria == undefined) {
					search.operator = "or";
					search.criteria = [];
				}

				handler.filter(search);
			};
		}],
		link: function(scope, element, attrs) {

			var menu = element.children('.filter-menu');

			scope.onSearch = function(e) {
				var hidden = $(e.currentTarget).is('.active');
				if (hidden) {
					return menu.hide();
				}
				menu.show();
				menu.position({
					my: "left top",
					at: "left bottom",
					of: e.currentTarget
				});
			};

			// append menu to view page to overlap the view
			setTimeout(function() {
				element.parents('.view-pane').after(menu);
			});
		},
		replace: true,
		template:
		"<div class='btn-group'>" +
			"<button class='btn' ng-click='onRefresh()'><i class='icon icon-refresh'></i> <span x-translate>Refresh</span></button>" +
			"<button class='btn' ng-click='onSearch($event)' data-toggle='button'>"+
				"<i class='icon icon-search'></i> <span x-translate>Search</span>" +
			"</button>" +
			"<div class='filter-menu'>"+
				"<div class='filter-list'>" +
					"<dl>" +
						"<dt>Filters</dt>" +
						"<dd ng-repeat='filter in viewFilters' " +
							"ng-click='toggleFilter(filter)' " +
							"ng-class='{selected: isSelected(filter)}'>{{filter.title}}</dd>" +
					"</dl>" +
					"<dl>" +
						"<dt>My Filters</dt>" +
						"<dd>filter 1</dd>" +
						"<dd>filter 2</dd>" +
					"</dl>" +
				"</div>" +
				"<hr>" +
				"<div ui-filter-form x-model='model'></div>" +
				"<hr>" +
				"<div class='form-inline'>" +
					"<div class='control-group'>" +
						"<input type='text' placeholder='save filter as'> " +
						"<label class='checkbox'>" +
							"<input type='checkbox'> <span x-translate>Share</span>" +
						"</label>" +
					"</div>" +
					"<button class='btn btn-small'>save</button>" +
				"</div>" +
			"</div>" +
		"</div>"
	};
});

}).call(this);
