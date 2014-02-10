/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
(function() {

var module = angular.module('axelor.ui');

MenuBarCtrl.$inject = ['$scope', '$element'];
function MenuBarCtrl($scope, $element) {

	this.isDivider = function(item) {
		return !item.title && !item.icon;
	};
}

module.directive('uiMenuBar', function() {

	return {
		replace: true,
		controller: MenuBarCtrl,
		scope: {
			menus: '=',
			handler: '='
		},
		link: function(scope, element, attrs, ctrl) {

			ctrl.handler = scope.handler;
			
			scope.onMenuClick = _.once(function onMenuClick() {
				element.find('.dropdown-toggle').dropdown('toggle');
			});

			scope.canShowTitle = function(menu) {
				return menu.showTitle === null || menu.showTitle === undefined || menu.showTitle;
			};
		},

		template:
			"<ul class='nav menu-bar'>" +
				"<li class='menu dropdown' ng-repeat='menu in menus'>" +
					"<a href='' class='dropdown-toggle' data-toggle='dropdown' ng-click='onMenuClick()'>" +
						"<img ng-show='menu.icon != null' ng-src='{{menu.icon}}'> " +
						"<span ng-show='canShowTitle(menu)'>{{menu.title}}</span> " +
						"<b class='caret'></b>" +
					"</a>" +
					"<ul ui-menu='menu'></ul>" +
				"</li>" +
			"</ul>"
	};
});

module.directive('uiMenu', function() {

	return {
		replace: true,
		require: '^uiMenuBar',
		scope: {
			menu: '=uiMenu'
		},
		link: function(scope, element, attrs, ctrl) {

		},
		template:
			"<ul class='dropdown-menu'>" +
				"<li ng-repeat='item in menu.items' ui-menu-item='item'>" +
			"</ul>"
	};
});

module.directive('uiMenuItem', ['ActionService', function(ActionService) {

	return {
		replace: true,
		require: '^uiMenuBar',
		scope: {
			item: '=uiMenuItem'
		},
		link: function(scope, element, attrs, ctrl) {

			var item = scope.item;
			var handler = null;

			scope.field  = item;
			scope.isDivider = ctrl.isDivider(item);

			if (item.action) {
				handler = ActionService.handler(ctrl.handler, element, {
					action: item.action
				});
			}

			scope.isRequired = function(){};
			scope.isValid = function(){};

			var attrs = {
				hidden: false,
				readonly: false
			};

			scope.attr = function(name, value) {
				attrs[name] = value;
			};

			scope.isReadonly = function(){
				return attrs.readonly;
			};

			scope.isHidden = function(){
				return attrs.hidden;
			};

			var form = element.parents('.form-view:first');
			var formScope = form.data('$scope');

			if (formScope) {
				formScope.$watch('record', function(rec) {
					scope.record = rec;
				});
			}

			scope.onClick = function(e) {
				$(e.srcElement).parents('.dropdown').dropdown('toggle');
				if (item.action) {
					return handler.onClick();
				}
			};

			scope.cssClass = function() {
				if (scope.isDivider) {
					return 'divider';
				}
			};
		},
		template:
			"<li ng-class='cssClass()' ui-widget-states	ng-show='!isHidden()'>" +
				"<a href='' ng-show='isReadonly()' class='disabled'>{{item.title}}</a>" +
				"<a href='' ng-show='!isDivider && !isReadonly()' ng-click='onClick($event)'>{{item.title}}</a>" +
			"</li>"
	};
}]);

}).call(this);