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
(function($, undefined) {

	var extend = angular.extend,
		isArray = angular.isArray,
		isObject = angular.isObject,
		forEach = angular.forEach,
		push = [].push;

	var ds = angular.module('axelor.ds');

	ds.factory('DataSource', ['$injector', '$rootScope', '$http', '$q', '$exceptionHandler', function($injector, $rootScope, $http, $q, $exceptionHandler) {

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
			this._lastDomain = null;
			this._lastContext = null;
			this._showArchived = opts.archived;
			
			if (opts.archived === undefined && _.has(opts.params || {}, 'showArchived')) {
				this._showArchived = opts.params.showArchived;
			}

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
			
			_new: function(model, options) {
				return new DataSource(model, options);
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
					if (obj.id > 0 && !obj.$dirty && !_.has(obj, "version")) {
						return obj.id;
					}
					var res = {};
					_.each(obj, function(v, k) {
						if (k.substring(0, 2) === '__') return;
						res[k] = compact(v);
					});
					return res;
				}
				return angular.equals(compact(a), compact(b));
			},

			/**
			 * Return the difference between two records.
			 * 
			 * The relational fields are compared by it's id if version
			 * property is missing (assuming, the are not modified).
			 * 
			 * @param {Object} a - source record
			 * @param {Object} b - original record
			 * 
			 * @returns {Object}
			 */
			diff: function(a, b) {
				if (a === b) return a;
				if (a === null || b === null) return a;
				if (!a && !b) return a;
				if (!a.id || a.id < 1) return a;
				var result = _.pick(a, 'id', 'version'),
					that = this;

				_.each(a, function (value, key) {
					if (!that.equals(value, b[key])) {
						result[key] = value;
					}
				});

				return result;
			},
			
			rpc: function(method, options) {
				
				var params = _.extend({
					model: this._model,
					domain:  this._domain,
					context: this._lastContext
				}, options);
				
				var promise = $http.post('ws/action/' + method, {
					model : this._model,
					data : params
				});
				
				promise.success = function(fn){
					promise.then(function(response){
						fn(response.data);
					});
					return promise;
				};
				
				promise.error = function(fn){
					promise.then(null, fn);
					return promise;
				};
				
				return promise;
			},
			
			search: function(options) {
				
				var opts = _.extend({
					store: true
				}, options);
				
				var limit = opts.limit == undefined ? this._page.limit : opts.limit;
				var offset = opts.offset == undefined ? this._page.from : opts.offset;
				var domain = opts.domain === undefined ? (this._lastDomain || this._domain) : opts.domain;
				var context = opts.context == undefined ? (this._lastContext || this._context) : opts.context;
				var archived = opts.archived == undefined ? this._showArchived : opts.archived;
				
				var fields = _.isEmpty(opts.fields) ? null : opts.fields;
				var filter = opts.filter || this._filter;
				var sortBy = opts.sortBy || this._sortBy;

				if (opts.store) {
					this._filter = filter;
					this._sortBy = sortBy;
					this._lastDomain = domain;
					this._lastContext = context;
					
					if (opts.archived !== undefined) {
						this._showArchived = opts.archived;
					}
				}

				offset = opts.offset || 0;
				context = _.extend({}, this._context, context);

				var query = extend({
					_domain: domain,
					_domainContext: context,
					_domainAction: opts.action,
					_archived: archived
				}, filter);

				var that = this,
					page = this._page,
					records = this._data,
					params = {
						fields: fields,
						sortBy: sortBy,
						data: query,
						limit: limit,
						offset: offset,
						parent: opts.parent
					};
				
				var promise = this._request('search').post(params);

				promise = promise.then(function(response){
					var res = response.data;
					res.offset = offset;
					res.data = res.data || [];
					if (opts.store) {
						that._accept(res);
						page.index = -1;
					} else {
						records = res.data || [];
						page = that._pageInfo(res);
					}
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
				for (var i = 0; i < this._data.length; i++) {
					if (this._data[i].id === id) {
						return this._data[i];
					}
				}
			},
			
			/**
			 * Read the object with the given id.
			 * 
			 * If options are provided then POST request is used otherwise GET is used. 
			 * @param id record id
			 * @param options request options
			 * @returns promise
			 */
			read: function(id, options) {
				var promise, record;
				if (options) {
					promise = this._request('fetch', id).post({
						fields: options.fields,
						related: options.related
					});
				} else {
					promise = this._request(null, id).get();
				}
				
				promise.then(function(response){
					var res = response.data;
					record = res.data;
					if (isArray(record)) {
						record = record[0];
					}
				});
				
				promise.success = function(fn){
					promise.then(function(response){
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

			isPermitted: function(action, values) {

				var data = _.extend({}, {
					id: values && values.id,
					action: action
				});
				var promise = this._request("perms").get({
					params: data
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

			upload: function(values, field, file) {
				var that = this,
					page = this._page,
					record = null,
					deferred = $q.defer(),
					promise = deferred.promise;
				
				var indicator = $injector.get('httpIndicator');
				promise = indicator(promise);

				var xhr = new XMLHttpRequest();
				var data = new FormData();
				var progress_cb = angular.noop;

				promise.then(function(response) {
					var res = response.data;
					res.data = res.data[0];
					record = that._accept(res);
				});
				promise.progress = function(fn) {
					progress_cb = fn;
					return promise;
				};
				promise.success = function(fn) {
					promise.then(function(response) {
						fn(record, page);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, function(response){
						fn(response.data);
					});
					return promise;
				};
				
				var request = {
					data: values
				};
				
				data.append("file", file);
				data.append("field", field);
				data.append("request", angular.toJson(request));

				xhr.upload.addEventListener("progress", function(e) {
					var complete = Math.round(e.loaded * 100 / e.total);
					progress_cb(complete);
				}, false);
				
				xhr.onreadystatechange = function(e) {
					if (xhr.readyState == 1) {
						_.each($http.defaults.transformRequest, function(tr) {
							tr(data);
						});
					}
					if (xhr.readyState == 4) {
						var data = angular.fromJson(xhr.responseText);
						var response = {
							data: data,
							status: xhr.status
						};
						if (xhr.status == 200) {
							deferred.resolve(response);
						} else {
							deferred.reject(response);
						}
						$rootScope.applyLater();
					}
				};

				xhr.open("POST", "ws/rest/" + this._model + "/upload", true);
				xhr.send(data);

				return promise;
			},

			save: function(values) {

				var that = this,
					page = this._page,
					record, promise;

				if (values && values.$upload) {
					var upload = values.$upload;
					return this.upload(values, upload.field, upload.file);
				}

				promise = this._request().post({
					data: values
				});
				
				promise.then(function(response){
					var res = response.data;
					res.data = res.data[0];
					record = that._accept(res);
				});

				promise.success = function(fn) {
					promise.then(function(response){
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

				promise.then(function(response){
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
			
			updateMass: function (values, ids) {
				
				var domain = this._lastDomain;
				var context = this._lastContext;
				var filter = this._filter;

				var items = _.compact(ids);
				if (items.length > 0) {
					if (domain) {
						domain = domain + " AND self.id IN (:__ids__)";
					} else {
						domain = "self.id IN (:__ids__)";
					}
					context = _.extend({
						__ids__: items
					}, context);
				}
				
				var query = extend({
					_domain: domain,
					_domainContext: context,
					_archived: this._showArchived
				}, filter);
				
				
				
				var promise = this._request('updateMass').post({
					records: [values],
					sortBy: this._sortBy,
					data: query
				});

				promise.success = function(fn) {
					promise.then(function(response){
						var res = response.data;
						fn(res);
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
			
			details: function(id, name) {
				var promise = this._request('details', id).get({
					params: {
						name: name
					}
				});
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
			
			_pageInfo: function(res) {
				var records = res.data || [];
				var page = _.extend({}, this._page);

				page.from = res.offset === undefined ? page.from : res.offset;
				page.to = page.from + records.length;
				page.total = res.total === undefined ? page.total : res.total;
				page.size = records.length;
				
				return page;
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
					
					_.extend(page, this._pageInfo(res));
					
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
						var old = records.splice(index, 1, data)[0];
						_.each(old, function (value, key) {
							if (key.indexOf('.') > -1 && !data.hasOwnProperty(key)) {
								data[key] = value;
							}
						});
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
			},
			
			attachment: function(id, options) {
				if (options == null)
					options = {};

				var params = {
					fields: options.fields
				};
				var promise = this._request('attachment', id).post(params);
				promise.success = function(fn){
					promise.then(function(response){
						var res = response.data,
							records = res.data;
						fn(records);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},
			
			addAttachment: function(objectId, metaFileId) {
				var data = _.extend({}, {
					id: metaFileId
				});
				var promise = this._request('addAttachment', objectId).post({
					data: data
				});
				promise.success = function(fn){
					promise.then(function(response){
						var res = response.data,
							records = res.data;
						fn(records);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},
			
			removeAttachment: function(selection) {
				selection = _.map(selection, function(record){
					return { "id" : record.id, "version": record.version };
				});
				
				var promise = this._request('removeAttachment').post({
					records: selection
				});

				promise.success = function(fn){
					promise.then(function(response){
						var res = response.data,
							records = res.data;
						fn(records);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
			},

			export_: function(fields) {
				var domain = this._lastDomain || this._domain;
				var context = this._lastContext;
				var filter = this._filter;

				var query = extend({
					_domain: domain,
					_domainContext: context,
					_archived: this._showArchived
				}, filter);

				var params = {
					fields: fields,
					data: query,
					sortBy: this._sortBy
				};

				var promise = this._request('export').post(params);

				promise.success = function(fn){
					promise.then(function(response){
						var res = response.data,
							records = res.data;
						fn(records);
					});
					return promise;
				};
				promise.error = function(fn) {
					promise.then(null, fn);
					return promise;
				};
				return promise;
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
