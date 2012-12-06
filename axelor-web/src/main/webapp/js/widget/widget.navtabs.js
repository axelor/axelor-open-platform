angular.module('axelor.ui').directive('navTabs', function() {
	return {
		restrict: 'EA',
		replace: true,
		link: function(scope, elem, attrs) {
			scope.$watch('tabs.length', function(value, oldValue){
				if (value != oldValue) $.event.trigger('adjust');
			});
			$(elem).bsTabs();
		},
		templateUrl: 'partials/nav-tabs.html'
	};
});
