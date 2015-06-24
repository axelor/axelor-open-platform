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
(function() {

var ui = angular.module('axelor.ui');

DashboardCtrl.$inject = ['$scope', '$element'];
function DashboardCtrl($scope, $element) {

	var view = $scope._views['dashboard'];
	if (view.items) {
		$scope.$timeout(function () {
			$scope.parse(view);
		});
	} else {
		$scope.loadView('dashboard', view.name).success(function(fields, schema){
			$scope.parse(schema);
		});
	}

	$scope.applyLater(function(){
		if (view.deferred)
			view.deferred.resolve($scope);
	}, 0);

	$scope.show = function(promise) {
		$scope.updateRoute();
	};

	$scope.onShow = function() {

	};

	$scope.getContext = function() {
		return _.extend({}, $scope._context);
	};

	$scope.getRouteOptions = function() {
		return {
			mode: 'dashboard',
			args: []
		};
	};

	$scope.setRouteOptions = function(options) {
		if (!$scope.isNested) {
			$scope.updateRoute();
		}
	};

	$scope.parse = function(schema) {
		var items = angular.copy(schema.items || []);
		var row = [];

		items.forEach(function (item) {
			var span = item.colSpan || 6;

			item.spanCss = {};
			item.spanCss['dashlet-cs' + span] = true;

			row.push(item);
		});

		$scope.schema = schema;
		$scope.row = row;
	};
}

ui.directive('uiViewDashboard', ['$compile', 'ViewService', function($compile, ViewService) {

	return {
		scope: true,
		controller: DashboardCtrl,
		link: function(scope, element, attrs) {

			function save() {
				var schema = scope.schema;
				var items = [];

				element.find('.dashlet').each(function (i) {
					var j = $(this).data('index');
					$(this).data("index", i);
					items.push(schema.items[j]);
				});

				if (angular.equals(schema.items, items)) {
					return;
				}

				schema.items = items;
				return ViewService.save(schema);
			}

			function makeSortable() {
				element.sortable({
					handle: ".dashlet-header",
					cancel: ".dashlet-buttons",
					items: ".dashlet",
					tolerance: "pointer",
					activate: function(e, ui) {
						var height = ui.helper.height();
						ui.placeholder.height(height);
					},
					deactivate: function(event, ui) {
						axelor.$adjustSize();
					},
					stop: function (event, ui) {
						save();
					}
				});
			}

			var unwatch = scope.$watch("row.length", function (length) {
				if (!length) { return; }
				unwatch();
				unwatch = null;
				scope.waitForActions(makeSortable);
			});
		},
		replace: true,
		transclude: true,
		template:
		"<div>" +
			"<div class='dashlet' ng-class='dashlet.spanCss' ng-repeat='dashlet in row' data-index='{{$index}}' ui-view-dashlet></div>" +
		"</div>"
	};
}]);

DashletCtrl.$inject = ['$scope', '$element', 'MenuService', 'DataSource', 'ViewService'];
function DashletCtrl($scope, $element, MenuService, DataSource, ViewService) {

	var self = this;

	function init() {

		ViewCtrl.call(self, $scope, DataSource, ViewService);

		$scope.show = function() {

		};

		$scope.onShow = function() {

		};
	}

	$scope.initDashlet = function(dashlet, options) {

		var action = dashlet.action;
		if (!action) {
			return init();
		}

		MenuService.action(action, options).success(function(result){
			if (_.isEmpty(result.data)) {
				return;
			}
			var view = result.data[0].view;

			$scope._viewParams = view;
			$scope._viewAction = action;

			init();

			$scope.title = dashlet.title || view.title;
			$scope.parseDashlet(dashlet, view);
		});
	};

	$scope.$on('on:attrs-change:refresh', function(e) {
		e.preventDefault();
		if ($scope.onRefresh) {
			$scope.onRefresh();
		}
	});

	$scope.$on('on:tab-reload', function(e) {
		if ($scope.onRefresh) {
			$scope.onRefresh();
		}
	});
}

ui.directive('uiViewDashlet', ['$compile', function($compile){
	return {
		scope: true,
		controller: DashletCtrl,
		link: function(scope, element, attrs) {

			var lazy = true;
			scope.waitForActions(function () {
				var unwatch = scope.$watch(function () {
					var dashlet = scope.dashlet;
					if (!dashlet) {
						return;
					}

					if (element.parent().is(":hidden")) {
						return lazy = true;
					}

					unwatch();
					unwatch = null;

					var ctx = undefined;
					if (scope.getContext) {
						ctx = scope.getContext();
					}
					scope.initDashlet(dashlet, {
						context: ctx
					});
				});
			});

			scope.parseDashlet = _.once(function(dashlet, view) {
				var body = element.find('.dashlet-body:first');
				var template = $('<div ui-portlet-' + view.viewType + '></div>');

				scope.noFilter = !dashlet.canSearch;

				template = $compile(template)(scope);
				body.append(template);

				element.removeClass('hidden');

				scope.show();

				// if lazy, load data
				if (scope.onRefresh && lazy) {
					scope.onRefresh();
				}
			});

			scope.onDashletToggle = function(event) {
				var e = $(event.target);
				e.toggleClass('fa-chevron-up fa-chevron-down');
				element.toggleClass('dashlet-minimized');
				if (e.hasClass('fa-chevron-up')) {
					axelor.$adjustSize();
				}
			};

			scope.doNext = function() {
				if (this.canNext()) this.onNext();
			};

			scope.doPrev = function() {
				if (this.canPrev()) this.onPrev();
			};
		},
		replace: true,
		template:
			"<div class='dashlet hidden'>" +
				"<div class='dashlet-header'>" +
					"<div class='dashlet-title pull-left'>{{title}}</div>" +
					"<div class='dashlet-buttons pull-right'>" +
						"<a href='' ng-click='onRefresh()'><i class='fa fa-refresh'></i></a>" +
					"</div>" +
					"<div class='dashlet-pager pull-right' ng-show='showPager'>" +
						"<span class='dashlet-pager-text'>{{pagerText()}}</span>" +
						"<a href='' ng-click='doPrev()' ng-class='{disabled: !canPrev()}'><i class='fa fa-step-backward'></i></a>" +
						"<a href='' ng-click='doNext()' ng-class='{disabled: !canNext()}'><i class='fa fa-step-forward'></i></a>" +
					"</div>" +
				"</div>" +
				"<div class='dashlet-body'></div>" +
			"</div>"
	};
}]);

})();