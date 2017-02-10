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
(function(){

/* global d3: true, nv: true, D3Funnel: true, RadarChart: true, GaugeChart: true  */

"use strict";

var ui = angular.module('axelor.ui');

ui.ChartCtrl = ChartCtrl;
ui.ChartCtrl.$inject = ['$scope', '$element', '$http', 'ActionService'];

function ChartCtrl($scope, $element, $http, ActionService) {

	var views = $scope._views;
	var view = $scope.view = views.chart;
	
	var viewChart = null;
	var searchScope = null;
	var actionHandler = null;

	var loading = false;
	var unwatch = null;

	function refresh() {

		if (viewChart && searchScope && $scope.searchFields && !searchScope.isValid()) {
			return;
		}

		var context = $scope._context || {};
		if ($scope.getContext) {
			context = _.extend({}, $scope.getContext(), context);
			if ($scope.onSave && !context.id) { // if embedded inside form view
				if (viewChart) {
					$scope.render(_.omit(viewChart, 'dataset'));
				}
				return;
			}
		}

		context = _.extend({}, context, (searchScope||{}).record, { _domainAction: $scope._viewAction });
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
			var isInitial = viewChart === null;

			if (viewChart === null) {
				viewChart = data;
				if (data.config && data.config.onClick) {
					actionHandler = ActionService.handler($scope, $element, {
						action: data.config.onClick
					});
				}
			} else {
				data = _.extend({}, viewChart, data);
			}

			if ($scope.searchFields === undefined && data.search) {
				$scope.searchFields = data.search;
				$scope.searchInit = data.onInit;
			} else {
				$scope.render(data);
				if (isInitial) {
					refresh(); // force loading data
				}
			}
			loading = false;
		}, function () {
			loading = false;
		});
	}

	$scope.setSearchScope = function (formScope) {
		searchScope = formScope;
	};

	$scope.handleClick = function (e) {
		if (actionHandler) {
			actionHandler._getContext = function () {
				return _.extend({}, e.data.raw, { _model: $scope._model || 'com.axelor.meta.db.MetaView' });
			};
			actionHandler.handle();
		}
	};

	$scope.onRefresh = function(force) {
		if (unwatch || loading) {
			return;
		}

		// in case of onInit
		if ($scope.searchInit && !(searchScope||{}).record && !force) {
			return;
		}

		unwatch = $scope.$watch(function () {
			if ($element.is(":hidden")) {
				return;
			}
			unwatch();
			unwatch = null;
			refresh();
		});
	};

	$scope.render = function(data) {
		
	};

	// refresh to load chart
	$scope.onRefresh();
}

ChartFormCtrl.$inject = ['$scope', '$element', 'ViewService', 'DataSource'];
function ChartFormCtrl($scope, $element, ViewService, DataSource) {

	$scope._dataSource = DataSource.create('com.axelor.meta.db.MetaView');
	
	ui.FormViewCtrl.call(this, $scope, $element);

	$scope.setEditable();
	$scope.setSearchScope($scope);

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
			items: [{
				type: 'panel',
				noframe: true,
				items: _.map(meta.fields, function (item) {
					return _.extend({}, item, {
						showTitle: false,
						placeholder: item.title || item.autoTitle
					});
				})
			}]
		};

		ViewService.process(meta, view);

		view.onLoad = $scope.searchInit;
		
		$scope.fields = meta.fields;
		$scope.schema = view;
		$scope.schema.loaded = true;

		var interval;

		function reload() {
			$scope.$parent.onRefresh();
			$scope.applyLater();
		}
		
		function delayedReload() {
			clearTimeout(interval);
			interval = setTimeout(reload, 500);
		}

		function onNewOrEdit() {
			if ($scope.$events.onLoad) {
				$scope.$events.onLoad().then(delayedReload);
			}
		}

		$scope.$on('on:new', onNewOrEdit);
		$scope.$on('on:edit', onNewOrEdit);

		$scope.$watch('record', function (record) {
			if (interval === undefined) {
				interval = null;
				return;
			}
			if ($scope.isValid()) delayedReload();
		}, true);
		
		$scope.$watch('$events.onLoad', function (handler) {
			if (handler) {
				handler().then(delayedReload);
			}
		});
	});
}

function $conv(value, type) {
	if (!value && type === 'text') return 'N/A';
	if (!value) return 0;
	if (_.isNumber(value)) return value;
	if (/^(-)?\d+(\.\d+)?$/.test(value)) {
		return +value;
	}
	return value;
}

function applyXY(chart, data) {
	var type = data.xType;
	chart.y(function (d) { return d.y; });
	if (type == "date") {
		return chart.x(function (d) { return moment(d.x).toDate(); });
	}
	return chart.x(function (d) { return d.x; });
}

var color_shades = [
	d3.scale.category10().range(), // no shades
	d3.scale.category20().range(), // 2 shades
	d3.scale.category20b().range() // 4 shades
];

function colors(color, shades, type) {
	if (color) {
		var n = +(shades) || 4;
		var rest = color_shades[n-1];
		return _.flatten(color.split(',').map(function (c) {
			return _.first(_.range(0, n + 1).map(d3.scale.linear().domain([0, n + 1]).range([c, 'white'])), n);
		}).concat(rest));
	}
	return type == 'pie' ? d3.scale.category10().range() : d3.scale.category20().range();
}

var CHARTS = {};

function PlusData(series, data) {
	var result = _.chain(data.dataset)
	.groupBy(data.xAxis)
	.map(function (group, name) {
		var value = 0;
		_.each(group, function (item) {
			value += $conv(item[series.key]);
		});
		var raw = {};
		if (group[0]) {
			raw[data.xAxis] = name;
			raw[series.key] = value;
			raw[data.xAxis + 'Id'] = group[0][data.xAxis + 'Id'];
		}
		return {
			x: name === 'null' ? 'N/A' : name,
			y: value,
			raw: raw
		};
	 }).value();

	return result;
}

function PlotData(series, data) {
	var ticks = _.chain(data.dataset).pluck(data.xAxis).unique().map(function (v) { return $conv(v, data.xType); }).value();
	var groupBy = series.groupBy;
	var datum = [];

	_.chain(data.dataset).groupBy(groupBy)
	.map(function (group, groupName) {
		var name = groupBy ? groupName : null;
		var values = _.map(group, function (item) {
			var x = $conv(item[data.xAxis], data.xType) || 0;
			var y = $conv(item[series.key] || name || 0);
			return { x: x, y: y, raw: item };
		});

		var my = _.pluck(values, 'x');
		var missing = _.difference(ticks, my);
		if (ticks.length === missing.length) {
			return;
		}

		_.each(missing, function(x) {
			values.push({ x: x, y: 0 });
		});

		values = _.sortBy(values, 'x');

		datum.push({
			key: name || series.title,
			type: series.type,
			values: values
		});
	});

	return datum;
}

function PieChart(scope, element, data) {
	
	var series = _.first(data.series);
	var datum = PlusData(series, data);
	var config = data.config || {};

	var chart = nv.models.pieChart()
		.showLabels(false)
		.height(null)
		.width(null)
		.x(function(d) { return d.x; })
		.y(function(d) { return d.y; });

	if (series.type === "donut") {
		chart.donut(true)
			 .donutRatio(0.40);
	}

	if (_.toBoolean(config.percent)) {
		chart.showLabels(true)
			.labelType("percent")
			.labelThreshold(0.05);
	}
	
	d3.select(element[0])
	  .datum(datum)
	  .transition().duration(1200).call(chart);

	chart.pie.dispatch.on('elementClick', function (e) {
		scope.handleClick(e);
	});

	return chart;
}

CHARTS.pie = PieChart;
CHARTS.donut = PieChart;

function DBarChart(scope, element, data) {

	var series = _.first(data.series);
	var datum = PlusData(series, data);

	datum = [{
		key: data.title,
		values: datum
	}];

	var chart = nv.models.discreteBarChart()
	    .x(function(d) { return d.x; })
	    .y(function(d) { return d.y; })
	    .staggerLabels(true)
	    .showValues(true);
	
	d3.select(element[0])
	  .datum(datum)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function BarChart(scope, element, data) {
	
	var series = _.first(data.series);
	var datum = PlotData(series, data);

	var chart = nv.models.multiBarChart()
		.reduceXTicks(false);

	chart.multibar.hideable(true);
	chart.stacked(data.stacked);

	d3.select(element[0])
	  .datum(datum)
	  .transition().duration(500).call(chart);
	
	chart.multibar.dispatch.on('elementClick', function (e) {
		scope.handleClick(e);
	});

	return chart;
}

function HBarChart(scope, element, data) {
	
	var series = _.first(data.series);
	var datum = PlotData(series, data);

	var chart = nv.models.multiBarHorizontalChart();

	chart.stacked(data.stacked);

	d3.select(element[0])
	  .datum(datum)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function FunnelChart(scope, element, data) {
	
	if(!data.dataset){
		return;
	}
	
	var chart = new D3Funnel(element[0]);
	var w = element.width();
	var h = element.height();
	var config = _.extend({}, data.config);
	var props = {
			fillType: 'gradient',
			hoverEffects: true,
			dynamicArea: true,
			animation: 200};
	
	if(config.width){
		props.width = w*config.width/100;
	}
	if(config.height){
		props.height = h*config.height/100;
	}
	
	var series = _.first(data.series) || {};
	var opts = [];
	_.each(data.dataset, function(dat){
		opts.push([dat[data.xAxis],($conv(dat[series.key])||0)]);
	});
	chart.draw(opts, props);
	
	chart.update = function(){};
	
	return chart;
}

CHARTS.bar = BarChart;
CHARTS.dbar = DBarChart;
CHARTS.hbar = HBarChart;
CHARTS.funnel = FunnelChart;

function LineChart(scope, element, data) {

	var series = _.first(data.series);
	var datum = PlotData(series, data);

	var chart = nv.models.lineChart()
		.showLegend(true)
		.showYAxis(true)
		.showXAxis(true);

	applyXY(chart, data);
	
	d3.select(element[0])
	  .datum(datum)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function AreaChart(scope, element, data) {

	var series = _.first(data.series);
	var datum = PlotData(series, data);

	var chart = nv.models.stackedAreaChart();

	applyXY(chart, data);

	d3.select(element[0])
	  .datum(datum)
	  .transition().duration(500).call(chart);

	return chart;
}

CHARTS.line = LineChart;
CHARTS.area = AreaChart;

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
	
	var w = element.width();
	var h = element.height();

	var parent = element.hide().parent();
	
	parent.children('svg').remove();
	parent.append(element);

	var chart = GaugeChart(parent[0], {
		size: 300,
		clipWidth: 300,
		clipHeight: h,
		ringWidth: 60,
		minValue: min,
		maxValue: max,
		transitionMs: 4000
	});

	chart.render();
	chart.update(value);

	parent.children('svg:last')
		.css('display', 'block')
		.css('width', 'auto')
		.css('margin', 'auto')
		.css('margin-top', 0);
}

function TextChart(scope, element, data) {

	var config = _.extend({
		strong: true,
		shadow: false,
		fontSize: 22
	}, data.config);

	var values = _.first(data.dataset) || {};
	var series = _.first(data.series) || {};

	var value = values[series.key];

	if (config.format) {
		value = _t(config.format, value);
	}

	var svg = d3.select(element.empty()[0]);
	var text = svg.append("svg:text")
		.attr("x", "50%")
		.attr("y", "50%")
		.attr("dy", ".3em")
	    .attr("text-anchor", "middle")
		.text(value);

	if (config.color) text.attr("fill", config.color);
	if (config.fontSize) text.style("font-size", config.fontSize);
	if (_.toBoolean(config.strong)) text.style("font-weight", "bold");
	if (_.toBoolean(config.shadow)) text.style("text-shadow", "0 1px 2px rgba(0, 0, 0, .5)");
}

CHARTS.text = TextChart;
CHARTS.radar = RadarCharter;
CHARTS.gauge = GaugeCharter;

function Chart(scope, element, data) {
	
	var type = null;
	var config = data.config || {};

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
		
		var noData = _t('No records found.');
		if (data.dataset && data.dataset.stacktrace) {
			noData = data.dataset.message;
			data.dataset = [];
		}

		var maker = CHARTS[type] || CHARTS.bar || function () {};
		var chart = maker(scope, element, data);

		if (!chart) {
			return;
		}

		if (chart.color) {
			chart.color(colors(config.colors, config.shades, type));
		}

		if (chart.noData) {
			chart.noData(noData);
		}
		if(chart.controlLabels) {
			chart.controlLabels({
				grouped: _t('Grouped'),
				stacked: _t('Stacked'),
				stream: _t('Stream'),
				expanded: _t('Expanded'),
				stack_percent: _t('Stack %')
			});
		}
		
		var tickFormats = {
			"date" : function (d) {
				var f = config.xFormat;
				return moment(d).format(f || 'YYYY-MM-DD');
			},
			"month" : function(d) {
				var v = "" + d;
				var f = config.xFormat;
				if (v.indexOf(".") > -1) return "";
				if (_.isString(d) && /^(\d+)$/.test(d)) {
					d = parseInt(d);
				}
				if (_.isNumber(d)) {
					return moment([moment().year(), d - 1, 1]).format(f || "MMM");
				}
				if (_.isString(d) && d.indexOf('-') > 0) {
					return moment(d).format(f || 'MMM, YYYY');
				}
				return d;
			},
			"year" : function(d) {
				return moment([moment().year(), d - 1, 1]).format("YYYY");
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

		var margin = null;
		['top', 'left', 'bottom', 'right'].forEach(function (side) {
			var key = 'margin-' + side;
			var val = parseInt(config[key]);
			if (val) {
				(margin||(margin={}))[side] = val;
			}
		});
		if (chart.margin && margin) {
			chart.margin(margin);
		}

		var lastWidth = 0;
		var lastHeight = 0;
		
		function adjust() {
			
			if (!element[0] || element.parent().is(":hidden")) {
				return;
			}

			var rect = element[0].getBoundingClientRect();
			var w = rect.width,
				h = rect.height;
			
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
			
			var svg = element.children('svg');
			var form = element.children('.chart-controls');
			
			scope.render = function(data) {
				if (element.is(":hidden")) {
					return;
				}
				setTimeout(function () {
					svg.height(element.height() - form.height()).width('100%');
					if (!scope.dashlet || !scope.dashlet.title) {
						scope.title = data.title;
					}
					Chart(scope, svg, data);
					return;
				});
			};

			function onNewOrEdit() {
				if (scope.searchInit && scope.searchFields) {
					return;
				}
				scope.onRefresh(true);
			}
			
			scope.$on('on:new', onNewOrEdit);
			scope.$on('on:edit', onNewOrEdit);
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

})();
