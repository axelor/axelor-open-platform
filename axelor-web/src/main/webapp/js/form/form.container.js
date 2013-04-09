(function(){

var ui = angular.module('axelor.ui');

/**
 * The Group widget.
 *
 */
var Group = {

	scope: true,
	
	link: function(scope, element, attrs) {

		var props = scope.getViewDef(element);

		scope.canCollapse = props.canCollapse;
		scope.collapsed = false;
		
		scope.setCollapsed = function(collapsed) {
			scope.collapsed = collapsed;
			element.children('legend').nextAll(':not(br)')[collapsed ? 'hide' : 'show']();
			$.event.trigger('adjustSize');
		};

		scope.toggle = function() {
			scope.collapsed = !scope.collapsed;
			scope.setCollapsed(scope.collapsed);
		};
		
		// if auto title, then don't show it
		if (attrs.title === attrs.field) {
			attrs.$set('title', '');
		}
		
		attrs.$observe('title', function(value){
			scope.title = value;
		});
	},
	css: 'form-item-group',
	cellCss: 'form-item v-align-top',
	transclude: true,
	template:
		'<fieldset ng-class="{\'bordered-box\': title}" ng-transclude x-layout-after="&gt; legend:first">'+
			'<legend ng-show="title">'+
				'<i ng-show="canCollapse" ng-click="toggle()" ng-class="{\'icon-plus\': collapsed, \'icon-minus\': !collapsed}"></i>'+
				'<span ng-bind-html-unsafe="title"></span></legend>'+
		'</fieldset>'
};

/**
 * The Tabs widget (notebook).
 */
var Tabs = {
	cellCss: 'form-item v-align-top',
	transclude: true,
	scope: true,
	controller: ['$scope', '$element', function($scope, $element) {
		
		var tabs = $scope.tabs = [],
			selected = 0;
		
		$scope.select = function(tab) {

			angular.forEach(tabs, function(tab, i){
				tab.selected = false;
			});
			
			tab.selected = true;
			selected = _.indexOf(tabs, tab);
			
			var select = tabs[selected],
			    onSelect = null;
			if(select){
			    onSelect = select.onSelect;
			}
			setTimeout(function() {
				if ($scope.$tabs) {
					$scope.$tabs.trigger('adjust');
				}
				$.event.trigger('adjustSize');
				if(onSelect){
					onSelect.handle();
				}
			});
		};
		
		this.addTab = function(tab) {
			if (tabs.length === 0) $scope.select(tab);
			tabs.push(tab);
		};
		
		function inRange(index) {
			return index > -1 && index < tabs.length;
		}
		
		function findItem(index) {
			return $element.find('ul.nav-tabs:first > li:nth-child(' + (index+1) + ')');
		}
		
		$scope.showTab = function(index) {
			
			if (!inRange(index))
				return;
			
			var item = findItem(index);
			
			item.show();
			
			if (selected == -1 || selected === index)
				return $scope.select(tabs[index]);
			
			$.event.trigger('adjustSize');
		};
		
		$scope.hideTab = function(index) {
			
			if (!inRange(index))
				return;
			
			var item = findItem(index),
				tab = tabs[index];
			
			if (item.is(':hidden'))
				return;
			
			item.hide();
			item.removeClass('active');
			
			tab.selected = false;
			
			if (selected > -1 && selected !== index)
				return $.event.trigger('adjustSize');
			
			for(var i = 0 ; i < tabs.length ; i++) {
				item = findItem(i);
				if (item.is(':visible'))
					return $scope.select(tabs[i]);
			}
			selected = -1;
		};
	}],
	
	link: function(scope, elem, attrs) {
		var props = scope.getViewDef(elem);
		
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
	
	template:
		'<div class="tabbable-tabs">' +
			'<div class="nav-tabs-wrap">' +
				'<div class="nav-tabs-scroll-l"><a tabindex="-1" href="#"><i class="icon-chevron-left"></i></a></div>' +
				'<div class="nav-tabs-scroll-r"><a tabindex="-1" href="#"><i class="icon-chevron-right"></i></a></div>' +
				'<div class="nav-tabs-strip">' +
					'<ul class="nav nav-tabs">' +
						'<li tabindex="-1" ng-repeat="tab in tabs" ng-class="{active:tab.selected}">'+
							'<a tabindex="-1" href="" ng-click="select(tab)" ng-bind-html-unsafe="tab.title"></a>' +
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
			'<div class="tab-content" ng-transclude></div>' +
		'</div>'
};

/**
 * The Tab widget (notebook page).
 */ 
var Tab = {
	require: '^uiTabs',
	scope: true,
	link: function(scope, elem, attrs, tabs) {
		
		setTimeout(function(){
			scope.onSelect = elem.data('$onSelect');
		});
		
		tabs.addTab(scope);
		attrs.$observe('title', function(value){
			scope.title = value;
		});
	},
	cellCss: 'form-item v-align-top',
	transclude: true,
	template: '<div ui-actions class="tab-pane" ng-class="{active: selected}" ng-transclude></div>'
};


// register directives
ui.formDirective('uiGroup', Group);
ui.formDirective('uiTabs', Tabs);
ui.formDirective('uiTab', Tab);

})(this);
