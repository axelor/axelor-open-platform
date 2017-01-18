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

ui.directive('uiNavTree', ['MenuService', 'TagService', function(MenuService, TagService) {

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
				if (menu.action && (menu.children||[]).length === 0) {
					handler(e, menu);
				}
			};

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
					} else if (canAccept(item)){
						menus.push(item);
						item.icon = item.icon || 'fa-bars';
						item.iconBackground = item.iconBackground || 'green';
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

			var that =  this;
			TagService.listen(function (data) {
				that.update(data.tags);
			});
		}],
		link: function (scope, element, attrs, ctrl) {
			
			MenuService.all().success(function (res) {
				ctrl.load(res.data);
			});
		},
		replace: true,
		template:
			"<ul class='nav nav-tree'>" +
				"<li ng-repeat='menu in menus' ui-nav-sub-tree x-menu='menu'></li>" +
			"</ul>"
	};
}]);

ui.directive('uiNavSubTree', ['$compile', function ($compile) {

	return {
		scope: {
			menu: "="
		},
		require: "^uiNavTree",
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
					"<ul class='nav ui-nav-sub-tree'>" +
						"<li ng-repeat='child in menu.children' ui-nav-sub-tree x-menu='child'></li>" +
					"</ul>");
				sub = $compile(sub)(scope);
				sub.appendTo(element);
			}

			setTimeout(function () {
				var icon = element.find("span.nav-icon:first");
				if (menu.iconBackground && icon.size() > 0) {
					var cssName = menu.parent ? 'color' : 'background-color';
					var clsName = menu.parent ? 'fg-' : 'bg-';

					if (!menu.parent) {
						icon.addClass("fg-white");
					}

					if (menu.iconBackground.indexOf("#") === 0) {
						icon.css(cssName, menu.iconBackground);
					} else {
						icon.addClass(clsName + menu.iconBackground);
					}

					// get computed color value
					var color = icon.css(cssName);
					var bright = d3.rgb(color).brighter(.3).toString();

					// add hover effect
					element.hover(function () {
						icon.css(cssName, bright);
					}, function () {
						icon.css(cssName, color)
					});

					// use same color for vertical line
					if (!menu.parent) {
						element.css("border-left-color", color);
						element.hover(function () {
							element.css("border-left-color", color);
						}, function () {
							element.css("border-left-color", bright);
						});
					}
				}
			});

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

				if (menu.action && (menu.children||[]).length === 0) {
					scope.applyLater(function () {
						ctrl.onClick(e, menu);
					});
				}
				if ($list.size() === 0) return;
				if (element.hasClass('open')) {
					hide($list);
				} else {
					show($list);
				}
			});
		},
		replace: true,
		template:
			"<li ng-class='{folder: menu.children, tagged: menu.tag }' data-name='{{menu.name}}'>" +
				"<a href='#'>" +
					"<img class='nav-image' ng-if='menu.icon' ng-src='{{menu.icon}}'></img>" +
					"<span class='nav-icon' ng-if='menu.fa'><i class='fa' ng-class='menu.fa'></i></span>" +
					"<span ng-show='menu.tag' ng-class='menu.tagCss' class='nav-tag label'>{{menu.tag}}</span>" +
					"<span class='nav-title'>{{menu.title}}</span>" +
				"</a>" +
			"</li>"
	};
}]);

})();
