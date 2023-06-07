/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

function BaseCardsCtrl(type, $scope, $element, ViewService) {

  ui.DSViewCtrl(type, $scope, $element);

  $scope.getRouteOptions = function() {
    return {
      mode: type,
      args: []
    };
  };

  $scope.setRouteOptions = function(options) {
    if (!$scope.isNested) {
      $scope.updateRoute();
    }
  };

  var ds = $scope._dataSource;
  var initialized = false;

  $scope.onShow = function (viewPromise) {

    if (initialized) {
      return $scope.onRefresh();
    }

    initialized = true;

    viewPromise.then(function (meta) {
      $scope.parse(meta.fields, meta.view);
    });
  };

  $scope.parse = function (fields, view) {

  };

  $scope.onNew = function () {
    if ($scope.onEditPopup(null, false)) return;
    ds._page.index = -1;
    $scope.switchTo('form', function (formScope) {
      formScope.edit(null);
      formScope.setEditable();
      formScope.$broadcast("on:new");
    });
  };

  $scope.onEditPopup = function (record, readonly) {
    var view = $scope.schema || {};
    if (view.editWindow === 'popup' || (view.editWindow === 'popup-new' && !record) || $scope.$$portlet) {
      $scope.showEditor(record, readonly);
      return true;
    }
  };

  $scope.onRefresh = function () {
    return $scope.filter({});
  };

  function update(records) {
    $scope.records = records;
  }

  $scope.handleEmpty = function () {
  };

  $scope.filter = function(options) {
    var view = $scope.schema || {};
    var opts = _.extend(_.pick(options, ['action', 'domain', 'context', 'archived']), {
      fields: _.pluck($scope.fields, 'name')
    });
    var handleEmpty = $scope.handleEmpty.bind($scope);

    if (options.criteria || options._domains) {
      opts.filter = options;
    }
    if (options.archived !== undefined) {
      opts.archived = options.archived;
    }
    if (view.orderBy) {
      opts.sortBy = view.orderBy.split(',');
    }

    var promise = ds.search(opts);
    promise.then(handleEmpty, handleEmpty);
    return promise.success(update).then(function () {
      $scope.handleEmpty();
      return ds.fixPage(opts);
    });
  };

  $scope.pagerText = function() {
    var page = ds._page;
    if (page && page.from !== undefined) {
      if (page.total === 0) return null;
      return _t("{0} to {1} of {2}", page.from + 1, page.to, page.total);
    }
  };

  $scope.onNext = function() {
    var fields = _.pluck($scope.fields, 'name');
    return ds.next(fields).success(update);
  };

  $scope.onPrev = function() {
    var fields = _.pluck($scope.fields, 'name');
    return ds.prev(fields).success(update);
  };

  $scope.getActionData = function(context) {
    return _.extend({
      _domain: ds._lastDomain,
      _domainContext: _.extend({}, ds._lastContext, context),
      _archived: ds._showArchived
    }, ds._filter);
  };

  $scope.attr = function(name) {
    if (!$scope.schema || $scope.schema[name] === undefined) {
      return true;
    }
    return $scope.schema[name];
  };

  var editor = null;

  $scope.showEditor = function(record, readonly) {
    if (!editor) {
      var editorScope = $scope.$new(true);
      editorScope._viewParams = _.extend({}, $scope._viewParams);
      editorScope.$$readonly = true;
      editorScope.editorCanSave = true;
      editorScope.select = angular.noop;
      editorScope.getRouteOptions = angular.noop;
      editorScope.setRouteOptions = angular.noop;
      editor = ViewService.compile('<div ui-editor-popup></div>')(editorScope);
      editor.data('$target', $element);
      $scope.$on('$destroy', function () {
        editor.remove();
        editorScope.$destroy();
      });
    }

    var popup = editor.isolateScope();

    popup.show(record, function () {
      $scope.onRefresh();
    });

    popup.waitForActions(function() {
      if (!record || !record.id) {
        popup.$broadcast("on:new");
      } else {
        popup.setEditable(!readonly);
      }
    });
  };

  $scope.canNew = function () {
    return $scope.hasButton("new");
  };

  $scope.onHotKey = function (e, action) {
    switch (action) {
      case "refresh":
        $scope.onRefresh();
        break;
      case "new":
        if ($scope.canNew()) {
          $scope.onNew();
        }
        break;
      case "next":
        if ($scope.canNext()) {
          $scope.onNext();
        }
        break;
      case "prev":
        if ($scope.canPrev()) {
          $scope.onPrev();
        }
        break;
    }

    $scope.$applyAsync();
    return false;
  };
}

ui.controller("CardsCtrl", ['$scope', '$element', 'ViewService', function CardsCtrl($scope, $element, ViewService) {

  BaseCardsCtrl.call(this, 'cards', $scope, $element, ViewService);

  $scope.viewItems = {};

  $scope.parse = function (fields, view) {
    var viewItems = {};
    _.each(view.items, function (item) {
      if (item.name) {
        viewItems[item.name] = _.extend({}, item, fields[item.name], item.widgetAttrs);
      }
    });
    $scope.viewItems = viewItems;
    $scope.onRefresh();
    $scope.waitForActions(axelor.$adjustSize);
  };

  $scope.onExport = function (full) {
    var fields = full ? [] : _.pluck($scope.viewItems, 'name');
    return $scope._dataSource.export_(fields).success(function(res) {
      var fileName = res.fileName;
      var filePath = 'ws/rest/' + $scope._model + '/export/' + fileName;
      ui.download(filePath, fileName);
    });
  };
}]);

ui.controller("KanbanCtrl", ['$scope', '$element', 'ViewService', 'ActionService', function KanbanCtrl($scope, $element, ViewService, ActionService) {

  BaseCardsCtrl.call(this, 'kanban', $scope, $element, ViewService);

  $scope.parse = function (fields, view) {
    var params = $scope._viewParams.params || {};
    var hideCols = (params['kanban-hide-columns'] || '').split(',');
    var columnBy = fields[view.columnBy] || {};
    var columns = _.filter(view.columns, function (item) {
      return hideCols.indexOf(item.value) === -1;
    });

    var first = _.first(columns);
    if (view.onNew) {
      first.canCreate = true;
    }

    var sequenceBy = fields[view.sequenceBy] || {};
    if (["integer", "long"].indexOf(sequenceBy.type) === -1 || ["id", "version"].indexOf(sequenceBy.name) > -1) {
      throw new Error("Invalid sequenceBy field in view: " + view.name);
    }

    $scope.sortableOptions.disabled = !view.draggable || !$scope.hasPermission('write');
    $scope.columns = columns;
    $scope.colWidth = params['kanban-column-width'];

    if (columnBy.target) {
      $scope.toColumnValue = function (value) {
        return { id : value };
      };
    }
  };

  $scope.toColumnValue = function (value) {
    return value;
  };

  $scope.move = function (record, to, next, prev) {
    if(!record) {
        return;
    }
    var ds = $scope._dataSource._new($scope._model);
    var view = $scope.schema;

    var rec = _.pick(record, "id", "version", view.sequenceBy);
    var prv = prev ? _.pick(prev, "id", "version", view.sequenceBy) : null;
    var nxt = next ? _.pick(next, "id", "version", view.sequenceBy) : null;

    // update columnBy
    rec[view.columnBy] = $scope.toColumnValue(to);

    // update sequenceBy
    var all = _.compact([prv, rec, nxt]);
    var offset = _.min(_.pluck(all, view.sequenceBy)) || 0;

    _.each(all, function (item, i) {
      item[view.sequenceBy] = offset + i;
    });

    function doSave() {
      return ds.saveAll(all).success(function (records) {
        _.each(_.compact([prev, rec, next]), function (item) {
          _.extend(item, _.pick(ds.get(item.id), "version", view.sequenceBy));
        });
        _.extend(record, rec);
      }).error(function () {
        $scope.onRefresh();
      });
    }

    if (view.onMove) {
      var actScope = $scope.$new();
      actScope.record = rec;
      actScope.getContext = function () {
        return _.extend({}, $scope._context, rec);
      };
      return ActionService.handler(actScope, $(), { action: view.onMove }).handle().then(function () {
        return doSave();
      }, function (err) {
        $scope.onRefresh();
      });
    }

    return doSave();
  };

  $scope.onRefresh = $scope.reload = function () {
    $scope.$broadcast("on:refresh");
  };

  $scope.filter = function(searchFilter) {
    var options = {};
    if (searchFilter.criteria || searchFilter._domains) {
      options = {
        filter: searchFilter
      };
      if (searchFilter.archived !== undefined) {
        options.archived = searchFilter.archived;
      }
      $scope.$broadcast("on:filter", options);
    }
  };

  $scope.sortableOptions = {
    connectWith: ".kanban-card-list",
    items: ".kanban-card",
    tolerance: "pointer",
    helper: "clone",
    stop: function (event, ui) {
      $scope.$broadcast('on:re-attach-click');
      var item = ui.item;
      var sortable = item.sortable;
      var source = sortable.source.scope();
      var target = (sortable.droptarget || $(this)).scope();

      var next = item.next().scope();
      var prev = item.prev().scope();
      if (next) next = next.record;
      if (prev) prev = prev.record;

      var index = sortable.dropindex;
      if (source === target && sortable.index === index) {
        return;
      }

      $scope.move(target.records[index], target.column.value, next, prev);
      $scope.$applyAsync();
    }
  };

  $scope.isAllowedToExport = function () {
    return false;
  };
}]);

ui.directive('uiKanban', function () {
  return {
    replace: true,
    template:
      "<div class='kanban-view row-fluid' ng-class='::schema.css'>" +
        "<div class='kanban-column' ng-repeat='column in ::columns' ui-kanban-column>" +
          "<h3>{{::column.title}}</h3>" +
          "<div class='input-group' ng-if='::column.canCreate' ng-show='hasPermission(\"create\")'>" +
            "<input type='text' class='form-control' ng-model='$parent.newItem'>" +
            "<span class='input-group-btn'>" +
              "<button type='button' class='btn' ng-click='onCreate()'><span x-translate>Add</span></button>" +
            "</span>" +
          "</div>" +
          "<ul class='kanban-card-list' ui-sortable='sortableOptions' ng-model='records'>" +
            "<li class='kanban-card' ng-class='hilite.color' ng-repeat='record in records' ui-card>" +
              "<div class='kanban-card-menu btn-group pull-right' ng-if='hasButton(\"edit\") || hasButton(\"delete\")' ng-show='hasPermission(\"write\") || hasPermission(\"remove\")'>" +
                "<a tabindex='-1' href='javascript:' class='btn btn-link dropdown-toggle' data-toggle='dropdown'>" +
                  "<i class='fa fa-caret-down'></i>" +
                "</a>" +
                "<ul class='dropdown-menu pull-right'>" +
                  "<li><a href='javascript:' ng-if='hasButton(\"edit\")' ng-show='hasPermission(\"write\")' ng-click='onEdit(record)' x-translate>Edit</a></li>" +
                  "<li><a href='javascript:' ng-if='hasButton(\"delete\")' ng-show='hasPermission(\"remove\")' ng-click='onDelete(record)' x-translate>Delete</a></li>" +
                "</ul>" +
              "</div>" +
              "<div class='kanban-card-body'></div>" +
            "</li>" +
          "</ul>" +
          "<div class='kanban-empty'>" +
            "<span class='help-block text-center' x-translate>No records found.</span>" +
          "</div>" +
          "<div class='kanban-more ng-hide' ng-show='hasMore()'>" +
            "<a class='btn btn-load-more' tabindex='-1' href='' role='button' ng-click='onMore()'>" +
              "<span x-translate>load more</span>" +
              "<i class='fa fa-arrow-right fa-fw'></i>" +
            "</a>" +
          "</div>" +
        "</div>" +
      "</div>"
  };
});

ui.directive('uiKanbanColumn', ["ActionService", function (ActionService) {

  return {
    scope: true,
    link: function (scope, element, attrs) {

      var ds = scope._dataSource._new(scope._model);
      var view = scope.schema;
      var elemMore = element.children(".kanban-more");
      var columnFilter = {
        operator: 'and',
        criteria: [{
          fieldName: view.columnBy,
          operator: '=',
          value: scope.toColumnValue(scope.column.value)
        }]
      };

      ds._context = _.extend({}, scope._dataSource._context);
      ds._page.limit = view.limit || parseInt((scope._viewParams.params || {}).limit) || 20;
      ds._sortBy = [view.sequenceBy];
      ds._domain = scope._dataSource._domain;
      ds._filter = columnFilter;

      if (!scope.prepareFilter) scope.prepareFilter = function (options) { return options; };

      scope.records = [];

      function handleEmpty() {
        element.toggleClass('empty', scope.isEmpty());
      }

      function combineCriteria(first, second) {
        first = angular.copy(first) || {};
        second = angular.copy(second) || {};
        if (_.isEmpty(first.criteria)) {
          first.criteria = second.criteria;
          first.operator = 'and';
        } else if (!_.isEmpty(second.criteria)) {
          first.criteria = [{
            criteria: first.criteria,
            operator: first.operator
          }, {
            criteria: second.criteria,
            operator: second.operator
          }];
          first.operator = 'and';
        }
        return first;
      }

      function fetch(options) {
        var opts = _.extend({
          fields: _.pluck(scope.fields, 'name')
        }, scope.prepareFilter(options));

        if (options) {
          opts.filter = combineCriteria(opts.filter, columnFilter);
        }

        elemMore.hide();
        var promise = ds.search(opts);
        promise.success(function (records) {
          scope.records = scope.records.concat(records);
          elemMore.fadeIn('slow');
        });
        return promise.then(handleEmpty, handleEmpty);
      }

      scope.$watch('records.length', handleEmpty);

      scope.hasMore = function () {
        var page = ds._page;
        var next = page.from + page.limit;
        return next < page.total;
      };

      scope.isEmpty = function () {
        return scope.records.length == 0;
      };

      scope.onMore = function () {
        var page = ds._page;
        var next = scope.records.length;
        if (next < page.total) {
          return fetch({
            offset: next
          });
        }
      };

      var onNew = null;

      scope.getContext = function () {
        var ctx = _.extend({}, scope._context);
        ctx._value = scope.newItem;
        return ctx;
      };

      scope.newItem = null;
      scope.onCreate = function () {

        var rec = scope.record = {};
        var view = scope.schema;

        rec[view.columnBy] = scope.toColumnValue(scope.column.value);

        if (onNew === null) {
          onNew = ActionService.handler(scope, element, {
            action: view.onNew
          });
        }

        var ds = scope._dataSource;
        var promise = onNew.handle();
        promise.then(function () {
          ds.save(scope.record).success(function (rec) {
            scope.newItem = null;
            scope.records.unshift(rec);
          });
        });
      };

      scope.onEdit = function (record, readonly) {
        if (scope.onEditPopup(record, readonly)) return;
        scope._dataSource._record = record;
        scope.switchTo('form', function (formScope) {
          formScope.setEditable(!readonly && scope.hasPermission('write') && formScope.canEdit());
        });
      };

      scope.onDelete = function (record) {
        axelor.dialogs.confirm(_t("Do you really want to delete the selected record?"),
        function(confirmed) {
          if (!confirmed) {
            return;
          }
          ds.removeAll([record]).success(function(records, page) {
            var index = scope.records.indexOf(record);
            scope.records.splice(index, 1);
          });
        });
      };

      scope.$on("on:refresh", function (e) {
        scope.newItem = null;
        scope.records.length = 0;
        fetch();
      });

      scope.$on("on:filter", function (e, options) {
        scope.newItem = null;
        scope.records.length = 0;
        return fetch(options);
      });

      if (scope.colWidth) {
        element.width(scope.colWidth);
      }

      setTimeout(function () {
        element.find('[ui-sortable]').sortable("option", "appendTo", element.parent());
      });

      fetch();
    }
  };
}]);

ui.directive('uiCards', function () {
  return {
    link: function (scope, element, attrs) {
      var onRefresh = scope.onRefresh;
      scope.onRefresh = scope.reload = function () {
        scope.records = null;
        return onRefresh.apply(scope, arguments);
      };

      scope.onEdit = function (record, readonly) {
        if (scope.onEditPopup(record, readonly)) return;
        var ds = scope._dataSource;
        var page = ds._page;
        page.index = record ? ds._data.indexOf(record) : -1;
        scope.switchTo('form', function (formScope) {
          formScope.setEditable(!readonly && scope.hasPermission('write') && formScope.canEdit());
        });
      };

      scope.onDelete = function (record) {
        axelor.dialogs.confirm(_t("Do you really want to delete the selected record?"),
        function(confirmed) {
          if (!confirmed) {
            return;
          }
          var ds = scope._dataSource;
          ds.removeAll([record]).success(function() {
            scope.onRefresh();
          });
        });
      };

      scope.isEmpty = function () {
        return (scope.records||[]).length == 0;
      };

      scope.handleEmpty = function () {
        element.toggleClass('empty', scope.isEmpty());
      };
    },
    replace: true,
    template:
    "<div class='cards-view row-fluid' ng-class='::schema.css'>" +
      "<div class='kanban-card-list'>" +
        "<div class='cards-no-records' x-translate>No records found.</div>" +
        "<div class='kanban-card-container' ng-repeat='record in records'>" +
        "<div class='kanban-card' ng-class='hilite.color' ui-card>" +
          "<div class='kanban-card-menu btn-group pull-right' ng-if='hasButton(\"edit\") || hasButton(\"delete\")' ng-show='hasPermission(\"write\") || hasPermission(\"remove\")'>" +
            "<a tabindex='-1' href='javascript:' class='btn btn-link dropdown-toggle' data-toggle='dropdown'>" +
                "<i class='fa fa-caret-down'></i>" +
            "</a>" +
            "<ul class='dropdown-menu pull-right'>" +
                "<li><a href='javascript:' ng-if='hasButton(\"edit\")' ng-show='hasPermission(\"write\")' ng-click='onEdit(record)' x-translate>Edit</a></li>" +
                "<li><a href='javascript:' ng-if='hasButton(\"delete\")' ng-show='hasPermission(\"remove\")' ng-click='onDelete(record)' x-translate>Delete</a></li>" +
            "</ul>" +
          "</div>" +
          "<div class='kanban-card-body'></div>" +
        "</div>" +
        "</div>" +
      "</div>" +
    "</div>"
  };
});

ui.directive('uiCard', ["$compile", function ($compile) {

  return {
    scope: true,
    link: function (scope, element, attrs) {

      var body = element.find(".kanban-card-body");
      var record = scope.record;
      var evalScope = axelor.$evalScope(scope);

      evalScope.record = record;
      evalScope.getContext = scope.getContext = function () {
        var ctx = _.extend({}, scope._context, scope.record);
        ctx._model = scope._model;
        return ctx;
      };

      if (!record.$processed) {
        element.hide();
      }

      function process(record) {
        if (record.$processed) {
          return record;
        }
        record.$processed = true;
        for (var name in record) {
          if (!record.hasOwnProperty(name) || name.indexOf('.') === -1) {
            continue;
          }
          var nested = record;
          var names = name.split('.');
          var head = _.first(names, names.length - 1);
          var last = _.last(names);
          var i, n;
          for (i = 0; i < head.length; i++) {
            n = head[i];
            nested = nested[n] || (nested[n] = {});
          }
          nested[last] = record[name];
        }
        return record;
      }

      evalScope.$watch("record", function cardRecordWatch(record) {
        _.extend(evalScope, process(record));
      }, true);

      evalScope.$image = function (fieldName, imageName) {
        return ui.formatters.$image(scope, fieldName, imageName);
      };

      evalScope.$fmt = function (fieldName) {
        return ui.formatters.$fmt(scope, fieldName, evalScope[fieldName]);
      };

      var template = (scope.schema.template || "<span></span>").trim();
      if (template.indexOf('<') !== 0) {
        template = "<span>" + template + "</span>";
      }

      scope.hilite = null;

      $compile(template)(evalScope).appendTo(body);

      var hilites = scope.schema.hilites || [];
      for (var i = 0; i < hilites.length; i++) {
        var hilite = hilites[i];
        if (axelor.$eval(evalScope, hilite.condition, scope.record)) {
          scope.hilite = hilite;
          break;
        }
      }

      if (scope.schema.width) {
        element.parent().css("width", scope.schema.width);
      }
      if (scope.schema.minWidth) {
        element.parent().css("min-width", scope.schema.minWidth);
      }
      if (scope.schema.maxWidth) {
        element.parent().css("max-width", scope.schema.maxWidth);
      }

      function onClick(e) {
        var elem = $(e.target);
        var selector = '[ng-click],[ui-action-click],button,a,.iswitch,.ibox,.kanban-card-menu';
        if (elem.is(selector) || element.find(selector).has(elem).length) {
          return;
        }
        var record = $(this).scope().record;
        scope.onEdit(record, true);
        scope.$applyAsync();
      }

      function attachClick() {
        element.on('click', onClick);
      }

      attachClick();

      scope.$on('on:re-attach-click', function () {
        element.off('click', onClick);
        setTimeout(attachClick, 100);
      });

      element.fadeIn("slow");

      var summaryHandler;
      var summaryPlacement;
      var summary = body.find('.card-summary.popover');

      var configureSummary = _.once(function configureSummary() {
        element.popover({
          placement: function (tip, el) {
            summaryPlacement = setTimeout(function () {
              $(tip).css('visibility', 'hidden').css('max-width', 400).position({
                my: 'left',
                at: 'right',
                of: el,
                using: function (pos, feedback) {
                  $(feedback.element.element)
                    .css(pos)
                    .css('visibility', '')
                    .removeClass('left right')
                    .addClass(feedback.horizontal === 'left' ? 'right' : 'left');
                  summaryPlacement = null;
                }
              });
            });
          },
          container: 'body',
          trigger: 'manual',
          title: summary.attr('title'),
          content: summary.html(),
          html: true
        });
      });

      function showSummary() {
        if((summary.text() || "").trim() == "") {
          return;
        }
        configureSummary();
        if (summaryPlacement) {
          clearTimeout(summaryPlacement);
          summaryPlacement = null;
        }
        summaryHandler = setTimeout(function () {
          summaryHandler = null;
          element.popover('show');
        }, 500);
      }

      function hideSummary() {
        if (summaryPlacement) {
          clearTimeout(summaryPlacement);
          summaryPlacement = null;
        }
        if (summaryHandler) {
          clearTimeout(summaryHandler);
          summaryHandler = null;
        }
        element.popover('hide');
      }

      if (summary.length > 0) {
        element.on('mouseenter.summary', showSummary);
        element.on('mouseleave.summary', hideSummary);
        element.on('mousedown.summary', hideSummary);
      }

      function destroy() {
        if (summaryHandler) {
          clearTimeout(summaryHandler);
          summaryHandler = null;
        }
        if (element) {
          element.off('mouseenter.summary');
          element.off('mouseleave.summary');
          element.off('mousedown.summary');
          element.popover('destroy');
          element = null;
        }
      }

      element.on('$destroy', destroy);
      scope.$on('$destroy', destroy);
    }
  };
}]);

function linker(scope, element, atts) {
  scope.$$portlet = true;

  var _filter = scope.filter;
  var _action = scope._viewAction;

  scope.prepareFilter = function (options) {
    var opts = _.extend({}, options, {
      action: _action
    });
    if (scope._context && scope.formPath && scope.getContext) {
      opts.context = _.extend({id: null}, scope._context, scope.getContext());
    }
    return opts;
  };

  scope.filter = function (options) {
    var opts = scope.prepareFilter(options);
    return _filter.call(scope, opts);
  };

  function refresh() {
    scope.onRefresh();
  }

  scope.$on('on:new', refresh);
  scope.$on('on:edit', refresh);
}

angular.module('axelor.ui').directive('uiPortletCards', function () {
  return {
    controller: 'CardsCtrl',
    replace: true,
    link: function (scope, element, attrs) {
      linker(scope, element, attrs);
      scope.showPager = true;
      scope.showSearch = true;
    },
    template:
      "<div class='portlet-cards' ui-portlet-refresh>" +
        "<div ui-cards></div>" +
      "</div>"
  };
});

angular.module('axelor.ui').directive('uiPortletKanban', function () {
  return {
    controller: 'KanbanCtrl',
    replace: true,
    link: linker,
    template:
      "<div class='portlet-kanban' ui-portlet-refresh>" +
        "<div ui-kanban></div>" +
      "</div>"
  };
});

})();
