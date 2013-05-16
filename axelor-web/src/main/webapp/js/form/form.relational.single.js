(function(){

var ui = angular.module("axelor.ui");

ui.ManyToOneCtrl = ManyToOneCtrl;
ui.ManyToOneCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function ManyToOneCtrl($scope, $element, DataSource, ViewService) {

	ui.RefFieldCtrl.apply(this, arguments);

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
	
	$scope.select = function(value) {
		
		if (_.isArray(value)) {
			value = _.first(value);
		}
		
		var field = $scope.field,
			nameField = field.targetName || 'id';

		var record = value;

		// fetch '.' names
		var path = $element.attr('x-path');
		var relatives = $element.parents().find('[x-field][x-path^="'+path+'."]').map(
				function(){
					return $(this).attr('x-path').replace(path+'.','');
				}).get();
		
		relatives = _.unique(relatives);
		if (relatives.length > 0 && value && value.id) {
			return ds.read(value.id, {
				fields: relatives
			}).success(function(rec){
				var record = { 'id' : value.id };
				record[nameField] = rec[nameField];
				_.each(relatives, function(name) {
					var prefix = name.split('.')[0];
					record[prefix] = rec[prefix];
				});
				$scope.setValue(record, true);
			});
		}
		// end fetch '.' names

		if (value && value.id) {
			record = { 'id' : value.id };
			record[nameField] = value[nameField];
			if (nameField && _.isUndefined(value[nameField])) {
				return ds.details(value.id).success(function(rec){
					$scope.setValue(rec, true);
				});
			}
		}
		
		$scope.setValue(record, true);
	};
	
	$scope.onEdit = function() {
		var record = $scope.getValue();
		$scope.showEditor(record);
	};
	
	$scope.onSummary = function() {
		var record = $scope.getValue();
		if (record && record.id) {
			return ds.read(record.id).success(function(record){
				$scope.showNestedEditor(record);
			});
		}
		$scope.showNestedEditor(record);
	};
}

ui.formInput('ManyToOne', 'Select', {

	css	: 'many2one-item',
	
	controller: ManyToOneCtrl,

	init: function(scope) {
		this._super(scope);

		scope.formatItem = function(item) {
			if (item) {
				return item[(scope.field.targetName || "id")];
			}
			return "";
		};
	},

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		if (scope.field.widget === 'NestedEditor') {
			setTimeout(function(){
				scope.showNestedEditor();
			});
		}
	},

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var input = this.findInput(element);
		
		scope.ngModel = model;
		scope.formPath = scope.formPath ? scope.formPath + "." + scope.field.name : scope.field.name;

		scope.loadSelection = function(request, response) {
			this.fetchSelection(request, response);
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
				if (e.shiftKey) {
					scope.onNew();
				} else {
					scope.onEdit();
				}
				handled = true;
			}
			if (e.keyCode == 114) { // F3
				scope.onSelect();
				handled = true;
			}
			if (!handled) {
				return;
			}
			e.preventDefault();
			e.stopPropagation();
			return false;
		});
	},
	template_editable:
	'<div class="picker-input picker-icons-3">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-eye-open" ng-click="onSummary()" ng-show="hasPermission(\'read\') && _viewParams.summaryView"></i>'+
			'<i class="icon-pencil" ng-click="onEdit()" ng-show="hasPermission(\'read\')" title="{{\'Edit\' | t}}"></i>'+
			'<i class="icon-plus" ng-click="onNew()" ng-show="hasPermission(\'write\') && !isDisabled()" title="{{\'New\' | t}}"></i>'+
			'<i class="icon-search" ng-click="onSelect()" ng-show="hasPermission(\'read\') && !isDisabled()" title="{{\'Select\' | t}}"></i>'+
		'</span>'+
	'</div>',
	template_readonly:
	'<a href="" ng-click="onEdit()">{{text}}</a>'
});

ui.formInput('SuggestBox', 'ManyToOne', {

	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
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
				.attr('x-watch', watch)
				.attr('x-ref', ref);

			return ViewService.compile(elem)($scope);
		};

		$scope.createElement = function(name, selection, related) {

			var elemGroup = $('<group ui-group ui-table-layout cols="2" x-widths="200,*"></group>');
			var elemSelect = $('<input ui-select noLabel="true">')
				.attr("name", name)
				.attr("ng-model", "record." + name);

			var elemSelects = $('<group ui-group>');
			var elemItems = _.map(selection, function(s) {
				return $('<input ui-ref-item ng-show="canShow(\'' + s.value + '\')"/>')
					.attr('ng-model', 'record.$_' + related)
					.attr('x-target', s.value)
					.attr('x-watch', name)
					.attr('x-ref', related);
			});

			elemGroup.append(elemSelect).append(elemSelects.append(elemItems));

			return ViewService.compile(elemGroup)($scope);
		};
	}],

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var name = scope.field.name,
			selection = scope.field.selection,
			related = scope.field.related || scope.field.name + "Id";
		
		scope.canShow = function(value) {
			return value === scope.getValue();
		};

		var elem = scope.createElement(name, selection, related);

		setTimeout(function() {
			element.append(elem);
		});
	},
	template_editable: null,
	template_readonly: null
});

ui.formInput('RefItem', 'ManyToOne', {

	showTitle: false,

	link: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);

		if (scope.field.targetName) {
			return this._link.apply(this, arguments);
		}

		var self = this;
		scope.loadView('grid').success(function(fields, view) {
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
				targetSearch: search
			});

			self._link(scope, element, attrs, model);
		});
	},

	_link: function(scope, element, attrs, model) {
		
		var ref = element.attr('x-ref');
		var watch = element.attr('x-watch');
		
		function doRender() {
			if (scope.$render_editable) scope.$render_editable();
			if (scope.$render_readonly) scope.$render_readonly();
		}
		
		function getRef() {
			return scope.record[ref];
		}
		
		function setRef(value) {
			return scope.record[ref] = value;
		}
		
		var __setValue = scope.setValue;
		scope.setValue = function(value) {
			__setValue.call(scope, value);
			setRef(value ? value.id : 0);
		};

		var selected = false;
		
		scope.$watch("record." + ref, function(value, old) {
			setTimeout(function() {
				var v = scope.getValue();
				if ((v && v.id === value) || !selected) return;
				scope.select(value ? {id: value } : null);
			});
		});
		
		scope.$watch("record." + watch, function(value, old) {
			selected = value === scope._model;
			if (value === old) return;
			scope.setValue(null);
		});

		model.$render = function() {
			if (selected) doRender();
		};
	},
});

}).call(this);