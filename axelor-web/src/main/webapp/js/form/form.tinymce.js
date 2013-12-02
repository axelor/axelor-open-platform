/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
(function(){

var ui = angular.module('axelor.ui');

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
