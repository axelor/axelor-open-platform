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

		function action(name) {

			return $http.post('ws/action/' + name, {
				model : 'com.axelor.meta.db.MetaAction',
				data : {}
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

			if (isArray(meta.fields)) {
				forEach(meta.fields, function(field){
					field.type = _.chain(field.type || 'string').underscored().dasherize().value();
					field.title = _.chain(field.title || _.humanize(field.name)).value();
					fields[field.name] = field;
					// if nested field then make it readonly
					if (field.name.indexOf('.') > -1) {
						field.readonly = true;
						field.required = false;
					}
				});
				meta.fields = fields;
			} else {
				fields = meta.fields || {};
			}

			forEach(view.items || view.pages, function(item) {
				processWidget(item);
				forEach(fields[item.name], function(value, key){
					if (!item.hasOwnProperty(key)) {
						item[key] = value;
					}
				});
				if (item.items || item.pages) {
					ViewService.prototype.process(meta, item);
				}
			});
		};

		function processWidget(field) {

			var widget = field.widget || '',
				match = widget.match(/^([\w-]*)\[(.*?)\]$/),
				widgetAttrs = {};

			if (widget) {
				field.widgetName = widget;
			}
			if (!match) {
				return;
			}

			field.widgetName = match[1].trim();
			field.widgetAttrs = widgetAttrs;

			_.each(match[2].split(/\s*\|\s*/), function(part) {
				var parts = part.split(/\s*=\s*/);
				var attrName = parts[0].trim();
				var attrValue = parts[1].trim();
				if (attrValue.match(/^(\d+)$/)) {
					attrValue = +attrValue;
				}
				if (attrValue === "true") {
					attrValue = true;
				}
				if (attrValue === "false") {
					attrValue = false;
				}
				if (attrValue === "null") {
					attrValue = null;
				}
				widgetAttrs[attrName] = attrValue;
			});
		}

		function useIncluded(view) {
			var items = [];
			_.each(view.items, function(item) {
				if (item.type === "include") {
					if (item.view) {
						items = items.concat(useIncluded(item.view));
					}
				} else {
					items.push(item);
				}
			});
			return items;
		}

		function findFields(view) {
			var items = [];
			var fields = view.items || view.pages;

			if (fields == null)
				return [];

			if (view.items && !view._included) {
				view._included = true;
				fields = view.items = useIncluded(view);
			}

			_.each(fields, function(item) {
				if (item.items || item.pages) {
					items = items.concat(findFields(item));
				}
				else if (item.type === 'field') {
					items.push(item.name);
				}
			});

			if (view.type === "calendar") {
				items.push(view.eventStart);
				items.push(view.eventStop);
				items.push(view.colorBy);
			}

			return _.compact(items);
		}

		var fieldsCache = $cacheFactory("viewFields", { capacity: 1000 });

		ViewService.prototype.getMetaDef = function(model, view) {
			var self = this,
				deferred = $q.defer();

			var promise = deferred.promise;
			promise.success = function(fn) {
				promise.then(function(res){
					fn(res.fields, res.view);
				});
				return promise;
			};

			function loadFields(view) {

				var fields = findFields(view) || [];
				var key = model + "|" + view.type + "|" + view.name + "|" + fields.join("|");

				if (!model || !fields || fields.length === 0) {
					deferred.resolve({view: view});
					return promise;
				}

				var result = fieldsCache.get(key);
				if (result && result.fields) {
					deferred.resolve(angular.copy(result));
					return promise;
				}
				if (result && result.then) {
					result.then(resolver);
					return promise;
				}

				function resolver(response) {
					var res = response.data,
						data = res.data;

					view.perms = data.perms;
					self.process(data, view);

					if (view.perms && !view.perms.write) {
						view.editable = false;
					}

					var fields = data.fields;
					var result = {
						fields: fields,
						view: view
					};

					fieldsCache.put(key, angular.copy(result));
					deferred.resolve(result);

					return promise;
				}

				var _promise = $http.post('ws/meta/view/fields', {
					model: model,
					fields: findFields(view)
				});

				_promise.then(resolver);
				fieldsCache.put(key, _promise);

				return promise;
			}

			if (_.isArray(view.items)) {
				return loadFields(view);
			};

			$http.get('ws/meta/view', {
				cache: true,
				params: {
					model: model,
					type: view.type,
					name: view.name
				}
			}).then(function(response) {
				var res = response.data,
					result = res.data;

				if (!result) {
					return deferred.reject('view not found', view);
				}

				if (_.isArray(result.items)) {
					loadFields(result);
				} else {
					deferred.resolve({
						fields: result.items,
						view: result
					});
				}
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
