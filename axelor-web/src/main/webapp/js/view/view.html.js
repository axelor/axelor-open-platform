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
(function(){

var ui = angular.module('axelor.ui');

this.HtmlViewCtrl = HtmlViewCtrl;

HtmlViewCtrl.$inject = ['$scope', '$element'];
function HtmlViewCtrl($scope, $element) {

	var views = $scope._views;
	$scope.view = views['html'];
};

var directiveFn = function(){
	return {
		controller: HtmlViewCtrl,
		replace: true,
		template:
		'<div class="iframe-container">'+
			'<iframe ng-src="{{view.name || view.resource}}" frameborder="0" scrolling="auto"></iframe>'+
		'</div>'
	};
};

ui.directive('uiViewHtml', directiveFn);
ui.directive('uiPortletHtml', directiveFn);

}).call(this);
