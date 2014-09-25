/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

ui.formInput('ImageLink', {
	css: 'image-item',
	cssClass: 'from-item image-item',

	BLANK: "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",

	controller: ['$scope', '$element', '$interpolate', function($scope, $element, $interpolate) {

		$scope.parseText = function(text) {
			if (!text|| !text.match(/{{.*?}}/)) {
				return text;
			}
			return $interpolate(text)($scope.record);
		};
	}],

	init: function(scope) {
		var field = scope.field;

		var width = field.width || 140;
		var height = field.height || '100%';
		
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

		var BLANK = this.BLANK;
		var image = element.children('img:first');

		scope.$render_readonly = function() {
			var content = model.$viewValue || null;
			if (!content) {
				image.get(0).src = BLANK;
			}
			image.get(0).src = scope.parseText(content) || BLANK;
		};

		scope.$watch('isReadonly()', function(readonly, old) {
			if (!readonly || readonly === old) return;
			scope.$render_readonly();
		});
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
		var BLANK = this.BLANK;
		var META_FILE = "com.axelor.meta.db.MetaFile";
		
		if (!isBinary && field.target !== META_FILE) {
			throw new Error("Invalid field type for Image widget.");
		}

		scope.parseText = function (value) {
			return scope.getLink(value);
		};

		scope.getLink = function (value) {
			if (!value || isBinary) {
				return value || BLANK;
			}
			return "ws/rest/" + META_FILE + "/" + (value.id || value) + "/content/download";
		};
	},

	link_editable: function(scope, element, attrs, model) {

		var field = scope.field;
		var input = element.children('input:first');
		var image = element.children('img:first');
		var buttons = element.children('.btn-group');
		var META_FILE = "com.axelor.meta.db.MetaFile";
		
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
			var uploadSize = scope.$eval('app.fileUploadSize');
			
			if (!file) {
				return;
			}

			if(file.size > 1048576 * parseInt(uploadSize)) {
				return axelor.dialogs.say(_t("You are not allow to upload a file bigger than") + ' ' + uploadSize + 'MB');
			}
			
			if (!isBinary) {
				return doUpload(file);
			}

			var reader = new FileReader();
			reader.onload = function(e) {
				doUpdate(e.target.result, file.name);
			};

			reader.readAsDataURL(file);
		});
		
		function doProgress(n) {

		}
		
		function doUpload(file) {

			if (!scope.record || !scope.record.id) {
				return;
			}

			var ds = scope._dataSource._new(META_FILE);
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

			ds.save(record).progress(doProgress).success(function (saved) {
				update(saved);
			});
		}

		function doUpdate(value) {
			image.get(0).src = scope.getLink(value);
			model.$setViewValue(getData(value));			
		}

		function update(value) {
			scope.applyLater(function() {
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

		scope.$render_editable = function() {
			image.get(0).src = scope.getLink(model.$viewValue);
		};
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

	link_editable: function(scope, element, attrs, model) {

		var field = scope.field;
		var input = element.children('input:first').hide();
		var frame = element.children("iframe").hide();

		scope.doSelect = function() {
			input.click();
		};

		scope.doSave = function() {
			var record = scope.record,
				model = scope._model;

			var url = "ws/rest/" + model + "/" + record.id + "/" + field.name + "/download";
			frame.attr("src", url);
			setTimeout(function(){
				frame.attr("src", "");
			},100);
		};

		scope.doRemove = function() {
			var record = scope.record;
			input.val(null);
			model.$setViewValue(null);
			record.$upload = null;
		};

		input.change(function(e) {
			var file = input.get(0).files[0];
			var record = scope.record;
			if (file) {
				record.$upload = {
					field: field.name,
					file: file
				};
				//Update file and mime just in case of new record
				if(!record.id && scope._model == 'com.axelor.meta.db.MetaFile'){
					record.fileName = file.name;
					record.mime = file.type;
				}
				record.size = file.size;
				scope.applyLater(function(){
					model.$setViewValue(0); // mark form for save
				});
			}
		});

	},
	template_editable:
	'<div>' +
		'<iframe></iframe>' +
		'<input type="file">' +
		'<div class="btn-group">' +
			'<button ng-click="doSelect()" class="btn" type="button"><i class="fa fa-arrow-circle-up"></i></button>' +
			'<button ng-click="doRemove()" class="btn" type="button"><i class="fa fa-times"></i></button>' +
		'</div>' +
	'</div>'
});

})(this);
