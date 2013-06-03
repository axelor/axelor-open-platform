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
			$.event.trigger('adjustSize');
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
		
		attrs.$observe('title', function(value){
			scope.title = value;
		});
	},
	transclude: true,
	template:
		'<fieldset ng-class="{\'bordered-box\': title}" x-layout-selector="&gt; div:first">'+
			'<legend ng-show="title">'+
				'<i ng-show="canCollapse()" ng-click="toggle()" ng-class="{\'icon-plus\': collapsed, \'icon-minus\': !collapsed}"></i>'+
				'<span ng-bind-html-unsafe="title"></span></legend>'+
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

		if (field.height) {
			element.height(field.height);
		}
		
		element.resizable({
			handles: 's',
			resize: _.debounce(function() {
				$.event.trigger('adjustSize');
				element.width('auto');
			}, 100)
		});
	},
	
	template:
	'<div>'+
		'<div ui-view-portlet x-action="{{actionName}}" x-can-search="{{canSearch}}"></div>'+
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
			selected = 0;
		
		$scope.select = function(tab) {
			
			var current = selected;

			angular.forEach(tabs, function(tab, i){
				tab.selected = false;
			});
			
			tab.selected = true;
			selected = _.indexOf(tabs, tab);
			
			setTimeout(function() {
				if ($scope.$tabs) {
					$scope.$tabs.trigger('adjust');
				}
				$.event.trigger('adjustSize');
				if(current != selected){
					doSelect();
				}
			});
		};
		
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
		
		function doSelect() {
			var select = tabs[selected],
			    onSelect = null;
			
			if(select){
			    onSelect = select.onSelect;
			}
			
			if(onSelect){
				onSelect.handle();
			}
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

			$.event.trigger('adjustSize');
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
			tab.selected = false;
			
			if (!wasHidden && selected > -1 && selected !== index)
				return $.event.trigger('adjustSize');
			
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
		
		$scope.$on('on:edit', function(event){
			doSelect();
		});
	}],
	
	link: function(scope, elem, attrs) {
		
		var props = scope.field;

		scope.$tabs = $(elem).bsTabs({
			closable: false
		});
		
		elem.on('click', '.dropdown-toggle', function(e){
			$.event.trigger('adjustSize');
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
				'<div class="nav-tabs-scroll-l"><a tabindex="-1" href="#"><i class="icon-chevron-left"></i></a></div>' +
				'<div class="nav-tabs-scroll-r"><a tabindex="-1" href="#"><i class="icon-chevron-right"></i></a></div>' +
				'<div class="nav-tabs-strip">' +
					'<ul class="nav nav-tabs">' +
						'<li tabindex="-1" ng-repeat="tab in tabs" ng-class="{active:tab.selected}">'+
							'<a tabindex="-1" href="" ng-click="select(tab)">'+
								'<img class="prefix-icon" ng-show="tab.icon" ng-src="{{tab.icon}}">'+
								'<span ng-bind-html-unsafe="tab.title"></span>'+
							'</a>' +
						'</li>' +
					'</ul>' +
				'</div>' +
				'<div class="nav-tabs-menu">'+
					'<div class="dropdown pull-right">'+
						'<a class="dropdown-toggle" data-toggle="dropdown" href="#"><i class="caret"></i></a>'+
							'<ul class="dropdown-menu" role="menu">'+
							    '<li ng-repeat="tab in tabs">'+
							    	'<a tabindex="-1" href="javascript: void(0)" ng-click="select(tab)" ng-bind-html-unsafe="tab.title"></a>'+
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
	
	link: function(scope, elem, attrs, tabs) {
		
		setTimeout(function(){
			scope.onSelect = scope.$events.onSelect;
		});
		
		scope.selected = false;
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
	},
	cellCss: 'form-item v-align-top',
	transclude: true,
	template: '<div ui-actions class="tab-pane" ng-class="{active: selected}" ui-transclude></div>'
});

})(this);
