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
// localization

(function () {

/* global gantt: true */

"use strict";

$(function () {
  $('<script>')
    .attr('type', 'text/javascript')
    .attr('src', 'https://export.dhtmlx.com/gantt/api.js').appendTo('head');
});

var regional = {
  month_full: [
    _t('January'),
    _t('February'),
    _t('March'),
    _t('April'),
    _t('May'),
    _t('June'),
    _t('July'),
    _t('August'),
    _t('September'),
    _t('October'),
    _t('November'),
    _t('December')],
  month_short: [
    _t('Jan'),
    _t('Feb'),
    _t('Mar'),
    _t('Apr'),
    _t('May'),
    _t('Jun'),
    _t('Jul'),
    _t('Aug'),
    _t('Sep'),
    _t('Oct'),
    _t('Nov'),
    _t('Dec')],
  day_full: [
    _t('Sunday'),
    _t('Monday'),
    _t('Tuesday'),
    _t('Wednesday'),
    _t('Thursday'),
    _t('Friday'),
    _t('Saturday')],
  day_short :	[_t('Sun'), _t('Mon'), _t('Tue'), _t('Wed'), _t('Thu'), _t('Fri'), _t('Sat')]
};

gantt.locale = {
  date: regional,
  labels:{
    new_task: _t("New task"),
    icon_save: _t("Save"),
    icon_cancel: _t("Cancel"),
    icon_details: _t("Details"),
    icon_edit: _t("Edit"),
    icon_delete: _t("Delete"),
    confirm_closing:"",// Your changes will be lost, are your sure ?
    confirm_deleting: _t("Task will be deleted permanently, are you sure?"),
        section_description: _t("Description"),
        section_time: _t("Time period"),
    section_type: _t("Type"),

        /* grid columns */

        column_text : _t("Task name"),
        column_start_date : _t("Start time"),
        column_duration : _t("Duration"),
        column_add : "",

    /* link confirmation */
    link: _t("Link"),
    confirm_link_deleting: _t("will be deleted"),
    link_start: " " + _t("(start)"),
    link_end: " " + _t("(end)"),

    type_task: _t("Task"),
    type_project: _t("Project"),
    type_milestone: _t("Milestone"),

        minutes: _t("Minutes"),
        hours: _t("Hours"),
        days: _t("Days"),
        weeks: _t("Week"),
        months: _t("Months"),
        years: _t("Years")
  }
};

var ui = angular.module('axelor.ui');
ui.controller('GanttViewCtrl', GanttViewCtrl);

GanttViewCtrl.$inject = ['$scope', '$element'];

function GanttViewCtrl($scope, $element) {

  ui.DSViewCtrl('gantt', $scope, $element);
  var ds = $scope._dataSource;
  var view = $scope._views.gantt;
  var initialized = false;
  var offset = 0;
  var dsPage = null;

  $scope.onShow = function(viewPromise) {

    if (initialized) {
      return $scope.refresh();
    }

    viewPromise.then(function(){
      var schema = $scope.schema;
      initialized = true;
      $scope._viewResolver.resolve(schema, $element);
      $scope.updateRoute();
    });

  };

  $scope.select = function() {

  };

  $scope.fetchItems = function(callback) {

    var schema = $scope.schema;

    var searchFields = _.pluck(this.fields, "name");
    searchFields.push(schema.taskStart);

    var optionalFields = [schema.taskProgress,
                          schema.taskEnd,
                          schema.taskDuration,
                          schema.taskParent,
                          schema.taskSequence,
                          schema.taskProgress,
                          schema.finishToStart,
                          schema.startToStart,
                          schema.finishToFinish,
                          schema.startToFinish,
                          schema.taskUser
    ];

    _.each(optionalFields,function(optField){
      if(optField){
        searchFields.push(optField);
      }
    });

    var opts = {
      fields: searchFields,
      filter: false,
      domain: this._domain,
      store: false,
      offset:offset
    };

    function fetchParents(records,cb) {

      var recordIds = _.chain(records)
                          .map(function(record) {
                            return record.id;
                          })
                          .uniq()
                          .value();

      var _parentIds = _.chain(records)
                          .map(function(record) {
                                return record[schema.taskParent] ?
                                        record[schema.taskParent].id :
                                        null;
                          })
                          .filter(function(id) {
                                return id && !recordIds.includes(id);
                          })
                          .uniq()
                          .value();
      if(_parentIds.length == 0) {
        return cb(records);
      }

      opts=_.extend(opts, {
        domain: "self.id in (:_parentIds) and " + opts.domain,
        context: { _parentIds: _parentIds},
        offset:0
      });

      ds.search(opts).success(function(parentRecords) {
        records=records.concat(parentRecords);
        fetchParents(records,cb);
      });
    }

    ds.search(opts).success(function(records, page) {
      dsPage=page;
      fetchParents(records,function(records){
        callback(records);
      });
    });

  };

  $scope.getContext = function() {
    return _.extend({}, $scope._context);
  };

  $scope.getRouteOptions = function() {
    return {
      mode: 'gantt',
      args: []
    };
  };

  $scope.setRouteOptions = function(options) {
    var opts = options || {};
    if (opts.mode === "gantt") {
      return $scope.updateRoute();
    }
    var params = $scope._viewParams;
    if (params.viewType !== "calendar") {
      return $scope.show();
    }
  };

  $scope.doSave  = function(task, callback){
       var record = _.clone(task.record);
    return ds.save(record).success(function(res){
      callback(task, res);
    });
  };

  $scope.canNext = function() {
    var page = dsPage;
    return page && page.to < page.total;
  };

   $scope.canPrev = function() {
    var page = dsPage;
    return page && page.from > 0;
  };

   $scope.onNext = function() {
    var page = dsPage;
    offset= page.from + page.limit;
    $scope.onRefresh();
  };

   $scope.onPrev = function() {
    var page = dsPage;
    offset= Math.max(0, page.from - page.limit);
    $scope.onRefresh();
  };

   $scope.pagerText = function() {
    var page = dsPage;
    if (page && page.from !== undefined) {
      if (page.total === 0) return null;
      return _t("{0} to {1} of {2}", page.from + 1, page.to, page.total);
    }
  };

  $scope.doRemove = function(id, task){
       var record = _.clone(task.record);
    return ds.remove(record).success(function(res){
      return true;
    });
  };

}

ui.directive('uiViewGantt', ['ViewService', 'ActionService', function(ViewService, ActionService) {

  function link(scope, element, attrs, controller) {
    var main = element.children(".gantt-main");
    var schema = scope.schema;
    var fields = scope.fields;
    var fieldNames = _.pluck(schema.items, "name");
    var firstField = fields[fieldNames[0]];
    var mode = schema.mode || "week";
    var editor = null;
    ganttInit();

    function byId(list, id) {
      for (var i = 0; i < list.length; i++) {
        if (list[i].key == id)
          return list[i].label || "";
      }
      return "";
    }

    function setScaleConfig(value){

      switch (value) {
        case "day":
          gantt.config.scale_unit = "day";
          gantt.config.date_scale = "%d/%m/%Y";
          gantt.config.subscales = [{unit:"hour", step:1, date:"%H:%i"}];
          gantt.templates.date_scale = null;
          gantt.config.min_column_width = 50;
          break;
        case "week":
          var weekScaleTemplate = function(date){
            var dateToStr = gantt.date.date_to_str("%d/%m/%Y");
            var endDate = gantt.date.add(gantt.date.add(date, 1, "week"), -1, "day");
            return gantt.date.date_to_str("%W")(date) + "(" + dateToStr(date) + " - " + dateToStr(endDate) + ")";
          };
          gantt.config.scale_unit = "week";
          gantt.templates.date_scale = weekScaleTemplate;
          gantt.config.min_column_width = 50;
          gantt.config.subscales = [
            {unit:"day", step:1, date:"%D %d" }];
          break;
        case "month":
          gantt.config.scale_unit = "month";
          gantt.config.date_scale = "%F, %Y";
          gantt.config.subscales = [
            {unit:"week", step:1, date:"%W" }
          ];
          gantt.templates.date_scale = null;
          gantt.config.min_column_width = 50;
          break;
        case "year":
          gantt.config.scale_unit = "year";
          gantt.config.date_scale = "%Y";
          gantt.templates.date_scale = null;
          gantt.config.min_column_width = 100;
          gantt.config.subscales = [
            {unit:"month", step:1, date:"%M" }
          ];
          break;
      }
    }

     function getGanttColumns() {

       var colHeader = '<div class="gantt_grid_head_cell gantt_grid_head_add" onclick="gantt.createTask()"></div>';

       var  colContent = function(task){
        return '<i class="fa gantt_button_grid gantt_grid_add fa-plus" onclick="gantt.createTask(null, '+task.id+')"></i>'+
        '<i class="fa gantt_button_grid gantt_grid_delete fa-times" onclick="gantt.confirm({ ' +
        'title: gantt.locale.labels.confirm_deleting_title,'+
        'text: gantt.locale.labels.confirm_deleting,'+
        'callback: function(res){ '+
        '	if(res)'+
        '		gantt.deleteTask('+task.id+');'+
        '}})"></i>';
      };


       var columns = [];

       if (schema.taskUser) {
         columns.push({name: "users", label: fields[schema.taskUser].title, align: "center", template: function (item) {
          return byId(gantt.serverList("users"), item.user_id);
         }});
       }

       var isTree = true;
       _.each(fieldNames, function(fname){
         var field = fields[fname];
         if (columns.length == 0) {
           columns.push({ name:"text", label:field.title, tree:isTree,
         template: function(item){
                  if(moment(item[fname], moment.ISO_8601, true).isValid()) {
              return moment(item[fname], moment.ISO_8601, true).format(ui.dateTimeFormat);
            }
            return item.text;
             }});
         }
         else {
           columns.push({ name:field.name, label:field.title, tree:isTree,
             template: function(item){
               if (!item.label) {
                    if(moment(item[fname], moment.ISO_8601, true).isValid()) {
                return moment(item[fname], moment.ISO_8601, true).format(ui.dateTimeFormat);
              }
              return item[fname];
               }
               return "";
             }
           });
         }
         isTree = false;
       });
       columns.push({ name:"buttons", label:colHeader, width:30, template:colContent });

       return columns;
     }

     function setChildTaskDisplay() {

       function createBox(sizes, class_name){
        var box = document.createElement('div');
        box.style.cssText = [
          "height:" + sizes.height + "px",
          "line-height:" + sizes.height + "px",
          "width:" + sizes.width + "px",
          "top:" + sizes.top + 'px',
          "left:" + sizes.left + "px",
          "position:absolute"
        ].join(";");
        box.className = class_name;

        return box;
      }

      gantt.templates.grid_row_class = gantt.templates.task_class=function(start, end, task){
        var css = [];
        if(gantt.hasChild(task.id)){
          css.push("task-parent");
        }
        if (!task.$open && gantt.hasChild(task.id)) {
          css.push("task-collapsed");
        }

        if (task.$virtual || task.type == gantt.config.types.project)
          css.push("summary-bar");

        if(task.user_id){
          css.push("gantt_resource_task gantt_resource_" + task.user_id);
        }

        return css.join(" ");
      };

     }

     function ganttInit(){
       gantt = main.dhx_gantt();
       setScaleConfig("week");
       gantt.templates.leftside_text = function(start, end, task){
          if (!task.progress){
          return "";
        }
        return "<span style='text-align:left;'>"+Math.round(task.progress*100)+ "% </span>";
      };
       gantt.config.step = 1;
       gantt.config.duration_unit = "hour";
       gantt.config.duration_step = 1;
       gantt.config.scale_height = 75;
       gantt.config.grid_width = 400;
       gantt.config.fit_tasks = true;
       gantt.config.columns = getGanttColumns();
       gantt._onTaskIdChange = null;
       gantt._onLinkIdChange = null;
       gantt.config.autosize = "x";
       gantt.config.grid_resize = true;
       gantt.config.order_branch = true;
       gantt.config.date_grid = "%d/%m/%Y %H %i";
       gantt.serverList("users", []);

       gantt.eachSuccessor = function(callback, root){
         if(!this.isTaskExists(root))
           return;

         // remember tasks we've already iterated in order to avoid infinite loops
         var traversedTasks = arguments[2] || {};
         if(traversedTasks[root])
           return;
         traversedTasks[root] = true;

         var rootTask = this.getTask(root);
         var links = rootTask.$source;
         if(links){
           for(var i=0; i < links.length; i++){
             var link = this.getLink(links[i]);
             if(this.isTaskExists(link.target)){
               callback.call(this, this.getTask(link.target));

               // iterate the whole branch, not only first-level dependencies
               this.eachSuccessor(callback, link.target, traversedTasks);
             }
           }
         }
       };

       gantt.templates.task_class=function(start, end, task){
      if(task.$virtual)
        return "summary-bar";
       };
       ganttAttachEvents();
       setChildTaskDisplay();
       fetchRecords();
     }

     function ganttAttachEvents(){

       gantt.templates.rightside_text = function(start, end, task){
        return byId(gantt.serverList("users"), task.user_id);
       };

      if (schema.taskUser) {
        gantt.attachEvent("onParse", function(){
        var styleId = "dynamicGanttStyles";
        var element = document.getElementById(styleId);
        if(!element){
          element = document.createElement("style");
          element.id = styleId;
          document.querySelector("head").appendChild(element);
        }
        var html = [".gantt_cell:nth-child(1) .gantt_tree_content{" +
            " border-radius: 16px;" +
            " width: 100%;" +
            " height: 70%;" +
            " margin: 5% 0;" +
            " line-height: 230%;}"];
        var resources = gantt.serverList("users");

        resources.forEach(function(r){
          html.push(".gantt_task_line.gantt_resource_" + r.key + "{" +
            "background-color:"+r.backgroundColor+"; " +
            "color:"+r.textColor+";" +
          "}");
          html.push(".gantt_row.gantt_resource_" + r.key + " .gantt_cell:nth-child(1) .gantt_tree_content{" +
            "background-color:"+r.backgroundColor+"; " +
            "color:"+r.textColor+";" +
            "}");
        });
        element.innerHTML = html.join("");
      });
      }

       gantt.attachEvent("onAfterTaskAdd", updateRecord);
       gantt.attachEvent("onAfterTaskUpdate", updateRecord);
       gantt.attachEvent("onAfterTaskDelete", scope.doRemove);

       gantt.attachEvent("onAfterLinkAdd", updateLink);
       gantt.attachEvent("onAfterLinkUpdate", updateLink);
       gantt.attachEvent("onAfterLinkDelete", deleteLink);

       gantt.attachEvent("onTaskCreated",function(task){
         scope.showEditor(task, true);
         return false;
       });

       gantt.attachEvent("onBeforeLightbox", function(id) {
         var task = gantt.getTask(id);
         scope.showEditor(task, false);
         return false;
       });

       var diff = 0;

       gantt.attachEvent("onBeforeTaskChanged", function(id, mode, originalTask){
         var modes = gantt.config.drag_mode;
         if(mode == modes.move ){
           var modifiedTask = gantt.getTask(id);
           diff = modifiedTask.start_date - originalTask.start_date;
         }
         return true;
       });

       //rounds positions of the child items to scale
       gantt.attachEvent("onAfterTaskDrag", function(id, mode, e){
         var modes = gantt.config.drag_mode;
         if(mode == modes.move ){
           gantt.eachSuccessor(function(child){
             child.start_date = gantt.roundDate(new Date(child.start_date.valueOf() + diff));
             child.end_date = gantt.calculateEndDate(child.start_date, child.duration);
             gantt.updateTask(child.id);
           },id );
         }
       });

     }

     function fetchRecords() {

       scope.fetchItems(function(records) {
         var data = [];
         var links = [];
        _.each(records, function(rec) {
          addData(data, rec);
          addLinks(links, rec);
        });
        gantt.parse({ "data":data, "links":links });
      });

     }

     function updateRecordItem(id,link,toRemove){

       var linkMap = {
           "0":"finishToStart",
           "1":"startToStart",
           "2":"finishToFinish",
           "3":"startToFinish"
       };

       var linkField = schema[linkMap[link.type]];
       var task = gantt.getTask(link.target);
       var record = task.record;

       if(record && linkField){
        var endRecord = gantt.getTask(link.source).record;
        if(endRecord){
          var recordList = record[linkField];
          recordList = recordList.filter(function(item, idx) {
            return item.id != endRecord.id;
          });

          if(!toRemove){
            recordList.push(endRecord);
          }
          record[linkField] = recordList;
          task.record = record;
          scope.doSave(task, updateTaskRecord);
        }
       }

     }

     function updateLink(id,link){
       updateRecordItem(id, link,  false);
     }

     function updateTaskRecord(task, rec){
       task.record = rec;
     }

     function deleteLink(id, link){
       updateRecordItem(id, link, true);
     }

     function updateRecord(id, item){

         var record = item.record;
        if(!record){ record = {}; }

      var duration = item.duration || 1;

      record[schema.taskStart] = item.start_date.toJSON();
      record[firstField.name] = item.text;

      if(schema.taskProgress){
        record[schema.taskProgress] = item.progress*100;
      }
      if(schema.taskSequence){
        record[schema.taskSequence] = item.order;
      }
      if(schema.taskDuration){
        record[schema.taskDuration] = duration;
      }
      if(schema.taskEnd){
        record[schema.taskEnd] = item.end_date.toJSON();
      }

      if(schema.taskParent && item.parent && !record[schema.taskParent]){
        var parentTask = gantt.getTask(item.parent);
        var parentRecord = parentTask.record;
        if(parentRecord){
          record[schema.taskParent] = parentRecord;
        }
      }

      return scope.doSave(item, updateTaskRecord);
    }

    function addData(data, rec){

      if(rec[schema.taskStart]){
        var dict = {
          id:rec.id,
          open:true,
          isNew:true
        };
        dict = updateData(dict, rec);
        dict.isNew = false;

        if(dict.start_date){
          data.push(dict);
        }
      }

    }

    function addLinkDict(links, targetRecordId, sourceRecords, linkType){

      _.each(sourceRecords, function(sourceRec){
        links.push({
          id:targetRecordId+"-"+sourceRec.id,
          target:targetRecordId,
          source:sourceRec.id,
          type:linkType
        });
      });

    }

    function addLinks(links,record){

      var linkMap = {
          "finishToStart":"0",
          "startToStart":"1",
          "finishToFinish":"2",
          "startToFinish":"3"
      };

      _.each(_.keys(linkMap), function(key) {
          if(schema[key]){
            addLinkDict(links, record.id, record[schema[key]], linkMap[key]);
          }
      });

    }

    function updateTask(task, rec){

      task = updateData(task, rec);

      if(!task.isNew){
        gantt.refreshTask(task.id);
      }
      task.isNew = false;

      return task;
    }

    function updateData(task, rec){

      task.record = rec;

      var name = firstField.targetName ? rec[firstField.targetName] : rec[firstField.name];
      task.text = "";
      if(name){
        task.text = name;
      }
      _.each(fields,function(field){
        var val = rec[field.name];
        if(_.isObject(val) && field.targetName){
          val = val[field.targetName];
        }
        task[field.name] = val || "";
      });

      var endDate = null;
      if(schema.taskEnd && rec[schema.taskEnd]){
        endDate = moment(rec[schema.taskEnd]);
        task.end_date  = endDate.toDate();
        if(task.isNew){
          task.end_date  = endDate.format("DD-MM-YYYY HH:mm:SS");
        }
      }

      var startDate = moment(rec[schema.taskStart]);
      if(task.isNew){
        task.start_date = startDate.format("DD-MM-YYYY HH:mm:SS");
      }
      else{
        task.start_date = startDate.toDate();
      }


      if(schema.taskDuration && rec[schema.taskDuration]){
        task.duration = rec[schema.taskDuration];
      }
      else if(endDate){
        task.duration = gantt.calculateDuration(startDate.toDate(), endDate);
      }
      else{
        task.duration = "1";
      }

      if(!endDate){
        task.end_date = gantt.calculateEndDate(startDate.toDate(), task.duration);
      }

      if(schema.taskProgress){
        task.progress = rec[schema.taskProgress]/100;
      }

      if(schema.taskParent){
        if(rec[schema.taskParent] && rec[schema.taskParent].id != task.id){
        task.parent = rec[schema.taskParent].id;
        }
        else{
          task.parent = 0;
        }
      }

      if(schema.taskSequence){
        task.sortorder = rec[schema.taskSequence];
      }

      if(schema.taskUser && rec[schema.taskUser]) {
        task.user_id = rec[schema.taskUser].id;
        if(!byId(gantt.serverList("users"), task.user_id)) {
          gantt.serverList("users").push({key:task.user_id,
            label:rec[schema.taskUser][fields[schema.taskUser].targetName],
            backgroundColor: get_random_color(),
            textColor:"#FFF"
            });
        }
      }

      return task;

    }

    function get_random_color() {
        function c() {
          var hex = Math.floor(Math.random()*256).toString(16);
          return ("0"+String(hex)).substr(-2); // pad with zero
        }
        return "#"+c()+c()+c();
     }


    scope.onMode = function(name) {
      setScaleConfig(name);
      mode = name;
      gantt.render();
    };

    scope.isMode = function(name) {
      return mode === name;
    };

    scope.onRefresh = function () {
      gantt.clearAll();
      fetchRecords();
    };

    scope.onPrint = function () {
      gantt.exportToPDF({
        name: "Gantt.pdf",
          callback: function(result){
        window.open(result.url , '_self');
      }});
    };

    scope.$on('$destroy', function() {
      gantt.clearAll();
      gantt.detachAllEvents();
    });

    scope.showEditor = function(task, isNew) {
      var record = _.extend({}, task.record);
      if (!editor) {
        editor = ViewService.compile('<div ui-editor-popup></div>')(scope.$new());
        editor.data('$target', element);
      }

      var popup = editor.isolateScope();
      popup.setEditable(true);

        if(isNew && schema.taskParent && task.parent && !record[schema.taskParent]){
          var parentTask = gantt.getTask(task.parent);
          var parentRecord = parentTask.record;
          if(parentRecord){
            record[schema.taskParent] = parentRecord;
          }
        }

      popup.show(record, function(result) {
        task.isNew = isNew;
        task = updateTask(task, result);
        if(isNew){
          gantt.addTask(task);
        }
        else {
          gantt.updateTask(task.id);
        }
      });

      if (!record || !record.id) {
        popup.waitForActions(function() {
            popup.$broadcast("on:new");
         });
      }
    };
  }

  return {
      link:function(scope, element, attrs, controller) {
        scope._viewPromise.then(function(){
          link(scope, element, attrs, controller);
        });
      },
      replace:true,
      template:
    '<div>'+
      '<div class="gantt-main"></div>'+
    '</div>'
    };
}]);

})();
