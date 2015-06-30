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

var ui = angular.module('axelor.ui');

ui.formInput('Spreadsheet', {

	css: "spreadsheet-item",

	link: function (scope, element, attrs, model) {

		var field = scope.field;
		var height = field.height || 400;

		element.height(height).css("overflow", "hidden");

		var inst = new Handsontable(element[0], {
			rowHeaders: true,
			colHeaders: true,
			contextMenu: true,
			afterChange: function (change, source) {
				if (!inst || source === 'loadData') { return; }
				var value = JSON.stringify(inst.getData());
				var current = model.$viewValue;
				if (value === current) {
					return;
				}
				scope.setValue(value, true);
				scope.applyLater();
			}
		});

		model.$render = function () {
			var value = null;
			try {
				value = JSON.parse(model.$viewValue) || null;
			} catch (e) {
			}
			inst.loadData(value || null);
		};

		scope.$on("$destroy", function () {
			if (inst) {
				inst.destroy();
				inst = null;
			}
		});

		scope.$watch("isReadonly()", function (readonly) {
			inst.updateSettings({
				readOnly: !!readonly
			});
		});

		scope.$timeout(function () {
			inst.render();
		});
	},
	template_editable: null,
	template_readonly: null,
	template:
		"<div></div>"
});

})(this);
