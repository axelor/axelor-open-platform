(function(){

"use strict";

nv.dev = false;

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

function PieChart(scope, element, data) {

	var chart_data = [];
	
	_.each(data.series, function(series) {
		var key = series.key,
			expr = series.expr;
		
		_.chain(data.data).each(function(item) {
			var x = item[data.xAxis];
			var y = expr ? $eval(scope, expr, item) : item[key];
			chart_data.push({
				x: $conv(x),
				y: $conv(y)
			});
		});
	});

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
	
	var chart_data = [];
	
	_.each(data.series, function(series) {
		var key = series.key,
			type = series.type,
			expr = series.expr;

		_.chain(data.data).groupBy(key).each(function(group, name) {
			var values = _.map(group, function(item) {
				var x = item[data.xAxis] || 0;
				var y = expr ? $eval(scope, expr, item) : name;
				return {
					x: $conv(x),
					y: $conv(y)
				};
			});
			chart_data.push({
				key: name,
				type: type,
				values: values
			});
		});
	});
	
	var chart = nv.models.multiBarChart()
		.showControls(false)
		.reduceXTicks(false)
		.barColor(d3.scale.category20().range());
	
	chart.multibar.hideable(true);

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
	
	if (data.series.length > 1) {
		type = "multi";
	}

	return nv.addGraph(function() {

		var chart = null;

		if (type === "pie") {
			chart = PieChart(scope, element, data);
		}
		if (type === "bar") {
			chart = BarChart(scope, element, data);
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
