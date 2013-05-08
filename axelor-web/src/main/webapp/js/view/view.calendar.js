CalendarViewCtrl.$inject = ['$scope', '$element'];
function CalendarViewCtrl($scope, $element) {

	DSViewCtrl('calendar', $scope, $element);

	var ds = $scope._dataSource;

	var view = {};
	var colors = {};
	
	var initialized = false;

	$scope.onShow = function(viewPromise) {

		if (initialized) {
			return $scope.refresh();
		}

		viewPromise.then(function(){

			var schema = $scope.schema;

			initialized = true;
			
			view = {
				start: schema.eventStart,
				stop: schema.eventStop,
				length: parseInt(schema.eventLength) || 0,
				color: schema.colorBy,
				title:schema.items[0].name
			};

			$scope._viewResolver.resolve(schema, $element);
			$scope.updateRoute();
		});
	};

	$scope.isAgenda = function() {
		var field = this.fields[view.start];
		return field && field.type === "datetime";
	};

	var d3_colors = d3.scale.category10().range().concat(
				    d3.scale.category20().range());

	function nextColor(n) {
		if (n === undefined || n < 0 || n >= d3_colors.length) {
			n = _.random(0, d3_colors.length);
		}
		var c = d3.rgb(d3_colors[n]);
		return {
			bg: "" + c,
			fg: "" + c.brighter(1),
			bc: "" + c.darker(.9)
		};
	};
	
	$scope.fetchItems = function(start, end, callback) {

		var fields = _.pluck(this.fields, 'name');
		var fieldName = view.start;

		var criteria = {
			operator: "and",
			criteria: [{
				operator: "greaterOrEqual",
				fieldName: fieldName,
				value: start
			}, {
				operator: "lessOrEqual",
				fieldName: fieldName,
				value: end
			}]
		};
		
		// consider stored filter
		if (ds._filter) {
			_.each(ds._filter.criteria, function(criterion) {
				criteria.criteria.push(criterion);
			});
		}

		var opts = {
			fields: fields,
			filter: criteria,
			domain: this._domain,
			context: this._context,
			store: false
		};

		ds.search(opts).success(function(records) {
			updateColors(records, true);
			callback(records);
		});
	};

	function updateColors(records, reset) {
		var colorBy = view.color;
		var colorField = $scope.fields[colorBy];

		if (!colorField) {
			return colors;
		}
		if (reset) {
			colors = {};
		}

		_.each(records, function(record) {
			var item = record[colorBy];
			if (!item) {
				return;
			}
			var key = $scope.getColorKey(record, item);
			var title = colorField.targetName ? item[colorField.targetName] : item;
			if (!colors[key]) {
				colors[key] = {
					item: item,
					title: title || _t('Unknown'),
					color: nextColor(_.size(colors))
				};
			}
			record.$colorKey = key;
		});
		return colors;
	}
	
	$scope.getColors = function() {
		return colors;
	};
	
	$scope.getColor = function(record) {
		var key = this.getColorKey(record);
		if (key && !colors[key]) {
			updateColors([record], false);
		}
		if (colors[key]) {
			return colors[key].color;
		}
		return nextColor(0);
	};
	
	$scope.getColorKey = function(record, key) {
		if (key) {
			return "" + (key.id || key);
		}
		if (record) {
			return this.getColorKey(null, record[view.color]);
		}
		return null;
	};
	
	$scope.getEventInfo = function(record) {
		var info = {},
			value;

		value = record[view.start];
		info.start = value ? moment(value).toDate() : new Date();

		value = record[view.stop];
		info.end = value ? moment(value).toDate() : moment(info.start).add("hours", view.length || 1).toDate();

		var diff = moment(info.end).diff(info.start, "minutes");
		var title = this.fields[view.title];

		if (title) {
			value = record[title.name];
			if (title.targetName) {
				value = value[title.targetName];
			}
			info.title = value;
		}

		info.allDay = diff <= 0 || diff >= (8 * 60);
		info.className = info.allDay ? "calendar-event-allDay" : "calendar-event-day";
		
		return info;
	};
	
	$scope.onEventChange = function(event, dayDelta, minuteDelta, allDay) {
		
		var record = _.clone(event.record);

		record[view.start] = event.start;
		record[view.stop] = event.end;

		var promise = ds.save(record);
		
		promise.success(function(res){
			//TODO: update record
			event.record.version = res.version;
		});
		
		return promise;
	};
	
	$scope.removeEvent = function(event, callback) {
		ds.remove(event.record).success(callback);
	};
	
	$scope.select = function() {
		
	};
	
	$scope.refresh = function() {
		
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
		
		if (opts.state === "calendar") {
			return;
		}
		var params = $scope._viewParams;
		if (params.viewType !== "calendar") {
			return $scope.show();
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
	
	function link(scope, element, attrs, controller) {

		var main = element.children('.calendar-main');
		var mini = element.find('.calendar-mini');
		
		var schema = scope.schema;
		var mode = schema.mode || "month";
		var editable = schema.editable === undefined ? true : schema.editable;

		var EventManager = (function() {

			var all = Array();
			var filtered = Array();
			
			return {
				
				setEvents : function(events) {
					all = events;
				},
				
				filter: function() {
					var selected = element.find('.calendar-legend input:checked').map(function(){
						var child = $(this).parent().data('$scope');
						var item = child.color.item;
						return scope.getColorKey(null, item);
					}).toArray();

					main.fullCalendar('removeEventSource', filtered);
					filtered = all;
					
					if (selected.length) {
						filtered = _.filter(all, function(event) {
							return _.contains(selected, event.$colorKey);
						});
					}
					
					main.fullCalendar('addEventSource', filtered);
					adjustSize();
				}
			};
		})();

		mini.datepicker({
			showOtherMonths: true,
			selectOtherMonths: true,
			onSelect: function(dateStr) {
				main.fullCalendar('gotoDate', mini.datepicker('getDate'));
			}
		});
		
		main.fullCalendar({
			
			header: false,
			
			editable: editable,
			
			selectable: editable,
			
			selectHelper: editable,
			
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
				hideBubble();
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
				showBubble(event, jsEvent.srcElement);
			},

			events: function(start, end, callback) {
				scope._viewPromise.then(function(){
					scope.fetchItems(start, end, function(records) {
						callback([]);
						EventManager.setEvents(records);
						EventManager.filter();
					});
				});
			},

			eventDataTransform: function(record) {
				return updateEvent(null, record);
			},
			
			viewDisplay: function(view) {
				hideBubble();
				mini.datepicker('setDate', main.fullCalendar('getDate'));
			}
		});
		
		var editor = null;
		var bubble = null;
		
		function hideBubble() {
			if (bubble) {
				bubble.popover('destroy');
				bubble = null;
			}
		}

		function showBubble(event, elem) {
			hideBubble();
			bubble = $(elem).popover({
				html: true,
				title: "<b>" + event.title + "</b>",
				placement: "top",
				content: function() {
					var html = $("<div></div>").addClass("calendar-bubble-content");
					
					$("<span>").text(moment(event.start).format("LLL")).appendTo(html);
					
					if (event.end) {
						$("<span> - </span>").appendTo(html);
						$("<span>").text(moment(event.end).format("LLL")).appendTo(html);
					}
					
					$("<hr>").appendTo(html);
					
					if (scope.isEditable()) {
						$('<a href="javascript: void(0)"></a>').text(_t("Delete"))
						.appendTo(html)
						.click(function(e){
							hideBubble();
							scope.$apply(function(){
								scope.removeEvent(event, function(){
									main.fullCalendar("removeEvents", event.id);
								});
							});
						});
					}
					
					$('<a class="pull-right" href="javascript: void(0)"></a>')
					.append(_t("Edit event")).append("<strong> »</strong>")
					.appendTo(html)
					.click(function(e){
						hideBubble();
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
				hideBubble();
			}
		});

		function updateEvent(event, record) {
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

			event = _.extend(event, scope.getEventInfo(record));
			
			if (!event.allDay) {
				event.textColor = event.borderColor;
			}

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
			
			popup.setEditable(scope.isEditable());
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
		
		scope.isEditable = function() {
			return editable;
		};

		scope.refresh = function(record) {
			main.fullCalendar("refetchEvents");
		};
		
		scope.filterEvents = function() {
			EventManager.filter();
		};

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
		
		function adjustSize() {
			if (main.is(':hidden')) {
				return;
			}
			hideBubble();
			main.css('right', mini.parent().outerWidth(true));
			main.fullCalendar('render');
			main.fullCalendar('option', 'height', element.height());
		}

		main.on("adjustSize", _.debounce(adjustSize, 100));
		setTimeout(function() {
			scope.onMode(mode);
			adjustSize();
		});
	}
	
	return {
		link: function(scope, element, attrs, controller) {
			scope._viewPromise.then(function(){
				link(scope, element, attrs, controller);
			});
		},
		replace: true,
		template:
		'<div class="webkit-scrollbar-all">'+
			'<div class="calendar-main"></div>'+
			'<div class="calendar-side">'+
				'<div class="calendar-mini"></div>'+
				'<form class="form calendar-legend">'+
					'<label class="checkbox" ng-repeat="color in getColors()" style="color: {{color.color.bc}}">'+
						'<input type="checkbox" ng-click="filterEvents()"> {{color.title}}</label>'+
				'</div>'+
			'</div>'+
		'</div>'
	};
}]);
