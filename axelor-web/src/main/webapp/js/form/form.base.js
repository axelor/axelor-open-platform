/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
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

	function link(scope, element, attrs, controller) {
		
		element.addClass(this.css).parent().addClass(this.cellCss);
		element.data('$attrs', attrs); // store the attrs object for event handlers

		var getViewDef = this.getViewDef || scope.getViewDef || function() {return {}; };

		var field = getViewDef.call(scope, element);
		var props = _.pick(field, ['readonly', 'required', 'hidden']);
		var state = _.clone(props);
		
		if (field.css) {
			element.addClass(field.css);
		}
		if (field.width && field.width !== '*') {
			element.width(field.width);
		}
		
		scope.$events = {};
		scope.field = field || {};
		
		scope.attr = function(name) {
			if (arguments.length > 1) {
				state[name] = arguments[1];
				if (name === "highlight") {
					setHighlight(state.highlight);
				}
			}
			return state[name];
		};
		
		scope.$on("on:edit", function(e, rec){
			if (_.isEmpty(rec)) {
				state = _.clone(props);
			}
		});

		scope.isRequired = function() {
			return this.attr("required") || false;
		};
		
		scope.isReadonly = function() {
			var parent = this.$parent;
			if (parent && parent.isReadonly && parent.isReadonly()) {
				return true;
			}
			if (scope.isEditable && !scope.isEditable()) {
				return true;
			}
			return this.attr("readonly") || false;
		};

		scope.isHidden = function() {
			return this.attr("hidden") || false;
		};

		scope.fireAction = function(name, success, error) {
			var handler = this.$events[name];
			if (handler) {
				return handler().then(success, error);
			}
		};

		if (angular.isFunction(this._link_internal)) {
			this._link_internal.call(this, scope, element, attrs, controller);
		}
		if (angular.isFunction(this.init)) {
			this.init.call(this, scope);
		}
		if (angular.isFunction(this.link)) {
			this.link.call(this, scope, element, attrs, controller);
		}

		function hideWidget(hidden) {
			var elem = element,
				parent = elem.parent('td'),
				label = elem.data('label') || $(),
				label_parent = label.parent('td');

			// label scope should use same isHidden method (#1514)
			var lScope = label.data('$scope');
			if (lScope && lScope.isHidden !== scope.isHidden) {
				lScope.isHidden = scope.isHidden;
			}

			if (parent.size() == 0)
				parent = elem;
			if (label_parent.size())
				label = label_parent;
			return hidden ? parent.add(label).hide() : parent.add(label).show();
		}

		var hideFn = _.contains(this.handles, 'isHidden') ? angular.noop : hideWidget;

		var hiddenSet = false;
		scope.$watch("isHidden()", function(hidden, old) {
			if (hiddenSet && hidden === old) return;
			hiddenSet = true;
			return hideFn(hidden);
		});
		
		var readonlySet = false;
		scope.$watch("isReadonly()", function(readonly, old) {
			if (readonlySet && readonly === old) return;
			readonlySet = true;
			return element.toggleClass("readonly", readonly);
		});
		
		function setHighlight(highlight) {
			var params = field.hilite,
				label = null;
			if (params && params.css) {
				label = element.data('label') || $();
				element.toggleClass(params.css, highlight);
				label.toggleClass(params.css.replace(/(hilite-[^-]+\b(?!-))/g, ''), highlight);
			}
		}

		this.prepare(scope, element, attrs, controller);
		
		scope.$evalAsync(function() {
			if (scope.isHidden()) {
				hideFn(true);
			}
		});
	}

	return angular.bind(this, link);
};

ui.formDirective = function(name, object) {

	if (object.compile === undefined) {
		object.compile = angular.bind(object, function(element, attrs){
			return ui.formCompile.apply(this, arguments);
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
					if (scope.validate) {
						model.$parsers.unshift(function(viewValue) {
							var valid = scope.validate(viewValue);
							model.$setValidity('valid', valid);
							return valid ? viewValue : undefined;
						});
					}
					// focus the first input field
					if (scope.$elem_editable.is('.input-append,.picker-input')) {
						scope.$elem_editable.on('click', '.btn, i', function(){
							scope.$elem_editable.find('input:first').focus();
						});
					}

					if (scope.$elem_editable.is(':input')) {
						scope.$elem_editable.attr('placeholder', scope.field.placeholder);
					}

					if (scope.$elem_editable.is('.picker-input')) {
						scope.$elem_editable.find(':input:first').attr('placeholder', scope.field.placeholder);
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
			
			scope.$watch("isReadonly()", function(readonly) {
				if (readonly && showReadonly()) {
					return;
				}
				return showEditable();
			});
			scope.$watch("isRequired()", function(required, old) {
				if (required === old) return;
				var elem = element,
					label = elem.data('label') || $();
				if (label) {
					label.toggleClass('required', required);
				}
				attrs.$set('required', required);
			});
			
			if (scope.field && scope.field.validIf) {
				scope.$watch("attr('valid')", function(valid) {
					if (valid === undefined) return;
					model.$setValidity('invalid', valid);
				});
			}
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
			var onChange = this.$events.onChange;

			model.$setViewValue(val);
			this.text = txt;

			model.$render();
			if (onChange && fireOnChange) {
				onChange();
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

		// Clear invalid fields (use $setPrestine of angular.js 1.1)
		scope.$on('on:new', function(e, rec) {
			if (!model.$valid && model.$viewValue) {
				model.$viewValue = undefined;
				model.$render();
			}
		});
	},

	link: function(scope, element, attrs, model) {

	},
	
	link_editable: function(scope, element, attrs, model) {
		
		scope.$render_editable = function() {
			var value = this.format(this.getValue());
			element.val(value);
		};

		function bindListeners() {
			var onChange = scope.$events.onChange || angular.noop,
				onChangePending = false;

			function listener() {
				var value = _.str.trim(element.val()) || null;
				if (value !== model.$viewValue) {
					scope.$apply(function() {
						var val = scope.parse(value);
						var txt = scope.format(value);

						model.$setViewValue(val);
						scope.text = txt;
					});
					onChangePending = true;
				}
			}
			
			element.bind('input', listener);
			
			element.change(listener);
			
			element.blur(function(e){
				if (onChangePending) {
					onChangePending = false;
					setTimeout(onChange);
				}
			});
		}

		if (element.is(':input')) {
			setTimeout(bindListeners);
		}

		scope.$render_editable();
	},

	link_readonly: function(scope, element, attrs, model) {

	},

	template_editable: '<input type="text">',

	template_readonly: '<span class="display-text">{{text}}</span>',
	
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
	var widget = _.str.capitalize(name.replace(/^ui/, ''));
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
	var name = type,
		widget = registry["ui" + name] || registry[name];
	if (!widget) {
		name = _.str.classify(name);
		widget = registry["ui" + name] || registry[name];
	}
	if (widget) {
		widget = widget.replace(/^ui/, '');
		return _.chain(widget).underscored().dasherize().value();
	}
	return null;
};

})(this);
