/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

/* jshint newcap: false */

"use strict";

var ui = angular.module('axelor.ui');

function TableLayout(items, attrs, $scope, $compile) {

	var colWidths = attrs.widths,
		numCols = +attrs.cols || 4,
		curCol = 0,
		layout = [[]];

	function add(item, label) {

		if (item.is('br')) {
			curCol = 0;
			item.hide();
			return layout.push([]);
		}

		var row = _.last(layout),
			cell = null,
			colspan = +item.attr('x-colspan') || 1,
			rowspan = +item.attr('x-rowspan') || 1;

		if (curCol + colspan >= numCols + 1) {
			curCol = 0, row = [];
			layout.push(row);
		}

		if (label) {
			cell = {};
			cell.elem = label;
			cell.css = label.attr('x-cell-css');
			row.push(cell);
			if (rowspan > 1) cell.rowspan = rowspan;
			if (colspan > 1) colspan -= 1;
			curCol += 1;
		}

		cell = {};
		cell.elem = item;
		cell.css = item.attr('x-cell-css');
		if (colspan > 1) cell.colspan = colspan;
		if (rowspan > 1) cell.rowspan = rowspan;

		row.push(cell);
		curCol += colspan;
	}

	if (colWidths && angular.isString(colWidths)) {
		colWidths = colWidths.trim().split(/\s*,\s*/);
		for(var i = 0 ; i < colWidths.length; i++) {
			var width = colWidths[i];
			if (/^(\d+)$/.test(width)) width = width + 'px';
			if (width == '*') width = 'auto';
			colWidths[i] = width;
		}
	}

	items.each(function(){
		var el = $(this),
			title = el.attr('x-title'),
			noTitle = el.attr('x-show-title') == 'false';

		var labelScope = el.data('$scope');
		if (labelScope) {
			labelScope = labelScope.$new();
		}

		if (numCols > 1 && !noTitle && title) {
			var label = $('<label ui-label></label>').html(title).attr('x-for-widget', el.attr('id')),
				labelElem = $compile(label)(labelScope || $scope);
			el.data('label', labelElem);
			return add(el, labelElem);
		}
		add(el);
	});

	var table = $('<table class="form-layout"></table>');

	function isLabel(cell) {
		return cell.css === "form-label" || (cell.elem && cell.elem.is('label,.spacer-item'));
	}

	function computeWidths(row) {
		if (row.length === 1) return null;
		var widths = [],
			labelCols = 0,
			itemCols = 0,
			emptyCols = 0;

		_.each(row, function(cell) {
			if (isLabel(cell)) {
				labelCols += (cell.colspan || 1);
			} else {
				itemCols += (cell.colspan || 1);
			}
		});

		emptyCols = numCols - (labelCols + itemCols);

		labelCols += (emptyCols / 2);
		itemCols += (emptyCols / 2) + (emptyCols % 2);

		var labelWidth = labelCols ? Math.min(50, (12 * labelCols)) / labelCols : 0;
		var itemWidth = (100 - (labelWidth * labelCols)) / itemCols;

		_.each(row, function(cell, i) {
			var width = ((isLabel(cell) ? labelWidth : itemWidth) * (cell.colspan || 1));
			widths[i] = width + "%";
		});

		return widths;
	}

	_.each(layout, function(row){
		var tr = $('<tr></tr>'),
			numCells = 0,
			widths = colWidths || computeWidths(row);

		_.each(row, function(cell, i) {
				var el = $('<td></td>')
					.addClass(cell.css)
					.attr('colspan', cell.colspan)
					.attr('rowspan', cell.rowspan)
					.append(cell.elem)
					.appendTo(tr);
				if (_.isArray(widths) && widths[i]) {
					el.width(widths[i]);
				}
				numCells += cell.colspan || 1;
		});

		// append remaining cells
		for (var i = numCells ; i < numCols ; i++) {
			$('<td></td>').appendTo(tr).width((widths||[])[i]);
		}

		tr.appendTo(table);
	});

	return table;
} //- TableLayout


ui.directive('uiTableLayout', ['$compile', function($compile) {

	return function(scope, element, attrs) {
		var elem = attrs.layoutSelector ? element.find(attrs.layoutSelector) : element;
		var items = elem.children();

		var layout = TableLayout(items, attrs, scope, $compile);
		var brTags = element.children('br:hidden'); // detach all the <br> tags

		scope.$on('$destroy', function(){
			brTags.remove();
		});

		elem.append(layout);
	};

}]);

function PanelLayout(items, attrs, $scope, $compile) {

	var stacked = attrs.stacked || false,
		flexbox = attrs.flexbox || false,
		numCols = 12,
		numSpan = +(attrs.itemSpan) || 6,
		curCol = 0,
		canAddRow = !stacked && !flexbox,
		rowClass = flexbox ? 'panel-flex' : 'row-fluid',
		cellClass = flexbox ? 'flex' : 'span',
		layout = [$('<div>').addClass(rowClass)];

	function add(item, label) {
		var row = _.last(layout),
			cell = $('<div>'),
			span = +item.attr('x-colspan') || numSpan,
			offset = +item.attr('x-coloffset') || 0;

		span = Math.min(span, numCols);
		if (stacked) {
			span = 0;
		}

		if (item.is('.spacer-item')) {
			curCol += (span + offset);
			item.remove();
			return;
		}

		if (curCol + (span + offset) >= numCols + 1 && canAddRow) {
			curCol = 0, row = $('<div>').addClass(rowClass);
			layout.push(row);
		}
		if (label) {
			label.appendTo(cell);
			row.addClass('has-labels');
		}

		cell.addClass(item.attr('x-cell-css'));

		if (span) {
			cell.addClass(cellClass + span);
		}
		if (offset) {
			cell.addClass('offset' + offset);
		}

		cell.append(item);
		cell.appendTo(row);

		curCol += (span + offset);
	}

	items.each(function (item, i) {
		var el = $(this),
			title = el.attr('x-title'),
			noTitle = el.attr('x-show-title') == 'false';

		var labelScope = el.data('$scope');
		if (labelScope) {
			labelScope = labelScope.$new();
		}

		if (!noTitle && title) {
			var label = $('<label ui-label></label>').html(title).attr('x-for-widget', el.attr('id')),
				labelElem = $compile(label)(labelScope || $scope);
			el.data('label', labelElem);
			return add(el, labelElem);
		}
		add(el);
	});

	var container = $('<div class="panel-layout"></div>').append(layout);

	return container;
}

ui.directive('uiPanelLayout', ['$compile', function($compile) {

	return {
		priority: 1000,
		link: function(scope, element, attrs) {
			var elem = element.children('[ui-transclude]:first');
			var items = elem.children();
			var layout = PanelLayout(items, attrs, scope, $compile);
			elem.append(layout);
		}
	};

}]);

function BarLayout(items, attrs, $scope, $compile) {

	var main = $('<div class="bar-main">');
	var side = $('<div class="bar-side">');
	var wrap = $('<div class="bar-wrap">').appendTo(main);


	items.each(function(item, i) {
		var elem = $(this);
		var prop = elem.scope().field || {};
		if (elem.attr('x-sidebar')) {
			elem.appendTo(side);
		} else {
			elem.appendTo(wrap);
		}
		if (prop.attached) {
			elem.addClass("attached");
		}
	});

	var row = $('<div class="bar-container">').append(main);

	if (side && axelor.device.small) {
		side.children().first().prependTo(wrap);
		side.children().appendTo(wrap);
	}

	wrap.children('[ui-panel-mail]').appendTo(main);

	if (side.children().length > 0) {
		side.appendTo(row);
	}

	return row;
}

ui.directive('uiBarLayout', ['$compile', function($compile) {

	return function(scope, element, attrs) {
		var items = element.children();
		var layout = BarLayout(items, attrs, scope, $compile);
		var schema = scope.schema || {};
		var css = null;

		scope._isPanelForm = true;

		element.append(layout);
		element.addClass('bar-layout');

		if (element.has('[x-sidebar]').length === 0) {
			css = "mid";
		}
		if (element.is('form') && ["mini", "mid", "large"].indexOf(schema.width) > -1) {
			css = scope.schema.width;
		}
		if (css) {
			element.addClass(css + '-form');
		}
	};
}]);

ui.directive('uiPanelViewer', function () {
	return {
		scope: true,
		link: function (scope, element, attrs) {
			var field = scope.field;
			var isRelational = /-to-one$/.test(field.type);
			if (isRelational) {
				Object.defineProperty(scope, 'record', {
					enumerable: true,
					get: function () {
						return (scope.$parent.record||{})[field.name];
					}
				});
			}
		}
	};
});

ui.directive('uiPanelEditor', ['$compile', 'ActionService', function($compile, ActionService) {

	return {
		scope: true,
		link: function(scope, element, attrs) {
			var field = scope.field;
			var editor = field.editor;

			if (!editor) {
				return;
			}

			function applyAttrs(item, level) {
				if (item.showTitle === undefined && !item.items) {
					item.showTitle = (editor.widgetAttrs||{}).showTitles !== "false";
				}
				if (!item.showTitle && !item.items) {
					var itemField = (editor.fields||scope.fields||{})[item.name] || {};
					item.placeholder = item.placeholder || itemField.placeholder || item.title || itemField.title || item.autoTitle;
				}
				if (editor.itemSpan && !item.colSpan && !level) {
					item.colSpan = editor.itemSpan;
				}
				if (item.items) {
					_.map(item.items, function (x) {
						applyAttrs(x, (level||0) + 1);
					});
				}
			}

			var items = editor.items || [];
			var widths = _.map(items, function (item) {
				applyAttrs(item);
				var width = item.width || (item.widgetAttrs||{}).width;
				return width ? width : (item.widget === 'toggle' ? 24 : '*');
			});

			var schema = {
				cols: items.length,
				colWidths: widths.join(','),
				items: items
			};

			if (editor.layout !== 'table') {
				schema = {
					items: [{
						type: 'panel',
						items: items,
						flexbox: editor.flexbox
					}]
				};
			}

			scope.fields = editor.fields || scope.fields;

			var form = ui.formBuild(scope, schema, scope.fields);
			var isRelational = /-to-one$/.test(field.type);

			if (isRelational) {
				Object.defineProperty(scope, 'record', {
					enumerable: true,
					get: function () {
						return (scope.$parent.record||{})[field.name];
					},
					set: function (value) {
						scope.setValue(value, true);
					}
				});
				Object.defineProperty(scope, '$$original', {
					enumerable: true,
					get: function () {
						return (scope.$parent.$$original||{})[field.name];
					},
					set: function (value) {}
				});
				scope.$$setEditorValue = function (value, fireOnChange) {
					scope.setValue(value, fireOnChange === undefined ? true: fireOnChange);
				};
			}

			if (field.target) {
				scope.getContext = function () {
					var context = _.extend({}, scope.record);
					context._model = scope._model;
					context._parent = scope.$parent.getContext();
					return ui.prepareContext(scope._model, context);
				};
				// make sure to fetch missing values
				var fetchMissing = function (value) {
					var ds = scope._dataSource;
					var record = scope.record;
					if (value <= 0 || !value || record.$fetched || record.$fetchedRelated) {
						return;
					}
					var missing = _.filter(_.keys(editor.fields), function (name) {
						if (!record) return false;
						if (name.indexOf('.') === -1) {
							return !record.hasOwnProperty(name);
						}
						var path = name.split('.');
						var nested = record;
						for (var i = 0; i < path.length - 1; i++) {
							nested = nested[path[i]];
							if (!nested) {
								return false;
							}
						}
						return !nested.hasOwnProperty(path[path.length - 1]);
					});
					if (missing.length === 0) {
						return;
					}
					record.$fetchedRelated = true;
					return ds.read(value, {fields: missing}).success(function(rec) {
						var values = _.pick(rec, missing);
						record = _.extend(record, values);
					});
				};
				// make sure to trigger record-change with proper record data
				var watchRun = function (value, old) {
					if (value && value !== old) {
						value.$changed = true;
						value.version = _.isNumber(value.version) ? value.version : value.$version;
					}
					if (value) {
						// parent form's getContext will check this to prepare context for editor
						// to have proper selection flags in nest o2m/m2m
						value.$editorModel = scope._model;
						fetchMissing(value.id);
					}
					scope.$applyAsync(function () {
						scope.$broadcast("on:record-change", value || {}, true);
					});
					// if it's an o2m editor, make sure to update values
					if (scope.$itemsChanged) {
						scope.$itemsChanged();
					}
				};
				scope.$watch('record', _.debounce(watchRun, 100), true);
				scope.$timeout(function () {
					scope.$broadcast("on:record-change", scope.record || {}, true);
				});
			}

			form = $compile(form)(scope);
			form.removeClass('mid-form mini-form').children('div.row').removeClass('row').addClass('row-fluid');
			element.append(form);

			if (field.target) {
				var handler = null;
				if (editor.onNew) {
					schema.onNew = editor.onNew;
					form.data('$editorForm', form);
					handler = ActionService.handler(scope, form, {
						action: editor.onNew
					});
				}
				scope.$watch('record.id', function editorRecordIdWatch(value, old) {
					if (!value && handler) {
						handler.onNew();
					}
				});
			}

			scope.isValid = function () {
				return scope.form && scope.form.$valid;
			};

			function isEmpty(record) {
				if (!record || _.isEmpty(record)) return true;
				var values = _.filter(record, function (value, name) {
					return !(/[\$_]/.test(name) || value === null || value === undefined);
				});
				return values.length === 0;
			}

			scope.$watch(function editorValidWatch() {
				if (isRelational && editor.showOnNew === false && !scope.canShowEditor()) {
					return;
				}
				var valid = scope.isValid();
				if (!valid && !field.jsonFields && !scope.$parent.isRequired() && isEmpty(scope.record)) {
					var errors = (scope.form || {}).$error || {};
					valid = !errors.valid;
				}
				if (scope.setValidity) {
					scope.setValidity('valid', valid);
					element.toggleClass('nested-not-required', valid);
				} else {
					scope.$parent.form.$setValidity('valid', valid, scope.form);
				}
			});

			scope.$on('$destroy', function () {
				if (scope.setValidity) {
					scope.setValidity('valid', true);
				}
			});
		}
	};
}]);

})();
