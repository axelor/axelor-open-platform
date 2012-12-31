(function(){

var ui = angular.module('axelor.ui');

/**
 * Perform common compile operations.
 * 
 * example:
 *    ui.formCompile.call(this, element, attrs)
 */
ui.formCompile = function(element, attrs, linkerFn) {

	var showTitle = attrs.showTitle || this.showTitle,
		title = attrs.title || attrs.field;

	attrs.$set('show-title', showTitle, true, 'x-show-title');
	if (title)
		attrs.$set('title', title, true, 'x-title');
	if (this.cellCss)
		attrs.$set('x-cell-css', this.cellCss);
	
	// fix the size of bootstrap's input-append widget
	if (element.hasClass('input-append')) {
		var buttons = element.find('.btn'),
			n = buttons.size(),
			w = 28 * n; // assume icon only buttons

		element.css('position', 'relative');
		element.find('input:last')
	  		   .css('width', 'inherit')
	  		   .css('z-index', 'inherit !important')
	  		   .css('padding-right', w - n);
		element.find('.btn').each(function(i){
			$(this).css('position', 'absolute')
				   .css('right', 27 * (n - i - 1));
		});
	}
		
	return angular.bind(this, function(scope, element, attrs, controller) {
		element.addClass(this.css).parent().addClass(this.cellCss);
		element.data('$attrs', attrs); // store the attrs object for event handlers

		if (element.is('.input-append,.picker-input')) { // focus the first input field
			element.on('click', '.btn, i', function(){
				element.find('input:first').focus();
			});
		}
		
		if (scope.getViewDef) {
			var field = scope.getViewDef(element);
			if (field.hidden) {
				setTimeout(function(){
					scope.setHidden(element);
				});
			}
			if (field.readonly) {
				setTimeout(function(){
					scope.setReadonly(element);
				});
			}
		}

		if (angular.isFunction(linkerFn)) {
			linkerFn.call(this, scope, element, attrs, controller);
		}
		
		if (angular.isFunction(this.link)) {
			this.link.call(this, scope, element, attrs, controller);
		}
	});
};

ui.formDirective = function(name, object) {

	if (object.compile == undefined) {
		object.compile = angular.bind(object, function(element, attrs){
			return ui.formCompile.call(this, element, attrs);
		});
	}
	
	if (object.restrict == undefined) {
		object.restrict = 'EA';
	}
	
	if (object.template && object.replace == undefined) {
		object.replace = true;
	}
	
	if (object.cellCss == undefined) {
		object.cellCss = 'form-item';
	}

	return ui.directive(name, function() {
		return object;
	});
};

})(this);
