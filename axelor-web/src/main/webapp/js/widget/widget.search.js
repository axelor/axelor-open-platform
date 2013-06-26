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

	$scope.addFilter = function(filter) {
		$scope.filters.push(filter || {});
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
		
		if (custom.$selected) {
			select(custom);
		} else {
			$scope.addFilter();
		}
		
		return $scope.applyFilter();
	});

	function select(custom) {

		var criteria = custom.criteria;
		
		$scope.operator = criteria.operator || 'or';

		_.each(criteria.criteria, function(item) {
			var filter = {
				field: item.fieldName,
				value: item.value,
				value2: item.value2
			};
			
			var field = $scope.fields[item.fieldName] || {};
			
			filter.type = field.type || 'string';
			filter.operator = item.operator;

			if (item.operator === '=' && filter.value === true) {
				filter.operator = 'true';
			}
			if (filter.operator === '=' && filter.value === false) {
				filter.operator = 'false';
			}
			
			$scope.addFilter(filter);
		});
	}

	$scope.clearFilter = function() {
		$scope.filters.length = 0;
		$scope.addFilter();
		
		if ($scope.$parent.onClear) {
			$scope.$parent.onClear();
		}

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
		controller: ['$scope', 'ViewService', 'DataSource', function($scope, ViewService, DataSource) {
			
			var handler = $scope.handler,
				params = (handler._viewParams || {}).params;

			var filterView = {
				name: params['search-filters'],
				type: 'search-filters'
			};

			var filterDS = DataSource.create('com.axelor.meta.db.MetaFilter');

			$scope.model = handler._model;
			$scope.view = {};

			$scope.viewFilters = [];
			$scope.custFilters = [];
			
			if (filterView) {
				ViewService.getMetaDef($scope.model, filterView).success(function(fields, view) {
					
					filterDS.rpc('com.axelor.meta.web.MetaUserController:findFilters', {
						model: 'com.axelor.meta.db.MetaFilter',
						context: {
							filterView: view.name
						}
					}).success(function(res) {
						_.each(res.data, function(item) {
							acceptCustom(item);
						});
					});

					$scope.view = view;
					$scope.viewFilters = angular.copy(view.filters);
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
			}

			$scope.toggleFilter = function(filter, isCustom) {

				filter.$selected = !filter.$selected;

				var selection = isCustom ? current.customs : current.domains;
				var i = selection.indexOf(filter);
				
				if (filter.$selected) {
					selection.push(filter);
				} else if (i > -1) {
					selection.splice(i, 1);
				}
				
				if (isCustom) {
					$scope.custName = filter.$selected ? filter.name : null;
					$scope.custTitle = filter.$selected ? filter.title : '';
					$scope.custShared = filter.$selected ? filter.shared : false;
					return $scope.$broadcast('on:select-custom', filter, selection);
				}

				$scope.onFilter();
			};

			$scope.isSelected = function(filter) {
				return filter.$selected;
			};
			
			$scope.onRefresh = function() {
				handler.onRefresh();
			};
			
			$scope.canSaveNew = function() {
				if ($scope.custName && $scope.custTitle) {
					return !angular.equals($scope.custName, _.underscored($scope.custTitle));
				}
				return false;
			};

			$scope.onSave = function(saveAs) {
				
				var title = _.trim($scope.custTitle),
					name = $scope.custName || _.underscored(title);

				if (saveAs) {
					name = _.underscored(title);
				}
				
				var selected = new Array();
				
				_.each($scope.viewFilters, function(item, i) {
					if (item.$selected) selected.push(i);
				});
				
				var custom = current.criteria || {};

				custom = _.extend({
					operator: custom.operator,
					criteria: custom.criteria
				});

				var value = {
					name: name,
					title: title,
					shared: $scope.custShared,
					filters: selected.join(', '),
					filterView: $scope.view.name,
					filterCustom: angular.toJson(custom)
				};
				
				filterDS.rpc('com.axelor.meta.web.MetaUserController:saveFilter', {
					model: 'com.axelor.meta.db.MetaFilter',
					context: value
				}).success(function(res) {
					acceptCustom(res.data);
				});
			};
			
			$scope.onDelete = function() {
				
				var name = $scope.custName;
				if (!name) {
					return;
				}
				
				function doDelete() {
					filterDS.rpc('com.axelor.meta.web.MetaUserController:removeFilter', {
						model: 'com.axelor.meta.db.MetaFilter',
						context: {
							name: name,
							filterView: $scope.view.name
						}
					}).success(function(res) {
						var found = _.findWhere($scope.custFilters, {name: name});
						if (found) {
							$scope.custFilters.splice($scope.custFilters.indexOf(found), 1);
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
				
				_.each(current.domains, function(d) { d.$selected = false; });
				_.each(current.customs, function(d) { d.$selected = false; });
				
				current.domains.length = 0;
				current.customs.length = 0;
				
				$scope.custName = null;
				$scope.custTitle = null;
				$scope.custShared = false;
			};

			$scope.onFilter = function(criteria) {

				if (criteria) {
					current.criteria = criteria;
				} else {
					criteria = current.criteria;
				}

				var search = _.extend({}, criteria);
				if (search.criteria == undefined) {
					search.operator = "or";
					search.criteria = [];
				} else {
					search.criteria = _.clone(search.criteria);
				}

				var domains = [],
					customs = [];

				_.each(current.domains, function(domain) {
					domains.push(domain);
				});

				_.each(current.customs, function(custom) {
					if (custom.criteria && custom.criteria.criteria) {
						customs.push({
							operator: custom.criteria.operator || 'or',
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

				search._domains = domains;
				
				if (customs.length > 0) {
					search.criteria.push({
						operator: 'or',
						criteria: customs
					});
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
						"<dd ng-repeat='filter in custFilters' " +
							"ng-click='toggleFilter(filter, true)' " +
							"ng-class='{selected: isSelected(filter)}'>{{filter.title}}</dd>" +
					"</dl>" +
				"</div>" +
				"<hr>" +
				"<div ui-filter-form x-model='model'></div>" +
				"<hr>" +
				"<div class='form-inline'>" +
					"<div class='control-group'>" +
						"<input type='text' placeholder='save filter as' ng-model='custTitle'> " +
						"<label class='checkbox'>" +
							"<input type='checkbox' ng-model='custShared'> <span x-translate>Share</span>" +
						"</label>" +
					"</div>" +
					"<button class='btn btn-small' ng-click='onSave()' ng-disabled='!custTitle'>save</button> " +
					"<button class='btn btn-small' ng-click='onSave(true)' ng-show='canSaveNew()'>save as</button> " +
					"<button class='btn btn-small' ng-click='onDelete()' ng-show='custName'>delete</button>" +
				"</div>" +
			"</div>" +
		"</div>"
	};
});

}).call(this);
