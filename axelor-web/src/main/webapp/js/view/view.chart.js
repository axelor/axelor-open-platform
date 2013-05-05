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

function barChart(scope, element, data) {
	
	var chart = nv.models.multiBarChart()
		.showControls(false)
		.reduceXTicks(false)
		.barColor(d3.scale.category20().range());

	chart.multibar.hideable(true);

	d3.select(element[0])
	  .datum(data)
	  .transition().duration(500).call(chart);

	return chart;
}

function pieChart(scope, element, data) {

    var chart = nv.models.pieChart()
    	.showLabels(false)
    	.x(function(d) { return d.x; })
        .y(function(d) { return d.y; })
        .values(function(d) { return d; })
        .color(d3.scale.category10().range());

	d3.select(element[0])
    	.datum([data])
        .transition().duration(1200).call(chart);
    
    return chart;
}


function makeChart(scope, element, data) {

	var chart_data = [];
	
	function $eval(expr, locals) {
		var context = _.extend({
			"$moment": moment
		}, locals);
		//TODO: user root scope to prevent access to current scope
		return scope.$eval(expr, context);
	}
	
	function $conv(value) {
		if (/\d+(\.\d+)?/.test(value)) {
			return +value;
		}
		return value;
	}

	function barData(s) {
		var key = s.key,
			type = s.type,
			expr = s.expr;

		_.chain(data.data).groupBy(key).each(function(group, name, i) {
			var series = {};
			series.key = name;
			series.type = type;
			series.values = [];
			_.each(group, function(item) {
				var x = item[data.xAxis] || 0;
				var y = expr ? $eval(expr, item) : name;
				series.values.push({
					x: $conv(x),	
					y: $conv(y)
				});
			});
			chart_data.push(series);
		});
	}
	
	function pieData(s) {
		var key = s.key,
			expr = s.expr;
		_.chain(data.data).each(function(item, i) {
			var x = item[data.xAxis];
			var y = expr ? $eval(expr, item) : item[key];
			
			chart_data.push({
				x: $conv(x),
				y: $conv(y)
			});
		});
	}

	var type = null;

	for(var i = 0 ; i < data.series.length ; i++) {
		var s = data.series[i];
		
		type = s.type;

		if (s.type === "pie") {
			pieData(s);
			break;
		}
		if (s.type === "bar") {
			barData(s);
		}
	}
	
	if (data.series.length > 1) {
		type = "multi";
	}

	return nv.addGraph(function() {

		var chart = null;

		if (type == "pie") chart = pieChart(scope, element, chart_data);
		if (type == "bar") chart = barChart(scope, element, chart_data, data.xType);

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

		element.on('adjustSize', chart.update);
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
				makeChart(scope, elem, data);
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
