/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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

"use strict";

var ui = angular.module('axelor.ui');

function inputDialog(options, callback) {

  var opts = _.extend({
    value: "",
    title: "",
    titleOK: _t("OK"),
    titleCancel: _t("Cancel")
  }, options);

  var html = "" +
  "<div>" +
    "<input type='text'>" +
  "</div>";

  var dialog;

  function close() {
    if (dialog) {
      dialog.dialog("close");
      dialog = null;
    }
  }

  dialog = axelor.dialogs.box(html, {
    title: opts.title,
    buttons: [{
      'text': opts.titleCancel,
      'class': 'btn',
      'click': close
    }, {
      'text': opts.titleOK,
      'class': 'btn btn-primary',
      'click': submit
    }],
    onOpen: function(e) {
      $(e.target).find('input').val(opts.value);
    }
  })
  .on("keypress", "input", function (e) {
    if (e.keyCode === 13) {
      submit();
    }
  });

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
DMSFileListCtrl.$inject = ['$scope', '$element', 'NavService'];
function DMSFileListCtrl($scope, $element, NavService) {
  ui.GridViewCtrl.call(this, $scope, $element);

  var _params = $scope._viewParams;
  var _domain = $scope._domain || "";
  if (_domain) {
    _domain += " AND ";
  }

  $scope.$emptyMessage = _t("No documents found.");
  $scope.$confirmMessage = function() {
    var strong = function (text, quote) {
      return "<strong>" + (quote ? "<em>\"" + text + "\"</em>" : text) + "</strong>";
    };
    var all = getSelectedAll();
    if (all.length === 1 || $scope.currentFolder) {
      var doc = _.first(all) || $scope.currentFolder;
      return _t('Are you sure you want to delete {0}?', strong(doc.fileName));
    }
    return _t("Are you sure you want to delete the {0} selected documents?", strong(all.length));
  };

  $scope.currentFilter = null;
  $scope.currentFolder = null;
  $scope.currentPaths = [];

  Object.defineProperty($scope, "_domain", {
    get: function () {
      if ($scope.currentFilter && $scope.getCurrentHome()) {
        return _domain + "self.isDirectory = false AND self.parent.id = " + $scope.getCurrentHome().id;
      }
      if ($scope.currentFilter) {
        return _domain + "self.isDirectory = false";
      }
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
  };

  $scope.getCurrentParent = function () {
    var base = $scope.currentFolder || $scope.getCurrentHome();
    if (base && base.id > 0) {
      return _.pick(base, "id");
    }
    return base;
  };

  $scope.addRelatedValues = function (record, baseFolder) {
    // apply details about related object
    var base = baseFolder || $scope.currentFolder;
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
    var record = getSelected();

    if (!record) {
      var records = _.map($scope.selection, function(i) {
        return $scope.dataView.getItem(i);
      });
      if (records.length) {
        record = records[0];
      } else {
        record = $scope.currentFolder;
      }
    }

    if (record) {
      if ($scope._isPopup) {
        $scope.onClose();
      }
      NavService.openTabByName("dms.file", {
        mode: "edit",
        state: record.id
      });
    }
  };

  $scope.sync = function () {
  };

  $scope.reload = function () {
    var fields = _.pluck($scope.fields, 'name');
    var ds = $scope._dataSource;
    var context = $scope.getContext();

    return ds.search({
      fields: _.unique(fields),
      domain: $scope._domain,
      context: context
    });
  };

  $scope._dataSource.on("change", function (e, records) {
    $scope.$broadcast('on:accept-folders', records);
  });

  function resetFilter() {
    $scope.currentFilter = null;
    $scope._dataSource._filter = null;
    $scope._dataSource._domain = null;
    $scope._dataSource._page.from = 0;
    $scope.$broadcast("on:clear-filter-silent");
  }

  var __onShow = $scope.onShow;
  $scope.onShow = function (viewPromise) {
    $scope.waitForActions(function () {
      __onShow.call($scope, viewPromise);
    });
  };

  var __filter = $scope.filter;
  $scope.filter = function (searchFilter) {

    var filter = _.extend({}, searchFilter);
    var fields = $scope.fields || {};

    _.each(["relatedId", "relatedModel", "isDirectory", "metaFile.id"], function (name) {
      fields[name] = fields[name] || { name: name };
    });

    var advance = !_.isEmpty(filter.criteria) || !_.isEmpty(filter._domains);
    if (advance) {
      $scope.currentFilter = filter;
      $scope.currentFolder = null;
      $scope.currentPaths.length = 0;
    } else {
      resetFilter();
    }

    filter._domain = $scope._domain;
    filter._context = $scope.getContext();

    return __filter.call($scope, filter);
  };

  $scope.onFolder = function(folder, currentPaths) {

    // reset filter
    resetFilter();

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
  };

  $scope.onItemClick = function(event, args) {
    var elem = $(event.target);
    $scope.$timeout(function () {
      var record = getSelected();
      if (elem.is('.fa-file')) {
        $scope.onEdit();
        $scope.$applyAsync();
        return;
      }
      if (elem.is('.fa-folder')) return $scope.onFolder(record);
      if (elem.is('.fa-download')) return $scope.onDownload(record);
      if (elem.is('.fa-info-circle')) return $scope.onDetails(record);
      if (elem.is('.fa') && record) {
        if (record.contentType === "html" || record.contentType === "spreadsheet") {
          return $scope.onEditFile(record);
        }
        $scope.onEdit();
        $scope.$applyAsync();
      }
    });
  };

  $scope.onItemDblClick = function(event, args) {
    var elem = $(event.target);
    if (elem.hasClass("fa")) return;
    var record = getSelected();
    if (record.typeIcon === "fa fa-folder") {
      return $scope.onFolder(record);
    }
    setTimeout(function () {
      if (record) {
        if (record.contentType === "html" || record.contentType === "spreadsheet") {
          return $scope.onEditFile(record);
        }
      }
      $scope.onEdit();
      $scope.$applyAsync();
    });
  };

  function getSelected() {
    var index = _.first($scope.selection || []);
    return $scope.dataView.getItem(index);
  }

  function getSelectedAll() {
    return _.map($scope.selection || [], function(i) { return $scope.dataView.getItem(i); });
  }

  function onNew(options, callback) {

    if (!$scope.canCreateDocument(true)) {
      return;
    }

    var opts = _.extend({
      name: _t("New Folder"),
      title: _t("Create folder")
    }, options);

    var count = 1;
    var selected = $scope.getActiveFolder() || {};
    var existing = _.pluck((selected.nodes || []), "fileName");

    existing = existing.concat(_.pluck($scope.dataView.getItems(), "fileName"));

    var name = opts.name;
    while(existing.indexOf(name) > -1) {
      name = opts.name + " (" + (++count) + ")";
    }

    inputDialog({
      value: name,
      title: opts.title,
      titleOK: _t("Create")
    }, function (value, done) {
      var parent = $scope.getCurrentParent();
      var record = _.extend({}, opts.record, {
        fileName: value
      });
      if (parent && parent.id > 0) {
        record.parent = parent;
      }
      record = $scope.addRelatedValues(record);
      var promise = $scope._dataSource.save(record);
      promise.then(done, done);
      promise.success(function (record) {
        $scope.reload();
        callback(record);
      });
    });
  }

  $scope.onNewFolder = function () {
    onNew({
      name: _t("New Folder"),
      title: _t("Create folder"),
      record: {
        isDirectory: true
      }
    }, function (record) {
      $scope.$broadcast('on:accept-folders', [record]);
    });
  };

  $scope.onNewDoc = function () {
    onNew({
      name: _t("New Document"),
      title: _t("Create document"),
      record: {
        isDirectory: false,
        contentType: "html"
      }
    }, function (record) {
      $scope.onEditFile(record);
    });
  };

  $scope.onNewSheet = function () {
    onNew({
      name: _t("New Spreadsheet"),
      title: _t("Create spreadsheet"),
      record: {
        isDirectory: false,
        contentType: "spreadsheet"
      }
    }, function (record) {
      $scope.onEditFile(record);
    });
  };

  $scope.onRename = function () {
    var record = getSelected() || $scope.currentFolder;
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
      var rec = _.pick(record, ['id', 'version', 'fileName']);
      var ds = $scope._dataSource;

      if (record === $scope.currentFolder) {
        ds = ds._new(ds._model);
      }

      var promise = ds.save(rec);
      promise.then(done, done);
      promise.success(function (res) {
        record.version = res.version;
        var folder = $scope.folders[record.id];
        if (folder) {
          folder.version = record.version;
          folder.fileName = record.fileName;
        }
        if ($scope._dataSource === ds) {
          $scope.reload();
        }
      });
    }
  };

  var __onDelete = $scope.onDelete;
  $scope.onDelete = function () {
    var record = getSelected();
    if (record) {
      return __onDelete.apply($scope, arguments);
    }
    record = $scope.currentFolder;
    if (!record || !record.id) {
      return;
    }
    var message = $scope.$confirmMessage();
    axelor.dialogs.confirm(message, function(confirmed) {
      if (!confirmed) {
        return;
      }
      var rec = _.pick(record, ['id', 'version']);
      $scope._dataSource.removeAll([rec]).success(function() {
        $scope.onFolder(record.parent);
      });
    });
  };

  $scope.onMoveFiles = function (files, toFolder) {
    var items = files || [];
    if (items.length === 0) {
      return;
    }

    items = items.map(function (item) {
      var res = _.extend({}, item);
      if (toFolder && toFolder.id) {
        res.parent = {
          id: toFolder.id
        };
      } else {
        res.parent = null;
      }
      // reset related record
      res.relatedId = null;
      res.relatedModel = null;
      return res;
    });

    var folders = items.filter(function (item) { return item.isDirectory; });

    $scope._dataSource.saveAll(items).success(function (records) {
      $scope.$broadcast('on:change-folders', folders);
      setTimeout(function () {
        $scope.reload();
      });
    });
  };

  $scope.onDownload = function () {

    var http = $scope._dataSource._http;
    var records = _.map($scope.selection, function (i) { return $scope.dataView.getItem(i); });
    if (records.length === 0 && $scope.currentFolder) {
      records = [$scope.currentFolder];
    }
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
        ui.download("ws/dms/download/" + batchId, batchName);
      }
    }, function (e) {
      if (e.status == 404) {
        var fname = records.length === 1 ?
          '<strong>' + axelor.sanitize(_.first(records).fileName) + '</strong>' : '';
        axelor.notify.error("<p>" + _t("File {0} does not exist.", fname) + "</p>");
      }
    }
    );
  };

  $scope.onOffline = function () {
    var http = $scope._dataSource._http;
    var record = getSelected() || {};
    if (record.id > 0) {
      http.post("ws/dms/offline", {
        model: $scope._model,
        records: [record.id],
        data: {
          unset: !record.offline
        }
      }).then(function (res) {
        record.offline = !record.offline;
      });
    }
  };

  $scope.onShowRelated = function () {
    var record = getSelected() || {};
    var id = record.relatedId;
    var model = record.relatedModel;
    if (id && model) {
      $scope.$root.openTabByName("form::" + model, {
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

  };

  $scope.canCreateDocument = function (notify) {
    var parent = $scope.currentFolder || $scope.getCurrentHome();
    if (parent && parent.canWrite === false) {
      if (notify) {
        axelor.notify.error(_t("You can't create document here."));
      }
      return false;
    }
    return true;
  };

  $scope.canEditFile = function () {
    var record = getSelected();
    return record && !!record.contentType;
  };

  $scope.onEditFile = function (record) {
    if ($scope._isPopup) {
      $scope.onClose();
    }
    record = record || getSelected();
    var view = {
      action: "$act:dms" + record.id,
      model: $scope._model,
      title: record.contentType === "spreadsheet" ? _t("Spreadsheet") : _t("Document"),
      viewType: "form",
      views: [{
        type: "form",
        width: "large",
        items: [{
          type: "panel",
          items: [{
            type: "panel",
            items: [{
              type: "button",
              title: _t("Save"),
              icon: "fa-save",
              onClick: "save",
              colSpan: "3"
            }, {
              type: "button",
              title: _t("Download"),
              icon: "fa-download",
              onClick: "save,action-dms-file-download",
              colSpan: "3"
            }]}, {
            type: "field",
            name: "content",
            showTitle: false,
            widget: record.contentType || "html",
            colSpan: 12,
            height: 520
          }]
        }]
      }],
      recordId: record.id,
      forceEdit: true,
      params: {
        'show-toolbar': false
      }
    };

    $scope.$root.openTab(view);
    $scope.waitForActions(function () {
      var formScope = view.$viewScope;
      if (formScope) {
        formScope.$on("$destroy", function () {
          if (formScope.record) {
            record.version = formScope.record.version;
          }
        });
      }
    });
  };
}

ui.directive('uiDmsUploader', ['$q', '$http', function ($q, $http) {

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
      if (element.is(e.target) || element.has(e.target).length) {
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

    var uploads = {
      items: [],
      pending: [],
      running: false,
      queue: function (info) {
        info.pending = true;
        info.progress = 0;
        info.transfer = _t("Pending");
        info.abort = function () {
          info.transfer = _t("Cancelled");
          info.pending = false;
        };
        info.retry = function () {
          uploads.queue(info);
          uploads.process();
        };
        if (this.items.indexOf(info) === -1) {
          this.items.push(info);
        }
        if (this.pending.indexOf(info) === -1) {
          this.pending.push(info);
        }
      },
      process: function () {
        if (this.running || this.pending.length === 0) {
          if (_.all(this.items, function (item) { return item.complete; })) {
            this.items.length = 0;
          }
          return;
        }
        this.running = true;
        var info = this.pending.shift();
        while (info && !info.pending) {
          info = this.pending.shift();
        }
        if (!info) {
          this.running = false;
          return;
        }

        var that = this;
        var promise = uploadSingle(info);

        function error(reason) {
          that.running = false;
          info.active = false;
          info.pending = false;
          info.progress = 0;
          info.transfer = reason.message;
          info.failed = true;
          return that.process();
        }

        function success() {
          that.running = false;
          info.active = false;
          info.pending = false;
          info.complete = true;
          info.progress = "100%";
          return that.process();
        }

        return promise.then(success, error);
      }
    };

    // expose uploads
    scope.uploads = uploads;
    scope.onCloseUploadFiles = function() {
      uploads.items.length = 0;
      uploads.pending.length = 0;
    };

    var uploadSize = +(axelor.config["file.upload.size"]) || 0;

    function doUpload(files) {
      if (!scope.canCreateDocument(true)) {
        return;
      }

      var all = files;
      if (files.fileName) {
        files = [files];
      }

      var i, file;
      for (i = 0; i < all.length; i++) {
        file = all[i];
        if (uploadSize > 0 && file.size > 1048576 * uploadSize) {
          return axelor.dialogs.say(_t("You are not allow to upload a file bigger than") + ' ' + uploadSize + 'MB');
        }
      }
      for (i = 0; i < all.length; i++) {
        file = all[i];
        var info = {
          file: file
        };
        uploads.queue(info);
      }
      uploads.process();
    }

    function uploadSingle(info) {
      var deferred = $q.defer();
      var promise = deferred.promise;
      var file = info.file;
      var xhr = new XMLHttpRequest();

      function formatSize(done, total) {
        function format(size) {
          if(size > 1000000000) return parseFloat(size/1000000000).toFixed(2) + " GB";
          if(size > 1000000) return parseFloat(size/1000000).toFixed(2) + " MB";
          if(size >= 1000) return parseFloat(size/1000).toFixed(2) + " KB";
          return size + " B";
        }
        return format(done || 0) + "/" + format(total);
      }

      function doClean() {
        return $http({
          method: "DELETE",
          url: "ws/files/upload/" + info.uuid,
          silent: true,
          transformRequest: []
        });
      }

      function onError(reason) {
        function done() {
          var message = reason && reason.error ? reason.error : _t("Failed");
          deferred.reject({ message: message, failed: true });
        }
        doClean().then(done, done);
      }

      function onCancel(clean) {
        function done() {
          deferred.reject({ message: _t("Cancelled"), cancelled: true });
        }
        return clean ? doClean().then(done, done) : done();
      }

      function onSuccess(meta) {
        var ds = scope._dataSource;
        var parent = scope.getCurrentParent();
        var record = {
          fileName: meta.fileName,
          metaFile: _.pick(meta, "id")
        };
        if (parent && parent.id > 0) {
          record.parent = parent;
        }
        record = scope.addRelatedValues(record);
        ds.save(record).success(function (dmsFile) {
          deferred.resolve(info);
        }).error(onError);
      }

      function onChunk(response) {

        info._start = info._end;
        info._end = Math.min(info._end + info._size, file.size);

        if (response && response.fileId) {
          info.uuid = response.fileId;
        }
        if (response && response.id) {
          return onSuccess(response);
        }

        if (info.loaded) {
          return onError();
        }

        // continue with next chunk
        sendChunk();
        scope.$applyAsync();
      }

      function sendChunk() {
        xhr.open("POST", "ws/files/upload", true);
        xhr.overrideMimeType("application/octet-stream");
        xhr.setRequestHeader("Content-Type", "application/octet-stream");
        xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        xhr.setRequestHeader($http.defaults.xsrfHeaderName, axelor.readCookie($http.defaults.xsrfCookieName));

        if (info.uuid) {
          xhr.setRequestHeader("X-File-Id", info.uuid);
        }

        xhr.setRequestHeader("X-File-Name", encodeURIComponent(file.name));
        xhr.setRequestHeader("X-File-Type", file.type);
        xhr.setRequestHeader("X-File-Size", file.size);
        xhr.setRequestHeader("X-File-Offset", info._start);

        if (info._end > file.size) {
          info._end = file.size;
        }

        var chunk = file.slice(info._start, info._end);

            xhr.send(chunk);
      }

      info.uuid = null;
      info._start = 0;
      info._size = 1000 * 1000; // 1MB
      info._end = info._size;

      info.active = true;
      info.transfer = formatSize(0, file.size);
      info.abort = function () {
        xhr.abort();
        onCancel();
      };
      info.retry = function () {
        // put back on queue
        uploads.queue(info);
        uploads.process();
      };

      xhr.upload.addEventListener("progress", function (e) {
        var total = info._start + e.loaded;
        var done = Math.round((total / file.size) * 100);
        info.progress = done > 95 ? "95%" : done + "%";
        info.transfer = formatSize(total, file.size);
        info.loaded = total === file.size;
        scope.$applyAsync();
      });

      xhr.onreadystatechange = function(e) {
        if (xhr.readyState == 4) {
          switch(xhr.status) {
          case 0:
          case 406:
            onCancel(true);
            break;
          case 200:
            onChunk(xhr.responseText ? angular.fromJson(xhr.responseText) : null);
            break;
          default:
            onError(xhr.responseText ? angular.fromJson(xhr.responseText) : null);
          }
          scope.$applyAsync();
        }
      };

      sendChunk();

      return promise;
    }

    input.change(function() {
      scope.$applyAsync(function () {
        doUpload(input.get(0).files);
        input.val(null);
      });
    });

    scope.onUpload = function () {
      input.click();
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
    home.nodes = _.filter(records, function (item) {
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

  $scope.getActiveFolder = function () {
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
      domain += " AND self.id != " + home.id + " AND self.parent.id = " + home.id;
    } else {
      domain += " AND self.parent is NULL";
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
      fields: ["fileName", "parent.id"],
      domain: getDomain(),
      context: { _populate: false },
      limit: -1
    }).success(syncFolders);
  };

  $scope.showTree = !axelor.device.small;

  $scope.onToggleTree = function () {
    $scope.showTree = !$scope.showTree;
    axelor.$adjustSize();
  };

  $scope.onFolderClick = function (node) {
    if (!node || !node.id || node.home) {
      $scope.onFolder();
      $scope.$applyAsync();
      return;
    }
    var paths = [];
    var parent = node.parent;
    while (parent) {
      paths.unshift(parent);
      parent = parent.parent;
    }
    $scope.onFolder(node, paths);
    $scope.$applyAsync();
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

          dd.helper = proxy;
          return proxy;
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

      scope.$watch("currentFolder", function dmsCurrentFolderWatch(folder) {
        var folders = scope.folders || {};
        var rootFolders = scope.rootFolders || [];
        var id, node;

        for (id in folders) {
          folders[id].active = false;
        }

        (rootFolders[0]||{}).active = false;

        id = (folder||{}).id;
        node = folders[id] || rootFolders[0];

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
      });

      scope._dataSource.on("on:remove", function (e, records) {
        var ids = _.pluck(records, 'id');
        records
          .filter(function (record) { return record.id in scope.folders; })
          .forEach(function (record) {
            var found = scope.folders[record.id];
            var parent = scope.folders[(found.parent||{}).id] || _.first(scope.rootFolders);
            if (parent) {
              parent.nodes = _.filter(parent.nodes, function (node) {
                return ids.indexOf(node.id) === -1;
              });
            }
            delete scope.folders[record.id];
          });
      });

      scope.$on('on:accept-folders', function (e, records) {
        var parent = scope.getActiveFolder() || _.first(scope.rootFolders);
        var folders = records.filter(function (record) {
          return record.isDirectory && !scope.folders[record.id];
        });
        folders.forEach(function (folder) {
          folder.nodes = [];
          folder.parent = parent;
          scope.folders[folder.id] = folder;
          parent.nodes.push(folder);
        });
      });

      scope.$on('on:change-folders', function (e, records) {
        var folders = records.filter(function (record) { return record.isDirectory; });
        // first remove from old parent
        folders.forEach(function (record) {
          var found = scope.folders[record.id];
          var parent = scope.folders[((found||{}).parent||{}).id] || _.first(scope.rootFolders);
          if (parent) {
            parent.nodes = _.filter(parent.nodes, function (node) {
              return node.id !== record.id;
            });
          }
        });
        // and add to new parent
        folders.forEach(function (record) {
          var found = scope.folders[record.id] || (scope.folders[record.id] = record);
          var oldParent = found.parent;
          var newParent = scope.folders[(record.parent||{}).id] || _.first(scope.rootFolders);
          if (found && oldParent !== newParent) {
            newParent.nodes.push(found);
            found.parent = newParent;
          }
          found.nodes = found.nodes || [];
        });
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

      element.children('.highlight').on("xdropstart", function (e, dd) {
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

      element.children('.highlight').on("xdropend", function (e, dd) {
        element.removeClass("dropping");
      });

      element.children('.highlight').on("xdrop", function (e, dd) {
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
    "<a href='' ng-click='onClick($event, node)' ng-class='{active: node.active}'>" +
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
  "<li ng-repeat='node in nodes track by node.id' ng-class='{empty: !node.nodes.length}' class='dms-tree-folder'>" +
    "<a x-node='node' ui-dms-tree-node></a>" +
    "<ul ng-if='node.open' x-handler='handler' x-nodes='node.nodes' ui-dms-tree></ul>" +
  "</li>";

  return {
    scope: {
      nodes: "=",
      handler: "="
    },
    link: function (scope, element, attrs) {
      var handler = scope.handler;

      scope.onClick = function (e, node) {
        if ($(e.target).is("i.handle")) {
          node.open = !node.open;
          return;
        }
        return handler.onFolderClick(node);
      };

      scope.onMoveFiles = function (files, toFolder) {
        return handler.onMoveFiles(files, toFolder);
      };

      $compile(template)(scope).appendTo(element);
    }
  };
}]);

ui.directive("uiDmsDetails", function () {

  return {
    controller: ["$scope", function ($scope) {

      function getUserName(record) {
        if(!record) {
          return null;
        }
        var key = axelor.config["user.nameField"] || "name";
        return record[key] || record.name || record.code;
      }

      function set(record) {
        var info = $scope.details = {};
        if (record) {
          info.id = record.id;
          info.version = record.version;
          info.name = record.fileName;
          info.type = record.isDirectory ? _t("Directory") : record.fileType || _t("Unknown");
          info.tags = record.tags;
          info.owner = getUserName(record.createdBy);
          info.created = moment(record.createdOn).format(ui.dateTimeFormat);
          info.updated = moment(record.updatedOn || record.createdOn).format(ui.dateTimeFormat);
          info.canOffline = !record.isDirectory && (record.metaFile || record['metaFile.id']);
          info.offline = record.offline;
        }
      }

      $scope.tagsFormName = "dms-file-tags-form";
      $scope.showDetails = false;
      $scope.showTagEditor = false;

      $scope.onDetails = function (record) {
        $scope.showDetails = true;
        axelor.$adjustSize();
        set(record);
      };

      $scope.onCloseDetails = function () {
        $scope.showDetails = false;
        $scope.showTagEditor = false;
        axelor.$adjustSize();
      };

      $scope.onAddTags = function () {
        $scope.showTagEditor = true;
      };

      $scope.onSaveTags = function () {
        var ds = $scope._dataSource;
        function doClose(rec) {
          $scope.showTagEditor = false;
          $scope.details.tags = rec.tags;
          $scope.details.version = rec.version;
        }
        var record = _.pick($scope.details, "id", "version", "tags");
        ds.save(record).success(doClose);
      };

      $scope.$watch("selection[0]", function dmsSelectionWatch(index) {
        if (index === undefined || !$scope.showDetails) return;
        var details = $scope.details || {};
        var record = $scope.dataView.getItem(index) || {};
        if (details.id !== record.id) {
          $scope.onCloseDetails();
        }
      });
    }],
    link: function (scope, element, attrs) {
      //XXX: ui-dialog issue
      element.zIndex(element.siblings(".slickgrid").zIndex() + 2);
    }
  };
});

// members popup
ui.directive("uiDmsMembersPopup", ["$compile", function ($compile) {
  return {
    controller: ["$scope", function($scope) {

      $scope.onSavePermissions = function () {
        $scope._onSavePermissions();
      };
    }],
    link: function (scope, element, attrs) {

      var form = null;

      scope.permissionFormName = "dms-file-permission-form";

      Object.defineProperty(scope, 'permissionFormTitle', {
        get: function () {
          return scope.fileName ? _t("Permissions ({0})", scope.fileName) : _t("Permissions");
        }
      });

      function getSelected() {
        var index = _.first(scope.selection || []);
        return scope.dataView.getItem(index) || scope.currentFolder;
      }

      scope.canShare = function () {
        var record = getSelected();
        return record && record.canShare;
      };

      scope.onPermissions = function () {

        if (form === null) {
          form = $("<div ui-dms-inline-form></div>")
            .attr("x-record", "record")
            .attr("x-form-name", "permissionFormName")
            .attr("x-form-title", "permissionFormTitle");
          form = $compile(form)(scope);
          form.appendTo(element);
          form.width("100%");
        }

        var record = getSelected();
        var formScope = form.isolateScope();

        scope.fileName = record.fileName;

        formScope.doRead(record.id).success(function (rec) {
          formScope.edit(rec);
          formScope.setEditable(true);
          setTimeout(function () {
            element.dialog("option", "height", 320);
            element.dialog("option", "title", scope.permissionFormTitle);
            element.dialog("open");
          });
        });
      };

      scope._onSavePermissions = function () {

        var ds = scope._dataSource._new("com.axelor.dms.db.DMSPermission");
        var formScope = form.isolateScope();

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
          scope.fileName = null;
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
          var formScope = form.isolateScope();
          if (formScope) {
            formScope.$destroy();
          }
          form = null;
        }
      });
    },
    replace: true,
    template:
      "<div ui-dialog x-on-ok='onSavePermissions' x-css='ui-dialog-small dms-permission-popup' title='Permissions'></div>"
  };
}]);

ui.directive("uiDmsInlineForm", function () {
  return {
    scope: {
      formName: "=",
      formTitle: "=",
      record: "="
    },
    controller: ["$scope", "$element", 'DataSource', 'ViewService', function($scope, $element, DataSource, ViewService) {
      $scope._viewParams = {
        action: _.uniqueId('$act'),
        title: $scope.formTitle,
        model: "com.axelor.dms.db.DMSFile",
        viewType: "form",
        views: [{
          type: "form",
          name: $scope.formName
        }]
      };
      ui.ViewCtrl.call(this, $scope, DataSource, ViewService);
      ui.FormViewCtrl.call(this, $scope, $element);

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
    scope: {
      onSelect: "&"
    },
    controller: ["$scope", 'DataSource', 'ViewService', 'NavService', function($scope, DataSource, ViewService, NavService) {

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

      ui.ViewCtrl.apply(this, arguments);

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

        var domain = "" +
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
        var ds = DataSource.create("com.axelor.dms.db.DMSFile");
        var record = forScope.record;
        var domain = "self.relatedModel = :name AND self.relatedId = :id " +
            "AND COALESCE(self.isDirectory, FALSE) = FALSE";
        var context = {name: forScope._model, id: record.id };
        var promise = ds.search({
          fields: ['id'],
          domain: domain,
          context: context
        });

        promise.success(function (records) {
          record.$attachments = _.size(records);
          forScope.$broadcast('on:load-messages', record);
        });

        return promise.then(done, done);
      };

      $scope.onClose = function () {
        $scope._onClose();
      };

      if ($scope.onSelect()) {
        $scope.buttons = [{
          text: _t("Select"),
          'class': 'btn btn-primary',
          click: function (e) {
            var viewScope = $(this).find(".grid-view").scope();
            var items = _.map(viewScope.selection, function (i) {
              return viewScope.dataView.getItem(i);
            });
            $scope._onSelectFiles(items);
          }
        }];
      }
    }],
    link: function (scope, element, attrs) {

      scope._onSelectFiles = function (items) {
        scope.$applyAsync(function () {
          var promise = scope.onSelect()(items);
          if (promise && promise.then) {
            promise.then(function () {
              element.dialog("close");
            });
          } else {
            element.dialog("close");
          }
        });
      };

      scope.$evalAsync(function () {
        scope._setTitle(_t('Attachments'));
      });

      scope.onHotKey = function (e, action) {
        var elem = element.find(".grid-view:first");
        var viewScope = elem.scope();
        if (viewScope && viewScope.onHotKey) {
          return viewScope.onHotKey(e, action);
        }
      };

      var formScope = null;

      scope._onClose = function () {
        if (formScope) {
          scope.countAttachments(formScope, function () {
            scope.$destroy();
          });
        } else {
          scope.$destroy();
        }
        formScope = null;
      };

      scope.showPopup = function (forScope) {

        formScope = forScope;

        function doOpen() {
          var content = "<div ng-include='\"partials/views/dms-file-list.html\"'></div>";
          content = $compile(content)(scope);
          content.appendTo(element);
          scope._doShow();
          setTimeout(function () {
            //XXX: ui-dialog issue
            element.find('.filter-box').zIndex(element.zIndex() + 1);
          }, 100);
        }

        if (!formScope) {
          return doOpen();
        }

        scope.findHome(forScope, function (home) {
          scope._viewParams.currentHome = home;
          doOpen();
        });
      };

    },
    replace: true,
    template: "<div ui-dialog ui-dialog-size x-buttons='buttons' x-on-ok='false' x-on-close='onClose' class='dms-popup' title='Attachments'></div>"
  };
}]);

ui.download = function download(url, fileName) {

  function doDownload() {
    var link = document.createElement('a');

    link.innerHTML = name;
    link.download = name;
    link.href = url;

    _.extend(link.style, {
      position: "absolute",
      visibility: "hidden",
      zIndex: 1000000000
    });

    document.body.appendChild(link);

    link.onclick = function(e) {
      setTimeout(function () {
        document.body.removeChild(e.target);
      }, 300);
    };

    setTimeout(function () {
      link.click();
    }, 100);

    var fname = "<strong>" + name + "</strong>";
    axelor.notify.info(_t("Downloading {0}...", fname));
  }

  var name = axelor.sanitize(fileName);
  $.ajax({
    url : url,
    type : 'HEAD',
    success : doDownload,
    error : function (e) {
      if (e.status == 404) {
        var fname = "<strong>" + name + "</strong>";
        axelor.notify.error("<p>" + _t("File {0} does not exist.", fname) + "</p>");
      }
    }
  });
};

// prevent download on droping files
$(function () {
  window.addEventListener("dragover",function(e) {
    e.preventDefault();
  }, false);

  window.addEventListener("drop",function(e) {
    e.preventDefault();
  }, false);
});

})();
