/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
(function () {

"use strict";

var ui = angular.module("axelor.ui");

EditorCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService', '$q'];

function EditorCtrl($scope, $element, DataSource, ViewService, $q) {
	
	var parent = $scope.$parent;
	
	$scope._viewParams = parent._viewParams;
	$scope.editorCanSave = parent.editorCanSave;
	$scope.editorCanReload = parent.editorCanReload;

	ui.ViewCtrl.call(this, $scope, DataSource, ViewService);
	ui.FormViewCtrl.call(this, $scope, $element);
	
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
		recordVersion = record ? record.version : -1;
		if (recordVersion === undefined && record) {
			recordVersion = record.$version;
		}
		this.edit(record);
	};

	function doEdit(record, fireOnLoad) {
		if (record && record.id > 0 && (!_.isNumber(record.version) || !record.$fetched)) {
			$scope.doRead(record.id).success(function(rec) {
				if (record.$dirty) {
					rec = _.extend({}, rec, record);
				}
				originalEdit(rec, fireOnLoad);
			});
		} else {
			originalEdit(record, fireOnLoad);
		}
		canClose = false;
	}

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
		var id = ($scope.record || {}).id;
		if (!id || id < 0) {
			return $scope.hasPermission('create');
		}
		return $scope.hasPermission('write') &&
			$scope.canEditTarget() &&
			isEditable.call($scope);
	};
	
	var canEdit = $scope.canEdit;
	$scope.canEdit = function() {
		return $scope.canEditTarget() && canEdit.call($scope);
	};

	$scope.edit = function(record, fireOnLoad) {
		if (isClosed) return;
		$scope._viewPromise.then(function(){
			doEdit(record, fireOnLoad);
			$scope.setEditable(!$scope.$parent.$$readonly);
		});
	};

	function isChanged() {
		if ($scope.isDirty()) return true;
		var record = $scope.record || {};
		var version = record.version;
		return recordVersion !== version || record.$forceDirty;
	}

	function canOK() {
		if (isClosed) return false;
		return isChanged();
	}

	function onOK() {

		var record = $scope.record;

		function close(value, forceSelect) {
			if (value && (forceSelect || canOK())) {
				value.$fetched = true;
				value.selected = true;
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
		}

		var event = $scope.$broadcast('on:before-save', record);
		if (event.defaultPrevented) {
			if (event.error) {
				axelor.dialogs.error(event.error);
			}
			return;
 		}

		// wait for onChange actions
		$scope.waitForActions(function() {
			if ($scope.editorCanSave && isChanged()) {
				if (record.id < 0)
					record.id = null;
				return $scope.onSave({force: true}).then(function(record, page) {
					// wait for onSave actions
					$scope.waitForActions(function(){
						close(record, true);
					});
				});
			}
			if ($scope.isValid()) {
				close(record);
			}
		}, 100);
	}
	
	$scope.onOK = function() {
		$scope.$timeout(onOK, 10);
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
			$(e.target).blur().focus();
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

	ui.ViewCtrl.call(this, $scope, DataSource, ViewService);
	ui.GridViewCtrl.call(this, $scope, $element);
	
	var searchLimit = (parent.field||{}).searchLimit || 0;
	if (searchLimit > 0) {
		$scope._dataSource._page.limit = searchLimit;
	}

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
				$element.closest('.ui-dialog').css('opacity', 0);
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
		return $scope.hasPermission('create') && $scope.$parent.canNew();
	};
}

ui.directive('uiDialogSize', function() {

	return function (scope, element, attrs) {
		
		// use only with dialogs
		if (attrs.uiDialog === undefined && !element.hasClass('ui-dialog-content')) {
			return;
		}
		
		var addMaximizeButton = _.once(function () {
			var elemDialog = element.parent();
			var elemTitle = elemDialog.find('.ui-dialog-title');
			var resizable = elemDialog.hasClass('ui-resizable');
			var draggable = elemDialog.hasClass('ui-draggable');
			$('<a href="#" class="ui-dialog-titlebar-max"><i class="fa fa-expand"></i></a>').click(function (e) {
				$(this).children('i').toggleClass('fa-expand fa-compress');
				elemDialog.toggleClass('maximized');
				element.dialog('option', 'position', 'center');
				if (resizable) {
					element.dialog('option', 'resizable', !elemDialog.hasClass('maximized'));
				}
				if (resizable) {
					element.dialog('option', 'draggable', !elemDialog.hasClass('maximized'));
				}
				axelor.$adjustSize();
				return false;
			}).insertAfter(elemTitle);

			// remove maximized state on close
			element.on('dialogclose', function(e, ui) {
				elemDialog.removeClass('maximized');
				if (resizable) {
					element.dialog('option', 'resizable', true);
				}
				if (draggable) {
					element.dialog('option', 'draggable', true);
				}
			});
		});

		var initialized = false;

		function adjustSize() {

			var form = element.children('[ui-view-form],[ui-view-pane]').find('form[ui-form]:first');
			var maxHeight = $(document).height() - 16;
			var height = maxHeight;

			height -= element.parent().children('.ui-dialog-titlebar').outerHeight(true) + 4;
			height -= element.parent().children('.ui-dialog-buttonpane').outerHeight(true) + 4;

			if (element.is('.nav-tabs-popup,[ui-selector-popup]')) {
				height = Math.min(height, 480);
			} else if (height > element[0].scrollHeight) {
				height = element[0].scrollHeight + 8;
			}

			if (form.size() && height > form[0].scrollHeight) {
				height = form[0].scrollHeight + 8;
			}

			element.height(height);

			function doAdjust() {
				// set height to wrapper to fix overflow issue
				var wrapper = element.dialog('widget');
				wrapper.height(wrapper.height());

				// show the dialog
				element.dialog('option', 'position', 'center');
				element.closest('.ui-dialog').css('opacity', '');

				initialized = true;
			}

			if (!form.size() || initialized) {
				return doAdjust();
			}

			var last = form[0].scrollHeight;
			scope.ajaxStop(function () {
				if (last < form[0].scrollHeight) {
					return adjustSize();
				}
				doAdjust();
			}, 100);
		}

		function doShow() {

			addMaximizeButton();

			// focus first element
			if (!axelor.device.mobile) {
				element.find(':input:first').focus();
			}

			//XXX: ui-dialog issue
			element.find('.slick-headerrow-column,.slickgrid,[ui-embedded-editor]').zIndex(element.zIndex());
			element.find('.record-toolbar .btn').zIndex(element.zIndex()+1);

			scope.ajaxStop(function() {
				adjustSize();
				axelor.$adjustSize();
			}, 100);
		}

		// a flag used by evalScope to detect popup (see form.base.js)
		scope._isPopup = true;
		scope._doShow = function(viewPromise) {
			element.dialog('open');
			element.closest('.ui-dialog').css('opacity', 0);
			viewPromise.then(function(s) {
				scope.waitForActions(doShow);
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

ui.directive('uiEditorPopup', function() {
	
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

			var onNewHandler = scope.onNewHandler;
			scope.onNewHandler = function (event) {
				if (element.dialog('isOpen')) {
					return onNewHandler.apply(scope, arguments);
				}
			};

			scope.isPopupOpen = true;
			setTimeout(function () {
				var isOpen = false;
				element.on('dialogclose', function (e) {
					isOpen = false;
					scope.waitForActions(function () {
						scope.isPopupOpen = isOpen;
					}, 2000); // delay couple of seconds to that popup can cleanup
				});
				element.on('dialogopen', function (e) {
					scope.isPopupOpen = isOpen = true;
					scope.applyLater();
				});
			});
		},
		replace: true,
		template:
		'<div ui-dialog ui-dialog-size x-resizable="true" x-on-ok="onOK" x-on-before-close="onBeforeClose" ui-watch-if="isPopupOpen">'+
		    '<div ui-view-form x-handler="this"></div>'+
		'</div>'
	};
});

ui.directive('uiSelectorPopup', function(){
	
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
		'<div ui-dialog ui-dialog-size x-resizable="true" x-on-ok="onOK">'+
		    '<div ui-view-grid x-view="schema" x-data-view="dataView" x-handler="this" x-editable="false" x-selector="{{selectMode}}"></div>'+
		    '<div ui-record-pager></div>'+
		    '<div class="ui-dialog-buttonset-left pull-left" ng-show="canNew()">'+
		    	'<button class="btn" ng-click="onCreate()" x-translate>Create</button>'+
		    '</div>'+
		'</div>'
	};
});

})();
