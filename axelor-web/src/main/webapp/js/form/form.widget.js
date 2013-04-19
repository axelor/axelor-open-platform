(function(){

var ui = angular.module('axelor.ui');

/**
 * The Form widget.
 *
 */
ui.formWidget('Form', {

	priority: 100,
	
	css: "dynamic-form",
	
	scope: false,
	
	compile: function(element, attrs) {

		element.find('[x-field],[data-field]').each(function(){
			
			var elem = $(this),
				name = elem.attr('x-field') || elem.attr('data-field');

			if (name && !elem.attr('ng-model')) {
				elem.attr('ng-model', 'record.' + name);
			}
			if (name && !elem.attr('ng-required')) {
				// always attache a required validator to make
				// dynamic `required` attribute change effective
				elem.attr('ng-required', false);
			}
			
			elem.attr('ui-actions', '');
		});
		
		return ui.formCompile.call(this, element, attrs);
	},
	
	link: function(scope, element, attrs, controller) {
		if (!window.s) window.s = scope;
		element.on('submit', function(e) {
			e.preventDefault();
		});
	}
});

})(this);
