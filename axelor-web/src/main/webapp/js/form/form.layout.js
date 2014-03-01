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
					$(cell.elem).prepend($('<span class="icon icon-pencil hot-edit-icon"></span>'));
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
