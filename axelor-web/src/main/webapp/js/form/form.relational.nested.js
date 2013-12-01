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
(function() {

var ui = angular.module("axelor.ui");

var NestedForm = {
	scope: true,
	controller: [ '$scope', '$element', function($scope, $element) {
		
		FormViewCtrl.call(this, $scope, $element);
		
		$scope.onShow = function(viewPromise) {
			
		};
		
		$scope.registerNested($scope);
		$scope.show();
	}],
	link: function(scope, element, attrs, ctrl) {

	},
	template: '<div ui-view-form x-handler="this"></div>'
};

ui.EmbeddedEditorCtrl = EmbeddedEditorCtrl;
ui.EmbeddedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function EmbeddedEditorCtrl($scope, $element, DataSource, ViewService) {
	
	var params = angular.copy($scope._viewParams);
	
	params.views = _.compact([params.summaryView]);
	$scope._viewParams = params;

	ViewCtrl($scope, DataSource, ViewService);
	FormViewCtrl.call(this, $scope, $element);

	$scope.onShow = function() {
		
	};
	
	var originalEdit = $scope.edit;
	
	function doEdit(record) {
		if (record && record.id > 0 && !record.$fetched) {
			$scope.doRead(record.id).success(function(record){
				originalEdit(record);
			});
		} else {
			originalEdit(record);
		}
	};
	
	function doClose() {
		if ($scope.isDetailView) {
			$scope.edit($scope.getSelectedRecord());
			return;
		}
		$element.hide();
		$element.data('$rel').show();
	};
	
	$scope.edit = function(record) {
		doEdit(record);
	};

	$scope.onClose = function() {
		doClose();
	};
	
	$scope.onOK = function() {
		if (!$scope.isValid()) {
			return;
		}
		var record = $scope.record;
		if (record) record.$fetched = true;
		$scope.select(record);
		setTimeout(doClose);
	};
	
	$scope.onClear = function() {
		$scope.record = {};
	};
	
	$scope.$on('grid:changed', function(event) {
		var record = $scope.getSelectedRecord();
		if ($scope.isDetailView) {
			$scope.edit(record);
		}
	});
	
	$scope.$parent.$watch('isReadonly()', function(readonly, old) {
		if (readonly === old) return;
		$scope.setEditable(!readonly);
	});

	$scope.show();
}

var EmbeddedEditor = {
	restrict: 'EA',
	css: 'nested-editor',
	scope: true,
	controller: EmbeddedEditorCtrl,
	template:
		'<fieldset class="form-item-group bordered-box">'+
			'<div ui-view-form x-handler="this"></div>'+
			'<div class="btn-toolbar pull-right">'+
				'<button type="button" class="btn btn btn-info" ng-click="onClose()" ng-show="isReadonly()"><span x-translate>Back</span></button> '+
				'<button type="button" class="btn btn-danger" ng-click="onClose()" ng-show="!isReadonly()"><span x-translate>Cancel</span></button> '+
				'<button type="button" class="btn btn-primary" ng-click="onOK()" ng-show="!isReadonly()"><span x-translate>OK</span></button>'+
			'</div>'+
		'</fieldset>'
};

ui.NestedEditorCtrl = NestedEditorCtrl;
ui.NestedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function NestedEditorCtrl($scope, $element, DataSource, ViewService) {

	var params = angular.copy($scope._viewParams);

	params.views = _.compact([params.summaryView]);
	$scope._viewParams = params;

	ui.ManyToOneCtrl.call(this, $scope, $element, DataSource, ViewService);
	
	$scope.nested = null;
	$scope.registerNested = function(scope) {
		$scope.nested = scope;
		
		$scope.$watch("isReadonly()", function(readonly) {
			scope.setEditable(!readonly);
		});
	};
}

var NestedEditor = {
	restrict: 'EA',
	css: 'nested-editor',
	require: '?ngModel',
	scope: true,
	controller: NestedEditorCtrl,
	link: function(scope, element, attrs, model) {
		
		var configured = false,
			updateFlag = true;
		
		function setValidity(nested, valid) {
			model.$setValidity('valid', nested.isValid());
			if (scope.setValidity) {
				scope.setValidity('valid', nested.isValid());
			}
		}
		
		function configure(nested) {
			
			//FIX: select on M2O doesn't apply to nested editor
			var valueSet = false;
			scope.$watch(attrs.ngModel + '.id', function(id, old){
				if (id === old && valueSet) return;
				valueSet = true;
				scope.applyLater();
			});
			
			//FIX: accept values updated with actions
			scope.$watch(attrs.ngModel + '.$updatedValues', function(value) {
				if (!nested || !value) return;
				var record = nested.record || {};
				if (record.id === value.id) {
					_.extend(record, value);
				}
			});

			var validitySet = false;
			nested.$watch('form.$valid', function(valid, old){
				if (valid === old && validitySet) {
					return;
				}
				validitySet = true;
				setValidity(nested, valid);
			});
			nested.$watch('record', function(rec, old){
				if (updateFlag && rec != old) {
					if (_.isEmpty(rec)) {
						rec = null;
					} else {
						rec.$dirty = true;
					}
					if (rec) {
						model.$setViewValue(rec);
					}
				}
				updateFlag = true;
				setValidity(nested, nested.isValid());
			}, true);
		}
		
		scope.ngModel = model;
		
		scope.onClear = function() {
			scope.$parent.setValue(null, true);
			scope.$parent.$broadcast('on:new');
		};

		scope.onClose = function() {
			scope.$parent.__nestedOpen = false;
			scope.visible = false;
			element.hide();
		};

		scope.canClose = function() {
			return scope.canToggle() && scope.canSelect();
		};

		attrs.$observe('title', function(title){
			scope.title = title;
		});
		
		model.$render = function() {
			var nested = scope.nested,
				promise = nested._viewPromise,
				oldValue = model.$viewValue;

			if (nested == null)
				return;
			
			if (!configured) {
				configured = true;
				promise.then(function(){
					configure(nested);
				});
			}
			
			promise.then(function() {
				var value = model.$viewValue;
				if (oldValue !== value) { // prevent unnecessary onLoad
					return;
				}
				if (!value || !value.id || value.$dirty) {
					return nested.edit(value);
				}
				return nested.doRead(value.id).success(function(record){
					updateFlag = false;
					return nested.edit(record);
				});
			});
		};
	},
	template:
	'<fieldset class="form-item-group bordered-box" ui-show="visible">'+
		'<legend>'+
			'<span ng-bind-html-unsafe="title"></span> '+
			'<span class="legend-toolbar" ng-show="!isReadonly()">'+
				'<a href="" tabindex="-1" ng-click="onClear()" title="{{\'Clear\' | t}}"><i class="icon-ban-circle"></i></a> '+
				'<a href="" tabindex="-1" ng-click="onSelect()" title="{{\'Select\' | t}}"><i class="icon-search"></i></a> '+
				'<a href="" tabindex="-1" ng-click="onClose()" title="{{\'Close\' | t}}" ng-show="canClose()"><i class="icon-remove-sign"></i></a>'+
			'</span>'+
		'</legend>'+
		'<div ui-nested-form></div>'+
	'</fieldset>'
};

ui.formDirective('uiNestedEditor', NestedEditor);
ui.formDirective('uiEmbeddedEditor', EmbeddedEditor);
ui.formDirective('uiNestedForm', NestedForm);
	
}).call(this);