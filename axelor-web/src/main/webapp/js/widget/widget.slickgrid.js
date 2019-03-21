/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

/* global Slick: true */

"use strict";

var ui = angular.module('axelor.ui');

//used to keep track of columns by x-path
function setDummyCols(element, cols) {
  var e = $('<div>').appendTo(element).hide();
  _.each(cols, function(col, i) {
    $('<span class="slick-dummy-column">')
      .data('column', col)
      .attr('x-path', col.xpath)
      .appendTo(e);
  });
}

function makeFilterCombo(input, selection, callback) {

  var data = _.map(selection, function(item){
    return {
      key: item.value,
      value: item.title
    };
  });

  function update(item) {
    var filter = {};
    item = item || {};
    input.val(item.value || '');
    filter[input.data('columnId')] = item.key || '';
    callback(filter);
  }

  input.autocomplete({
    minLength: 0,
    source: data,
    focus: function(event, ui) {
      return false;
    },
    select: function(event, ui) {
      update(ui.item);
      return false;
    }
  }).keyup(function(e){
    return false;
  }).keydown(function(e){
    switch(e.keyCode) {
    case 8:		// backspace
    case 46:	// delete
      update(null);
    }
  }).click(function(){
    input.autocomplete("search", "");
  });

  $("<i class='fa fa-caret-down combo-icon'></i>").appendTo(input.parent()).click(function () {
    input.focus();
    input.autocomplete("search", "");
  });
}

var Editor = function(args) {

  var element;
  var column = args.column;
  var scope;
  var external;

  var form = $(args.container)
    .parents('[ui-slick-grid]:first')
    .find('[ui-slick-editors]:first');

  element = form.find('[x-field="'+ column.field +'"]');
  scope = form.data('$scope');

  if (!element.parent().is('td.form-item'))
    element = element.parent();
  element.data('$parent', element.parent());
  element.data('$editorForm', form);

  external = element.is('.text-item,.html-item');

  this.init = function() {

    var container = $(args.container);
    if (external) {
      container = container.parents('.ui-dialog-content:first,.view-container:first').first();
      $(document).on('mousedown.slick-external', function (e) {
        if (element.is(e.target) || element.find(e.target).length > 0) {
          return;
        }
        args.grid.getEditorLock().commitCurrentEdit();
      });
    }

    element.css('display', 'inline-block')
      .addClass('slick-external-editor')
      .appendTo(container)
      .hide();

    if (!element.data('keydown.nav')) {
      element.data('keydown.nav', true);
      element.bind("keydown.nav", function (e) {
        switch (e.keyCode) {
        case 37: // LEFT
        case 39: // RIGHT
        case 38: // UP
        case 40: // DOWN
          e.stopImmediatePropagation();
          break;
        case 9: // TAB
          if (external) {
            args.grid.onKeyDown.notify(args.grid.getActiveCell(), e);
            return false;
          }
        }
      });

      element.bind('close:slick-editor', function(e) {
        var grid = args.grid;
        var lock = grid.getEditorLock();
        if (lock.isActive()) {
          lock.commitCurrentEdit();
        }
      });
    }
  };

  this.shouldWait = function () {
    var es = element.scope();
    if (es && es.field && es.field.onChange) {
      return true;
    }
    return false;
  };

  this.destroy = function() {
    scope.$lastEditor = this;
    element.appendTo(element.data('$parent') || form)
         .removeData('$parent')
         .removeData('$editorForm');
    element.trigger("hide:slick-editor");
    element.parent().zIndex('');
    $(document).off('mousedown.slick-external');
  };

  this.position = function(pos) {
    //XXX: ui-dialog issue
    var zIndex = element.parents('.slickgrid:first').zIndex();
    if (zIndex) {
      element.parent().zIndex(zIndex);
    }
    if (external) {
      setTimeout(adjustExternal);
    }
  };

  function adjustExternal() {

    var container = $(args.container);
    var parent = element.data('$parent') || element;
    var zIndex = (parent.parents('.slickgrid:first').zIndex() || 0) + container.zIndex();
    element.css({
      border: 0,
      width: container.width(),
      zIndex: zIndex + 1
    });
    element.css('position', 'absolute');
    element.position({
      my: 'left top',
      at: 'left top',
      of: args.container
    });

    // focus html editor
    if (element.is('.html-item')) {
      element.find('[contenteditable]').focus();
    }
  }

  function focus() {
    // Firefox throws exception if element is hidden
    if (element.is(':hidden')) return;
    if (element.is(':input')) {
      element.focus().select();
    } else {
      element.find(':input:first').focus().select();
    }
    if (element.is('[x-cell-css*=select-item]') && element.scope().showSelection) {
      element.scope().showSelection(300);
    }
  }

  this.focus = function() {
    _.delay(focus);
  };

  this.loadValue = function(item) {
    var that = this,
      record = scope.record || {},
      current = item || { id: 0 },
      updated = false;

    if ((!current.id || current.id < 0) && (current[column.field] === undefined)) {
      var defaults = scope.$events.onNew ? scope.record : scope.defaultValues;
      current[column.field] = (defaults||{})[column.field] || null;
    }

    var changed = (record.id !== item.id || record.version !== current.version);
    if (changed) {
      scope.editRecord(current);
    } else {
      record[column.field] = current[column.field];
      updated = true;
    }
    setTimeout(function(){
      if (updated) {
        scope.$applyAsync();
      }
      element.show();
      that.focus();
    });
  };

  this.serializeValue = function() {
    var record = scope.record || {};
    var value = record[column.field];
    return value === undefined ? "" : value;
  };

  this.applyValue = function(item, state) {
    if (item.id === undefined) {
      item = _.extend(item, scope.record);
    }
    item[column.field] = state;
    item.$dirty = true;
    if (item.id === undefined) {
      args.grid.onCellChange.notify(args.grid.getActiveCell());
    }
  };

  this.isValueChanged = function() {

    // force change event on spinner widget
    element.find('.ui-spinner-input').trigger('grid:check', args.item);
    element.find('.ui-mask').trigger('grid:check');

    var record = scope.record || {};
    var current = args.item || { id: 0 };

    var v1 = record[column.field];
    var v2 = current[column.field];

    var changed = !angular.equals(v1, v2);
    if (changed && element.scope().isRequired() && !v1) {
      current[column.field] = v1;
    }

    return changed;
  };

  this.validate = function() {
    return {
      valid: !element.hasClass('ng-invalid'),
      msg: null
    };
  };

  this.init();
};

var Formatters = {

  "string": function(field, value, context) {
    return ui.formatters.string(field, value, context);
  },

  "integer": function(field, value) {
    return ui.formatters.integer(field, value);
  },

  "decimal": function(field, value, context) {
    return ui.formatters.decimal(field, value, context);
  },

  "boolean": function(field, value) {
    return value ? '<i class="fa fa-check"></i>' : "<i class='fa'></i>";
  },

  "duration": function(field, value) {
    return ui.formatDuration(field, value);
  },

  "date": function(field, value) {
    return value ? moment(value).format('DD/MM/YYYY') : "";
  },

  "time": function(field, value) {
    return value ? value : "";
  },

  "datetime": function(field, value) {
    return value ? moment(value).format('DD/MM/YYYY HH:mm') : "";
  },

  "one-to-one": function(field, value) {
    var text = (value||{})[field.targetName];
    return text ? _.escapeHTML(text) : "";
  },

  "many-to-one": function(field, value) {
    var text = (value||{})[field.targetName];
    return text ? _.escapeHTML(text) : "";
  },

  "one-to-many": function(field, value) {
    return value ? '(' + value.length + ')' : "";
  },

  "many-to-many": function(field, value) {
    return value ? '(' + value.length + ')' : "";
  },

  "button": function(field, value, context, grid) {
    var elem;
    var isIcon = field.icon && field.icon.indexOf('fa-') === 0;
    var css = isIcon ? "slick-icon-button fa " + field.icon : "slick-img-button";
    var help = field.help || field.title;
    var handler = grid.scope.handler;

    if (field.readonlyIf && axelor.$eval(grid.scope, field.readonlyIf, _.extend({}, handler._context, context))) {
      css += " readonly disabled";
    }

    if(isIcon) {
      elem = '<a href="javascript: void(0)" tabindex="-1"';
      if (help) {
        elem += ' title="' + _.escapeHTML(help) + '"';
      }
      elem += '><i class="' + css + '"></i></a>';
    } else if (field.icon) {
      elem = '<img class="' + css + '" src="' + field.icon + '"';
      if (help) {
        elem += ' title="' + _.escapeHTML(help) + '"';
      }
      elem += '>';
    } else {
      return "";
    }

    return elem;
  },

  "progress": function(field, value) {
    var props = ui.ProgressMixin.compute(field, value);
    return '<div class="progress ' + props.css + '" style="height: 18px; margin: 0; margin-top: 1px;">'+
      '<div class="bar" style="width: ' + props.width +'%;"></div>'+
    '</div>';
  },

  "selection": function(field, value) {
    var cmp = field.type === "integer" ? function(a, b) { return a == b ; } : _.isEqual;
    var findSelect = function (v) {
      var val = field.type === "integer" ? v : _.unescapeHTML(v);
      var res = _.extend({}, _.find(field.selectionList, function(item) {
        return cmp(item.value, val);
      }));
      if (_.isString(res.title)) {
        res.title = _.escapeHTML(res.title);
      }
      return res;
    };

    if (value && field.widget === 'MultiSelect') {
      var items = value.split(/\s*,\s*/).map(findSelect).map(function (res) {
        return '<span class="label label-primary"><span class="tag-text">'+res.title+'</span></span>';
      });
      return '<span class="tag-select">' + items.join(' ') + '</span>';
    }

    var res = findSelect(value);
    var text = res.title;
    if (field.widget === 'ImageSelect' && res.icon) {
      var image = "<img style='max-height: 24px;' src='" + (res.icon || res.value) + "'>";
      return field.labels === false ? image : image + " " + text;
    }
    return text;
  },

  "url": function(field, value) {
    return '<a target="_blank" ng-show="text" href="' + _.escapeHTML(value) + '">' + _.escapeHTML(value) + '</a>';
  },

  "icon": function(field, value, dataContext, grid) {
    if (value && value.indexOf("fa-") > -1) {
      return '<i class="slick-icon ' + value + '"></i>';
    }
    return Formatters.button(field, value, dataContext, grid);
  },

  "jsonRef": function (field, value) {
    var val = _.extend({}, value);
    var id = val.id;
    if (!id || id < 0) return '';
    delete val.model;
    delete val.version;
    delete val.id;
    var vals = _.flatten([id, _.values(val)]);
    return '[' + vals.join(', ') + ']';
  },

  "json": function(field, value) {
    if (!value || !field.jsonFields || field.jsonFields.length === 0) return "";
    var that = this;
    var items = [];
    var json = angular.fromJson(value);
    field.jsonFields.forEach(function (item) {
      if (json[item.name] === undefined || json[item.name] === null) return;
      var value = json[item.name];
      var type = item.selection ? 'selection' : item.type;
      if (item.widget === 'json-ref-select') type = 'jsonRef';
      var func = that[type];
      if (func) {
        value = func(item, value);
      }
      items.push('<strong>' + item.title + '</strong>: ' + value);
    });

    return items.join(' &bull; ');
  }
};

function totalsFormatter(totals, columnDef) {

  var field = columnDef.descriptor;
  if (["integer", "long", "decimal"].indexOf(field.type) === -1) {
    return "";
  }

  var vals = totals[field.aggregate || 'sum'] || {};
  var val = vals[field.name];

  var formatter = Formatters[field.type];
  if (formatter) {
    return formatter(field, val);
  }

  return val;
}

function Factory(grid) {
  this.grid = grid;
}

_.extend(Factory.prototype, {

  getEditor : function(col) {
    var field = col.descriptor;
    if (!field || field.readonly || col.forEdit === false) {
      return null;
    }
    if (field.type == 'binary') {
      return null;
    }
    if (!col.editor) {
      col.editor = Editor;
    }
    return col.editor;
  },

  getFormatter: function(col) {
    return _.bind(this.formatter, this);
  },

  formatter: function(row, cell, value, columnDef, dataContext) {

    var field = columnDef.descriptor || {},
      attrs = _.extend({}, field, field.widgetAttrs),
      widget = attrs.widget || "",
      type = attrs.type;

    if (widget === 'json-field' || attrs.json) {
      return Formatters.json(field, value);
    }

    if (attrs.jsonPath && attrs.jsonField) {
      var jsonValue = dataContext[attrs.name];
      if (jsonValue === undefined) {
        jsonValue = dataContext[attrs.jsonField];
        if (jsonValue) {
          jsonValue = angular.fromJson(jsonValue);
          value = jsonValue[attrs.jsonPath];
        }
      } else if (jsonValue && attrs.target) { // relational field value
        value = angular.fromJson(jsonValue);
      }
    }

    if (widget === "Progress" || widget === "progress" || widget === "SelectProgress") {
      type = "progress";
    }
    if (_.isArray(field.selectionList) && widget !== "SelectProgress") {
      type = "selection";
    }

    if (typeof value === 'string') {
      value = axelor.sanitize(value);
    }

    if (type === "button" || type === "progress") {
      return Formatters[type](field, value, dataContext, this.grid);
    }

    if (["Url", "url", "duration"].indexOf(widget) > 0) {
      type = widget.toLowerCase();
    }

    if (widget.toLowerCase() === "image" || widget.toLowerCase() === "binary-link" || (type === "binary" && field.name === "image")) {
      var url = null;
      if (field.target === "com.axelor.meta.db.MetaFile") {
        if (value) {
          url = ui.makeImageURL("com.axelor.meta.db.MetaFile", "content", (value.id || value));
        }
        if (url && widget.toLowerCase() === "binary-link") {
          return '<a href="' + url + '" download="' + value.fileName + '">' + value.fileName + '</a>';
        }
      } else {
        url = ui.makeImageURL(this.grid.handler._model, field.name, dataContext) + "&image=true";
      }
      return url ? '<img src="' + url + '" style="height: 21px;margin-top: -2px;">' : '';
    }

    if (widget.toLowerCase() === "html") {
      return value ? '<span>' + value + '</span>' : '';
    }

    // try to get dotted field value from related object
    if ((value === null || value === undefined) && field.name && field.name.indexOf('.') > -1) {
      var path = field.name.split('.');
      var val = dataContext || {};
      var idx = 0;
      while (val && idx < path.length) {
        val = val[path[idx++]];
      }
      if (idx === path.length) {
        value = val;
      }
    }

    var fn = Formatters[type];
    if (fn) {
      value = fn(field, value, dataContext, this.grid);
    } else if (_.isString(value)) {
      value = _.escapeHTML(value);
     }
    if (value === null || value === undefined || (_.isObject(value) && _.isEmpty(value))) {
      return "";
    }
    return value;
  },

  formatProgress: function(field, value) {

    var props = ui.ProgressMixin.compute(field, value);

    return '<div class="progress ' + props.css + '" style="height: 18px; margin: 0; margin-top: 1px;">'+
      '<div class="bar" style="width: ' + props.width +'%;"></div>'+
    '</div>';
  },

  formatDecimal: function(field, value) {
    var scale = field.scale || 2,
      num = +(value);
    if (num) {
      return num.toFixed(scale);
    }
    return value;
  },

  formatButton: function(field, value, columnDef) {
    return '<img class="slick-img-button" src="' + field.icon + '">';
  }
});

var Grid = function(scope, element, attrs, ViewService, ActionService) {

  var noFilter = scope.$eval('noFilter');
  if (_.isString(noFilter)) {
    noFilter = noFilter === 'true';
  }

  this.compile = function(template) {
    return ViewService.compile(template)(scope.$new());
  };

  this.newActionHandler = function(scope, element, options) {
    return ActionService.handler(scope, element, options);
  };

  this.scope = scope;
  this.element = element;
  this.attrs = attrs;
  this.handler = scope.handler;
  this.showFilters = !noFilter;
  this.$oldValues = null;
  this.grid = this.parse(scope.view);
};

function buttonScope(scope) {
  var btnScope = scope.$new();
  var handler = scope.handler;

  btnScope._dataSource = handler._dataSource;
  btnScope.editRecord = function (record) {};
  btnScope.reload = function () {
    if ((handler.field||{}).target) {
      handler.$parent.reload();
    }
    return handler.onRefresh();
  };
  if ((handler.field||{}).target) {
    btnScope.onSave = function () {
      return handler.$parent.onSave.call(handler.$parent, {
        callOnSave: false,
        wait: false
      });
    };
  }

  return btnScope;
}

Grid.prototype.parse = function(view) {

  var that = this,
    scope = this.scope,
    handler = scope.handler,
    dataView = scope.dataView,
    element = this.element;

  scope.fields_view = {};

  var cols = [];
  var allColsHasWidth = true;

  _.each(view.items, function(item) {
    var field = handler.fields[item.name] || {},
      path = handler.formPath, type;

    type = (item.widgetAttrs||{}).type || field.type || item.serverType || item.type || 'string';

    field = _.extend({}, field, item, {type: type});
    scope.fields_view[item.name] = field;
    path = path ? path + '.' + item.name : item.name;

    if (type === 'field' || field.selection) {
      type = 'string';
    }

    if (!item.width) {
      allColsHasWidth = false;
    }

    var sortable = view.sortable !== undefined && field.sortable === undefined
      ? view.sortable !== false
      : field.sortable !== false;

    switch (field.type) {
    case 'field': // dummy field
    case 'icon':
    case 'button':
    case 'one-to-many':
    case 'many-to-many':
      sortable = false;
      break;
    }

    if (field.transient || field.json || field.encrypted) {
      sortable = false;
    }

    if (field.type == "button") {
      if (scope.selector) return;
      field.image = field.title;
      field.handler = that.newActionHandler(buttonScope(scope), element, {
        action: field.onClick
      });
    }

    if (field.type == "button" || field.type == "icon") {
      item.title = "&nbsp;";
      item.width = field.width || 32;
    }

    var column = {
      name: item.title || field.title || item.autoTitle || _.chain(item.name).humanize().titleize().value(),
      id: item.name,
      field: item.name,
      toolTip: item.help,
      forEdit: item.forEdit,
      descriptor: field,
      sortable: sortable,
      width: parseInt(item.width) || null,
      hasWidth: item.width ? true : false,
      cssClass: type,
      headerCssClass: type,
      xpath: path
    };

    var minWidth = view.colWidth || 100;

    column.minWidth = Math.min(minWidth, column.width || minWidth);
    column._title = column.name;

    var css = [type];
    if (item.forEdit !== false) {
      if (!field.readonly) {
        css.push('slick-cell-editable');
      }
      if (field.required) {
        css.push('slick-cell-required');
      }
    }
    column.cssClass = css.join(' ');

    cols.push(column);

    if (field.aggregate) {
      column.groupTotalsFormatter = totalsFormatter;
    }

    if (field.type === "button" || field.type === "boolean" || field.type === "icon") {
      return;
    }

    var menus = [sortable ? {
      iconImage: "lib/slickgrid/images/sort-asc.gif",
      title: _t("Sort Ascending"),
      command: "sort-asc"
    } : null, sortable ? {
      iconImage: "lib/slickgrid/images/sort-desc.gif",
      title: _t("Sort Descending"),
      command: "sort-desc"
    } : null, sortable ? {
      separator: true
    } : null, view.editable ? null : {
      title: _t("Group by") + " <i>" + column.name + "</i>",
      command: "group-by"
    }, view.editable ? null : {
      title: _t("Ungroup"),
      command: "ungroup"
    }, view.editable ? null : {
      separator: true
    }, {
      title: _t("Hide") + " <i>" + column.name + "</i>",
      command: "hide"
    }];

    menus = _.compact(menus);

    column.header = {
      menu: {
        items: menus,
        position: function($menu, $button) {
          $menu.css('top', 0)
             .css('left', 0);
          $menu.position({
            my: 'left top',
            at: 'left bottom',
            of: $button
          });
        }
      }
    };
  });

  // if all columns are fixed width, add a dummy column
  if (allColsHasWidth) {
    cols.push({
      name: "&nbsp;"
    });
  }

  // create edit column
  var editColumn = null;
  if (view.editIcon && (!scope.selector || scope.selector === "checkbox") && (!handler.hasPermission || handler.hasPermission('write'))) {
    editColumn = new EditIconColumn({
      onClick: function (e, args) {
        if (e.isDefaultPrevented()) {
          return;
        }
        e.preventDefault();
        var elem = $(e.target);
        if (elem.is('.fa-minus') && handler) {
          return handler.dataView.deleteItem(0);
        }
        if (handler && handler.onEdit) {
          handler.waitForActions(function () {
            args.grid.setActiveCell(args.row, args.cell);
            handler.$applyAsync(function () {
              handler.onEdit(true);
            });
          });
        }
      }
    });
    cols.unshift(editColumn.getColumnDefinition());
  }

  // create checkbox column
  var selectColumn = null;
  if (scope.selector) {
    selectColumn = new Slick.CheckboxSelectColumn({
      cssClass: "slick-cell-checkboxsel",
      multiSelect: scope.selector !== "single"
    });

    cols.unshift(_.extend(selectColumn.getColumnDefinition(), {
      headerCssClass: "slick-cell-checkboxsel"
    }));
  }

  // add column for re-ordering rows
  this._canMove = view.canMove && view.orderBy === "sequence" && !view.groupBy;
  if (this._canMove) {
    cols.push({
      id: "_move_column",
        name: "",
        width: 32,
        behavior: "selectAndMove",
        selectable: false,
        resizable: false,
        cssClass: "fa fa-bars move-icon",
        canMove: function () {
          if (handler.isReadonly && handler.isReadonly()) {
            return false;
          }
          return true;
        }
    });
  }

  var factory = new Factory(this);

  var options = {
    rowHeight: Math.max(view.rowHeight || 26, 26),
    editable: view.editable && !axelor.device.mobile,
    editorFactory:  factory,
    formatterFactory: factory,
    enableCellNavigation: true,
    enableColumnReorder: false,
    fullWidthRows: true,
    multiColumnSort: true,
    showHeaderRow: this.showFilters,
    multiSelect: scope.selector !== "single",
    explicitInitialization: true
  };

  var grid = new Slick.Grid(element, dataView, cols, options);

  this.cols = cols;
  this.grid = grid;

  this._selectColumn = selectColumn;
  this._editColumn = editColumn;

  element.show();
  element.data('grid', grid);

  // delegate some methods to handler scope
  handler.showColumn = _.bind(this.showColumn, this);
  handler.resetColumns = _.bind(this.resetColumns, this);
  handler.setColumnTitle = _.bind(this.setColumnTitle, this);
  handler.getVisibleCols = _.bind(this.getVisibleCols, this);

  // set dummy columns to apply attrs if grid is not initialized yet
  setDummyCols(element, this.cols);

  function adjustSize(event, force) {
    scope.ajaxStop(function () {
      var forceAdjust = force || handler._isPopup;
      if (forceAdjust || element.is(':visible')) {
        that.adjustSize(forceAdjust);
        if (forceAdjust) {
          setTimeout(grid.autosizeColumns, 100);
        }
      }
    });
  }

  scope.$on('grid:adjust-size', function (e, viewScope) {
    adjustSize(e, viewScope === handler);
  });

  scope.$onAdjust(adjustSize, 100); // handle global events

  scope.$callWhen(function () {
    return element.is(':visible');
  }, adjustSize, 100);

  element.addClass('slickgrid-empty');
  this.doInit = _.once(function doInit() {
    this._doInit(view);
    element.removeClass('slickgrid-empty');
  }.bind(this));

  handler._dataSource.on('change', function (e, records, page) {
    element.toggleClass('slickgrid-empty-message', page && page.size === 0);
  });

  var emptyMessage = handler.$emptyMessage || _t("No records found.");
  element.append($("<div class='slickgrid-empty-text'>").hide().text(emptyMessage));

  return grid;
};

Grid.prototype._doInit = function(view) {

  var that = this,
    grid = this.grid,
    scope = this.scope,
    handler = this.scope.handler,
    dataView = this.scope.dataView,
    element = this.element;

  var headerMenu = new Slick.Plugins.HeaderMenu({
    buttonImage: "lib/slickgrid/images/down.gif"
  });

  var rowMoveManager = new Slick.RowMoveManager({
    cancelEditOnDrag: true
  });

  grid.setSelectionModel(new Slick.RowSelectionModel());
  grid.registerPlugin(new Slick.Data.GroupItemMetadataProvider());
  grid.registerPlugin(headerMenu);
  if (this._selectColumn) {
    grid.registerPlugin(this._selectColumn);
  }
  if (this._editColumn) {
    grid.registerPlugin(this._editColumn);
  }
  if (this._canMove) {
    grid.registerPlugin(rowMoveManager);
  }

  // performance tweaks
  var _containerH = 0;
  var _containerW = 0;
  var _resizeCanvas = grid.resizeCanvas;
  grid.resizeCanvas = _.debounce(function() {
    var w = element.width(),
      h = element.height();
    if (element.is(':hidden') || (w === _containerW && h === _containerH)) {
      return;
    }
    _containerW = w;
    _containerH = h;
    _resizeCanvas.call(grid);
  }, 100);

  grid.init();
  this.$$initialized = true;

  // end performance tweaks

  dataView.$syncSelection = function(old, oldIds, focus) {
    var selection = dataView.mapIdsToRows(oldIds || []);
    if (!focus) {
      _.each(dataView.getItems(), function (item, i) {
        if (item.selected) {
          selection.push(i);
        }
      });
    }
    selection = _.unique(selection);
    grid.setSelectedRows(selection);
    if (selection.length === 0 && !grid.getEditorLock().isActive()) {
      grid.setActiveCell(null);
        } else if (focus) {
          grid.setActiveCell(_.first(selection), 1);
          grid.focus();
        }
  };
  dataView.$setSelection = function(selection, focus) {
    var rows = selection || [];
    grid.setSelectedRows(rows);
    if (selection.length === 0 && !grid.getEditorLock().isActive()) {
      grid.setActiveCell(null);
        } else if (focus) {
          grid.setActiveCell(_.first(selection), 1);
          grid.focus();
        }
  };

  if (this._canMove) {
    dataView.$resequence = _.bind(this._resequence, this);
  }

  // register grid event handlers
  this.subscribe(grid.onSort, this.onSort);
  this.subscribe(grid.onSelectedRowsChanged, this.onSelectionChanged);
  this.subscribe(grid.onClick, this.onItemClick);
  this.subscribe(grid.onDblClick, this.onItemDblClick);

  this.subscribe(grid.onKeyDown, this.onKeyDown);
  this.subscribe(grid.onCellChange, this.onCellChange);
  this.subscribe(grid.onBeforeEditCell, this.onBeforeEditCell);

  // register dataView event handlers
  this.subscribe(dataView.onRowCountChanged, this.onRowCountChanged);
  this.subscribe(dataView.onRowsChanged, this.onRowsChanged);

  // register header menu event handlers
  this.subscribe(headerMenu.onBeforeMenuShow, this.onBeforeMenuShow);
  this.subscribe(headerMenu.onCommand, this.onMenuCommand);

  // register row move handlers
  this.subscribe(rowMoveManager.onBeforeMoveRows, this.onBeforeMoveRows);
  this.subscribe(rowMoveManager.onMoveRows, this.onMoveRows);

  // hilite support
  var getItemMetadata = dataView.getItemMetadata;
  dataView.getItemMetadata = function (row) {
    var item = grid.getDataItem(row);
    if (item && !item.$style) {
      that.hilite(row);
    }
    var meta = getItemMetadata.apply(dataView, arguments);
    var my = that.getItemMetadata(row);
    if (my && meta && meta.cssClasses) {
      my.cssClasses += " " + meta.cssClasses;
    }
    return meta || my;
  };
  this.subscribe(grid.onCellChange, function (e, args) {
    that.hilite(args.row);
    grid.invalidateRow(args.row);
    grid.render();
  });

  function setFilterCols() {

    if (!that.showFilters) {
      return;
    }

    var filters = {};
    var filtersRow = $(grid.getHeaderRow());

    function updateFilters(event) {
      /* jshint validthis: true */
      var elem = $(this);
      if (elem.is('.ui-autocomplete-input')) {
        return;
      }
      filters[$(this).data('columnId')] = $(this).val().trim();
    }

    function clearFilters() {
      filters = {};
      filtersRow.find(":input").val("");
    }

    handler.clearFilters = clearFilters;

    filtersRow.on('keyup', ':input', updateFilters);
    filtersRow.on('keypress', ':input', function(event){
      if (event.keyCode === 13) {
        updateFilters.call(this, event);
        scope.handler.filter(filters, that.advanceFilter);
      }
    });

    function _setInputs(cols) {
      _.each(cols, function(col){
        if (!col.xpath || col.descriptor.type === 'button' || col.descriptor.json || col.descriptor.encrypted) return;
        var header = grid.getHeaderRowColumn(col.id),
          input = $('<input type="text">').data("columnId", col.id).appendTo(header),
          field = col.descriptor || {};
        input.on("change", function () {
          input.attr('placeholder', input.is(':focus') ? _t('Search...') : null);
        });
        input.on("focus", function () {
          input.attr('placeholder', _t('Search...'));
        });
        input.on("blur", function () {
          input.attr('placeholder', '');
        });
        if (_.isArray(field.selectionList)) {
          makeFilterCombo(input, field.selectionList, function(filter){
            _.extend(filters, filter);
          });
        }
      });
    }

    var _setColumns = grid.setColumns;
    grid.setColumns = function(columns) {
      _setColumns.apply(grid, arguments);
      _setInputs(columns);
    };

    _setInputs(that.cols);
  }

  setFilterCols();

  var onInit = scope.onInit();
  if (_.isFunction(onInit)) {
    onInit(grid, this);
  }

  if (view.groupBy) {
    this.groupBy(view.groupBy);
  }

  setTimeout(function () {
    grid.setColumns(that.getVisibleCols());
  });

  if (scope.$parent._viewResolver) {
    scope.$parent._viewResolver.resolve(view, element);
  }

  scope.$on("cancel:grid-edit", function(e) {

    if (that.$oldValues && that.canSave()){

      dataView.beginUpdate();
      dataView.setItems(that.$oldValues);
      dataView.endUpdate();

      that.$oldValues = null;

      that.clearDirty();

      grid.invalidateAllRows();
      grid.render();

      e.preventDefault();
    }
  });

  scope.$on("on:new", function(e) {
    that.$oldValues = null;
    that.clearDirty();
    that.resetColumns();
  });

  scope.$on("on:edit", function(e, record) {
    if (record && record.id > 0) {
      that.$oldValues = null;
      that.clearDirty();
      that.resetColumns();
    }
  });

  scope.$on("on:advance-filter", function (e, criteria) {
    if (e.targetScope === handler) {
      that.advanceFilter = criteria;
    }
  });

  scope.$on("on:context-field-change", function (e, data) {
    that.contextField = data && data.field ? data.field.name : null;
    that.contextValue = data.value;
    that.resetColumns();
  });

  scope.$on("on:before-save", function(e) {

    // only for editable grid
    if (!that.editable) {
      return;
    }

    var row = null;
    var lock = grid.getEditorLock();
    if (lock.isActive()) {
      lock.commitCurrentEdit();
      row = grid.getDataItem(grid.getDataLength() - 1); // to check if adding new row
    }
    if (grid.getActiveCell() && that.focusInvalidCell(grid.getActiveCell())) {
      e.preventDefault();
      showErrorNotice();
      return false;
    }

    var beforeSavePending = that.__beforeSavePending || (row && row.id === 0);

    that.__beforeSavePending = false;

    function showErrorNotice() {

      var args = that.grid.getActiveCell() || {};
      var col = that.getColumn(args.cell);

      if (!col || !col.xpath) {
        return;
      }

      var name = col.name;
      if (that.handler.field &&
        that.handler.field.title) {
        name = that.handler.field.title + "[" + args.row +"] / " + name;
      }

      var items = "<ul><li>" + name + "</li></ul>";
      axelor.notify.error(items, {
        title: _t("The following fields are invalid:")
      });
    }

    var node = that.element.find('.slick-cell-required:empty,.slick-cell > .ng-invalid').first();
    if (node.parent().is('.slick-cell')) {
      node = node.parent();
    }
    if (node.length) {
      that.grid.setActiveNode(node[0]);
      that.grid.editActiveCell();
      e.preventDefault();
      showErrorNotice();
      return false;
    }

    if (!that.isDirty() || !beforeSavePending || that.saveChanges()) {
      return;
    }
    if (!that.editorScope || that.editorScope.isValid()) {
      return;
    }
    if (that.editorForm && that.editorForm.is(":hidden")) {
      return;
    }

    var args = that.grid.getActiveCell();
    if (args) {
      that.focusInvalidCell(args);
      showErrorNotice();
    } else {
      var item = that.editorScope.record;
      if (item && item.id === 0) {
        // new row was canceled
        return;
      }
      axelor.dialogs.error(_t('There are some invalid rows.'));
    }

    e.preventDefault();
    return false;
  });


  scope.$timeout(function () {
    that.zIndexFix();
    grid.invalidate();
    grid.autosizeColumns();
    // focus first filter input
    if (!axelor.device.mobile && !that.element.parent().is('.portlet-grid')) {
      that.element
        .find('.slick-headerrow:first input[type=text]:first')
        .focus()
        .select();
    }
  });

  var scrollTop;
  this.subscribe(grid.onScroll, function (e, args) {
    scrollTop = args.scrollTop;
  });
  function resetScroll() {
    if (scrollTop) {
      setTimeout(function () {
        that.element.children('.slick-viewport').scrollTop(scrollTop);
      });
    }
  }
  scope.$on('dom:attach', resetScroll);
  scope.$on('tab:select', resetScroll);

  var onColumnsResized = false;
  this.subscribe(grid.onColumnsResized, function (e, args) {
    onColumnsResized = true;
  });

  scope.$on('grid:adjust-columns', function () {
    if (!onColumnsResized) {
      grid.autosizeColumns();
    }
  });

  scope.$on('$destroy', function () {
    if (that.editorForm) {
      that.editorForm.remove();
    }
    if (that.editorScope) {
      that.editorScope.$destroy();
    }
    grid.destroy();
  });
};

Grid.prototype.subscribe = function(event, handler) {
  event.subscribe(_.bind(handler, this));
};

Grid.prototype.zIndexFix = function() {
  //XXX: ui-dialog issue (filter row)
  var zIndex = this.element.parents('.ui-dialog:first').zIndex();
  if (zIndex) {
    this.element.find('.slick-headerrow-column').zIndex(zIndex);
  }
};

Grid.prototype.adjustSize = function(force) {
  if (!this.grid || (!force && this.element.is(':hidden')) || this.grid.getEditorLock().isActive()) {
    return;
  }
  this.doInit();
  if (this.grid.getCanvasNode() && !this.grid.getCanvasNode().hasChildNodes()) {
    this.grid.invalidate();
  }
  this.adjustToScreen();
  this.grid.resizeCanvas();
  this.zIndexFix();
};

Grid.prototype.adjustToScreen = function() {
  var compact = this.__compact;
  var mobile = axelor.device.small;

  if (!compact && !mobile) {
    return;
  }
  if (mobile && compact) {
    return;
  }

  this.__compact = mobile;

  _.each(this.cols, function (col, i) {
    var field = col.descriptor || {};
    if (field.hidden) {
      return;
    }
  }, this);
};

Grid.prototype.getColumn = function(indexOrName) {
  var cols = this.grid.getColumns(),
    index = indexOrName;

  if (_.isString(index)) {
    index = this.grid.getColumnIndex(index);
  }
  return cols[index];
};

Grid.prototype.showColumn = function(name, show) {

  var that = this,
    grid = this.grid,
    cols = this.cols;

  this.visibleCols = this.visibleCols || _.pluck(this.getVisibleCols(), 'id');

  show = _.isUndefined(show) ? true : show;

  var visible = [],
    current = [];

  _.each(cols, function(col){
    if (col.id != name && _.contains(that.visibleCols, col.id))
      return visible.push(col.id);
    if (col.id == name && show)
      return visible.push(name);
  });

  this.visibleCols = visible;

  if (!this.$$initialized) {
    return;
  }

  current = _.filter(cols, function(col) {
    return _.contains(visible, col.id);
  });

  grid.setColumns(current);
  grid.getViewport().rightPx = 0;
  grid.resizeCanvas();
  grid.autosizeColumns();

  this.zIndexFix();
};

Grid.prototype.getVisibleCols = function(reset) {
  var visible = reset ? [] : (this.visibleCols || []);
  if (visible.length === 0) {
    var contextField = this.contextField;
    var contextValue = this.contextValue;
    return this.cols.filter(function (col) {
      var desc = col.descriptor||{};
      if (desc.contextField) {
        return !desc.hidden
          && desc.contextField === contextField
          && desc.contextFieldValue === contextValue;
      }
      return !desc.hidden;
    });
  }
  return this.cols.filter(function (col) {
    return visible.length ? _.contains(visible, col.id) : true;
  });
};

Grid.prototype.resetColumns = function() {
  var grid = this.grid,
    cols = this.getVisibleCols(true);

  this.visibleCols = _.pluck(cols, 'id');

  grid.setColumns(cols);
  grid.getViewport().rightPx = 0;
  grid.resizeCanvas();
  grid.autosizeColumns();
};

Grid.prototype.setColumnTitle = function(name, title) {
  if (this.$$initialized) {
    return this.grid.updateColumnHeader(name, title);
  }
  var col = this.getColumn(name);
  if (col && title) {
    col.name = title;
  }
};

Grid.prototype.getItemMetadata = function(row) {
  var item = this.grid.getDataItem(row);
  if (item && item.$style) {
    return {
      cssClasses: item.$style
    };
  }
  return null;
};

Grid.prototype.hilite = function (row, field) {
  var view = this.scope.view,
    record = this.grid.getDataItem(row),
    params = null;

  if (!view || !record || record.__group || record.__groupTotals) {
    return null;
  }

  if (!field) {
    _.each(this.scope.fields_view, function (item) {
      if (item.hilites) this.hilite(row, item);
    }, this);
  }

  var hilites = field ? field.hilites : view.hilites;
  if (!hilites || hilites.length === 0) {
    return null;
  }

  record.$style = null;

  var ctx = record || {};
  if (this.handler._context) {
    ctx = _.extend({}, this.handler._context, ctx);
  }

  for (var i = 0; i < hilites.length; i++) {
    params = hilites[i];
    var condition = params.condition,
      styles = null,
      pass = false;

    try {
      pass = axelor.$eval(this.scope, condition, ctx);
    } catch (e) {
    }
    if (!pass && field) {
      styles = record.$styles || (record.$styles = {});
      styles[field.name] = null;
    }
    if (!pass) {
      continue;
    }
    if (field) {
      styles = record.$styles || (record.$styles = {});
      styles[field.name] = params.css;
    } else {
      record.$style = params.css;
    }
    break;
  }
};

Grid.prototype.onBeforeMenuShow = function(event, args) {

  var menu = args.menu;
  if (!menu || !menu.items || !this.visibleCols) {
    return;
  }

  menu.items = _.filter(menu.items, function(item) {
    return item.command !== 'show';
  });

  _.each(this.cols, function(col) {
    if (_.contains(this.visibleCols, col.id)) return;
    menu.items.push({
      title: _t('Show') + " <i>" + col.name + "</i>",
      command: 'show',
      field: col.field
    });
  }, this);
};

Grid.prototype.onMenuCommand = function(event, args) {

  var grid = this.grid;

  if (args.command === 'sort-asc' ||
    args.command == 'sort-desc') {

    var opts = {
      grid: grid,
      multiColumnSort: true,
      sortCols: [{
          sortCol: args.column,
          sortAsc: args.command === 'sort-asc'
      }]
    };
    return grid.onSort.notify(opts, event, grid);
  }

  var groups, index;

  if (args.command === 'group-by') {
    groups = this._groups || [];
    index = groups.indexOf(args.column.field);
    if (index === -1) {
      groups.push(args.column.field);
    }
    return this.groupBy(groups);
  }

  if (args.command === 'ungroup') {
    groups = this._groups || [];
    index = groups.indexOf(args.column.field);
    if (index > -1) {
      groups.splice(index, 1);
    }
    return this.groupBy(groups);
  }

  if (args.command === 'hide') {
    return this.showColumn(args.column.field, false);
  }

  if (args.command === 'show') {
    return this.showColumn(args.item.field, true);
  }
};

Grid.prototype.onBeforeEditCell = function(event, args) {
  if (this.$oldValues === null) {
    this.$oldValues = [];
    var n = 0;
    while (n < this.grid.getDataLength()) {
      var item = this.grid.getDataItem(n++);
      if (item && item.id) {
        this.$oldValues.push(_.clone(item));
      }
    }
  }
  if (args.item && args.item._original === undefined) {
    args.item._original = _.clone(args.item);
  }
};

Grid.prototype.onKeyDown = function(e, args) {
  var that = this,
    grid = this.grid,
    lock = grid.getEditorLock();

  if (e.which === $.ui.keyCode.ENTER && $(e.target).is('textarea,[contenteditable]')) {
    return;
  }

  if (e.isDefaultPrevented()){
    e.stopImmediatePropagation();
    return false;
  }

  if (!e.isBlocked && (blocked() || !lock.isActive())) {
    return false;
  }

  function blockCallback(blocked) {
    if (blocked && e.which === $.ui.keyCode.TAB) {
      setTimeout(function(){
        var cell = e.shiftKey ? that.findPrevEditable(args.row, args.cell)
                    : that.findNextEditable(args.row, args.cell);
        if (cell) {
          grid.setActiveCell(cell.row, cell.cell);
          grid.editActiveCell();
        }
      });
    }
  }

  function blocked() {
    if (that.isDirty() && axelor.blockUI(blockCallback)) {
      grid.focus();
      e.stopImmediatePropagation();
      return true;
    }
    return false;
  }

  function commitChanges() {
    if (lock.commitCurrentEdit() && !blocked()) {
      return true;
    }
    return false;
  }

  function focusCell(row, cell) {
    grid.setActiveCell(row, cell);
    // make sure cell has focus RM-3938
    setTimeout(function () {
      grid.editActiveCell();
    });
  }

  // firefox & IE fails to trigger onChange
  if ((axelor.browser.mozilla || axelor.browser.msie) &&
      (e.which === $.ui.keyCode.TAB || e.which === $.ui.keyCode.ENTER)) {
    var editor = grid.getCellEditor(),
      target = $(e.target);
    if (editor.isValueChanged()) {
      target.change();
    }
    setTimeout(function(){
      target.blur();
    });
  }
  var handled = false;
  if (e.which === $.ui.keyCode.TAB) {
    var cell = e.shiftKey ? this.findPrevEditable(args.row, args.cell) :
                this.findNextEditable(args.row, args.cell);

    if (commitChanges() && cell && cell.row > args.row && this.isDirty()) {
      args.item = null;
      this.scope.waitForActions(function () {
        that.scope.waitForActions(function () {
          that.addNewRow(args);
        });
      });
    } else if (cell) {
      focusCell(cell.row, cell.cell);
    }

    handled = true;
  }

  if (e.which === $.ui.keyCode.ENTER) {
    if (e.ctrlKey) {
      if (!this.saveChanges(args)) {
        this.focusInvalidCell(args);
      }
    } else {
      if (!lock.commitCurrentEdit()) {
        this.focusInvalidCell(args);
      }
      grid.focus();
    }
    grid.focus();
    handled = true;
  }

  if (e.which === $.ui.keyCode.ESCAPE) {
    grid.focus();
  }

  if (handled) {
    e.stopImmediatePropagation();
    return false;
  }
};

Grid.prototype.isCellEditable = function(row, cell) {
  var cols = this.grid.getColumns(),
    col = cols[cell];
  if (!col || col.id === "_edit_column" || col.id === "_move_column" || col.id === "_checkbox_selector") {
    return false;
  }
  var field = col.descriptor || {};
  var form = this.editorForm;

  if (field.type === 'button' || (field.name && field.name.indexOf('.') > -1)) {
    return false;
  }
  if (!form) {
    return !field.readonly;
  }

  var current = this.grid.getActiveCell();
  if (current && current.row === row) {
    var item = this.element.find('[x-field=' + field.name + ']:first');
    if (item.length) {
      return !item.scope().isReadonly();
    }
  }

  return !field.readonly;
};

Grid.prototype.findNextEditable = function(posY, posX) {
  var grid = this.grid,
    cols = grid.getColumns(),
    args = {row: posY, cell: posX + 1};
  while (args.cell < cols.length) {
    if (this.isCellEditable(args.row, args.cell)) {
      return args;
    }
    args.cell += 1;
  }
  if (grid.getDataItem(args.row)) {
    args.row += 1;
  }
  args.cell = 0;
  while (args.cell <= posX) {
    if (this.isCellEditable(args.row, args.cell)) {
      return args;
    }
    args.cell += 1;
  }
  return null;
};

Grid.prototype.findPrevEditable = function(posY, posX) {
  var grid = this.grid,
    cols = grid.getColumns(),
    args = {row: posY, cell: posX - 1};
  while (args.cell > -1) {
    if (this.isCellEditable(args.row, args.cell)) {
      return args;
    }
    args.cell -= 1;
  }
  if (args.row > 0) {
    args.row -= 1;
  }
  args.cell = cols.length - 1;
  while (args.cell >= posX) {
    if (this.isCellEditable(args.row, args.cell)) {
      return args;
    }
    args.cell -= 1;
  }
  return null;
};

Grid.prototype.saveChanges = function(args, callback) {

  // onBeforeSave may cause recursion
  if (this._saveChangesRunning) {
    return;
  }

  var that = this;
  var grid = this.grid;
  var lock = grid.getEditorLock();
  var force = arguments[2];

  if (!force &&
      ((lock.isActive() && !lock.commitCurrentEdit()) ||
       (this.editorScope && !this.editorScope.isValid()))) {
    return false;
  }

  this._saveChangesRunning = true;
  if (this.editorScope) {
    this.editorScope.$emit("on:before-save", this.editorScope.record);
  }
  var params = arguments;
  this.scope.waitForActions(function () {
    that.__saveChanges.apply(that, params);
    that._saveChangesRunning = false;
  }, 100);

  return true;
};

Grid.prototype.__saveChanges = function(args, callback) {

  var that = this;
  var grid = this.grid;

  if (!args) {
    args = _.extend({ row: 0, cell: 0 }, grid.getActiveCell());
    args.item = grid.getDataItem(args.row);
  }

  var ds = this.handler._dataSource;
  var data = this.scope.dataView;
  var records = [];

  records = _.map(data.getItems(), function(rec) {
    var res = {};
    for(var key in rec) {
      var val = rec[key];
      if (_.isString(val) && val.trim() === "")
        val = null;
      res[key] = val;
    }
    if (res.id === 0) {
      res.id = null;
    }
    if (res.$dirty && _.isUndefined(res.version)) {
      res.version = res.$version;
    }
    return res;
  });

  function focus() {
    grid.setActiveCell(args.row, args.cell);
    grid.focus();
    if (callback) {
      that.handler.waitForActions(callback);
    }
  }

  var onBeforeSave = this.scope.onBeforeSave(),
    onAfterSave = this.scope.onAfterSave();

  if (onBeforeSave && onBeforeSave(records) === false) {
    return setTimeout(focus, 200);
  }

  // prevent cache
  var saveDS = ds;
  var handler = this.handler || {};
  if (handler.field && handler.field.target) {
    saveDS = ds._new(ds._model, {
      domain: ds._domain,
      context: ds._context
    });
  }

  records = _.where(records, { $dirty: true });
  if (records.length === 0) {
    return setTimeout(focus, 200);
  }

  var fields = handler.selectFields ? handler.selectFields() : undefined;

  return saveDS.saveAll(records, fields).success(function(records, page) {
    if (data.getItemById(0)) {
      data.deleteItem(0);
    }
    if (onAfterSave) {
      onAfterSave(records, page);
    }
    setTimeout(focus);
  });
};

Grid.prototype.canSave = function() {
  return this.editorScope && this.editorScope.isValid() && this.isDirty();
};

Grid.prototype.isDirty = function(row) {
  var grid = this.grid;
  var item;

  if (row === null || row === undefined) {
    var n = 0;
    while (n < grid.getDataLength()) {
      item = grid.getDataItem(n);
      if (item && item.$dirty) {
        return true;
      }
      n ++;
    }
  } else {
    item = grid.getDataItem(row);
    if (item && item.$dirty) {
      return true;
    }
  }
  return false;
};

Grid.prototype.__markHandlerDirty = function () {
  if (this.scope.handler && this.scope.handler.$dirtyGrid) {
    this.$gid = this.$gid || _.uniqueId('grid');
    this.scope.handler.$dirtyGrid(this.$gid, this.isDirty());
  }
};

Grid.prototype.markDirty = function(row, field) {

  var grid = this.grid,
    dataView = this.scope.dataView,
    hash = grid.getCellCssStyles("highlight") || {},
    items = hash[row] || {};

  items[field] = "dirty";
  hash[row] = items;

  var record = dataView.getItem(row);
  if (this.handler.$$ensureIds && record && !record.id) {
    this.handler.$$ensureIds([record]);
  }

  grid.setCellCssStyles("highlight", hash);
  grid.invalidateAllRows();
  grid.render();

  this.__markHandlerDirty();
  this.__beforeSavePending = true;
};

Grid.prototype.clearDirty = function(row) {
  var grid = this.grid,
    hash = grid.getCellCssStyles("highlight") || {};

  if (row === null || row === undefined) {
    hash = {};
  } else {
    delete hash[row];
  }

  grid.setCellCssStyles("highlight", hash);
  grid.invalidateAllRows();
  grid.render();

  this.__markHandlerDirty();
  this.__beforeSavePending = false;
};

Grid.prototype.focusInvalidCell = function(args) {
  var grid = this.grid,
    formCtrl = this.editorForm.children('form').data('$formController'),
    error = formCtrl.$error || {};

  if (this.editorForm.is(':hidden') && _.isEmpty(this.editorScope.record)) {
    return false;
  }

  for(var name in error) {
    var errors = error[name] || [];
    if (errors.length) {
      name = errors[0].$name;
      var cell = grid.getColumnIndex(name);
      if (cell > -1) {
        grid.setActiveCell(args.row, cell);
        grid.editActiveCell();
        return true;
      }
    }
  }

  return false;
};

Grid.prototype.addNewRow = function (args) {
  var self = this,
    scope = this.scope,
    grid = this.grid,
    dataView = scope.dataView,
    lock = grid.getEditorLock();

  if (lock.isActive()) {
    lock.commitCurrentEdit();
  }

  args.row = Math.max(0, args.row);
  args.cell = Math.max(0, args.cell);

  var cell = self.findNextEditable(args.row, 0);

  function addRow(defaults) {
    var args = { row: grid.getDataLength(), cell: 0 };
    var item = _.extend({ id: 0 }, defaults);

    grid.invalidateRow(dataView.length);
    dataView.addItem(item);

    self.scope.waitForActions(function () {
      cell = self.findNextEditable(args.row, args.cell);
      if (cell) {
        grid.focus();
        grid.setActiveCell(cell.row, cell.cell);
        setTimeout(function () {
          grid.editActiveCell();
        }, 200);
      }
    }, 100);
  }

  function focus() {
    grid.focus();
    grid.setActiveCell(cell.row, cell.cell);

    if (grid.getDataLength() > cell.row) {
      return grid.editActiveCell();
    }
    if (!self.canAdd()) {
      return;
    }

    self.editorScope.doOnNew();
    self.scope.waitForActions(function () {
      self.scope.waitForActions(function () {
        addRow(self.editorScope.record);
      });
    }, 100);
  }

  if (args.item || grid.getDataLength() === 0) {
    return focus();
  }
  var saved = self.saveChanges(args, function () {
    cell.row += 1;
    focus();
  });
  if (!saved) {
    self.focusInvalidCell(args);
  }
};

Grid.prototype.canEdit = function () {
  var handler = this.handler || {};
  if (!this.editable) return false;
  if (handler.canEdit && !handler.canEdit()) return false;
  if (handler.isReadonly && handler.isReadonly()) return false;
  return true;
};

Grid.prototype.canAdd = function () {
  var handler = this.handler || {};
  if (!this.editable) return false;
  if (handler.isReadonly && handler.isReadonly()) return false;
  return handler.canNew && handler.canNew();
};

Grid.prototype.setEditors = function(form, formScope, forEdit) {
  var grid = this.grid,
    data = this.scope.dataView,
    element = this.element;

  this.editable = forEdit = forEdit === undefined ? true : forEdit;

  grid.setOptions({
    editable: !axelor.device.mobile,
    asyncEditorLoading: false,
    editorLock: new Slick.EditorLock()
  });

  form.prependTo(element).hide();
  formScope.onChangeNotify = function(scope, values) {
    var item, editor, cell = grid.getActiveCell();
    if (!cell || formScope.record !== scope.record) {
      return;
    }
    item = grid.getDataItem(cell.row);
    if (item) {
      editor = grid.getCellEditor();
      if (grid.getEditorLock().isActive()) {
        grid.getEditorLock().commitCurrentEdit();
      }
      item = _.extend(item, values);

      // update dotted fields
      _.filter(grid.getColumns(), function (col) {
        return col.field && col.field.indexOf('.') > -1;
      }).forEach(function (col) {
        var path = col.field.split('.');
        var val = item || {};
        var idx = 0;
        while (val && idx < path.length) {
          val = val[path[idx++]];
        }
        if (idx === path.length && val !== undefined) {
          item[col.field] = val;
        }
      });

      grid.updateRowCount();
      grid.invalidateRow(cell.row);
      grid.render();

      grid.setActiveCell(cell.row, cell.cell);

      if (editor) {
        grid.focus();
      }
    }
  };

  formScope.onNewHandler = function (event) {

  };

  formScope.doOnNew = function () {

    if (formScope.defaultValues === null) {
      formScope.defaultValues = {};
      _.each(formScope.fields, function (field, name) {
        if (field.defaultValue !== undefined) {
          formScope.defaultValues[name] = field.defaultValue;
        }
      });
    }

    var values = angular.copy(formScope.defaultValues);
    var args = grid.getActiveCell();

    formScope.editRecord(values);
    formScope.$applyAsync();

    if (!formScope.$events.onNew) {
      return;
    }

    var handler = formScope.$events.onNew;
    var lock = grid.getEditorLock();
    if (lock.isActive()) {
      lock.commitCurrentEdit();
    }

    var promise = handler();
    promise.then(function () {
      grid.focus();
      grid.editActiveCell();
    });
  };

  // delegate isDirty to the dataView
  data.canSave = _.bind(this.canSave, this);
  data.saveChanges = _.bind(this.saveChanges, this);

  var that = this;
  var onNew = this.handler.onNew;
  if (onNew) {
    this.handler.onNew = function () {
      var lock = that.grid.getEditorLock();

      if (that.editable) {
        if (lock.isActive()) {
          lock.commitCurrentEdit();
        }

        var cell = that.findNextEditable(that.grid.getDataLength() - 1, 0) || { row: that.grid.getDataLength(), cell: 0 };
        that.grid.focus();
        that.grid.setActiveCell(cell.row, cell.cell);
        that.grid.editActiveCell();
        return that.scope.$timeout(function () {
          return that.addNewRow(cell);
        });
      }
      return onNew.apply(that.handler, arguments);
    };
  }

  if (!forEdit) {
    formScope.setEditable(false);
  }

  this.editorForm = form;
  this.editorScope = formScope;
  this.editorForEdit = forEdit;
};

Grid.prototype.onSelectionChanged = function(event, args) {
  var grid = this.grid;
  var activeCell = grid.getActiveCell();
  var selectedRows = args.rows || [];

  if (activeCell && selectedRows.indexOf(activeCell.row) === -1) {
    grid.resetActiveCell();
  }

  if (this.handler.onSelectionChanged) {
    this.handler.onSelectionChanged(event, args);
  }
  this.element.find(' > .slick-viewport > .grid-canvas > .slick-row')
    .removeClass('selected')
    .find(' > .slick-cell.selected')
    .parent()
    .each(function () {
      $(this).addClass('selected');
    });
};

Grid.prototype.onCellChange = function(event, args) {
  var grid = this.grid,
    cols = grid.getColumns(),
    name = cols[args.cell].field;

  var es = this.editorScope;
  if (es.record && es.record.version === undefined) {
    es.record.version = es.record.$version;
  }
  if (es.isDirty()) {
    this.markDirty(args.row, name);
  }
};

Grid.prototype.onSort = function(event, args) {
  if (this.canSave())
    return;
  if (this.handler.onSort)
    this.handler.onSort(event, args);
};

Grid.prototype.onBeforeMoveRows = function (event, args) {
  var data = this.scope.dataView;
  for (var i = 0; i < args.rows.length; i++) {
    // no point in moving before or after itself
    if (args.rows[i] == args.insertBefore || args.rows[i] == args.insertBefore - 1) {
      event.stopPropagation();
      return false;
    }
  }
  return true;
};

Grid.prototype._resequence = function (items) {
  var min = _.min(_.map(items, function (item) {
      return item.sequence || 0;
    }));
    for (var i = 0; i < items.length; i++) {
      var last = items[i].sequence;
      var next = min++;
      if (items[i].sequence !== next) {
          items[i].sequence = next;
        items[i].$dirty = true;
      }
  }
    return items;
};

Grid.prototype.onMoveRows = function (event, args) {
  var grid = this.grid;
  var dataView = this.scope.dataView;
    var rows = args.rows;
    var items = dataView.getItems();
    var insertBefore = args.insertBefore;

    var left = items.slice(0, insertBefore);
    var right = items.slice(insertBefore, items.length);
    var extractedRows = [];

    rows.sort(function(a, b) { return a - b; });

    var i;

    for (i = 0; i < rows.length; i++) {
      extractedRows.push(items[rows[i]]);
    }

    rows.reverse();

    for (i = 0; i < rows.length; i++) {
      var row = rows[i];
      if (row < insertBefore) {
        left.splice(row, 1);
      } else {
        right.splice(row - insertBefore, 1);
      }
    }

    items = left.concat(extractedRows.concat(right));

    var selectedRows = [];
    for (i = 0; i < rows.length; i++) {
      selectedRows.push(left.length + i);
  }

    // resequence
    this._resequence(items);

    function resetSelection() {
      grid.setActiveCell(selectedRows[0], 0);
      grid.setSelectedRows(selectedRows);
    }

    dataView.beginUpdate();
  dataView.setItems(items);
  dataView.endUpdate();
  resetSelection();
    grid.render();

    var that = this;
    this.scope.$timeout(function () {
      dataView.$isResequencing = true;
      var saved = that.saveChanges(null, function() {
        delete dataView.$isResequencing;
        resetSelection();
      }, true);
      if (saved === false) {
        delete dataView.$isResequencing;
      }
    });
};

Grid.prototype.onButtonClick = function(event, args) {

  if ($(event.srcElement || event.target).is('.readonly') || this._buttonClickRunning) {
    event.stopImmediatePropagation();
    return false;
  }

  var grid = this.grid;
  var data = this.scope.dataView;
  var cols = this.getColumn(args.cell);
  var field = (cols || {}).descriptor || {};

  // set selection
  grid.setSelectedRows([args.row]);
  grid.setActiveCell(args.row, args.cell);

  if (field.handler) {
    this._buttonClickRunning = true;

    var handlerScope = this.scope.handler;
    var model = handlerScope._model;
    var record = data.getItem(args.row) || {};
    var that = this;

    // defer record access so that any pending changes are applied
    Object.defineProperty(field.handler.scope, 'record', {
      enumerable: true,
      configurable: true,
      get: function () {
        return data.getItem(args.row) || {};
      }
    });

    if(field.prompt) {
      field.handler.prompt = field.prompt;
    }
    field.handler.scope.getContext = function() {
      var context = _.extend({
        _model: model
      }, record);
      if (handlerScope.field && handlerScope.field.target) {
        context._parent = handlerScope.getContext();
      }
      if (context.id === 0) {
        context.id = null;
      }
      return context;
    };
    field.handler.onClick().then(function(res){
      delete that._buttonClickRunning;
      delete field.handler.scope.record;
      grid.invalidateRows([args.row]);
      grid.render();
    }, function () {
      delete that._buttonClickRunning;
    });
  }
};

Grid.prototype.onItemClick = function(event, args) {

  var that = this;
  var waitCallback = function (done) {
    setTimeout(function () {
      that.handler.waitForActions(function () {
        that.__onItemClick(event, args);
        if (done) {
          done();
        }
      }, 100)
    }, 100);
  };

  var lock = this.grid.getEditorLock();
  if (lock.isActive()) {
    lock.commitCurrentEdit();
    if (this.editorScope &&
      this.editorScope.$lastEditor &&
      this.editorScope.$lastEditor.shouldWait()) {
      return waitCallback;
    }
  }

  // prevent edit if some action is still in progress
  if (this.isDirty() && axelor.blockUI()) {
    return waitCallback;
  }

  return this.__onItemClick(event, args);
}

Grid.prototype.__onItemClick = function(event, args) {

  // prevent edit if some action is still in progress
  if (this.isDirty() && axelor.blockUI()) {
    return;
  }

  var source = $(event.target);
  if (source.is('img.slick-img-button,i.slick-icon-button')) {
    return this.onButtonClick(event, args);
  }

  // checkbox column
  if (this.scope.selector && args.cell === 0) {
    return false;
  }

  //XXX: hack to show popup grid (selector and editable in conflict?)
  if (this.scope.selector && this.editable) {
    var col = this.grid.getColumns()[args.cell] || {},
      field = col.descriptor || {};
    if (col.forEdit !== false &&
        (field.type === 'one-to-many' || field.type === 'many-to-many' || this.scope.selector === 'checkbox')) {
      this.grid.setActiveCell(args.row, args.cell);
      this.grid.editActiveCell();
      event.preventDefault();
      event.stopImmediatePropagation();
      return false;
    }
  }

  if (!this.scope.selector && this.canEdit()) {
    return this.grid.setActiveCell();
  }
  if (this.handler.onItemClick) {
    this.handler.onItemClick(event, args);
  }
};

Grid.prototype.onItemDblClick = function(event, args) {

  if ($(event.srcElement || event.target).is('img.slick-img-button,i.slick-icon-button')) {
    return this.onButtonClick(event, args);
  }

  var col = this.grid.getColumns()[args.cell];
  if (col.id === '_edit_column') return;
  var item = this.grid.getDataItem(args.row) || {};
  if (item.__group || item.__groupTotals) {
    return;
  }
  if (!this.handler.field && this.canSave())
    return;

  var selected = this.grid.getSelectedRows() || [];
  if (selected.length === 0) {
    this.grid.setSelectedRows([args.row]);
  }

  if (this.handler.onItemDblClick)
    this.handler.onItemDblClick(event, args);
  event.stopImmediatePropagation();
};

Grid.prototype.onRowCountChanged = function(event, args) {
  this.grid.updateRowCount();
  this.grid.render();
};

Grid.prototype.onRowsChanged = function(event, args) {
  var grid = this.grid,
    data = this.scope.dataView,
    forEdit = this.editorForEdit;

  if (!this.isDirty()) {
    this.clearDirty();
  }
  grid.invalidateRows(args.rows);
  grid.render();
};

Grid.prototype.groupBy = function(names) {

  var grid = this.grid,
    data = this.scope.dataView,
    cols = this.grid.getColumns();

  var aggregators = _.map(cols, function(col) {
    var field = col.descriptor;
    if (!field) return null;
    if (field.aggregate === "sum") {
      return new Slick.Data.Aggregators.Sum(field.name);
    }
    if (field.aggregate === "avg") {
      return new Slick.Data.Aggregators.Avg(field.name);
    }
    if (field.aggregate === "min") {
      return new Slick.Data.Aggregators.Min(field.name);
    }
    if (field.aggregate === "max") {
      return new Slick.Data.Aggregators.Max(field.name);
    }
  });

  aggregators = _.compact(aggregators);

  var all = names;

  if (_.isString(all)) {
    all = all.split(/\s*,\s*/);
  }

  var fields = _.compact(_.pluck(this.cols, 'descriptor'));

  var grouping = _.map(all, function(name) {
    var that = this;
    var field = _.findWhere(fields, { name: name });
    return {
      getter: function(item) {
        var value = item[name];
        var formatter = Formatters[field.selection ? 'selection' : field.type];
        if (field.jsonPath && field.jsonField) {
          var jsonValue = item[field.jsonField];
          if (jsonValue) {
            jsonValue = angular.fromJson(jsonValue);
            value = jsonValue[field.jsonPath];
          }
        }
        return (formatter ? formatter(field, value, item, grid) : value) || _t('N/A');
      },
      formatter: function(g) {
        var title = field.title + ": " + g.value;
        return '<span class="slick-group-text">' + title + '</span>' + ' ' +
             '<span class="slick-group-count">' + _t("({0} items)", g.count) + '</span>';
      },
      aggregators: aggregators,
      aggregateCollapsed: false
    };
  }, this);

  this._groups = all;
  data.setGrouping(grouping);
};

function EditIconColumn(options) {

  var _grid;
    var _self = this;
    var _handler = new Slick.EventHandler();

    var _opts = _.extend({
      onClick: angular.noop
    }, options);

    function init(grid) {
      _grid = grid;
      _handler.subscribe(grid.onClick, handleClick);
    }

    function handleClick(e, args) {
      if (_grid.getColumns()[args.cell].id !==  '_edit_column' || !$(e.target).is("i")) {
        return;
      }
      return _opts.onClick(e, args);
    }

    function destroy() {
      _handler.unsubscribeAll();
    }

    function editFormatter(row, cell, value, columnDef, dataContext) {
  if (!dataContext || !dataContext.id) return '<i class="fa fa-minus"></i>';
      return '<i class="fa fa-pencil"></i>';
    }

    function getColumnDefinition() {
    return {
      id : '_edit_column',
      name : "<span class='slick-column-name'>&nbsp;</span>",
      field : "edit",
      width : 24,
      resizable : false,
      sortable : false,
      cssClass: 'edit-icon',
      formatter : editFormatter
    };
  }

    $.extend(this, {
    "init": init,
    "destroy": destroy,

    "getColumnDefinition": getColumnDefinition
  });
}

ui.directive('uiSlickEditors', function() {

  return {
    restrict: 'EA',
    replace: true,
    controller: ['$scope', '$element', 'DataSource', 'ViewService', function($scope, $element, DataSource, ViewService) {
      ui.ViewCtrl($scope, DataSource, ViewService);
      ui.FormViewCtrl.call(this, $scope, $element);
      $scope.setEditable();
      $scope.onShow = function(viewPromise) {

      };

      var _getContext = $scope.getContext;
      $scope.getContext = function() {
        var context = _getContext();
        var handler = $scope.handler || {};
        if (context && handler.field && handler.field.target) {
          context._parent = handler.getContext();
        }
        if (context.id === 0) {
          context.id = null;
        }
        return context;
      };

      $scope.show();
    }],
    link: function(scope, element, attrs) {

      var grid = null;
      scope.canWatch = function () {
        return true;
      };
    },
    template: '<div ui-view-form x-handler="true" ui-watch-if="canWatch()"></div>'
  };
});

ui.directive('uiSlickGrid', ['ViewService', 'ActionService', function(ViewService, ActionService) {

  var types = {
    'one-to-many' : 'one-to-many-inline',
    'many-to-many' : 'many-to-many-inline'
  };

  function makeForm(scope, model, items, fields, forEdit, onNew) {

    var _fields = fields || {},
      _items = [];

    _.each(items, function(item) {
      var field = _fields[item.name] || item,
        type = types[field.type];

      // force lite html widget
      if (item.widget && item.widget.toLowerCase() === 'html') {
        item.lite = true;
      }

      if (!type && !forEdit) {
        item.forEdit = false;
        return;
      }

      var params = _.extend({}, item, { showTitle: false });
      if (type) {
        params.widget = type;
        params.canEdit = forEdit;
      }

      _items.push(params);
    });

    var schema = {
      cols: _items.length,
      colWidths: '=',
      viewType : 'form',
      onNew: onNew,
      items: _items
    };

    scope._viewParams = {
      model: model,
      views: [schema]
    };

    return ViewService.compile('<div ui-slick-editors></div>')(scope);
  }

  return {
    restrict: 'EA',
    replace: false,
    scope: {
      'view'		: '=',
      'dataView'	: '=',
      'handler'	: '=',
      'selector'	: '@',
      'editable'	: '@',
      'noFilter'	: '@',
      'onInit'	: '&',
      'onBeforeSave'	: '&',
      'onAfterSave'	: '&'
    },
    link: function(scope, element, attrs) {

      var grid = null,
        schema = null,
        handler = scope.handler,
        initialized = false;

      function doInit() {
        if (initialized || !schema || !scope.dataView) return;
        initialized = true;

        if (attrs.editable === "false") {
          schema.editable = false;
        }
        scope.selector = attrs.selector;
        scope.noFilter = attrs.noFilter;

        if (axelor.config["view.grid.selection"] === "checkbox" && !scope.selector) {
          scope.selector = "checkbox";
        }

        var forEdit = schema.editable || false,
          canEdit = schema.editable || false,
          hasMulti = false;

        hasMulti = _.find(schema.items, function(item) {
          var field = handler.fields[item.name] || {};
          return _.str.endsWith(field.type, '-many');
        });

        if (hasMulti) {
          canEdit = true;
        }

        var form = null,
          formScope = null;

        if (handler.field && handler.field.onNew) {
          schema.onNew = handler.field.onNew;
        }

        if (canEdit) {
          formScope = scope.$new();
          form = makeForm(formScope, handler._model, schema.items, handler.fields, forEdit, schema.onNew);
        }

        if (forEdit) {
          element.addClass('slickgrid-editable');
        }

        grid = new Grid(scope, element, attrs, ViewService, ActionService);
        if (form) {
          formScope.grid = grid;
          grid.setEditors(form, formScope, forEdit);
        }

        if (!handler._isPopup && schema.inlineHelp && !axelor.config["user.noHelp"]) {
          addHelp(schema.inlineHelp);
        }

        // handle pending attrs change on dashlets
        if (handler.$$pendingAttrs) {
          _.each(handler.$$pendingAttrs, function (itemAttrs, itemName) {
            _.each(itemAttrs, function (attrValue, attrName) {
              switch (attrName) {
              case 'hidden':
                grid.showColumn(itemName, !attrValue);
                break;
              case 'title':
                grid.setColumnTitle(itemName, attrValue);
                break;
              }
            });
          });
          handler.$$pendingAttrs = undefined;
        }

        grid.adjustSize();
      }

      function addHelp(helpItem) {

        var helpElem = $('<div>')
          .css('visibility', 'hidden')
          .addClass('help-item alert')
          .html(helpItem.text);

        var css = helpItem.css || '';
        if (css.indexOf('alert-') === -1) {
          css = (css + ' alert-info').trim();
        }

        helpElem.addClass(css);
        helpElem.appendTo('body');

        element.css('top', helpElem.height() + 36 + 40);

        setTimeout(function () {
          element.before(helpElem.css('visibility', ''));
        });
      }

      element.addClass('slickgrid').hide();
      var unwatch = scope.$watch("view.loaded", function gridSchemaWatch(viewLoaded) {
        if (!viewLoaded || !scope.dataView) {
          return;
        }
        unwatch();
        schema = scope.view;

        var field = handler.field || {};
        if (field.canMove !== undefined) {
          schema.canMove = field.canMove;
        }
        if (field.editable !== undefined) {
          schema.editable = field.editable;
        }
        schema.rowHeight = field.rowHeight || schema.rowHeight;
        schema.orderBy = field.orderBy || schema.orderBy;
        schema.groupBy = field.groupBy || schema.groupBy;
        schema.groupBy = (schema.editable || schema.groupBy === "false") ? false : schema.groupBy;
        schema.canMassUpdate = !!_.find(schema.items, function (item) { return item.massUpdate; });

        element.show();
        doInit();
      });
    }
  };
}]);

})();
