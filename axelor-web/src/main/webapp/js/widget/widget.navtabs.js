angular.module('axelor.ui').directive('navTabs', function() {
	return {
		restrict: 'EA',
		replace: true,
		link: function(scope, elem, attrs) {
			scope.$watch('tabs.length', function(value, oldValue){
				if (value != oldValue) $.event.trigger('adjust');
			});
			$(elem).bsTabs();
			setTimeout(function() {
				var wrap = elem.children('.nav-tabs-wrap').css('margin-right', 100);
				var logo = $('<img src="img/axelor.png" width="100" height="32">');
				$('<a href="http://axelor.com" target="_blank"></a>').append(logo)
					.css("position", "absolute")
					.css("right", 0).css("top", 0)
					.css("border-bottom", wrap.find('ul.nav-tabs').css('border-bottom'))
					.appendTo(elem);
				
				logo.height(wrap.height() - 1);
				wrap.find('ul').height(wrap.height() - 1);
			});
		},
		templateUrl: 'partials/nav-tabs.html'
	};
});
