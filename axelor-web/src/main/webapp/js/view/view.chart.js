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

var ui = angular.module('axelor.ui');

this.ChartCtrl = ChartCtrl;

ChartCtrl.$inject = ['$scope', '$element', '$http'];
function ChartCtrl($scope, $element, $http) {

	var views = $scope._views;
	var view = $scope.view = views['chart'];
	
	function refresh() {
		return $http.get('ws/meta/chart/' + view.name).then(function(response) {
			var res = response.data;
			$scope.render(res.data);
		});
	}
	
	refresh();
	
	$scope.onRefresh = function() {
		refresh();
	};

	$scope.render = function(data) {
		
	};
};

function $eval(scope, expr, locals) {
	var root = scope.$root || scope;
	var context = _.extend({
		"$moment": moment,
		"$number": function(v) { return +v; }
	}, locals);
	return root.$eval(expr, context);
}

function $conv(value) {
	if (!value) return 0;
	if (_.isNumber(value)) return value;
	if (/(-)?\d+(\.\d+)?/.test(value)) {
		return +value;
	}
	return value;
}

function PlusData(scope, data, type) {
	var result = [];
	_.each(data.series, function(series) {
		if (series.type !== type) return;
		var key = series.key;
		var expr = series.expr;
		_.chain(data.data).groupBy(data.xAxis).each(function(group, name) {
			var value = 0;
			_.each(group, function(item) {
				value += $conv(expr ? $eval(scope, expr, item) : item[key]);
			});
			result.push({x: name, y: value});
		});
	});
	return result;
}

function PlotData(scope, data, type) {
	var chart_data = [];
	var points = _.chain(data.data).pluck(data.xAxis).unique().value();
	_.each(data.series, function(series) {
		if (series.type !== type) return;
		var key = series.key;
		var expr = series.expr;
		_.chain(data.data).groupBy(key).each(function(group, name) {
			var my = [];
			var values = _.map(group, function(item) {
				var x = $conv(item[data.xAxis]) || 0;
				var y = $conv(expr ? $eval(scope, expr, item) : name);
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
	});
	return chart_data;
}

function PieChart(scope, element, data) {

	var chart_data = PlusData(scope, data, "pie");

	var chart = nv.models.pieChart()
		.showLabels(false)
		.x(function(d) { return d.x; })
	    .y(function(d) { return d.y; })
	    .values(function(d) { return d; })
	    .color(d3.scale.category10().range());
	
	d3.select(element[0])
	  .datum([chart_data])
	  .transition().duration(1200).call(chart);

	return chart;
}

function BarChart(scope, element, data) {
	
	var chart_data = PlotData(scope, data, "bar");
	var chart = nv.models.multiBarChart()
		.reduceXTicks(false)
		.color(d3.scale.category10().range());

	chart.multibar.hideable(true);

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function HBarChart(scope, element, data) {
	
	var chart_data = PlotData(scope, data, "hbar");
	var chart = nv.models.multiBarHorizontalChart()
		.color(d3.scale.category10().range());

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function LineChart(scope, element, data) {

	var chart_data = PlotData(scope, data, "line");
	var chart = nv.models.lineChart()
		.color(d3.scale.category10().range());

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function AreaChart(scope, element, data) {

	var chart_data = PlotData(scope, data, "area");
	var chart = nv.models.stackedAreaChart()
		.color(d3.scale.category10().range());

	d3.select(element[0])
	  .datum(chart_data)
	  .transition().duration(500).call(chart);
	
	return chart;
}

function Chart(scope, element, data) {
	
	var type = null;

	for(var i = 0 ; i < data.series.length ; i++) {
		type = data.series[i].type;
		if (type === "pie") {
			break;
		}
	}

	if (type === "pie" && data.series.length > 1) {
		return;
	}
	
	if (data.series.length > 1) {
		type = "multi";
	}

	nv.addGraph(function generate() {

		var chart = null;
		
		element.off('adjustSize').empty();

		if (type === "pie") {
			chart = PieChart(scope, element, data);
		}
		if (type === "bar") {
			chart = BarChart(scope, element, data);
		}
		if (type === "hbar") {
			chart = HBarChart(scope, element, data);
		}
		if (type === "line") {
			chart = LineChart(scope, element, data);
		}
		if (type === "area") {
			chart = AreaChart(scope, element, data);
		}

		if (chart == null) {
			return;
		}
		
		var tickFormats = {
			"month" : function(d) {
				return moment([2000, d, 1]).format("MMM");
			},
			"year" : function(d) {
				return moment([2000, d, 1]).format("YYYY");
			},
			"number": d3.format(',f'),
			"decimal": d3.format(',.1f')
		};
		
		var tickFormat = tickFormats[data.xType];
		if (chart.xAxis && tickFormat) {
			chart.xAxis
				.rotateLabels(-45)
				.tickFormat(tickFormat);
		}
		
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
		setTimeout(chart.update);

		return chart;
	});
}

var directiveFn = function(){
	return {
		controller: ChartCtrl,
		link: function(scope, element, attrs) {
			var elem = element.children('svg');
			scope.render = function(data) {
				Chart(scope, elem, data);
			};
		},
		replace: true,
		template:
		'<div class="chart-container" style="background-color: white; ">'+
			'<svg></svg>'+
		'</div>'
	};
};

ui.directive('uiViewChart', directiveFn);
ui.directive('uiPortletChart', directiveFn);

}).call(this);
