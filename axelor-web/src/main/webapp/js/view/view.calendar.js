CalendarViewCtrl.$inject = ['$scope', '$element'];
function CalendarViewCtrl($scope, $element) {

	DSViewCtrl('calendar', $scope, $element);

	var ds = $scope._dataSource;
	
	var colorBy = null;
	var colors = {};

	$scope.onShow = function(viewPromise) {
		
		viewPromise.then(function(){
			var view = $scope.schema;
			
			colorBy = view.colorBy;
			
			$scope._viewResolver.resolve(view, $element);
			$scope.updateRoute();
		});
	};
	
	$scope.isAgenda = function() {
		var view = this.schema || {},
			field = this.fields[view.eventStart] || {};
		return field.type === "datetime";
	};
	
	var tango = new Array(
		["#fce94f", "#edd400", "#c4a000"],
		["#fcaf3e", "#f57900", "#ce5c00"],
		["#e9b96e", "#c17d11", "#8f5902"],
		["#8ae234", "#73d216", "#4e9a06"],
		["#729fcf", "#3465a4", "#204a87"],
		["#ad7fa8", "#75507b", "#5c3566"],
		["#ef2929", "#cc0000", "#a40000"]).reverse();

	function nextColor(n) {
		if (n === undefined || n < 0 || n > 7) {
			n = _.randrom(0, 7);
		}
		var c = tango[n];
		return {
			bg: c[0],
			fg: c[2],
			bc: c[1]
		};
	};
	
	$scope.fetchItems = function(start, end, callback) {
		
		var view = this.schema;
		var fields = _.pluck(this.fields, 'name');

		var criteria = {
			operator: "and",
			criteria: [{
				operator: "greaterOrEqual",
				fieldName: view.eventStart,
				value: start
			}, {
				operator: "lessOrEqual",
				fieldName: view.eventStart,
				value: end
			}]
		};
		
		var opts = {
			fields: fields,
			filter: criteria,
			domain: this._domain,
			context: this._context
		};

		ds.search(opts).success(function(records) {
			colors = {};
			_.each(records, function(record) {
				var item = record[colorBy];
				if (!item) return;
				var key = item.id ? item.id : item;
				if (!colors[key]) {
					colors[key] = {
						item: item,
						color: nextColor(_.size(colors))
					};
				}
			});
			callback(records);
		});
	};
	
	$scope.getColors = function() {
		return colors;
	};
	
	$scope.getColor = function(record) {
		var item = record[colorBy];
		if (item && colors[item.id || item]) {
			return colors[item.id || item].color;
		}
		return nextColor();
	};

	$scope.getRouteOptions = function() {
		var args = [],
			query = {};

		return {
			mode: 'calendar',
			args: args,
			query: query
		};
	};
	
	$scope.setRouteOptions = function(options) {
		var opts = options || {};

		if (opts.state !== "calendar") {
			$scope.show();
		}
	};

	$scope.canNext = function() {
		return true;
	};
	
	$scope.canPrev = function() {
		return true;
	};
}

angular.module('axelor.ui').directive('uiViewCalendar', function(){
	
	return {
		
		link: function(scope, element, attrs, controller) {
		
			var main = element.children('.calendar-main');
			var mini = element.find('.calendar-mini');
			var mode = "month";

			mini.datepicker({
				showOtherMonths: true,
				selectOtherMonths: true,
				onSelect: function(dateStr) {
					main.fullCalendar('gotoDate', mini.datepicker('getDate'));
				}
			});
			
			main.fullCalendar({
				header: false,
				events: function(start, end, callback) {
					scope._viewPromise.then(function(){
						scope.fetchItems(start, end, callback);
					});
				},
				eventDataTransform: function(record) {
					var view = scope.schema,
						start = moment(record[view.eventStart] || new Date()),
						stop = moment(record[view.eventStop] || new Date());

					var color = scope.getColor(record);
					var diff = moment(stop).diff(start, "minutes");

					var event = {
						record: record,
						title: record.name,
						start: start.toDate(),
						end: stop.toDate(),
						allDay: diff <= 0 || diff >= (8 * 60)
					};
					
					event.backgroundColor = color.bg;
					event.borderColor = color.bc;

					return event;
				},
				viewDisplay: function(view) {
					mini.datepicker('setDate', main.fullCalendar('getDate'));
				}
			});

			main.on("adjustSize", _.debounce(function(){
				if (main.is(':hidden')) {
					return;
				}
				main.fullCalendar('render');
				main.fullCalendar('option', 'height', element.height());
				main.css('right', mini.parent().outerWidth(true));
			}, 100));

			scope.pagerText = function() {
				return main.fullCalendar("getView").title;
			};

			scope.isMode = function(name) {
				return mode === name;
			};

			scope.onMode = function(name) {
				mode = name;
				if (name === "week") {
					name = scope.isAgenda() ? "agendaWeek" : "basicWeek";
				}
				if (name === "day") {
					name = scope.isAgenda() ? "agendaDay" : "basicDay";
				}
				main.fullCalendar("changeView", name);
			};

			scope.onNext = function() {
				main.fullCalendar('next');
			};

			scope.onPrev = function() {
				main.fullCalendar('prev');
			};
			
			scope.onToday = function() {
				main.fullCalendar('today');
			};
			
		},
		replace: true,
		template: '<div class="webkit-scrollbar">'+
			'<div class="calendar-main"></div>'+
			'<div class="calendar-side">'+
				'<div class="calendar-mini"></div>'+
				'<form class="form calendar-legend">'+
					'<label class="checkbox" ng-repeat="color in getColors()" style="color: {{color.color.bc}}"><input type="checkbox"> {{color.item.name}}</label>'+
				'</div>'+
			'</div>'+
		'</div>'
	};
});
