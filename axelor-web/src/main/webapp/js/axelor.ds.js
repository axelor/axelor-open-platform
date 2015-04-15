/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

	var ds = angular.module('axelor.ds', ['ngResource']);

	var forEach = angular.forEach,
		extend = angular.extend,
		isArray = angular.isArray;

	ds.factory('MenuService', ['$http', function($http) {

		function get(parent) {

			return $http.get('ws/action/menu', {
				params : {
					parent : parent
				}
			});
		}

		function all() {
			return $http.get('ws/action/menu/all');
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
			action: action
		};
	}]);

	ds.factory('ViewService', ['$http', '$q', '$cacheFactory', '$compile', function($http, $q, $cacheFactory, $compile) {

		var ViewService = function() {

		};

		ViewService.prototype.accept = function(params) {
			views = {};
			forEach(params.views, function(view){
				var type = view.type || view.viewType;
				if (params.viewType == null) {
					params.viewType = type;
				}
				views[type] = extend({}, view, {
					deferred: $q.defer()
				});
			});
			return views;
		};

		ViewService.prototype.compile = function(template) {
			return $compile(template);
		};

		ViewService.prototype.process = function(meta, view) {

			var fields = {};

			meta = meta || {};
			view = view || {};

			meta.fields = processFields(meta.fields);

			forEach(view.items || view.pages, function(item) {
				processWidget(item);
				processSelection(item);
				forEach(fields[item.name], function(value, key){
					if (!item.hasOwnProperty(key)) {
						item[key] = value;
					}
				});
				if (item.items || item.pages) {
					ViewService.prototype.process(meta, item);
				}
				if (item.password) {
					item.widget = "password";
				}
			});
		};
		
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
				attrs[_.str.camelize(name)] = value;
			});
			if (field.serverType) {
				field.serverType = _.chain(field.serverType).underscored().dasherize().value();
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
				return view.menubar = my;
			}
			function useToolbar(toolbar) {
				if (!toolbar) return;
				var my = view.toolbar || toolbar;
				if (my !== toolbar) {
					my = my.concat(toolbar);
				}
				return view.toolbar = my;
			}
			function useItems(view) {
				return useIncluded(view)
			}

			var items = [];

			_.each(view.items, function(item) {
				if (item.type === "include") {
					if (item.view) {
						items = items.concat(useItems(item.view));
						useMenubar(item.view.menubar);
						useToolbar(item.view.toolbar)
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
				related: {},
				hasMessages: false
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
				_.each(editor.items, function (child) {
					if (child.name && collect.indexOf(child.name) === -1 && child.type === 'field') {
						collect.push(child.name);
					}
				});
			}

			_.each(fields, function(item) {
				if (item.editor) acceptEditor(item);
				if (item.type === 'panel-mail') {
					result.hasMessages = true;
				}
				if (item.type === 'panel-related') {
					items.push(item.name);
				} else if (item.items || item.pages) {
					findFields(item, result);
				} else if (item.type === 'field') {
					items.push(item.name);
				}
			});

			if (view.type === "calendar") {
				items.push(view.eventStart);
				items.push(view.eventStop);
				items.push(view.colorBy);
			}

			return result;
		}

		var viewCache = $cacheFactory("viewFields", { capacity: 1000 });

		function viewGet(key) {
			var result = viewCache.get(key);
			return angular.copy(result);
		}
		
		function viewSet(key, result) {
			if (result.then) {
				return viewCache.put(key, result);
			}
			var view = result.view;
			if (view && _.isArray(view.items)) {
				return;
			}
			viewCache.put(key, angular.copy(result));
		}

		ViewService.prototype.getMetaDef = function(model, view, context) {

			var self = this,
				hasItems = _.isArray(view.items),
				deferred = $q.defer(),
				promise = deferred.promise;

			promise.success = function(fn) {
				promise.then(function(res){
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

			function loadFields(data) {

				var fields_data = findFields(data.view);
				var fields = _.unique(_.compact(fields_data.fields.sort()));
				var key = _.flatten([model, data.view.type, data.view.name, fields]).join();

				data.related = fields_data.related;
				data.hasMessages = fields_data.hasMessages;

				if (!_.isEmpty(data.fields)) {
					viewSet(key, data);
					deferred.resolve(process(data));
					return promise;
				}

				if (!model || !fields || fields.length === 0) {
					deferred.resolve(data);
					return promise;
				}

				var result = viewGet(key);
				if (result && result.fields) {
					deferred.resolve(result);
					return promise;
				}
				if (result && result.then) {
					result.then(resolver);
					return promise;
				}

				function resolver(response) {
					var res = response.data,
						result = res.data;

					result.view = data.view || view;
					result = process(result);
					result.related = fields_data.related;
					result.hasMessages = fields_data.hasMessages;

					viewSet(key, result);

					deferred.resolve(result);

					return promise;
				}

				var _promise = $http.post('ws/meta/view/fields', {
					model: model,
					fields: fields
				});

				_promise.then(resolver);

				if (!hasItems) viewSet(key, _promise);

				return promise;
			}

			if (hasItems) {
				return loadFields({view: view});
			};

			$http.post('ws/meta/view', {
				model: model,
				data: {
					type: view.type,
					name: view.name,
					context: context
				}
			}).then(function(response) {
				var res = response.data,
					result = res.data;

				if (!result || !result.view) {
					return deferred.reject('view not found', view);
				}

				if (_.isArray(result.view.items)) {
					return loadFields(result);
				}
				
				if (result.searchForm) {
					result.view.searchForm = result.searchForm;
				}

				deferred.resolve({
					fields: result.view.items,
					view: result.view
				});
			});
			return promise;
		};

		ViewService.prototype.defer = function() {
			return $q.defer();
		};

		ViewService.prototype.action = function(action, model, context) {

			var ctx = _.extend({
				_model: model
			}, context);

			var params = {
				model: model,
				action: action,
				data: {
					context: ctx
				}
			};

			var promise = $http.post('ws/action', params);
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

		ViewService.prototype.getFields = function(model) {

			var that = this,
				promise = $http.get('ws/meta/fields/' + model, {
					cache: true
				});

			promise.success = function(fn) {
				promise.then(function(response) {
					var res = response.data,
						data = res.data;
					that.process(data);
					fn(data.fields);
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


})(this);
