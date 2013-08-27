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
	
	var params = $scope._viewParams;
	
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
			setTimeout(function(){
				model.$setValidity('valid', nested.isValid());
				if (scope.setValidity) {
					scope.setValidity('valid', nested.isValid());
				}
				scope.$apply();
			});
		}
		
		function configure(nested) {
			
			//FIX: select on M2O doesn't apply to nested editor
			scope.$watch(attrs.ngModel + '.id', function(){
				setTimeout(function(){
					nested.$apply();
				});
			});

			nested.$watch('form.$valid', function(valid){
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
			model.$setViewValue(null);
			model.$render();
		};

		scope.onClose = function() {
			element.hide();
		};

		attrs.$observe('title', function(title){
			scope.title = title;
		});
		
		model.$render = function() {
			var nested = scope.nested,
				promise = nested._viewPromise,
				value = model.$viewValue;

			if (nested == null)
				return;
			
			if (!configured) {
				configured = true;
				promise.then(function(){
					configure(nested);
				});
			}
			if (value == null || !value.id || value.$dirty) {
				return nested.edit(value);
			}
			
			promise.then(function(){
				nested.doRead(value.id).success(function(record){
					updateFlag = false;
					nested.edit(record);
				});
			});
		};
	},
	template:
	'<fieldset class="form-item-group bordered-box">'+
		'<legend>'+
			'<span ng-bind-html-unsafe="title"></span> '+
			'<span class="legend-toolbar" ng-show="!isReadonly()">'+
				'<a href="" tabindex="-1" ng-click="onClear()" title="{{\'Clear\' | t}}"><i class="icon-ban-circle"></i></a> '+
				'<a href="" tabindex="-1" ng-click="onSelect()" title="{{\'Select\' | t}}"><i class="icon-search"></i></a> '+
				'<a href="" tabindex="-1" ng-click="onClose()" title="{{\'Close\' | t}}"><i class="icon-remove-sign"></i></a>'+
			'</span>'+
		'</legend>'+
		'<div ui-nested-form></div>'+
	'</fieldset>'
};

ui.formDirective('uiNestedEditor', NestedEditor);
ui.formDirective('uiEmbeddedEditor', EmbeddedEditor);
ui.formDirective('uiNestedForm', NestedForm);
	
}).call(this);