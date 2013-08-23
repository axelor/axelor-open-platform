(function() {

var module = angular.module('axelor.ui');

NavMenuCtrl.$inject = ['$scope', '$element', 'MenuService', 'NavService'];
function NavMenuCtrl($scope, $element, MenuService, NavService) {

	$scope.menus = []; 	// the first four visible menus
	$scope.more = [];	// rest of the menus

	var hasSideBar = __appSettings['application.menu'] !== 'top';

	MenuService.all().then(function(response) {
		var res = response.data,
			data = res.data;

		var all = {};
		var top = [];

		_.each(data, function(item) {
			all[item.name] = item;
			if (item.children === undefined) {
				item.children = [];
			}

		});

		_.each(data, function(item) {

			if (hasSideBar && item.parent == null && !item.top) {
				return;
			}

			if (item.parent == null) {
				return top.push(item);
			}
			var parent = all[item.parent];
			if (parent) {
				parent.children.push(item);
			}
		});

		$scope.menus = _.first(top, 5);
		$scope.more = _.rest(top, 5);

		$scope.extra = {
			title: 'More',
			children: $scope.more
		};
	});

	$scope.hasMore = function() {
		return $scope.more && $scope.more.length > 0;
	};

	this.isSubMenu = function(item) {
		return item && item.children && item.children.length > 0;
	};

	this.onItemClick = function(item) {
		if (this.isSubMenu(item) || item.isFolder) {
			return;
		}
		NavService.openTabByName(item.action);
	};
}

module.directive('navMenuBar', function() {

	return {

		replace: true,

		controller: NavMenuCtrl,

		scope: true,

		link: function(scope, element, attrs, ctrl) {

			var unwatch = scope.$watch('menus', function(menus, old) {
				if (!menus || menus.length == 0  || menus === old) {
					return;
				}

				unwatch();

				setTimeout(function() {
					element.find('.dropdown-toggle').dropdown();
					element.find('.dropdown.nav-menu').hover(function() {
						$(this).addClass('open');
					}, function() {
						$(this).removeClass('open');
					});
				}, 100);
			});
		},

		template:
			"<ul class='nav nav-menu-bar'>" +
				"<li class='nav-menu dropdown' ng-repeat='menu in menus'>" +
					"<a href='' class='dropdown-toggle' data-toggle='dropdown'>" +
						"<img ng-show='menu.icon != null' ng-src='{{menu.icon}}'> " +
						"<span>{{menu.title}}</span> " +
						"<b class='caret'></b>" +
					"</a>" +
					"<ul nav-menu='menu'></ul>" +
				"</li>" +
				"<li ng-if='hasMore()' class='nav-menu dropdown'>" +
					"<a href='' class='dropdown-toggle' data-toggle='dropdown'>" +
						"<span x-translate>More</span>" +
						"<b class='caret'></b>" +
					"</a>" +
					"<ul nav-menu='extra'></ul>" +
				"</li>" +
			"</ul>"
	};
});

module.directive('navMenu', function() {

	return {
		replace: true,
		require: '^navMenuBar',
		scope: {
			menu: '=navMenu'
		},
		link: function(scope, element, attrs, ctrl) {

		},
		template:
			"<ul class='dropdown-menu'>" +
				"<li ng-repeat='item in menu.children' nav-menu-item='item'>" +
			"</ul>"
	};
});

module.directive('navMenuItem', ['$compile', function($compile) {

	return {
		replace: true,
		require: '^navMenuBar',
		scope: {
			item: '=navMenuItem'
		},
		link: function(scope, element, attrs, ctrl) {

			var item = scope.item;

			scope.isSubMenu = ctrl.isSubMenu(item);
			scope.isActionMenu = item.action != null;

			scope.onClick = function(e, item) {
				ctrl.onItemClick(item);
				$(e.srcElement).parents('.dropdown').dropdown('toggle');
			};

			scope.cssClass = function() {
				if (ctrl.isSubMenu(item)) return 'dropdown-submenu';
				if (item.action) return 'action-menu';
			};

			if (ctrl.isSubMenu(item)) {
				$compile('<ul nav-menu="item"></ul>')(scope, function(cloned, scope) {
					element.append(cloned);
				});
			}
		},
		template:
			"<li ng-class='cssClass()'>" +
				"<a href='' ng-click='onClick($event, item)'>{{item.title}}</a>" +
			"</li>"
	};
}]);

}).call(this);