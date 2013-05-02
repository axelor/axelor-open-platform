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
			'<iframe src="{{view.name || view.resource}}" frameborder="0" scrolling="auto"></iframe>'+
		'</div>'
	};
};

ui.directive('uiViewHtml', directiveFn);
ui.directive('uiPortletHtml', directiveFn);

}).call(this);
