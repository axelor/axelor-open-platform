/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
angular.module('axelor.ui').directive('navTree', function() {
	return {
		restrict: 'EA',
		require: '?ngModel',
		scope: {
			itemClick: '&' 
		},

		controller: ['$scope', '$element', 'MenuService', function($scope, $element, MenuService) {

			function canAccept(item) {
				return item.left || item.left === undefined;
			}

			$scope.load = function(parent, successFn) {

				var name = parent ? parent.name : null;

				MenuService.get(name).success(function(res){
					var items = res.data;
					if (successFn) {
						return successFn(items);
					}

					if (name == null) {
						items = _.filter(items, canAccept);
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
