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
 * The String widget.
 */
ui.formInput('String', {
	css: 'string-item',

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var field = scope.field,
			regex = field.pattern ? new RegExp(field.pattern, 'i') : null,
			minSize = +(field.minSize),
			maxSize = +(field.maxSize);

		scope.validate = function(value) {
			if (_.isEmpty(value)) {
				return true;
			}
			var length = value.length,
				valid = true;

			if (minSize) {
				valid = length >= minSize;
			}
			if(valid && maxSize) {
				valid = length <= maxSize;
			}
			if (valid && regex) {
				valid = regex.test(value);
			}

			return valid;
		};
	},

	template_readonly: '<input type="text" ng-show="text.length" tabindex="-1" readonly="readonly" class="display-text" value="{{text}}">'
});

/**
 * The Email input widget.
 */
ui.formInput('Email', {

	css: 'email-item',

	pattern: /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/,

	link: function(scope, element, attrs, model) {

		var pattern = this.pattern;

		scope.validate = function(value) {
			if(_.isEmpty(value)) {
				return true;
			}
			return pattern.test(value);
		};
	},

	template_editable: '<input type="email">',
	template_readonly: '<a target="_blank" ng-show="text" href="mailto:{{text}}">{{text}}</a>'
});

/**
 * The URL input widget.
 */
ui.formInput('Url', {
	css: 'url-item',
	template_editable: '<input type="url">',
	template_readonly: '<a target="_blank" ng-show="text" href="{{text}}">{{text}}</a>'
});

/**
 * The Phone input widget.
 */
ui.formInput('Phone', 'String', {
	css: 'phone-item',
	template_editable: '<input type="tel">'
});


/**
 * The Text input widget.
 */
ui.formInput('Text', {
	css: 'text-item',
	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var field = scope.field,
			textarea = element.get(0);

		textarea.rows = field.height || 8;

		//Firefox add one more line
		if ($.browser.mozilla){
			textarea.rows -= 1;
		}
    },
	template_editable: '<textarea></textarea >',
	template_readonly: '<pre ng-show="text">{{text}}</pre>'
});

ui.formInput('Password', 'String', {

	css: 'password-item',

	init: function(scope) {

		scope.password = function() {
			var value = this.getValue() || "";
			return _.str.repeat('*', value.length);
		};
	},
	template_readonly: '<input type="password" ng-show="text" tabindex="-1" readonly="readonly" class="display-text" value="{{password()}}"></input>',
	template_editable: '<input type="password">'
});

})(this);
