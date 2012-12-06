angular.module('axelor.ui').directive('uiSplitter', function() {
	return {
		restrict: 'EA',
		transclude: true,
		scope: {
			orientation: '@',
			position: '@',
			inverse: '@'
		},
		link: function(scope, elem, attrs) {
			
			scope.$watch('orientation', function(){
				elem.splitter({
					orientation: scope.orientation,
					position: scope.position,
					inverse: scope.inverse == 'true',
					toggleOn: 'click'
				}).on('splitter:dragstop', function(){
					$.event.trigger('adjustSize');
				});
			});
		},
		template: '<div ng-transclude></div>',
		replace: true
	};
});
