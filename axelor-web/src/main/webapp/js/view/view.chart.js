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
(function(){

"use strict";

nv.dev = false;

var tooltipShow = nv.tooltip.show;
nv.tooltip.show = function(pos, content, gravity, dist, parentContainer, classes) {
	var body = parentContainer;
	if (body && !$("body").is(body)) {
		var diff = $(body).offset();
		pos[0] += diff.left;
		pos[1] += diff.top;
		body = $("body")[0];
	}
	tooltipShow(pos, content, gravity, dist, body, classes);
};

// i18n
_.extend(nv.messages, {
	'Grouped': _t('Grouped'),
	'Stacked': _t('Stacked')
});

var ui = angular.module('axelor.ui');

this.ChartCtrl = ChartCtrl;

ChartCtrl.$inject = ['$scope', '$element', '$http'];
function ChartCtrl($scope, $element, $http) {

	var views = $scope._views;
	var view = $scope.view = views['chart'];
	
	var viewChart = null;
	var viewValues = null;

	var loading = false;

	function refresh() {
		
		if (loading || $element.is(":hidden")) {
			return;
		}

		// in case of onInit
		if ($scope.searchInit && !viewValues) {
			return;
		}

		var context = $scope._context || {};
		if ($scope.getContext) {
			context = _.extend({}, $scope.getContext(), context);
			if ($scope.onSave && !context.id) { // if embedded inside form view
				return;
			}
		}

		context = _.extend({}, context, viewValues);
		loading = true;
		
		var params = {
			data: context
		};

		if (viewChart) {
			params.fields = ['dataset'];
		}

		return $http.post('ws/meta/chart/' + view.name, params).then(function(response) {
			var res = response.data;
			var data = res.data;

			if (viewChart === null) {
				viewChart = data;
			} else {
				data = _.extend({}, viewChart, data);
			}

			if ($scope.searchFields === undefined && data.search) {
				$scope.searchFields = data.search;
				$scope.searchInit = data.onInit;
			} else {
				$scope.render(data);
			}
			loading = false;
		});
	}

	setTimeout(refresh);

	$scope.onRefresh = function() {
		refresh();
	};

	$scope.setViewValues = function (values) {
		viewValues = values;
	};

	$scope.render = function(data) {
		
	};
};

ChartFormCtrl.$inject = ['$scope', '$element', 'ViewService', 'DataSource'];
function ChartFormCtrl($scope, $element, ViewService, DataSource) {

	$scope._dataSource = DataSource.create('com.axelor.meta.db.MetaView');
	
	FormViewCtrl.call(this, $scope, $element);
	$scope.setEditable();
	
	function fixFields(fields) {
		_.each(fields, function(field){
			if (field.type == 'reference') {
				field.type = 'MANY_TO_ONE';
				field.canNew = false;
				field.canEdit = false;
			}
			
			if (field.type)
				field.type = field.type.toUpperCase();
			else
				field.type = 'STRING';
		});
		return fields;
	}
	
	var unwatch = $scope.$watch('searchFields', function (fields) {
		if (!fields) {
			return;
		}
		unwatch();

		var meta = { fields: fixFields(fields) };
		var view = {
			type: 'form',
			cols: 4,
			items: meta.fields
		};

		ViewService.process(meta, view);

		view.onLoad = $scope.searchInit;
		
		$scope.fields = meta.fields;
		$scope.schema = view;
		$scope.schema.loaded = true;

		var interval = undefined;

		function reload() {
			$scope.$parent.setViewValues($scope.record);
			$scope.$parent.onRefresh();
		}
		
		function delayedReload() {
			clearTimeout(interval);
			interval = setTimeout(reload, 500);
		}

		$scope.$watch('record', function (record) {
			if (interval === undefined) {
				return interval = null;
			}
			delayedReload();
		}, true);
		
		$scope.$watch('$events.onLoad', function (handler) {
			if (handler) {
				handler().then(delayedReload);
			}
		});
	});
}

function $conv(value) {
	if (!value) return 0;
	if (_.isNumber(value)) return value;
	if (/^(-)?\d+(\.\d+)?$/.test(value)) {
		return +value;
	}
	return value;
}

var REGISTRY = {};

function PlusData(scope, data, type) {
	var result = [];
	_.each(data.series, function(series) {
		if (series.type !== type) return;
		var key = series.key;
		_.chain(data.dataset).groupBy(data.xAxis).each(function(group, name) {
			var value = 0;
			_.each(group, function(item) {
				value += $conv(item[key]);
			});
			result.push({x: name, y: value});
		});
	});
	return result;
}

function PlotData(scope, data, type) {
	var chart_data = [];
	var points = _.chain(data.dataset).pluck(data.xAxis).unique().value();
	_.each(data.series, function(series) {
		if (series.type !== type) return;
		var key = series.key;
		var groupBy = series.groupBy || data.xAxis;
		_.chain(data.dataset).groupBy(groupBy).each(function(group, name) {
			var my = [];
			var values = _.map(group, function(item) {
				var x = $conv(item[data.xAxis]) || 0;
				var y = $conv(item[key] || name);
				my.push(x);
				return { x: x, y: y };
			});

			var missing = _.difference(points, my);
			if (points.length === missing.length) return;

			_.each(missing, function(x) {
				values.push({ x: x, y: 0 });
			});

			values = _.sortBy(values, "x");

			chart_data.push({
				key: name,
				type: type,
				values: values
			});
		});
		
		if (series.title) {
			if (series.side === "right") {
				data.y2Title = series.title;
			} else {
				data.yTitle = series.title;
			}
		}
	});

	return chart_data;
}

REGISTRY["pie"] = PieChart;
function PieChart(scope, element, data, type) {

	var chart_data = PlusData(scope, data, type || "pie");
	
	var chart = nv.models.pieChart()
		.showLabels(false)
		.x(function(d) { return d.x; })
	    .y(function(d) { return d.y; })
	    .values(function(d) { return d; })
	    .color(d3.scale.category10().range());

	if (type === "donut") {
		chart.showLabels(true)
			 .labelType("percent")
			 .labelThreshold(.05)
			 .donut(true)
			 .donutRatio(0.40);
	}

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(1200).call(chart);

	return chart;
}

REGISTRY["donut"] = DonutChart;
function DonutChart(scope, element, data) {
	return PieChart(scope, element, data, "donut");
}

REGISTRY["dbar"] = DBarChart;
function DBarChart(scope, element, data) {
	
	var chart_data = PlusData(scope, data, "bar");
	chart_data = [{
		key: data.title,
		values: chart_data
	}];
	
	var chart = nv.models.discreteBarChart()
	    .x(function(d) { return d.x; })
	    .y(function(d) { return d.y; })
	    .staggerLabels(true)
	    .showValues(true);
	
	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

REGISTRY["bar"] = BarChart;
function BarChart(scope, element, data) {
	
	var chart_data = PlotData(scope, data, "bar");
	var chart = nv.models.multiBarChart()
		.reduceXTicks(false)
		.color(d3.scale.category10().range());

	chart.multibar.hideable(true);
	chart.stacked(data.stacked);

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

REGISTRY["hbar"] = HBarChart;
function HBarChart(scope, element, data) {
	
	var chart_data = PlotData(scope, data, "hbar");
	var chart = nv.models.multiBarHorizontalChart()
		.color(d3.scale.category10().range());

	chart.stacked(data.stacked);

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

REGISTRY["line"] = LineChart;
function LineChart(scope, element, data) {

	var chart_data = PlotData(scope, data, "line");
	var chart = nv.models.lineChart()
		.color(d3.scale.category10().range());

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

REGISTRY["area"] = AreaChart;
function AreaChart(scope, element, data) {

	var chart_data = PlotData(scope, data, "area");
	var chart = nv.models.stackedAreaChart()
				  .color(d3.scale.category10().range());

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);

	return chart;
}

REGISTRY["radar"] = RadarCharter;
function RadarCharter(scope, element, data) {

	var result = _.map(data.dataset, function(item) {
		return _.map(data.series, function(s) {
			var title = s.title || s.key,
				value = item[s.key];
			return {
				axis: title,
				value: $conv(value) || 0
			};
		});
	});

	var id = _.uniqueId('_radarChart'),
		parent = element.parent();

	parent.attr('id', id)
		  .addClass('radar-chart')
		  .empty();

	var size = Math.min(parent.innerWidth(), parent.innerHeight());

	RadarChart.draw('#'+id, result, {
		w: size,
		h: size
	});

	parent.children('svg')
		.css('width', 'auto')
		.css('margin', 'auto')
		.css('margin-top', 10);
	
	return null;
}

REGISTRY["gauge"] = GaugeCharter;
function GaugeCharter(scope, element, data) {
	
	var config = data.config,
		min = +(config.min) || 0,
		max = +(config.max) || 100,
		value = 0;

	var item = _.first(data.dataset),
		series = _.first(data.series),
		key = series.key || data.xAxis;

	if (item) {
		value = item[key] || value;
	}
	
	var parent = element.hide().parent();
	
	parent.children('svg').remove();
	parent.append(element);

	var chart = GaugeChart(parent[0], {
		size: 300,
		clipWidth: 300,
		clipHeight: 300,
		ringWidth: 60,
		minValue: min,
		maxValue: max,
		transitionMs: 4000
	});

	chart.render();
	chart.update(value);

	parent.children('svg:last')
		.css('width', 'auto')
		.css('margin', 'auto')
		.css('margin-top', 10);
}

function Chart(scope, element, data) {
	
	var type = null;

	for(var i = 0 ; i < data.series.length ; i++) {
		type = data.series[i].type;
		if (type === "bar" && !data.series[i].groupBy) type = "dbar";
		if (type === "pie" || type === "dbar" || type === "radar" || type === "gauge") {
			break;
		}
	}

	if (type === "pie" && data.series.length > 1) {
		return;
	}
	
	if (type !== "radar" && data.series.length > 1) {
		type = "multi";
	}

	element.off('adjustSize').empty();

	nv.addGraph(function generate() {
		
		var maker = REGISTRY[type] || REGISTRY["bar"];
		var chart = maker(scope, element, data);

		if (chart == null) {
			return;
		}

		chart.noData(_t('No Data Available.'));
		var tickFormats = {
			"month" : function(d) {
				return moment([2000, d - 1, 1]).format("MMM");
			},
			"year" : function(d) {
				return moment([2000, d - 1, 1]).format("YYYY");
			},
			"number": d3.format(',f'),
			"decimal": d3.format(',.1f'),
			"text": function(d) { return d; }
		};
		
		var tickFormat = tickFormats[data.xType];
		if (chart.xAxis && tickFormat) {
			chart.xAxis
				.rotateLabels(-45)
				.tickFormat(tickFormat);
		}
		
		if (chart.yAxis && data.yTitle) {
			chart.yAxis.axisLabel(data.yTitle);
		}
		
		chart.margin({ left: 90, top: 25 });

		var lastWidth = 0;
		var lastHeight = 0;
		
		function adjust() {
			
			if (element.is(":hidden")) {
				return;
			}

			var w = element.width(),
				h = element.height();
			
			if (w === lastWidth && h === lastHeight) {
				return;
			}

			lastWidth = w;
			lastHeight = h;

			chart.update();
		}

		element.on('adjustSize', _.debounce(adjust, 100));
		setTimeout(chart.update, 10);

		return chart;
	});
}

var directiveFn = function(){
	return {
		controller: ChartCtrl,
		link: function(scope, element, attrs) {
			
			var initialized = false;

			scope.render = function(data) {
				var elem = element.children('svg');
				if (elem.is(":hidden")) {
					return initialized = false;
				}
				scope.title = data.title;
				Chart(scope, elem, data);
				return initialized = true;
			};

			element.on("adjustSize", function(e){
				if (!initialized) scope.onRefresh();
			});
			
			scope.$on("on:new", function(e) {
				scope.onRefresh();
			});
			scope.$on("on:edit", function(e) {
				scope.onRefresh();
			});
		},
		replace: true,
		template:
		'<div class="chart-container" style="background-color: white; ">'+
			'<div ui-chart-form></div>'+
			'<svg></svg>'+
		'</div>'
	};
};

ui.directive('uiChartForm', function () {
	
	return {
		scope: true,
		controller: ChartFormCtrl,
		link: function (scope, element, attrs, ctrls) {

		},
		replace: true,
		template:
			"<div class='chart-controls'>" +
				"<div ui-view-form x-handler='this'></div>" +
			"</div>"
	};
});


ui.directive('uiViewChart', directiveFn);
ui.directive('uiPortletChart', directiveFn);

}).call(this);
