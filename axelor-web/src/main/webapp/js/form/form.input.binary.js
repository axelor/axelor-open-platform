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

var BLANK = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
var META_FILE = "com.axelor.meta.db.MetaFile";
var META_JSON_RECORD = "com.axelor.meta.db.MetaJsonRecord";

function makeURL(model, field, recordOrId, version, scope, parentId) {
  var value = recordOrId;
  if (!value) return null;
  var id = value.id ? value.id : value;
  var ver = version;
  if (ver === undefined || ver === null) ver = value.version;
  if (ver === undefined || ver === null) ver = value.$version;
  if (ver === undefined || ver === null) ver = (new Date()).getTime();
  if (!id || id <= 0) return null;
  var url = "ws/rest/" + model + "/" + id + "/" + field + "/download?v=" + ver;
  if (scope) {
    if ((!parentId || parentId < 0) && scope.record) {
      parentId = scope.record.id;
      if ((!parentId || parentId < 0) && scope.field && scope._jsonContext && scope._jsonContext.$record) {
        parentId = scope._jsonContext.$record.id;
      }
    }
    if (parentId > 0) {
      url += "&parentId=" + parentId;
    }
    url += "&parentModel=" + scope._model;
  }
  return url;
}

ui.makeImageURL = makeURL;

ui.formInput('ImageLink', {
  css: 'image-item',
  cssClass: 'from-item image-item',
  metaWidget: true,
  controller: ['$scope', '$element', '$interpolate', function($scope, $element, $interpolate) {

    $scope.parseText = function(text) {
      if (!text) return BLANK;
      if (!text.match(/{{.*?}}/)) {
        return text;
      }
      return $interpolate(text)($scope.record);
    };
  }],

  init: function(scope) {
    var field = scope.field;

    var width = field.width || 140;
    var height = field.height || 'auto';

    scope.styles = [{
      'width': width,
      'max-width': '100%',
      'max-height': '100%'
    }, {
      'width': width,
      'height': height,
      'max-width': '100%',
      'max-height': '100%'
    }];

    if (field.noframe) {
      _.extend(scope.styles[1], {
        border: 0,
        padding: 0,
        background: 'none',
        boxShadow: 'none'
      });
    }
  },

  link_readonly: function(scope, element, attrs, model) {

    var image = element.children('img:first');
    var update = scope.$render_readonly = function () {
      if (scope.isReadonly()) {
        image.get(0).src = scope.parseText(model.$viewValue) || BLANK;
      }
    };

    scope.$watch("record.id", update);
    scope.$watch("record.version", update);
    scope.$watch("isReadonly()", update);
  },
  template_editable: '<input type="text">',
  template_readonly:
    '<div ng-style="styles[0]">'+
      '<img class="img-polaroid" ng-style="styles[1]">'+
    '</div>'
});

ui.formInput('Image', 'ImageLink', {

  init: function(scope) {
    this._super.apply(this, arguments);

    var field = scope.field;
    var isBinary = field.serverType === 'binary';

    if (!isBinary && field.target !== META_FILE) {
      throw new Error("Invalid field type for Image widget.");
    }

    scope.parseText = function (value) {
      return scope.getLink(value);
    };

    scope.getLink = function (value) {
      var record = scope.record || {};
      var model = scope._model;
      if (value === null) return BLANK;
      if (isBinary) {
        if (value) {
          return value;
        }
        if (record.id) {
          return makeURL(model, field.name, record, undefined, scope) + "&image=true";
        }
        return BLANK;
      }
      return value ? makeURL(META_FILE, "content", (value.id || value), value.version || value.$version, scope) : BLANK;
    };
  },

  link_editable: function(scope, element, attrs, model) {

    var field = scope.field;
    var input = element.children('input:first');
    var image = element.children('img:first');
    var buttons = element.children('.btn-group');

    var isBinary = field.serverType === 'binary';
    var timer = null;

    input.add(buttons).hide();
    element.on('mouseenter', function (e) {
      if (timer) clearTimeout(timer);
      timer = setTimeout(function () {
        buttons.slideDown();
      }, 500);
    });
    element.on('mouseleave', function (e) {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      buttons.slideUp();
    });

    scope.doSelect = function() {
      input.click();
    };

    scope.doSave = function() {
      var content = image.get(0).src;
      if (content) {
        window.open(content);
      }
    };

    scope.doRemove = function() {
      image.get(0).src = "";
      input.val(null);
      update(null);
    };

    input.change(function(e, ui) {
      var file = input.get(0).files[0];
      var uploadSize = +(axelor.config["file.upload.size"]) || 0;

      // reset file selection
      input.get(0).value = null;

      if (!file) {
        return;
      }

      if(uploadSize > 0 && file.size > 1048576 * uploadSize) {
        return axelor.dialogs.say(_t("You are not allow to upload a file bigger than") + ' ' + uploadSize + 'MB');
      }

      if (!isBinary) {
        return doUpload(file);
      }

      var reader = new FileReader();
      reader.onload = function(e) {
        update(e.target.result, file.name);
      };

      reader.readAsDataURL(file);
    });

    function doUpload(file) {
      var ds = scope._dataSource._new(META_FILE);
      var value = field.target === META_FILE ? (scope.getValue()||{}) : {};
      var record = {
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size,
        id: value.id,
        version: value.version || value.$version
        };

      record.$upload = {
          file: file
        };

      ds.save(record).success(function (saved) {
        update(saved);
      });
    }

    function doUpdate(value) {
      image.get(0).src = scope.getLink(value);
      model.$setViewValue(getData(value));
    }

    function update(value) {
      scope.$applyAsync(function() {
        doUpdate(value);
      });
    }

    function getData(value) {
      if (!value || isBinary) {
        return value;
      }
      return {
        id: value.id
      };
    }

    var updateLink = scope.$render_editable = function() {
      image.get(0).src = scope.getLink(model.$viewValue);
    };

    scope.$watch("record.id", updateLink);
    scope.$watch("record.version", updateLink);

    if (field.accept) {
      input.attr('accept', field.accept);
    }
  },
  template_editable:
  '<div ng-style="styles[0]" class="image-wrapper">' +
    '<input type="file" accept="image/*">' +
    '<img class="img-polaroid" ng-style="styles[1]" style="display: inline-block;">' +
    '<div class="btn-group">' +
      '<button ng-click="doSelect()" class="btn" type="button"><i class="fa fa-arrow-circle-up"></i></button>' +
      '<button ng-click="doRemove()" class="btn" type="button"><i class="fa fa-times"></i></button>' +
    '</div>' +
  '</div>'
});

ui.formInput('Binary', {

  css: 'file-item',
  cellCss: 'form-item file-item',

  link: function(scope, element, attrs, model) {

    var field = scope.field;
    var input = element.children('input:first').hide();

    scope.doSelect = function() {
      input.click();
    };

    scope.doSave = function() {
      var record = scope.record;
      var model = scope._model;
      var url = makeURL(model, field.name, record, undefined, scope);
      ui.download(url, record.fileName || field.name);
    };

    scope.doRemove = function() {
      var record = scope.record;
      input.val(null);
      model.$setViewValue(null);
      record.$upload = null;
      if(scope._model === META_FILE) {
        record.fileName = null;
        record.fileType = null;
      }
      record.fileSize = null;
    };

    scope.canDownload = function() {
      var record = scope.record || {};
      if (!record.id) return false;
      if (scope._model === META_FILE) {
        return !!record.fileName;
      }
      return true;
    };

    input.change(function(e) {
      var file = input.get(0).files[0];
      var record = scope.record;

      // reset file selection
      input.get(0).value = null;

      if (file) {
        record.$upload = {
          field: field.name,
          file: file
        };
        if(scope._model === META_FILE) {
          record.fileName = file.name;
        }
        scope.$applyAsync(function() {
          record.fileType = file.type;
          record.fileSize = file.size;
        });
      }
    });

    if (field.accept) {
      input.attr('accept', field.accept);
    }
  },
  template_readonly: null,
  template_editable: null,
  template:
  '<div>' +
    '<input type="file">' +
    '<div class="btn-group">' +
      '<button ng-click="doSelect()" ng-show="!isReadonly()" class="btn" type="button"><i class="fa fa-arrow-circle-up"></i></button>' +
      '<button ng-click="doSave()" ng-show="canDownload()" class="btn" type="button"><i class="fa fa-arrow-circle-down"></i></button>' +
      '<button ng-click="doRemove()" ng-show="!isReadonly()" class="btn" type="button"><i class="fa fa-times"></i></button>' +
    '</div>' +
  '</div>'
});

ui.formInput('BinaryLink', {

  css: 'file-item',
  cellCss: 'form-item file-item',
  metaWidget: true,

  link: function(scope, element, attrs, model) {
    scope.prepareTemplate = true;

    var field = scope.field;
    var input = element.children('input:first').hide();

    if (field.target !== META_FILE) {
      throw new Error("BinaryLink widget can be used with MetaFile field only.");
    }

    scope.doSelect = function() {
      input.click();
    };

    scope.doRemove = function() {
      input.val(null);
      scope.setValue(null, true);
    };

    scope.canDownload = function() {
      var value = model.$viewValue;
      return value && value.id > 0;
    };

    scope.format = function (value) {
      if (value) {
        return value.fileName;
      }
      return value;
    };

    scope.doSave = function() {
      var value = model.$viewValue;
      var version = value ? (value.version || value.$version) : undefined;
      var url = makeURL(META_FILE, "content", value, version, scope);
      ui.download(url, scope.text);
    };

    input.change(function(e) {
      var file = input.get(0).files[0];

      // reset file selection
      input.get(0).value = null;

      if (!file) {
        return;
      }

      var ds = scope._dataSource._new(META_FILE);
      var value = scope.getValue() || {};
      var record = _.extend({
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size
      }, {
        id: value.id,
        version: value.version || value.$version
      });

      record.$upload = {
        file: file
      };

      ds.save(record).success(function (rec) {
        scope.setValue(rec, true);
      });
    });

    if (field.accept) {
      input.attr('accept', field.accept);
    }
  },
  template_readonly: null,
  template_editable: null,
  template:
  '<div>' +
    '<input type="file">' +
    '<div class="btn-group">' +
      '<button ng-click="doSelect()" ng-show="!isReadonly()" class="btn select" type="button"><i class="fa fa-arrow-circle-up"></i></button>' +
      '<button ng-click="doRemove()" ng-show="canDownload() && !isReadonly()" class="btn" type="button"><i class="fa fa-times"></i></button>' +
    '</div> ' +
    '<a ng-show="text" href="javascript:" ng-click="doSave()">{{text}}</a>' +
  '</div>'
});

})();
