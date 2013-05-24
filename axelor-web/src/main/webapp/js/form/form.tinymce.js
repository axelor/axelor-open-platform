(function(){

var ui = angular.module('axelor.ui');

ui.formInput('Html', {

	css: "html-item",

	init: function(scope) {

		scope.parse = function(value) {
			if (!value) {
				return null;
			}
			return value.trim() || null;
		};
	},
	
	link: function(scope, element, attrs, model) {
		
		var selector = "#" + element.attr('id') + ' > textarea';
		
		var options = {
			selector: selector,
			height: 350,
			skin: 'bootstrap',
			
			plugins: "hr link image code table textcolor fullscreen",
		    
			toolbar: "undo redo | styleselect |" +
					" bold italic underline strikethrough |" +
					" alignleft aligncenter alignright alignjustify |" +
					" numlist bullist outdent indent | link image | fullscreen",
			
			setup: function(editor) {
				
				editor.on('change', function (e) {
					if (editor.isDirty()) {
						editor.save();
						update(editor.getContent());
					}
				});

				setTimeout(function() {
					
					var rendered = false;
					var elemHtml = element.children('div.html-display-text');
					
					function showWidget(readonly) {
						if (readonly === undefined) return;
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
						var value = model.$viewValue || "",
							html = editor.getContent();
	
						scope.text = scope.format(value);
						
						if (value === html) {
							return;
						}
						editor.setContent(value);
					}

					scope.$watch("isReadonly()", showWidget);
					
					model.$render = function() {
						if (!rendered) {
							rendered = true;
							showWidget(scope.isReadonly());
						}
						render();
					};
				}, 100);
			}
		};
		
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
			setTimeout(function() {
				scope.$apply();
			});
		}
		
		setTimeout(function(){
			tinymce.init(options);
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
