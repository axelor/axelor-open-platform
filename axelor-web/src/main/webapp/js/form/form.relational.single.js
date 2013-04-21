(function(){

var ui = angular.module("axelor.ui");

ui.ManyToOneCtrl = ManyToOneCtrl;
ui.ManyToOneCtrl.$inject = ['$scope', '$element', 'DataSource', 'ViewService'];

function ManyToOneCtrl($scope, $element, DataSource, ViewService) {

	ui.RefFieldCtrl.call(this, $scope, $element, DataSource, ViewService);

	var ds = $scope._dataSource,
		field = $scope.getViewDef($element),
		nameField = field.targetName || field.nameField || 'id';

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

ui.formInput('ManyToOne', {

	css	: 'many2one-item',
	
	controller: ManyToOneCtrl,

	init: function(scope) {

		var targetName = scope.field && scope.field.targetName;

		scope.format = function(value) {
			if (value && targetName) {
				return value[targetName];
			}
			return value;
		};
	},

	link_editable: function(scope, element, attrs, model) {

		scope.ngModel = model;
		
		var field = scope.field,
			input = element.children('input:first'),
			nameField = field.targetName || field.nameField || 'id';
		
		scope.formPath = scope.formPath ? scope.formPath + "." + field.name : field.name;
		
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
			if (e.keyCode == 46) { // DELETE
				scope.select(null);
				handled = true;
			}
			if (!handled) {
				return;
			}
			setTimeout(function(){
				scope.$apply();
			});
			e.preventDefault();
			e.stopPropagation();
			return false;
		});

		scope.$render_editable = function() {
			var value = scope.getValue();
			if (value) {
				value = scope.format(value);
			}
			input.val(value);
		};

		var embedded = null;
		var readonly = false;
		if (field.widget == 'NestedEditor') {
			setTimeout(function(){
				embedded = scope.showNestedEditor();
				embeddedScope = embedded.data('$scope');
				if (embeddedScope) {
					embeddedScope.attr('readonly', readonly);
				}
			});
		}

		scope.setValidity = function(key, value) {
			model.$setValidity(key, value);
		};
		
		function search(request, response) {
			var fields = field.targetSearch || [],
				filter = {}, ds = scope._dataSource;

			fields = ["id", nameField].concat(fields);
			fields = _.chain(fields).compact().unique().value();

			_.each(fields, function(name){
				if (name !== "id" && request.term) {
					filter[name] = request.term;
				}
			});
			
			var domain = scope._domain,
				context = scope._context;

			if (domain && scope.getContext) {
				context = _.extend({}, context, scope.getContext());
			}

			var params = {
				filter: filter,
				fields: fields,
				archived: true,
				limit: 10
			};
			
			if (domain) {
				params.domain = domain;
				params.context = context;
			}

			ds.search(params).success(function(records, page){
				if(_.isEmpty(records)){
		            onSelectFired = false;
			    }
				response(records);
			});
		}
		
		var onSelectFired = false;
		input.autocomplete({
			source: function(request, response) {
				var onSelect = scope.$evetns.onSelect;
				if (onSelect && !onSelectFired) {
					onSelect().then(function(){
						search(request, response);
					});
					onSelectFired = true;
				}
				else search(request, response);
			},
			close: function(event, ui) {
				onSelectFired = false;
			},
			focus: function(event, ui) {
				return false;
			},
			select: function(event, ui) {
				scope.select(ui.item);
				onSelectFired = false;
				return false;
			}
		}).data("autocomplete")._renderItem = function( ul, item ) {
			var label = item[nameField] || item.name || item.code || item.id;
			return $("<li><a>" + label  + "</a></li>")
				.data("item.autocomplete", item)
				.appendTo(ul);
		};
		
		var canSelect = _.isUndefined(field.canSelect) ? true : field.canSelect;
		setTimeout(function(){
			if (!canSelect) scope.attr('hidden', true);
		});
		
		if ((scope._viewParams || {}).summaryView) {
			element.removeClass('picker-icons-3').addClass('picker-icons-4');
		}

		scope.isDisabled = function() {
			return scope.isReadonly(element);
		};
	},
	
	link_readonly: function(scope, element, attrs, model) {
		
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
   '<span>{{text}}</span>'
});

ui.formInput('SuggestBox', 'ManyToOne', {

	link_editable: function(scope, element, attrs, model) {
		this._super.apply(this, arguments);
		var input = element.children(':input:first');
		input.autocomplete("option" , {
			minLength: 0
		});
		scope.showSelection = function() {
			if (scope.isReadonly(element)) {
				return;
			}
			input.autocomplete("search" , '');
		};
	},
	template_editable:
	'<span class="picker-input">'+
		'<input type="text" autocomplete="off">'+
		'<span class="picker-icons">'+
			'<i class="icon-caret-down" ng-click="showSelection()"></i>'+
		'</span>'+
   '</span>'
});

}).call(this);