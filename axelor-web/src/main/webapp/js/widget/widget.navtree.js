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
			var searchItems = [];

			var handler = $scope.itemClick();

			function canAccept(item) {
				return item.left || item.left === undefined;
			}

			this.onClick = function (e, menu) {
				if (menu.isFolder) {
					return;
				}
				handler(e, menu);
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
						item.icon = null;
					} else if (canAccept(item)){
						menus.push(item);
					}
				});

				items.forEach(function (item) {
					if (item.children.length === 0) {
						delete item.children;
						var label = item.title;
						var parent = nodes[item.parent];
						var lastParent;
						while (parent) {
							lastParent = parent;
							parent = nodes[parent.parent];
							if (parent) {
								label = lastParent.title + "/" + label;
							}
						}
						searchItems.push(_.extend({
							title: item.title,
							label: label,
							action: item.action,
							category: lastParent ? lastParent.name : '',
							categoryTitle: lastParent ? lastParent.title : ''
						}));
					}
				});
				$scope.menus = menus;
				$scope.searchItems = searchItems;
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
			var input = element.find('input');
			scope.showSearch = !!axelor.device.mobile;
			scope.toggleSearch = function (show) {
				input.val('');
				if (!axelor.device.mobile) {
					scope.showSearch = show === undefined ? !scope.showSearch : show;
				}
			}
			scope.onShowSearch = function () {
				scope.showSearch = true;
				setTimeout(function () {
					input.val('').focus();
				});
			}

			input.attr('placeholder', _t('Search...'));
			input.blur(function (e) {
				scope.$timeout(function () {
					scope.toggleSearch(false);
				});
			});
			input.keydown(function (e) {
				if (e.keyCode ===  27) { // escape
					scope.$timeout(function () {
						scope.toggleSearch(false);
					});
				}
			});

			function search(request, response) {
				var term = request.term;
				var items = _.filter(scope.searchItems, function (item) {
					var text = item.categoryTitle + '/' + item.label;
					var search = term;
					if (search[0] === '/') {
						search = search.substring(1);
						text = item.title;
					}
					text = text.replace('/', '').toLowerCase();
					if (search[0] === '"' || search[0] === '=') {
						search = search.substring(1);
						if (search.indexOf('"') === search.length - 1) {
							search = search.substring(0, search.length - 1);
						}
						return text.indexOf(search) > -1;
					}
					var parts = search.toLowerCase().split(/\s+/);
					for (var i = 0; i < parts.length; i++) {
						if (text.indexOf(parts[i]) === -1) {
							return false;
						}
					}
					return parts.length > 0;
				});
				response(items);
			}

			MenuService.all().success(function (res) {
				ctrl.load(res.data);
				input.autocomplete({
					source: search,
					select: function (e, ui) {
						ctrl.onClick(e, ui.item);
						scope.$timeout(function () {
							scope.toggleSearch(false);
						});
					},
					appendTo: element.parent()
				});
				
				input.data('autocomplete')._renderMenu = function (ul, items) {
					var all = _.groupBy(items, 'category');
					var that = this;
					scope.menus.forEach(function (menu) {
						var found = all[menu.name];
						if (found) {
							ul.append("<li class='ui-menu-category'>"+ menu.title +"</li>");
				            found.forEach(function (item) {
				            	that._renderItemData(ul, item);
				            });
						}
					});
				};
			});
		},
		replace: true,
		template:
			"<div>" +
				"<div class='nav-search-toggle' ng-show='!showSearch'>" +
					"<i ng-click='onShowSearch()' class='fa fa-angle-down'></i>" +
				"</div>" +
				"<div class='nav-search' ng-show='showSearch'>" +
					"<input type='text'>" +
				"</div>" +
				"<ul class='nav nav-tree'>" +
					"<li ng-repeat='menu in menus' ui-nav-sub-tree x-menu='menu'></li>" +
				"</ul>" +
			"</div>"
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
				var icon = element.find("span.nav-icon");
				if (menu.iconBackground) {
					icon.addClass("fg-white");
					if (menu.iconBackground.indexOf("#") === 0) {
						icon.css("background-color", menu.iconBackground);
					} else {
						icon.addClass("bg-" + menu.iconBackground);
					}

					// get computed color value
					var color = icon.css('background-color');
					var bright = d3.rgb(color).brighter(.3).toString();

					// use same color for vertical line
					element.css("border-left-color", color);

					// add hover effect
					element.hover(function () {
						icon.css('background-color', bright);
						element.css("border-left-color", bright);
					}, function () {
						icon.css('background-color', color)
						element.css("border-left-color", color);
					});
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

				if (!menu.isFolder) {
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
					"<span class='nav-title'>{{menu.title}}</span>" +
					"<span ng-show='menu.tag' ng-class='menu.tagCss' class='nav-tag label'>{{menu.tag}}</span>" +
				"</a>" +
			"</li>"
	};
}]);

})();
