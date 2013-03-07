(function($, undefined) {

	var extend = angular.extend,
		isArray = angular.isArray,
		isObject = angular.isObject,
		forEach = angular.forEach,
		push = [].push;

	var ds = angular.module('axelor.ds');

	ds.factory('DataSource', ['$http', '$q', '$exceptionHandler', function($http, $q, $exceptionHandler) {

		function DataSource(model, options) {
			
			var opts = extend({
				limit	: DataSource.DEFAULT_LIMIT,
				domain	: null,
				context	: null
			}, options);

			this._model = model;
			this._domain = opts.domain;
			this._context = opts.context;
		
			this._filter = null;
			this._sortBy = null;
			this._lastContext = null;
			this._showArchived = opts.archived;

			this._data = [];

			this._page = {
				index	: 0,
				from	: 0,
				to		: 0,
				size	: 0,
				total	: 0,
				limit	: opts.limit
			};
			
			this._listeners = {};
		};
		
		DataSource.DEFAULT_LIMIT = 40;

		DataSource.prototype = {

			constructor: DataSource,
			
			_request: function(action, id) {
				var url = 'ws/rest/' + this._model;
				if (id) url += '/' + id;
				if (action) url += '/' + action;
				
				return {
					get: function(data, config) {
						return $http.get(url, data, config);
					},
					post: function(data, config) {
						return $http.post(url, data, config);
					}
				};
			},
			
			on: function(name, listener) {
				var listeners = this._listeners[name];
				if(!listeners){
					this._listeners[name] = listeners = [];
				}
				listeners.push(listener);
				return function() {
					var index = listeners.indexOf(listener);
					if (index >= 0)
						listeners.splice(index, 1);
					return listener;
				};
			},
			
			trigger: function(name) {
				var listeners = this._listeners[name] || [],
					event = {
						name : name,
						target: this
					},
					listenerArgs = [event];
				listenerArgs = listenerArgs.concat([].slice.call(arguments, 1));

				forEach(listeners, function(listener) {
					try {
						listener.apply(null, listenerArgs);
					} catch (e) {
						$exceptionHandler(e);
					}
				});
			},
			
			/**
			 * Check whether two objects are equal.
			 * 
			 * It compares relational field values by it's id if version property
			 * is missing (assuming, they are not modified).
			 * 
			 */
			equals: function(a, b) {
				if (a === b) return true;
				if (a === null || b === null) return false;
				if (a !== a && b !== b) return true; // NaN === NaN
				
				function isWindow(obj) {
				  return obj && obj.document && obj.location && obj.alert && obj.setInterval;
				}

				function isScope(obj) {
				  return obj && obj.$evalAsync && obj.$watch;
				}

				function compact(obj) {
					if (!obj || !_.isObject(obj)) return obj;
					if (isScope(obj) || isWindow(obj)) return obj;
					if (_.isArray(obj)) return _.map(obj, compact).sort();
					if (_.isDate(obj)) return obj;
					if (obj.id > 0 && !_.has(obj, "version")) {
						return obj.id;
					}
					var res = {};
					_.each(obj, function(v, k) {
						res[k] = compact(v);
					});
					return res;
				}
				return angular.equals(compact(a), compact(b));
			},
			
			search: function(options) {
				
				if (options == null)
					options = {};

				var limit = options.limit == undefined ? this._page.limit : options.limit;
				var offset = options.offset == undefined ? this._page.from : options.offset;
				var domain = options.domain === undefined ? this._domain : options.domain;
				var context = options.context == undefined ? this._lastContext : options.context;
				var archived = options.archived == undefined ? this._showArchived : options.archived;

				if (options.filter) {
					this._filter = options.filter;
					offset = options.offset ? offset : 0;
				}
				if (options.sortBy) {
					this._sortBy = options.sortBy;
				}
				if (options.context) {
					this._lastContext = options.context;
				}
				if (options.archived !== undefined) {
					this._showArchived = options.archived;
				}
				
				context = _.extend({}, this._context, context);

				var query = extend({
					_domain: domain,
					_domainContext: context
				}, this._filter);

				if (!archived) {

					var criteria = _.clone(query.criteria || []);

					query.criteria = criteria;
					query.operator = query.operator || 'and';
					
					criteria.push({
						operator: 'or',
						criteria: [{
							fieldName: 'archived',
							operator: 'isNull'
						}, {
							fieldName: 'archived',
							operator: 'iEquals',
							value: false
						}]
					});
				}
				
				var that = this,
					page = this._page,
					records = this._data,
					sortBy = this._sortBy,
					params = {
						textMatchStyle: 'substring',
						sortBy: sortBy,
						fields: options.fields,
						data: query,
						limit: limit,
						offset: offset,
						parent: options.parent
					};
				
				var promise = this._request('search').post(params);

				promise = promise.then(function(response){
					var res = response.data;
					res.offset = offset;
					res.data = res.data || [];
					that._accept(res);
					page.index = -1;
				});
				promise.success = function(fn){
					promise.then(function(res){
						fn(records, page);
					});
					return promise;
				};
				promise.error = function(fn){
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},
			
			at: function(index) {
				return this._data[index];
			},
			
			get: function(id) {
				var i = 0;
				while(i++ < this._data.length) {
					if (this._data[i].id === id)
						return this._data[i];
				}
			},
			
			/**
			 * Reade the object with the given id.
			 * 
			 * If options are provided then POST request is used otherwise GET is used. 
			 * @param id record id
			 * @param options request options
			 * @returns promise
			 */
			read: function(id, options) {
				var promise;
				if (options) {
					promise = this._request('fetch', id).post({
						fields: options.fields
					});
				} else {
					promise = this._request(null, id).get();
				}
				promise.success = function(fn){
					promise.then(function(response){
						var res = response.data,
							record = res.data;
						if (isArray(record))
							record = record[0];
						fn(record);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},
			
			verify: function(values) {
				var promise = this._request("verify").post({
					data: values
				});
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
			},

			save: function(values) {

				var that = this,
					page = this._page,
					promise = this._request().post({
						data: values
					});

				promise.success = function(fn) {
					return promise.then(function(response){
						var res = response.data,
							record;
						
						res.data = res.data[0];
						record = that._accept(res);

						fn(record, page);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};

				return promise;
			},
			
			saveAll: function(items) {

				var that = this,
					page = this._page,
					records = this._data,
					promise = this._request().post({
						records: items
					});

				promise.success = function(fn) {
					return promise.then(function(response){
						var res = response.data;
						_.each(res.data || [], function(item){
							var found = _.find(records, function(rec){
								if (rec.id === item.id) {
									angular.copy(item, rec);
									return true;
								}
							});
							if (!found) {
								records.push(item);
								page.total += 1;
								page.size += 1;
							}
						});
						
						var i = 0;
						while(i < records.length) {
							var rec = records[i];
							if (rec.id === 0) {
								records.splice(i, 1);
								break;
							}
							i ++;
						}

						that.trigger('change', records, page);
						fn(records, page);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};

				return promise;
			},

			remove: function(record) {
				
				var that = this,
					page = this._page,
					records = this._data,
					promise = this._request('remove', record.id).post({
						data: record
					});

				promise = promise.then(function(reponse){
					var index = -1;
					for(var i = 0 ; i < records.length ; i++) {
						if (records[i].id == record.id) {
							index = i;
							break;
						}
					};
					if (index > -1) {
						records.splice(index, 1);
						page.total -= 1;
						page.size -= 1;
					}
					
					that.trigger('change', records, page);
				});

				promise.success = function(fn) {
					promise.then(function(response){
						fn(records, page);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				
				return promise;
			},
			
			removeAll: function(selection) {
				
				var that = this,
					page = this._page,
					records = this._data,
					promise;
				
				selection = _.map(selection, function(record){
					return { "id" : record.id, "version": record.version };
				});

				promise = this._request('removeAll').post({
					records: selection
				});
				
				promise = promise.then(function(response){
					var res = response.data;
					function remove(id) {
						var rec = _.find(records, function(record, i) {
							return record.id == id;
						});
						var index = _.indexOf(records, rec);
						if (index > -1) {
							records.splice(index, 1);
							page.total -= 1;
							page.size -= 1;
						}
					}
					_.each(res.data, function(record) {
						remove(record.id);
					});
					that.trigger('change', records, page);
				});

				promise.success = function(fn) {
					promise.then(function(response){
						fn(records, page);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				
				return promise;
			},

			copy: function(id) {
				var promise = this._request('copy', id).get();
				promise.success = function(fn) {
					promise.then(function(response){
						var res = response.data,
							record = res.data;
						if (isArray(record))
							record = record[0];
						fn(record);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},
			
			details: function(id) {
				var promise = this._request('details', id).get();
				promise.success = function(fn){
					promise.then(function(response){
						var res = response.data,
							record = res.data;
						if (isArray(record))
							record = record[0];
						fn(record);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},
			
			/**
			 * Get current page info.
			 * 
			 */
			page: function() {
				return this._page;
			},

			next: function(fields) {
				var page = this._page;
				return this.search({
					offset: page.from + page.limit,
					fields: fields
				});
			},
		
			prev: function(fields) {
				var page = this._page;
				return this.search({
					offset: page.from - page.limit,
					fields: fields
				});
			},
			
			nextItem: function(success) {
				var self = this,
					page = this._page,
					index = page.index + 1,
					record = this.at(index);

				if (index < page.size) {
					page.index = index;
					return success(record);
				}
				
				this.next().success(function(){
					page.index = 0;
					record = self.at(0);
					success(record);
				});
			},
			
			prevItem: function(success) {
				var self = this,
					page = this._page,
					index = page.index - 1,
					record = this.at(index);
	
				if (index > -1) {
					page.index = index;
					return success(record);
				}
				
				this.prev().success(function(){
					page.index = page.size - 1;
					record = self.at(page.index);
					success(record);
				});
			},
			
			canNext: function() {
				var page = this._page;
				return page.to < page.total;
			},

			canPrev: function() {
				var page = this._page;
				return page.from > 0;
			},
			
			_accept: function(res) {
				var page = this._page,
					records = this._data,
					data = res.data,
					accepted = null;

				if (isArray(data)) {
					
					records.length = 0;
					forEach(data, function(record){
						records.push(record);
					});
					
					page.from = res.offset == undefined ? page.from : res.offset;
					page.to = page.from + records.length;
					page.total = res.total == undefined ? page.total : res.total;
					page.size = records.length;
					
					accepted = records;

				} else if (isObject(data)) {
					
					var record = data;
					var index = -1;
					
					for(var i = 0 ; i < records.length ; i++) {
						if (records[i].id === data.id) {
							index = i;
							break;
						};
					}
					
					if (index > -1) {
						records.splice(index, 1, data);
					} else {
						records.push(record);
						page.total += 1;
						page.size += 1;
						page.index = page.size - 1;
					}
					
					accepted = record;
				}

				this.trigger('change', records, page);
				
				return accepted;
			}
		};
		
		return {
			create: create
		};
		
		function create(model, options) {
			return new DataSource(model, options);
		};

	}]);

})(jQuery);
