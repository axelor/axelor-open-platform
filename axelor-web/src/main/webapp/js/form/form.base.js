(function(){

var ui = angular.module('axelor.ui');
var widgets = {};
var registry = {};

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
	if (title) {
		attrs.$set('title', title, true, 'x-title');
	}
	if (this.cellCss) {
		attrs.$set('x-cell-css', this.cellCss);
	}

	return angular.bind(this, function(scope, element, attrs, controller) {

		element.addClass(this.css).parent().addClass(this.cellCss);
		element.data('$attrs', attrs); // store the attrs object for event handlers

		if (element.is('.input-append,.picker-input')) { // focus the first input field
			element.on('click', '.btn, i', function(){
				element.find('input:first').focus();
			});
		}

		var field = scope.getViewDef ? scope.getViewDef(element) : {};
		var props = _.pick(field, ['readonly', 'required', 'hidden']);
		var state = _.clone(props);
		
		scope.field = field || {};

		scope.attr = function(name) {
			if (arguments.length > 1) {
				return state[name] = arguments[1];
			}
			return state[name];
		};

		scope.$on("on:new", function(){
			state = _.clone(attrs);
		});
		scope.$on("on:edit", function(){
			state = _.clone(attrs);
		});

		scope.isRequired = function() {
			return this.attr("required") || false;
		};
		
		scope.isReadonly = function() {
			if (scope.view_mode !== "edit") {
				return true;
			}
			return this.attr("readonly") || false;
		};

		scope.isHidden = function() {
			return this.attr("hidden") || false;
		};

		if (angular.isFunction(this._link_internal)) {
			this._link_internal.call(this, scope, element, attrs, controller);
		}
		if (angular.isFunction(this.init)) {
			this.init.call(this, scope);
		}
		if (angular.isFunction(linkerFn)) {
			linkerFn.call(this, scope, element, attrs, controller);
		}
		if (angular.isFunction(this.link)) {
			this.link.call(this, scope, element, attrs, controller);
		}

		this.prepare(scope, element, attrs, controller);
	});
};

ui.formDirective = function(name, object) {

	if (object.compile === undefined) {
		object.compile = angular.bind(object, function(element, attrs){
			return ui.formCompile.call(this, element, attrs);
		});
	}
	
	if (object.restrict === undefined) {
		object.restrict = 'EA';
	}
	
	if (object.template && object.replace == undefined) {
		object.replace = true;
	}
	
	if (object.cellCss === undefined) {
		object.cellCss = 'form-item';
	}

	if (object.scope === undefined) {
		object.scope = true;
	}
	
	if (object.require === undefined) {
		object.require = '?ngModel';
	}

	function prepare_templates($compile) {
		
		object.prepare = angular.bind(object, function(scope, element, attrs, model) {

			var self = this;
			
			if (this.template_editable == null && this.template_readonly == null) {
				return;
			}
			
			scope.$elem_editable = null;
			scope.$elem_readonly = null;
			
			function showEditable() {
				if (!self.template_editable) {
					return false;
				}
				if (scope.$elem_editable == null) {
					scope.$elem_editable = $compile(self.template_editable)(scope);
					if (self.link_editable) {
						self.link_editable.call(self, scope, scope.$elem_editable, attrs, model);
					}
					if (object.validate) {
						model.$parsers.unshift(function(viewValue) {
							var valid = object.validate(viewValue);
							model.$setValidity('valid', valid);
							return valid ? viewValue : undefined;
						});
					}
				}
				if (scope.$elem_readonly) {
					scope.$elem_readonly.detach();
				}
				element.append(scope.$elem_editable);
				if (scope.$render_editable) scope.$render_editable();
				return true;
			}

			function showReadonly() {
				if (!self.template_readonly) {
					return false;
				}
				if (scope.$elem_readonly == null) {
					scope.$elem_readonly = $compile(self.template_readonly)(scope);
					if (self.link_readonly) {
						self.link_readonly.call(self, scope, scope.$elem_readonly, attrs, model);
					}
				}
				if (scope.$elem_editable) {
					scope.$elem_editable.detach();
				}
				element.append(scope.$elem_readonly);
				return true;
			}

			scope.$watch('view_mode', function(mode) {
				if (mode === 'edit' && showEditable()) {
					return;
				}
				return showReadonly();
			});
		});

		return object;
	}

	return ui.directive(name, ['$compile', function($compile) {
		return prepare_templates($compile);
	}]);
};

var FormItem = {
	
	css: 'form-item',
	
	cellCss: 'form-item'
};

var FormInput = {
	
	_link_internal: function(scope, element, attrs, model) {

		scope.format = function(value) {
			return value;
		};
		
		scope.parse = function(value) {
			return value;
		};
		
		scope.validate = function(value) {
			return true;
		};

		scope.setValue = function(value, fireOnChange) {

			var val = this.parse(value);
			var txt = this.format(value);
			var onChange = element.data('$onChange');

			model.$setViewValue(val);
			scope.text = txt;

			setTimeout(function(){
				model.$render();
			});
			
			if (onChange && fireOnChange) {
				setTimeout(function(){
					onChange.handle();
				});
			}
		};

		scope.getValue = function() {
			if (model != null) {
				return model.$viewValue;
			}
			return null;
		};
		
		scope.getText = function() {
			return this.text;
		};

		scope.initValue = function(value) {
			this.text = this.format(value);
		};
		
		model.$render = function() {
			scope.initValue(scope.getValue());
			if (scope.$render_editable) {
				scope.$render_editable();
			}
			if (scope.$render_readonly) {
				scope.$render_readonly();
			}
		};
	},

	link_editable: function(scope, element, attrs, model) {
		
		scope.$render_editable = function() {
			var value = this.format(this.getValue());
			element.val(value);
		};

		element.change(function(e){
			scope.setValue(element.val());
			scope.$apply();
		});
		
		scope.$render_editable();
	},

	link_readonly: function(scope, element, attrs, model) {

	},

	template_editable: '<input type="text">',

	template_readonly: '<span>{{text}}</span>',
	
	template: '<span class="form-item-container"></span>'
};

function inherit(array) {
	
	var args = _.chain(array).rest(1).flatten(true).value();
	var last = _.last(args);
	var base = null;
	var obj = {};

	_.chain(args).each(function(source, index) {
		if (_.isString(source)) {
			source = widgets[source];
		}
		if (index === args.length - 2) {
			base = source;
		}
		_.extend(obj, source);
	});

	if (base == null) {
		return obj;
	}

	function overridden(name) {
		return name !== "controller" &&
			_.isFunction(last[name]) && !last[name].$inject &&
			_.isFunction(base[name]);
	};

	for(var name in last) {
		if (overridden(name)) {
			obj[name] = (function(name, fn){
				return function() {
					var tmp = this._super;
					this._super = base[name];
					var ret = fn.apply(this, arguments);
					this._super = tmp;
					return ret;
				};
			})(name, obj[name]);
		}
	}

	return obj;
}

ui.formWidget = function(name, object) {
	var obj = inherit(arguments);
	var widget = _.str.titleize(name.replace(/^ui/, ''));
	var directive = "ui" + widget;

	registry[directive] = directive;
	_.each(obj.widgets, function(alias){
		registry[alias] = directive;
	});
	delete obj.widgets;

	widgets[widget] = _.clone(obj);

	ui.formDirective(directive, obj);
	
	return obj;
};

ui.formItem = function(name, object) {
	return ui.formWidget(name, FormItem, _.rest(arguments, 1));
};

ui.formInput = function(name, object) {
	return ui.formWidget(name, FormItem, FormInput, _.rest(arguments, 1));
};

ui.getWidget = function(type) {
	var name = _.chain(type).camelize().capitalize().value();
	var widget = registry["ui" + name] || registry[name];
	if (widget) {
		widget = widget.replace(/^ui/, '');
		return _.chain(widget).underscored().dasherize().value();
	}
	return null;
};

})(this);
