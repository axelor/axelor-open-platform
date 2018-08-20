/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

"use strict";

var ui = angular.module('axelor.ui');

/**
 * The String widget.
 */
ui.formInput('String', {
	css: 'string-item',

	init: function(scope) {
		var field = scope.field;
		var isReadonly = scope.isReadonly;
		var trKey = "$t:" + field.name;

		scope.isReadonly = function () {
			scope.$$readonlyOrig = isReadonly.apply(this, arguments);
			return (scope.record && scope.record[trKey]) || scope.$$readonlyOrig;
		};

		scope.format = function (value) {
			if ((scope.record && scope.record[trKey])) {
				return scope.record[trKey];
			}
			return value;
		};
	},

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

	template_readonly: '<input type="text" ng-show="text" tabindex="-1" readonly="readonly" class="display-text" value="{{text}}">'
});

/**
 * The Email input widget.
 */
ui.formInput('Email', {

	css: 'email-item',

	metaWidget: true,

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
	metaWidget: true,
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

		textarea.rows = parseInt(field.height) || 8;

		//Firefox add one more line
		if (axelor.browser.mozilla) {
			textarea.rows -= 1;
		}

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
	template_editable: '<textarea></textarea >',
	template_readonly: '<pre ng-show="text">{{text}}</pre>'
});

ui.formInput('Password', 'String', {

	css: 'password-item',

	metaWidget: true,

	init: function(scope) {

		scope.password = function() {
			var value = this.getValue() || "";
			return _.str.repeat('*', value.length);
		};
	},
	template_readonly: '<input type="password" ng-show="text" tabindex="-1" readonly="readonly" class="display-text" value="{{password()}}"></input>',
	template_editable: '<input type="password" autocomplete="new-password">'
});

ui.directive('uiTextareaAutoSize', function () {

	return function (scope, element, attrs) {

		if (!element.is('textarea')) return;

		function resize() {
			var diff = element.outerHeight() - element.innerHeight();
			element.css('height', 'auto').css('height', element[0].scrollHeight + diff);
		}

		element.on('focus keyup input', resize);
		setTimeout(resize);
	};
});

})();
