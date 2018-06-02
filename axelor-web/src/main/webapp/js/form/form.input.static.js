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
	if (axelor.device.mobile) {
		return false;
	}
	if(!axelor.config['user.technical']) {
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

	function enter(e, show) {
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
		}, (e.ctrlKey || show) ? 0 : 1000);
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
		if (popoverTimer) {
			clearTimeout(popoverTimer);
			popoverTimer = null;
		}
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
	
	element.on('$destroy', destroy);
	scope.$on('$destroy', destroy);
}

function setupPopover(scope, element, getHelp, placement) {

	if (!canDisplayPopover(scope, false)) {
		return;
	}
	
	var timer = null;
	element.on('mouseenter.help.setup', function (e) {
		if (timer) {
			clearTimeout(timer);
		}
		timer = setTimeout(function () {
			element.off('mouseenter.help.setup');
			element.off('mouseleave.help.setup');
			makePopover(scope, element, getHelp, placement);
			element.trigger('mouseenter.popover', true);
		}, e.ctrlKey ? 0 : 1000);
	});
	element.on('mouseleave.help.setup $destroy', function () {
		if (timer) {
			clearTimeout(timer);
			timer = null;
		}
	});
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

	return function(scope, element, attrs) {
		setupPopover(scope, element, getHelp, 'bottom');
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
		
		if(!canDisplayPopover(scope, true)) {
			return;
		}

		if (text) {
			addRow(null, '<hr noshade>', 'help-text');
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
		if (field.help && axelor.config['user.noHelp'] !== true) {
			if (element.parent('label').length) {
				element.parent('label').addClass('has-help');
			} else {
				element.addClass('has-help');
			}
		}
		setupPopover(scope, element, getHelp);
	}

	return function(scope, element, attrs) {
		var field = scope.field;
		if (!_.isEmpty(field)) {
			return doLink(scope, element, attrs);
		}
		var unwatch = scope.$watch('field', function popoverFieldWatch(field, old) {
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

	template:
		"<label><span ui-help-popover ng-transclude></span></label>"
});

ui.directive('uiTranslateIcon', ['$q', function ($q) {
	return {
		link: function (scope, element) {
			var icon = $("<i class='fa fa-flag translate-icon'></i>").attr('title', _t('Show translations.')).appendTo(element);
			var toggle = function () {
				icon.toggle(!scope.$$readonlyOrig);
			};

			scope.$watch("$$readonlyOrig", toggle);
			scope.$on("on:new", toggle);
			scope.$on("on:edit", toggle);

			var myDs = scope._dataSource;
			var trDs = scope._dataSource._new("com.axelor.meta.db.MetaTranslation");
			trDs._sortBy = ["id"];

			function saveData(value, data, orig, callback) {
				var changed = [];
				var removed = [];
				
				data.forEach(function (item) {
					var found = _.findWhere(orig, { id: item.id });
					if (!angular.equals(found, item)) {
						changed.push(item);
					}
				});
				
				orig.forEach(function (item) {
					var found = _.findWhere(data, { id: item.id });
					if (!found) {
						removed.push(item);
					}
				});
				
				function saveTranslations() {
					var all = [];

					if (removed.length) {
						all.push(trDs.removeAll(removed));
					}
					if (changed.length) {
						all.push(trDs.saveAll(changed));
					}
					
					if (all.length) {
						$q.all(all).then(function () {
							var lang = axelor.config['user.lang'] || en;
							var key = 'value:' + scope.getValue();
							var trKey = '$t:' + scope.field.name;
							return trDs.search({
								domain: "self.key = :key and self.language = :lang",
								context: { key: key, lang: lang },
								limit: 1
							}).success(function (records) {
								var record = _.first(records);
								if (scope.record) {
									scope.record[trKey] = (record||{}).message;
									scope.$parent.$parent.text = scope.format(scope.getValue());
									var rec = scope._dataSource.get(scope.record.id);
									if (rec) {
										rec[trKey] = scope.record[trKey];
									}
								}
							});
						}).then(callback, callback);
					} else {
						callback();
					}
				}

				if (value !== scope.getValue()) {
					scope.$parent.$parent.setValue(value, true);
					scope.waitForActions(function () {
						scope.$parent.$parent.onSave().then(saveTranslations, callback);
					});
				} else {
					saveTranslations();
				}
			}

			function showPopup(data) {
				
				if (!data || data.length == 0) {
					data = [];
				}
				
				var value = scope.getValue();

				var orig = angular.copy(data);
				var form = $("<form>");
				
				var valueInput = $("<input type='text' class='span12'>")
				.prop('name', scope.field.name)
				.prop('required', true)
				.val(value)
				.on('input', function () {
					value = this.value;
					data.forEach(function (item) {
						item.key = 'value:' + value;
					});
				});

				// add value fields
				$("<div class='row-fluid'>")
					.append($("<label class='span12'>").text(_t("Value")))
					.appendTo(form);
				$("<div class='row-fluid'>")
					.append(valueInput)
					.appendTo(form);

				form.append('<hr>');

				// add translation fields
				$("<div class='row-fluid'>")
					.append($("<label class='span8'>").text(_t("Translation")))
					.append($("<label class='span4'>").text(_t("Language")))
					.appendTo(form);

				function addRow(item) {
					var onchange = function () {
						var v = item[this.name];
						if (v !== this.value) {
							item[this.name] = this.value;
						}
					};

					item.key = item.key || ('value:' + value);

					var input1 = $("<input type='text' class='span8'>")
						.prop("name", "message")
						.prop("required", true)
						.val(item.message)
						.on("input", onchange);
					var input2 = $("<input type='text' class='span4'>")
						.prop("name", "language")
						.prop("required", true)
						.val(item.language)
						.on("input", onchange);
					var row = $("<div class='row-fluid'>")
						.append(input1)
						.append(input2)
						.appendTo(form);
					
					if (dialog) {
						input1.focus();
					}
					
					// remove icon
					$("<i class='fa fa-times'>")
						.add('help', _t('Remove'))
						.appendTo(row)
						.click(function () {
							var i = data.indexOf(item);
							data.splice(i, 1);
							row.remove();
						});
				}

				function addNew() {
					var item = {};
					data.push(item);
					addRow(item);
				}
				
				var dialog;
				
				function validate() {
					var empty = html.find('input:text[value=""]');
					if (empty.length) {
						empty.first().focus();
						return false;
					}
					return true;
				}

				var html = $("<div>").append(form);

				// add icon
				$("<i class='fa fa-plus'>")
					.attr('help', _t('Add'))
					.appendTo(html)
					.click(function () {
						if (validate()) {
							addNew();
						}
					});

				data.forEach(addRow);
				
				if (data.length === 0) {
					addNew();
				}
				
				function close() {
					if (dialog) {
						dialog.dialog('close');
					}
				}

				dialog = axelor.dialogs.box(html, {
					title: _t('Translations'),
					buttons: [{
						'text'	: _t('Cancel'),
						'class'	: 'btn',
						'click'	: close
					}, {
						'text'	: _t('OK'),
						'class'	: 'btn btn-primary',
						'click'	: function() {
							if (validate()) {
								saveData(value, data, orig, close);
							}
						}
					}]
				}).addClass('translation-form');
			}

			icon.click(function (e) {
				var value = scope.getValue();
				if (value) {
					trDs.search({
						domain: "self.key = :key",
						context: { key: 'value:' + value }
					}).success(showPopup);
				}
			});
		}
	};
}]);

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
		
		element.one('mouseover', function () {
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
				var t = element.data('tooltip');
				if (t) {
					t.destroy();
					t = null
				}
			});
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
		scope.$watch('isReadonlyExclusive()', function buttonReadonlyWatch(readonly, old) {
			if (readonly === old && readonlySet) return;
			readonlySet = true;
			return setDisabled(readonly);
		});
		
		scope.$watch('attr("title")', function buttonTitleWatch(title, old) {
			if (!title || title === old) return;
			if (element.is('button')) {
				return element.html(title);
			}
			element.children('.btn-text').html(title);
		});

		scope.$watch('attr("css")', function buttonCssWatch(css, old) {
			var curr = css || field.css || 'btn-success';
			var prev = old || field.css || 'btn-success';
			if (curr !== prev) {
				element.removeClass(prev || '').addClass(curr);
			}
		});

		scope.$watch('attr("icon")', function buttonIconWatch(icon, old) {
			if (icon === old || (icon && icon.indexOf('fa-') !== 0)) return;
			var iconElem = element.find('i.fa:first');
			if (iconElem.length == 0) {
				iconElem = $('<i>').addClass('fa').prependTo(element.prepend(' '));
			}
			iconElem.removeClass(old || '').addClass(icon || field.icon || '');
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
		scope.$watch('attr("title")', function infoButtonTitleWatch(title, old) {
			if (!title || title === old) return;
			scope.title = title;
		});
		Object.defineProperty(scope, 'value', {
			get: function () {
				return field.currency
					? ui.formatters.decimal(field, (scope.record || {})[field.name], scope.record)
					: ui.formatters.$fmt(scope, field.name);
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
