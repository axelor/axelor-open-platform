/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
(function(){

var ui = angular.module('axelor.ui');

var SpaceIndentKeys = {
	Tab: function (cm) {
		var spaces = Array(cm.getOption("indentUnit") + 1).join(" ");
		cm.replaceSelection(spaces, "end", "+input");
	},
	Backspace: function (cm) {
		var cur = cm.getCursor(),
			num = cm.getOption("indentUnit"),
			line = cm.getLine(cur.line),
			space = line.substring(cur.ch - num, cur.ch);
		
		if (space.length > 0 && space.trim() === "") {
			cm.setSelection({line: cur.line, ch: cur.ch - num}, cur);
			cm.replaceSelection("");
			return true;
		}
		return CodeMirror.Pass;
	}
};

ui.formInput('CodeEditor', {

	css: "code-editor",

	link: function(scope, element, attrs, model) {
		
		var editor = null;
		var loading = false;

		var field = scope.field;
		var props = {
			autofocus: true,
			lineNumbers: true
		};

		if (field.mode || field.syntax) {
			props.mode = field.mode || field.syntax;
		}
		
		if (field.theme) {
			props.theme = field.theme || "default";
		}
		
		if (props.mode === "xml") {
			props = _.extend(props, {
				foldGutter : true,
				gutters : ["CodeMirror-linenumbers", "CodeMirror-foldgutter"],
				autoCloseBrackets : true,
				autoCloseTags : true,
				tabSize : 2,
				indentUnit : 2,
				extraKeys : SpaceIndentKeys
			});
		}

		if (field.height) {
			element.height(field.height);
		}

		setTimeout(function () {
			props.readOnly = scope.$$readonly;
			editor = CodeMirror(element.get(0), props);
			readonlySet(props.readOnly);
			editor.setSize('100%', '100%');
			editor.on("change", changed);
		});
		
		scope.$watch('$$readonly', readonlySet);

		model.$render = function() {
			loading = true;
			var val = model.$modelValue;
			if (editor) {
				editor.setValue(val || "");
				editor.clearHistory();
			}
			loading = false;
		};

		model.$formatters.push(function (value) {
			return value || '';
		});

		function readonlySet(readonly) {
			if (editor) {
				editor.setOption('readOnly', readonly ? "nocursor" : false);
			}
		}

		function changed(instance, changedObj) {
			if (loading || !editor) return;
			var value = editor.getValue();
			if (value !== model.$viewValue) {
				model.$setViewValue(value);
			}
			scope.applyLater();
		}

		function resize() {
			if (editor) {
				editor.refresh();
			}
			element.width('');
		}
		
		element.resizable({
			handles: 's',
			resize: resize
		});
	},

	replace: true,

	transclude: true,
	
	template_editable: null,
	
	template_readonly: null,
	
	template: '<div ng-transclude></div>'
});

})(this);