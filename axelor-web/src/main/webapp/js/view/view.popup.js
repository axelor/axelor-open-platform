/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
EditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService', '$q'];
function EditorCtrl($scope, $element, DataSource, ViewService, $q) {
	
	var parent = $scope.$parent;
	
	$scope._viewParams = parent._viewParams;
	$scope.editorCanSave = parent.editorCanSave;
	$scope.editorCanReload = parent.editorCanReload;

	ViewCtrl.call(this, $scope, DataSource, ViewService);
	FormViewCtrl.call(this, $scope, $element);
	
	var closeCallback = null;
	var originalEdit = $scope.edit;
	var originalShow = $scope.show;

	var recordVersion = -1;
	var canClose = false;
	var isClosed = true;

	$scope.show = function(record, callback) {
		originalShow();
		if (_.isFunction(record)) {
			callback = record;
			record = null;
		}
		closeCallback = callback;
		isClosed = false;
		recordVersion = -1;
		this.edit(record);
	};

	function doEdit(record) {
		if (record && record.id > 0 && (!(record.version >= 0) || !record.$fetched)) {
			$scope.doRead(record.id).success(function(record){
				if (recordVersion === -1) {
					recordVersion = record.version;
				}
				originalEdit(record);
			});
		} else {
			if (recordVersion === -1 && record) {
				recordVersion = record.version;
			}
			originalEdit(record);
		}
		canClose = false;
	};

	var parentCanEditTarget = null;
	
	$scope.canEditTarget =  function () {
		if (parentCanEditTarget === null) {
			var parent = $scope.$parent;
			var func = parent.canEditTarget;
			while (parent && func === $scope.canEditTarget) {
				parent = parent.$parent;
				func = parent.canEditTarget;
			}
			parentCanEditTarget = func || angular.noop;
		}
		return parentCanEditTarget() !== false;
	};

	var isEditable = $scope.isEditable;
	$scope.isEditable = function () {
		if (!($scope.record || {}).id > 0) {
			return true;
		}
		return $scope.canEditTarget() && isEditable.call($scope);
	};
	
	var canEdit = $scope.canEdit;
	$scope.canEdit = function() {
		return $scope.canEditTarget() && canEdit.call($scope);
	};

	$scope.edit = function(record) {
		if (isClosed) return;
		$scope._viewPromise.then(function(){
			doEdit(record);
			$scope.setEditable(!$scope.$parent.$$readonly);
		});
	};

	$scope.canOK = function() {
		if (isClosed) return false;
		if ($scope.isDirty()) return true;
		var record = $scope.record || {};
		var version = record.version;
		return recordVersion !== version;
	};

	function onOK() {

		var record = $scope.record;

		function close(value) {
			if (value) {
				value.$fetched = true;
				$scope.$parent.select(value);
			}
			canClose = true;
			$element.dialog('close');
			if ($scope.editorCanReload) {
				$scope.$parent.parentReload();
			}
			if (closeCallback && value) {
				closeCallback(value);
			}
			closeCallback = null;
			isClosed = true;
		};

		if ($scope.editorCanSave && $scope.isDirty()) {
			if (record.id < 0)
				record.id = null;
			$scope.onSave().then(function(record, page) {
				$scope.applyLater(function(){
					close(record);
				});
			});
		} else {
			close(record);
		}
	};
	
	$scope.onOK = function() {

		if (!$scope.isValid())
			return;

		$scope.ajaxStop(function() {
			setTimeout(function() {
				onOK();
			}, 100);
		});
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
	
	$scope.onHotKey = function (e, action) {
		
		if (action === "save") {
			$scope.onOK();
		}
		
		$scope.applyLater();
		
		return false;
	};
}

SelectorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];
function SelectorCtrl($scope, $element, DataSource, ViewService) {
	
	var parent = $scope.$parent;
	
	$scope._viewParams = parent._viewParams;
	$scope.getDomain = parent.getDomain;

	ViewCtrl.call(this, $scope, DataSource, ViewService);
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
	
	var _getContext = $scope.getContext;
	$scope.getContext = function() {
		// selector popup should return parent's context
		if ($scope.$parent && $scope.$parent.getContext) {
			return $scope.$parent.getContext();
		}
		return _getContext();
	};
	
	$scope.onItemClick = function(e, args) {
		$scope.applyLater($scope.onOK);
	};
	
	var origOnShow = $scope.onShow;
	$scope.onShow = function(viewPromise) {
		
		viewPromise.then(function(){
			if (!initialized) {
				$element.closest('.ui-dialog').css('visibility', 'hidden');
			}
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
			$scope.applyLater(function () {
				$scope.$parent.select(selection);
				$scope.selection = [];
			});
		}

		$element.dialog('close');
	};

	$scope.onCreate = function () {
		$element.dialog('close');
		$scope.$parent.onNew();
	};

	$scope.canNew = function () {
		return $scope.$parent.canNew();
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
		progress = $element.find('.progress').hide(),
		uploadSize = $scope.$eval('app.fileUploadSize');
		
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
		var fields = _.pluck($scope.fields, 'name');
			options = {
				fields: fields
			};

		return objectDS.attachment($scope.record.id, options).success(function(records) {
			if(records) {
				$scope.setItems(records);
			}
		});
	};
	
	$scope.onSort = function() {
		
	};

	$scope.onShow = function(viewPromise) {
		viewPromise.then(function() {
			$element.dialog('open');
			initialized = true;
			origOnShow(viewPromise);
		});
	};
	
	$scope.onItemClick = function(e, args) {
		
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
				var newDS = DataSource.create($scope._model);
				newDS.removeAttachment(selected).success(function(records){
					$scope.updateItems(records, true);
				});
			}
		});
	};
	
	$scope.onDownload = function() {
		var selected = getSelected(),
			select = selected[0];
		
		if(!select) {
			return ;
		}

		var url = "ws/rest/com.axelor.meta.db.MetaFile/" + select.id + "/content/download";
		
		if ($scope.doDownload) {
			$scope.doDownload(url);
		} else {
			window.open(url);
		}
	};
	
	$scope.onUpload = function() {
		input.click();
	};
	
	input.change(function(e) {
		var file = input.get(0).files[0];
		if (!file) {
			return;
		}

		if(file.size > 1048576 * parseInt(uploadSize)) {
			return axelor.dialogs.say(_t("You are not allow to upload a file bigger than") + ' ' + uploadSize + 'MB');
		}
	    
	    var record = {
			fileName: file.name,
			mime: file.type,
			size: file.size,
			id: null,
			version: null
	    };

	    record.$upload = {
		    file: file
	    };

	    setTimeout(function() {
	    	progress.show();
	    });
	    
	    var newDS = DataSource.create($scope._model);
	    newDS.save(record).progress(function(fn) {
	    	$scope.updateProgress(fn > 95 ? 95 : fn);
	    }).success(function(file) {
	    	$scope.updateProgress(100);
	    	if(file && file.id) {
	    		objectDS.addAttachment($scope.record.id, file.id)
				.success(function(record) {
				    progress.hide();
				    $scope.updateProgress(0);
					$scope.updateItems(file, false);
				}).error(function() {
					progress.hide();
					$scope.updateProgress(0);
				});
			}
		}).error(function() {
			progress.hide();
			$scope.updateProgress(0);
		});
	});
	
	$scope.updateProgress = function(value) {
		progress.children('.bar').css('width',value + '%');
	    progress.children('.bar').text(value + '%');
	};
	
	$scope.updateItems = function(value, removed) {
		var items = value,
			records;
		
		if (!_.isArray(value)) {
			items = [value];
		}

		records = _.map($scope.getItems(), function(item) {
			return _.clone(item);
		});
		
		_.each(items, function(item) {
			item = _.clone(item);
			var find = _.find(records, function(rec) {
				return rec.id && rec.id == item.id;
			});
			
			if (find && !removed) {
				_.extend(find, item);
			} else if(!removed) {
				records.push(item);
			} else {
				var index = records.indexOf(find);
				records.splice(index, 1);
			}
		});
		
		_.each(records, function(rec) {
			if (rec.id <= 0) rec.id = null;
		});

		// update attachment counter
		($scope.$parent.record || {}).$attachments = _.size(records);
		
		$scope.setItems(records);
	};
}

angular.module('axelor.ui').directive('uiDialogSize', function() {

	return function (scope, element, attrs) {
		
		// use only with ui-dialog directive
		if (attrs.uiDialog === undefined) {
			return;
		}
		
		var addMaximizeButton = _.once(function () {
			var elemDialog = element.parent();
			var elemTitle = elemDialog.find('.ui-dialog-title');
			$('<a href="#" class="ui-dialog-titlebar-max"><i class="fa fa-expand"></i></a>').click(function (e) {
				$(this).children('i').toggleClass('fa-expand fa-compress');
				elemDialog.toggleClass('maximized');
				axelor.$adjustSize();
				return false;
			}).insertAfter(elemTitle);
		});
		
		element.on('adjustSize', _.throttle(adjustSize));
		
		function adjustSize() {
			var maxHeight = $(document).height() - 16;
			var height = maxHeight;

			height -= element.parent().children('.ui-dialog-titlebar').outerHeight(true) + 4;
			height -= element.parent().children('.ui-dialog-buttonpane').outerHeight(true) + 4;

			if (element.is('.nav-tabs-popup')) {
				var toolbar = element.find('.form-view:first > .record-toolbar');
				var form = element.find('.form-view:first > [ui-view-form]');
				var h = toolbar.height() + form[0].scrollHeight - 16;
				height = Math.min(height, h);
			} else if (element.is('[ui-selector-popup]')) {
				height = Math.min(height, 480);
			} else if (height > element[0].scrollHeight - 16) {
				height = 'auto';
			}
			element.height(height);
		}

		function doShow() {

			axelor.$adjustSize();

			addMaximizeButton();

			return scope.ajaxStop(function () {
				adjustSize();
				element.closest('.ui-dialog').css('visibility', '');

				// focus first element
				if (!axelor.device.mobile) {
					element.find(':input:first').focus();
				}

				//XXX: ui-dialog issue
				element.find('.slick-headerrow-column,.slickgrid').zIndex(element.zIndex());

			}, 100);
		}
		
		// a flag used by evalScope to detect popup (see form.base.js)
		scope._isPopup = true;
		scope._doShow = function(viewPromise) {
			
			viewPromise.then(function(s) {
				element.closest('.ui-dialog').css('visibility', 'hidden');
				element.dialog('open');
				scope.ajaxStop(doShow, 100);
			});
		};
		
		scope._setTitle = function (title) {
			if (title) {
				element.closest('.ui-dialog').find('.ui-dialog-title').text(title);
			}
		};

		scope.adjustSize = function() {

		};
	};
});

angular.module('axelor.ui').directive('uiEditorPopup', function() {
	
	return {
		restrict: 'EA',
		controller: EditorCtrl,
		scope: {},
		link: function(scope, element, attrs) {
			
			scope.onShow = function(viewPromise) {
				scope._doShow(viewPromise);
			};
			
			scope.$watch('schema.title', function (title) {
				scope._setTitle(title);
			});

			element.scroll(function (e) {
				$.event.trigger('adjustScroll');
			});

			var btnOK = null;
			function buttonState(canSave) {
				if (btnOK === null) {
					btnOK = element.siblings('.ui-dialog-buttonpane').find('.button-ok');
				}
				if (canSave) {
					return btnOK.show();
				}
				return btnOK.hide();
			}

			scope.$watch('canOK()', buttonState);
		},
		replace: true,
		template:
		'<div ui-dialog ui-dialog-size x-on-ok="onOK" x-on-before-close="onBeforeClose">'+
		    '<div ui-view-form x-handler="this"></div>'+
		'</div>'
	};
});

angular.module('axelor.ui').directive('uiSelectorPopup', function(){
	
	return {
		restrict: 'EA',
		controller: SelectorCtrl,
		scope: {
			selectMode: "@"
		},
		link: function(scope, element, attrs) {

			var height = $(window).height();
			height = (70 * height / 100);
			
			scope._calcHeight = function (h) {
				return height;
			};

			var onShow = scope.onShow;
			scope.onShow = function (viewPromise) {
				if (scope.clearFilters) {
					scope.clearFilters();
					scope.selection = [];
				}
				onShow(viewPromise);
				scope._doShow(viewPromise);
			};
			
			scope.$watch('schema.title', function(title){
				scope._setTitle(title);
			});

			var btnOK = null;

			function buttonState(count) {
				if (btnOK === null) {
					btnOK = element.siblings('.ui-dialog-buttonpane').find('.btn:last');
				}
				return btnOK.attr('disabled', !count || count <= 0);
			}

			scope.$watch('selection.length', buttonState);

			setTimeout(function(){
				var footer = element.closest('.ui-dialog').find('.ui-dialog-buttonpane'),
					header = element.closest('.ui-dialog').find('.ui-dialog-titlebar'),
					pager = element.find('.record-pager'),
					buttons = element.find('.ui-dialog-buttonset-left');
				header.find('.ui-dialog-title').after(pager);
				footer.prepend(buttons);
				footer.find('.button-ok').html(_t("Select"));
			});
		},
		replace: true,
		template:
		'<div ui-dialog ui-dialog-size x-on-ok="onOK">'+
		    '<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-editable="false" x-selector="{{selectMode}}"></div>'+
		    '<div class="record-pager">'+
		    	'<span class="record-pager-text">{{pagerText()}}</span>'+
			    '<div class="btn-group">'+
			      '<button class="btn" ng-disabled="!canPrev()" ng-click="onPrev()"><i class="fa fa-chevron-left"></i></button>'+
			      '<button class="btn" ng-disabled="!canNext()" ng-click="onNext()"><i class="fa fa-chevron-right"></i></button>'+
			    '</div>'+
		    '</div>'+
		    '<div class="ui-dialog-buttonset-left pull-left" ng-show="canNew()">'+
		    	'<button class="btn" ng-click="onCreate()" x-translate>Create</button>'+
		    '</div>'+
		'</div>'
	};
});

angular.module('axelor.ui').directive('uiAttachmentPopup', function(){
	return {
		restrict: 'EA',
		controller: AttachmentCtrl,
		link: function(scope, element, attrs) {
			
			var doResize = _.once(function doResize() {
				
				var width = $(window).width();
				var height = $(window).height();

				width = Math.min(1000, (70 * width / 100));
				height = Math.min(600, (70 * height / 100));
				
				element.dialog('option', 'width', width);
				element.dialog('option', 'height', height);
				
				element.closest('.ui-dialog').position({
			      my: "center",
			      at: "center",
			      of: window
			    });
			});

			scope.onOpen = function(e, ui) {
				setTimeout(doResize);
			};
			
			scope.doDownload = function (url) {
				var frame = element.find('iframe:first');
				frame.attr("src", url);
				setTimeout(function(){
					frame.attr("src", "");
				}, 100);
			};
			
			scope.$watch('schema.title', function(title){
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
		'<div ui-dialog x-on-open="onOpen" x-on-ok="false">'+
			'<iframe style="display: hidden;"></iframe>'+
			'<input type="file">' +
			'<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-editable="false" x-selector="true" x-no-filter="true"></div>'+
		    '<div class="record-pager pull-left">'+
			     '<div class="btn-group">'+
			     	'<button class="btn btn-primary" ng-disabled="!canUpload()" ng-click="onUpload()"><i class="fa fa-arrow-circle-up"/> <span x-translate>Upload</span></button>'+
		     		'<button class="btn btn-success" ng-disabled="!canDownload()" ng-click="onDownload()"><i class="fa fa-arrow-circle-down"/> <span x-translate>Download</span></button>'+
	     			'<button class="btn btn-danger" ng-disabled="!canDelete()" ng-click="onDelete()"><i class="fa fa-trash-o"/> <span x-translate>Remove</span></button>'+
			    '</div>'+
			    '<div class="btn-group">'+
				    '<div class="progress progress-striped active" style="width: 300px; background: gainsboro; margin-top: 5px; margin-bottom: 0px;">'+
	            		'<div class="bar" style="width: 0%;"></div>'+
	        		'</div>'+
        		'</div>'+
		    '</div>'+
		'</div>'
	};
});
