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
(function(){

var ui = angular.module('axelor.ui');

function acceptNumber(value) {
	if (value === null || value === undefined) {
		return value;
	}
	if (_.isNumber(value)) {
		return +value;
	}
	if (/^(-)?\d+(\.\d+)?$/.test(value)) {
		return +value;
	}
	return value;
}

function parseNumber(field, value) {
	if (value === null || value === undefined) {
		return value;
	}
	if (!field || ['integer', 'long'].indexOf(field.serverType) === -1) {
		return value;
	}
	var num = +value;
	if (num === NaN) {
		return value;
	}
	return num;
}

ui.formWidget('BaseSelect', {
	
	showSelectionOn: "click",

	findInput: function(element) {
		return element.find('input:first');
	},

	init: function(scope) {
		
		scope.loadSelection = function(request, response) {

		};

		scope.parse = function(value) {
			return value;
		};

		scope.format = function(value) {
			return this.formatItem(value);
		};

		scope.formatItem = function(item) {
			return item;
		};
	},

	link_editable: function (scope, element, attrs, model) {

		var input = this.findInput(element);

		scope.showSelection = function(e) {
			if (scope.isReadonly()) {
				return;
			}
			if (!axelor.device.mobile) {
				input.focus();
			}
			input.autocomplete("search" , '');
		};

		scope.handleDelete = function(e) {

		};

		scope.handleSelect = function(e, ui) {
			
		};
		
		scope.handleClose = function(e, ui) {
			
		};
		
		scope.handleOpen = function(e, ui) {
			
		};

		function renderItem(ul, item) {
			var el = $("<li>").append( $("<a>").html(item.label)).appendTo(ul);
			if (item.click) {
				el.addClass("tag-select-action");
				ul.addClass("tag-select-action-menu");
			}
			return el;
		};

		var showOn = this.showSelectionOn;
		var doSetup = _.once(function (input) {
		
			var loading = false;
			var pending = null;
			var showing = false;

			function doLoad(request, response) {
				if (loading) {
					return pending = _.partial(doLoad, request, response);
				}
				loading = true;
				scope.loadSelection(request, function() {
					loading = false;
					response.apply(null, arguments);
					if (pending) {
						pending();
						pending = null;
					}
				});
			}

			input.autocomplete({
				
				minLength: 0,
				
				position: {  collision: "flip"  },
				
				source: doLoad,
	
				focus: function(event, ui) {
					return false;
				},
				
				select: function(event, ui) {
					var ret = scope.handleSelect(event, ui);
					if (ret !== undefined) {
						return ret;
					}
					return false;
				},
				
				open: function(event, ui) {
					showing = true;
					scope.handleOpen(event, ui);
				},
				
				close: function(event, ui) {
					showing = false;
					scope.handleClose(event, ui);
				}
			});
	
			input.data('ui-autocomplete')._renderItem = scope.renderSelectItem || renderItem;

			element.on('adjustSize adjustScroll', function (e) {
				if (showing) {
					input.autocomplete('close');
				}
			});
		});
		
		input.focus(function() {
			doSetup(input);
			element.addClass('focus');
			if (showOn === "focus") {
				scope.showSelection();
			}
		}).blur(function() {
			element.removeClass('focus');
		}).keydown(function(e) {
			var KEY = $.ui.keyCode;
			switch(e.keyCode) {
			case KEY.DELETE:
			case KEY.BACKSPACE:
				scope.handleDelete(e);
			}
		}).click(function() {
			doSetup(input);
			if (showOn === "click") {
				scope.showSelection();
			}
		});
	},

	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="fa fa-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
	'</span>'
});

function filterSelection(scope, field, selection, current) {
	if (_.isEmpty(selection)) return selection;
	if (_.isEmpty(field.selectionIn)) return selection;
	
	var context = (scope.getContext || angular.noop)() || {};
	var expr = field.selectionIn.trim();
	if (expr.indexOf('[') !== 0) {
		expr = '[' + expr + ']';
	}

	var list = axelor.$eval(scope, expr, context);
	var value = acceptNumber(current);
	
	if (_.isEmpty(list)) {
		return selection;
	}
	
	list = _.map(list, acceptNumber);
	
	return _.filter(selection, function (item) {
		var val = acceptNumber(item.value);
		return val === value || list.indexOf(val) > -1;
	});
}

ui.formInput('Select', 'BaseSelect', {

	css: 'select-item',
	cellCss: 'form-item select-item',

	init: function(scope) {
		
		this._super(scope);

		var field = scope.field,
			selectionList = field.selectionList || [],
			selectionMap = {};
		
		var data = _.map(selectionList, function(item) {
			var value = "" + item.value;
			selectionMap[value] = item.title;
			return {
				value: value,
				label: item.title || "&nbsp;"
			};
		});
	
		var dataSource = null;
		function getDataSource() {
			if (dataSource || !field.selection || !field.domain) {
				return dataSource;
			}
			return dataSource = scope._dataSource._new('com.axelor.meta.db.MetaSelectItem', {
				domain: "(self.select.name = :_select) AND (" + field.domain + ")",
				context: {
					_select: field.selection
				}
			});
		}
		
		scope.loadSelection = function(request, response) {
			
			var  ds = getDataSource();
			
			function select(records) {
				var items = _.filter(records, function(item) {
					var label = item.label || "",
						term = request.term || "";
					return label.toLowerCase().indexOf(term.toLowerCase()) > -1;
				});
				items = filterSelection(scope, field, items);
				return response(items);
			}
			
			if (ds) {
				return ds.search({
					fields: ['value', 'title'],
					context: scope.getContext ? scope.getContext() : undefined
				}).success(function (records) {
					_.each(records, function (item) {
						item.label = selectionMap[item.value] || item.title;
					});
					return select(records);
				});
			}
			return select(data);
		};

		scope.formatItem = function(item) {
			var key = _.isNumber(item) ? "" + item : item;
			if (!key) {
				return item;
			}
			if (_.isString(key)) {
				return selectionMap[key] || "";
			}
			return item.label;
		};
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		
		var input = this.findInput(element);
		
		function update(value) {
			var val = parseNumber(scope.field, value);
			scope.setValue(val, true);
			scope.applyLater();
		}

		scope.handleDelete = function(e) {
			if (e.keyCode === 46) { // DELETE
				update(null);
			}
			if (e.keyCode === 8) { // BACKSPACE
				var value = scope.getValue();
				if (value || (e.target.value||'').length < 2) {
					update(null);
				}
			}
		};

		scope.handleSelect = function(e, ui) {
			update(ui.item.value);
		};

		scope.$render_editable = function() {
			input.val(this.getText());
		};
	}
});

ui.formInput('ImageSelect', 'Select', {
	
	BLANK: "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
	
	link: function(scope, element, attrs) {
		this._super(scope, element, attrs);
		
		var field = scope.field;
		var formatItem = scope.formatItem;

		scope.canShowText = function () {
			return field.labels === undefined || field.labels;
		};

		scope.formatItem = function (item) {
			if (scope.canShowText()) {
				return formatItem(item);
			}
			return "";
		};
		
		scope.$watch('getValue()', function (value, old) {
			scope.image = value || this.BLANK;
			element.toggleClass('empty', !value);
		}.bind(this));
	},
	
	link_editable: function(scope, element, attrs) {
		this._super(scope, element, attrs);
		var input = this.findInput(element);
		
		scope.renderSelectItem = function(ul, item) {
			var a = $("<a>").append($("<img>").attr("src", item.value));
			var el = $("<li>").addClass("image-select-item").append(a).appendTo(ul);
			
			if (scope.canShowText()) {
				a.append($("<span></span>").html(item.label));
			}
			
			return el;
		};
	},
	template_readonly:
		'<span class="image-select readonly">'+
			'<img ng-src="{{image}}"></img> <span ng-show="canShowText()">{{text}}</span>' +
		'</span>',

	template_editable:
		'<span class="picker-input image-select">'+
			'<img ng-src="{{image}}"></img>' +
			'<input type="text" autocomplete="off">'+
			'<span class="picker-icons">'+
				'<i class="fa fa-caret-down" ng-click="showSelection()"></i>'+
			'</span>'+
		'</span>'
});

ui.formInput('MultiSelect', 'Select', {

	css: 'multi-select-item',
	cellCss: 'form-item multi-select-item',
	
	init: function(scope) {
		this._super(scope);

		var __parse = scope.parse;
		scope.parse = function(value) {
			if (_.isArray(value)) {
				return value.join(', ');
			}
			return __parse(value);
		};

		scope.format = function(value) {
			var items = value,
				values = [];
			if (!value) {
				scope.items = [];
				return value;
			}
			if (!_.isArray(items)) items = items.split(/,\s*/);
			values = _.map(items, function(item) {
				return {
					value: item,
					title: scope.formatItem(item)
				};
			});
			scope.items = values;
			return _.pluck(values, 'title').join(', ');
		};
		
		scope.matchValues = function(a, b) {
			if (a === b) return true;
			if (!a) return false;
			if (!b) return false;
			if (_.isString(a)) return a === b;
			return a.value === b.value;
		};

		scope.getSelection = function() {
			return this.items;
		};
		
		var max = +(scope.field.max);
		scope.limited = function(items) {
			if (max && items && items.length > max) {
				scope.more = _t("and {0} more", items.length - max);
				return _.first(items, max);
			}
			scope.more = null;
			return items;
		};
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var input = this.findInput(element);

		input.focus(function() {
			scaleInput();
		}).blur(function() {
			scaleInput(50);
			input.val('');
		});
		
		var placeholder = null;
		if (scope.field.placeholder) {
			placeholder = $('<span class="tag-select-placeholder hidden"></span>')
				.text(scope.field.placeholder)
				.appendTo(element)
				.click(function (e) {
					scope.showSelection();
				});
		}

		function scaleInput(width) {
			
			var elem = element.find('.tag-selector'),
				pos = elem.position();

			if (width) {
				input.css('position', '');
				elem.width('');
				return input.width(width);
			}

			var top = pos.top,
				left = pos.left,
				width = element.innerWidth() - left;

			elem.height(input.height() + 2);
			elem.width(50);

			input.css('position', 'absolute');
			input.css('top', top + 5);
			input.css('left', left + 2);
			input.css('width', width - 24);
		}

		function update(value) {
			var val = parseNumber(scope.field, value);
			scope.setValue(val, true);
			scope.applyLater();
			setTimeout(function () {
				scaleInput(50);
			});
		}
		
		scope.removeItem = function(item) {
			var items = this.getSelection(),
				value = _.isString(item) ? item : (item||{}).value;

			items = _.chain(items)
				     .pluck('value')
					 .filter(function(v){
						 return !scope.matchValues(v, value);
					 })
					 .value();

			update(items);
		};
		
		var __showSelection = scope.showSelection;
		scope.showSelection = function(e) {
			if (e && $(e.target || e.srcElement).is('li,i,span.tag-text')) {
				return;
			}
			return __showSelection(e);
		};

		scope.handleDelete = function(e) {
			if (input.val()) {
				return;
			}
			var items = this.getSelection();
			this.removeItem(_.last(items));
		};
		
		scope.handleSelect = function(e, ui) {
			var items = this.getSelection(),
				values = _.pluck(items, 'value');
			var found = _.find(values, function(v){
				return scope.matchValues(v, ui.item.value);
			});
			if (found) {
				return false;
			}
			values.push(ui.item.value);
			update(values);
			scaleInput(50);
		};

		scope.handleOpen = function(e, ui) {
			input.data('autocomplete')
				 .menu
				 .element
				 .position({
					 my: "left top",
					 at: "left bottom",
					 of: element
				 })
				 .width(element.width() - 4);
		};
		
		scope.$render_editable = function() {
			if (placeholder) {
				placeholder.toggleClass('hidden', !!scope.getValue());
			}
			return input.val('');
		};

		scope.$watch('items.length', function (value, old) {
			setTimeout(function () {
				scaleInput(50);
			});
		});
	},
	template_editable:
	'<div class="tag-select picker-input">'+
	  '<ul ng-click="showSelection($event)">'+
		'<li class="tag-item label label-info" ng-repeat="item in items">'+
			'<span ng-class="{\'tag-link\': handleClick}" class="tag-text" ng-click="handleClick($event, item.value)">{{item.title}}</span> '+
			'<i class="fa fa-times fa-small" ng-click="removeItem(item)"></i>'+
		'</li>'+
		'<li class="tag-selector">'+
			'<input type="text" autocomplete="off">'+
		'</li>'+
	  '</ul>'+
	  '<span class="picker-icons">'+
	  	'<i class="fa fa-caret-down" ng-click="showSelection()"></i>'+
	  '</span>'+
	'</div>',
	template_readonly:
	'<div class="tag-select">'+
		'<span class="label label-info" ng-repeat="item in limited(items)">'+
			'<span ng-class="{\'tag-link\': handleClick}" class="tag-text" ng-click="handleClick($event, item.value)">{{item.title}}</span>'+
		'</span>'+
		'<span ng-show="more"> {{more}}</span>'+
	'</div>'
});

ui.formInput('SelectQuery', 'Select', {

	link_editable: function(scope, element, attrs, model) {
		
		this._super.apply(this, arguments);

		var current = {};
		
		function update(value) {
			scope.setValue(value);
			scope.applyLater();
		}

		scope.format = function(value) {
			if (!value) return "";
			if (_.isString(value)) {
				return current.label || value;
			}
			current = value;
			return value.label;
		};
		
		scope.parse = function(value) {
			if (!value || _.isString(value)) return value;
			return value.value;
		};
		
		scope.handleSelect = function(e, ui) {
			update(ui.item);
		};

		var query = scope.$eval(attrs.query);
	
		scope.loadSelection = function(request, response) {
			return query(request, response);
		};
	}
});

ui.formInput('RadioSelect', {
	
	css: "radio-select",
	
	link: function(scope, element, attrs, model) {
		
		var field = scope.field;
		var selection = field.selectionList || [];
		
		scope.getSelection = function () {
			return filterSelection(scope, field, selection, scope.getValue());
		};

		element.on("change", ":input", function(e) {
			var val = parseNumber(scope.field, $(e.target).val());
			scope.setValue(val, true);
			scope.$apply();
		});
		
		if (field.direction === "vertical" || field.dir === "vert") {
			setTimeout(function(){
				element.addClass("radio-select-vertical");
			});
		}
	},
	template_editable: null,
	template_readonly: null,
	template:
	'<ul ng-class="{ readonly: isReadonly() }">'+
		'<li ng-repeat="select in getSelection()">'+
		'<label>'+
			'<input type="radio" name="radio_{{$parent.$id}}" value="{{select.value}}"'+
			' ng-disabled="isReadonly()"'+
			' ng-checked="getValue() == select.value"> {{select.title}}'+
		'</label>'+
		'</li>'+
	'</ul>'
});

ui.formInput('NavSelect', {
	
	css: "nav-select",
	
	link: function(scope, element, attrs, model) {
		
		var field = scope.field;
		var selection = field.selectionList || [];

		scope.getSelection = function () {
			return filterSelection(scope, field, selection, scope.getValue()) || [];
		};
		
		scope.onSelect = function(select) {
			if (scope.attr('readonly')) {
				return;
			}
			var val = parseNumber(scope.field, select.value);
			this.setValue(val, true);
			
			elemNavs.removeClass('open');
			elemMenu.removeClass('open');
			setMenuTitle();

			// if selection change is used to show/hide some elements
			// the layout should be adjustted
			axelor.$adjustSize();
		};

		scope.isSelected = function (select) {
			return select && scope.getValue() == select.value;
		};

		scope.onMenuClick = _.once(function(e) {
			var elem = $(e.currentTarget);
			elem.dropdown();
			elem.dropdown('toggle');
		});

		var lastWidth = 0;
		var lastValue = null;
		var menuWidth = 120; // max-width
		var elemNavs = null;
		var elemMenu = null;
		var elemMenuItems = null;

		function setup() {
			elemNavs = element.children('.nav-steps').children('li:not(.dropdown,.ignore)');
			elemMenu = element.children('.nav-steps').children('li.dropdown');
			elemMenuItems = elemMenu.find('li');
		}

		function setMenuTitle() {
			var more = null;
			var show = false;
			var navs = scope.getSelection();
			elemMenuItems.each(function (i) {
				var elem = $(this);
				var nav = navs[i] || {};
				if (elem.data('visible')) show = true;
				if (scope.isSelected(nav) && show) {
					more = nav;
				}
			});
			scope.more = more;
			if (show) {
				elemMenu.show();
			}
		}

		function adjust() {
			if (elemNavs === null) {
				return;
			}
			var currentValue = scope.getValue();
			var parentWidth = element.width() - menuWidth;
			if (parentWidth === lastWidth && currentValue === lastValue) {
				return;
			}
			lastWidth = parentWidth;
			lastValue = currentValue;

			elemNavs.parent().css('visibility', 'hidden');
			elemNavs.hide();
			elemMenu.hide();
			elemMenuItems.hide().data('visible', null);
			scope.more = null;

			var count = 0;
			var width = 0;
			var last = null;
			var navs = scope.getSelection();

			while (count < navs.length) {
				var elem = $(elemNavs[count]).show();
				width += elem.width();
				if (width > parentWidth && last) {
					// show menu...
					elem.hide();
					break;
				}
				last = elem;
				count++;
			}
			if (count === elemNavs.size()) {
				elemMenu.hide();
			}
			while(count < elemNavs.size()) {
				$(elemMenuItems[count++]).show().data('visible', true);
			}
			setMenuTitle();
			elemNavs.parent().css('visibility', '');
		}

		element.on('adjustSize', adjust);
		scope.$timeout(setup);
	},
	template_editable: null,
	template_readonly: null,
	template:
		"<div class='nav-select'>" +
		"<ul class='nav-steps' style='display: inline-flex; visibility: hidden;'>" +
			"<li class='nav-step' ng-repeat='select in getSelection()' ng-class='{ active: isSelected(select), last: $last }'>" +
				"<a href='' class='nav-label' ng-click='onSelect(select)' ng-bind-html-unsafe='select.title'></a>" +
			"</li>" +
			"<li class='nav-step dropdown' ng-class='{ active: isSelected(more) }'>" +
				"<a href='' class='nav-label dropdown-toggle' ng-click='onMenuClick($event)'><span ng-bind-html-unsafe='more.title'></span></a>" +
				"<ul class='dropdown-menu pull-right' data-toggle='dropdown'>" +
					"<li ng-repeat='select in getSelection()' ng-class='{active: getValue() == select.value}'>" +
						"<a tabindex='-1' href='' ng-click='onSelect(select)' ng-bind-html-unsafe='select.title'></a>" +
					"</li>" +
				"</ul>" +
			"</li>" +
		"</ul>"+
		"</div>"
});

})(this);
