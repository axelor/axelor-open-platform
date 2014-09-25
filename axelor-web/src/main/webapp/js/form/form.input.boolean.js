/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

ui.formInput('Toggle', 'Boolean', {
	cellCss: 'form-item toggle-item',
	link: function (scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var field = scope.field;
		var icon = element.find('i');

		scope.icon = function () {
			return model.$viewValue && field.iconActive ? field.iconActive : field.icon;
		};

		scope.toggle = function () {
			model.$setViewValue(!model.$viewValue);
			if (scope.setExclusive && field.exclusive) {
				scope.setExclusive(field.name, scope.record);
			}
		};

		if (field.help || field.title) {
			element.attr('title', field.help || field.title);
		}
	},
	template_editable: null,
	template_readonly: null,
	template:
		"<button tabindex='-1' class='btn btn-default' ng-class='{active: record[field.name]}' ng-click='toggle()'>" +
			"<i class='fa {{icon()}}'></i>" +
		"</button>"
});

})(this);
