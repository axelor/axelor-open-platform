/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
(function() {

/* jshint validthis: true */

"use strict";

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

		var getViewDef = this.getViewDef || scope.getViewDef || function() { return {}; };

		var field = getViewDef.call(scope, element);
		var props = _.pick(field, ['readonly', 'required', 'hidden', 'title']);
		var state = _.clone(props);
		
		if (field.css) {
			element.addClass(field.css);
		}
		if (field.width && field.width !== '*' && !element.is('label')) {
			element.width(field.width);
		}
		if (field.translatable) {
			element.addClass("translatable");
		}
		
		scope.$events = {};
		scope.field = field || {};
		
		scope.$$readonly = undefined;
		
		scope.attr = function(name) {
			if (arguments.length > 1) {
				var old = state[name];
				state[name] = arguments[1];
				if (name === "highlight") {
					setHighlight(state.highlight);
				}
				if (old !== state[name]) {
					scope.$broadcast("on:attrs-changed", {
						name: name,
						value: state[name]
					});
				}
			}
			var res = state[name];
			if (res === undefined) {
				res = field[name];
			}
			return res;
		};
		
		scope.$on("on:edit", function(e, rec) {
			if (_.isEmpty(rec)) {
				state = _.clone(props);
			}
			state["force-edit"] = false;
			scope.$$readonly = scope.$$isReadonly();
		});
		
		scope.$on("on:attrs-changed", function(event, attr) {
			if (attr.name === "readonly" || attr.name === "force-edit") {
				scope.$$readonly = scope.$$isReadonly();
			}
			if (attr.name === "readonly") {
				element.attr("x-readonly", scope.$$readonly);
			}
		});
		
		scope.$watch("isEditable()", function(editable, old) {
			if (editable === undefined) return;
			if (editable === old) return;
			scope.$$readonly = scope.$$isReadonly();
		});

		scope.isRequired = function() {
			return this.attr("required") || false;
		};
		
		scope.isReadonlyExclusive = function() {
			var parent = this.$parent || {};
			var readonly = this.attr("readonly");

			if (scope._isPopup && !parent._isPopup) {
				return readonly || false;
			}
			if (parent.isReadonlyExclusive && parent.isReadonlyExclusive()) {
				return true;
			}
			if (readonly !== undefined) {
				return readonly || false;
			}

			return readonly || false;
		};
		
		scope.isReadonly = function() {
			if (scope.$$readonly === undefined) {
				scope.$$readonly = scope.$$isReadonly();
			}
			return scope.$$readonly;
		};
		
		scope.$$isReadonly = function() {
			if ((this.hasPermission && !this.hasPermission('read')) || this.isReadonlyExclusive()) {
				return true;
			}
			if (!this.attr("readonly") && this.attr("force-edit")) {
				return false;
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
				parent = elem.parent('td,.form-item'),
				label = elem.data('label') || $(),
				label_parent = label.parent('td,.form-item'),
				isTable = parent.is('td');

			// label scope should use same isHidden method (#1514)
			var lScope = label.data('$scope');
			if (lScope && lScope.isHidden !== scope.isHidden) {
				lScope.isHidden = scope.isHidden;
			}
			
			elem = isTable && parent.size() ? parent : elem;
			label = isTable && label_parent.size() ? label_parent : label;

			if (!isTable) {
				parent.toggleClass("form-item-hidden", hidden);
			}

			if (hidden) {
				elem.add(label).hide();
			} else {
				elem.add(label).show();
			}

			return axelor.$adjustSize();
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
			element.toggleClass("readonly", readonly);
			element.toggleClass("editable", !readonly);
			if (scope.canEdit) {
				element.toggleClass("no-edit", scope.canEdit() === false);
			}
		});
		
		function setHighlight(args) {

			function doHilite(params, passed) {
				var label = element.data('label') || $();
				element.toggleClass(params.css, passed);
				label.toggleClass(params.css.replace(/(hilite-[^-]+\b(?!-))/g, ''), passed);
			}
			
			_.each(field.hilites, function(p) {
				if (p.css) doHilite(p, false);
			});
			
			if (args && args.hilite && args.hilite.css) {
				doHilite(args.hilite, args.passed);
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
	
	if (object.template && !object.replace) {
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
			
			if (!this.template_editable && !this.template_readonly) {
				return;
			}
			
			scope.$elem_editable = null;
			scope.$elem_readonly = null;
			
			function showEditable() {
				var template_editable = self.template_editable;
				if (scope.field && scope.field.editor) {
					template_editable = $('<div ui-panel-editor>');
				}
				if (_.isFunction(self.template_editable)) {
					template_editable = self.template_editable(scope);
				}
				if (!template_editable) {
					return false;
				}
				if (!scope.$elem_editable) {
					scope.$elem_editable = $compile(template_editable)(scope);
					if (self.link_editable) {
						self.link_editable.call(self, scope, scope.$elem_editable, attrs, model);
					}
					if (scope.validate) {
						model.$validators.valid = function(modelValue, viewValue) {
							return !!scope.validate(viewValue);
						};
					}
					// focus the first input field
					if (scope.$elem_editable.is('.input-append,.picker-input')) {
						scope.$elem_editable.on('click', '.btn, i', function(){
							if (!axelor.device.mobile) {
								scope.$elem_editable.find('input:first').focus();
							}
						});
					}

					if (scope.$elem_editable.is(':input')) {
						scope.$elem_editable.attr('placeholder', scope.field.placeholder);
					}

					if (scope.$elem_editable.is('.picker-input:not(.tag-select)')) {
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
				var field = scope.field || {};
				var template_readonly = self.template_readonly;
				if (field.viewer) {
					template_readonly = field.viewer;
					scope.$image = function (fieldName, imageName) { return ui.formatters.$image(scope, fieldName, imageName); };
					scope.$fmt = function (fieldName, fieldValue) {
						var args = [scope, fieldName];
						if (arguments.length > 1) {
							args.push(fieldValue);
						}
						return ui.formatters.$fmt.apply(null, args);
					};
				} else if (field.editor && field.editor.viewer) {
					template_readonly = $('<div ui-panel-editor>');
				}
				if (_.isFunction(self.template_readonly)) {
					template_readonly = self.template_readonly(scope);
				}
				if (!template_readonly) {
					return false;
				}
				if (_.isString(template_readonly)) {
					template_readonly = template_readonly.trim();
					if (template_readonly[0] !== '<') {
						template_readonly = '<span>' + template_readonly + '</span>';
					}
				}
				if (!scope.$elem_readonly) {
					scope.$elem_readonly = $compile(template_readonly)(scope);
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
			
			scope.$on('$destroy', function() {
				if (scope.$elem_editable) {
					scope.$elem_editable.remove();
					scope.$elem_editable = null;
				}
				if (scope.$elem_readonly) {
					scope.$elem_readonly.remove();
					scope.$elem_readonly = null;
				}
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
			var onChange = this.$events.onChange;

			model.$setViewValue(val);
			this.text = txt;

			model.$render();
			if (onChange && fireOnChange) {
				onChange();
			}
		};

		scope.getValue = function() {
			if (model) {
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
			
			var field = scope.field || {};
			if (!field.bind) {
				element.bind('input', listener);
			}

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

	if (!base) {
		return obj;
	}

	function overridden(name) {
		return name !== "controller" &&
			_.isFunction(last[name]) && !last[name].$inject &&
			_.isFunction(base[name]);
	}

	function override(name, fn){
		return function() {
			var tmp = this._super;
			this._super = base[name];
			var ret = fn.apply(this, arguments);
			this._super = tmp;
			return ret;
		};
	}

	for(var name in last) {
		if (overridden(name)) {
			obj[name] = override(name, obj[name]);
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

})();
