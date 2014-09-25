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

tinyMCE.baseURL = "lib/tinymce/";
tinyMCE.suffix = ".min";

ui.formInput('Html', {

	css: "html-item",

	init: function(scope) {

		scope.parse = function(value) {
			return value;
		};
	},
	
	link: function(scope, element, attrs, model) {
		
		var selector = "#" + element.attr('id') + ' > textarea';
		
		var liteOptions = {
			
			height: 250,
			
			menubar: false,
			
			statusbar : false,
			
			plugins: "hr link image code table textcolor",
			
			toolbar: "undo redo |" +
			        " bold italic underline strikethrough |" +
			        " forecolor backcolor |" +
					" alignleft aligncenter alignright alignjustify |" +
					" numlist bullist outdent indent | link image"
		};
		
		var language = __appSettings['user.lang'] || '';
		if (language === 'fr') language = 'fr_FR';
		if (language === 'en') language = null;
		
		var heavyOptions = {
			
			height: 350,
			
			plugins: "hr link image code table textcolor fullscreen",
		    
			toolbar: "undo redo | styleselect |" +
					" bold italic underline strikethrough |" +
					" forecolor backcolor |" +
					" alignleft aligncenter alignright alignjustify |" +
					" numlist bullist outdent indent | link image | fullscreen"
		};
		
		var options = {
			
			selector: selector,
			
			skin: 'bootstrap',
			
			language: language,
			
			setup: function(editor) {
				
				var elemHtml = element.children('div.html-display-text');
				var rendering = false;

				function showWidget(readonly) {
					if (readonly) {
						editor.hide();
						elemHtml.show();
					} else {
						elemHtml.hide();
						editor.show();
						render();
					}
				}

				function render() {
					var value = scope.getValue() || "",
						html = editor.getContent();

					scope.text = scope.format(value);
					
					if (value === html) {
						return;
					}
					rendering = true;
					editor.setContent(value);
				}

				editor.on('init', function(e) {
					showWidget(scope.isReadonly());
					scope.$watch("isReadonly()", showWidget);
					model.$render = render;
				});

				editor.on('change', function (e) {
					if (rendering) {
						rendering = false;
					} else if (editor.isDirty()) {
						editor.save();
						update(editor.getContent());
					}
				});
			}
		};
		
		options = _.extend(options, scope.field.lite ? liteOptions : heavyOptions);
		options.height = scope.field.height || options.height;

		function textTemplate(value) {
			if (!value || value.trim().length === 0) return "";
			return "<div>" + value + "</div>";
		}
		
		function update(value) {
			var old = scope.getValue();
			var val = scope.parse(value);
			
			if (old === val) {
				return;
			}

			scope.setValue(value, true);
			scope.applyLater();
		}
		
		scope.ajaxStop(function(){
			scope.$evalAsync(function() {
				tinymce.init(options);
			});
		});
	},

	replace: true,

	template_readonly: null,
	
	template_editable: null,
	
	template:
	'<div class="form-item-container">'+
		'<textarea class="html-edit-text" style="display: none;"></textarea>'+
		'<div class="html-display-text" ui-bind-template x-text="text" x-locals="record" x-live="field.live"></div>'+
	'</div>'
});

ui.directive('uiBindTemplate', ['$interpolate', function($interpolate){
	
	function expand(scope, template) {
		if (!template || !template.match(/{{.*?}}/)) {
			return template;
		}
		return $interpolate(template)(scope.locals());
	}
	
	return {
		terminal: true,
		scope: {
			locals: "&",
			text: "=text",
			live: "&"
		},
		link: function(scope, element, attrs) {
			
			var template = null;
			
			function update() {
				var output = expand(scope, template) || "";
				element.html(output);
			}

			scope.$watch("text", function(text, old) {
				
				if (text === template) {
					return;
				}
				template = text;
				update();
			});
			
			var live = false;
			scope.$watch("live()", function(value) {
				if (live || !value) {
					return;
				}
				live = true;
				scope.$watch("locals()", function(value) {
					update();
				}, true);
			});
		}
	};
}]);

})(this);
