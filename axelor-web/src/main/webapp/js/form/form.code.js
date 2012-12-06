(function(){

var ui = angular.module('axelor.ui');

var CodeEditor = {
	css: "code-editor",
	require: '?ngModel',
	scope: true,
	link: function(scope, element, attrs, model) {
		
		var field = scope.getViewDef(element);
		var editor = ace.edit(element.get(0));
		var session = editor.getSession();
		var props = _.extend({
			syntax: 'text',
			theme : "textmate",
			fontSize: '14px',
			readonly: false,
			height: 280
		}, field);

		session.setMode("ace/mode/" + props.syntax);
		editor.setTheme('ace/theme/' + props.theme);
		editor.setFontSize(props.fontSize);
		editor.setShowPrintMargin(false);
		editor.setReadOnly(props.readonly);
		
		var loadingText = false;
		model.$render = function() {
			loadingText = true;
			session.setValue(model.$viewValue || "");
			loadingText = false;
		};
		
		editor.on('change', function(e){
			if (loadingText)
				return;
			setTimeout(function(){
				scope.$apply(function(){
					model.$setViewValue(session.getValue());
				});
			});
		});
		attrs.$observe('readonly', function(value){
			editor.setReadOnly(value);
		});
		
		function resize() {
			editor.resize();
		}

		scope.$on("on:edit", resize);
		element.on('adjustSize', function(){
			if (element.is(':visible')) {
				element.width('auto');
				resize();
			}
		});

		element.resizable({
			handles: 's',
			resize: resize
		});
	},
	transclude: true,
	replace: true,
	template:
	'<div style="min-height: 280px;" ng-transclude></div>'
};

ui.formDirective('uiCodeEditor', CodeEditor);

})(this);