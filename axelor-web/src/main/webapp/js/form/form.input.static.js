/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
var popoverElem = null;
var popoverTimer = null;

function canDisplayPopover(scope, details) {
	var mode = axelor.config['application.mode'];
	var tech = axelor.config['user.technical'];
	
	if (axelor.device.mobile) {
		return false;
	}

	if(mode == 'prod' && !tech) {
		return details ? false : scope.field && scope.field.help;
	}

	return true;
}

function makePopover(scope, element, callback, placement) {
	
	var mode = axelor.config['application.mode'];
	var tech = axelor.config['user.technical'];
	var doc = $(document);
	
	var table = null;

	function addRow(label, text, klass) {
		if (table === null) {
			table = $('<table class="field-details"></table>');
		}
		
		var tr = $('<tr></tr>').appendTo(table);
		if (label) {
			$('<th></th>').text(label).appendTo(tr);
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
				viewport = { height: window.innerHeight, width: window.innerWidth };
			if(viewport.height < (coord.top + 100))
				return 'top';
			if(coord.left > (viewport.width / 2))
				return 'left';
			return 'right';
		},
		trigger: 'manual',
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
	
	element.on('mouseenter.popover', enter);
	element.on('mouseleave.popover', leave);

	function selectText(elem) {
		var el = $(elem).get(0);
        if (document.selection) {
            var range = document.body.createTextRange();
            range.moveToElementText(el);
            range.select();
        } else if (window.getSelection) {
            var range = document.createRange();
            range.selectNodeContents(el);
            window.getSelection().removeAllRanges();
            window.getSelection().addRange(range);
        }
    }

	function enter(e) {
		if (popoverTimer) {
			clearTimeout(popoverTimer);
		}
		popoverTimer = setTimeout(function () {
			if (popoverElem === null) {
				popoverElem = element;
				popoverElem.popover('show');
				if (e.ctrlKey) {
					selectText(table.find('.field-name,.model-name').get(0));
				}
			}
			var tip = element.data('popover').$tip;
			if (tip) {
				tip.attr('tabIndex', 0);
				tip.css('outline', 'none');
			}
		}, e.ctrlKey ? 0 : 1000);
	}
	
	function leave(e) {
		
		if (e.ctrlKey) {
			doc.off('mousemove.popover');
			doc.on('mousemove.popover', leave);
			return;
		}
		
		if (popoverTimer) {
			clearTimeout(popoverTimer);
			popoverTimer = null;
		}
		if (popoverElem) {
			popoverElem.popover('hide');
			popoverElem = null;
			doc.off('mousemove.popover');
		}
	}

	function destroy() {
		if (element) {
			element.off('mouseenter.popover');
			element.off('mouseleave.popover');
			element.popover('destroy');
			element = null;
		}
		if (table) {
			table.remove();
			table = null;
		}
		doc.off('mousemove.popover');
	}
	
	scope.$on('$destroy', destroy);
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
			addRow(_t('Object'), '<code>' + tab.model + '</code>', 'model-name');
		}
		if (tab.domain) {
			addRow(_t('Domain'), tab.domain);
		}
		if (view && view.name) {
			addRow(_t('View'), view.name);
		}
	}

	return function (scope, element, attrs) {
		if(canDisplayPopover(scope, true)) {
			return makePopover(scope, element, getHelp, 'bottom');
		}
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
		
		if(!canDisplayPopover(scope, true)) {
			return;
		}
		
		var model = scope._model;
		if (model === field.target) {
			model = scope._parentModel || scope.$parent._model;
		}

		addRow(_t('Object'), model);
		addRow(_t('Field Name'), '<code>' + field.name + '</code>', 'field-name');
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
		var length;

		if (value && /-one$/.test(field.serverType)) {
			value = _.compact([value.id, value[field.targetName]]).join(',');
			value = '(' + value + ')';
		}
		if (value && field.type === "password") {
			value = _.str.repeat('*', value.length);
		}
		if (value && /^(string|image|binary)$/.test(field.type)) {
			length = value.length;
			value = _.first(value, 50);
			if (length > 50) {
				value.push('...');
			}
			value = value.join('');
		}
		if (value && /(panel-related|one-to-many|many-to-many)/.test(field.serverType)) {
			length = value.length;
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
		if(canDisplayPopover(scope, false)) {
			makePopover(scope, element, getHelp);
		}
	}

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
		if (field && field.help && axelor.config['user.noHelp'] !== true) {
			element.addClass('has-help');
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
	template: '<div>{{field.title}}</div>'
});

/**
 * The Static Text widget.
 *
 */
ui.formItem('Static', {
	css: 'static-item',
	link: function (scope, element, attrs, ctrl) {
		var field = scope.field;
		element.html(field.text);
	},
	template: '<div></div>'
});

/**
 * The Static Label widget.
 *
 */
ui.formItem('StaticLabel', {
	css: 'static-item',
	transclude: true,
	template: '<label ng-transclude></label>'
});

/**
 * The Help Text widget.
 *
 */
ui.formItem('Help', {
	css: 'help-item',
	link: function (scope, element, attrs, ctrl) {
		var field = scope.field;
		var css = "alert alert-info";
		if (field.css && field.css.indexOf('alert-') > -1) {
			css = "alert";
		}
		element.addClass(css).html(field.text);
	},
	template: '<div></div>'
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
		
		var isIcon = icon.indexOf('fa-') === 0;
		
		if (isIcon || icon) {
			element.prepend(' ');
		}

		var css = field.css || '';
		if (css.indexOf('btn-') > -1 && css.indexOf('btn-primary') === -1) {
			element.removeClass('btn-primary');
		}

		if (field && field.help && axelor.config['user.noHelp'] !== true) {
			element.addClass('has-help');
		}

		if (isIcon) {
			var e = $('<i>').addClass('fa').addClass(icon).prependTo(element);
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
			element.removeClass('btn btn-primary').addClass('btn-link');
			element.attr("href", field.link);
		}
		
		element.tooltip({
			html: true,
			title: function() {
				if (field.help) {
					return field.help;
				}
				if (element.innerWidth() < element[0].scrollWidth) {
					return field.title;
				}
			},
			delay: { show: 1000, hide: 100 },
			container: 'body'
		});

		element.on("$destroy", function () {
			element.tooltip("destroy");
		});

		element.on("click", function(e) {

			if (scope.isReadonlyExclusive() || element.hasClass('disabled')) {
				return;
			}

			function enable() {
				scope.ajaxStop(function () {
					setDisabled(scope.isReadonlyExclusive());
				}, 100);
			}

			function setEnable(p) {
				if (p && p.then) {
					p.then(enable, enable);
				} else {
					scope.ajaxStop(enable, 500);
				}
			}

			function doClick() {
				setEnable(scope.fireAction("onClick"));
			}

			setDisabled(true);

			if (scope.waitForActions) {
				return scope.waitForActions(doClick);
			}
			return doClick();
		});
		
		function setDisabled(disabled) {
			if (disabled || disabled === undefined) {
				return element.addClass("disabled").attr('tabindex', -1);
			}
			return element.removeClass("disabled").removeAttr('tabindex');
		}

		var readonlySet = false;
		scope.$watch('isReadonlyExclusive()', function(readonly, old) {
			if (readonly === old && readonlySet) return;
			readonlySet = true;
			return setDisabled(readonly);
		});
		
		scope.$watch('attr("title")', function(title, old) {
			if (!title || title === old) return;
			if (element.is('button')) {
				return element.html(title);
			}
			element.children('.btn-text').html(title);
		});

		scope.$watch('attr("css")', function (css, old) {
			if (css !== old) {
				element.removeClass(old || '').addClass(css || field.css || '');
			}
		});

		scope.$watch('attr("icon")', function (icon, old) {
			if (icon === old || (icon && icon.indexOf('fa-') !== 0)) return;
			element.find('i.fa:first').removeClass(old || '').addClass(icon || field.icon || '');
		});
	},
	template: '<a href="" class="btn btn-primary">'+
		'<span class="btn-text" ng-transclude></span>'+
	'</a>'
});

ui.formItem('InfoButton', 'Button', {
	link: function (scope, element, attrs) {
		this._super.apply(this, arguments);
		var field = scope.field || {};
		scope.title = field.title;
		scope.$watch('attr("title")', function(title, old) {
			if (!title || title === old) return;
			scope.title = title;
		});
		Object.defineProperty(scope, 'value', {
			get: function () {
				return scope.record ? scope.record[field.name] : "";
			}
		});
	},
	replace: true,
	template:
		"<div class='btn info-button'>" +
			"<div class='info-button-data'>" +
				"<span class='info-button-value'>{{value}}</span>" +
				"<small class='info-button-title'>{{title}}</small>" +
			"</div>" +
		"</div>"
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
		scope.showTitle = field.showTitle !== false;

		scope.btn.isHidden = function() {
			return scope.isHidden();
		};
	},

	template:
		'<button class="btn" ui-show="!isHidden()" name="{{btn.name}}" ui-actions ui-widget-states>' +
			'<span ng-show="showTitle">{{title}}</span>' +
		'</button>'
});

})();
