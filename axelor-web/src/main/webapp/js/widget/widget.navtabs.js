/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
angular.module('axelor.ui').directive('navTabs', function() {
	return {
		restrict: 'EA',
		replace: true,
		link: function(scope, elem, attrs) {
			scope.$watch('tabs.length', function(value, oldValue){
				if (value != oldValue) $.event.trigger('adjust');
			});
			elem.bsTabs();
			elem.on('contextmenu', '.nav-tabs-main > li > a', showMenu);

			var menu = elem.find('#nav-tabs-menu');
			
			menu.css({
				position: 'absolute',
				zIndex: 1000
			}).hide();
			
			function showMenu(e) {
				var tabElem = $(e.target).parents('li:first');
				var tabScope = tabElem.data('$scope');
				if (!tabScope || !tabScope.tab) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();
				
				if (!tabScope.canCloseTab(tabScope.tab)) {
					return;
				}

				var offset = elem.offset();
				menu.show().css({
					left: e.pageX - offset.left,
					top: e.pageY - offset.top
				});

				scope.current = tabScope.tab;
				$(document).on('mousedown.nav-tabs-menu', hideMenu);
			}
			
			function hideMenu(e) {
				if (menu.is(e.target) || menu.has(e.target).size() > 0) {
					scope.$evalAsync(function () {
						scope.current = null;
						menu.hide();
					});
				} else {
					scope.current = null;
					menu.hide();
				}
				$(document).off('mousedown.nav-tabs-menu');
			}
		},
		templateUrl: 'partials/nav-tabs.html'
	};
});
