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
	
	var loading = false;

	function refresh() {
		
		if (loading || $element.is(":hidden")) {
			return;
		}

		var context = $scope._context || {};
		if ($scope.getContext) {
			context = $scope.getContext();
			if (!context.id) {
				return;
			}
		}
		loading = true;
		return $http.post('ws/meta/chart/' + view.name, {
			data: context
		}).then(function(response) {
			var res = response.data;
			$scope.render(res.data);
			loading = false;
		});
	}
	
	setTimeout(refresh);
	
	$scope.onRefresh = function() {
		refresh();
	};

	$scope.render = function(data) {
		
	};
};

function $conv(value) {
	if (!value) return 0;
	if (_.isNumber(value)) return value;
	if (/(-)?\d+(\.\d+)?/.test(value)) {
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
	});
	return chart_data;
}

REGISTRY["pie"] = PieChart;
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

function Chart(scope, element, data) {
	
	var type = null;

	for(var i = 0 ; i < data.series.length ; i++) {
		type = data.series[i].type;
		if (type === "bar" && !data.series[i].groupBy) type = "dbar";
		if (type === "pie" || type === "dbar") {
			break;
		}
	}

	if (type === "pie" && data.series.length > 1) {
		return;
	}
	
	if (data.series.length > 1) {
		type = "multi";
	}

	element.off('adjustSize').empty();

	nv.addGraph(function generate() {
		
		var maker = REGISTRY[type] || REGISTRY["bar"];
		var chart = maker(scope, element, data);

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
			"decimal": d3.format(',.1f'),
			"text": function(d) { return d; }
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
		setTimeout(chart.update, 10);

		return chart;
	});
}

var directiveFn = function(){
	return {
		controller: ChartCtrl,
		link: function(scope, element, attrs) {
			
			var elem = element.children('svg'),
				initialized = false;

			scope.render = function(data) {
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
			'<svg></svg>'+
		'</div>'
	};
};

ui.directive('uiViewChart', directiveFn);
ui.directive('uiPortletChart', directiveFn);

}).call(this);
