/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
(function(){

var ui = angular.module("axelor.ui");

ui.ManyToOneCtrl = ManyToOneCtrl;
ui.ManyToOneCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function ManyToOneCtrl($scope, $element, DataSource, ViewService) {

	ui.RefFieldCtrl.apply(this, arguments);

	$scope.selectEnable = true;
	var ds = $scope._dataSource;

	$scope.createNestedEditor = function() {

		var embedded = $('<div ui-nested-editor></div>')
			.attr('ng-model', $element.attr('ng-model'))
			.attr('name', $element.attr('name'))
			.attr('x-title', $element.attr('x-title'))
			.attr('x-path', $element.attr('x-path'));

		embedded = ViewService.compile(embedded)($scope);
		embedded.hide();

		var colspan = $element.parents("form.dynamic-form:first").attr('x-cols') || 4,
			cell = $('<td class="form-item"></td>').attr('colspan', colspan).append(embedded),
			row = $('<tr></tr>').append(cell);

		row.insertAfter($element.parents("tr:first"));

		return embedded;
	};

	$scope.selectMode = "single";

	$scope.select = function(value) {

		if (_.isArray(value)) {
			value = _.first(value);
		}

		var field = $scope.field,
			nameField = field.targetName || 'id';

		var record = value;

		// fetch '.' names
		var path = $element.attr('x-path');
		var related = {};
		var relatives = $element.parents().find('[x-field][x-path^="'+path+'."]').map(
				function(){
					return $(this).attr('x-path').replace(path+'.','');
				}).get();
		relatives = _.unique(relatives);
		missing = _.filter(relatives, function (name) {
			return !value || value[name] === undefined;
		});
		_.each(relatives, function(name) {
			var prefix = name.split('.')[0];
			related[prefix] = record[prefix];
		});
		if (missing.length > 0 && value && value.id) {
			return ds.read(value.id, {
				fields: missing
			}).success(function(rec){
				var record = _.extend({}, related, {
					id: value.id,
					$version: value.version || value.$version
				});
				record[nameField] = rec[nameField];
				_.each(missing, function(name) {
					var prefix = name.split('.')[0];
					record[prefix] = rec[prefix];
				});
				$scope.setValue(record, true);
			});
		}
		// end fetch '.' names

		if (value && value.id) {
			record = _.extend({}, related, {
				id: value.id,
				$version: value.version || value.$version
			});
			record[nameField] = value[nameField];
			if (nameField && _.isUndefined(value[nameField])) {
				return ds.details(value.id, nameField).success(function(rec){
					$scope.setValue(_.extend({}, related, rec), true);
				});
			}
			if (value.code) {
				record.code = value.code;
			}
		}

		$scope.setValue(record, true);
	};
	
	$scope.canEditTarget = function () {
		return $scope.canEdit() && $scope.attr('canEdit') !== false;
	};
	
	$scope.canEdit = function () {
		var parent = $scope.$parent;
		if (parent.canEditTarget && !parent.canEditTarget()) {
			return false;
		}
		return $scope.attr('canEdit') !== false && $scope.canView() && $scope.getValue();
	};

	$scope.canView = function () {
		return $scope.attr('canView') !== false && $scope.getValue();
	};

	$scope.onEdit = function() {
		var record = $scope.getValue();
		$scope.showEditor(record);
	};

	var canNew = $scope.canNew;
	$scope.canNew = function () {
		// disable new icon
		if ($scope.attr('canNew') === undefined) {
			return false;
		}
		return canNew.call($scope);
	};

	$scope._isNestedOpen = false;
	$scope.onSummary = function() {
		$scope._isNestedOpen = !$scope._isNestedOpen;
		var record = $scope.getValue();
		if (record && record.id) {
			return ds.read(record.id).success(function(record){
				$scope.showNestedEditor($scope._isNestedOpen);
			});
		}
		$scope.showNestedEditor($scope._isNestedOpen);
	};
	
	var icons = null;
	var actions = {
		'new': 'canNew',
		'create': 'canNew',
		'edit': 'canEdit',
		'select': 'canSelect',
		'remove': 'canRemove',
		'clear': 'canRemove'
	};

	$scope.canShowIcon = function (which) {
		var names;
		var field = $scope.field || {};
		var prop = actions[which];
		if (prop !== undefined && $scope.attr(prop) === false) {
			return false;
		}

		if (icons === null) {
			icons = {};
			names = $scope.field.showIcons || $scope.$parent.field.showIcons;
			if (names === false || names === 'false') {
				icons.$all = false;
			} else if (names === true || names === 'true' || names === undefined) {
				icons.$all = true;
			} else if (names) {
				icons.$all = false;
				names = names.split(',');
				names.forEach(function (name) {
					icons[name.trim()] = true;
				});
			}
		}
		return icons.$all || !!icons[which];
	};
}

ui.directive('uiCanSuggest', function () {

	return function (scope, element, attrs) {
		var field = scope.field || {};
		if (field.canSuggest !== false) {
			return;
		}
		element.attr("readonly", "readonly");
		element.addClass("not-readonly");
	};
});

var m2oTemplateReadonly = "" +
		"<span class='display-text' ng-show='text'>" +
			"<a href='' ng-show='canView()' ng-click='onEdit()'>{{text}}</a>" +
			"<span ng-show='!canView()'>{{text}}</span>" +
		"</span>";

var m2oTemplateEditable = '' +
'<div class="picker-input picker-icons-3 tag-select-single">'+
'<input type="text" autocomplete="off" ui-can-suggest>'+
'<span class="tag-item label label-info" ng-if="canShowTag" ng-show="text">'+
	'<span class="tag-link tag-text" ng-click="onTagSelect($event)">{{text}}</span> '+
	'<i class="fa fa-times fa-small" ng-click="onTagRemove($event)"></i>'+
'</span>'+
'<span class="picker-icons">'+
	'<i class="fa fa-eye" ng-click="onSummary()" ng-show="hasPermission(\'read\') && _viewParams.summaryView && canToggle()"></i>'+
	'<i class="fa fa-pencil" ng-click="onEdit()" ng-show="hasPermission(\'read\') && canView() && canEdit()" title="{{\'Edit\' | t}}"></i>'+
	'<i class="fa fa-file-text-o" ng-click="onEdit()" ng-show="hasPermission(\'read\') && canView() && !canEdit()" title="{{\'View\' | t}}"></i>'+
	'<i class="fa fa-plus" ng-click="onNew()" ng-show="canNew() && hasPermission(\'write\') && !isDisabled()" title="{{\'New\' | t}}"></i>'+
	'<i class="fa fa-search" ng-click="onSelect()" ng-show="canSelect() && hasPermission(\'read\') && !isDisabled()" title="{{\'Select\' | t}}"></i>'+
'</span>'+
'</div>';

ui.formInput('ManyToOne', 'Select', {

	css	: 'many2one-item',

	widgets: ['OneToOne'],

	controller: ManyToOneCtrl,

	showSelectionOn: null,

	init: function(scope) {
		this._super(scope);

		scope.canShowTag = scope.field['tag-edit'];
		
		scope.formatItem = function(item) {
			if (item) {
				return item[(scope.field.targetName || "id")];
			}
			return "";
		};
	},

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		scope.ngModel = model;
		scope.formPath = scope.formPath ? scope.formPath + "." + scope.field.name : scope.field.name;

		var field = scope.field;

		scope.canToggle = function() {
			return (field.widgetAttrs || {}).toggle;
		};

		if (field.widget === 'NestedEditor') {

			var isHidden = scope.isHidden;
			scope.isHidden = function() {
				if (!scope.canSelect()) {
					return true;
				}
				if (scope.canToggle() === 'both' && scope._isNestedOpen) {
					return true;
				}
				return isHidden.call(scope);
			};
			
			var hiddenSet = false;
			var hasHidden = false;
			
			scope.$watch('attr("hidden")', function (hidden, old) {
				if (hidden && !hiddenSet) hasHidden = true;
				if (hiddenSet && hidden === old) return;
				hiddenSet = true;
				if (scope._isNestedOpen || hasHidden) {
					scope.showNestedEditor(!hidden);
				}
			});
			scope.$on("on:check-nested-values", function (e, value) {
				if (scope.isHidden() && value) {
					var val = scope.getValue() || {};
					if (val.$updatedValues === value && val.id === value.id) {
						_.extend(val, value);
					}
				}
			});

			scope.$timeout(function() {
				if (!scope.attr("hidden")) {
					scope.onSummary();
				}
			});
		}
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var input = this.findInput(element);
		var field = scope.field;

		function create(term, popup) {
			scope.createOnTheFly(term, popup, function (record) {
				scope.select(record);
			});
		}

		scope.loadSelection = function(request, response) {

			if (!scope.canSelect()) {
				return response([]);
			}

			this.fetchSelection(request, function(items, page) {
				var term = request.term;
				
				if (scope.canSelect() && items.length < page.total) {
					items.push({
						label: _t("Search..."),
						click: function() { scope.showSelector(); }
					});
				}
				
				if (field.create && term && scope.canNew()) {
					items.push({
						label : _t('Create "{0}" and select...', term),
						click : function() { create(term); }
					});
					items.push({
						label : _t('Create "{0}"...', term),
						click : function() { create(term, true); }
					});
				}
				
				if ((field.create === undefined || (field.create && !term)) && scope.canNew()) {
					items.push({
						label: _t("Create..."),
						click: function() { scope.showPopupEditor(); }
					});
				}

				response(items, page);
			});
		};

		scope.matchValues = function(a, b) {
			if (a === b) return true;
			if (!a) return false;
			if (!b) return false;
			return a.id === b.id;
		};

		scope.setValidity = function(key, value) {
			model.$setValidity(key, value);
		};

		if ((scope._viewParams || {}).summaryView) {
			element.removeClass('picker-icons-3').addClass('picker-icons-4');
		}

		scope.isDisabled = function() {
			return this.isReadonly();
		};

		input.keydown(function(e){
			var handled = false;
			if (e.keyCode == 113) { // F2
				if (e.shiftKey || !scope.getValue()) {
					if (scope.canNew()) {
						scope.onNew();
					}
				} else {
					if (scope.canEdit()) {
						scope.onEdit();
					}
				}
				handled = true;
			}
			if (e.keyCode == 114) { // F3
				if (scope.canSelect()) {
					scope.onSelect();
				}
				handled = true;
			}
			if (!handled) {
				return;
			}
			e.preventDefault();
			e.stopPropagation();
			return false;
		});

		scope.handleSelect = function(e, ui) {
			if (ui.item.click) {
				setTimeout(function(){
					input.val("");
				});
				ui.item.click.call(scope);
			} else {
				scope.select(ui.item.value);
			}
			setTimeout(adjustPadding, 100);
			scope.applyLater();
		};
		
		scope.$render_editable = function() {

			if (scope.canShowTag) {
				return setTimeout(adjustPadding, 100);
			}

			var value = scope.getValue();
			var name = scope.field.targetName;
			if (value && value.id > 0 && !value[name]) {
				return scope._dataSource.details(value.id, name).success(function(rec) {
					value[name] = rec[name];
					input.val(scope.getText());
				});
			}

			input.val(scope.getText());
		};
		
		if (scope.field && scope.field['tag-edit']) {
			scope.attachTagEditor(scope, element, attrs);
		}
		
		scope.onTagSelect = function (e) {
			if (scope.canEdit()) {
				return scope.onTagEdit(e, scope.getValue());
			}
			scope.onEdit();
		};

		scope.onTagRemove = function (e) {
			scope.setValue(null);
		};

		function adjustPadding() {
			var tag = element.find('span.tag-link');
			if (tag.size() && tag.is(':visible')) {
				input.css('padding-left', tag.width() + 24);
			} else {
				input.css('padding-left', '');
			}
			if (scope.canShowTag) {
				input.val('');
			}
		}
		
		if (scope.canShowTag) {
			input.focus(adjustPadding);
		}
	},
	template_editable: m2oTemplateEditable,
	template_readonly: m2oTemplateReadonly
});

ui.InlineManyToOneCtrl = InlineManyToOneCtrl;
ui.InlineManyToOneCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function InlineManyToOneCtrl($scope, $element, DataSource, ViewService) {

	var field = $scope.field || $scope.getViewDef($element);
	var params = {
		model: field.target
	};

	if (!field.editor) {
		throw "No editor defined.";
	}

	params.views = [{
		type: 'form',
		items: field.editor.items
	}];

	$scope._viewParams = params;

	ManyToOneCtrl.call(this, $scope, $element, DataSource, ViewService);

	$scope.select = function (value) {
		var editor = field.editor;
		var names = [];

		if (_.isArray(value)) {
			value = _.first(value);
		}
		if (_.isEmpty(value)) {
			return $scope.setValue(null, true);
		}

		names = _.pluck(editor.items, 'name');
		names = _.compact(names);

		if ($scope.field.targetName) {
			names.push($scope.field.targetName);
		}

		names = _.unique(names);

		function set(val) {
			var record = _.pick(val, names);
			record.id = val.id;
			record.$version = val.version || val.$version;
			$scope.setValue(record, true);
		}

		var record = _.pick(value, names);

		// if some field is missing
		if (_.keys(record).length !== names.length && value.id) {
			return $scope._dataSource.read(value.id, {
				fields: names
			}).success(function (rec) {
				set(rec);
			});
		}
		set(value);
	};

	$scope.onClear = function() {
		$scope.setValue(null, true);
	};
}

ui.formInput('InlineManyToOne', 'ManyToOne', {

	widgets: ['InlineOneToOne'],

	controller: InlineManyToOneCtrl,

	template_readonly: function (scope) {
		var field = scope.field || {};
		if (field.viewer) {
			return field.viewer;
		}
		if (field.editor && (field.editor.viewer || !field.targetName)) {
			return null;
		}
		return m2oTemplateReadonly;
	},

	template_editable: function (scope) {
		var editor = scope.field.editor || {};
		var template = "" +
		"<div class='m2o-editor'>" +
			"<div class='m2o-editor-controls' ng-show='!isReadonly()'>" +
				"<a href='' ng-show='canEdit() && canShowIcon(\"edit\")' ng-click='onEdit()'><i class='fa fa-pencil'></i></a>" +
				"<a href='' ng-show='canView() && !canEdit() && canShowIcon(\"view\")' ng-click='onEdit()'><i class='fa fa-file-text-o'></i></a>" +
				"<a href='' ng-show='canSelect() && canShowIcon(\"select\")' ng-click='onSelect()'><i class='fa fa-search'></i></a>" +
				"<a href='' ng-show='canShowIcon(\"clear\")' ng-click='onClear()'><i class='fa fa-times-circle'></i></a>" +
			"</div>" +
			"<div class='m2o-editor-form' ui-panel-editor></div>" +
		"</div>";
		if (editor.showOnNew !== false) {
			return template;
		}
		scope.canShowEditor = function () {
			return (scope.record || {}).id > 0 || !!scope.getValue();
		}
		template =
			"<div class='m2o-editor-switcher'>" +
			"<div ng-show='!canShowEditor()' class='form-item-container'>"+ m2oTemplateEditable +"</div>" +
			"<div ng-show='canShowEditor()'>"+ template +"</div>" +
			"</div>";
		return template;
	},

	template: null
});

ui.formInput('SuggestBox', 'ManyToOne', {

	showSelectionOn: "click",

	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off" ui-can-suggest>'+
		'<span class="picker-icons">'+
			'<i class="fa fa-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
   '</span>'
});

ui.formInput('RefSelect', {

	css: 'multi-object-select',

	controller: ['$scope', 'ViewService', function($scope, ViewService) {

		$scope.createSelector = function(select, ref, watch) {
			var value = select.value;
			var elem = $('<input ui-ref-item ng-show="canShow(\'' + value + '\')"/>')
				.attr('ng-model', '$_' + ref)
				.attr('x-target', value)
				.attr('x-watch-name', watch)
				.attr('x-ref', ref);

			return ViewService.compile(elem)($scope);
		};

		$scope.createElement = function(id, name, selectionList, related) {

			var elemGroup = $('<div ui-group ui-table-layout cols="2" x-widths="150,*"></div>');
			var elemSelect = $('<input ui-select showTitle="false">')
				.attr("name", name)
				.attr("x-for-widget", id)
				.attr("ng-model", "record." + name);

			var elemSelects = $('<div ui-group></div>');
			var elemItems = _.map(selectionList, function(s) {
				return $('<input ui-ref-item ng-show="canShow(this)" style="display: none;"/>')
					.attr('ng-model', 'record.$_' + related)
					.attr('x-target', s.value)
					.attr('x-watch-name', name)
					.attr('x-ref', related);
			});

			elemGroup.append(elemSelect).append(elemSelects.append(elemItems));

			return ViewService.compile(elemGroup)($scope);
		};
	}],

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var name = scope.field.name,
			selectionList = scope.field.selectionList,
			related = scope.field.related || scope.field.name + "Id";

		scope.canShow = function(itemScope) {
			return itemScope.targetValue === scope.getValue();
		};

		scope.isReadonly = function() {
			return false;
		};

		scope.refFireEvent = function (name) {
			var handler = scope.$events[name];
			if (handler) {
				return handler();
			}
		};
		
		var elem = scope.createElement(element.attr('id'), name, selectionList, related);
		setTimeout(function() {
			element.append(elem);
		});
	},
	template_editable: null,
	template_readonly: null
});

ui.formInput('RefItem', 'ManyToOne', {

	showTitle: false,
	
	controller: ['$scope', '$element', 'DataSource', 'ViewService', function ($scope, $element, DataSource, ViewService) {

		var target = $element.attr('x-target');
		var data = (_.findWhere($scope.$parent.field.selectionList, { value: target})||{}).data || {};
		
		var getViewDef = $scope.getViewDef;
		$scope.getViewDef = function (elem) {
			if (elem === $element) {
				return _.extend({}, {
					domain: data.domain,
					formView: data.form,
					gridView: data.grid
				});
			}
			return getViewDef.call($scope, elem);
		}

		ManyToOneCtrl.apply(this, arguments);
	}],

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		if (scope.field.targetName) {
			return this._link.apply(this, arguments);
		}

		var self = this;
		var target = element.attr('x-target');
		var data = (_.findWhere(scope.$parent.field.selectionList, {value: target})||{}).data || {};

		scope.loadFields().success(function(fields) {
			var name = false,
				search = [];

			_.each(fields, function(f) {
				if (f.nameColumn) name = f.name;
				if (f.name === "name") search.push("name");
				if (f.name === "code") search.push("code");
			});

			if (!name && _.contains(search, "name")) {
				name = "name";
			}

			_.extend(scope.field, {
				target: scope._model,
				targetName: name,
				targetSearch: search,
				domain: data.domain
			});

			self._link(scope, element, attrs, model);
		});
	},

	_link: function(scope, element, attrs, model) {

		var ref = element.attr('x-ref');
		var watch = element.attr('x-watch-name');
		var target = element.attr('x-target');
		
		function doRender() {
			if (scope.$render_editable) scope.$render_editable();
			if (scope.$render_readonly) scope.$render_readonly();
		}

		function getRef() {
			return (scope.record||{})[ref];
		}

		function setRef(value) {
			if (!scope.record) {
				return;
			}
			
			var old = scope.record[ref];
			scope.record[ref] = value;
			
			if (old != value) {
				scope.refFireEvent('onChange');
			}
		}
		
		scope.targetValue = target;

		var __setValue = scope.setValue;
		scope.setValue = function(value) {
			__setValue.call(scope, value);
			setRef(value ? value.id : 0);
		};

		var selected = false;

		scope.$watch("record." + ref, function(value, old) {
			scope.$timeout(function() {
				var v = scope.getValue();
				if ((v && v.id === value) || !selected) return;
				scope.select(value ? {id: value } : null);
			});
		});
		
		var lastId = null;

		scope.$watch("record." + watch, function(value, old) {
			selected = value === scope._model;
			if (value === old || old === undefined) return;
			if (lastId == scope.record.id) {
				scope.setValue(null);
			}
			lastId = scope.record.id;
		});

		model.$render = function() {
			if (selected) doRender();
		};
	}
});

}).call(this);
