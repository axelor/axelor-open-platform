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

function inputDialog(options, callback) {

	var opts = _.extend({
		value: "",
		title: "",
		titleOK: _t("OK"),
		titleCancel: _t("Cancel"),
	}, options);

	var html = "" +
	"<div>" +
		"<input type='text' value='" + opts.value +"'>" +
	"</div>";

	var dialog = axelor.dialogs.box(html, {
		title: opts.title,
		buttons: [{
			'text': opts.titleCancel,
			'class': 'btn',
			'click': close
		}, {
			'text': opts.titleOK,
			'class': 'btn btn-primary',
			'click': submit
		}]
	})
	.on("keypress", "input", function (e) {
		if (e.keyCode === 13) {
			submit();
		}
	});

	function close() {
		if (dialog) {
			dialog.dialog("close");
			dialog = null;
		}
	}

	function submit() {
		var value = dialog.find("input").val().trim();
		if (value) {
			return callback(value, close);
		}
		return close();
	}

	dialog.parent().addClass("dms-folder-dialog");
	setTimeout(function() {
		dialog.find("input").focus().select();
	});
}

ui.controller("DMSFileListCtrl", DMSFileListCtrl);
DMSFileListCtrl.$inject = ['$scope', '$element'];
function DMSFileListCtrl($scope, $element) {
	GridViewCtrl.call(this, $scope, $element);

	var _domain = $scope._domain || "";
	if (_domain) {
		_domain += " AND ";
	}

	$scope.$emptyMessage = _t("No documents found.");
	$scope.$confirmMessage = _t("Are you sure you want to delete selected documents?");

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

	$scope.onFolder = function(folder, currentPaths) {

		var paths = currentPaths || $scope.currentPaths || [];
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

	$scope.onNewFolder = function () {
		var count = 1;
		var selected = $scope.getSelected() || {};
		var existing = _.pluck((selected.nodes || []), "fileName");

		var name = _t("New Folder");
		var _name = name;
		while(existing.indexOf(_name) > -1) {
			_name = name + " (" + ++count + ")";
		}
		name = _name;

		inputDialog({
			value: name,
			title: _t("Create folder"),
			titleOK: _t("Create")
		}, function (value, done) {
			var record = {
				fileName: value,
				isDirectory: true
			};
			if ($scope.currentFolder) {
				record.parent = _.pick($scope.currentFolder, "id");
			}
			var promise = $scope._dataSource.save(record);
			promise.then(done, done);
			promise.success(function (record) {
				$scope.reload();
			});
		});
	};

	$scope.onRename = function () {
		var record = getSelected();
		if (!record || !record.id) {
			return;
		}

		inputDialog({
			value: record.fileName
		}, function(value, done) {
			if (record.fileName !== value) {
				record.fileName = value;
				rename(record, done);
			} else {
				done();
			}
		});

		function rename(record, done) {
			var promise = $scope._dataSource.save(record);
			promise.then(done, done);
			promise.success(function (record) {
				$scope.reload();
			});
		}
	};

	$scope.onMoveFiles = function (files, toFolder) {
		if (_.isEmpty(files)) { return; }
		_.each(files, function (item) {
			if (toFolder && toFolder.id) {
				item.parent = {
					id: toFolder.id
				};
			} else {
				item.parent = null;
			}
		});

		$scope._dataSource.saveAll(files)
		.success(function (records) {
			$scope.reload();
			$scope._dataSource.trigger("on:save", []);
		});
	};
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

DmsFolderTreeCtrl.$inject = ["$scope", "DataSource"];
function DmsFolderTreeCtrl($scope, DataSource) {

	var ds = DataSource.create("com.axelor.dms.db.DMSFile");
	var domain = "self.isDirectory = true";
	var params = $scope._viewParams;
	if (params.domain) {
		domain += " AND " + params.domain;
	}

	$scope.folders = {};
	$scope.rootFolders = [];

	function syncFolders(records) {

		var home = {};
		var folders = {};
		var rootFolders = [];

		_.each(records, function (item) {
			folders[item.id] = item;
			item.nodes = [];
		});
		_.each(records, function (item) {
			var parent = folders[item["parent.id"]];
			if (parent) {
				parent.nodes.push(item);
				item.parent = parent;
			}
		});

		home = {
			fileName: _t("Home")
		};
		home.open = true;
		home.active = true;
		home.nodes = _.filter(folders, function (item) {
			return !item.parent;
		});

		rootFolders = [home];

		// sync with old state
		_.each($scope.folders, function (item, id) {
			var folder = folders[id];
			if (folder) {
				folder.open = item.open;
				folder.active = item.active;
			}
		});
		_.each($scope.rootFolders, function (root, i) {
			var folder = rootFolders[i];
			if (folder) {
				folder.open = root.open;
				folder.active = root.active;
			}
		});

		$scope.folders = folders;
		$scope.rootFolders = rootFolders;
	}

	$scope.getSelected = function () {
		for (var id in $scope.folders) {
			var folder = $scope.folders[id];
			if (folder && folder.active) {
				return folder;
			}
		}
		var home = _.first($scope.rootFolders);
		if (home && home.active) {
			return home;
		}
	};

	$scope.sync = function () {
		return ds.search({
			fields: ["fileName", "parent.id"],
			domain: domain,
			limit: -1,
		}).success(syncFolders);
	};

	$scope.showTree = true;

	$scope.onToggleTree = function () {
		$scope.showTree = !$scope.showTree;
		axelor.$adjustSize();
	};

	$scope.onFolderClick = function (node) {
		if (!node || !node.id) {
			$scope.onFolder();
			$scope.applyLater();
			return;
		}
		var paths = [];
		var parent = node.parent;
		while (parent) {
			paths.unshift(parent);
			parent = parent.parent;
		}
		$scope.onFolder(node, paths);
		$scope.applyLater();
	};
}

ui.directive("uiDmsFolders", function () {
	return {
		controller: DmsFolderTreeCtrl,
		link: function (scope, element, attrs) {

			scope.onGridInit = _.once(function (grid, instance) {

				grid.onDragInit.subscribe(function (e, dd) {
					e.stopImmediatePropagation();
				});

				grid.onDragStart.subscribe(function (e, dd) {

					var cell = grid.getCellFromEvent(e);
					if (!cell) return;

					dd.row = cell.row;
					var record = grid.getDataItem(dd.row);
					if (!record || !record.id) {
						return;
					}

					e.stopImmediatePropagation();
					dd.mode = "recycle";

					var rows = grid.getSelectedRows();
					if (rows.length === 0 || rows.indexOf(dd.row) === -1) {
						rows = [dd.row];
					}

					grid.setSelectedRows(rows);
					grid.setActiveCell(cell.row, cell.cell);

					dd.rows = rows;
					dd.count = rows.length;
					dd.records = _.map(rows, function (i) {
						return grid.getDataItem(i);
					});

					var text = "<span>" + record.fileName + "</span>";
					if (dd.count > 1) {
						text += " <span class='badge badge-important'>"+ dd.count +"</span>";
					}

					var proxy = $("<div class='grid-dnd-proxy'></div>")
						.hide()
						.html(text)
						.appendTo("body");

					return dd.helper = proxy;
				});

				grid.onDrag.subscribe(function (e, dd) {
					if (dd.mode != "recycle") { return; }
					dd.helper.show().css({top: e.pageY + 5, left: e.pageX + 5});
				});

				grid.onDragEnd.subscribe(function (e, dd) {
					if (dd.mode != "recycle") { return; }
					dd.helper.remove();
				});

				$.drop({
					mode: "intersect"
				});
			});

			scope.$watch("currentFolder", function (folder) {
				var folders = scope.folders || {};
				var rootFolders = scope.rootFolders || [];

				for (var id in folders) {
					folders[id].active = false;
				}

				(rootFolders[0]||{}).active = false;

				var id = (folder||{}).id;
				var node = folders[id] || rootFolders[0];
				if (node) {
					node.active = true;
				}
			});

			function sync() {
				return scope.sync();
			}

			scope._dataSource.on("on:save", sync);
			scope._dataSource.on("on:remove", sync);

			sync();
		},
		template: "<ul ui-dms-tree x-handler='this' x-nodes='rootFolders' class='dms-tree'></ul>"
	};
});

ui.directive("uiDmsTreeNode", function () {

	return {
		scope: true,
		link: function (scope, element, attrs) {

			element.children('.highlight').on("dropstart", function (e, dd) {
				var records = dd.records;
				if (!records || records.length === 0) {
					return;
				}
				if (scope.node.active) {
					return;
				}
				for (var i = 0; i < records.length; i++) {
					var record = records[i];
					var current = scope.node;
					while(current) {
						if (record.id === current.id) { return; }
						if (record.parent && record.parent.id === current.id) { return; }
						current = current.parent;
					}
				}
				element.addClass("dropping");
			});

			element.children('.highlight').on("dropend", function (e, dd) {
				element.removeClass("dropping");
			});

			element.children('.highlight').on("drop", function (e, dd) {
				var records = dd.records;
				if (!records || records.length === 0 || !scope.node) {
					return;
				}
				if (!element.hasClass("dropping")) {
					return;
				}
				scope.onMoveFiles(records, scope.node);
			});
		},
		replace: true,
		template: "" +
		"<a ng-click='onClick($event, node)' ng-class='{active: node.active}'>" +
			"<span class='highlight'></span>" +
			"<i class='fa fa-caret-down handle' ng-show='node.open'></i> " +
			"<i class='fa fa-caret-right handle' ng-show='!node.open'></i> " +
			"<i class='fa fa-folder'></i> " +
			"<span class='title'>{{node.fileName}}</span>" +
		"</a>"
	};
});

ui.directive("uiDmsTree", ['$compile', function ($compile) {

	var template = "" +
	"<li ng-repeat='node in nodes' ng-class='{empty: !node.nodes.length}' class='dms-tree-folder'>" +
		"<a x-node='node' ui-dms-tree-node></a>" +
		"<ul ng-show='parent.open' x-parent='node' x-handler='handler' x-nodes='node.nodes' ui-dms-tree></ul>" +
	"</li>";

	return {
		scope: {
			parent: "=",
			nodes: "=",
			handler: "="
		},
		link: function (scope, element, attrs) {
			var handler = scope.handler;

			scope.onClick = function (e, node) {
				if ($(e.target).is("i.handle")) {
					return node.open = !node.open;
				}
				return handler.onFolderClick(node);
			};

			scope.onMoveFiles = function (files, toFolder) {
				return handler.onMoveFiles(files, toFolder);
			}

			$compile(template)(scope).appendTo(element);
		}
	};
}]);

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
