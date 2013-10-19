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
			input.autocomplete("search" , '');
			input.focus();
		};

		scope.handleDelete = function(e) {

		};

		scope.handleSelect = function(e, ui) {
			
		};
		
		scope.handleClose = function(e, ui) {
			
		};
		
		scope.handleOpen = function(e, ui) {
			
		};

		input.autocomplete({
			
			minLength: 0,
			
			source: function(request, response) {
				scope.loadSelection(request, response);
			},

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
				scope.handleOpen(event, ui);
			},
			
			close: function(event, ui) {
				scope.handleClose(event, ui);
			}
		});
		
		var showOn = this.showSelectionOn;

		input.focus(function() {
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
			if (showOn === "click") {
				scope.showSelection();
			}
		});
		
		input.data('ui-autocomplete')._renderItem = function(ul, item) {
			var el = $("<li>").append( $("<a>").html( item.label ) )
							  .appendTo(ul);
			if (item.click) {
				el.addClass("tag-select-action");
				ul.addClass("tag-select-action-menu");
			}
			return el;
		};
	},

	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
	'</span>'
});

ui.formInput('Select', 'BaseSelect', {

	css: 'select-item',
	cellCss: 'form-item select-item',

	init: function(scope) {
		
		this._super(scope);

		var field = scope.field,
			selection = field.selection || [],
			selectionMap = {};
		
		if (_.isArray(field.selection)) {
			selection = field.selection;
		}

		var data = _.map(selection, function(item) {
			var value = "" + item.value;
			selectionMap[value] = item.title;
			return {
				value: value,
				label: item.title || "&nbsp;"
			};
		});

		scope.loadSelection = function(request, response) {
			var items = _.filter(data, function(item) {
				var label = item.label || "",
					term = request.term || "";
				return label.toLowerCase().indexOf(term.toLowerCase()) > -1;
			});
			response(items);
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
			scope.setValue(value, true);
			setTimeout(function(){
				scope.$apply();
			});
		}

		scope.handleDelete = function(e) {
			if (e.keyCode === 46) { // DELETE
				update('');
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
			scope.setValue(value, true);
			setTimeout(function(){
				scope.$apply();
			});
		}
		
		scope.removeItem = function(item) {
			var items = this.getSelection(),
				value = _.isString(item) ? item : item.value;

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
			if (e && $(e.srcElement).is('li,i,span.tag-text')) {
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
			return input.val('');
		};
	},
	template_editable:
	'<div class="tag-select picker-input">'+
	  '<ul ng-click="showSelection($event)">'+
		'<li class="tag-item label label-info" ng-repeat="item in items">'+
			'<span ng-class="{\'tag-link\': handleClick}" class="tag-text" ng-click="handleClick($event, item.value)">{{item.title}}</span> '+
			'<i class="icon-remove icon-small" ng-click="removeItem(item)"></i>'+
		'</li>'+
		'<li class="tag-selector">'+
			'<input type="text" autocomplete="off">'+
		'</li>'+
	  '</ul>'+
	  '<span class="picker-icons">'+
	  	'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
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
			setTimeout(function(){
				scope.$apply();
			});
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
		
		var field = scope.field,
			selection = [];
	
		if (_.isArray(field.selection)) {
			selection = field.selection;
		}
		scope.selection = selection;

		element.on("change", ":input", function(e) {
			scope.setValue($(e.target).val(), true);
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
		'<li ng-repeat="select in selection">'+
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
		
		var field = scope.field,
			selection = [];
	
		if (_.isArray(field.selection)) {
			selection = field.selection;
		}
		scope.selection = selection;
		
		scope.onSelect = function(select) {
			if (scope.attr('readonly')) {
				return;
			}
			this.setValue(select.value, true);
			
			// if selection change is used to show/hide some elements
			// the layout should be adjustted
			$.event.trigger('adjustSize');
		};

	},
	template_editable: null,
	template_readonly: null,
	template:
	'<div class="nav-select">'+
	'<ul class="steps">'+
		'<li ng-repeat="select in selection" ng-class="{ active: getValue() == select.value }">'+
			'<a href="" ng-click="onSelect(select)">{{select.title}}</a>'+
		'</li>'+
		'<li></li>'+
	'</ul>'+
	'</div>'
});

})(this);
