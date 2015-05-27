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
angular.module('axelor.ui').directive('navTree', function() {
	return {
		restrict: 'EA',
		require: '?ngModel',
		scope: {
			itemClick: '&' 
		},

		controller: ['$scope', '$element', 'MenuService', function($scope, $element, MenuService) {

			var hasMobileMenus = false;

			function canAccept(item) {
				if (item.left === false) {
					return false;
				}
				if (axelor.device.mobile && item.mobile === false && hasMobileMenus) {
					return false;
				}
				if (item.mobile) {
					hasMobileMenus = true;
				}
				return true;
			}

			$scope.load = function(parent, successFn) {

				var name = parent ? parent.name : null;

				MenuService.get(name).success(function(res){
					var items = _.filter(res.data, canAccept);
					if (successFn) {
						return successFn(items);
					}
					$element.navtree('addItems', items);
				});
			};
		}],
		
		link: function(scope, elem, attrs) {
			
			var onClickHandler = scope.itemClick();

			$(elem).navtree({
				idField: 'name',
				onLazyFetch: function(parent, successFn) {
					scope.load(parent, successFn);
				},
				onClick: function(e, record) {
					if (record.isFolder)
						return;
					onClickHandler(e, record);
				}
			});

			scope.load();
		},
		template: '<div></div>',
		replace: true
	};
});
