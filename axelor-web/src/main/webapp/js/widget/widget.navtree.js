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

"use strict";

var ui = angular.module('axelor.ui');

ui.directive('navTree', ['MenuService', function(MenuService) {

	return {
		scope: {
			itemClick: "&"
		},
		controller: ["$scope", function ($scope) {

			var items = [];
			var menus = [];
			var nodes = {};

			var handler = $scope.itemClick();

			function canAccept(item) {
				return item.left || item.left === undefined;
			}

			this.onClick = function (e, menu) {
				if (menu.isFolder) {
					return;
				}
				handler(e, menu);
				MenuService.tags().success(function (res) {
					this.update(res.data);
				}.bind(this));
			}

			this.load = function (data) {
				if (!data || !data.length) return;

				items = data;
				items.forEach(function (item) {
					nodes[item.name] = item;
					item.children = [];
				});

				items.forEach(function (item) {
					var node = nodes[item.parent];
					if (node) {
						node.children.push(item);
						item.icon = null;
					} else if (canAccept(item)){
						menus.push(item);
					}
				});

				items.forEach(function (item) {
					if (item.children.length === 0) {
						delete item.children;
					}
				});
				$scope.menus = menus;
			};

			this.update = function (data) {
				if (!data || data.length === 0) return;
				data.forEach(function (item) {
					var node = nodes[item.name];
					if (node) {
						node.tag = item.tag;
						node.tagStyle = item.tagStyle;
						if (node.tagStyle) {
							node.tagCss = "label-" + node.tagStyle;
						}
					}
				});
			};
		}],
		link: function (scope, element, attrs, ctrl) {
			
			MenuService.all().success(function (res) {
				ctrl.load(res.data);
			});
		},
		replace: true,
		template:
			"<ul class='nav nav-tree'>" +
				"<li ng-repeat='menu in menus' nav-sub-tree x-menu='menu'></li>" +
			"</ul>"
	};
}]);

ui.directive('navSubTree', ['$compile', function ($compile) {

	return {
		scope: {
			menu: "="
		},
		require: "^navTree",
		link: function (scope, element, attrs, ctrl) {
			var menu = scope.menu;
			if (menu.icon && menu.icon.indexOf('fa') === 0) {
				menu.fa = menu.icon;
				delete menu.icon;
			}
			if (menu.tagStyle) {
				menu.tagCss = "label-" + menu.tagStyle;
			}
			if (menu.children) {
				var sub = $(
					"<ul class='nav nav-sub-tree'>" +
						"<li ng-repeat='child in menu.children' nav-sub-tree x-menu='child'></li>" +
					"</ul>");
				sub = $compile(sub)(scope);
				sub.appendTo(element);
			}

			var animation = false;

			function show(el) {

				var parent = el.parent("li");

				if (animation || parent.hasClass('open')) {
					return;
				}

				function done() {
					parent.addClass('open');
					parent.removeClass('animate');
					el.height('');
					animation = false;
				}

				hide(parent.siblings("li.open").children('ul'));

				animation = true;

				parent.addClass('animate');
				el.height(el[0].scrollHeight);

				setTimeout(done, 300);
			}

			function hide(el) {

				var parent = el.parent("li");

				if (animation || !parent.hasClass('open')) {
					return;
				}

				function done() {
					parent.removeClass('open');
					parent.removeClass('animate');
					animation = false;
				}

				animation = true;

				el.height(el.height())[0].offsetHeight;
				parent.addClass('animate');
				el.height(0);

				setTimeout(done, 300);
			}

			element.on('click', '> a', function (e) {
				e.preventDefault();

				if (animation) return;

				var $list = element.children('ul');

				element.parents('.nav-tree').find('li.active').not(element).removeClass('active');
				element.addClass('active');

				if (!menu.isFolder) {
					scope.applyLater(function () {
						ctrl.onClick(e, menu);
					});
				}
				if ($list.size() === 0) return;
				if (element.hasClass('open')) {
					hide($list)
				} else {
					show($list);
				}
			});
		},
		replace: true,
		template:
			"<li ng-class='{folder: menu.children, tagged: menu.tag }'>" +
				"<a href='#'>" +
					"<img class='nav-image' ng-if='menu.icon' ng-src='{{menu.icon}}'></img>" +
					"<i ng-if='menu.fa' class='nav-icon fa' ng-class='menu.fa'></i>" +
					"<span class='nav-title'>{{menu.title}}</span>" +
					"<span ng-show='menu.tag' ng-class='menu.tagCss' class='nav-tag label'>{{menu.tag}}</span>" +
				"</a>" +
			"</li>"
	}
}]);

})();
