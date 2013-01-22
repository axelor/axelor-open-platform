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

		function action(name) {

			return $http.post('ws/action/' + name, {
				model : 'com.axelor.meta.db.MetaAction',
				data : {}
			});
		}

		return {
			get: get,
			action: action
		};
	}]);

	ds.factory('ViewService', ['$http', '$q', '$cacheFactory', '$compile', function($http, $q, $cache, $compile) {

		var ViewService = function() {
			
			this.FIELD_TYPES = {
				'STRING'		: 'string',
				'INTEGER'		: 'integer',
				'LONG'			: 'integer',
				'DECIMAL'		: 'decimal',
				'BOOLEAN'		: 'boolean',
				'DATE'			: 'date',
				'TIME'			: 'time',
				'DATETIME'		: 'datetime',
				'TEXT'			: 'text',
				'ONE_TO_ONE'	: 'many-to-one',
				'MANY_TO_ONE'	: 'many-to-one',
				'ONE_TO_MANY'	: 'one-to-many',
				'MANY_TO_MANY'	: 'many-to-many',
				'label'			: 'static', 
				'notebook'		: 'tabs',
				'page'			: 'tab',
				'password'		: 'password',
				'SuggestBox'	: 'suggest-box',
				'CodeEditor'	: 'code-editor',
				'ActionSelector': 'action-selector'
			};
		};
		
		function titleize(str){
			if (!str) return;
		    return str.replace(/_|\.|\s+/g, ' ')
	    			  .replace(/\s*Id$/, '')
	    			  .replace(/\s*Ids$/, 's')
	    			  .replace(/([a-z\d])([A-Z])/g, '$1 $2')
	    			  .replace(/(?:^|\s)\S/g, function(ch){
		    	return ch.toUpperCase();
		    });
		}
		
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

			var fields = {},
				types = this.FIELD_TYPES; 
			
			meta = meta || {};
			view = view || {};
			
			if (isArray(meta.fields)) {
				forEach(meta.fields, function(field){
					field.type = types[field.type] || field.type || 'string';
					field.title = titleize(field.title || field.name);
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
			
			forEach(view.items, function(item) {
				forEach(fields[item.name], function(value, key){
					if (!item.hasOwnProperty(key))
						item[key] = value;
				});
			});
		};
		
		function findFields(view) {
			var items = [];
			var fields = view.items || view.pages;
			
			if (fields == null)
				return [];
			
			_.each(fields, function(item) {
				if (item.items || item.pages) {
					items = items.concat(findFields(item));
				}
				else if (item.type === 'field') {
					items.push(item.name);
				}
			});
			return items;
		}
		
		var fieldsCache = $cache("viewFields", { capacity: 100 });

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
				var key = model + "|" + fields.join("|");
				
				if (!model || !fields || fields.length === 0) {
					deferred.resolve({view: view});
					return promise;
				}

				var result = fieldsCache.get(key);
				if (result) {
					deferred.resolve(angular.copy(result));
					return promise;
				}

				$http.post('ws/meta/view/fields', {
					model: model,
					fields: findFields(view)
				}).then(function(response) {
					var res = response.data,
						fields = res.data,
						data = { fields: fields };

					self.process(data, view);
					fields = data.fields;

					var result = {
						fields: fields,
						view: view
					};
					
					fieldsCache.put(key, angular.copy(result));
					deferred.resolve(result);
				});

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
				var res = response.data;
				result = res.data;
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
				_model: model,
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

		return new ViewService();
	}]);
	

})(this);
