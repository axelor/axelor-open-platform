EditorCtrl.$inject = ['$scope', '$element', 'DataSource'];
function EditorCtrl($scope, $element, DataSource) {
	
	$scope._dataSource = DataSource.create($scope._model, {
		domain: $scope._domain,
		context: $scope._context
	});
	
	FormViewCtrl.call(this, $scope, $element);
	
	$scope.onShow = function(viewPromise) {
		
		viewPromise.then(function(){
			$element.dialog('open');
		});
	};
	
	$scope.$popup = $scope;

	var ds = $scope._dataSource;
	var originalEdit = $scope.edit;

	function doEdit(record) {
		if (record && record.id > 0 && !record.$fetched) {
			$scope.doRead(record.id).success(function(record){
				originalEdit(record);
			});
		} else {
			originalEdit(record);
		}
		canClose = false;
	};

	$scope.edit = function(record) {
		$scope._viewPromise.then(function(){
			doEdit(record);
		});
	};

	var canClose = false;
	
	$scope.onOK = function() {
		
		if (!$scope.isValid())
			return;

		var record = $scope.record,
			events = $scope._$events,
			saveAction = events.onSave;

		function close(value) {
			if (value) {
				value.$fetched = true;
				$scope.select(value);
			}
			canClose = true;
			$element.dialog('close');
		};
		
		function doSave() {
			ds.save(record).success(function(record, page){
				setTimeout(function(){
					close(record);
				});
			});
		}
		
		if ($scope.editorCanSave && $scope.isDirty()) {
			if (record.id < 0)
				record.id = null;
			if (saveAction) {
				saveAction().then(doSave);
			} else {
				doSave();
			}
		} else {
			close(record);
		}
	};
	
	$scope.onBeforeClose = function(event, ui) {

		if (canClose || !$scope.isDirty()) {
			$scope.record = null;
			return true;
		}
		
		event.preventDefault();
		
		$scope.confirmDirty(function(){
			canClose = true;
			$element.dialog('close');
		});
	};
}

SelectorCtrl.$inject = ['$scope', '$element', 'DataSource'];
function SelectorCtrl($scope, $element, DataSource) {
	
	$scope._dataSource = DataSource.create($scope._model, {
		domain: $scope._domain,
		context: $scope._context
	});
	
	GridViewCtrl.call(this, $scope, $element);

	function doFilter() {
		$scope.filter($scope.getDomain());
	}
	
	var initialized = false;
	var origShow = $scope.show;
	$scope.show = function() {
		origShow();
		if (initialized) {
			doFilter();
		}
	};
	
	$scope.onItemClick = function(e, args) {
		setTimeout(function(){
			$scope.$apply($scope.onOK);
		});
	};
	
	var origOnShow = $scope.onShow;
	$scope.onShow = function(viewPromise) {
		
		viewPromise.then(function(){
			$element.dialog('open');
			initialized = true;
			origOnShow(viewPromise);
		});
	};
	
	$scope.onOK = function() {
		
		var selection = _.map($scope.selection, function(index){
			return $scope._dataSource.at(index);
		});

		$scope.select(selection);
		$element.dialog('close');
	};
}

angular.module('axelor.ui').directive('uiEditorPopup', function(){
	
	return {
		restrict: 'EA',
		controller: EditorCtrl,
		link: function(scope, element, attrs) {
			
			var initialized = false;
			
			function adjustSize() {
				element.find(':input:first').focus();
				$.event.trigger('adjustSize');
				
				if (initialized)
					return;
				
				initialized = true;
				autoSize();
			}

			function autoSize() {
				var elem = element.find('table:first'),
					maxWidth = $(document).width() - 8,
					maxHeight = $(document).height() - 8,
					width = elem.outerWidth(true) + 32,
					height = elem.outerHeight(true) + 16;

				height += element.parent().children('.ui-dialog-titlebar').outerHeight(true);
				height += element.parent().children('.ui-dialog-buttonpane').outerHeight(true);
				
				width = Math.min(maxWidth, width) || 'auto';
				height = Math.min(maxHeight, height) || 'auto';
				
				element.dialog('option', 'width', width);
				element.dialog('option', 'height', height);
				
				element.closest('.ui-dialog').position({
			      my: "center",
			      at: "center",
			      of: window
			    });
			}

			scope.onOpen = function(e, ui) {
				
				scope._viewPromise.then(function(){
					setTimeout(function(){
						adjustSize();
					});
				});
			};
			
			scope.$watch('schema.title', function(title){
				if (title == null)
					return;
				element.closest('.ui-dialog').find('.ui-dialog-title').text(title);
			});
			scope.$on('adjust:dialog', _.throttle(autoSize, 300));
		},
		replace: true,
		template:
		'<div ui-dialog x-on-open="onOpen" x-on-ok="onOK" x-on-before-close="onBeforeClose">'+
		    '<div ui-view-form x-handler="this"></div>'+
		'</div>'
	};
});

angular.module('axelor.ui').directive('uiSelectorPopup', function(){
	
	return {
		restrict: 'EA',
		controller: SelectorCtrl,
		link: function(scope, element, attrs) {
			
			var initialized = false;
			scope.onOpen = function(e, ui) {

				setTimeout(function(){
					
					element.find('input[type=text]:first').focus();
					$.event.trigger('adjustSize');
					
					//XXX: ui-dialog issue
					var zIndex = element.zIndex();
					element.find('.slick-headerrow-column').zIndex(zIndex);

					if (initialized)
						return;

					var width = $(window).width();
					var height = $(window).height();
					
					element.dialog('option', 'width', (70 * width / 100));
					element.dialog('option', 'height', (70 * height / 100));
					
					element.closest('.ui-dialog').position({
				      my: "center",
				      at: "center",
				      of: window
				    });
					
					initialized = true;
				});
			};
			
			scope.$watch('schema.title', function(title){
				if (title == null)
					return;
				element.closest('.ui-dialog').find('.ui-dialog-title').text(title);
			});
			
			setTimeout(function(){
				var footer = element.closest('.ui-dialog').find('.ui-dialog-buttonpane'),
				pager = element.find('.record-pager');

				footer.prepend(pager);
			});
		},
		replace: true,
		template:
		'<div ui-dialog x-on-open="onOpen" x-on-ok="onOK">'+
		    '<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-selector="true"></div>'+
		    '<div class="record-pager pull-left">'+
			    '<div class="btn-group">'+
			      '<button class="btn" ng-disabled="!canPrev()" ng-click="onPrev()"><i class="icon-chevron-left"></i></button>'+
			      '<button class="btn" ng-disabled="!canNext()" ng-click="onNext()"><i class="icon-chevron-right"></i></button>'+
			    '</div>'+
			    '<span>{{pagerText()}}</span>'+
		    '</div>'+
		'</div>'
	};
});
