(function(){

var ui = angular.module('axelor.ui');

/**
 * The Boolean input widget.
 */
ui.formInput('Boolean', {
	
	css: 'boolean-item',
	
	cellCss: 'form-item boolean-item',
	
	link_editable: function(scope, element, attrs, model) {

		var onChange = scope.$events.onChange || angular.noop;
		
		scope.$render_editable = function() {
			element[0].checked = scope.parse(model.$viewValue);
		};
		
		element.click(function(){
			scope.setValue(this.checked);
			setTimeout(function(){
				scope.$apply();
				setTimeout(onChange);
			});
		});
	},
	template_editable: '<input type="checkbox">',
	template_readonly: '<input type="checkbox" disabled="disabled" ng-checked="text">'
});

})(this);
