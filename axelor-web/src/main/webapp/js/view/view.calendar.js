/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
(function() {

/* global d3: true */

"use strict";

var ui = angular.module('axelor.ui');

ui.controller('CalendarViewCtrl', CalendarViewCtrl);

CalendarViewCtrl.$inject = ['$scope', '$element'];
function CalendarViewCtrl($scope, $element) {

  ui.DSViewCtrl('calendar', $scope, $element);

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
        title: schema.items[0].name
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
      fg: "" + c.brighter(99),
      bc: "" + c.darker(0.9)
    };
  }

  $scope.fetchItems = function(start, end, callback) {
    start = start.clone().local();
    end = end.clone().local();
    var fields = _.pluck(this.fields, 'name');
    var criteria = {
      operator: "and",
      criteria: [{
        fieldName: view.start,
        operator: ">=",
        value: start
      }, {
        fieldName: view.start,
        operator: "<=",
        value: end
      }]
    };

    // make sure to include items whose end date falls in current range
    if (view.stop) {
      criteria = {
        operator: "or",
        criteria: [criteria, {
          operator: "and",
          criteria: [{
            fieldName: view.stop,
            operator: ">=",
            value: start
          }, {
            fieldName: view.start,
            operator: "<=",
            value: end
          }]
        }]
      };
    }

    // consider stored filter
    if (ds._filter) {
      if (ds._filter.criteria) {
        criteria = {
          operator: "and",
          criteria: [criteria].concat(ds._filter.criteria)
        };
      }
      if (_.size(ds._filter._domains) > 0) {
        criteria._domains = ds._filter._domains;
      }
    }

    var opts = {
      fields: fields,
      filter: criteria,
      domain: this._domain,
      context: this._context,
      store: false,
      limit: -1
    };

    ds.search(opts).success(function(records) {
      var items = _.clone(records);
      items.sort(function (x, y) { return x.id - y.id; });
      updateColors(items, true);
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
      if (item === null || item === undefined) {
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
    info.start = value ? moment(value) : moment();

    value = record[view.stop];
    info.end = value ? moment(value) : moment(info.start).add(view.length || 1, "hours");

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
    info.allDay = isAllDay(info);
    info.className = info.allDay ? "calendar-event-allDay" : "calendar-event-day";

    return info;
  };

  function isAllDay(event) {
    if($scope.fields[view.start] && $scope.fields[view.start].type === 'date') {
      return true;
    }
    var start = moment(event.start);
    var end = moment(event.end);
    if (start.format("HH:mm") !== "00:00") {
      return false;
    }
    return !event.end || end.format("HH:mm") === "00:00";
  }

  $scope.onEventChange = function(event, delta) {

    var record = _.clone(event.record);

    var start = event.start;
    var end = event.end;

    if (isAllDay(event)) {
      start = start.clone().startOf("day").local();
      end = (end || start).clone().startOf("day").local();
    }

    record[view.start] = start;
    record[view.stop] = end;

    $scope.record = record;

    function reset() {
      $scope.record = null;
    }

    function doSave() {
      return ds.save(record).success(function(res){
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
    var legend = element.find('.calendar-legend');

    var ctx = (scope._viewParams.context||{});
    var params = (scope._viewParams.params||{});

    var schema = scope.schema;
    var mode = ctx.calendarMode || params.calendarMode || schema.mode || "month";
    var date = ctx.calendarDate || params.calendarDate;

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

      function events(start, end, timezone, callback) {
        calRange.start = start;
        calRange.end = end;
        scope._viewPromise.then(function(){
          scope.fetchItems(start, end, function(items) {
            callback([]);
            add(items);
          });
        });
      }

      return {
        add: add,
        remove: remove,
        filter: filter,
        events: events
      };
    }());

    if (date) {
      date = moment(date).toDate();
    }

    mini.datepicker({
      showOtherMonths: true,
      selectOtherMonths: true,
      onSelect: function(dateStr) {
        main.fullCalendar('gotoDate', mini.datepicker('getDate'));
      }
    });

    if (date) {
      mini.datepicker('setDate', date);
    }

    var lang = axelor.config["user.lang"] || 'en';

    var options = {

      header: false,

      timeFormat: 'h(:mm)t',

      axisFormat: 'h(:mm)t',

      timezone: 'local',

      lang: lang,

      firstDay: mini.datepicker('option', 'firstDay'),

      editable: editable,

      selectable: editable,

      selectHelper: editable,

      select: function(start, end) {
        var event = {
          start: start,
          end: end
        };

        // all day
        if (!start.hasTime() && !end.hasTime()) {
          event.start = start.clone().startOf("day").local();
          event.end = end.clone().startOf("day").local();
        }

        scope.$applyAsync(function(){
          scope.showEditor(event);
        });
        main.fullCalendar('unselect');
      },

      defaultDate: date,

      events: RecordManager,

      eventDrop: function(event, delta, revertFunc, jsEvent, ui, view) {
        hideBubble();
        scope.onEventChange(event, delta).error(function(){
          revertFunc();
        });
      },

      eventResize: function( event, delta, revertFunc, jsEvent, ui, view ) {
        scope.onEventChange(event, delta).error(function(){
          revertFunc();
        });
      },

      eventClick: function(event, jsEvent, view) {
        showBubble(event, jsEvent.target);
      },

      eventDataTransform: function(record) {
        return updateEvent(null, record);
      },

      viewDisplay: function(view) {
        hideBubble();
        mini.datepicker('setDate', main.fullCalendar('getDate'));
      },

      allDayText: _t('All Day')
    };

    if (lang.indexOf('fr') === 0) {
      _.extend(options, {
        timeFormat: 'H:mm',
        axisFormat: 'H:mm',

        views: {
          week: {
            titleFormat: 'D MMM YYYY',
            columnFormat: 'ddd DD/MM'
          },

          day: {
            titleFormat: 'D MMM YYYY'
          }
        }
      });
    }

    main.fullCalendar(options);

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
          var start = event.start;
          var end = event.end && event.allDay ? moment(event.end).add(-1, 'second') : event.end;
          var singleDay = (event.allDay || !scope.isAgenda()) && (!end || moment(start).isSame(end, 'day'));
          var dateFormat = !scope.isAgenda() || event.allDay ? "ddd D MMM" : "ddd D MMM HH:mm";

          $("<span>").text(moment(start).format(dateFormat)).appendTo(html);

          if (schema.eventStop && end && !singleDay) {
            $("<span> - </span>").appendTo(html);
            $("<span>").text(moment(end).format(dateFormat)).appendTo(html);
          }

          $("<hr>").appendTo(html);

          if (scope.isEditable()) {
            $('<a href="javascript: void(0)" style="margin-right: 5px;"></a>').text(_t("Delete"))
            .appendTo(html)
            .click(function(e){
              hideBubble();
              scope.$applyAsync(function(){
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
            scope.$applyAsync(function(){
              scope.showEditor(event);
            });
          });

          return html;
        }
      });

      bubble.popover('show');
    }

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
      if (!event || !event.id) {
        var color = scope.getColor(record);
        event = {
          id: record.id,
          record: record,
          backgroundColor: color.bg,
          borderColor: color.bc,
          textColor: color.fg
        };
      } else {
        _.extend(event.record, record);
      }

      event = _.extend(event, scope.getEventInfo(record));

      return event;
    }

    scope.editorCanSave = true;

    scope.showEditor = function(event) {

      var view = this.schema;
      var record = _.extend({}, event.record);

      record[view.eventStart] = event.start ? event.start.clone().local() : event.start;
      record[view.eventStop] = event.end ? event.end.clone().local() : event.end;

      if (!editor) {
        editor = ViewService.compile('<div ui-editor-popup></div>')(scope.$new());
        editor.data('$target', element);
      }

      var popup = editor.isolateScope();

      popup.show(record, function(result) {
        RecordManager.add(result);
      });
      popup.waitForActions(function() {
        if (!record || !record.id) {
          popup.$broadcast("on:new");
        } else {
          popup.setEditable(scope.isEditable());
        }
      });
    };

    scope.isEditable = function() {
      return editable;
    };

    scope.refresh = function(record) {
      if (calRange.start && calRange.end) {
        return RecordManager.events(calRange.start, calRange.end, options.timezone, function () {});
      }
      return main.fullCalendar("refetchEvents");
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
      scope.refresh();
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
      main.fullCalendar('render');
      main.fullCalendar('option', 'height', element.height());
      legend.css("max-height", legend.parent().height() - mini.height()
          - (parseInt(legend.css('marginTop')) || 0)
          - (parseInt(legend.css('marginBottom')) || 0));
    }

    scope.$onAdjust(adjustSize, 100);

    scope.$callWhen(function () {
      return main.is(':visible');
    }, function() {
      if (scope.viewType !== 'dashboard') {
        element.parents('.view-container:first').css('overflow', 'inherit');
      }
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
    '<div>'+
      '<div class="calendar-main" ui-attach-scroll=".fc-scroller"></div>'+
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

angular.module('axelor.ui').directive('uiPortletCalendar', function () {
  return {
    controller: CalendarViewCtrl,
    replace: true,
    link: function (scope, element, attrs) {
      var colSpan = (scope.dashlet||{}).colSpan || 6;
      var height = (scope.dashlet||{}).height;
      height = height || (50 * colSpan);
      setTimeout(function () {
        element.parent().height(height);
      });
      scope.showPager = true;
    },
    template:
      "<div class='portlet-calendar' ui-portlet-refresh>" +
        "<div ui-view-calendar x-handler='this'></div>" +
      "</div>"
  };
});

})();
