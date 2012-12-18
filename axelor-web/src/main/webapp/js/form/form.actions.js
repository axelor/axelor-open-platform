(function(){
	
var ui = angular.module('axelor.ui');

var equals = angular.equals,
	forEach = angular.forEach,
	isArray = angular.isArray,
	isObject = angular.isObject,
	isDate = angular.isDate;

function updateValues(source, target) {
	if (equals(source, target))
		return;
	forEach(source, function(value, key) {
		if (isArray(value) || isDate(value))
			return target[key] = value;
		if (isObject(value)) {
			var dest = target[key];
			if (dest) {
				dest = _.extend({}, dest);
				updateValues(value, dest);
				return target[key] = dest;
			}
			return target[key] = value;
		}
		return target[key] = value;
	});
}

function handleError(scope, item, message) {
	
	if (item == null) {
		return;
	}

	var ctrl = item.data('$ngModelController');
	if (ctrl == null || ctrl.$doReset) {
		return;
	}

	var e = $('<span class="error"></span>').text(message);
	var p = item.parent('td.form-item');

	if (item.is(':input,.input-append')) {
		p.append(e);
	} else {
		p.prepend(e);
	}

	var clear = scope.$on('on:edit', function(){
		ctrl.$doReset();
	});
	
	function cleanUp(items) {
		var idx = items.indexOf(ctrl.$doReset);
		if (idx > -1) {
			items.splice(idx, 1);
		}
	}
	
	ctrl.$doReset = function(value) {
		
		cleanUp(ctrl.$viewChangeListeners);
		cleanUp(ctrl.$formatters);
		
		ctrl.$setValidity('invalid', true);
		ctrl.$doReset = null;
		
		e.remove();
		clear();
		
		return value;
	};
	
	ctrl.$setValidity('invalid', false);
	ctrl.$viewChangeListeners.push(ctrl.$doReset);
	ctrl.$formatters.push(ctrl.$doReset);
}

function ActionHandler($scope, ViewService, options) {

	if (options == null || !options.action)
		throw 'No action provided.';

	this.canSave = options.canSave;
	this.prompt = options.prompt;
	this.action = options.action;
	this.element = options.element || $();

	this.scope = $scope;
	this.ws = ViewService;
}

ActionHandler.prototype = {
	
	constructor: ActionHandler,
	
	onLoad : function() {
		return this._handle();
	},
	
	onNew: function() {
		return this._handle();
	},
	
	onSave: function() {
		return this._handle();
	},
	
	onSelect: function() {
		return this._handle();
	},
	
	onClick: function(event) {
		var self = this;
		if (this.prompt) {
			axelor.dialogs.confirm(this.prompt, function(confirmed){
				if (confirmed)
					self._handle();
			});
		} else
			this._handle();
	},

	onChange: function(event) {
		var element = $(event.target);
		if (element.is('[type="checkbox"],.select-item')) {
			var self = this;
			return setTimeout(function(){
				return self._handle();
			});
		}
		this._onChangePending = true;
	},
	
	onBlur: function(event) {
		if (this._onChangePending) {
			this._onChangePending = false;
			return this._handle();
		}
	},
	
	_handle: function() {
		var actions = _.invoke(this.action.split(','), 'trim'),
			context = this._getContext();
			
		return this._handleActions(actions, context).then(function(){
			//DONE!
		});
	},
	
	_getContext: function() {
		var scope = this.scope,
			context = scope.getContext ? scope.getContext() : scope.record,
			viewParams = scope._viewParams || {};
			
		return _.extend({}, viewParams.context, context);
	},
	
	_handleActions: function(actions, context) {
		
		var self = this,
			scope = self.scope,
			deferred = this.ws.defer(),
			promise = deferred.promise;

		if (!actions || actions.legth === 0 || !actions[0]) {
			setTimeout(function(){
				scope.$apply(function(){
					deferred.resolve();
				});
			});
			return promise;
		}

		var action = actions.shift();

		if (action === 'save') {
			if (!scope.isValid()) {
				deferred.reject();
				return promise;
			}
			if (!scope.isDirty()) {
				return self._handleActions(actions, context);
			}
			var ds = scope._dataSource;
			promise = ds.save(scope.record);
			promise = promise.success(function(rec, page) {
				scope.editRecord(rec);
				return self._handleActions(actions, context);
			});
			this._invalidateContext = true;
			return promise;
		}
		
		if (this._invalidateContext) {
			context = this._getContext();
			this._invalidateContext = false;
		}
		
		promise = this.ws.action(action, scope._model, context);
		promise = promise.then(function(res){
			var d = self.ws.defer(),
				p = d.promise;

			self._handleResponse(res.data, function(result){
				if (result) {
					d.resolve();
				}
			});
			
			return p.then(function(res){
				return self._handleActions(actions, context);
			});
		});
		
		return promise;
	},

	_handleResponse: function(response, callback) {

		if (response.data == null) {
			return callback(true);
		}

		var data = _.first(response.data),
			scope = this.scope,
			formElement = this.element.parents('form:first');

		if (formElement.length == 0)
			formElement = this.element;

		if(data.flash) {
			//TODO: show embedded message instead
			axelor.dialogs.say(data.flash);
		}

		if (data.error) {
			return axelor.dialogs.error(data.error, function(){
				callback(false);
			});
		}

		if (data.alert) {
			return axelor.dialogs.confirm(data.alert, function(confirmed){
				setTimeout(function(){
					scope.$apply(function(){
						callback(confirmed);
					});
				});
			});
		}
		
		if (!_.isEmpty(data.errors)) {
			_.each(data.errors, function(v, k){
				var item = findItems(k).first();
				handleError(scope, item, v);
			});
			return callback(false);
		}
		
		if (data.values) {
			updateValues(data.values, scope.record, scope);
			this._invalidateContext = true;
		}
		
		if (data.reload) {
			var promise = scope.reload(true);
			this._invalidateContext = true;
			if (promise) {
				return promise.then(function(){
					callback(true);
				}, function(){
					callback(false);
				});
			}
		}
		
		function findItems(name) {

			var items;
			var containers = formElement.parents('.form-view:first')
										.find('.record-toolbar:first')
										.add(formElement);

			// first search by name
			items = containers.find('[name="' + name +'"]');
			if (items.size())
				return items;

			// then search by x-path
			items = containers.find('[x-path="' + name + '"]');
			if (items.size()) {
				return items;
			}
			
			// then search with relative path
			if (scope.formPath) {
				items = containers.find('[x-path="' + scope.formPath + '.' + name + '"]');
				if (items.size()) {
					return items;
				}
			}
		}
		
		function setAttrs(item, itemAttrs) {
			
			var label = item.data('label'),
				itemScope = item.data('$scope'),
				column;

			// handle o2m/m2m columns
			if (item.is('.slick-dummy-column')) {
				column = item.data('column');
				itemScope = item.parents('[x-path]:first').data('$scope');
				forEach(itemAttrs, function(value, attr){
					if (attr == 'hidden')
						itemScope.showColumn(column.id, !value);
					if (attr == 'title')
						setTimeout(function(){
							itemScope.setColumnTitle(column.id, value);
						});
				});
				return;
			}
			
			// handle notebook
			if (item.is('.tab-pane')) {
				var index = item.parent().children('.tab-pane').index(item);
				itemScope = item.parents('.tabbable-tabs:first').data('$scope');
				
				forEach(itemAttrs, function(value, attr){
					if (attr == 'hidden') {
						if (value)
							itemScope.hideTab(index);
						else
							itemScope.showTab(index);
					}
				});
				return;
			}

			forEach(itemAttrs, function(value, attr){
				
				switch(attr) {
				case 'required':
					scope.setRequired(item, value);
					break;
				case 'readonly':
					scope.setReadonly(item, value);
					break;
				case 'hidden':
					scope.setHidden(item, value);
					break;
				case 'collapse':
					if (itemScope.setCollapsed)
						itemScope.setCollapsed(value);
					break;
				case 'title':
					if (label) {
						label.html(value);
					} else if (item.is('label')) {
						item.html(value);
					}
					break;
				case 'color':
					//TODO: set color
				case 'domain':
					if (itemScope.setDomain)
						itemScope.setDomain(value);
					break;
				}
			});
		}

		forEach(data.attrs, function(itemAttrs, itemName) {
			var items = findItems(itemName);
			if (items == null || items.length == 0) {
				return;
			}
			items.each(function() {
				setAttrs($(this), itemAttrs);
			});
		});
		
		if (data.view) {
			var tab = data.view;
			tab.action = _.uniqueId('$act');
			if (!tab.viewType)
				tab.viewType = 'grid';
			if (tab.viewType == 'grid' || tab.viewType == 'form')
				tab.model = tab.model || tab.resource;
			if (!tab.views) {
				tab.views = [{ type: tab.viewType }];
				if (tab.viewType === 'html') {
					angular.extend(tab.views[0], {
						resource: tab.resource,
						title: tab.title
					});
				}
			}
			if (tab.viewType == 'form' || tab.viewType == 'grid') {
				var views = _.groupBy(tab.views, 'type');
				if (!views.grid) tab.views.push({type: 'grid'});
				if (!views.form) tab.views.push({type: 'form'});
			}
			scope.openTab(tab);
		}
		
		callback(true);
	}
};

ui.directive('uiActions', ['ViewService', function(ViewService) {

	return function(scope, element, attrs) {

		var props = scope.getViewDef(element),
			action;

		props = _.isEmpty(props) ? scope.schema : props;
		if (props == null)
			return;
		
		function isRelational(elem) {
			return elem.is('.many2one-item,.one2many-item,.many2many-item');
		}

		action = props.onClick;
		if (action) {
			var handler = new ActionHandler(scope, ViewService, {
				element: element,
				action: action,
				canSave: props.canSave,
				prompt: props.prompt
			});

			element.on('click', _.bind(handler.onClick, handler));
		}
		
		action = props.onChange;
		if (action) {
			var _scope = isRelational(element) ? scope.$parent : scope;
			var handler = new ActionHandler(_scope, ViewService, {
				element: element,
				action: action
			});
			
			if (element.is('.input-append') || !(element.is(':input'))) {
				element.data('$onChange', handler);
			} else {
				var input = element.find(':input:first').andSelf().last();
				input.on('change', _.bind(handler.onChange, handler));
				input.on('blur', _.bind(handler.onBlur, handler));
			}
		}

		action = props.onSelect;
		if (action) {
			var _scope = isRelational(element) ? scope.$parent : scope;
			var handler = new ActionHandler(_scope, ViewService, {
				element: element,
				action: action
			});
			element.data('$onSelect', handler);
		}
		
		action = props.onNew;
		if (action) {
			var handler = new ActionHandler(scope, ViewService, {
				element: element,
				action: action
			});
			scope._$events.onNew = _.bind(handler.onNew, handler);
		}
		
		action = props.onLoad;
		if (action) {
			var handler = new ActionHandler(scope, ViewService, {
				element: element,
				action: action
			});
			scope._$events.onLoad = _.bind(handler.onLoad, handler);
		}
		
		action = props.onSave;
		if (action) {
			var handler = new ActionHandler(scope, ViewService, {
				element: element,
				action: action
			});
			scope._$events.onSave = _.bind(handler.onSave, handler);
		}
	};
}]);

ui.directive('uiToolButton', ['ViewService', function(ViewService) {

	return function(scope, element, attrs) {
		
		var button = scope.$eval(attrs.uiToolButton);
		var handler = new ActionHandler(scope, ViewService, {
			element: element,
			action: button.onClick,
			canSave: button.canSave,
			prompt: button.prompt
		});

		element.on('click', _.bind(handler.onClick, handler));
	};
}]);

}).call(this);
