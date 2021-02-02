/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
  if (isNaN(num)) {
    return value;
  }
  return num;
}

ui.formWidget('BaseSelect', {

  findInput: function(element) {
    return element.find('input:first:not([ui-panel-editor] input)');
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

    scope.findColor = function(item) {
      return null;
    };
  },

  link_editable: function (scope, element, attrs, model) {

    var input = this.findInput(element);
    var showing = false;
    var willShow = false;

    scope.showSelection = function(delay) {
      if (scope.isReadonly() || showing || willShow) {
        return;
      }
      if (input.is('.x-focus')) {
        input.removeClass('.x-focus');
        return;
      }
      willShow = true;
      input.addClass('.x-focus');
      doSetup(input);
      setTimeout(function () {
        if (input.is(':focus')) {
          input.autocomplete("search" , '');
          input.removeClass('.x-focus');
        }
        willShow = false;
      }, delay || 100);
    };

    scope.handleClear = function(e) {
      scope.setValue(null, true);
    };

    scope.handleDelete = function(e) {

    };

    scope.handleEnter = function(e) {
      var widget = input.autocomplete('widget');
      if (widget) {
        var item = widget.find('li .ui-state-focus').parent();
        if (item.length === 0) {
          item = widget.find('li:not(.tag-select-action)');
          item = item.length === 1 ? item.first() : null;
        }
        var data = item ? item.data('ui-autocomplete-item') : null;
        if (data) {
          input.autocomplete('close');
          if (model.$viewValue !== data.value) {
            scope.setValue(data.value, true);
            scope.$applyAsync();
          }
        }
      }
    };

    scope.handleSelect = function(e, ui) {

    };

    scope.handleClose = function(e, ui) {

    };

    scope.handleOpen = function(e, ui) {

    };

    function renderItem(ul, item) {
      var el = $("<li>").append($("<a>").append($("<span>").html(item.label))).appendTo(ul);
      if (item.color) {
        el.addClass('tag-select-list-item').addClass(item.color);
      }
      if (item.click) {
        el.addClass("tag-select-action");
        ul.addClass("tag-select-action-menu");
      }
      return el;
    }

    var doSetup = _.once(function (input) {

      var loading = false;
      var pending = null;

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
          // do not select with tab key, to prevent unexpected result on editable grid
          if (event.keyCode === 9) {
            return false;
          }
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

      scope.$onAdjust('size scroll', function (e) {
        if (e.type === 'adjust:size' && e.target !== document) {
          return;
        }
        if (showing) {
          input.autocomplete('close');
        }
      });
    });

    input.focus(function(e) {
      element.addClass('focus');
      doSetup(input);
    }).blur(function() {
      element.removeClass('focus');
      if (showing) {
        input.autocomplete('close');
      }
    }).keyup(function(e) {
      // if TAB key
      if (e.which === 9) {
        scope.showSelection(300);
      }
    }).keydown(function(e) {
      var KEY = $.ui.keyCode;
      switch(e.keyCode) {
      case KEY.DELETE:
      case KEY.BACKSPACE:
        scope.handleDelete(e);
        break;
      case KEY.ENTER:
        scope.handleEnter(e);
        break;
      }
    }).click(function() {
      scope.showSelection();
    });

    if (axelor.browser.mozilla) {
      input.mousedown(function () {
        if (!input.is(':focus')) {
          scope.showSelection(300);
        }
      });
    }
  },

  template_editable:
  '<span class="picker-input">'+
    '<input type="text" autocomplete="off">'+
    '<span class="picker-icons picker-icons-2">'+
      '<i class="fa fa-times" ng-show="text" ng-click="handleClear()"></i>'+
      '<i class="fa fa-caret-down" ng-click="showSelection()"></i>'+
    '</span>'+
  '</span>'
});

function filterSelection(scope, field, selection, current) {
  var selectionIn = scope.attr('selection-in') || field.selectionIn;
  if (_.isEmpty(selection)) return selection;
  if (_.isEmpty(selectionIn)) return selection;

  var list = selectionIn;

  if (_.isString(selectionIn)) {
    var expr = selectionIn.trim();
    if (expr.indexOf('[') !== 0) {
      expr = '[' + expr + ']';
    }
    var context = scope.getContext && scope.getContext() || {};
    list = axelor.$eval(scope, expr, context);
  }

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
      selectionMap = {},
      selectionColors = {};

    var data = _.map(selectionList, function(item) {
      var value = "" + item.value;
      selectionMap[value] = item.title;
      selectionColors[value] = item.color;
      return {
        value: value,
        label: item.title || "&nbsp;",
        color: item.color
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

    scope.findColor = function(item) {
      if (!item) return null;
      if (field.colorField && field.colorField in item) {
        return item[field.colorField];
      }
      return selectionColors["" + item];
    };

    if (field.enumType) {
      var __enumValues = {};
      var __hasValue = false;

      _.each(selectionList, function (item) {
        __enumValues[item.value] = (item.data || {}).value;
        __hasValue = __hasValue || __enumValues[item.value] !== undefined;
      });

      if (__hasValue) {
        scope.$watch('record.' + field.name, function selectFieldNameWatch(value, old) {
          if (value && value !== old) {
            var enumValue = __enumValues[value];
            if (scope.record && enumValue !== value) {
              scope.record[field.name + '$value'] = enumValue;
            }
          }
        });
      }
    }
  },

  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var input = this.findInput(element);

    function update(value) {
      var val = parseNumber(scope.field, value);
      scope.setValue(val, true);
      scope.$applyAsync();
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

    scope.$on('on:edit', function () {
      // force update input text, fixes #5965
      scope.$render_editable();
    });
  }
});

ui.formInput('Enum', 'Select');

ui.formInput('ImageSelect', 'Select', {

  metaWidget: true,

  BLANK: "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",

  link: function(scope, element, attrs) {
    this._super(scope, element, attrs);

    var field = scope.field;
    var formatItem = scope.formatItem;
    var selectIcons = {};

    _.each(field.selectionList, function (item) {
      selectIcons[item.value] = item.icon || item.value;
    });

    scope.canShowText = function () {
      return field.labels === undefined || field.labels;
    };

    scope.formatItem = function (item) {
      if (scope.canShowText()) {
        return formatItem(item);
      }
      return "";
    };

    scope.findImage = function (value) {
      return selectIcons[value] || this.BLANK;
    };

    scope.$watch('getValue()', function selectFieldValueWatch(value, old) {
      scope.image = scope.findImage(value);
      scope.isIcon = scope.image && scope.image.indexOf('fa-') === 0;
      element.toggleClass('empty', !value);
    }.bind(this));
  },

  link_editable: function(scope, element, attrs) {
    this._super(scope, element, attrs);
    var input = this.findInput(element);
    var selects = {};
    _.each(scope.field.selectionList, function (item) {
      selects[item.value] = (item.data||{}).icon || item.value;
    });

    scope.renderSelectItem = function(ul, item) {
      var a = $("<a>");
      var el = $("<li>").addClass("image-select-item").append(a).appendTo(ul);
      var image = scope.findImage(item.value);

      if (image && image.indexOf('fa-') === 0) {
        a.append($("<i>").addClass("fa").addClass(image));
      } else {
        a.append($("<img>").attr("src", image));
      }

      if (scope.canShowText()) {
        a.append($("<span></span>").html(item.label));
      }

      return el;
    };

    scope.onShowSelection = function() {
      scope.$timeout(function() {
        input.focus();
        scope.showSelection();
      });
    };

    var $render_editable = scope.$render_editable;
    scope.$render_editable = function () {
      $render_editable.apply(scope, arguments);
      setTimeout(function () {
        if (!scope.canShowText()) {
          var img = element.find('i.image,img');
          img.addClass('image-select-no-labels');
        }
      });
    };
  },
  template_readonly:
    '<span class="image-select readonly">'+
      '<i ng-if="isIcon" class="fa" ng-class="image"></i>'+
      '<img ng-if="image && !isIcon" ng-src="{{image}}"></img> <span ng-if="canShowText()">{{text}}</span>' +
    '</span>',

  template_editable:
    '<span class="picker-input image-select">'+
      '<i ng-if="isIcon" class="fa" ng-class="image" ng-click="onShowSelection()"></i>'+
      '<img ng-if="image && !isIcon" ng-src="{{image}}" ng-click="onShowSelection()"></img>' +
      '<input type="text" autocomplete="off">'+
      '<span class="picker-icons">'+
        '<i class="fa fa-caret-down" ng-click="onShowSelection()"></i>'+
      '</span>'+
    '</span>'
});

ui.formInput('MultiSelect', 'Select', {

  css: 'multi-select-item',
  cellCss: 'form-item multi-select-item',
  metaWidget: true,

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
      if (_.isBlank(value)) {
        scope.items = [];
        return value;
      }
      if (!_.isArray(items)) {
        if (_.isString(items)) {
          items = items.split(/,\s*/);
        } else {
          items = ["" + items];
        }
      }
      values = _.map(items, function(item) {
        return {
          value: item,
          title: scope.formatItem(item),
          color: scope.findColor(item),
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

    input.on('input focus', function() {
      scaleInput();
    }).on('blur', function() {
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
        return input.width(width);
      }

      input.css('width', element.innerWidth() - pos.left - 24);
    }

    function update(value) {
      var val = parseNumber(scope.field, value);
      scope.setValue(val, true);
      scope.$applyAsync();
      setTimeout(function () {
        scaleInput(50);
      });
    }

    scope.selectItems = function (items) {
      update(items);
    };

    scope.removeItem = function(item) {
      var value = _.isString(item) ? item : (item||{}).value;
      var items = _.chain(this.getSelection())
          .pluck('value')
          .filter(function(v) { return !scope.matchValues(v, value); })
          .value();
      scope.selectItems(items);
    };

    scope.onShowSelection = function(e) {
      if (e && $(e.target || e.srcElement).is('input,li,i,span.tag-text')) {
        return;
      }
      input.focus();
      setTimeout(function() {
        scope.showSelection();
      });
    };

    scope.handleDelete = function(e) {
      if (input.val()) {
        return;
      }
      var items = this.getSelection();
      this.removeItem(_.last(items));
    };

    scope.handleSelect = function(e, ui) {
      var items = this.getSelection();
      var values = _.pluck(items, 'value');
      var found = _.find(values, function(v){ return scope.matchValues(v, ui.item.value); });
      if (found) {
        return false;
      }
      values.push(ui.item.value);
      scope.selectItems(values);
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

    scope.handleEnter = function(e) {
      var widget = input.autocomplete('widget');
      if (widget) {
        var item = widget.find('li .ui-state-focus').parent();
        if (item.length === 0) {
          item = widget.find('li:not(.tag-select-action)');
          item = item.length === 1 ? item.first() : null;
        }
        var data = item ? item.data('ui-autocomplete-item') : null;
        if (data) {
          var items = this.getSelection(), values = _.pluck(items, 'value');
          var found = _.find(values, function(v) {
            return scope.matchValues(v, data.value);
          });
          if (found) {
            return false;
          }
          input.autocomplete('close');
          values.push(data.value);
          scope.selectItems(values);
        }
      }
    };

    scope.$render_editable = function() {
      if (placeholder) {
        placeholder.toggleClass('hidden', !!scope.getValue());
      }
      return input.val('');
    };

    input.on("input blur", function () {
      if (placeholder) {
        placeholder.toggleClass('hidden', !!(input.val() || scope.getValue()));
      }
    });

    scope.$watch('items.length', function selectItemsLengthWatch(value, old) {
      setTimeout(function () {
        scaleInput(50);
      });
    });
  },
  template_editable:
  '<div class="tag-select picker-input" ng-click="onShowSelection($event)">'+
    '<ul>'+
    '<li class="tag-item label label-primary" ng-class="item.color" ng-repeat="item in items">'+
      '<span ng-class="{\'tag-link\': handleClick}" class="tag-text" ng-click="handleClick($event, item.value)">{{item.title}}</span> '+
      '<i class="fa fa-times fa-small" ng-click="removeItem(item)"></i>'+
    '</li>'+
    '<li class="tag-selector">'+
      '<input type="text" autocomplete="off">'+
    '</li>'+
    '</ul>'+
    '<span class="picker-icons">'+
      '<i class="fa fa-caret-down" ng-click="onShowSelection()"></i>'+
    '</span>'+
  '</div>',
  template_readonly:
  '<div class="tag-select">'+
    '<span class="label label-primary" ng-class="item.color" ng-repeat="item in limited(items)">'+
      '<span ng-class="{\'tag-link\': handleClick}" class="tag-text" ng-click="handleClick($event, item.value)">{{item.title}}</span>'+
    '</span>'+
    '<span ng-show="more"> {{more}}</span>'+
  '</div>'
});

ui.formInput('SingleSelect', 'MultiSelect', {

  link_editable: function(scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var selectItems = scope.selectItems;

    scope.selectItems = function(items) {
      selectItems.call(scope, _.last(items));
    };
  }
});

ui.formInput('SelectQuery', 'Select', {

  link_editable: function(scope, element, attrs, model) {

    this._super.apply(this, arguments);

    var current = {};

    function update(value) {
      scope.setValue(value);
      scope.$applyAsync();
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
  metaWidget: true,

  link: function(scope, element, attrs, model) {
    scope.prepareTemplate = true;

    var field = scope.field;
    var selection = field.selectionList || [];

    scope.getSelection = function () {
      return filterSelection(scope, field, selection, scope.getValue());
    };

    element.on("change", ":input", function(e) {
      var val = parseNumber(scope.field, $(e.target).val());
      scope.setValue(val, true);
      scope.$applyAsync();
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
    '<label class="ibox round">'+
      '<input type="radio" name="radio_{{$parent.$id}}" value="{{select.value}}"'+
      ' ng-disabled="isReadonly()"'+
      ' ng-checked="getValue() == select.value">'+
      '<span class="box"></span>'+
      '<span class="title">{{select.title}}</span>'+
    '</label>'+
    '</li>'+
  '</ul>'
});

ui.formInput('CheckboxSelect', {

  css: "checkbox-select",
  metaWidget: true,

  link: function(scope, element, attrs, model) {
    scope.prepareTemplate = true;

    var field = scope.field;
    var selection = field.selectionList || [];

    scope.getSelection = function () {
      return filterSelection(scope, field, selection, scope.getValue());
    };

    scope.isSelected = function (select) {
      var value = scope.getValue();
      var current = ("" + value).split(",").map(function (val) {
        return parseNumber(scope.field, val);
      });
      return current.indexOf(select.value) > -1;
    };

    element.on("change", ":input", function(e) {
      var all = element.find("input:checked");
      var selected = [];
      all.each(function () {
        var val = parseNumber(scope.field, $(this).val());
        selected.push(val);
      });
      var value =  selected.length === 0 ? null : selected.join(",");
      scope.setValue(value, true);
      scope.$applyAsync();
    });

    if (field.direction === "vertical" || field.dir === "vert") {
      setTimeout(function(){
        element.addClass("checkbox-select-vertical");
      });
    }
  },
  template_editable: null,
  template_readonly: null,
  template:
  '<ul ng-class="{ readonly: isReadonly() }">'+
    '<li ng-repeat="select in getSelection()">'+
    '<label class="ibox">'+
      '<input type="checkbox" value="{{select.value}}"'+
      ' ng-disabled="isReadonly()"'+
      ' ng-checked="isSelected(select)">'+
      '<span class="box"></span>'+
      '<span class="title">{{select.title}}</span>'+
    '</label>'+
    '</li>'+
  '</ul>'
});

ui.formInput('NavSelect', {

  css: "nav-select",
  metaWidget: true,

  link: function(scope, element, attrs, model) {
    scope.prepareTemplate = true;

    var field = scope.field;
    var selection = field.selectionList || [];
    var isReference = field.target;
    var targetName = field.targetName;

    scope.getSelection = function () {
      return filterSelection(scope, field, selection, scope.getValue()) || [];
    };

    scope.$watch('text', function navSelectTextWatch(text, old) {
      adjust();
    });

    scope.$watch('attr("selection-in")', function (filter, old) {
      if (filter !== old) {
        setup();
      }
    });

    scope.onSelect = function(select) {
      if (scope.attr('readonly')) {
        return;
      }

      var val = parseNumber(scope.field, select.value);

      if (isReference) {
        val = { id: parseInt(val) };
        // using translated value?
        if (select.data && targetName in select.data) {
          val[targetName] = select.data[targetName];
          val['$t:' + targetName] = select.title;
        } else {
          val[targetName] = select.title;
        }
      }

      this.setValue(val, true);

      elemNavs.removeClass('open');
      elemMenu.removeClass('open');

      // if selection change is used to show/hide some elements
      // the layout should be adjusted
      axelor.$adjustSize();
    };

    scope.isSelected = function (select) {
      var current = scope.getValue();
      var value = select ? (isReference || _.isNumber(current) ? parseInt(select.value) : select.value) : null;
      if (current && isReference) {
        current = current.id;
      }
      return select && value === current;
    };

    var lastWidth = 0;
    var lastValue = null;
    var elemNavs = null;
    var elemMenu = null;
    var elemMenuTitle = null;
    var elemMenuItems = null;

    function setup() {
      elemNavs = element.children('.nav-steps').children('li:not(.dropdown,.ignore)');
      elemMenu = element.children('.nav-steps').children('li.dropdown');
      elemMenuTitle = elemMenu.find('a.nav-label > span');
      elemMenuItems = elemMenu.find('li');
      adjust();
    }

    var setMenuTitle = (function() {
      var setActive = _.debounce(function(selected) {
        elemMenu.toggleClass('active', !!selected);
      });
      return function setMenuTitle(selected) {
        elemMenu.show();
        elemMenuTitle.html(selected && selected.title);
        setActive(selected);
      };
    }());

    function adjust() {
      if (elemNavs === null || element.is(":hidden")) {
        return;
      }
      var currentValue = scope.getValue();
      var parentWidth = element.width() - 16;
      if (parentWidth === lastWidth && currentValue === lastValue) {
        return;
      }
      lastWidth = parentWidth;
      lastValue = currentValue;

      elemNavs.parent().css('visibility', 'hidden');
      elemNavs.show();
      elemMenu.hide();

      if (elemNavs.parent().width() <= parentWidth) {
        elemNavs.parent().css('visibility', '');
        return;
      }

      var navs = scope.getSelection();
      var selected = _.find(navs, scope.isSelected.bind(scope));
      var selectedIndex = navs.indexOf(selected);

      var elem = null;
      var index = navs.length;

      setMenuTitle(null);

      while (index >= 0 && elemNavs.parent().width() > parentWidth) {
        elem = $(elemNavs[--index]);
        elem.hide();
        if (index === selectedIndex) {
          setMenuTitle(selected);
        }
      }

      elemMenuItems.hide();
      while(index < navs.length) {
        $(elemMenuItems[index++]).show();
      }

      elemNavs.parent().css('visibility', '');
    }

    scope.$onAdjust(adjust);
    scope.$callWhen(function () {
      return element.is(':visible');
    }, setup);
  },
  template_editable: null,
  template_readonly: null,
  template:
    "<div class='nav-select'>" +
      "<ul class='nav-steps' style='display: inline-flex; visibility: hidden;'>" +
        "<li class='nav-step' ng-repeat='select in getSelection()' ng-class='{ active: isSelected(select), last: $last }'>" +
          "<a href='' class='nav-label' ng-click='onSelect(select)' ng-bind-html='select.title'></a>" +
        "</li>" +
        "<li class='nav-step dropdown'>" +
          "<a href='' class='nav-label dropdown-toggle' data-toggle='dropdown'><span></span></a>" +
          "<ul class='dropdown-menu pull-right'>" +
            "<li ng-repeat='select in getSelection()' ng-class='{active: getValue() == select.value}'>" +
              "<a tabindex='-1' href='' ng-click='onSelect(select)' ng-bind-html='select.title'></a>" +
            "</li>" +
          "</ul>" +
        "</li>" +
      "</ul>"+
    "</div>"
});

ui.formInput('ThemeSelect', 'Select', {

  init: function (scope) {
    scope.field.selectionList = _.map(axelor.config['application.themes'], function (name) {
      return { value: name, title: _.titleize(name) };
    });
    scope.field.selectionList.unshift({
      value: "default",
      title: "Default"
    });
    this._super(scope);
  }
});

ui.formInput('WidgetSelect', 'Select', {

  init: function (scope) {
    scope.field.selectionList = _.map(ui.getMetaWidgets(), function (name) {
      return { value: name, title: name };
    });
    this._super(scope);
  }
});

})();
