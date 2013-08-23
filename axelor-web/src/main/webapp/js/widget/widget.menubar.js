(function() {

var module = angular.module('axelor.ui');

MenuBarCtrl.$inject = ['$scope', '$element'];
function MenuBarCtrl($scope, $element) {

	this.isDivider = function(item) {
		return !item.title && !item.icon;
	};
}

module.directive('uiMenuBar', function() {

	return {
		replace: true,
		controller: MenuBarCtrl,
		scope: {
			menus: '=',
			handler: '='
		},
		link: function(scope, element, attrs, ctrl) {

			ctrl.handler = scope.handler;

			var unwatch = scope.$watch('menus', function(menus, old) {
				if (!menus || menus.length == 0 || menus === old) {
					return;
				}

				unwatch();

				setTimeout(function() {
					element.find('.dropdown-toggle').dropdown();
				}, 100);
			});

			scope.canShowTitle = function(menu) {
				return menu.showTitle === null || menu.showTitle === undefined || menu.showTitle;
			};
		},

		template:
			"<ul class='nav menu-bar'>" +
				"<li class='menu dropdown' ng-repeat='menu in menus'>" +
					"<a href='' class='dropdown-toggle' data-toggle='dropdown'>" +
						"<img ng-show='menu.icon != null' ng-src='{{menu.icon}}'> " +
						"<span ng-show='canShowTitle(menu)'>{{menu.title}}</span> " +
						"<b class='caret'></b>" +
					"</a>" +
					"<ul ui-menu='menu'></ul>" +
				"</li>" +
			"</ul>"
	};
});

module.directive('uiMenu', function() {

	return {
		replace: true,
		require: '^uiMenuBar',
		scope: {
			menu: '=uiMenu'
		},
		link: function(scope, element, attrs, ctrl) {

		},
		template:
			"<ul class='dropdown-menu'>" +
				"<li ng-repeat='item in menu.items' ui-menu-item='item'>" +
			"</ul>"
	};
});

module.directive('uiMenuItem', ['ActionService', function(ActionService) {

	return {
		replace: true,
		require: '^uiMenuBar',
		scope: {
			item: '=uiMenuItem'
		},
		link: function(scope, element, attrs, ctrl) {

			var item = scope.item;
			var handler = null;

			scope.field  = item;
			scope.isDivider = ctrl.isDivider(item);

			if (item.action) {
				handler = ActionService.handler(ctrl.handler, element, {
					action: item.action
				});
			}

			scope.onClick = function(e) {
				$(e.srcElement).parents('.dropdown').dropdown('toggle');
				if (item.action) {
					return handler.onClick();
				}
			};

			scope.cssClass = function() {
				if (scope.isDivider) {
					return 'divider';
				}
			};
		},
		template:
			"<li ng-class='cssClass()'>" +
				"<a href='' ng-show='!isDivider' ng-click='onClick($event)'>{{item.title}}</a>" +
			"</li>"
	};
}]);

}).call(this);