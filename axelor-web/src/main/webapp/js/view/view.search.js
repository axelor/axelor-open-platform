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

ui.controller('SearchViewCtrl', SearchViewCtrl);

SearchViewCtrl.$inject = ['$scope', '$element', '$http', 'DataSource', 'ViewService', 'MenuService'];
function SearchViewCtrl($scope, $element, $http, DataSource, ViewService, MenuService) {

  var view = $scope._views.search || {};

  $scope._dataSource = DataSource.create('multi-search');

  $scope.$applyAsync(function(){
    if (view.deferred)
      view.deferred.resolve($scope);
  });

  function fixFields(fields) {
    _.each(fields, function(field){
      if (field.type == 'reference') {
        field.type = 'MANY_TO_ONE';
        field.canNew = false;
        field.canEdit = false;
      }

      if (field.type)
        field.type = field.type.toUpperCase();
      else
        field.type = 'STRING';
    });
    return fields;
  }

  $scope.show = function(viewPromise) {
    if (!viewPromise) {
      viewPromise = $scope.loadView('search', view.name);
      viewPromise.success(function(fields, schema){
        $scope.initView(schema);
      });
    }

    $scope.onShow(viewPromise);
  };

  $scope.onShow = function() {

  };

  $scope.initView = function(schema) {

    var params = $scope._viewParams;

    $scope._searchFields = fixFields(schema.searchFields);
    $scope._resultFields = fixFields(schema.resultFields);

    $scope._searchView = schema;
    $scope._showSingle = params.params && params.params.showSingle;
    $scope._forceEdit = params.params && params.params.forceEdit;
    $scope._hideActions = params.params && params.params.hideActions;

    $scope.updateRoute();

    if (params.options && params.options.mode == "search") {
      $scope.setRouteOptions(params.options);
    }

  };

  $scope.getRouteOptions = function() {
    var args = [],
      query = $scope._routeSearch;

    return {
      mode: 'search',
      args: args,
      query: query
    };
  };

  $scope._routeSearch = null;
  var onNewCalled = false;
  $scope.setRouteOptions = function(options) {
    var opts = options || {},
      fields = $scope._searchFields || [],
      search = opts.search,
      record = {};

    var changed = !angular.equals($scope._routeSearch, search);

    $scope._routeSearch = search;

    if (!onNewCalled && _.isEmpty(search)) {
      onNewCalled = true;
      scopes.form.$broadcast('on:new');
    }
    if (!search || _.isEmpty(search) || !changed) {
      return $scope.updateRoute();
    }

    _.each(fields, function(field) {
      var value = search[field.name];
      if (value === undefined) {
        return;
      }
      if (field.target) {
        if (value) {
          record[field.name] = {id: +value};
        }
      } else {
        record[field.name] = value;
      }
    });

    if (search.objects) {
      scopes.toolbar.editRecord({
        objectSelect: search.objects
      });
    }

    scopes.form.editSearch(record, fields);

    function _doSearch() {
      var promise = $scope.doSearch();
      if (promise && promise.then && $scope._showSingle) {
        promise.then(function () {
          var items = scopes.grid.getItems();
          if (items && items.length === 1) {
            scopes.grid.selection = [0];
            scopes.grid.onEdit();
          }
        });
      }
    }

    var promise = scopes.toolbar._viewPromise;
    if (promise && promise.then) {
      promise.then(function() {
        $scope.$timeout(_doSearch);
      });
    } else {
      _doSearch();
    }

  };

  var scopes = {};
  $scope._register = function(key, scope) {
    scopes[key] = scope;
  };

  $scope.doSearch = function() {
    var params = _.extend({}, scopes.form.record),
      empty =  _.chain(params).values().compact().isEmpty().value();
    if (empty)
      return $scope.doClear();

    var selected = (scopes.toolbar.record || {}).objectSelect;

    _.extend(params,{
      __name: view.name,
      __selected: _.isEmpty(selected) ? null : selected.split(/,\s*/)
    });

    var promise = $http.post('ws/search', {
      limit: view.limit || 80,
      data: params
    });

    return promise.then(function(response){
      var res = response.data,
        records = res.data || [];

      // slickgrid expects unique `id` so generate them and store original one
      _.each(records, function(rec, i){
        rec._id = rec.id;
        rec.id = i + 1;
      });

      scopes.grid.setItems(records);

      if (scopes.form.$events.onLoad) {
        scopes.form.record._count = records.length;
        scopes.form.record._countByModels = _.countBy(records, function(rec) {
          return rec._model;
        });
        scopes.form.$events.onLoad();
      }

      if (_.isEmpty(records)) {
        axelor.notify.info(_t("No records found."));
      }
    });

  };

  $scope.doClear = function(all) {
    scopes.form.edit(null);
    scopes.form.$broadcast('on:new');
    scopes.grid.setItems([]);
    if (all) {
      scopes.toolbar.edit(null);
      scopes.toolbar.doReset();
    }
  };

  $scope.doAction = function() {

    var action = scopes.toolbar.getMenuAction();
    if (!action) {
      return;
    }

    var grid = scopes.grid,
      index = _.first(grid.selection),
      record = grid.getItem(index);

    action = action.action;
    record = _.extend({
      _action: action
    }, record);

    record.id = record._id;

    MenuService.action(action).success(function(result){

      if (!result.data) {
        return;
      }

      var view = result.data[0].view;
      var tab = view;

      tab.action = _.uniqueId('$act');
      tab.viewType = 'form';

      tab.context = _.extend({}, tab.context, {
        _ref : record
      });

      $scope.openTab(tab);
    });
  };
}

ui.controller('SearchFormCtrl', SearchFormCtrl);

SearchFormCtrl.$inject = ['$scope', '$element', 'ViewService'];
function SearchFormCtrl($scope, $element, ViewService) {

  ui.FormViewCtrl.call(this, $scope, $element);
  $scope._register('form', $scope);
  $scope.setEditable();

  // prevent requesting defaults
  $scope.defaultValues = {};

  $scope.$watch('_searchView', function searchSchemaWatch(schema) {
    if (!schema) return;
    var form = {
      title: 'Search',
      type: 'form',
      cols: 1,
      items: [{
        type: 'panel',
        title: schema.title,
        items: schema.searchFields
      }]
    };

    var meta = { fields: schema.searchFields };
    ViewService.process(meta, schema.searchForm);

    function process(item) {
      if (item.items || item.pages) {
        return _.each(item.items || item.pages, process);
      }
      switch (item.widget) {
      case 'ManyToOne':
      case 'OneToOne':
      case 'SuggestBox':
        item.canNew = false;
        item.canEdit = false;
        break;
      case 'OneToMany':
      case 'ManyToMany':
      case 'MasterDetail':
        item.hidden = true;
      }
    }

    if (schema.searchForm && schema.searchForm.items) {
      _.each(schema.searchForm.items, process);
    }

    $scope.fields = meta.fields;
    $scope.schema = schema.searchForm || form;
    $scope.schema.loaded = true;
  });

  var model = null;
  var getContext = $scope.getContext;

  $scope.getContext = function() {
    var view = $scope._searchView || {};
    if (model === null && view.selects) {
      model = (_.first(view.selects) || {}).model;
    }

    var ctx = getContext.apply(this, arguments) || {};
    ctx._model = model;
    return ctx;
  };

  $scope.editSearch = function (record, fields) {
    $scope.editRecord(record);
    setTimeout(function () {
      _.each(fields, function (field) {
        if (!field.target || !$scope.record) return;
        var item = $element.find('[x-field=' + field.name + ']');
        var itemScope = item.data('$scope');
        var value = itemScope.getValue();
        if (value && itemScope && !itemScope.text && itemScope.select) {
          itemScope.select(value);
        }
      });
    });
  };
}

ui.controller('SearchGridCtrl', SearchGridCtrl);

SearchGridCtrl.$inject = ['$scope', '$element', 'ViewService', '$interpolate'];
function SearchGridCtrl($scope, $element, ViewService, $interpolate) {

  ui.GridViewCtrl.call(this, $scope, $element);
  $scope._register('grid', $scope);

  var viewTitles = {};

  $scope.$watch('_searchView', function searchSchemaWatch(schema) {
    if (!schema) return;
    var view = {
      title: 'Search',
      type: 'grid',
      editIcon: true,
      items: []
    };

    var objItem = _.findWhere(schema.resultFields, {name: 'object'});
    if (!objItem) {
      view.items.push(objItem = {});
    }

    objItem = _.extend(objItem, { name : '_modelTitle', title: _t('Object') });
    view.items = view.items.concat(schema.resultFields);

    if (+(objItem.width) === 0) {
      objItem.hidden = true;
    }

    var meta = { fields: schema.resultFields };
    ViewService.process(meta);

    _.each(schema.selects, function (select) {
      viewTitles[select.model] = select.viewTitle;
    });

    _.each(view.items, function (item) {
      if (item.width) {
        objItem.width = objItem.width || 220;
      }
    });

    $scope.fields = meta.fields;
    $scope.schema = view;
    $scope.schema.loaded = true;
  });

  $scope.onEdit = function(force) {

    var index = _.first(this.selection),
      records = this.getItems(),
      record = this.getItem(index),
      ids, domain, views;

    ids = _.chain(records).filter(function(rec){
        return rec._model == record._model;
      }).pluck('_id').value();

    domain = "self.id IN (" + ids.join(',') + ")";

    views = _.map(['form', 'grid'], function(type){
      var view = { type : type };
      var name = record["_" + type];
      if (name) view.name = name;
      return view;
    });

    if (force === undefined) {
      force = $scope._forceEdit;
    }

    var title = viewTitles[record._model];
    if (title) {
      title = $interpolate(title)(record);
    }

    var tab = {
      action: _.uniqueId('$act'),
      model: record._model,
      title: title || record._modelTitle,
      forceTitle: true,
      domain: domain,
      recordId: record._id,
      forceEdit: force,
      viewType: 'form',
      views: views
    };

    this.openTab(tab);
  };

  $scope.onSort = function(event, args) {
    var grid = args.grid;
    var data = grid.getData();
    var sortCols = args.sortCols;

    var types = {};

    _.each($scope.fields, function (field) {
      types[field.name] = field.type;
    });

    data.sort(function(dataRow1, dataRow2) {
      for (var i = 0, l = sortCols.length; i < l; i++) {
        var name = sortCols[i].sortCol.field;
        var sign = sortCols[i].sortAsc ? 1 : -1;
        var value1 = dataRow1[name], value2 = dataRow2[name];

        switch (types[name]) {
        case "integer":
        case "long":
          value1 = value1 || 0;
          value2 = value2 || 0;
          break;
        default:
          value1 = value1 || "";
          value2 = value2 || "";
        }

        var result = (value1 == value2 ? 0 : (value1 > value2 ? 1 : -1)) * sign;
        if (result) {
          return result;
        }
      }
      return 0;
    });

    grid.invalidate();
    grid.render();
  };
}

ui.controller('SearchToolbarCtrl', SearchToolbarCtrl);

SearchToolbarCtrl.$inject = ['$scope', '$element', '$http'];
function SearchToolbarCtrl($scope, $element, $http) {

  ui.FormViewCtrl.call(this, $scope, $element);
  $scope._register('toolbar', $scope);
  $scope.setEditable();

  var menus = {};

  function fetch(key, parent, request, response) {

    if (menus[key]) {
      return response(menus[key]);
    }

    var promise = $http.get('ws/search/menu', {
      params: {
        parent: parent
      }
    });
    promise.then(function(res){
      var data = res.data.data;
      data = _.map(data, function(item){
        return {
          value: item.name,
          action: item.action,
          label: item.title
        };
      });
      menus[key] = data;
      response(data);
    });
  }

  $scope.fetchRootMenus = function(request, response) {
    fetch('menuRoot', null, request, response);
  };

  $scope.fetchSubMenus = function(request, response) {
    fetch('menuSub', $scope.record.menuRoot, request, response);
  };

  $scope.fetchItemMenus = function(request, response) {
    fetch('menuItem', $scope.record.menuSub, request, response);
  };

  $scope.resetSelector = function(a, b) {
    _.each(arguments, function(name){
      if (_.isString(name)) {
        $scope.record[name] = null;
        menus[name] = null;
      }
    });
  };

  $scope.getMenuAction = function() {
    return _.find(menus.menuItem, function(item){
      return item.value === $scope.record.menuItem;
    });
  };

  $scope.$watch('_searchView', function searchSchemaWatch(schema) {

    if (!schema) {
      return;
    }

    var selected = [];

    $scope.fields = {
      'objectSelect' : {
        type : 'string',
        placeholder: _t('Search Objects'),
        multiple : true,
        selectionList : _.map(schema.selects, function(x) {
          if (x.selected) {
            selected.push(x.model);
          }
          return {
            value : x.model,
            title : x.title
          };
        })
      },
      'menuRoot' : {
        type : 'string',
        placeholder: _t('Action Category'),
        widget: 'select-query',
        attrs: {
          query: 'fetchRootMenus',
          'ng-change': 'resetSelector("menuSub", "menuItem")'
        }
      },
      'menuSub' : {
        placeholder: _t('Action Sub-Category'),
        widget: 'select-query',
        attrs: {
          query: 'fetchSubMenus',
          'ng-change': 'resetSelector($event, "menuItem")'
        }
      },
      'menuItem' : {
        placeholder: _t('Action'),
        widget: 'select-query',
        attrs: {
          query: 'fetchItemMenus'
        }
      }
    };

    var items1 = [{
      name : 'objectSelect',
      showTitle : false,
      colSpan: 8
    }, {
      type : 'button',
      title : _t('Search'),
      colSpan: 2,
      attrs: {
        'ng-click': 'doSearch()'
      }
    }, {
      type : 'button',
      title : _t('Clear'),
      colSpan: 2,
      attrs: {
        'ng-click': 'doClear(true)'
      }
    }];

    var items2 = [{
      name : 'menuRoot',
      showTitle : false,
      colSpan: 3
    }, {
      name : 'menuSub',
      showTitle : false,
      colSpan: 3
    }, {
      name : 'menuItem',
      showTitle : false,
      colSpan: 4
    }, {
      type : 'button',
      title : _t('Go'),
      colSpan: 2,
      attrs: {
        'ng-click': 'doAction()'
      }
    }];

    var item1 = {
      type: "panel",
      colSpan: $scope._hideActions ? 12 : 6,
      items: items1
    };

    var item2 = {
      type: "panel",
      colSpan: 6,
      items: items2
    };

    schema = {
      type : 'form',
      items: [{
        type: 'panel',
        items: $scope._hideActions ? [item1] : [item1, item2]
      }]
    };

    $scope.schema = schema;
    $scope.schema.loaded = true;

    $scope.doReset = function () {
      var record = $scope.record || {};
      if (selected.length > 0 && _.isEmpty(record.objectSelect)) {
        record.objectSelect = selected.join(', ');
        $scope.edit(record);
      }
    };

    $scope.$timeout($scope.doReset);
  });
}

angular.module('axelor.ui').directive('uiViewSearch', function(){
  return {
    controller: SearchViewCtrl,
    link: function(scope, element, attrs, ctrl) {

      element.on('keypress', '.search-view-form form:first', function(event){
        if (event.keyCode == 13 && $(event.target).is('input')){
          scope.doSearch();
        }
      });

      var grid = element.children('.search-view-grid');
      scope.$onAdjust(function(){
        if (!element.is(':visible'))
          return;
        grid.height(element.height() - grid.position().top);
      });
    }
  };
});

// ActionSelector (TODO: re-use search view toolbar)

ActionSelectorCtrl.$inject = ['$scope', '$element', '$attrs', '$http', 'MenuService'];
function ActionSelectorCtrl($scope, $element, $attrs, $http, MenuService) {

  ui.FormViewCtrl.call(this, $scope, $element);
  var menus = {},
    category = $attrs.category;

  function fetch(key, request, response, params) {

    if (menus[key]) {
      return response(menus[key]);
    }

    var promise = $http.get('ws/search/menu', {
      params: params
    });
    promise.then(function(res){
      var data = res.data.data;
      data = _.map(data, function(item){
        return {
          value: item.name,
          action: item.action,
          label: item.title
        };
      });
      menus[key] = data;
      response(data);
    });
  }

  $scope.fetchRootMenus = function(request, response) {
    fetch('$menuRoot', request, response, {
      parent: '',
      category: category
    });
  };

  $scope.fetchSubMenus = function(request, response) {
    if (!$scope.record.$menuRoot) return;
    fetch('$menuSub', request, response, {
      parent: $scope.record.$menuRoot
    });
  };

  $scope.fetchItemMenus = function(request, response) {
    if (!$scope.record.$menuSub) return;
    fetch('$menuItem', request, response, {
      parent: $scope.record.$menuSub
    });
  };

  $scope.resetSelector = function(a, b) {
    _.each(arguments, function(name){
      if (_.isString(name)) {
        $scope.record[name] = null;
        menus[name] = null;
      }
    });
  };

  $scope.getMenuAction = function() {
    return _.find(menus.$menuItem, function(item){
      return item.value === $scope.record.$menuItem;
    });
  };

  $scope.doAction = function() {

    var action = $scope.getMenuAction();
    if (!action) {
      return;
    }

    var context = $scope.$parent.getContext(),
      record;

    action = action.action;
    record = {
      id : context.id,
      _action: action,
      _model: context._model
    };

    MenuService.action(action).success(function(result){

      if (!result.data) {
        return;
      }

      var view = result.data[0].view;
      var tab = view;

      tab.action = _.uniqueId('$act');
      tab.viewType = 'form';

      tab.context = _.extend({}, tab.context, {
        _ref : record
      });

      $scope.openTab(tab);
    });
  };

  $scope.fields = {
    '$menuRoot' : {
      type : 'string',
      placeholder: _t('Action Category'),
      widget: 'select-query',
      attrs: {
        query: 'fetchRootMenus',
        'ng-change': 'resetSelector("$menuSub", "$menuItem")'
      }
    },
    '$menuSub' : {
      placeholder: _t('Action Sub-Category'),
      type: 'select-query',
      attrs: {
        query: 'fetchSubMenus',
        'ng-change': 'resetSelector($event, "$menuItem")'
      }
    },
    '$menuItem' : {
      placeholder: _t('Action'),
      widget: 'select-query',
      attrs: {
        query: 'fetchItemMenus'
      }
    }
  };

  $scope.schema = {
    cols : 4,
    colWidths : '30%,30%,30%,10%',
    type : 'form',
    items : [ {
      name : '$menuRoot',
      showTitle : false
    }, {
      name : '$menuSub',
      showTitle : false
    }, {
      name : '$menuItem',
      showTitle : false
    }, {
      type : 'button',
      title : _t('Go'),
      attrs: {
        'ng-click': 'doAction()'
      }
    } ]
  };
}

angular.module('axelor.ui').directive('uiActionSelector', function(){
  return {
    scope: true,
    controller: ActionSelectorCtrl,
    template: '<div ui-view-form x-handler="this"></div>'
  };
});

})();
