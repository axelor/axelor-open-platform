(function() {

var ui = angular.module('axelor.ui');

PortalCtrl.$inject = ['$scope', '$element'];
function PortalCtrl($scope, $element) {

	var view = $scope._views['portal'];
	var viewPromise = $scope.loadView('portal', view.name);

	setTimeout(function(){
		$scope.$apply(function(){
			if (view.deferred)
				view.deferred.resolve($scope);
		});
	});

	viewPromise.success(function(fields, schema){
		$scope.parse(schema);
	});
	
	$scope.parse = function(schema) {
	};
	
	$scope.show = function(promise) {
		$scope.updateRoute();
	};
	
	$scope.getRouteOptions = function() {
		return {
			mode: 'portal',
			args: []
		};
	};
	
	$scope.setRouteOptions = function(options) {
		$scope.updateRoute();
	};
	
}

ui.directive('uiViewPortal', function(){
	return {
		scope: true,
		controller: PortalCtrl,
		link: function(scope, element, attrs) {
			
			scope.parse = function(schema) {
				scope.portletCols = schema.cols || 2;
				scope.portlets = schema.items;
			};
			
			setTimeout(function(){
				element.sortable({
					handle: ".portlet-header",
					items: "> .portlet"
				});
			});
		},
		replace: true,
		transclude: true,
		template:
		'<div class="portal" ng-transclude>'+
			'<div ui-view-portlet x-action="{{portlet.action}}" ng-repeat="portlet in portlets"></div>'+
		'</div>'
	};
});

PortletCtrl.$inject = ['$scope', '$element', 'MenuService', 'DataSource', 'ViewService'];
function PortletCtrl($scope, $element, MenuService, DataSource, ViewService) {
	
	$scope.initPortlet = function(action) {

		MenuService.action(action).success(function(result){
			if (_.isEmpty(result.data)) {
				return;
			}
			var view = result.data[0].view;
			
			$scope._viewParams = view;
			$scope._viewAction = action;
			
			ViewCtrl.call(self, $scope, DataSource, ViewService);

			$scope.title = view.title;
			$scope.parsePartnet(view);
		});
	};
}

ui.directive('uiViewPortlet', ['$compile', function($compile){
	return {
		scope: true,
		require: '^uiViewPortal',
		controller: PortletCtrl,
		link: function(scope, element, attrs, portal) {
			setTimeout(function(){
				scope.initPortlet(attrs.action);
			});
			
			var initialized = false;
			scope.parsePartnet = function(view) {
				if (initialized) {
					return;
				}
				initialized = true;
				
				scope.noFilter = !portlet.canSearch;

				var template = $compile($('<div ui-portlet-' + view.viewType + '></div>'))(scope);
				element.find('.portlet-content:first').append(template);
				
				scope.show();
			};
			
			scope.onPortletToggle = function(event) {
				var e = $(event.target);
				e.toggleClass('icon-chevron-up icon-chevron-down');
				element.toggleClass('portlet-minimized');
				if (e.hasClass('icon-chevron-up')) {
					$.event.trigger('adjustSize');
				}
			};
			
			var portlet = scope.portlet;
			var cols = scope.portletCols;
			var colSpan = portlet.colSpan || 1;
			var rowSpan = portlet.rowSpan || 1;

			var width = 100;
			var height = 250 * rowSpan;
			
			width = (width / cols) * colSpan;
			
			element.width(width + '%').height(height);
		},
		replace: true,
		template:
		'<div class="portlet">'+
			'<div class="portlet-body stackbar">'+
				'<div class="portlet-header navbar">'+
					'<div class="navbar-inner">'+
						'<div class="container-fluid">'+
							'<span class="brand" href="" ng-bind-html-unsafe="title"></span>'+
							'<span class="icons-bar pull-right">'+
								'<i ng-click="onRefresh()" title="{{\'Refresh\' | t}}" class="icon-refresh"></i>'+
								'<i ng-click="onPortletToggle($event)" title="{{\'Toggle\' | t}}" class="icon-chevron-up"></i>'+
							'</span>'+
						'</div>'+
					'</div>'+
				'</div>'+
				'<div class="portlet-content"></div>'+
			'</div>'+
		'</div>'
	};
}]);

})();