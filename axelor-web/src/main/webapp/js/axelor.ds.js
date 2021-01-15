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

  var ds = angular.module('axelor.ds', ['ngResource']);

  var forEach = angular.forEach,
    extend = angular.extend,
    isArray = angular.isArray;

  ds.factory('MenuService', ['$http', function($http) {

    function get(parent) {

      return $http.get('ws/action/menu', {
        cache: true,
        params : {
          parent : parent
        }
      });
    }

    function all() {
      return $http.get('ws/action/menu/all', {
        cache: true
      });
    }

    function tags() {
      // Select visible menus with tags
      var names = $('.tagged:visible').get().map(function(elem) {
        return elem.dataset.name;
      });
      return $http.post('ws/action/menu/tags', {
        names: names
      }, {
        silent: true
      });
    }

    function action(name, options) {

      return $http.post('ws/action/' + name, {
        model : 'com.axelor.meta.db.MetaAction',
        data : options
      });
    }

    return {
      get: get,
      all: all,
      tags: tags,
      action: action
    };
  }]);

  ds.factory('TagService', ['$q', '$timeout', '$rootScope', 'MenuService', function($q, $timeout, $rootScope, MenuService) {

    var POLL_INTERVAL = 10000;

    var pollResult = {};
    var pollPromise = null;
    var pollIdle = null;

    var listeners = [];

    function cancelPolling() {
      if (pollPromise) {
        $timeout.cancel(pollPromise);
        pollPromise = null;
      }
      if (pollIdle) {
        clearTimeout(pollIdle);
        pollIdle = null;
      }
    }

    function startPolling() {
      if (pollPromise === null) {
        findTags();
      }
    }

    var starting = false;
    function findTags() {
      if (starting) { return; }
      if (pollPromise) {
        $timeout.cancel(pollPromise);
      }
      starting = true;
      MenuService.tags().success(function (res) {
        var data = _.first(res.data);
        var values = data.values;
        for (var i = 0; i < listeners.length; i++) {
          listeners[i](values);
        }
        pollPromise = $timeout(findTags, POLL_INTERVAL);
        if (pollIdle === null) {
          pollIdle = setTimeout(cancelPolling, POLL_INTERVAL * 2);
        }
        starting = false;
      });
    }

    window.addEventListener("mousemove", startPolling, false);
    window.addEventListener("mousedown", startPolling, false);
    window.addEventListener("keypress", startPolling, false);
    window.addEventListener("DOMMouseScroll", startPolling, false);
    window.addEventListener("mousewheel", startPolling, false);
    window.addEventListener("touchmove", startPolling, false);
    window.addEventListener("MSPointerMove", startPolling, false);

    // start polling
    startPolling();

    return {
      find: findTags,
      listen: function(listener) {
        listeners.push(listener);
        return function () {
          var i = listeners.indexOf(listener);
          if (i >= 0) {
            listeners.splice(i, 1);
          }
          return listener;
        };
      }
    };
  }]);

  ds.factory('ViewService', ['$http', '$q', '$cacheFactory', '$compile', function($http, $q, $cacheFactory, $compile) {

    var ViewService = function() {

    };

    ViewService.prototype.accept = function(params) {
      var views = {};
      forEach(params.views, function(view){
        var type = view.type || view.viewType;
        params.viewType = params.viewType || type;
        views[type] = extend({}, view, {
          deferred: $q.defer()
        });
      });
      return views;
    };

    ViewService.prototype.compile = function(template) {
      return $compile(template);
    };

    ViewService.prototype.process = function(meta, view, parent) {

      var fields = {};

      meta = meta || {};
      view = view || {};

      if (meta.jsonAttrs && view && view.items) {
        if (view.type === 'grid') {
          view.items = (function (items) {
            var button = _.findWhere(items, { type: 'button' });
            var index = items.indexOf(button);
            if (index < 0) {
              index = items.length;
            }
            items.splice(index, 0, {
              type: 'field',
              name: 'attrs',
              jsonFields: meta.jsonAttrs
            });
            return items;
          })(view.items);
        }
        if (view.type === 'form') {
          view.items.push({
            type: 'panel',
            title: _t('Attributes'),
            itemSpan: 12,
            items: [{
              type: 'field',
              name: 'attrs',
              jsonFields: meta.jsonAttrs
            }]
          });
        }
      }

      view = processJsonForm(view);
      meta.fields = processFields(meta.fields);

      (function () {
        var helps = meta.helps = meta.helps || {};
        var items = [];

        if (view.helpOverride && view.helpOverride.length) {
          helps = _.groupBy(view.helpOverride || [], 'type');
          helps = meta.helps = _.object(_.map(helps, function(items, key) {
            return [key, _.reduce(items, function(memo, item) {
              memo[item.field] = item;
              return memo;
            }, {})];
          }));

          if (helps.tooltip && helps.tooltip.__top__) {
            view.help = helps.tooltip.__top__.help;
          }
        }

        var help = helps.tooltip || {};
        var placeholder = helps.placeholder || {};
        var inline = helps.inline || {};

        forEach(view.items, function (item) {
          if (help[item.name]) {
            item.help = help[item.name].help;
          }
          if (meta.view && meta.view.type === 'form') {
            if (placeholder[item.name]) {
              item.placeholder = placeholder[item.name].help;
            }
            if (inline[item.name] && !inline[item.name].used) {
              inline[item.name].used = true;
              items.push({
                type: 'help',
                text: inline[item.name].help,
                css: inline[item.name].style,
                colSpan: 12
              });
            }
          }
          items.push(item);
        });

        forEach(view.toolbar, function (item) {
          if (help[item.name]) {
            item.help = help[item.name].help;
          }
        });

        if (items.length) {
          view.items = items;
        }
      })();

      forEach(view.items || view.pages, function(item) {
        processWidget(item);
        processSelection(item);
        forEach(fields[item.name], function(value, key){
          if (!item.hasOwnProperty(key)) {
            item[key] = value;
          }
        });

        ["canNew", "canView", "canEdit", "canRemove", "canSelect"].forEach(function (name) {
          if (item[name] === "false" || item[name] === "true") {
            item[name] = item[name] === "true";
          }
        });

        if (item.items || item.pages) {
          ViewService.prototype.process(meta, item, view);
        }
        if (item.password) {
          item.widget = "password";
        }
        if (item.jsonFields && item.widget !== 'json-raw') {
          var editor = {
            layout: view.type === 'panel-json' ? 'table' : undefined,
            flexbox: true,
            items: [],
          };
          var panel = null;
          var panelTab = null;
          item.jsonFields.sort(function (x, y) { return x.sequence - y.sequence; });
          item.jsonFields.forEach(function (field) {
            if (field.widgetAttrs) {
              field.widgetAttrs = angular.fromJson(field.widgetAttrs);
              if (field.widgetAttrs.showTitle !== undefined) {
                field.showTitle = field.widgetAttrs.showTitle;
              }
              if (field.widgetAttrs.multiline) {
                field.type = 'text';
              }
              if (field.widgetAttrs.targetName) {
                field.targetName = field.widgetAttrs.targetName;
              }

              // remove x- prefix from all widget attributes
              for (var key in field.widgetAttrs) {
                if (_.startsWith(key, 'x-')) {
                  field.widgetAttrs[key.substring(2)] = field.widgetAttrs[key];
                  delete field.widgetAttrs[key];
                }
              }
            }
            processWidget(field);
            // apply all widget attributes directly on field
            _.extend(field, field.widgetAttrs);
            if (field.type === 'panel' || field.type === 'separator') {
              field.visibleInGrid = false;
            }
            if (field.type === 'panel') {
              panel = _.extend({}, field, { items: [] });
              if ((field.widgetAttrs || {}).sidebar && parent) {
                panel.sidebar = true;
                parent.width = 'large';
              }
              if ((field.widgetAttrs || {}).tab) {
                panelTab = panelTab || {
                  type: 'panel-tabs',
                  colSpan: 12,
                  items: []
                };
                panelTab.items.push(panel);
              } else {
                editor.items.push(panel);
              }
              return;
            }
            if (field.type !== 'separator') {
              field.title = field.title || field.autoTitle;
            }
            var colSpan = (field.widgetAttrs||{}).colSpan || field.colSpan;
            if (field.type == 'one-to-many') {
              field.type = 'many-to-many';
              field.canSelect = false;
            }
            if (field.type == 'separator' || (field.type == 'many-to-many' && !field.widget)) {
              field.showTitle = false;
              field.colSpan = colSpan || 12;
            }
            if (panel) {
              panel.items.push(field);
            } else {
              editor.items.push(field);
            }
          });

          if (panelTab) {
            editor.items.push(panelTab);
          }

          item.widget = 'json-field';
          item.editor = editor;
          if (!item.viewer) {
            item.editor.viewer = true;
          }
        }
      });

      // include json fields in grid
      if (view.type === 'grid') {
        var items = [];
        _.each(view.items, function (item) {
          if (item.jsonFields) {
            _.each(item.jsonFields, function (field) {
              var type = field.type || 'text';
              if (type.indexOf('-to-many') === -1 && field.visibleInGrid) {
                items.push(_.extend({}, field, { name: item.name + '.' + field.name }));
              }
            });
          } else {
            if (item.name.indexOf('.') > -1 && (meta.fields[item.name] || {}).jsonField) {
              var field = meta.fields[item.name];
              if (field.widgetAttrs) {
                field.widgetAttrs = angular.fromJson(field.widgetAttrs);
              }
              processWidget(field);
              if (field.widgetAttrs && field.widgetAttrs.targetName) {
                field.targetName = field.widgetAttrs.targetName;
              }
            }
            items.push(item);
          }
        });
        items = items.sort(function (x, y) { return x.columnSequence - y.columnSequence; });
        view.items = items;
      }

      if (view.type === 'form') {
        // more attrs action
        var onLoad = 'com.axelor.meta.web.MetaController:moreAttrs';
        if (view.onLoad) onLoad = view.onLoad + ',' + onLoad;
        view.onLoad = onLoad;
        // wkf status
        view.items.unshift({
          colSpan: 12,
          type: "field",
          name: "$wkfStatus",
          showTitle: false,
          widget: "WkfStatus",
          showIf: "$wkfStatus"
        });
      }
    };

    function processJsonForm(view) {
      if (view.type !== 'form') return view;
      if (view.model !== 'com.axelor.meta.db.MetaJsonRecord') return view;

      var panel = _.first(view.items) || {};
      var jsonField = _.first(panel.items) || {};
      var jsonFields = jsonField.jsonFields || [];

      var first = _.first(jsonFields) || {};
      if (first.type === 'panel') {
        panel.type = 'panel-json';
        if (first.widgetAttrs) {
          var attrs = angular.fromJson(first.widgetAttrs);
          view.width = view.width || attrs.width;
        }
      }

      return view;
    }

    function processFields(fields) {
      var result = {};
      if (isArray(fields)) {
        forEach(fields, function(field){
          field.type = _.chain(field.type || 'string').underscored().dasherize().value();
          field.title = field.title || field.autoTitle;
          result[field.name] = field;
          // if nested field then make it readonly
          if (field.name.indexOf('.') > -1) {
            field.readonly = true;
            field.required = false;
          }
          processSelection(field);
        });
      } else {
        result = fields || {};
      }
      return result;
    }

    function processSelection(field) {
      _.each(field.selectionList, function (item) {
        if (_.isString(item.data)) {
          item.data = angular.fromJson(item.data);
        }
      });
    }

    function processWidget(field) {
      var attrs = {};
      _.each(field.widgetAttrs || {}, function (value, name) {
        if (value === "true") value = true;
        if (value === "false") value = false;
        if (value === "null") value = null;
        if (/^(-)?\d+$/.test(value)) value = +(value);
        if (name === "widget" && value) value = _.chain(value).underscored().dasherize().value();
        attrs[_.str.camelize(name)] = value;
      });
      if (field.serverType) {
        field.serverType = _.chain(field.serverType).underscored().dasherize().value();
      }
      if (field.widget) {
        field.widget = _.chain(field.widget).underscored().dasherize().value();
      }
      field.widgetAttrs = attrs;
    }

    function useIncluded(view) {

      function useMenubar(menubar) {
        if (!menubar) return;
        var my = view.menubar || menubar;
        if (my !== menubar && menubar) {
          my = my.concat(menubar);
        }
        view.menubar = my;
      }
      function useToolbar(toolbar) {
        if (!toolbar) return;
        var my = view.toolbar || toolbar;
        if (my !== toolbar) {
          my = my.concat(toolbar);
        }
        view.toolbar = my;
      }
      function useItems(view) {
        return useIncluded(view);
      }

      var items = [];

      _.each(view.items, function(item) {
        if (item.type === "include") {
          if (item.view) {
            items = items.concat(useItems(item.view));
            useMenubar(item.view.menubar);
            useToolbar(item.view.toolbar);
          }
        } else {
          items.push(item);
        }
      });
      return items;
    }

    function findFields(view, res) {
      var result = res || {
        fields: [],
        related: {}
      };
      var items = result.fields;
      var fields = view.items || view.pages;

      if (!fields) return items;
      if (view.items && !view._included) {
        view._included = true;
        fields = view.items = useIncluded(view);
      }

      function acceptEditor(item) {
        var collect = items;
        var editor = item.editor;
        if (item.target) {
          collect = result.related[item.name] || (result.related[item.name] = []);
        }
        if (editor.fields) {
          editor.fields = processFields(editor.fields);
        }
        var acceptItems = function (items) {
          _.each(items, function (child) {
            if (child.name && collect.indexOf(child.name) === -1 && child.type === 'field') {
              collect.push(child.name);
            } else if (child.type === 'panel') {
              acceptItems(child.items);
            }
            if (child.widget === 'ref-select') {
              collect.push(child.related);
            }
            if (child.depends) {
              child.depends.split(/\s*,\s*/).forEach(function (name) {
                collect.push(name);
              });
            }
          });
        };
        acceptItems(editor.items);
      }

      function acceptViewer(item) {
        var collect = items;
        var viewer = item.viewer;
        if (item.target) {
          collect = result.related[item.name] || (result.related[item.name] = []);
        }
        _.each(viewer.fields, function (item) {
          collect.push(item.name);
        });
        if (viewer.fields) {
          viewer.fields = processFields(viewer.fields);
        }
      }

      _.each(fields, function(item) {
        if (item.editor) acceptEditor(item);
        if (item.viewer) acceptViewer(item);
        if (item.type === 'panel-related') {
          items.push(item.name);
        } else if (item.items || item.pages) {
          findFields(item, result);
        } else if (item.type === 'field') {
          items.push(item.name);
        }

        // process tag-select
        processWidget(item);
        if (item.widget === 'tag-select') {
          // fetch colors
          if (item.colorField) {
            (result.related[item.name] || (result.related[item.name] = [])).push(item.colorField);
          }
          // fetch target names
          if (item.targetName) {
            (result.related[item.name] || (result.related[item.name] = [])).push(item.targetName);
          }
        }
      });

      if (view.type === "calendar") {
        items.push(view.eventStart);
        items.push(view.eventStop);
        items.push(view.colorBy);
      }
      if (view.type === "kanban") {
        items.push(view.columnBy);
        items.push(view.sequenceBy);
      }

      if (view.type === "gantt") {
        items.push(view.taskUser);
      }

      return result;
    }

    var viewCache = $cacheFactory("viewCache", { capacity: 1000 });

    function createStore(prefix) {
      var toKey = function (name) {
        return prefix + ':' + axelor.config['user.id'] + ':' + name;
      };
      return {
        get: function (name) {
          return new $q(function (resolve) {
            resolve(angular.copy(viewCache.get(toKey(name))));
          });
        },
        set: function (name, value) {
          if (value) {
            return new $q(function (resolve) {
              var val = viewCache.put(toKey(name), angular.copy(value));
              resolve(val);
            });
          }
          return $q.resolve(value);
        }
      };
    }

    var PENDING_REQUESTS = {};

    var FIELDS = createStore('f');
    var VIEWS = createStore('v');
    var PERMS = createStore('p');

    ViewService.prototype.getMetaDef = function(model, view, context) {
      var self = this;
      var deferred = $q.defer();
      var promise = deferred.promise;

      promise.success = function(fn) {
        promise.then(function(res) {
          fn(res.fields, res.view);
        });
        return promise;
      };

      function process(data) {
        data.view.perms = data.view.perms || data.perms;
        self.process(data, data.view);

        if (data.perms && data.perms.write === false) {
          data.view.editable = false;
        }

        return data;
      }

      function updateFields(fetched) {
        fetched = fetched || {};
        return FIELDS.get(model).then(function (current) {
          current = current || {};
          if (current !== fetched.fields) {
            _.extend(current, _.object(_.pluck(fetched.fields, 'name'), fetched.fields));
          }
          return $q.all([FIELDS.set(model, current), PERMS.set(model, fetched.perms)]);
        });
      }

      function fetchFields(data) {
        var fields_data = findFields(data.view);
        var fields = _.unique(_.compact(fields_data.fields.sort()));

        data.related = fields_data.related;

        if (_.isArray(data.fields) && data.fields.length > 0) {
          updateFields(data).then(function () {
            deferred.resolve(process(data));
          });
          return promise;
        }
        if (!model || _.isEmpty(fields)) {
          deferred.resolve(data);
          return promise;
        }

        function resolve(fetched) {
          return updateFields(fetched).then(function (res) {
            var current = res[0];
            var perms = res[1];
            var result = _.extend({}, fetched, {
              fields: _.map(fields, function(n) { return current[n]; }),
              perms: perms
            });

            result.view = data.view || view;
            result.fields = _.compact(result.fields);
            result.related = data.related;
            result = process(result);

            deferred.resolve(result);
            return promise;
          });
        }

        $q.all([FIELDS.get(model), PERMS.get(model)]).then(function (res) {
          var fetchedFields = res[0] || {};
          var pendingFields = _.filter(fields, function (n) { return !fetchedFields.hasOwnProperty(n); });
          if (pendingFields.length == 0) {
            resolve({
              fields: _.values(fetchedFields),
              perms: res[1]
            });
            return promise;
          }

          var key = _.flatten([model, pendingFields]).join();
          var pending = PENDING_REQUESTS[key];

          function clear() {
            delete PENDING_REQUESTS[key];
          }

          if (pending) {
            pending.then(clear, clear);
            pending.then(function (response) {
              resolve((response && response.data || {}).data);
            });
            return promise;
          }

          pending = $http.post('ws/meta/view/fields', { model: model, fields: pendingFields }).then(function (response) {
            resolve((response.data || {}).data);
          });

          pending.then(clear, clear);
          PENDING_REQUESTS[key] = pending;
        });
        return promise;
      }

      function fetchView() {
        var key = [model, view.type, view.name].join(':');

        function resolve(response) {
          var result = (response.data || {}).data;
          if (!result || !result.view) {
            return deferred.reject('view not found', view);
          }
          if (result.searchForm) {
            result.view.searchForm = result.searchForm;
          }

          if (_.isArray(result.view.items)) {
            var fieldsPromise = fetchFields(result, key);
            if (!_.isArray(view.items)) {
              // only cache fetched views
              return fieldsPromise.then(function (res) {
                return VIEWS.set(key, _.extend({}, res, { view: result.view }));
              });
            }
            return fieldsPromise;
          }

          result = {
            fields: result.view.items,
            view: result.view
          };

          VIEWS.set(key, result).then(function () {
            deferred.resolve(result);
          });
        }

        VIEWS.get(key).then(function (loaded) {
          if (loaded) {
            return deferred.resolve(loaded);
          }

          function clear() {
            delete PENDING_REQUESTS[key];
          }

          var pending = PENDING_REQUESTS[key];
          if (pending) {
            pending.then(clear, clear);
            return pending.then(resolve);
          }

          pending = $http.post('ws/meta/view', {
            model: model,
            data: {
              type: view.type,
              name: view.name,
              context: context
            }
          });

          pending.then(resolve);

          pending.then(clear, clear);
          PENDING_REQUESTS[key] = pending;
        });

        return promise;
      }

      if (_.isArray(view.items)) {
        return fetchFields({ view: view, fields: view.fields });
      }
      return fetchView();
    };

    ViewService.prototype.defer = function() {
      return $q.defer();
    };

    ViewService.prototype.action = function(action, model, context, data, config) {

      var params = {
        model: model,
        action: action,
        data: _.extend({ criteria: [] }, data, {
          context: _.extend({ _model: model }, context)
        })
      };

      var promise = $http.post('ws/action', params, config);
      promise.success = function(fn) {
        promise.then(function(response){
          fn(response.data);
        });
        return promise;
      };
      promise.error = function(fn) {
        promise.then(null, fn);
        return promise;
      };

      return promise;
    };

    ViewService.prototype.getFields = function(model, jsonModel) {

      var that = this,
        promise = $http.get('ws/meta/fields/' + model, {
          cache: true,
          params: jsonModel ? {
            jsonModel: jsonModel
          } : undefined
        });

      promise.success = function(fn) {
        promise.then(function(response) {
          var res = response.data,
            data = res.data;
          that.process(data);
          fn(data.fields, data.jsonFields);
        });
        return promise;
      };

      promise.error = function(fn) {
        promise.then(null, fn);
        return promise;
      };

      return promise;
    };

    ViewService.prototype.save = function(schema) {
      var promise = $http.post("ws/meta/view/save", {
        data: schema
      });

      promise.success = function(fn) {
        promise.then(function(response) {
          var res = response.data,
            data = res.data;
          fn(data);
        });
        return promise;
      };

      promise.error = function(fn) {
        promise.then(null, fn);
        return promise;
      };

      return promise;
    };

    return new ViewService();
  }]);

})();
