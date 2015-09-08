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

// this directive is used as a replacement for ng-transclude directive
// which fails to keep scope hierarchy (see: https://github.com/angular/angular.js/issues/1809)
ui.directive('uiTransclude', function() {
	return {
		compile: function(tElement, tAttrs, transclude) {
			return function(scope, element, attrs, ctrl) {
				transclude(scope.$new(), function(clone) {
					element.append(clone);
				});
			};
		}
	};
});

/**
 * The Group widget.
 *
 */
ui.formWidget('Group', {
	
	css: 'form-item-group',
	cellCss: 'form-item v-align-top',
		
	link: function(scope, element, attrs) {

		var props = scope.field;

		scope.collapsed = false;
		
		scope.canCollapse = function() {
			return props.canCollapse || props.collapseIf;
		};
		
		scope.setCollapsed = function(collapsed) {
			scope.collapsed = collapsed;
			element.children('legend').nextAll(':not(br)')[collapsed ? 'hide' : 'show']();
			axelor.$adjustSize();
		};

		scope.toggle = function() {
			scope.collapsed = !scope.collapsed;
			scope.setCollapsed(scope.collapsed);
		};
		
		scope.$watch("attr('collapse')", function(collapsed) {
			scope.setCollapsed(collapsed);
		});
		
		// if auto title, then don't show it
		if (attrs.title === attrs.field) {
			attrs.$set('title', '');
		}

		if (props.showTitle !== false) {
			attrs.$observe('title', function(value){
				scope.title = value;
			});
		}
	},
	transclude: true,
	template:
		'<fieldset ng-class="{\'bordered-box\': title, \'has-title\': title}" x-layout-selector="&gt; div:first">'+
			'<legend ng-show="title">'+
				'<i ng-show="canCollapse()" ng-click="toggle()" ng-class="{\'fa fa-plus\': collapsed, \'fa fa-minus\': !collapsed}"></i>'+
				'<span ng-bind-html="title"></span></legend>'+
			'<div ui-transclude></div>'+
		'</fieldset>'
});

ui.formWidget('Portlet', {

	css: 'form-item-portlet',
	cellCss: 'form-item v-align-top',
	
	showTitle: false,

	link: function(scope, element, attrs) {
		
		var field = scope.field;
		
		scope.canSearch = field.canSearch !== "false";
		scope.actionName = field.action;
		
		if (field.name) {
			scope.formPath = field.name;
		}

		if (field.height) {
			element.height(field.height);
		}
		
		element.resizable({
			handles: 's',
			resize: _.debounce(function() {
				axelor.$adjustSize();
				element.width('auto');
			}, 100)
		});
	},
	
	template:
	'<div>'+
		'<div ui-view-portlet x-action="{{actionName}}" x-can-search="{{canSearch}}"></div>'+
	'</div>'
});

ui.formWidget('Dashlet', {

	css: 'dashboard',

	showTitle: false,

	link: function(scope, element, attrs) {

		var field = scope.field;
		var dashlet = _.extend({}, scope.field);

		scope.dashlet = dashlet;

		if (field.name) {
			scope.formPath = field.name;
		}

		if (field.height) {
			element.height(field.height);
		}
	},

	template:
	'<div>'+
		'<div ui-view-dashlet></div>'+
	'</div>'
});

/**
 * The Tabs widget (notebook).
 */
ui.formWidget('Tabs', {
	
	cellCss: 'form-item v-align-top',
	
	widgets: ['Notebook'],

	controller: ['$scope', '$element', function($scope, $element) {
		
		var tabs = $scope.tabs = [],
			selected = -1;

		var doOnSelectPending = false;
		var doOnSelect = _.debounce(function () {
			var select = tabs[selected];
			if (doOnSelectPending || !select) {
				return;
			}
			doOnSelectPending = true;
			$scope.waitForActions(function () {
				if (select.handleSelect) {
					select.handleSelect();
				}
				$scope.waitForActions(function () {
					doOnSelectPending = false;
				});
			});
		}, 100);
		
		$scope.select = function(tab) {
			
			var current = selected;

			angular.forEach(tabs, function(tab, i){
				tab.tabSelected = false;
			});
			
			tab.tabSelected = true;
			selected = _.indexOf(tabs, tab);
			
			if (current === selected) {
				return;
			}
			
			setTimeout(function() {
				if ($scope.$tabs) {
					$scope.$tabs.trigger('adjust');
				}
				axelor.$adjustSize();
				if(current != selected){
					doOnSelect();
				}
			});
		};

		$scope.$on('on:edit', function (e, record) {
			if ($scope.record === record) {
				doOnSelect();
			}
		});
		
		this.addTab = function(tab) {
			if (tabs.length === 0) $scope.select(tab);
			tab.index = tabs.length;
			tabs.push(tab);
		};
		
		function inRange(index) {
			return index > -1 && index < tabs.length;
		}
		
		function findItem(index) {
			return $element.find('ul.nav-tabs:first > li:nth-child(' + (index+1) + ')');
		}
		
		this.showTab = function(index) {
			
			if (!inRange(index)) {
				return;
			}

			var tab = tabs[index];
			var item = findItem(index);

			tab.hidden = false;
			item.show();

			if (selected == -1 || selected === index) {
				return $scope.select(tabs[index]);
			}

			axelor.$adjustSize();
		};
		
		this.hideTab = function(index) {
			
			if (!inRange(index))
				return;
			
			var item = findItem(index),
				tab = tabs[index];
			
			var wasHidden = item.is(":hidden");

			item.hide();
			item.removeClass('active');
			
			tab.hidden = true;
			tab.tabSelected = false;
			
			if (!wasHidden && selected > -1 && selected !== index)
				return axelor.$adjustSize();
			
			for(var i = 0 ; i < tabs.length ; i++) {
				var tab = tabs[i];
				if (!tab.hidden) {
					return $scope.select(tabs[i]);
				}
			}
			selected = -1;
		};
		
		$scope.setTitle = function(value,index){
			var item = findItem(index),
				pageScope = item.first().data('$scope');

			pageScope.tab.title = value;
		};
	}],
	
	link: function(scope, elem, attrs) {
		
		var props = scope.field;

		scope.$tabs = $(elem).bsTabs({
			closable: false
		});
		
		elem.on('click', '.dropdown-toggle', function(e){
			axelor.$adjustSize();
		});
		
		// set height (#1011)
		if (props.height) {
			elem.children('.tab-content:first').height(props.height);
		}
	},
	transclude: true,
	template:
		'<div class="tabbable-tabs">' +
			'<div class="nav-tabs-wrap">' +
				'<div class="nav-tabs-scroll-l"><a tabindex="-1" href="#"><i class="fa fa-chevron-left"></i></a></div>' +
				'<div class="nav-tabs-scroll-r"><a tabindex="-1" href="#"><i class="fa fa-chevron-right"></i></a></div>' +
				'<div class="nav-tabs-strip">' +
					'<ul class="nav nav-tabs">' +
						'<li tabindex="-1" ng-repeat="tab in tabs" ng-class="{active:tab.tabSelected}">'+
							'<a tabindex="-1" href="" ng-click="select(tab)">'+
								'<img class="prefix-icon" ng-show="tab.icon" ng-src="{{tab.icon}}">'+
								'<span ng-bind-html="tab.title"></span>'+
							'</a>' +
						'</li>' +
					'</ul>' +
				'</div>' +
				'<div class="nav-tabs-menu">'+
					'<div class="dropdown pull-right">'+
						'<a class="dropdown-toggle" data-toggle="dropdown" href="#"><i class="caret"></i></a>'+
							'<ul class="dropdown-menu" role="menu">'+
							    '<li ng-repeat="tab in tabs">'+
							    	'<a tabindex="-1" href="javascript: void(0)" ng-click="select(tab)" ng-bind-html="tab.title"></a>'+
							    '</li>' +
							'</ul>' +
						'</a>'+
					'</div>'+
				'</div>'+
			'</div>' +
			'<div class="tab-content" ui-transclude></div>' +
		'</div>'
});

/**
 * The Tab widget (notebook page).
 */ 
ui.formWidget('Tab', {
	
	require: '^uiTabs',
	
	widgets: ['Page'],
	
	handles: ['isHidden'],

	link: function(scope, elem, attrs, tabs) {
		
		scope.tabSelected = false;
		scope.icon = scope.field && scope.field.icon;

		tabs.addTab(scope);
		
		attrs.$observe('title', function(value){
			scope.title = value;
		});
		
		scope.$watch("isHidden()", function(hidden, old) {
			if (hidden) {
				return tabs.hideTab(scope.index);
			}
			return tabs.showTab(scope.index);
		});
		
		scope.handleSelect = function () {
			var onSelect = scope.$events.onSelect;
			if (onSelect && !elem.is(":hidden")) {
				onSelect();
			}
		};
	},
	cellCss: 'form-item v-align-top',
	transclude: true,
	template: '<div ui-actions class="tab-pane" ng-class="{active: tabSelected}" x-layout-selector="&gt; div:first">'+
		'<div ui-transclude></div>'+
	'</div>'
});

ui.formWidget('ButtonGroup', {

	link: function (scope, element, attrs) {
		function adjust() {
			var visible = element.children('a.btn:visible').size();
			if (visible) {
				element.children('a.btn:visible')
					.css('max-width', (100.00/visible) + '%')
					.css('width', (100.00/visible) + '%');
			}
		}
		scope.$watch(adjust);
		scope.$callWhen(function () {
			return element.is(':visible')
		}, adjust);
	},
	transclude: true,
	template_editable: null,
	template_readonly: null,
	template:
		"<div class='btn-group' ui-transclude></div>"
});

ui.formWidget('Panel', {

	showTitle: false,

	link: function (scope, element, attrs) {

		var field = scope.field || {};
		var body = element.children(".panel-body");

		element.addClass(field.serverType);
		if (field.sidebar && !attrs.itemSpan) {
			attrs.$set('itemSpan', 12, true, 'x-item-span');
		}

		scope.menus = null;
		if (field.menu) {
			scope.menus = [field.menu];
		}

		scope.canCollapse = function() {
			return field.canCollapse || field.collapseIf;
		};

		scope.setCollapsed = function(collapsed) {
			var old = scope.collapsed;
			var action = collapsed ? "hide" : "show";

			scope.collapsed = collapsed;
			scope.collapsedIcon = collapsed ? 'fa-chevron-down' : 'fa-chevron-up';

			if (collapsed === old) {
				return;
			}

			element.removeClass("collapsed");
			body[action]("blind", 200, function () {
				element.toggleClass("collapsed", !!collapsed);
				if (body.css('display') !== 'none' && action === 'hide') {
					body.hide();
				}
				axelor.$adjustSize();
			});
		};

		scope.toggle = function() {
			scope.setCollapsed(!scope.collapsed);
		};

		scope.$watch("attr('collapse')", function(collapsed) {
			scope.setCollapsed(collapsed);
		});

		var nested = element.parents('.panel:first').size() > 0;
		if (nested) {
			element.addClass("panel-nested");
		}
		if (field.showFrame === false) {
			element.addClass('noframe');
		}
		scope.notitle = field.showFrame === false || field.showTitle === false;
		scope.title = field.title;
		scope.$watch('attr("title")', function (title, old) {
			if (title === undefined || title === old) return;
			scope.title = title;
		});
	},

	transclude: true,
	template:
		"<div class='panel panel-default'>" +
			"<div class='panel-header' ng-show='field.title' ng-if='!notitle'>" +
				"<div ng-show='canCollapse()' class='panel-icons pull-right'>" +
					"<a href='' ng-click='toggle()'><i class='fa' ng-class='collapsedIcon'></i></a>" +
				"</div>" +
				"<div ng-if='menus' ui-menu-bar menus='menus' handler='this' class='pull-right'></div>" +
				"<div class='panel-title'>{{title}}</div>" +
			"</div>" +
			"<div class='panel-body' ui-transclude></div>" +
		"</div>"
});

ui.formWidget('PanelStack', {
	transclude: true,
	template: "<div class='panel-stack'>" +
				"<span ui-transclude></span>" +
			"</div>"
});

ui.formWidget('PanelTabs', {

	link: function (scope, element, attrs) {

		scope.tabs = [];
		scope.more = null;

		element.find('> .tab-content > div').each(function (index) {
			var elem = $(this);
			var tab = {
				title: elem.attr('x-title'),
				selected: false,
				hidden: false,
				elem: elem,
				tabItem: $(),
				menuItem: $()
			}
			scope.tabs.push(tab);
		});
		
		var selected = null;

		function findTab(tab) {
			var found = scope.tabs[tab] || tab;
			if (!found || _.isNumber(found)) {
				return null;
			}
			return found;
		}
		
		var doOnSelectPending = false;
		var doOnSelect = _.debounce(function () {
			if (doOnSelectPending || !selected || !selected.elem) {
				return;
			}
			doOnSelectPending = true;
			scope.waitForActions(function () {
				var elemScope = selected.elem.scope();
				if (elemScope.handleSelect) {
					elemScope.handleSelect();
				}
				doOnSelectPending = false;
			});
		}, 100);

		scope.selectTab = function(tab) {
			var current = selected;
			var found = findTab(tab);
			if (!found) {
				return;
			}
			scope.tabs.forEach(function (current) {
				current.selected = false;
				current.elem.hide();
			});

			selected = found;

			found.selected = true;
			found.elem.show();

			found.elem
				.add(found.tabItem)
				.add(found.menuItem)
				.addClass('active');

			setMenuTitle();

			setTimeout(function () {
				elemTabs.removeClass('open');
				elemMenu.removeClass('open');
				axelor.$adjustSize();
				if (current != selected) {
					doOnSelect();
				}
			});
		};

		scope.showTab = function (tab) {
			var found = findTab(tab);
			if (!found) {
				return;
			}

			found.hidden = false;
			found.tabItem.show();

			if (!selected || selected === found) {
				return scope.selectTab(found);
			}

			found.elem.hide();

			axelor.$adjustSize();
		};

		scope.hideTab = function (tab) {
			var found = findTab(tab);
			if (!found) {
				return;
			}

			var wasHidden = found.hidden;

			found.hidden = true;
			found.selected = false;
			found.elem.hide();

			found.tabItem.add(found.menuItem).hide().removeClass('active');

			if (!wasHidden && selected && selected !== found) {
				return axelor.$adjustSize();
			}

			var tabs = scope.tabs;
			for(var i = 0 ; i < tabs.length ; i++) {
				var tab = tabs[i];
				if (!tab.hidden) {
					return scope.selectTab(tabs[i]);
				}
			}
			selected = null;
		};

		scope.onMenuClick = _.once(function(e) {
			var elem = $(e.currentTarget);
			elem.dropdown();
			setTimeout(function () {
				elem.dropdown('toggle');
			});
		});

		var menuWidth = 120; // max-width
		var elemTabs = $();
		var elemMenu = $();
		var elemMenuTitle = $();
		var elemMenuItems = $();

		function setup() {
			elemTabs = element.children('.nav-tabs').children('li:not(.dropdown)');
			elemMenu = element.children('.nav-tabs').children('li.dropdown');
			elemMenuTitle = elemMenu.children('a:first').children('span');
			elemMenuItems = elemMenu.find('li');

			_.each(scope.tabs, function (tab, index) {
				tab.tabItem = $(elemTabs[index]);
				tab.menuItem = $(elemMenuItems[index]);
			});
		}

		function setMenuTitle() {
			var more = null;
			var show = false;
			elemMenuItems.each(function (i) {
				var elem = $(this);
				var tab = scope.tabs[i] || {};
				if (elem.data('visible')) show = true;
				if (tab.selected && show) {
					more = tab;
				}
			});
			scope.more = more;
			if (show) {
				elemMenu.show();
				elemMenuTitle.html((more||{}).title);
			}
		}

		var adjusting = false;

		function adjust() {

			if (elemTabs === null || adjusting) {
				return;
			}

			var parentWidth = element.width() - 32;
			if (parentWidth <= 0) {
				return;
			}

			adjusting = true;

			elemTabs.hide().css('visibility', 'hidden');
			elemMenu.hide();
			elemMenuTitle.empty();
			elemMenuItems.hide().data('visible', null);

			var width = 0;
			var last = null;

			for (var count = 0; count < scope.tabs.length; count++) {
				var tab = scope.tabs[count];
				tab.$visible = false;
			}

			for (var count = 0; count < scope.tabs.length; count++) {
				var tab = scope.tabs[count];
				var elem = tab.tabItem;

				if (tab.hidden) {
					continue;
				}

				width += elem.show().width() + 3;
				if (width > parentWidth && last) {
					// requires menu...
					elem.hide();
					if (width + menuWidth - elem.width() > parentWidth) {
						last.tabItem.hide();
						last.$visible = false;
					}
					break;
				}
				tab.$visible = true;
				elem.css('visibility', '');
				last = tab;
			}

			var menuVisible = false;
			for (var count = 0; count < scope.tabs.length; count++) {
				var tab = scope.tabs[count];
				if (tab.hidden || tab.$visible) continue;
				tab.menuItem.show().data('visible', true);
				menuVisible = true;
			}
			if (!menuVisible) {
				elemMenu.hide();
			}
			setMenuTitle();
			adjusting = false;
		}

		element.on('adjustSize', _.debounce(adjust, 10));

		scope.$timeout(function() {
			setup();
			scope.selectTab(_.first(scope.tabs));
		});

		scope.$on('on:edit', function (e, record) {
			if (scope.record === record && !doOnSelectPending) {
				scope.ajaxStop(doOnSelect, 100);
			}
		});
	},

	transclude: true,
	template:
		"<div class='panel-tabs tabbable-tabs'>" +
			"<ul class='nav nav-tabs nav-tabs-responsive'>" +
				"<li ng-repeat='tab in tabs' ng-class='{active: tab.selected}'>" +
					"<a tabindex='-1' href='' ng-click='selectTab(tab)' ng-bind-html='tab.title'></a>" +
				"</li>" +
				"<li class='dropdown' ng-class='{active: more.selected}' style='display: none'>" +
					"<a tabindex='-1' href='' title='{{more.title}}' class='dropdown-toggle' ng-click='onMenuClick($event)'>" +
						"<span></span><b class='caret'></b>" +
					"</a>" +
					"<ul class='dropdown-menu pull-right' data-toggle='dropdown'>" +
						"<li ng-repeat='tab in tabs' ng-class='{active: tab.selected}'>" +
							"<a tabindex='-1' href='' ng-click='selectTab(tab)' ng-bind-html='tab.title'></a>" +
						"</li>" +
					"</ul>" +
				"</li>" +
			"</ul>" +
			"<div class='tab-content' ui-transclude></div>" +
		"</div>"
});

ui.formWidget('PanelTab', {

	link: function (scope, element, attrs) {

		var index = element.parent().children().index(element);
		var tab = null;
		var isHidden = scope.isHidden;

		function findTab() {
			return tab || (tab = (scope.tabs||[])[index]) || {};
		}

		scope.handleSelect = function () {
			var onTabSelect = scope.$events.onTabSelect;
			if (onTabSelect && !element.is(":hidden")) {
				onTabSelect();
			}
		};

		scope.isHidden = function () {
			var tab = findTab();
			return !tab.selected || isHidden.call(scope);
		};

		attrs.$observe('title', function(value) {
			var tab = findTab();
			tab.title = value;
		});

		scope.$watch("attr('hidden')", function(hidden, old) {
			scope.$evalAsync(function () {
				if (hidden) {
					return scope.hideTab(index);
				}
				return scope.showTab(index);
			});
		});
	}
});

})(this);
