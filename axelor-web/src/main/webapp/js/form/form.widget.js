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

		element.hide();
		element.find('[x-field],[data-field]').each(function(){
			
			var elem = $(this),
				name = elem.attr('x-field') || elem.attr('data-field');
				
			if (name && elem.attr('ui-button') === undefined) {
				if (!elem.attr('ng-model')) {
					elem.attr('ng-model', 'record.' + name);
				}
				if (!elem.attr('ng-required')) {
					// always attache a required validator to make
					// dynamic `required` attribute change effective
					elem.attr('ng-required', false);
				}
			}
			
			elem.attr('ui-actions', '');
		});
		
		return ui.formCompile.apply(this, arguments);
	},
	
	link: function(scope, element, attrs, controller) {
		
		element.on('submit', function(e) {
			e.preventDefault();
		});

		scope.$watch('record', function(rec, old) {
			if (element.is(':visible')) {
				return;
			}
			scope.ajaxStop(function() {
				element.show();
				$.event.trigger('adjustSize');
			});
		});
	}
});

})(this);
