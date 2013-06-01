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
