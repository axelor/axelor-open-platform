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
(function() {

var ui = angular.module('axelor.ui');

ui.directive('uiSplitter', function() {
	return {
		restrict: 'EA',
		transclude: true,
		scope: {
			orientation: '@',
			position: '@',
			inverse: '@',
			toggleOn: '@'
		},
		link: function(scope, elem, attrs) {
			
			scope.$watch('orientation', function(){
				elem.splitter({
					orientation: scope.orientation,
					position: scope.position,
					inverse: scope.inverse == 'true',
					toggleOn: scope.toggleOn || 'click'
				}).on('splitter:dragstop', function(){
					axelor.$adjustSize();
				});
			});
		},
		template: '<div ng-transclude></div>',
		replace: true
	};
});

ui.directive('navMenuToggle',  function() {

	function hide(splitter) {
		splitter.dragger.add(splitter.elemOne).hide().width(0);
		splitter._adjust(true);
	}
	
	function toggle(splitter) {
		splitter.dragger.css('left', 0);
		splitter._adjust(true);
	}

	return function(scope, element, attrs) {
		
		var loaded = false;
		
		scope.$parent.$watch('app.navigator', function(navigator) {
			if (loaded || navigator === undefined) {
				return;
			}
			loaded = true;
			
			element.css('visibility', '');
			
			var state = "visible";
			if (navigator) {
				state = navigator;
			}
			_.delay(function() {
				if (state === 'visible') return;
				var splitter = element.data('splitter');
				if (state === 'hidden') hide(splitter);
				if (state === 'collapse') toggle(splitter);
			});
		});
	};
});

}).call(this);
