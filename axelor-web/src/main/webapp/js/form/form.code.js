/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

ui.formInput('CodeEditor', {

	css: "code-editor",

	link: function(scope, element, attrs, model) {
		
		var editor = null;
		var loading = false;

		var field = scope.field;
		var props = {
			autofocus: true,
			lineNumbers: true,
			theme: field.codeTheme || "default"
		};

		if (field.mode || field.codeSyntax) {
			props.mode = field.mode || field.codeSyntax;
		}

		if (props.mode === "xml") {
			props = _.extend(props, {
				foldGutter : true,
				gutters : ["CodeMirror-linenumbers", "CodeMirror-foldgutter"],
				autoCloseBrackets : true,
				autoCloseTags : true,
				tabSize : 2,
				indentUnit : 2,
				indentWithTabs: false
			});
		}

		if (field.height) {
			element.height(field.height);
		}

		setTimeout(function () {
			props.readOnly = scope.$$readonly;
			editor = CodeMirror(element.get(0), props);
			model.$render();
			readonlySet(props.readOnly);
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
				editor.setOption('readOnly', _.toBoolean(readonly));
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
		
		element.on('adjustSize', _.debounce(resize));
	},

	replace: true,

	transclude: true,
	
	template_editable: null,
	
	template_readonly: null,
	
	template: '<div ng-transclude></div>'
});

})();
