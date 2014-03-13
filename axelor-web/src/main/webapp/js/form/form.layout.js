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
(function() {

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
	
	var table = $('<table class="form-layout"></table');
	
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
				el = $('<td></td>')
					.addClass(cell.css)
					.attr('colspan', cell.colspan)
					.attr('rowspan', cell.rowspan)
					.append(cell.elem)
					.appendTo(tr);
				if (_.isArray(widths) && widths[i]) {
					el.width(widths[i]);
				}
				if ($(cell.elem).is('.form-item-container') && __appSettings['view.form.hot-edit']) {
					$(cell.elem).prepend($('<span class="fa fa-pencil hot-edit-icon"></span>'));
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

})(this);
