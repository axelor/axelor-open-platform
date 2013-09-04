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
					setTimeout(function(){
						$.event.trigger('adjustSize');
					});
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
