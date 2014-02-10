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

ui.ProgressMixin = {

	css: 'progress-item',
	cellCss: 'form-item progress-item',
	
	link_readonly: function(scope, element, attrs, model) {
		
		var field = scope.field || {},
			that = this;

		scope.$watch("getValue()", function(value, old) {
			var props = that.compute(field, value);
			scope.css = props.css;
			scope.width = props.width;
		});
	},
	
	compute: function(field, value) {
		
		var max = +(field.max) || 100,
			min = +(field.min) || 0;

		colors = [
			["r", 24],	// 00 - 24 (red)
			["y", 49],	// 25 - 49 (yellow)
			["b", 74],  // 50 - 74 (blue)
			["g", 100]  // 75 - 100 (green)
		];
		
		if (field.colors) {
			colors = _.chain(field.colors.split(/,/)).invoke('split', /:/).value() || [];
		}

		colors.reverse();
		
		var styles = {
			"r": "progress-danger",
			"y": "progress-warning",
			"b": "",
			"g": "progress-success"
		};

		var width = +(value) || 0;
		var css = "progress-striped";

		width = (width * 100) / (max - min);
		width = Math.min(Math.round(width), 100);

		var color = "";
		for(var i = 0 ; i < colors.length; i++) {
			var c = colors[i][0];
			var v = +colors[i][1];
			if (width <= v) {
				color = styles[c] || "";
			}
		}
		
		css += " " + color;
		if (width < 100) {
			css += " " + "active";
		}
		
		return {
			css: css,
			width: width
		};
	},
	
	template_readonly:
	'<div class="progress {{css}}">'+
	  '<div class="bar" style="width: {{width}}%;"></div>'+
	'</div>'
};

/**
 * The Progress widget with integer/decimal input.
 * 
 */
ui.formInput('Progress', 'Integer', _.extend({}, ui.ProgressMixin));

/**
 * The Progress widget with selection input.
 * 
 */
ui.formInput('SelectProgress', 'Select', _.extend({}, ui.ProgressMixin));

})(this);
