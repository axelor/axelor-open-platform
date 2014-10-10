/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
				title: schema.items[0].name,
				dayLength: schema.dayLength || 8
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
				fieldName: fieldName,
				operator: ">=",
				value: start
			}, {
				fieldName: fieldName,
				operator: "<=",
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
			if (colorField.selectionList) {
				var select = _.find(colorField.selectionList, function (select) {
					return ("" + select.value) === ("" + title);
				});
				if (select) {
					title = select.title;
				}
			}
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
		var titleText = null;
		
		if (title) {
			value = record[title.name];
			if (title.targetName) {
				value = value[title.targetName];
			} else if (title.selectionList) {
				var select = _.find(title.selectionList, function (select) {
					return ("" + select.value) === ("" + value);
				});
				if (select) {
					titleText = select.title;
				}
			}
			titleText = titleText || value || _t('Unknown');
		}
		
		info.title = ("" + titleText);
		info.allDay = diff <= 0 || diff >= (view.dayLength * 60);
		info.className = info.allDay ? "calendar-event-allDay" : "calendar-event-day";
		
		return info;
	};
	
	$scope.onEventChange = function(event, dayDelta, minuteDelta, allDay) {
		
		var record = _.clone(event.record);

		record[view.start] = event.start;
		record[view.stop] = event.end;
		
		$scope.record = record;
		
		function reset() {
			$scope.record = null;
		}

		function doSave() {
			return ds.save(record).success(function(res){
				var info = $scope.getEventInfo(record);
				event.record = _.extend(event.record, info.record);
				event.start = info.start;
				event.end = info.end;
				event.allDay = info.allDay;
				event.record.version = res.version;
				return $scope.refresh();
			});
		}
		
		var handler = $scope.onChangeHandler;
		if (handler) {
			var promise = handler.onChange().then(function () {
				return doSave();
			});
			promise.success = function(fn) {
				promise.then(fn);
				return promise;
			};
			promise.error = function (fn) {
				promise.then(null, fn);
				return promise;
			};
			return promise;
		}

		return doSave();
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
		
		if (opts.mode === "calendar") {
			return $scope.updateRoute();
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

angular.module('axelor.ui').directive('uiViewCalendar', ['ViewService', 'ActionService', function(ViewService, ActionService) {
	
	function link(scope, element, attrs, controller) {

		var main = element.children('.calendar-main');
		var mini = element.find('.calendar-mini');
		
		var schema = scope.schema;
		var mode = schema.mode || "month";
		var editable = schema.editable === undefined ? true : schema.editable;
		var calRange = {};
		
		var RecordManager = (function () {

			var records = [],
				current = [];

			function add(record) {
				if (!record || _.isArray(record)) {
					records = record || [];
					return filter();
				}
				var found = _.findWhere(records, {
					id: record.id
				});
				if (!found) {
					found = record;
					records.push(record);
				}
				if (found !== record) {
					_.extend(found, record);
				}
				return filter();
			}

			function remove(record) {
				records = _.filter(records, function (item) {
					return record.id !== item.id;
				});
				return filter();
			}

			function filter() {

				var selected = [];
				
				_.each(scope.getColors(), function (color) {
					if (color.checked) {
						selected.push(scope.getColorKey(null, color.item));
					}
				});
				
				main.fullCalendar('removeEventSource', current);
				current = records;

				if (selected.length) {
					current = _.filter(records, function(record) {
						return _.contains(selected, record.$colorKey);
					});
				}

				main.fullCalendar('addEventSource', current);
				adjustSize();
			}

			return {
				add: add,
				remove: remove,
				filter: filter
			};
		}());

		mini.datepicker({
			showOtherMonths: true,
			selectOtherMonths: true,
			onSelect: function(dateStr) {
				main.fullCalendar('gotoDate', mini.datepicker('getDate'));
			}
		});
		
		main.fullCalendar({
			
			header: false,
			
			timeFormat: {
				agenda: 'h(:mm)t{ - h(:mm)t}',
				'': 'h(:mm)t'
			},
			
			editable: editable,
			
			selectable: editable,
			
			selectHelper: editable,
			
			select: function(start, end, allDay) {
				var event = {
					start: start,
					end: end,
					allDay: allDay
				};
				scope.applyLater(function(){
					scope.showEditor(event);
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
				showBubble(event, jsEvent.target);
			},

			events: function(start, end, callback) {
				calRange.start = start;
				calRange.end = end;
				scope._viewPromise.then(function(){
					scope.fetchItems(start, end, function(records) {
						callback([]);
						RecordManager.add(records);
					});
				});
			},

			eventDataTransform: function(record) {
				return updateEvent(null, record);
			},
			
			viewDisplay: function(view) {
				hideBubble();
				mini.datepicker('setDate', main.fullCalendar('getDate'));
			},

			allDayText: _t('All Day'),

			monthNames: ($.datepicker._defaults || {}).monthNames,

			monthNamesShort: ($.datepicker._defaults || {}).monthNamesShort,

			dayNames: ($.datepicker._defaults || {}).dayNames,

			dayNamesShort: ($.datepicker._defaults || {}).dayNamesShort,

			firstDay: ($.datepicker._defaults || {}).firstDay
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
				container: 'body',
				content: function() {
					var html = $("<div></div>").addClass("calendar-bubble-content");
					
					$("<span>").text(moment(event.start).format("LLL")).appendTo(html);
					
					if (event.end) {
						$("<span> - </span>").appendTo(html);
						$("<span>").text(moment(event.end).format("LLL")).appendTo(html);
					}
					
					$("<hr>").appendTo(html);
					
					if (scope.isEditable()) {
						$('<a href="javascript: void(0)" style="margin-right: 5px;"></a>').text(_t("Delete"))
						.appendTo(html)
						.click(function(e){
							hideBubble();
							scope.$apply(function(){
								scope.removeEvent(event, function() {
									RecordManager.remove(event.record);
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
			var elem = $(e.target || e.srcElement);
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
				RecordManager.add(result);
			});
			if (record == null || !record.id) {
				popup.ajaxStop(function() {
					scope.applyLater(function() {
						popup.$broadcast("on:new");
					});
				});
			}
		};
		
		scope.isEditable = function() {
			return editable;
		};

		scope.refresh = function(record) {
			main.fullCalendar("refetchEvents");
		};
		
		scope.filterEvents = function() {
			RecordManager.filter();
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
		
		scope.onRefresh = function () {
			if (!calRange.start) return;
			scope.fetchItems(calRange.start, calRange.end, function (records) {
				RecordManager.add(records);
			});
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
		
		scope.onChangeHandler = null;
		if (schema.onChange) {
			scope.onChangeHandler = ActionService.handler(scope, element, {
				action: schema.onChange
			});
		}
		
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

		scope.$callWhen(function () {
			return main.is(':visible');
		}, function() {
			scope.onMode(mode);
			adjustSize();
		}, 100);
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
						'<input type="checkbox" ng-click="filterEvents()" ng-model="color.checked"> {{color.title}}</label>'+
				'</div>'+
			'</div>'+
		'</div>'
	};
}]);
