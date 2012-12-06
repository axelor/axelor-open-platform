HtmlViewCtrl.$inject = ['$scope', '$element'];
function HtmlViewCtrl($scope, $element) {

	var views = $scope._views;
	$scope.view = views['html'];
}

angular.module('axelor.ui').directive('uiViewHtml', function(){
	return {
		controller: HtmlViewCtrl,
		replace: true,
		template:
		'<div class="iframe-container">'+
			'<iframe src="{{view.resource}}" frameborder="0" scrolling="auto"></iframe>'+
		'</div>'
	};
});
