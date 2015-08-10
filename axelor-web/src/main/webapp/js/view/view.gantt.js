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

// localization

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
		link_start: _t(" (start)"),
		link_end: _t(" (end)"),

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

	DSViewCtrl('gantt', $scope, $element);
	var ds = $scope._dataSource;
	var view = $scope._views['gantt'];
	var initialized = false;

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
		searchFields.push(schema.taskDuration);

		var optionalFields = [schema.taskParent,
		                      schema.taskSequence,
		                      schema.taskProgress,
		                      schema.finishToStart,
		                      schema.startToStart,
		                      schema.finishToFinish,
		                      schema.startToFinish
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
			store: false
		};

		ds.search(opts).success(function(records) {
			callback(records);
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
   	}

	$scope.doRemove = function(id, task){
   		var record = _.clone(task.record);
		if(task.childtask || task.parent){
			return childDs.remove(record).success(function(res){
				return $scope.refresh();
			});
		}
		return ds.remove(record).success(function(res){
			return $scope.refresh();
		});
   	}

	$scope.refresh = function() {
	};

}

angular.module('axelor.ui').directive('uiViewGantt', ['ViewService', 'ActionService', function(ViewService, ActionService) {

	function link(scope, element, attrs, controller) {
		var main = element[0];
		var schema = scope.schema;
		var fields = scope.fields;
		var fieldNames = _.pluck(schema.items, "name");
		var firstField = fields[fieldNames[0]];
		var editor = null;
		ganttInit();

		function setScaleConfig(value){
			
			switch (value) {
				case "1":
					gantt.config.scale_unit = "day";
					gantt.config.step = 1;
					gantt.config.date_scale = "%d %M";
					gantt.config.subscales = [{unit:"hour", step:3, date:"%H:%i"}];
					gantt.config.scale_height = 27;
					gantt.templates.date_scale = null;
					break;
				case "2":
					var weekScaleTemplate = function(date){
						var dateToStr = gantt.date.date_to_str("%d %M");
						var endDate = gantt.date.add(gantt.date.add(date, 1, "week"), -1, "day");
						return dateToStr(date) + " - " + dateToStr(endDate);
					};
					gantt.config.scale_unit = "week";
					gantt.config.step = 1;
					gantt.templates.date_scale = weekScaleTemplate;
					gantt.config.min_column_width = 50;
					gantt.config.subscales = [
						{unit:"day", step:1, date:"%D" }
					];
					gantt.config.scale_height = 50;
					break;
				case "3":
					gantt.config.scale_unit = "month";
					gantt.config.date_scale = "%F, %Y";
					gantt.config.subscales = [
						{unit:"day", step:1, date:"%j, %D" }
					];
					gantt.config.scale_height = 50;
					gantt.templates.date_scale = null;
					gantt.config.min_column_width = 50;
					break;
				case "4":
					gantt.config.scale_unit = "year";
					gantt.config.step = 1;
					gantt.config.date_scale = "%Y";
					gantt.config.min_column_width = 50;

					gantt.config.scale_height = 90;
					gantt.templates.date_scale = null;

					gantt.config.subscales = [
						{unit:"month", step:1, date:"%M" }
					];
					break;
			}
		}

	   function getGanttColumns() {
		   
		   var colHeader = '<div class="gantt_grid_head_cell gantt_grid_head_add" onclick="gantt.createTask()"></div>'

		   var  colContent = function(task){
				return '<i class="fa gantt_button_grid gantt_grid_add fa-plus" onclick="gantt.createTask(null, '+task.id+')"></i> \
				<i class="fa gantt_button_grid gantt_grid_delete fa-times" onclick="dhtmlx.confirm({ \
				title: gantt.locale.labels.confirm_deleting_title, \
				text: gantt.locale.labels.confirm_deleting,\
				callback: function(res){ \
					if(res) \
						gantt.deleteTask('+task.id+');\
				}})"></i>';
			};
			
		   var columns = [];
		   var isTree = true;
		   _.each(fieldNames, function(fname){
			   var field = fields[fname];
			   columns.push({ name:field["name"], label:field["title"], tree:isTree })
			   isTree = false;
		   });
		   columns.push({ name:"buttons", label:colHeader, width:75, template:colContent });

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

				return css.join(" ");
			};

			gantt.addTaskLayer(function show_hidden(task) {
				
				if (!task.$open && gantt.hasChild(task.id)) {
					
					var sub_height = gantt.config.row_height - 5,
						el = document.createElement('div'),
						sizes = gantt.getTaskPosition(task);

					var sub_tasks = gantt.getChildren(task.id);
					var child_el;
					for (var i = 0; i < sub_tasks.length; i++){
						var child = gantt.getTask(sub_tasks[i]);
						var child_sizes = gantt.getTaskPosition(child);

						child_el = createBox({
							height: sub_height,
							top:sizes.top,
							left:child_sizes.left,
							width: child_sizes.width
						}, "child_preview gantt_task_line");
						child_el.innerHTML =  child.text;
						el.appendChild(child_el);
					}
					return el;
				}
				
				return false;
			});
	   }

	   function ganttInit(){
		   setScaleConfig("1");
		   gantt.config.grid_width = 400;
		   gantt.config.duration_unit = "hour";
		   gantt.config.fit_tasks = true;
		   gantt.config.columns = getGanttColumns();
		   gantt._onTaskIdChange = null;
		   gantt._onLinkIdChange = null;
		   ganttAttachEvents();
		   setChildTaskDisplay();
		   gantt.init(main);
		   fetchRecords();	
	   }

	   function ganttAttachEvents(){

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
				  scope.doSave(task, updateTask);
			  }
		   }

	   }

	   function updateLink (id,link){
		   
		   updateRecordItem(id, link,  false)
		   
	   }

	   function deleteLink(id, link){
		   
		   updateRecordItem(id, link, true)
		   
	   }

	   function updateRecord(id, item){

		    var record = item.record;
		    if(!record){ record = {}; }
		    
			var duration = item.duration;
			if(duration == 0){ duration = 1;}
			
			record[schema.taskStart] = item.start_date.toJSON();
			record[firstField.name] = item.text;
			record[schema.taskDuration] = duration;
			
			if(schema.taskProgress){ 
				record[schema.taskProgress] = item.progress; 
			}
			if(schema.taskSequence){ 
				record[schema.taskSequence] = item.order; 
			}

			if(schema.taskParent && item.parent && !record[schema.taskParent]){
				var parentTask = gantt.getTask(item.parent);
				var parentRecord = parentTask.record;
				if(parentRecord){
					record[schema.taskParent] = parentRecord;
				}
			}
			
			item.record = record;

			return scope.doSave(item, updateTask);
		}

		function addData(data, rec){
			
			if(!rec[schema.taskStart]){
				return false;
			};

			var startDate = moment(rec[schema.taskStart]).format("DD-MM-YYYY HH:mm:SS");
			var name = firstField.targetName ? rec[firstField.targetName] : rec[firstField.name];
			var dict = {
					id:rec.id,
					start_date:startDate,
					duration:rec[schema.taskDuration] ? rec[schema.taskDuration] : 0,
					text:name || "",
					open:true,
					record:rec
			}
			
			if(schema.taskSequence){
				dict["sortorder"] = rec[schema.taskSequence];
			}
			
			if(schema.taskProgress){
				dict["progress"] = rec[schema.taskProgress];
			}

			if(schema.taskParent && rec[schema.taskParent]){
				dict["parent"] = rec[schema.taskParent].id;
			}
			_.each(fields,function(field){
				var val = rec[field.name];
				if(_.isObject(val) && field.targetName){
					val = val[field.targetName];
				}
				dict[field.name] = val || "";
			});

			data.push(dict);
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

		function updateTask(task, res){
			
			task.record = res;
			
			_.each(fields, function(field){
				var val = res[field.name];
				if(_.isObject(val) && field.targetName){
					val = val[field.targetName];
				}
				task[field.name] = val || "";
			});
			
			if(res[firstField.name] != null){
				task["text"] = res[firstField.name];
			}
			task["duration"] = res[schema.taskDuration];
			if(res[schema.taskStart]){
				task["start_date"] = moment(res[schema.taskStart]).toDate();
			}
			if(schema.taskSequence){
				task["sortorder"] = res[schema.taskSequence];
			}

			if(schema.taskProgress){
				task["progress"] = res[schema.taskProgress];
			}

			if(schema.taskParent && res[schema.taskParent]){
				task["parent"] = res[schema.taskParent].id;
			}
			if(!task.isNew){
				gantt.refreshTask(task.id);
			}
			task.isNew = false;
			
			return task;
		}

		scope.onMode = function(name) {
			mode = name;
			var scale = '4';
			if (name === "week") {
				scale = '2';
			}
			if (name === "day") {
				scale = '1';
			}
			setScaleConfig(scale);
			gantt.render();
		};

		scope.onRefresh = function () {
			gantt.clearAll();
			fetchRecords();
		};

		scope.$on('$destroy', function() {
			gantt.clearAll();
			gantt.detachAllEvents();
		});

		scope.showEditor = function(task, isNew) {
			
			var record = _.extend({}, task.record);
			if (editor == null) {
				editor = ViewService.compile('<div ui-editor-popup></div>')(scope.$new());
				editor.data('$target', element);
			}

			var popup = editor.isolateScope();
			popup.setEditable(true);
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

			if (record == null || !record.id) {
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
		'<div class="webkit-scrollbar-all">'+
			'<div class="gantt-main"></div>'+
		'</div>'
	  };
}]);
