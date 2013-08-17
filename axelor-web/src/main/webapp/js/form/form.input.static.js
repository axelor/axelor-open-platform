(function(){

var ui = angular.module('axelor.ui');

ui.directive('uiHelpPopover', function() {
	
	function addRow(table, label, text, klass) {
		var tr = $('<tr></tr>').appendTo(table);
		if (label) {
			$('<th></th>').text(label + ':').appendTo(tr);
		}
		if (klass == null) {
			text = '<code>' + text + '</code>';
		}
		var td = $('<td></td>').html(text).addClass(klass).appendTo(tr);
		if (!label) {
			td.attr('colspan', 2);
		}
		return table;
	}
	
	function getHelp(scope, element, field, mode) {
		
		var text = field.help;
		var table = $('<table class="field-details"></table>');

		if (text) {
			text = text.replace(/\\n/g, '<br>');
			addRow(table, null, text, 'help-text');
		}
		
		if (mode != 'dev') {
			return table;
		}

		if (text) {
			addRow(table, null, '<hr noshade>', 'help-text');
		}
		
		var model = scope._model;
		if (model === field.target) {
			model = scope.$parent._model;
		}

		addRow(table, _t('Object'), model);
		addRow(table, _t('Field Name'), field.name);
		addRow(table, _t('Field Type'), field.serverType);
		
		if (field.type == 'text') {
			return table;
		}
		
		if (field.domain) {
			addRow(table, _t('Filter'), field.domain);
		}
		
		if (field.target) {
			addRow(table, _t('Reference'), field.target);
		}

		var value = scope.$eval('$$original.' + field.name);
		if (value && field.type === 'many-to-one') {
			value = value.id;
		}
		if (value && field.type === "password") {
			value = _.str.repeat('*', value.length);
		}
		if (value && /^(string|image|binary)$/.test(field.type)) {
			var length = value.length;
			value = _.first(value, 50);
			if (length > 50) {
				value.push('...');
			}
			value = value.join('');
		}
		if (value && /-many$/.test(field.type)) {
			var length = value.length;
			value = _.first(value, 5);
			value = _.map(value, function(v){
				return v.id;
			});
			if (length > 5) {
				value.push('...');
			}
			value = value.join(', ');
		}

		addRow(table, _t('Orig. Value'), value);

		return table;
	}

	return function(scope, element, attrs) {
		var field = scope.field;
		if (field == null) {
			return;
		}
		var mode = scope.$eval('app.mode') || 'dev';
		if (!field.help && mode != 'dev') {
			return;
		}

		element.popover({
			html: true,
			delay: { show: 1000, hide: 100 },
			animate: true,
			placement: function() {
				var coord = $(element.get(0)).offset(),
					viewport = {height: innerHeight, width: window.innerWidth};
				if(viewport.height < (coord.top + 100))
					return 'top';
				if(coord.left > (viewport.width / 2))
					return 'left';
				return 'right';
			},
			trigger: 'hover',
			container: 'body',
			title: function() {
				return element.text();
			},
			content: function() {
				return getHelp(scope, element, field, mode);
			}
		});
	};
});

/**
 * The Label widget.
 *
 */
ui.formItem('Label', {

	css: 'label-item',
	cellCss: 'form-label',

	transclude: true,
	
	link: function(scope, element, attrs) {
		var field = scope.field;
		if (field && field.required) {
			element.addClass('required');
		}
		
		scope.$watch("isReadonly()", function(readonly){
			element.toggleClass("readonly", readonly);
		});
	},

	template: '<label><span ui-help-popover ng-transclude></span></label>'
});

/**
 * The Spacer widget.
 *
 */
ui.formItem('Spacer', {
	css: 'spacer-item',
	template: '<div>&nbsp;</div>'
});

/**
 * The Separator widget.
 *
 */
ui.formItem('Separator', {
	css: 'separator-item',
	showTitle: false,
	scope: {
		title: '@'
	},
	template: '<div><span style="padding-left: 4px;">{{title}}</span><hr style="margin: 4px 0;"></div>'
});

/**
 * The Static Text widget.
 *
 */
ui.formItem('Static', {
	css: 'static-item',
	transclude: true,
	template: '<label ng-transclude></label>'
});

/**
 * The button widget.
 */
ui.formItem('Button', {
	css: 'button-item',
	transclude: true,
	link: function(scope, element, attrs, model) {
		var field = scope.field || {};
		
		var icon = field.icon || "";
		var iconHover = field.iconHover || "";
		
		var isIcon = icon.indexOf('icon-') === 0;

		if (isIcon) {
			var e = $('<i>').addClass(icon).prependTo(element);
			if (iconHover) {
				e.hover(function() {
					$(this).removeClass(icon).addClass(iconHover);
				}, function() {
					$(this).removeClass(iconHover).addClass(icon);
				});
			}
		} else if (icon) {
			$('<img>').attr('src', icon).prependTo(element);
		}

		if (!field.title) {
			element.addClass("icon-only");
		}
		
		if (_.isString(field.link)) {
			element.removeClass('btn');
			element.attr("href", field.link);
		}

		element.on("click", function(e) {
			if (!scope.attr('readonly')) {
				scope.fireAction("onClick");
			}
		});
		
		scope.$watch('attr("readonly")', function(readonly, old) {
			if (readonly === old) return;
			if (readonly) {
				return element.addClass("disabled");
			}
			return element.removeClass("disabled");
		});
	},
	template: '<a href="" class="btn">'+
		'<span ng-transclude></span>'+
	'</a>'
});

ui.formItem('ToolButton', 'Button', {

	getViewDef: function(element) {
		return this.btn;
	},

	link: function(scope, element, attrs) {
		this._super.apply(this, arguments);
		var field = scope.field;
		if (field == null) {
			return;
		}

		scope.title = field.title;

		scope.$watch("isHidden()", function(hidden, old) {
			setTimeout(function() {
				element.parent().children().removeClass('btn-fix-left btn-fix-right');
				element.parent().children(':visible:first').addClass('btn-fix-left');
				element.parent().children(':visible:last').addClass('btn-fix-right');
			});
		});
	},

	template: '<button class="btn" ui-actions ui-widget-states>{{title}}</button>'
});

})(this);
