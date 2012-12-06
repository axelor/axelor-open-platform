(function(){

var ui = angular.module('axelor.ui');

FormController.$inject = ['$scope', '$element', '$attrs'];
function FormController(scope, element, attrs) {
	//TODO: implement form widget controller
};

/**
 * The Form widget.
 *
 */
var Form = {

	priority: 100,
	css: "dynamic-form",

	compile: function(element, attrs) {

		element.find('[x-field],[data-field]').each(function(){
			
			var elem = $(this),
				name = elem.attr('x-field') || elem.attr('data-field');

			if (name && !elem.attr('ng-model')) {
				elem.attr('ng-model', 'record.' + name);
			}
			
			elem.attr('ui-actions', '');
		});
		
		return ui.formCompile.call(this, element, attrs);
	},
	
	link: function(scope, element, attrs, controller) {
		element.on('submit', function(e) {
			e.preventDefault();
		});
	}
};

// register directives
ui.formDirective('uiForm', Form);

})(this);
