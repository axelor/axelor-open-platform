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

ui.controller('TreeViewCtrl', TreeViewCtrl);

TreeViewCtrl.$inject = ['$scope', '$element', 'DataSource', 'ActionService'];
function TreeViewCtrl($scope, $element, DataSource, ActionService) {

  var view = $scope._views.tree;
  var viewPromise = $scope.loadView('tree', view.name);

  $scope.$applyAsync(function() {
    if (view.deferred) {
      view.deferred.resolve($scope);
    }
  });

  viewPromise.success(function(fields, schema){
    $scope.parse(schema);
  });

  $scope.show = function() {
    $scope.updateRoute();
  };

  $scope.onShow = function(promise) {

  };

  $scope.getRouteOptions = function() {
    return {
      mode: "tree"
    };
  };

  $scope.setRouteOptions = function(options) {
    $scope.updateRoute();
  };

  $scope.onRefresh = function() {

  };

  $scope.onSort = function(column) {
    if (column) {
      column.sort = true;
      column.desc = column.desc !== undefined && !column.desc;
      column.sortCss = column.desc ? "slick-sort-indicator-desc" : "slick-sort-indicator-asc";
    }
    $scope.onRefresh();
  };

  var first = null;

  $scope.parse = function(schema) {

    var columns = _.map(schema.columns, function(col) {
      return new Column($scope, col);
    });

    var last = null;
    var draggable = false;

    var loaders = _.map(schema.nodes, function(node) {
      var loader = new Loader($scope, node, DataSource);
      if (last) {
        last.child = loader;
      }
      if (loader.draggable) {
        draggable = true;
      }
      return last = loader;
    });

    $scope.viewTitle = schema.title;

    $scope.columns = columns;
    $scope.loaders = loaders;
    $scope.draggable = draggable;

    first = _.first(loaders);

    first.domain = $scope._domain;
    first.context = $scope._context;

    // recursive tree (parent -> child on same object)
    if (loaders.length === 2 && first.model === last.model) {
      last.child = last;
      $scope._countOn = _.last(schema.nodes).parent;
    }
  };

  $scope.onNext = function() {
    return first && first.onNext();
  };

  $scope.onPrev = function() {
    return first && first.onPrev();
  };

  $scope.canNext = function() {
    return first && first.canNext();
  };

  $scope.canPrev = function() {
    return first && first.canPrev();
  };

  $scope.pagerText = function() {
    return first ? first.pagerText() : "";
  };

  $scope.resetPager = function() {
    if (first) {
      first.resetPager();
    }
  };

  $scope.onClick = function(e, options) {

    var loader = options.loader,
      record = options.record;

    var target = $(e.target);
    if (target.is('img,i')) {
      target = target.parent();
    }
    if (e.type === 'click' && !target.is('.tree-button')) {
      return;
    }
    var action = target.attr('x-action') || loader.action;
    if (!action) {
      return;
    }

    var $handler = ActionService.handler($scope.$new(), $(e.currentTarget), {
      action: action
    });

    var model = loader.model;
    var context = record.$record;

    $handler.scope.record = context;
    $handler.scope.getContext = function() {
      return _.extend({
        _model: model
      }, context);
    };

    $handler.onClick().then(function(res){

    });
  };

  $scope.$on('on:tab-reload', function(e, tab) {
    if ($scope === e.targetScope && $scope.onRefresh) {
      $scope.onRefresh();
    }
  });
}

/**
 * Column controller.
 *
 */
function Column(scope, col) {

  this.css = col.type || 'string';
  this.name = col.name;
  this.title = col.title || col.autoTitle;

  if (this.title === null || this.title === undefined) {
    this.title = _.humanize(col.name);
  }
  if (col.type == 'button') {
    this.title = null;
  }

  this.cellCss = function(record) {
    return this.css;
  };

  this.cellText = function(record) {

    if (col.type === 'button') {
      var template = "---";
      var item = _.findWhere(record.$node.items, { type: 'button', name: col.name });
      if (item) {
        template = "<a href='javascript:' class='tree-button' x-action='"+ item.onClick +"'>";
        if (item.icon) {
          if (item.icon.indexOf('fa') === 0) {
            template += "<i class='fa " + item.icon + "'></i>";
          } else {
            template += "<img width='16px' src='"+ item.icon +"'>";
          }
        }
        if (item.title) {
          template += item.title;
        }
        template += "</a>";
      }
      return template;
    }

    var value = record[this.name];
    if (value === undefined || value === null) {
      return '---';
    }

    var selection = (record.$selection || {})[this.name];
    if (selection) {
      var cmp = col.type === "integer" ? function(a, b) { return a == b ; } : _.isEqual;
      var res = _.find(selection, function(item){
        return cmp(item.value, value);
      }) || {};

      if (col.widget === 'image-select' && res.icon) {
        var image = "<img style='max-height: 24px;' src='" + (res.icon || res.value) + "'>";
        if (col.labels === false) {
          return image;
        }
        return image + " " + res.title;
      }

      return res.title;
    }
    var type = col.type;
    if (type === 'reference') {
      type = 'many-to-one';
    }
    var item = _.findWhere(record.$node.items, { type: 'field', as: col.name });
    var attrs = _.extend({}, item, col);
    var fn = ui.formatters[type];
    if (fn) {
      value = fn(attrs, value, record);
    }
    return value === undefined || value === null ? '---' : value;
  };
}

/**
 * Node loader.
 *
 */
function Loader(scope, node, DataSource) {

  var ds = DataSource.create(node.model);
  var names = _.pluck(node.items, 'name');
  var domain = null;

  if (node.parent) {
    domain = "self." + node.parent + ".id = :parentId";
    ds._page.limit = -1;
  }

  if (node.domain) {
    if (domain) {
      domain = '(' + domain + ') AND (' + node.domain + ')';
    } else {
      domain = node.domain;
    }
  }

  this.node = node;

  this.child = null;

  this.model = node.model;

  this.action = node.onClick;

  this.draggable = node.draggable;

  this.getDomain = function(context) {
    var _domain = domain,
      _context = context;

    if (_domain && this.domain) {
      _domain = "(" + this.domain + ") AND (" + domain + ")";
    }

    _domain = _domain || this.domain;
    _context = _.extend({}, this.context, context);

    return {
      domain: _domain,
      context: _context
    };
  };

  this.resetPager = function () {
    ds._page.from = 0;
  };

  this.load = function(item, callback) {

    var context = _.extend({}, scope._context),
      current = item && item.$record;

    var sortOn = _.filter(scope.columns, function (col) { return col.sort; });
    var sortBy = _.map(sortOn, function (col) {
      var field = _.findWhere(node.items, { as: col.name });
      if (field) {
        return col.desc ? '-' + field.name : field.name;
      }
    });

    sortBy = _.compact(sortBy).join(',') || node.orderBy;

    if (scope.getContext) {
      context = _.extend(context, scope.getContext());
    }
    if (current) {
      context.parentId = current.id;
    }

    if (scope._countOn) {
      context._countOn = scope._countOn;
    } else if (this.child) {
      var child = this.child.node;
      context._childOn = {
        model: child.model,
        parent: child.parent
      };
    }

    var opts = _.extend(this.getDomain(context), {
      fields: names,
      action: scope._viewAction
    });

    if (sortBy) {
      opts.sortBy = sortBy.split(',');
    }

    var promise = ds.search(opts);

    promise.success(function(records) {
      if (callback) {
        callback(accept(item, records));
      }
    });

    return promise;
  };

  this.move = function(item, callback) {

    var record = item.$record,
      parent = { id: item.$parentId };

    record[node.parent || scope._countOn] = parent;

    return ds.save(record).success(function(rec) {
      record.version = rec.version;
      if (callback) {
        callback(rec);
      }
    });
  };

  var that = this;

  function accept(current, records) {

    var fields = node.items,
      parent = current && current.$record,
      child = that.child;

    return _.map(records, function(record) {

      var $id = _.uniqueId('row');
      var $parent = current ? current.$id : null;

      var item = {
        '$id': $id,
        '$model': node.model,
        '$node': node,
        '$record': record,
        '$selection': {},
        '$parent': $parent,
        '$parentId': parent && parent.id,
        '$parentModel': current && current.$model,
        '$draggable': node.draggable,
        '$folder': child && (record._children === undefined || record._children > 0)
      };

      item.$expand = function(callback) {
        if (child) {
          return child.load(this, callback);
        }
      };

      item.$move = function(callback) {
        return that.move(this, callback);
      };

      item.$click = function(e) {
        if (node.onClick) {
          scope.onClick(e, {
            loader: that,
            record: item,
            parent: parent
          });
        }
      };

      _.each(fields, function(field) {
        var name = field.as || field.name;
        item[name] = record[field.name];
        item.$selection[name] = field.selectionList;
      });

      return item;
    });
  }

  var page = {};

  ds.on('change', function(e, _records, _page) {
    page = _page;
  });

  this.canNext = function() {
    return ds.canNext();
  };

  this.canPrev = function() {
    return ds.canPrev();
  };

  this.onNext = function() {
    ds.next(names).success(function(records){
      scope.setRootNodes(accept(null, records));
    });
  };

  this.onPrev = function() {
    ds.prev(names).success(function(records){
      scope.setRootNodes(accept(null, records));
    });
  };

  this.pagerText = function() {
    if (page && page.from !== undefined) {
      if (page.total === 0) return null;
      return _t("{0} to {1} of {2}", page.from + 1, page.to, page.total);
    }
  };
}

ui.directive('uiViewTree', function(){

  return {

    replace: true,

    link: function(scope, element, attrs) {

      var table = element.find('.tree-table > table');

      table.treetable({

        indent: 16,

        expandable: true,

        clickableNodeNames: true,

        nodeIdAttr: "id",

        parentIdAttr: "parent",

        branchAttr: "folder",

        onNodeCollapse: function onNodeCollapse() {
          var node = this,
            row = node.row;

          if (node._state === "collapsed") {
            return;
          }
          node._state = "collapsed";

          table.treetable("collapseNode", row.data("id"));
          adjustCols();
        },

        onNodeExpand: function onNodeExpand() {

          var node = this,
            row = this.row,
            record = row.data('$record');

          if (node._loading || node._state === "expanded") {
            return;
          }

          node._state = "expanded";

          if (node._loaded) {
            table.treetable("expandNode", row.data("id"));
            return adjustCols();
          }

          node._loading = true;

          if (record.$expand) {
            record.$expand(function(records) {
              acceptNodes(records, node);
              node._loading = false;
              node._loaded = true;
              adjustCols();
            });
          }
        }
      });

      function acceptNodes(records, after) {
        var rows = _.map(records, makeRow);
        table.treetable("loadBranch", after, rows);
      }

      function makeRow(record) {

        var tr = $('<tr>')
          .attr('data-id', record.$id)
          .attr('data-parent', record.$parent)
          .attr('data-folder', record.$folder);

        tr.data('$record', record);

        _.each(scope.columns, function(col) {
          $('<td>').html(col.cellText(record)).appendTo(tr);
        });

        if (scope.draggable && (record.$folder || scope._countOn || !record.$parent)) {
          makeDroppable(tr);
        }
        if (record.$draggable || (scope.draggable && scope._countOn)) {
          makeDraggable(tr);
        }

        tr.on('click dblclick taphold', function(e) {
          record.$click(e);
        });

        return tr[0];
      }

      function onDrop(e, ui) {
        /* jshint validthis: true */
        var row = ui.draggable,
          record = row.data('$record'),
          current = $(this).data('$record'),
          node = table.treetable("node", row.data("id")),
          nodeParent = node.parentNode();

        table.treetable("move", node.id, $(this).data("id"));

        // make sure to remove expander icon if no children left
        if (nodeParent && nodeParent.children.length === 0) {
          nodeParent.row.removeClass('expanded');
          nodeParent.row.removeClass('branch');
          nodeParent.row.addClass('leaf');

          nodeParent.treeCell.off('click.treetable');
          nodeParent.treeCell.off('keydown.treetable');
          nodeParent.indenter.empty();
        }

        record.$parentId = current.$record.id;
        record.$move(function(result) {

        });
      }

      function isParent(source, target) {
        var parent = target.parent().find('[data-id=' + target.data('parent') + ']');
        if (parent.data('id') === source.data('id')) {
          return true;
        }
        if (parent.length) {
          return isParent(source, parent);
        }
        return false;
      }

      function makeDroppable(row) {

        row.droppable({
          accept: function(draggable, x) {
            var source = draggable.data('$record'),
              target = row.data('$record');

            // don't allow moving parent to child
            if (scope._countOn) {
              return !isParent(draggable, $(this));
            }

            return source && target && target.$model === source.$parentModel;
          },
              hoverClass: "accept",
              drop: onDrop,
              over: function(e, ui) {
                var row = ui.draggable;
                if(this != row[0] && !$(this).is(".expanded")) {
                  table.treetable("expandNode", $(this).data("id"));
                }
              }
        });
      }

      function makeDraggable(row) {

        var record = row.data('$record');
        if (!record.$draggable && !scope._countOn) {
          return;
        }

        row.draggable({
          helper: function() {
            return $('<span></span>').append(row.children('td:first').clone());
          },
          opacity: 0.75,
          containment: 'document',
          refreshPositions: true,
          revert: "invalid",
          revertDuration: 300,
          delay: 300,
          scroll: true
        });
      }

      function clear() {

        var tree = table.data('treetable');
        if (tree === undefined) {
          return;
        }

        _.each(tree.roots, function(node) {
          tree.unloadBranch(node);
          node.row.remove();
          delete tree.tree[node.id];
        });

        tree.nodes.length = 0;
        tree.roots.length = 0;
      }

      scope.onRefresh = function() {
        var root = _.first(scope.loaders);
        if (root) {
          root.load(null, function(nodes) {
            scope.setRootNodes(nodes);
          });
        }
      };

      scope.setRootNodes = function(nodes) {
        clear();
        acceptNodes(nodes);
      };

      scope.onHeaderClick = function (event, column) {
        if (!event.shiftKey) {
          _.each(scope.columns, function (col) {
            if (col !== column) {
              col.sort = false;
              col.desc = undefined;
              col.sortCss = null;
            }
          });
        }
        scope.onSort(column);
      };

      var watcher = scope.$watch('loaders', function treeLoadersWatch(loaders) {

        if (loaders === undefined) {
          return;
        }

        watcher();

        var root = _.first(loaders);
        if (root) {
          root.load(null, acceptNodes).then(adjustCols);
        }
      });

      var adjustCounter = 0;

      function adjustCols() {

        if (element.is(':hidden')) {
          if (adjustCounter++ < 10) {
            _.delay(adjustCols, 100);
          }
          return;
        }
        adjustCounter = 0;

        var tds = table.find('tr:first').find('td');
        var ths = element.find('.tree-header').find('th');
        var widths = [];

        if (tds.length !== ths.length) {
          return;
        }

        tds.each(function() {
          widths.push($(this).outerWidth());
        });

        ths.each(function(i) {
          $(this).width(widths[i] - 12);
        });
      }

      scope.$onAdjust(adjustCols, 100);

      table.on('mousedown.treeview', 'tbody tr', function(e) {
        table.find('tr.selected').removeClass('selected');
        $(this).addClass("selected");
      });
    },
    template:
    '<div class="tree-view-container" ui-attach-scroll="> .tree-table">'+
      '<table class="tree-header">'+
        '<thead>'+
          '<tr>'+
            '<th ng-repeat="column in columns" ng-class="column.css" ng-click="onHeaderClick($event, column)">' +
              '<span>{{column.title}}</span>'+
              '<span ng-if="column.sort" class="slick-sort-indicator" ng-class="column.sortCss"></span>'+
            '</th>'+
          '</tr>'+
        '</thead>'+
      '</table>'+
      '<div class="tree-table">'+
        '<table>'+
          '<tbody></tbody>'+
        '</table>'+
      '</div>'+
    '</div>'
  };
});

TreePortletCtrl.$inject = ['$scope', '$element', 'DataSource', 'ActionService'];
function TreePortletCtrl($scope, $element, DataSource, ActionService) {

  TreeViewCtrl.call(this, $scope, $element, DataSource, ActionService);

  $scope.showPager = true;

  $scope.$on("on:new", function(e) {
    $scope.resetPager();
    $scope.onRefresh();
  });
  $scope.$on("on:edit", function(e) {
    $scope.resetPager();
    $scope.onRefresh();
  });
}

ui.directive('uiPortletTree', function(){

  return {
    controller: TreePortletCtrl,
    template: '<div ui-view-tree></div>'
  };
});

})();
