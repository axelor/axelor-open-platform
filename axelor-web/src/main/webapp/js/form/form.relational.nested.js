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

var ui = angular.module("axelor.ui");

var NestedForm = {
	scope: true,
	controller: [ '$scope', '$element', function($scope, $element) {
		
		FormViewCtrl.call(this, $scope, $element);
		
		$scope.onShow = function(viewPromise) {
			
		};
		
		$scope.$$forceWatch = false;
		$scope.$$forceCounter = false;

		$scope.$setForceWatch = function () {
			$scope.$$forceWatch = true;
			$scope.$$forceCounter = true;
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

	params.views = _.compact([params.summaryView || params.summaryViewDefault]);
	$scope._viewParams = params;

	ViewCtrl($scope, DataSource, ViewService);
	FormViewCtrl.call(this, $scope, $element);

	$scope.visible = false;
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
		$scope.visible = false;
		$element.hide();
		$element.data('$rel').show();
	};
	
	$scope.edit = function(record) {
		doEdit(record);
		$scope.setEditable(!$scope.$parent.$$readonly);
	};

	$scope.onClose = function() {
		$scope.onClear();
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
	
	$scope.onAdd = function() {
		if (!$scope.isValid() || !$scope.record) {
			return;
		}
		
		var record = $scope.record;
		record.id = null;
		record.version = null;
		record.$version = null;
		
		$scope.onClear();
		
		function doSelect(rec) {
			if (rec) {
				$scope.select(rec);
			}
			return doEdit(rec);
		}
		
		if (!$scope.editorCanSave) {
			return doSelect(record);
		}
		
		$scope.onSave().then(function (rec) {
			doSelect(rec);
		});
	};
	
	$scope.onClear = function() {
		if ($scope.$parent.selection) {
			$scope.$parent.selection.length = 0;
		}
		doEdit(null);
	};
	
	$scope.canUpdate = function () {
		return $scope.record && $scope.record.id;
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
		'<fieldset class="form-item-group bordered-box" ui-show="visible">'+
			'<div ui-view-form x-handler="this"></div>'+
			'<div class="btn-toolbar pull-right">'+
				'<button type="button" class="btn btn btn-info" ng-click="onClose()" ng-show="isReadonly()"><span x-translate>Back</span></button> '+
				'<button type="button" class="btn btn-primary" ng-click="onOK()" ng-show="!isReadonly() && canUpdate()"><span x-translate>OK</span></button>'+
				'<button type="button" class="btn btn-primary" ng-click="onAdd()" ng-show="!isReadonly() && !canUpdate()"><span x-translate>Add</span></button> '+
				'<button type="button" class="btn btn-danger" ng-click="onClose()" ng-show="!isReadonly()"><span x-translate>Cancel</span></button> '+
			'</div>'+
		'</fieldset>'
};

ui.NestedEditorCtrl = NestedEditorCtrl;
ui.NestedEditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function NestedEditorCtrl($scope, $element, DataSource, ViewService) {

	var params = angular.copy($scope._viewParams);

	params.views = _.compact([params.summaryView || params.summaryViewDefault]);
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
		
		var configured = false;
		
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

			var parentAttrs = scope.$parent.field || {};
			if (parentAttrs.forceWatch) {
				nested.$$forceWatch = true;
			}
		}

		var unwatch = null;
		var original = null;

		function nestedEdit(record, fireOnLoad) {

			var nested = scope.nested;
			var counter = 0;

			if (!nested) return;
			if (unwatch) unwatch();

			original = angular.copy(record);

			unwatch = nested.$watch('record', function(rec, old) {
				
				if (counter++ === 0 && !nested.$$forceCounter) {
					return;
				}

				var ds = nested._dataSource;
				var name = scope.field.name;
				var orig = (scope.$$original||{})[name];

				if (_.isEmpty(rec)) rec = null;
				if (_.isEmpty(old)) old = null;

				if (rec) {
					rec.$dirty = !ds.equals(rec, original);
					model.$setViewValue(rec.$dirty ? rec : orig);
				}
				setValidity(nested, nested.isValid());
			}, true);

			return nested.edit(record, fireOnLoad);
		}
		
		scope.ngModel = model;
		scope.visible = false;
		
		scope.onClear = function() {
			scope.$parent.setValue(null, true);
			scope.$parent.$broadcast('on:new');
		};

		scope.onClose = function() {
			scope.$parent._isNestedOpen = false;
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
					return nestedEdit(value, false);
				}
				if (value.$fetched && (nested.record||{}).$fetched) return;
				return nested.doRead(value.id).success(function(record){
					record.$fetched = true;
					value.$fetched = true;
					return nestedEdit(record);
				});
			});
		};
	},
	template:
	'<fieldset class="form-item-group bordered-box" ui-show="visible">'+
		'<legend>'+
			'<span ng-bind-html-unsafe="title"></span> '+
			'<span class="legend-toolbar" style="display: none;" ng-show="!isReadonly()">'+
				'<a href="" tabindex="-1" ng-click="onClear()" title="{{\'Clear\' | t}}" ng-show="canShowIcon(\'clear\')"><i class="fa fa-ban"></i></a> '+
				'<a href="" tabindex="-1" ng-click="onSelect()" title="{{\'Select\' | t}}" ng-show="canShowIcon(\'select\')"><i class="fa fa-search"></i></a> '+
				'<a href="" tabindex="-1" ng-click="onClose()" title="{{\'Close\' | t}}" ng-show="canClose()"><i class="fa fa-times-circle"></i></a>'+
			'</span>'+
		'</legend>'+
		'<div ui-nested-form></div>'+
	'</fieldset>'
};

ui.formDirective('uiNestedEditor', NestedEditor);
ui.formDirective('uiEmbeddedEditor', EmbeddedEditor);
ui.formDirective('uiNestedForm', NestedForm);
	
}).call(this);