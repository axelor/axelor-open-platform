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

	var _params = $scope._viewParams;
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
			var parent = $scope.getCurrentParent();
			if (parent && parent.id) {
				return _domain + "self.parent.id = " + parent.id;
			}
			return _domain + "self.parent is null";
		},
		set: function () {
		}
	});

	$scope.getCurrentHome = function () {
		return _params.currentHome;
	}

	$scope.getCurrentParent = function () {
		var base = $scope.currentFolder || $scope.getCurrentHome();
		if (base && base.id > 0) {
			return _.pick(base, "id");
		}
		return base;
	};

	$scope.addRelatedValues = function (record) {
		// apply details about related object
		var base = $scope.currentFolder;
		if (!base || !base.relatedModel) {
			base = $scope.getCurrentHome();
		}
		if (base) {
			record.relatedId = base.relatedId;
			record.relatedModel = base.relatedModel;
		}
		return record;
	};

	$scope.onEdit = function() {
		var rec = getSelected();
		if (rec && rec.typeIcon === "fa fa-folder") {
			return $scope.onFolder(rec);
		}
	};

	$scope.sync = function () {
	}

	function doReload() {
		var fields = _.pluck($scope.fields, 'name');
		var ds = $scope._dataSource;

		fields.push("relatedId", "relatedModel");

		return ds.search({
			fields: _.unique(fields),
			domain: $scope._domain
		});
	};

	$scope.reload = function () {
		var promise = doReload();
		promise.then(function () {
			return $scope.sync();
		});
	}

	$scope.reloadNoSync = function () {
		return doReload();
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

		return $scope.reloadNoSync();
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
			var parent = $scope.getCurrentParent();
			var record = {
				fileName: value,
				isDirectory: true
			};
			if (parent && parent.id > 0) {
				record.parent = parent;
			}
			record = $scope.addRelatedValues(record);
			var promise = $scope._dataSource.save(record);
			promise.then(done, done);
			promise.success(function (record) {
				$scope.reloadNoSync();
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
			$scope.addRelatedValues(item);
		});

		$scope._dataSource.saveAll(files)
		.success(function (records) {
			$scope.reloadNoSync();
		});
	};

	$scope.onDownload = function () {

		var http = $scope._dataSource._http;
		var records = _.map($scope.selection, function (i) { return $scope.dataView.getItem(i); });
		var ids = _.pluck(_.compact(records), "id");
		if (ids.length === 0) {
			return;
		}

		http.post("ws/dms/download/batch", {
			model: $scope._model,
			records: ids
		})
		.then(function (res) {
			var data = res.data;
			var batchId = data.batchId;
			var batchName = data.batchName;
			if (batchId) {
				$scope.doDownload("ws/dms/download/" + batchId, batchName);
			}
		});
	};

	$scope.onShowRelated = function () {
		var record = getSelected() || {};
		var id = record.relatedId
		var model = record.relatedModel;
		if (id && model && $scope.openTabByName) {
			$scope.openTabByName("form::" + model, {
				"mode": "edit",
				"state": id
			});
		}
	};

	$scope.canShowRelated = function () {
		var record = getSelected();
		return record && !!record.relatedId;
	};

	$scope.onShowMembers = function () {

	}
}

ui.directive('uiDmsUploader', ['$q', function ($q) {

	return function (scope, element, attrs) {

		var input = element.find("input.upload-input");

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

			if (element.is(":hidden")) {
				return;
			}

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
				break;
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

		scope.uploadQueue = []

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
				var file = all[i];
				var info = {
					file: file,
					progress: "0%",
					complete: false
				};
				scope.uploadQueue.push(info);
			}
			processQueue();
		}

		var uploadRunning;

		function processQueue() {
			var promise;
			var queue = scope.uploadQueue;
			if (queue.length === 0 || uploadRunning) {
				return;
			}

			function failed(reason) {
				uploadRunning = false;
				queue.length = 0;
				axelor.notify.error(_t("Upload failed..."));
			}

			function success(info) {
				uploadRunning = false;
				processNext();
			}

			function processNext() {
				var next = _.findWhere(queue, { complete: false });
				if (next) {
					uploadRunning = true;
					promise = uploadSingle(next);
					promise.then(success, failed);
				} else {
					queue.length = 0;
					axelor.notify.info(_t("Upload complete..."));
				}
			}

			function uploadSingle(info) {

				var file = info.file;
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
				var deferred = $q.defer();
				var promise = deferred.promise;

				metaDS.save(record).progress(function(fn) {
					info.progress = (fn > 95 ? 95 : fn) + "%";
				}).success(function(metaFile) {
					var parent = scope.getCurrentParent();
					var record = {
						fileName: metaFile.fileName,
						metaFile: _.pick(metaFile, "id")
					};
					if (parent && parent.id > 0) {
						record.parent = parent;
					}
					record = scope.addRelatedValues(record);
					ds.save(record).success(function (dmsFile) {
						info.progress = "100%";
						info.complete = true;
						deferred.resolve(info);
					}).error(deferred.reject);
				}).error(deferred.reject);

				return promise;
			}

			return processNext();
		}

		input.change(function() {
			scope.applyLater(function () {
				doUpload(input.get(0).files);
			});
		});

		scope.onUpload = function () {
			input.click();
		};

		scope.doDownload = function (url, fileName) {
			var link = document.createElement('a');

			link.onclick = function(e) {
				setTimeout(function () {
					document.body.removeChild(e.target);
				}, 100);
			};

			link.href = url;
			link.download = fileName;
			link.innerHTML = fileName;
			link.style.display = "none";

			document.body.appendChild(link);

			setTimeout(function () {
				link.click();
			}, 300);

			axelor.notify.info(_t("Downloading {0}...", fileName));
		};
	};
}]);

DmsFolderTreeCtrl.$inject = ["$scope", "DataSource"];
function DmsFolderTreeCtrl($scope, DataSource) {

	var ds = DataSource.create("com.axelor.dms.db.DMSFile");

	$scope.folders = {};
	$scope.rootFolders = [];

	function syncFolders(records) {

		var home = $scope.getCurrentHome();
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

		home = home || {
			open: true,
			active: true,
			fileName: _t("Home")
		};

		home.open = true;
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

	function getDomain() {
		var params = $scope._viewParams;
		var domain = params.domain || "";
		var home = $scope.getCurrentHome();
		if (domain) {
			domain = domain + " AND ";
		}
		domain += "self.isDirectory = true";
		if (home && home.id > 0) {
			domain += " AND self.id != " + home.id;
		}
		if (home && home.relatedModel) {
			domain += " AND self.relatedModel = '" + home.relatedModel + "'";
		}
		if (home && home.relatedId) {
			domain += " AND self.relatedId = " + home.relatedId;
		}
		return domain;
	}

	$scope.sync = function () {
		return ds.search({
			fields: ["fileName", "parent.id", "relatedId", "relatedModel"],
			domain: getDomain(),
			limit: -1,
		}).success(syncFolders);
	};

	$scope.showTree = true;

	$scope.onToggleTree = function () {
		$scope.showTree = !$scope.showTree;
		axelor.$adjustSize();
	};

	$scope.onFolderClick = function (node) {
		if (!node || !node.id || node.home) {
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

			function syncHome(record) {
				var home = scope.getCurrentHome();
				if (home && home.id > 0) return;
				if (home && record && record.parent) {
					home.id = record.parent.id;
				}
			}

			scope._dataSource.on("on:save", function (e) {
				syncHome(scope._dataSource.at(0));
				return scope.sync();
			});
			scope._dataSource.on("on:remove", function () {
				return scope.sync();
			});

			scope.sync();
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
		"<a href='javascript:' ng-click='onClick($event, node)' ng-class='{active: node.active}'>" +
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

// members popup
ui.directive("uiDmsMembersPopup", ["$compile", function ($compile) {
	return {
		link: function (scope, element, attrs) {

			var form = null;

			scope.canShare = function () {
				if (!scope.selection || scope.selection.length === 0) return false;
				var selected = _.first(scope.selection);
				var record = scope.dataView.getItem(selected);
				return record && record.canShare;
			};

			scope.onPermissions = function () {

				if (form === null) {
					form = $compile("<div ui-dms-members-form></div>")(scope.$new(true));
					form.appendTo(element);
					form.width("100%");
				}

				var selected = _.first(scope.selection);
				var record = scope.dataView.getItem(selected);

				var formScope = form.scope();

				formScope.doRead(record.id).success(function (rec) {
					formScope.edit(rec);
					setTimeout(function () {
						element.dialog("option", "height", 320);
						element.dialog("open");
					});
				});
			};

			scope.onSavePermissions = function () {

				var ds = scope._dataSource._new("com.axelor.dms.db.DMSPermission");
				var formScope = form.scope();

				if (!formScope.isValid()) {
					return axelor.notify.error(_t("Invalid permissions"));
				}

				var record = formScope.record;
				var original = formScope.$$original.permissions || [];

				var toSave = _.map(record.permissions, function (item) {
					item.file = _.pick(record, "id");
					return item;
				});

				var toRemove = _.filter(original, function (item) {
					return !_.findWhere(toSave, { id: item.id });
				});

				function doClose() {
					element.dialog("close");
					formScope.edit(null);
				}

				var promise = null;
				if (toRemove.length) {
					promise = ds._request('removeAll').post({
						records: toRemove
					});
				}
				if (toSave.length) {
					promise = promise ? promise.then(function () {
						return ds.saveAll(toSave);
					}) : ds.saveAll(toSave);
				}

				if (promise) {
					promise.then(doClose);
				} else {
					doClose();
				}
			};

			scope.$on("$destroy", function () {
				if (form) {
					form.scope().$destroy();
					form = null;
				}
			});
		},
		replace: true,
		template:
			"<div ui-dialog x-on-ok='onSavePermissions' x-css='ui-dialog-small dms-permission-popup' title='Permissions'></div>"
	};
}]);

ui.directive("uiDmsMembersForm", function () {
	return {
		scope: true,
		controller: ["$scope", "$element", 'DataSource', 'ViewService', function($scope, $element, DataSource, ViewService) {
			$scope._viewParams = {
				action: _.uniqueId('$act'),
				title: _t("Permissions"),
				model: "com.axelor.dms.db.DMSFile",
				viewType: "form",
				views: [{
					type: "form",
					name: "dms-file-permission-form"
				}]
			};
			ViewCtrl.call(this, $scope, DataSource, ViewService);
			FormViewCtrl.call(this, $scope, $element);

			$scope.setEditable();
			$scope.onHotKey = function (e) {
				e.preventDefault();
				return false;
			};
		}],
		link: function (scope, element, attrs) {

		},
		template: "<div ui-view-form x-handler='true'></div>"
	};
});

// attachment popup
ui.directive("uiDmsPopup", ['$compile', function ($compile) {

	return {
		scope: true,
		controller: ["$scope", 'DataSource', 'ViewService', function($scope, DataSource, ViewService) {

			$scope._isPopup = true;
			$scope._viewParams = {
				action: _.uniqueId('$act'),
				model: "com.axelor.dms.db.DMSFile",
				viewType: "grid",
				views: [{
					type: "grid",
					name: "dms-file-grid"
				}]
			};

			ViewCtrl.apply(this, arguments);

			var ds = DataSource.create("com.axelor.dms.db.DMSFile");

			$scope.findHome = function (forScope, success) {

				var home = {};
				var record = forScope.record;

				function objectName() {
					return _.humanize(_.last(forScope._model.split(".")));
				}

				function findName() {
					for (var name in forScope.fields) {
						if (forScope.fields[name].nameColumn) {
							return record[name];
						}
					}
					return record.name || _.lpad(record.id, 5, '0');
				}

				var domain = "self.isDirectory = true AND self.relatedId = :rid AND self.relatedModel = :rmodel";

				domain = "" +
						"self.isDirectory = true AND " +
						"self.relatedId = :rid AND " +
						"self.relatedModel = :rmodel AND " +
						"self.parent.relatedModel = :rmodel AND " +
						"(self.parent.relatedId is null OR self.parent.relatedId = 0)";

				var context = {
					"rid": record.id,
					"rmodel": forScope._model
				};

				function process(home) {
					if (!home) {
						home = {};
						home.id = -1;
						home.fileName = findName();
						home.relatedModel = forScope._model;
						home.relatedId = record.id;
					}
					home.home = true;
					home.open = true;
					home.active = true;
					return home;
				}

				ds.search({
					limit: 1,
					fields: ['fileName', 'relatedModel', 'relatedId'],
					domain: domain,
					context: context
				}).success(function (records) {
					success(process(_.first(records)));
				});
			};

			$scope.countAttachments = function (forScope, done) {
				var ds = DataSource.create("com.axelor.meta.db.MetaAttachment");
				var record = forScope.record;
				var domain = "self.objectName = :name AND self.objectId = :id";
				var context = {name: forScope._model, id: record.id };
				var promise = ds.search({
					fields: ['id'],
					domain: domain,
					context: context
				});

				promise.success(function (records) {
					record.$attachments = _.size(records);
				});

				return promise.then(done, done);
			};
		}],
		link: function (scope, element, attrs) {

			setTimeout(function () {
				var elemDialog = element.parent();
				var elemTitle = elemDialog.find('.ui-dialog-title');
				$('<a href="#" class="ui-dialog-titlebar-max"><i class="fa fa-expand"></i></a>').click(function (e) {
					$(this).children('i').toggleClass('fa-expand fa-compress');
					elemDialog.toggleClass('maximized');
					axelor.$adjustSize();
					return false;
				}).insertAfter(elemTitle);

				var height = $(window).height();
				height = Math.min(480, height);
				element.dialog('option', 'height', height);
			});

			scope.showPopup = function (forScope) {
				scope.findHome(forScope, function (home) {
					scope._viewParams.currentHome = home;
					var content = "<div ng-include='\"partials/views/dms-file-list.html\"'></div>";
					content = $compile(content)(scope);
					content.appendTo(element);
					setTimeout(function () {
						element.dialog("open");
					});
				});
			};

			scope.onHotKey = function (e, action) {
				var elem = element.find(".grid-view:first");
				var viewScope = elem.scope();
				if (viewScope && viewScope.onHotKey) {
					return viewScope.onHotKey(e, action);
				}
			};

			scope.onClose = function () {
				scope.countAttachments(scope.$parent.$parent, function () {
					scope.$destroy();
				});
			};
		},
		replace: true,
		template: "<div ui-dialog x-on-ok='false' x-on-close='onClose' class='dms-popup' title='Attachments'></div>"
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
