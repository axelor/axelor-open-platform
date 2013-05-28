(function(){

var ui = angular.module('axelor.ui');

var ProgressMixin = {

	css: 'progress-item',
	cellCss: 'form-item progress-item',
	
	link_readonly: function(scope, element, attrs, model) {
		
		scope.$watch("getValue()", function(value, old) {
			var width = value || 0;
			var css = "progress-striped";
			
			if (width < 50) {
				css += " progress-danger active";
			} else if (width < 100) {
				css += " progress-warning active";
			} else {
				css += " progress-success";
			}
			
			scope.css = css;
			scope.width = width;
		});
	},
	
	template_readonly:
	'<div class="progress {{css}}">'+
	  '<div class="bar" style="width: {{width}}%;"></div>'+
	'</div>'
};

/**
 * The Progress widget with integer input.
 * 
 */
ui.formInput('Progress', 'Integer', _.extend({}, ProgressMixin));

/**
 * The Progress widget with selection input.
 * 
 */
ui.formInput('SelectProgress', 'Select', _.extend({}, ProgressMixin));

})(this);
