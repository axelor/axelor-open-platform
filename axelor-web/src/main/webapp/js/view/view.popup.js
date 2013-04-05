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
			events = $scope.$events,
			saveAction = events.onSave;

		function close(value) {
			if (value) {
				value.$fetched = true;
				$scope.select(value);
			}
			canClose = true;
			$element.dialog('close');
			if ($scope.editorCanReload) {
				$scope.parentReload();
			}
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
		
		if (!_.isEmpty(selection)) {
			$scope.select(selection);
		}

		$element.dialog('close');
	};
}

AttachmentCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function AttachmentCtrl($scope, $element, DataSource, ViewService) {
	
	var objectDS = $scope._dataSource,
		initialized = false;
	
	$scope._viewParams = {
		model : 'com.axelor.meta.db.MetaFile'
	};
	
	ViewCtrl.call(this, $scope, DataSource, ViewService);
	GridViewCtrl.call(this, $scope, $element);
	
	var origOnShow = $scope.onShow,
		origShow = $scope.show,
		input = $element.children('input:first').hide(),
		frame = $element.children("iframe").hide();
	
	function getSelected(){
		var dataView = $scope.dataView;
		return selected = _.map($scope.selection, function(index) {
			return dataView.getItem(index);
		});
	};
	
	$scope.show = function() {
		origShow();
		if (initialized) {
			$scope.filter();
		}
	};
	
	$scope.filter = function(searchFilter) {
		return objectDS.attachment($scope.record.id).success(function(records){
			if(records) {
				$scope.setItems(records);
			}
		});
	};
	
	$scope.onSort = function() {
		
	};

	$scope.onShow = function(viewPromise) {
		viewPromise.then(function(){
			$element.dialog('open');
			initialized = true;
			origOnShow(viewPromise);
		});
	};
	
	$scope.onItemClick = function(e, args) {
		
	};
	
	$scope.onOK = function() {
		$element.dialog('close');
	};
	
	$scope.canDelete = function() {
		if (_.isEmpty($scope.selection)) {
			return false ;
		}
		return true ;
	};
	
	$scope.canUpload = function() {
		return true ;
	};
	
	$scope.canDownload = function() {
		if (_.isEmpty($scope.selection)) {
			return false ;
		}
		return true ;
	};
	
	$scope.onDelete = function() {
		
		var selected = getSelected();
		
		if(selected === undefined) {
			return;
		}
		axelor.dialogs.confirm(
				_t("Do you really want to delete the selected record(s)?"),
		function(confirmed){
			if (confirmed) {
				_.each(selected, function(select){
				    if(select.id){
    					var newDS = DataSource.create($scope._model);
    					newDS.removeAttachment(select.id).success(function(records){
    						$scope.updateItems(select.id, true);
    					});
					}
				});
			}
			else {
				$scope.canDownload();
				$scope.canDelete();
			}
		});
		
		setTimeout(function(){
	        $scope.canDownload();
		    $scope.canDelete();
	    });
	};
	
	$scope.onDownload = function() {
		var selected = getSelected(),
			select = selected[0];
		
		if(!select) {
			return ;
		}

		var url = "ws/rest/com.axelor.meta.db.MetaFile/" + select.id + "/content/download";
		frame.attr("src", url);
		setTimeout(function(){
			frame.attr("src", "");
		},100);
	};
	
	$scope.onUpload = function() {
		input.click();
	};
	
	input.change(function(e) {
		var file = input.get(0).files[0];
		
		if (file) {
		    
		    var record = {
				fileName: file.name,
				mine: file.type,
				size: file.size,
				id: null,
				version: null
		    };
		    
		    record.$upload = {
			    field: 'content',
			    file: file
		    };
			
		    var newDS = DataSource.create($scope._model);
		    newDS.save(record).success(function(file){
				if(file.id){
					objectDS.addAttachment($scope.record.id, file.id).success(function(record){
						$scope.updateItems(file, false);
					});
				}
			});
		};
	});
	
	$scope.updateItems = function(value, removed) {
		var target = value,
			records;
		
		if (!_.isArray(value)) {
			items = [value];
		}

		records = _.map($scope.dataView.getItems(), function(item){
			return _.clone(item);
		});

		var find = _.find(records, function(rec){
			return rec.id && rec.id == target;
		});
		
		if (find && !removed){
			_.extend(find, target);
		}
		else if(!removed){
			records.push(target);
		}
		else{
			var index = records.indexOf(find);
			records.splice(index, 1);
		}
		
		_.each(records, function(rec){
			if (rec.id <= 0) rec.id = null;
		});
		
		$scope.setItems(records);
		
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
				var maxWidth = $(document).width() - 8,
					maxHeight = $(document).height() - 8,
					width = element[0].scrollWidth + 32,
					height = element[0].scrollHeight + 16;

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
			scope.$on('adjust:dialog', _.debounce(autoSize, 300));
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
		    '<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-editable="false" x-selector="true"></div>'+
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

angular.module('axelor.ui').directive('uiAttachmentPopup', function(){
	return {
		restrict: 'EA',
		controller: AttachmentCtrl,
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
				element.closest('.ui-dialog').find('.ui-dialog-title').text('Attachments');
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
			'<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-editable="false" x-selector="true" x-no-filter="true"></div>'+
		    '<div class="record-pager pull-left">'+
			    '<div class="btn-group">'+
			      '<button class="btn" ng-disabled="!canPrev()" ng-click="onPrev()"><i class="icon-chevron-left"></i></button>'+
			      '<button class="btn" ng-disabled="!canNext()" ng-click="onNext()"><i class="icon-chevron-right"></i></button>'+
			     '</div>'+
			     '<div class="btn-group">'+
			     	'<button class="btn btn-info" ng-disabled="!canUpload()" ng-click="onUpload()"><i class="icon-upload"><span x-translate>Upload</span></i></button>'+
		     		'<button class="btn btn-success" ng-disabled="!canDownload()" ng-click="onDownload()"><i class="icon-download"><span x-translate>Download</span></i></button>'+
	     			'<button class="btn btn-danger" ng-disabled="!canDelete()" ng-click="onDelete()"><i class="icon-trash"><span x-translate>Remove</span></i></button>'+
			    '</div>'+
		    '</div>'+
		    '<iframe></iframe>' +
			'<input type="file">' +
		'</div>'
	};
});
