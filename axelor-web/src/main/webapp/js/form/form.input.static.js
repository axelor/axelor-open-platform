/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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

function makePopover(scope, element, callback, placement) {
	
	var mode = __appSettings['application.mode'];
	var tech = __appSettings['user.technical'];

	if (mode != 'dev' && !tech) {
		return;
	}

	var table = null;

	function addRow(label, text, klass) {
		if (table === null) {
			table = $('<table class="field-details"></table>');
		}
		
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

	element.popover({
		html: true,
		delay: { show: 1000, hide: 100 },
		animate: true,
		placement: function() {
			if (placement) return placement;
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
			if (table) {
				table.remove();
				table = null;
			}
			callback(scope, addRow);
			if (table) return table;
			return "";
		}
	});

	function destroy() {
		if (element) {
			element.popover('destroy');
			element = null;
		}
		if (table) {
			table.remove();
			table = null;
		}
	}
	
	element.on('$destroy', destroy);
}

ui.directive('uiTabPopover', function() {
	
	function getHelp(scope, addRow) {
		var tab = scope.tab || {};
		var type = tab.viewType;
		var view = _.findWhere(tab.views, {type: type});
		
		var viewScope = tab.$viewScope;
		if (viewScope && viewScope.schema) {
			view = viewScope.schema;
		}
		
		if (tab.action) {
			addRow(_t('Action'), tab.action);
		}
		if (tab.model) {
			addRow(_t('Object'), tab.model);
		}
		if (tab.domain) {
			addRow(_t('Domain'), tab.domain);
		}
		if (view && view.name) {
			addRow(_t('View'), view.name);
		}
	}

	return function (scope, element, attrs) {
		return makePopover(scope, element, getHelp, 'bottom');
	};
});

ui.directive('uiHelpPopover', function() {

	function getHelp(scope, addRow) {

		var field = scope.field;
		var text = field.help;
		if (text) {
			text = text.replace(/\\n/g, '<br>');
			addRow(null, text, 'help-text');
		}

		if (text) {
			addRow(null, '<hr noshade>', 'help-text');
		}
		
		var model = scope._model;
		if (model === field.target) {
			model = scope.$parent._model;
		}

		addRow(_t('Object'), model);
		addRow(_t('Field Name'), field.name);
		addRow(_t('Field Type'), field.serverType);

		if (field.type === 'text') {
			return;
		}

		if (field.domain) {
			addRow(_t('Filter'), field.domain);
		}

		if (field.target) {
			addRow(_t('Reference'), field.target);
		}

		var value = scope.$eval('$$original.' + field.name);
		if (value && /-one$/.test(field.serverType)) {
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
		if (value && /-many$/.test(field.serverType)) {
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

		addRow(_t('Orig. Value'), value);
	}

	function doLink(scope, element, attrs) {
		var field = scope.field;
		if (field == null) {
			return;
		}
		makePopover(scope, element, getHelp);
	};

	return function(scope, element, attrs) {
		var field = scope.field;
		if (!_.isEmpty(field)) {
			return doLink(scope, element, attrs);
		}
		var unwatch = scope.$watch('field', function(field, old) {
			if (!field) {
				return;
			}
			unwatch();
			doLink(scope, element, attrs);
		}, true);
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
	template: '<div><span style="padding-left: 4px;">{{field.title}}</span><hr style="margin: 4px 0;"></div>'
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
			element.addClass("button-icon");
		}
		
		if (_.isString(field.link)) {
			element.removeClass('btn');
			element.attr("href", field.link);
		}
		
		if (field.help) {
			element.tooltip({
				html: true,
				title: field.help,
				delay: { show: 500, hide: 100 },
				container: 'body'
			});
		}

		element.on("click", function(e) {
			if (!scope.isReadonlyExclusive()) {
				scope.fireAction("onClick");
			}
		});
		
		var readonlySet = false;
		scope.$watch('isReadonlyExclusive()', function(readonly, old) {
			if (readonly === old && readonlySet) return;
			readonlySet = true;
			if (readonly) {
				return element.addClass("disabled").attr('tabindex', -1);
			}
			return element.removeClass("disabled").removeAttr('tabindex');
		});
		
		scope.$watch('attr("title")', function(title, old) {
			if (!title || title === old) return;
			element.children('.btn-text').html(title);
		});
	},
	template: '<a href="" class="btn">'+
		'<span class="btn-text" ng-transclude></span>'+
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

		scope.btn.isHidden = function() {
			return scope.isHidden();
		};
	},

	template: '<button class="btn" ui-show="!isHidden()" name="{{btn.name}}" ui-actions ui-widget-states>{{title}}</button>'
});

ui.directive('uiBtnGroupHelper', function () {
	
	return function (scope, element, attrs) {
		if (!element.is('.btn-group')) {
			return;
		}
		scope.$watch(function() {
			element.children().removeClass('btn-fix-left btn-fix-right');
			element.children(':not(.ui-hide):first').addClass('btn-fix-left');
			element.children(':not(.ui-hide):last').addClass('btn-fix-right');
		});
	};
});

})(this);
