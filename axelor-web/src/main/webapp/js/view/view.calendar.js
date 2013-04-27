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
			n = _.random(0, 7);
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
	
	$scope.onEventChange = function(event, dayDelta, minuteDelta, allDay) {
		
		var view = this.schema;
		var record = _.clone(event.record);
		
		record[view.eventStart] = event.start;
		record[view.eventStop] = event.end;

		var promise = ds.save(record);
		
		promise.success(function(res){
			
			event.record.version = res.version;
		});
		
		return promise;
	};
	
	$scope.removeEvent = function(event, callback) {
		//XXX: confirm?
		ds.remove(event.record).success(callback);
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

angular.module('axelor.ui').directive('uiViewCalendar', ['ViewService', function(ViewService){
	
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
				
				editable: true,
				
				selectable: true,
				
				selectHelper: true,
				
				select: function(start, end, allDay) {
					var event = {
						start: start,
						end: end,
						allDay: allDay
					};
					setTimeout(function(){
						scope.$apply(function(){
							scope.showEditor(event);
						});
					});
					main.fullCalendar('unselect');
				},

				eventDrop: function(event, dayDelta, minuteDelta, allDay, revertFunc, jsEvent, ui, view) {
					if (bubble) {
						bubble.popover('destroy');
						bubble = null;
					}
					scope.onEventChange(event, dayDelta, minuteDelta, allDay).error(function(){
						revertFunc();
					});
				},
				
				eventResize: function( event, dayDelta, minuteDelta, revertFunc, jsEvent, ui, view ) {
					scope.onEventChange(event, dayDelta, minuteDelta).error(function(){
						revertFunc();
					});
				},
				
				eventClick: function(event, jsEvent, view) {
					showBubble(event, jsEvent);
				},

				events: function(start, end, callback) {
					scope._viewPromise.then(function(){
						scope.fetchItems(start, end, callback);
					});
				},
				
				eventDataTransform: function(record) {
					return updateEvent(null, record);
				},
				
				viewDisplay: function(view) {
					if (bubble) {
						bubble.popover('destroy');
						bubble = null;
					}
					mini.datepicker('setDate', main.fullCalendar('getDate'));
				}
			});
			
			var editor = null;
			var bubble = null;

			function showBubble(event, jsEvent) {
				if (bubble) {
					bubble.popover('destroy');
					bubble = null;
				}
				bubble = $(jsEvent.srcElement).popover({
					html: true,
					title: "<b>" + event.title + "</b>",
					placement: "top",
					content: function() {
						var html = $("<div></div>");
						
						$("<span>").text(moment(event.start).format("LLL")).appendTo(html);
						
						if (event.end) {
							$("<span> - </span>").appendTo(html);
							$("<span>").text(moment(event.end).format("LLL")).appendTo(html);
						}
						
						$("<hr>").appendTo(html);
						$('<a href="javascript: void(0)">Delete</a>')
							.appendTo(html)
							.click(function(e){
								scope.$apply(function(){
									bubble.popover('destroy');
									bubble = null;
									scope.removeEvent(event, function(){
										main.fullCalendar("removeEvents", event.id);
									});
								});
							});
						$('<a class="pull-right" href="javascript: void(0)">Edit event <strong>»</strong></a>')
							.appendTo(html)
							.click(function(e){
								bubble.popover('destroy');
								bubble = null;
								scope.$apply(function(){
									scope.showEditor(event);
								});
							});

						return html;
					}
				});

				bubble.popover('show');
			};
			
			$("body").on("mousedown", function(e){
				var elem = $(e.srcElement);
				if (!bubble || bubble.is(elem) || bubble.has(elem).length) {
					return;
				}
				if (!elem.parents().is(".popover")) {
					bubble.popover("hide");
				}
			});

			function updateEvent(event, record) {
				var view = scope.schema,
					start = moment(record[view.eventStart] || new Date()),
					stop = moment(record[view.eventStop] || new Date());
	
				var diff = moment(stop).diff(start, "minutes");
				
				if (event == null || !event.id) {
					var color = scope.getColor(record);
					event = {
						id: record.id,
						record: record,
						backgroundColor: color.bg,
						borderColor: color.bc
					};
				} else {
					_.extend(event.record, record);
				}

				event.title = record.name;
				event.start = start.toDate();
				event.end = stop.toDate();
				event.allDay = diff <= 0 || diff >= (8 * 60);

				return event;
			}

			scope.editorCanSave = true;
			
			scope.showEditor = function(event) {

				var view = this.schema;
				var record = _.extend({}, event.record);

				record[view.eventStart] = event.start;
				record[view.eventStop] = event.end;

				if (editor == null) {
					editor = ViewService.compile('<div ui-editor-popup></div>')(scope.$new());
					editor.data('$target', element);
				}

				var popup = editor.data('$scope');
				popup.show(record, function(result) {
					if (!record.id && result && result.id) {
						main.fullCalendar('renderEvent', updateEvent(null, result));
					} else {
						main.fullCalendar('renderEvent', updateEvent(event, result));
					}
				});

				if (record == null) {
					popup.$broadcast("on:new");
				}
			};

			scope.select = function(record) {

			};

			main.on("adjustSize", _.debounce(function(){
				if (main.is(':hidden')) {
					return;
				}
				if (bubble) {
					bubble.popover('destroy');
					bubble = null;
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
		template:
		'<div class="webkit-scrollbar-all">'+
			'<div class="calendar-main"></div>'+
			'<div class="calendar-side">'+
				'<div class="calendar-mini"></div>'+
				'<form class="form calendar-legend">'+
					'<label class="checkbox" ng-repeat="color in getColors()" style="color: {{color.color.bc}}">'+
						'<input type="checkbox"> {{color.item.name}}</label>'+
				'</div>'+
			'</div>'+
		'</div>'
	};
}]);
