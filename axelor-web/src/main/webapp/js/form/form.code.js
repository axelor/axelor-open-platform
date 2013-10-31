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

// configure ace path
if (!ace.config.get('modePath')) {
	ace.config.set('modePath', 'lib/ace/js');
	ace.config.set('themePath', 'lib/ace/js');
	ace.config.set('workerPath', 'lib/ace/js');
}

ui.formInput('CodeEditor', {

	css: "code-editor",

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
		editor.renderer.setShowGutter(true);

		editor.commands.addCommands([{
		    name: "unfind",
		    bindKey: {
		        win: "Ctrl-F",
		        mac: "Command-F"
		    },
		    exec: function(editor, line) {
		        return false;
		    },
		    readOnly: true
		}]);
		editor.commands.addCommands([{
		    name: "unreplace",
		    bindKey: {
		        win: "Ctrl-R",
		        mac: "Command-R"
		    },
		    exec: function(editor, line) {
		        return false;
		    },
		    readOnly: true
		}]);
		
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
		scope.$watch('isReadonly()', function readonly(value){
			editor.setReadOnly(value);
			editor.setHighlightActiveLine(!value);
			editor.renderer.setHighlightGutterLine(!value);
		});
		
		function resize() {
			editor.resize();
		}

		scope.$on("on:edit", resize);
		element.on('adjustSize', function(){
			if (element.is(':visible')) {
				element.height(props.height);
				element.width('auto');
				editor.renderer.updateFull();
				resize();
			}
		});

		element.resizable({
			handles: 's',
			resize: resize
		});
	},

	replace: true,

	transclude: true,
	
	template_editable: null,
	
	template_readonly: null,
	
	template: '<div style="min-height: 280px;" class="webkit-scrollbar-all" ng-transclude></div>'
});

})(this);