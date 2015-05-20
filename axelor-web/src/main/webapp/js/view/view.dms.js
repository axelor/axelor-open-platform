/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
(function(){

var ui = angular.module('axelor.ui');

ui.controller("DMSFileListCtrl", DMSFileListCtrl);
DMSFileListCtrl.$inject = ['$scope', '$element'];
function DMSFileListCtrl($scope, $element) {
	GridViewCtrl.call(this, $scope, $element);

	var _domain = $scope._domain || "";
	if (_domain) {
		_domain += " AND ";
	}

	$scope.$emptyMessage = _t("No documents found.");

	$scope.currentFolder = null;
	$scope.currentPaths = [];

	Object.defineProperty($scope, "_domain", {
		get: function () {
			if ($scope.currentFolder) {
				return _domain + "self.parent.id = " + $scope.currentFolder.id;
			}
			return _domain + "self.parent is null";
		},
		set: function () {
		}
	});

	$scope.onEdit = function() {
		var rec = getSelected();
		if (rec && rec.typeIcon === "fa fa-folder") {
			return $scope.onFolder(rec);
		}
	};

	$scope.reload = function () {
		var fields = _.pluck($scope.fields, 'name');
		var ds = $scope._dataSource;
		return ds.search({
			fields: fields,
			domain: $scope._domain
		});
	};

	$scope.onFolder = function(folder) {

		var paths = $scope.currentPaths || [];
		var index = paths.indexOf(folder);

		if (index > -1) {
			paths = paths.splice(0, index + 1);
		}
		if (folder && index === -1) {
			paths.push(folder);
		}
		if (!folder) {
			paths = [];
		}

		$scope.currentFolder = folder;
		$scope.currentPaths = paths;

		return $scope.reload();
	}

	$scope.onNewFolder = function () {

		var html = "" +
				"<div>" +
					"<input type='text' value='" + _t("New Folder") +"'>" +
				"</div>";

		var dialog = axelor.dialogs.box(html, {
			title: _t("Create folder"),
			buttons: [{
				'text': _t("Cancel"),
				'class': 'btn',
				'click': close
			}, {
				'text': _t("Create"),
				'class': 'btn btn-primary',
				'click': function (e) {
					var val = dialog.find("input").val();
					if (val && val.trim().length) {
						createFolder(val);
					} else {
						close();
					}
				}
			}]
		});

		function close() {
			if (dialog) {
				dialog.dialog("close");
				dialog = null;
			}
		}

		function createFolder(name) {
			var ds = $scope._dataSource;
			var record = {
				fileName: name,
				isDirectory: true
			};
			if ($scope.currentFolder) {
				record.parent = _.pick($scope.currentFolder, "id");
			}
			ds.save(record).then(close, close);
		}

		dialog.parent().addClass("dms-folder-dialog");
		setTimeout(function() {
			dialog.find("input").focus().select();
		});
	};

	$scope.onItemClick = function(event, args) {
		var elem = $(event.target);
		$scope.$timeout(function () {
			if (elem.is('.fa-folder')) return $scope.onFolder(getSelected());
		});
	};

	$scope.onItemDblClick = function(event, args) {
		var elem = $(event.target);
		if (elem.hasClass("fa")) return;
		$scope.onEdit();
		$scope.$apply();
	};

	function getSelected() {
		var index = _.first($scope.selection || []);
		return $scope.dataView.getItem(index);
	}
}

ui.directive('uiDmsUploader', function () {

	return function (scope, element, attrs) {

		var input = element.find("input.upload-input");

		scope.uploadFiles = []

		var dndTimer = null;
		var dndInternal = false;
		var dndDropClass = "dropping";
		var dndDragClass = "dragging";
		var dndEvents = "dragstart,dragend,dragover,dragenter,dragleave,drop".split(",");

		function clearClassName(force) {
			if (dndTimer) {
				clearTimeout(dndTimer);
			}
			if (force) {
				element.removeClass(dndDropClass);
				return;
			}
			dndTimer = setTimeout(function () {
				element.removeClass(dndDropClass);
			}, 100);
		}

		function dragAndDropHandler(e) {
			switch (e.type) {
			case "dragstart":
			case "dragend":
				dndInternal = e.type === "dragstart";
				break;
			case "dragover":
				onDragOver(e);
				break;
			case "dragenter":
				break;
			case "dragleave":
				clearClassName();
			case "drop":
				clearClassName();
				onDropFiles(e);
				break;
			}
		}

		function onDragOver(e) {
			if (dndInternal) return;
			clearClassName(true);
			if (element.is(e.target) || element.has(e.target).size()) {
				element.addClass(dndDropClass);
			} else {
				clearClassName();
			}
		}

		function onDropFiles(e) {
			if (dndInternal) return;
			if (element.is(e.target) || element.has(e.target)) {
				doUpload(e.dataTransfer.files);
			}
		}

		dndEvents.forEach(function (name) {
			document.addEventListener(name, dragAndDropHandler);
		});

		scope.$on("$destroy", function() {
			dndEvents.forEach(function (name) {
				document.removeEventListener(name, dragAndDropHandler);
			});
		});

		function doUpload(files) {
			var all = files;
			if (files.fileName) {
				files = [files];
			}

			var i, file;
			for (i = 0; i < all.length; i++) {
				file = all[i];
			}
			for (i = 0; i < all.length; i++) {
				uploadSingle(all[i]);
			}
		}

		function uploadSingle(file) {

			var info = {
				name: file.name,
				progress: "0%",
				complete: false
			};

			scope.uploadFiles.push(info);

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

			var ds = scope._dataSource;
			var metaDS = ds._new("com.axelor.meta.db.MetaFile");
			metaDS.save(record).progress(function(fn) {
				info.progress = (fn > 95 ? 95 : fn) + "%";
			}).success(function(metaFile) {
				var record = {
					fileName: metaFile.fileName,
					metaFile: _.pick(metaFile, "id")
				};
				if (scope.currentFolder) {
					record.parent = _.pick(scope.currentFolder, "id");
				}
				ds.save(record).success(function (dmsFile) {
					info.progress = "100%";
					info.complete = true;
					var index = scope.uploadFiles.indexOf(info);
					if (index > -1) {
						scope.uploadFiles.splice(index, 1);
					}
				});
			});
		}

		input.change(function() {
			scope.applyLater(function () {
				doUpload(input.get(0).files);
			});
		});

		scope.onUpload = function () {
			input.click();
		};
	};
});


// prevent download on droping files
$(function () {
	window.addEventListener("dragover",function(e) {
		e.preventDefault();
	}, false);

	window.addEventListener("drop",function(e) {
		e.preventDefault();
	}, false);
});

}).call(this);
