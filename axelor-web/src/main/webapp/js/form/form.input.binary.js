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
		var height = field.height || 140;
		
		scope.styles = [{
			'min-width' : width
		}, {
			'width': width,
			'height': height
		}];
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

	link_editable: function(scope, element, attrs, model) {

		var input = element.children('input:first');
		var image = element.children('img:first');
		var buttons = element.children('.btn-group');
		var BLANK = this.BLANK;

		input.add(buttons).hide();
		image.add(buttons).hover(function(){
			buttons.show();
		}, function() {
			buttons.hide();
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
			image.get(0).src = null;
			image.get(0).src = BLANK;
			input.val(null);
			update(null);
		};

		input.change(function(e, ui) {
			var file = input.get(0).files[0];
			var reader = new FileReader();

			reader.onload = function(e){
				var content = e.target.result;
				image.get(0).src = content;
				update(content);
			};

			reader.readAsDataURL(file);
		});

		function update(value) {
			scope.applyLater(function(){
				model.$setViewValue(value);
			});
		}

		scope.$render_editable = function() {
			var content = model.$viewValue || null;
			if (!content) {
				image.get(0).src = BLANK;
			}
			image.get(0).src = content || BLANK;
		};
	},
	template_editable:
	'<div ng-style="styles[0]">' +
		'<input type="file" accept="image/*">' +
		'<img class="img-polaroid" ng-style="styles[1]" style="display: inline-block;">' +
		'<div class="btn-group">' +
			'<button ng-click="doSelect()" class="btn" type="button"><i class="fa fa-arrow-circle-up"></i></button>' +
			'<button ng-click="doSave()" class="btn" type="button"><i class="fa fa-arrow-circle-down"></i></button>' +
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
			'<button ng-click="doSave()" class="btn" type="button"><i class="fa fa-arrow-circle-down"></i></button>' +
			'<button ng-click="doRemove()" class="btn" type="button"><i class="fa fa-times"></i></button>' +
		'</div>' +
	'</div>'
});

})(this);
