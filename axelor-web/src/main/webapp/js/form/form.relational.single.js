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
(function() {

"use strict";

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

		var cell, row;

		// if panel form
		if ($element.parent().is("div.form-item")) {
			cell = $("<div></div>").addClass("span12 form-item").append(embedded);
			row = $("<div></div>").addClass("row-fluid").append(cell);
			row.insertAfter($element.parents(".row-fluid:first"));
			return embedded;
		}

		var colspan = $element.parents("form.dynamic-form:first").attr('x-cols') || 4;

		cell = $('<td class="form-item"></td>').attr('colspan', colspan).append(embedded);
		row = $('<tr></tr>').append(cell);
		row.insertAfter($element.parents("tr:first"));

		return embedded;
	};

	$scope.selectMode = "single";

	$scope.findRelativeFields = (function () {
		var relatives = null;
		return function () {
			if (relatives) {
				return relatives;
			}
			var path = $element.attr('x-path');
			relatives = $element.parents().find('[x-field][x-path^="'+path+'."]:not(label)').map(function() {
				return $(this).attr('x-path').replace(path+'.','');
			}).get();
			relatives.push($scope.field.targetName);
			relatives = _.unique(_.compact(relatives));
			return relatives;
		};
	})();

	$scope.fetchMissingValues = function (value, fields) {
		var nameField = $scope.field.targetName || 'id';
		var related = {};
		var relatives = fields || $scope.findRelativeFields();
		var missing = _.filter(relatives, function (name) {
			return !value || value[name] === undefined;
		});
		_.each(relatives, function(name) {
			var prefix = name.split('.')[0];
			related[prefix] = value[prefix];
		});
		if (missing.length > 0 && value && value.id) {
			return ds.read(value.id, {
				fields: missing
			}).success(function(rec){
				var record = _.extend({}, related, {
					id: value.id,
					$version: value.version || value.$version
				});
				if (nameField in rec) {
					record[nameField] = rec[nameField];
				}
				_.each(missing, function(name) {
					var prefix = name.split('.')[0];
					record[prefix] = rec[prefix];
				});
				$scope.setValue(record, false);
			});
		}
	};

	$scope.select = function(value) {

		if (_.isArray(value)) {
			value = _.first(value);
		}

		var nameField = $scope.field.targetName || 'id';
		var record = value;

		if (value && value.id) {
			record = _.extend({}, {
				id: value.id,
				$version: value.version || value.$version
			});
			record[nameField] = value[nameField];
			if (nameField && _.isUndefined(value[nameField])) {
				return ds.details(value.id, nameField).success(function(rec){
					$scope.setValue(_.extend({}, rec), true);
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
		element.prop("readonly", true);
		element.addClass("not-readonly");
		
		// jquery-ui doesn't allow keyboard navigation on autocomplete list
		element.on('keydown', function (e) {
			var inst = element.data('autocomplete');
			console.log(e.keyCode);
			switch (e.keyCode) {
			case 38: // up
				inst._keyEvent('previous', e);
				console.log('up');
				break;
			case 40: // down
				inst._keyEvent('next', e);
				console.log('down');
				break;
			case  9: // tab
				if (inst.menu.active) {
					inst.menu.select(event);
				}
				break;
			case 13: // enter
				if (inst.menu.active) {
					inst.menu.select(event);
					e.preventDefault();
				}
				break;
			case 27: // escape
				if (inst.menu.element.is(':visible')) {
					inst.close(e);
					e.preventDefault();
				}
				break;
			}
		});
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

		scope.$evalAsync(function() {
			var relatives = scope.findRelativeFields();
			if (relatives.length > 0) {
				scope.$watch(attrs.ngModel, function m2oValueWatch(value, old) {
					if (value && value.id > 0) {
						scope.fetchMissingValues(scope.getValue(), relatives);
					}
				}, true);
			}
		});

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
			
			scope.$watch('attr("hidden")', function m2oHiddenWatch(hidden, old) {
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

		input.on("change", function () {
			input.attr('placeholder', input.is(':focus') ? _t('Search...') : null);
		});
		input.on("focus", function () {
			// XXX: firefox prevents click event, bug in FF?
			if (!axelor.browser.mozilla) {
				input.attr('placeholder', _t('Search...'));
			}
		});
		input.on("blur", function () {
			input.attr('placeholder', field.placeholder || '');
		});

		scope.loadSelection = function(request, response) {

			if (!scope.canSelect()) {
				return response([]);
			}

			this.fetchSelection(request, function(items, page) {
				var term = request.term;
				
				if (scope.canSelect() && (items.length < page.total || (request.term && items.length === 0))) {
					items.push({
						label: _t("Search more..."),
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
		
		scope.handleEnter = function (e) {
			var widget = input.autocomplete('widget');
			if (widget) {
				var item = widget.find('li .ui-state-focus').parent();
				if (item.length === 0) {
					item = widget.find('li:not(.tag-select-action)');
					item = item.length === 1 ? item.first() : null;
				}
				var data = item ? item.data('ui-autocomplete-item') : null;
				if (data) {
					input.autocomplete('close');
					scope.select(data.value);
				}
			}
		};

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
			scope.$applyAsync();
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
			if (tag.length && tag.is(':visible')) {
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
			return field.viewer.template;
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
				"<a href='' ng-show='canEdit() && canShowIcon(\"edit\")' ng-click='onEdit()' title='{{\"Edit\" | t}}'><i class='fa fa-pencil'></i></a>" +
				"<a href='' ng-show='!canEdit() && canView() && canShowIcon(\"view\")' ng-click='onEdit()' title='{{\"View\" | t}}'><i class='fa fa-file-text-o'></i></a>" +
				"<a href='' ng-show='canSelect() && canShowIcon(\"select\")' ng-click='onSelect()' title='{{\"Select\" | t}}'><i class='fa fa-search'></i></a>" +
				"<a href='' ng-show='canShowIcon(\"clear\")' ng-click='onClear()' title='{{\"Clear\" | t}}'><i class='fa fa-times-circle'></i></a>" +
			"</div>" +
			"<div class='m2o-editor-form' ui-panel-editor></div>" +
		"</div>";
		if (editor.showOnNew !== false) {
			return template;
		}
		scope.canShowEditor = function () {
			return (scope.record || {}).id > 0 || !!scope.getValue();
		};
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
	metaWidget: true,
	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var field = scope.field;
		if (field.canNew === undefined && field.create) {
			field.canNew = true;
		}
		if (field.create === undefined && field.canNew) {
			field.create = true;
		}
	},
	template_editable:
	'<span class="picker-input picker-icons-2">'+
		'<input type="text" autocomplete="off" ui-can-suggest>'+
		'<span class="picker-icons picker-icons-2">'+
			'<i class="fa fa-pencil" ng-click="onEdit()" ng-show="hasPermission(\'read\') && canView() && canEdit()" title="{{\'Edit\' | t}}"></i>'+
			'<i class="fa fa-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
   '</span>'
});

ui.formInput('RefSelect', {

	css: 'multi-object-select',

	metaWidget: true,

	controller: ['$scope', 'ViewService', function($scope, ViewService) {

		$scope.createElement = function(id, name, selectionList, related) {

			var elemGroup = $('<div ui-group ui-table-layout cols="2" x-widths="150,*"></div>');
			var elemSelect = $('<input ui-select showTitle="false">')
				.attr("name", name)
				.attr("x-for-widget", id)
				.attr("ng-model", "record." + name);

			var elemSelects = $('<div></div>').attr('ng-switch', "record." + name);
			var elemItems = _.map(selectionList, function(s) {
				return $('<input ui-ref-item ng-switch-when="' + s.value +'">')
					.attr('ng-model', 'record.$_' + related)
					.attr('x-target', s.value)
					.attr('x-watch-name', name)
					.attr('x-ref', related);
			});

			elemGroup
				.append($('<div class="multi-object-select-first" ng-show="!isLink || (isLink && !isReadonly())"></div>').append(elemSelect))
				.append(elemSelects.append(elemItems));

			return ViewService.compile(elemGroup)($scope);
		};
	}],

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var name = scope.field.name,
			selectionList = scope.field.selectionList,
			related = scope.field.related || scope.field.name + "Id";

		scope.isLink = this.isLink;
		scope.fieldsCache = {};
		scope.selectionList = selectionList;

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

		setTimeout(function () {
			var selectScope = element.find('[name="'+name+'"]').scope();
			var setValue = selectScope.setValue;
			selectScope.setValue = function (value) {
				var old = scope.getValue();
				if (old !== value && scope.record) {
					scope.record[related] = null;
				}
				setValue.apply(selectScope, arguments);
			};
		});
	},
	template_editable: null,
	template_readonly: null
});

ui.formInput('RefLink', 'RefSelect', {
	css: 'multi-object-select multi-object-link',
	isLink: true
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
		var data = (_.findWhere(scope.$parent.selectionList, {value: target})||{}).data || {};
		
		function doLink(fields) {
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
			scope.setDomain(data.domain, data.context);

			if (scope.$parent.isLink) {
				scope.onEdit = function () {
					var value = scope.getValue() || {};
					scope.openTab({
						action: _.uniqueId('$act'),
						model: scope._model,
						viewType: "form",
						views: [{ type: "form" }]
					}, {
						mode: "edit",
						state: value.id
					});
				};
			}
		}

		if (scope.fieldsCache[scope._model]) {
			doLink(scope.fieldsCache[scope._model]);
		} else {
			scope.loadFields().success(function (fields) {
				scope.fieldsCache[scope._model] = fields;
				doLink(fields);
			});
		}
	},

	_link: function(scope, element, attrs, model) {

		var ref = element.attr('x-ref');
		var watch = element.attr('x-watch-name');
		var target = element.attr('x-target');
		var refField = scope.fields[ref] || {};

		function setRef(value) {
			if (!scope.record) {
				return;
			}
			
			if (value && refField.type === 'string') {
				value = '' + value;
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
		
		var doSelect = _.debounce(function () {
			scope.$timeout(function() {
				var value = (scope.record || {})[ref];
				var v = scope.getValue();
				if (v && v.id === value) return;
				scope.select(value ? { id: value } : null);
			});
		}, 100);

		var watchExpr = "record.id + record." + watch + " + record." + ref;
		scope.$watch(watchExpr, doSelect);
		scope.$watch("record", doSelect);
	}
});

ui.formInput('RefText', 'ManyToOne', {
	metaWidget: true,
	link_editable: function (scope, element, attrs) {
		this._super.apply(this, arguments);
		
		if (!scope.field.create) {
			return;
		}

		function freeSelect(text) {
			return function () { 
				scope.$timeout(function () {
					scope.select(text);
				});
			};
		}

		scope.loadSelection = function(request, response) {
			this.fetchSelection(request, function(items, page) {
				var term = request.term;
				if (term) {
					items.push({
						label : _t('Select "{0}"...', term),
						click : freeSelect(term)
					});
				}
				response(items);
			});
		};
	},
	link: function (scope, element, attrs, model) {
		scope.canNew = function () { return false; };
		scope.canView = function () { return false; };
		scope.canEdit = function () { return false; };
		
		var field = scope.field;
		var targetName = field.targetName || "name";
		
		scope._viewParams.views = [{
			type: "grid",
			items: [{ name: targetName, type: "field" }]
		}];
		
		scope.formatItem = function (record) {
			return record || "";
		};

		scope.select = function (value) {
			var record = _.isArray(value) ? _.first(value) : value;
			var val = _.isString(record) ? record : (record || {})[targetName];
			if (val === undefined) {
				val = null;
			}
			scope.setValue(val, true);
		};
	}
});

ui.formInput('EvalRefSelect', 'Select', {

	controller: ['$scope', '$element', 'DataSource', 'ViewService', function ($scope, $element, DataSource, ViewService) {

		var fetchDS;

		$scope.$fetchDS = function () {
			var target = this.$target;
			if (!fetchDS || fetchDS._model !== target) {
				fetchDS = DataSource.create(target);
			}
			return fetchDS;
		};

		$scope.canNew = function () {
			return false;
		};

		$scope.showSelector = function () {
			var child = $scope.$new();
			child._viewParams = {
				model: $scope.$target,
				views: [{
					type: 'grid',
					items: [{
						type: 'field',
						name: $scope.$targetName
					}]
				}]
			};

			var selector = ViewService.compile('<div ui-selector-popup x-select-mode="single"></div>')(child);
			var popup = selector.isolateScope();

			selector.on('dialogclose', function () {
				selector.remove();
				child.$destroy();
			});

			popup.show();
		};
	}],

	init: function(scope) {
		this._super.apply(this, arguments);
		
		var field = scope.field;
		
		function toValue(value) {
			var val = value;
			if (val === undefined || val === null) {
				val = null;
			} else {
				val = '"' + val + '"';
			}
			return val;
		}
		
		Object.defineProperties(scope, {

			$target: {
				get: function () {
					return scope.$eval(field.evalTarget);
				}
			},

			$targetName: {
				get: function () {
					return scope.$eval(field.evalTargetName);
				}
			},

			$recordValue: {
				get: function () {
					return scope.$eval(field.evalValue);
				},
				set: function (value) {
					scope.$eval(field.evalValue + " = " + toValue(value));
				}
			},

			$recordTitle: {
				get: function () {
					return scope.$eval(field.evalTitle);
				},
				set: function (value) {
					scope.$eval(field.evalTitle + " = " + toValue(value));
				}
			}
		});
	
		scope.formatItem = function (value) {
			return scope.$recordTitle;
		};

		scope.select = function (value) {
			var item = _.isArray(value) ? _.first(value) : value;
			scope.setValue(item);
		};

		scope.setValue = function (value) {
			var nameField = scope.$targetName;
			var val = value || {};
			scope.$recordValue = val.id;
			scope.$recordTitle = val[nameField];
		};
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		scope.handleSelect = function(e, ui) {
			if (ui.item.click) {
				ui.item.click.call(scope);
			} else {
				scope.select(ui.item.value);
			}
			scope.$applyAsync();
		};

		scope.loadSelection = function(request, response) {
			var targetName = scope.$targetName;
			if (!targetName) {
				return response([]);
			}

			var ds = scope.$fetchDS();
			var filter = {};

			if (request.term) {
				filter[targetName] = request.term;
			}

			ds.search({
				fields: ['id', targetName],
				filter: filter,
				limit: axelor.device.small ? 6 : 10
			}).success(function (records, page) {
				var items = _.map(records, function(record) {
					return {
						label: record[targetName],
						value: record
					};
				});

				if (items.length < page.total || (request.term && items.length === 0)) {
					items.push({
						label: _t("Search more..."),
						click: function() {
							scope.showSelector();
						}
					});
				}

				response(items, page);
			});
		};
	}
});

})();
