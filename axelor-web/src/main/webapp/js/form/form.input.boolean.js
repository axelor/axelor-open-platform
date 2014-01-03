/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
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
			scope.applyLater(function(){
				setTimeout(onChange);
			});
		});
	},
	template_editable: '<input type="checkbox">',
	template_readonly: '<input type="checkbox" disabled="disabled" ng-checked="text">'
});

/**
 * The Boolean widget with label on right.
 */
ui.formInput('InlineCheckbox', 'Boolean', {
	css: 'checkbox-inline',
	showTitle: false,
	link: function (scope, element, attrs, model) {
		this._super.apply(this, arguments);
		scope.$watch('attr("title")', function(title) {
			scope.label = title;
		});
	},
	template_editable: '<label class="checkbox"><input type="checkbox" ng-model="record[field.name]"> {{label}}</label>',
	template_readonly: '<label class="checkbox"><input type="checkbox" disabled="disabled" ng-checked="text"> {{label}}</label>'
});

})(this);
