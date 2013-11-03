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

	template_readonly: '<input type="text" ng-show="text" tabindex="-1" readonly="readonly" class="display-text" value="{{text}}">'
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
	template_readonly: '<pre>{{text}}</pre>'
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
