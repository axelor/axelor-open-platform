/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
(function(){

var ui = angular.module('axelor.ui');

this.FormListCtrl = FormListCtrl;
this.FormListCtrl.$inject = ["$scope", "$element", "$compile", "DataSource", "ViewService"];

function FormListCtrl($scope, $element, $compile, DataSource, ViewService) {
	
	DSViewCtrl('form', $scope, $element);

	$scope._viewParams.$viewScope = $scope;
	
	var view = $scope._views['trail'];
	$scope.applyLater(function(){
		if (view.deferred)
			view.deferred.resolve($scope);
	}, 0);
	
	var ds = $scope._dataSource;
	var dsChild = DataSource.create(ds._model, {
		
	});
	
	var params = $scope._viewParams.params || {};
	var sortBy = params['trail-order'] && params['trail-order'].split(/\s*,\s*/);
	
	var parentField = params['trail-parent-field'] || 'parent';
	var childrenField = params['trail-children-field'] || 'children';

	var initialized = false;
	
	$scope.onShow = function(viewPromise) {
		if (initialized) {
			return;
		}
		initialized = true;
		viewPromise.then(function() {
			$scope.updateRoute();
			if ($scope.records === undefined) {
				$scope.onReload();
			}
		});
	};

	$scope.getRouteOptions = function() {
		return {
			mode: "trail"
		};
	};
	
	$scope.setRouteOptions = function(options) {
		$scope.updateRoute();
	};
	
	$scope.canCreate = function() {
		return this._canCreate;
	};
	
	$scope.canExpand = function(item) {
		return !item.$expanded && hasChildren(item);
	};

	$scope.onNew = function() {
		this._canCreate = true;
		$scope.$broadcast("trail:on-new");
	};
	
	$scope.onCancel = function() {
		this._canCreate = false;
	};
	
	$scope.onReload = function() {
		this._canCreate = false;
		return ds.search({
			sortBy: sortBy
		}).success(function(records, page) {
			$scope.records = records;
		});
	};
	
	function hasChildren(item) {
		return !_.isEmpty(findChildren(item));
	}
	
	function hasParent(item) {
		return !_.isEmpty(findParent(item));
	}

	function findChildren(item) {
		return item[childrenField];
	}
	
	function findParent(item) {
		return item[parentField];
	}
	
	$scope.onExpand = function(item) {
		
		var record = _.isNumber(item) ? _.find($scope.records, function(rec) { return item === rec.id; }) : item;
		var current = record.$children || [];
		var children = findChildren(record);
		
		var ids = _.pluck(children, 'id');
		var criterion = {
			'fieldName': 'id',
			'operator': 'in',
			'value': ids
		};

		var filter = {
			operator: 'and',
			criteria: [criterion]
		};

		return dsChild.search({
			filter: filter,
			archived: true,
			limit: -1
		}).success(function(records, page) {
			current = current.concat(records);
			record.$children = current;
			record.$expanded = true;
		});
	};

	$scope.onRecord = function(record) {
		this._canCreate = false;
		var found = _.find($scope.records, function(item) {
			return item.id == record.id;
		});
		
		if (found) {
			return _.extend(found, record);
		}
		
		if (!hasParent(record)) {
			return $scope.records.unshift(record);
		}

		var parent = findParent(record);

		parent = _.find($scope.records, function(item) {
			return item.id == parent.id;
		});

		parent.$children = parent.$children || [];
		parent.$children.push(record);
	};
	
	$scope.show();
}

ui.directive('uiFormList', function() {

	return {
		
		controller: FormListCtrl,
		
		link: function(scope, element, attrs) {

		},
		
		template: '<div class="trail-list">'+
			'<div ng-repeat="item in records" ui-trail-form></div>'+
		'</div>'
	};
});

TrailFormCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function TrailFormCtrl($scope, $element, DataSource, ViewService) {
	
	var params = $scope.$parent._viewParams;
	var view = _.find(params.views, function(view) {
		return view.type === 'form';
	});
	
	if ($scope._editForm) {
		view = {
			type: view.type,
			name: $scope._editForm
		};
	}

	params = _.extend({}, {
		'title': params.title,
		'model': params.model,
		'domain': params.domain,
		'context': params.context,
		'viewType': 'form',
		'views': [view],
		'params': params.params
	});

	$scope._viewParams = params;
	
	ViewCtrl.call(this, $scope, DataSource, ViewService);
	FormViewCtrl.call(this, $scope, $element);
	
	// trail forms are by default editable
	$scope.setEditable(true);
	
	$scope.updateRoute = function(options) {
	};
	
	$scope.getRouteOptions = function() {
	};

	$scope.setRouteOptions = function(options) {
	};
	
	$scope.confirmDirty = function(fn) {
		return fn();
	};
	
	$scope.onShow = function(viewPromise) {
		viewPromise.then(function() {
			var record = $scope.$parent.item;
			if (record) {
				record.__expandable = $scope.$parent.canExpand(record);
			}
			$scope.edit(record);
		});
	};
	
	$scope.$on("on:new", function() {
		var trailScope = $scope.$parent.$parent;
		if (trailScope) {
			trailScope.onReload();
		}
	});
	
	$scope.$on("trail:record", function(e, record) {
		var parent = $scope.$parent || {};
		if (parent.onRecord) {
			parent.onRecord(record);
		}
	});

	$scope.$on("trail:expand", function(e, id) {
		var parent = $scope.$parent || {};
		if (parent.onExpand) {
			parent.onExpand(id).then(function() {
				$scope.record.__expandable = false;
			});
		}
	});

	$scope.show();
}

function trailWidth(scope, element) {
	var params = scope._viewParams.params || {};
	var width = params['trail-width'];
	if (width) {
		element.width(width);
	}
}

ui.directive("uiTrailForm", function() {

	return {
		scope: {},
		replace: true,
		controller: TrailFormCtrl,
		link: function(scope, element, attrs) {
			trailWidth(scope, element);
		},
		template:
		'<div ui-view-form x-handler="this" class="trail-form"></div>'
	};
});

TrailEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function TrailEditorCtrl($scope, $element, DataSource, ViewService) {
	
	var params = _.clone($scope.$parent._viewParams);
	if (params.params && params.params['trail-edit-form']) {
		$scope._editForm = params.params['trail-edit-form'];
	}
	
	$scope.$on("trail:on-new", function(e) {
		$scope.edit(null);
	});
	
	TrailFormCtrl.apply(this, arguments);
}

ui.directive("uiTrailEditor", function() {

	return {
		scope: {},
		controller: TrailEditorCtrl,
		link: function(scope, element, attrs) {
			trailWidth(scope, element);
		},
		template:
		'<div ui-view-form x-handler="this" class="trail-editor"></div>'
	};
});

}).call(this);
