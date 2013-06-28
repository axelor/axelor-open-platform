(function(){

var ui = angular.module('axelor.ui');

/**
 * The String widget.
 */
ui.formInput('String', {
	css: 'string-item',

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var props = scope.field,
			regExp = props.pattern ? new RegExp(props.pattern,'i') : null;

		scope.validate = function(value) {
			return lengthValidation(value) && patternValidation(value);
		};

		function patternValidation(value) {
			if(_.isEmpty(value) || !regExp) {
				return true;
			}

			return regExp.test(value);
		};

		function lengthValidation(value) {
			var length = value ? value.length : 0,
				minSize = +props.minSize,
				maxSize = +props.maxSize,
				valid = true;

			if (minSize) {
				valid = length >= minSize;
			}
			if(valid && maxSize) {
				valid = length <= maxSize;
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
	template_readonly: '<span>{{password()}}</span>',
	template_editable: '<input type="password">'
});

})(this);
