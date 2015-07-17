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

PortalCtrl.$inject = ['$scope', '$element'];
function PortalCtrl($scope, $element) {

	var view = $scope._views['portal'];
	if (view.items) {
		$scope.$timeout(function () {
			$scope.parse(view);
		});
	} else {
		$scope.loadView('portal', view.name).success(function(fields, schema){
			$scope.parse(schema);
		});
	}

	$scope.applyLater(function(){
		if (view.deferred)
			view.deferred.resolve($scope);
	}, 0);

	$scope.parse = function(schema) {
	};
	
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
			mode: 'portal',
			args: []
		};
	};
	
	$scope.setRouteOptions = function(options) {
		if (!$scope.isNested) {
			$scope.updateRoute();
		}
	};
}

var tmplPortlet =
	'<div ui-view-portlet '+
		'x-action="{{p.action}}" '+
		'x-can-search="{{p.canSearch}}" '+
		'x-col-span="{{p.colSpan}}" '+
		'x-row-span="{{p.rowSpan}}" ' +
		'x-height="{{p.height}}"></div>';

var tmplTabs =
	"<div ui-portal-tabs x-schema='p'></div>";

ui.directive('uiViewPortal', ['$compile', function($compile) {
	
	return {
		scope: true,
		controller: PortalCtrl,
		link: function(scope, element, attrs) {
			
			function init() {
				element.sortable({
					handle: ".portlet-header",
					items: "> .portlet, > .portal-tabs",
					forceHelperSize: true,
					forcePlaceholderSizeType: true,
					activate2: function(event, ui) {
						var width = ui.placeholder.width();
						var height = ui.placeholder.height();
						
						ui.placeholder.width(width - 4);
						ui.placeholder.height(height - 4);
						
						ui.placeholder.css({
							'left': '2px',
							'top': '2px',
							'margin-right': '4px'
						});
					},
					deactivate: function(event, ui) {
						axelor.$adjustSize();
					}
				});
			}

			scope.parse = function(schema) {
				scope.portletCols = schema.cols || 2;
				scope.portlets = schema.items;

				_.each(scope.portlets, function (item) {
					var tmpl = item.type === 'tabs' ? tmplTabs : tmplPortlet;
					var child = scope.$new();
					child.p = item;
					
					var elem = $compile(tmpl)(child);
					
					element.append(elem);
				});

				setTimeout(init);
			};
		},
		replace: true,
		transclude: true,
		template: '<div class="portal" ng-transclude></div>'
	};
}]);

PortletCtrl.$inject = ['$scope', '$element', 'MenuService', 'DataSource', 'ViewService'];
function PortletCtrl($scope, $element, MenuService, DataSource, ViewService) {
	
	var self = this;
	
	function init() {
		
		ViewCtrl.call(self, $scope, DataSource, ViewService);
		
		$scope.show = function() {

		};
		
		$scope.onShow = function() {
			
		};
	}
	
	$scope.initPortlet = function(action, options) {

		MenuService.action(action, options).success(function(result){
			if (_.isEmpty(result.data)) {
				return;
			}
			var view = result.data[0].view;
			
			$scope._viewParams = view;
			$scope._viewAction = action;

			init();

			$scope.title = view.title;
			$scope.parsePortlet(view);
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

function setPortletSize(scope, element, attrs) {
	var cols = scope.portletCols;
	var colSpan = +attrs.colSpan || 1;
	var rowSpan = +attrs.rowSpan || 1;

	var width = 100;
	var height = (+attrs.height || 250) * rowSpan;
	
	width = (width / cols) * colSpan;

	element.width(width + '%').height(height);
}

ui.directive('uiViewPortlet', ['$compile', function($compile){
	return {
		scope: true,
		controller: PortletCtrl,
		link: function(scope, element, attrs) {

			var lazy = false;
			var unwatch = scope.$watch(function () {
				var action = attrs.action;
				if (!action) {
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
				scope.initPortlet(action, {
					context: ctx
				});
			});
			
			scope.parsePortlet = _.once(function(view) {

				scope.noFilter = attrs.canSearch != "true";

				var template = $compile($('<div ui-portlet-' + view.viewType + '></div>'))(scope);
				element.find('.portlet-content:first').append(template);

				scope.show();
				
				if (scope.portletCols) {
					setPortletSize(scope, element, attrs);
				}

				// if lazy, load data
				if (scope.onRefresh && lazy) {
					scope.onRefresh();
				}
			});
			
			scope.onPortletToggle = function(event) {
				var e = $(event.target);
				e.toggleClass('fa-chevron-up fa-chevron-down');
				element.toggleClass('portlet-minimized');
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
		'<div class="portlet">'+
			'<div class="portlet-body stackbar">'+
				'<div class="portlet-header navbar">'+
					'<div class="navbar-inner">'+
						'<div class="container-fluid">'+
							'<span class="brand" ng-bind-html="title"></span>'+
							'<ul class="nav pull-right">'+
								'<li class="portlet-pager" ng-show="showPager">'+
									'<span class="portlet-pager-text">{{pagerText()}}</span>'+
									'<span class="icons-bar">'+
										'<i ng-click="doPrev()" ng-class="{disabled: !canPrev()}" class="fa fa-step-backward"></i>'+
										'<i ng-click="doNext()" ng-class="{disabled: !canNext()}" class="fa fa-step-forward"></i>'+
									'</span>'+
								'</li>'+
								'<li class="divider-vertical"></li>'+
								'<li>'+
									'<span class="icons-bar">'+
										'<i title="{{\'Refresh\' | t}}" ng-click="onRefresh()" class="fa fa-refresh"></i>'+
										'<i title="{{\'Toggle\' | t}}" ng-click="onPortletToggle($event)" class="fa fa-chevron-up"></i>'+
									'</span>'+
								'</li>'+
							'</ul>'+
						'</div>'+
					'</div>'+
				'</div>'+
				'<div class="portlet-content"></div>'+
			'</div>'+
		'</div>'
	};
}]);

ui.directive('uiPortalTabs',  function() {
	return {
		scope: {
			schema: '='
		},
		replace: true,
		link: function(scope, element, attrs) {
			
			var schema = scope.schema;
			
			var first = _.first(schema.tabs);
			if (first) {
				first.active = true;
			}

			scope.tabClick = function (tab) {
				_.each(schema.tabs, function (item) {
					item.active = false;
				});
				tab.active = true;
				axelor.$adjustSize();
			};
			
			scope.tabs = schema.tabs;
			scope.portletCols = scope.$parent.portletCols;

			setPortletSize(scope, element, {
				colSpan: schema.colSpan,
				rowSpan: schema.rowSpan,
				height: schema.height
			});
			
			element.height('auto');
		},
		template:
			"<div class='tabbable-tabs portal-tabs'>" +
				"<ul class='nav nav-tabs nav-tabs-scrollable'>" +
					"<li ng-repeat='tab in tabs' ng-class='{active: tab.active}'>" +
						"<a href='' ng-click='tabClick(tab)' >{{tab.title}}</a>" +
					"</li>" +
				"</ul>" +
				"<div class='tab-content portal-tab-content'>" +
					"<div ng-repeat='tab in tabs' ng-class='{active: tab.active}' class='tab-pane'>" +
						"<div ui-portal-tab x-schema='tab'></div>" +
					"</div>" +
				"</div>" +
			"</div>"
	};
	
});

ui.directive('uiPortalTab', function() {

	return {
		scope: {
			schema: '='
		},
		controller: ['$scope', 'DataSource', 'ViewService', function ($scope, DataSource, ViewService) {
			
			var view = $scope.schema;
			var params = {
				viewType: 'portal',
				views: [ view ]
			};
			
			view.type = 'portal';
			
			$scope._viewParams = params;
			$scope.isNested = true;
			$scope._model = null;

			ViewCtrl.apply(this, arguments);
		}],
		template: "<div ui-view-portal></div>"
	};
});

})();