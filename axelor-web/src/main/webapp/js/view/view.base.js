/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
ViewCtrl.$inject = ['$scope', 'DataSource', 'ViewService'];
function ViewCtrl($scope, DataSource, ViewService) {

	if ($scope._viewParams == null) {
		$scope._viewParams = $scope.selectedTab;
	}

	if ($scope._viewParams == null) {
		throw "View parameters are not provided.";
	}

	var params = $scope._viewParams;

	$scope._views = ViewService.accept(params);
	$scope._viewType = params.viewType;

	$scope._model = params.model;
	$scope._fields = {};

	$scope._dataSource = null;
	$scope._domain = params.domain;
	$scope._context = params.context;
	
	if (params.model) {
		$scope._dataSource = DataSource.create(params.model, params);
	}
	
	$scope._defer = function() {
		return ViewService.defer();
	};

	$scope.loadView = function(viewType, viewName) {
		var view = $scope._views[viewType];
		if (view == null) {
			view = {
				type: viewType,
				name: viewName
			};
		}
		return ViewService.getMetaDef($scope._model, view);
	};
	
	$scope.updateRoute = function() {
		this.$emit("on:update-route");
	};

	$scope.getRouteOptions = function() {
		throw "Not Implemented.";
	};
	
	$scope.setRouteOptions = function(options) {
		throw "Not Implemented.";
	};

	$scope.switchTo = function(viewType, /* optional */ callback) {

		var view = $scope._views[viewType];
		if (view == null) {
			return;
		}
		
		var promise = view.deferred.promise;
		promise.then(function(viewScope){

			if (viewScope == null) {
				return;
			}

			$scope._viewType = viewType;
			$scope._viewParams.viewType = viewType; //XXX: remove
			$scope._viewParams.$viewScope = viewScope;
			
			viewScope.show();
			
			if (viewScope.updateRoute) {
				viewScope.updateRoute();
			}

			if (callback) {
				callback(viewScope);
			}
		});
	};
	
	if (!params.action) {
		return;
	}

	// show single or default record if specified
	var context = params.context || {};
	if (context._showSingle || context._showRecord) {
		var ds = DataSource.create(params.model, params);
		
		function doEdit(id, readonly) {
			$scope.switchTo('form', function(scope){
				scope._viewPromise.then(function(){
					scope.doRead(id).success(function(record){
						scope.edit(record);
						if (readonly) scope.setEditable(false);
					});
				});
			});
		}
		
		if (context._showRecord > 0) {
			return doEdit(context._showRecord);
		}

		return ds.search({
			offset: 0,
			limit: 2,
			fields: ["id"]
		}).success(function(records, page){
			if (page.total === 1 && records.length === 1) {
				return doEdit(records[0].id, true);
			}
			return $scope.switchTo($scope._viewType || 'grid');
		});
	}
	
	$scope.tbTitleHide = __appSettings['application.view.toolbar-title'] === 'hide';

	// switch to the the current viewType
	$scope.switchTo($scope._viewType || 'grid');
}

/**
 * Base controller for DataSource views. This controller should not be used
 * directly but actual controller should inherit from it.
 * 
 */
function DSViewCtrl(type, $scope, $element) {

	if (type == null) {
		throw "No view type provided.";
	}
	if ($scope._dataSource == null) {
		throw "DataSource is not provided.";
	}
	
	$scope._viewResolver = $scope._defer();
	$scope._viewPromise = $scope._viewResolver.promise;
	
	var ds = $scope._dataSource;
	var view = $scope._views[type] || {};
	var viewPromise = null;
	var hiddenButtons = {};

	$scope.fields = {};
	$scope.schema = null;
	
	setTimeout(function(){
		$scope.$apply(function(){
			if (view.deferred)
				view.deferred.resolve($scope);
		});
	});

	$scope.show = function() {
		if (viewPromise == null) {
			viewPromise = $scope.loadView(type, view.name);
			viewPromise.success(function(fields, schema){
				var toolbar = [];
				_.each(schema.toolbar, function(button){
					button.custom = true;
					if (/^(new|edit|save|delete|copy|cancel|back|refresh|search|export|log|files)$/.test(button.name)) {
						hiddenButtons[button.name] = button;
						button.custom = false;
					}
					if (!button.title) {
						button.title = button.name;
					}
					toolbar.push(button);
				});

				$scope.fields = fields;
				$scope.schema = schema;
				$scope.toolbar = toolbar;
				$scope.menubar = schema.menubar;
			});
		}
		
		$scope.onShow(viewPromise);
	};
	
	$scope.onShow = function(promise) {
		
	};

	$scope.canNext = function() {
		return ds && ds.canNext();
	};

	$scope.canPrev = function() {
		return ds && ds.canPrev();
	};
	
	$scope.getPageSize = function() {
		var page = ds && ds._page;
		if (page) {
			return page.limit;
		}
		return 40;
	};

	$scope.setPageSize = function(value) {
		var page = ds && ds._page,
			limit = Math.max(0, +value) || 40;
		if (page && page.limit != limit) {
			page.limit = limit;
			$scope.onRefresh();
		}
	};

	$scope.hasButton = function(name) {
		if ((name == "new" || name == "copy") && !this.hasPermission("create")) {
			return false;
		}
		if (name == "save" && !this.hasPermission("write")) {
			return false;
		}
		if (name == "delete" && !this.hasPermission("delete")) {
			return false;
		}
		if (_(hiddenButtons).has(name)) {
			var button = hiddenButtons[name];
			if (button.isHidden) {
				return !button.isHidden();
			}
			return !button.hidden;
		}
		return true;
	};
	
	$scope.hasPermission = function(perm) {
		var view = $scope.schema;
		if (!view || !view.perms) return true;
		var perms = view.perms;
		var permitted = perms[perm];
		if (!permitted) {
			return false;
		}
		return true;
	};

	$scope.isPermitted = function(perm, record, callback) {
		var ds = this._dataSource;
		ds.isPermitted(perm, record).success(function(res){
			var errors = res.errors;
			if (errors) {
				return axelor.dialogs.error(errors.read);
			}
			callback();
		});
	};
	
	$scope.canShowToolbar = function() {
		var params = ($scope._viewParams || {}).params;
		if (params && params['show-toolbar'] === false) {
			return false;
		}
		return true;
	};
}

angular.module('axelor.ui').directive('uiRecordPager', function(){

	return {
		replace: true,
		link: function(scope, element, attrs) {
			
			var elText = element.find('.record-pager-text').show(),
				elChanger = element.find('.record-pager-change').hide(),
				elInput = elChanger.find('input');
			
			elText.click(function(e) {
				elText.add(elChanger).toggle();
			});

			elChanger.on('click', 'button',  function() {
				elText.add(elChanger).toggle();
				if (scope.setPageSize) {
					scope.setPageSize(elInput.val());
				}
			});
		},
		template:
		'<div class="record-pager">'+
	    '<span>'+
	    	'<span class="record-pager-text">{{pagerText()}}</span>'+
			'<span class="input-append record-pager-change">'+
				'<input type="text" style="width: 30px;" value="{{getPageSize()}}">'+
				'<button type="button" class="btn add-on"><i class="icon icon-ok"></i></button>'+
			'</span>'+
	    '</span>'+
	    '<div class="btn-group">'+
	    	'<button class="btn" ng-disabled="!canPrev()" ng-click="onPrev()"><i class="icon-chevron-left"></i></button>'+
	    	'<button class="btn" ng-disabled="!canNext()" ng-click="onNext()"><i class="icon-chevron-right"></i></button>'+
	    '</div>'+
	    '</div>'
	};
});

angular.module('axelor.ui').directive('uiViewSwitcher', function(){
	return {
		scope: true,
		link: function(scope, element, attrs) {

			element.find("button").click(function(e){
				var type = $(this).attr("x-view-type"),
					ds = scope._dataSource,
					page = ds && ds._page;

				if (type === "form" && page) {
					if (page.index === -1) page.index = 0;
				}

				if (scope.selectedTab.viewType === 'grid' && scope.selection) {
					page.index = _.first(scope.selection);
				}

				scope.switchTo(type);
				scope.$apply();
			})
			.each(function() {
				if (scope._views[$(this).attr("x-view-type")] === undefined) {
					$(this).hide();
				}
			});
			scope.$watch("_viewType", function(type){
				element.find("button").attr("disabled", false);
				element.find("button[x-view-type=" + type + "]").attr("disabled", true);
			});
		},
		replace: true,
		template:
		'<div class="view-switcher pull-right">'+
		  	'<div class="btn-group">'+
		  		'<button class="btn" x-view-type="grid"		><i class="icon-table"		></i></button>'+
		  		'<button class="btn" x-view-type="calendar"	><i class="icon-calendar"	></i></button>'+
		  		'<button class="btn" x-view-type="chart"	><i class="icon-bar-chart"	></i></button>'+
		  		'<button class="btn" x-view-type="form"		><i class="icon-edit"		></i></button>'+
		    '</div>'+
		'</div>'
	};
});
