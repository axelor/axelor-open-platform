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

ui.directive('uiWidgetStates', function() {

	var handleConditional = function(scope, field, attr, conditional, nagative){

		if (!field[conditional]) {
			return;
		}

		var evalScope = scope.$new(true);

		evalScope.$moment = function(d) { return moment(d); };			// moment.js helper
		evalScope.$number = function(d) { return +d; };					// number helper
		evalScope.$popup = function() { return scope._isPopup; };		// popup detect

		evalScope.$readonly = _.bind(scope.isReadonly, scope);
		evalScope.$required = _.bind(scope.isRequired, scope);
		evalScope.$valid = _.bind(scope.isValid, scope);
		evalScope.$invalid = function() { return !evalScope.$valid(); };

		scope.$on("on:record-change", function(e, rec) {
			if (rec === scope.record) {
				handle(rec);
			}
		});

		scope.$watch("isReadonly()", watcher);
		scope.$watch("isRequired()", watcher);
		scope.$watch("isValid()", watcher);

		function watcher(current, old) {
			if (current !== old) handle(scope.record);
		}

		function handle(rec) {
			var value = evalScope.$eval(field[conditional], rec);
			if (nagative) { value = !value; };
			scope.attr(attr, value);
		}
	};

	function register(scope) {
		var field = scope.field;
		if (field == null) {
			return;
		}
		handleConditional(scope, field, "valid", "validIf");
		handleConditional(scope, field, "hidden", "hideIf");
		handleConditional(scope, field, "hidden", "showIf", true);
		handleConditional(scope, field, "readonly", "readonlyIf");
		handleConditional(scope, field, "required", "requiredIf");
		handleConditional(scope, field, "collapse", "collapseIf");
	};

	return function(scope, element, attrs) {
		scope.$evalAsync(function() {
			register(scope);
		});
	};
});

})(this);
