/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

var ui = angular.module('axelor.ui');

var DEFAULT = '<?xml version="1.0" encoding="UTF-8"?>' +
'<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
					'xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" ' +
					'xmlns:x="http://axelor.com" ' +
					'xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" ' +
					'xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" ' +
					'targetNamespace="http://bpmn.io/schema/bpmn" ' +
					'id="Definitions_1">' +
	'<bpmn:process id="Process_1" name="" x:model="" isExecutable="false">' +
	'<bpmn:startEvent id="StartEvent_1"/>' +
	'<bpmn:endEvent id="EndEvent_1"/>' +
	'</bpmn:process>' +
	'<bpmndi:BPMNDiagram id="BPMNDiagram_1">' +
	'<bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">' +
		'<bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">' +
		'<dc:Bounds x="469" y="295" width="36" height="36"/>'+
		'</bpmndi:BPMNShape>' +
		'<bpmndi:BPMNShape id="_BPMNShape_EndEvent_2" bpmnElement="EndEvent_1">' +
		'<dc:Bounds x="734" y="295" width="36" height="36"/>'+
		'</bpmndi:BPMNShape>' +
	'</bpmndi:BPMNPlane>' +
	'</bpmndi:BPMNDiagram>' +
'</bpmn:definitions>';

var PROPS = {
	'bpmn:StartEvent': ['id', 'name'],
	'bpmn:Process': ['id', 'name', 'model'],
	'bpmn:Task': ['id', 'name', 'action'],
	'bpmn:SendTask': ['id', 'name'],
	'bpmn:ReceiveTask' : ['id', 'name'],
	'bpmn:UserTask' : ['id', 'name'],
    'bpmn:SequenceFlow': ['id', 'name', 'sequence', 'signal', 'action', 'role'],
    'bpmn:IntermediateCatchEvent' : ['id', 'name', 'datetime', 'timeduration']
};

ui.formInput('BpmnEditor', {

	css: "bpmn-editor",

	link: function(scope, element, attrs, model) {

		var canvas = element.find('.bpmn-canvas');

		// initialize bpmn modeler
		var modeler = new BpmnJS({
			container: canvas[0]
		});

		var selectedElement = null;

		function xname(name) {
			if (name === 'id' || name === 'name') {
				return name;
			}
			return 'x:' + name;
		}

		function onSelect(element) {
			selectedElement = element;
			scope.props = null;
			if (!element) {
				scope.applyLater();
				return;
			}

			var bo = element.businessObject;
			var names = PROPS[bo.$type] || PROPS["bpmn:StartEvent"];
			var props = {};
			var values = {};
			var x = {};

			props.$x = x;
			props.$type = bo.$type;

			names.forEach(function (name) {
				x[name] = true;
				var value = bo.get(xname(name));
				if (value && (name === 'action' || name == 'role')) {
					value = { name: value };
				}
				props[name] = value;
			});

			scope.props = props;
			scope.applyLater();
		}

		function updateProperty(name, value) {

			var selection = modeler.get('selection');
			var modeling = modeler.get('modeling');

			var all = selection.get() || [];
			var first = all[0] || selectedElement;

			if (!first || name === '$x' || name === '$type') {
				return;
			}
			if (name == 'name' && first.type !== "bpmn:Process") {
				modeling.updateLabel(first, value);
			}
			if (value && (name === 'action' || name == 'role')) {
				value = value.name;
			}
			var bo = first.businessObject;
			bo.set("xmlns:x", "http://axelor.com");
			bo.set(xname(name), value);

			doSave();
		}

		function doSave() {
			modeler.saveXML({ format: true }, function(err, xml) {
				scope.setValue(xml, true);
			});
		}

		function doLoad(xml) {
			modeler.importXML(xml, function(err) {
			});
		}

		var last = null;

		model.$render = function () {
			if (last === scope.record) {
				return;
			}
			last = scope.record;
			var xml = model.$viewValue;
			if (xml === null) {
				xml = DEFAULT;
			}
			scope.waitForActions(function () {
				return doLoad(xml);
			}, 100);
		}

		modeler.on('element.click', function (e) {
			if (e.element.type === "bpmn:Process") {
				scope.$timeout(function () {
					onSelect(e.element);
				});
			}
		});
		modeler.on(['shape.removed', 'connection.removed'], function (e) {
			scope.$timeout(doSave);
		});
		modeler.on(['shape.added', 'connection.added'], function (e) {
			if (e.element.type === "bpmn:Task" ||
				e.element.type === "bpmn:SubProcess" ||
				e.element.type === "bpmn:SendTask" ||
				e.element.type === "bpmn:ReceiveTask" ||
				e.element.type === "bpmn:UserTask" ||
				e.element.type === "bpmn:IntermediateCatchEvent") {
				scope.$timeout(function () {
					onSelect(e.element);
					doSave();
				});
			}
		});
		modeler.on(['selection.changed'], function (e) {
			var all = e.newSelection;
			setTimeout(function () {
				onSelect(all.length === 1 ? all[0] : null);
			});
		});

		modeler.on('element.changed', _.debounce(function (e) {
			setTimeout(function () {
				onSelect(e.element);
				doSave();
			});
		}, 100))

		var propNames = _.unique(_.flatten(_.values(PROPS)));
		propNames.forEach(function (name) {
			scope.$watch("props." + name, function (value, old) {
				if (value !== undefined && value !== old) {
					updateProperty(name, value);
				}
			});
		});

		// make container resizable
		element.resizable({
			handles: 's',
			resize: function () {
				// container resized, do something
			}
		});

		element.on('$destroy', function () {
			modeler.destroy();
		});
	},
	template_readonly: null,
	template_editable: null,
	template:
		"<div>" +
			"<div class='bpmn-canvas'></div>" +
			"<div class='bpmn-props' ng-show='props' ui-bpmn-props></div>" +
		"</div>"
});

ui.directive('uiBpmnProps', function () {

	return {
		scope: true,
		controller: ['$scope', '$element', 'ViewService', function ($scope, $element, ViewService) {

			FormViewCtrl.call(this, $scope, $element);

			$scope.setEditable();
			$scope.defaultValues = {};

			$scope.edit = function() {};
			$scope.editRecord = function() {};

			Object.defineProperty($scope, 'record', {
				enumerable: true,
				get: function () {
					return $scope.$parent.props;
				}
			});

			var items = [{
				title: _t('ID'),
				name: 'id',
				colSpan: 12,
			}, {
				title: _t('Name'),
				name: 'name',
				showIf: '$x.name',
				colSpan: 12,
			}, {
				title: _t('Model'),
				name: 'model',
				showIf: '$x.model',
				colSpan: 12
			}, {
				title: _t('Action'),
				name: 'action',
				type: 'many-to-one',
				target: 'com.axelor.meta.db.MetaAction',
				targetName: 'name',
				widget: 'BpmnManyToOne',
				showIf: '$x.action',
				colSpan: 12
			}, {
				title: _t('Role'),
				name: 'role',
				type: 'many-to-one',
				target: 'com.axelor.auth.db.Role',
				targetName: 'name',
				widget: 'BpmnManyToOne',
				showIf: '$x.role',
				colSpan: 12
			}, {
				title: _t('Date'),
				name: 'datetime',
				type: 'datetime',
				showIf: '$x.datetime',
				colSpan: 12
			}, {
				title: _t('Duration'),
				name: 'duration',
				type: 'time',
				showIf: '$x.duration',
				colSpan: 12
			}, {
				title: _t('Sequence'),
				name: 'sequence',
				type: 'integer',
				showIf: '$x.sequence',
				colSpan: 12
			}, {
				title: _t('Signal'),
				name: 'signal',
				type: 'string',
				showIf: '$x.signal',
				colSpan: 12
			}];

			var form = {
				type: 'form',
				items: [{
					type: 'panel',
					title: 'Properties',
					items: items
				}]
			};

			var meta = { fields: items };
			ViewService.process(meta, form);

			$scope.fields = items;
			$scope.schema = form;
			$scope.schema.loaded = true;
		}],
		template:
			"<div ui-view-form x-handler='this'></div>"
	};
});

ui.formInput('BpmnManyToOne', 'ManyToOne', {

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		scope.onEdit = function() {
			var ds = scope._dataSource;
			var value = scope.getValue();
			if (value && value.name) {
				ds.search({
					fields: ['name'],
					domain: 'self.name = :name',
					context: {name: value.name},
					limit: 1
				}).success(function (records, page) {
					var record = _.first(records);
					scope.showEditor(record);
				});
			}
		};
	}
});

})(this);
