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
(function() {

"use strict";

var app = angular.module("axelor.app");

function useSingleTabOnly() {
	return axelor.device.mobile || !!axelor.config['view.single.tab'] || axelor.config['user.singleTab'];
}

app.factory('NavService', ['$location', 'MenuService', function($location, MenuService) {

	var tabs = [];
	var popups = [];
	var selected = null;

	var VIEW_TYPES = {
		'list' : 'grid',
		'edit' : 'form'
	};

	function findTab(key) {
		return _.find(tabs, function(tab){
			return tab.action == key;
		});
	}

	function findTabTitle(tab) {
		var first;
		if (tab.title) {
			return tab.title;
		}
		first = _.first(tab.views);
		if (first) {
			return first.title || first.name;
		}
		return tab.name || "Unknown";
	}

	function openView(view, options) {
		if (view && (view.type || view.viewType) == 'html') {
			var first = _.first(view.views) || view;
			view.views = [{
				name: first.name,
				resource: first.resource,
				title: first.title,
				type: 'html'
			}];
		}

		var closable = options && options.__tab_closable;
		if (!closable && view.params && view.params.closable !== undefined) {
			closable = view.params.closable;
		}

		view.closable = closable;

		openTab(view, options);
	}

	function openTabByName(name, options) {

		var tab = findTab(name);
		if (tab) {
			return openTab(tab, options);
		}

		return MenuService.action(name).success(function(result){
			if (!result.data) {
				return;
			}
			var view = result.data[0].view;
			view.action = name;
			return openView(view, options);
		});
	}
	
	function openTabAsPopup(tab, options) {
		popups.push(tab);
	}

	function openTab(tab, options) {

		if (tab && tab.$popupParent) {
			return openTabAsPopup(tab, options);
		}

		options = options || tab.options;

		if (options && options.mode) {
			tab.viewType = VIEW_TYPES[options.mode] || options.mode;
		}

		tab.options = options;
		tab.title = tab.title || findTabTitle(tab);

		function __doSelect(found) {

			var lastScope = (selected||{}).$viewScope || {};
			if (lastScope.$locationChangeOff) {
				lastScope.$locationChangeOff();
			}

			found.selected = true;
			selected = found;

			if (options && tab.$viewScope) {
				var view = tab.$viewScope._views[tab.viewType],
					promise = view ? view.deferred.promise : null;
				if (promise) {
					promise.then(function(viewScope) {
						viewScope.setRouteOptions(options);
					});
				}
			}

			setTimeout(function(){
				$.event.trigger('adjust');
				axelor.$adjustSize();
			});
		}

		function __singleShow(found) {
			tabs.length = 0;
			tabs.push(found);
			return __doSelect(found);
		}

		var found = findTab(tab.action);

		if (useSingleTabOnly()) {

			if (found) {
				return __singleShow(found);
			}

			var last = _.last(tabs);
			if (last) {
				return closeTab(last, function () {
					__singleShow(tab);
				});
			}
			return __singleShow(tab);
		}

		if (!found) {
			found = tab;
			if (options && options.__tab_prepend) {
				tabs.unshift(tab);
			} else {
				tabs.push(tab);
			}
		}

		_.each(tabs, function(tab) {
			tab.selected = false;
		});

		return __doSelect(found);
	}

	function __closeTab(tab, callback) {
		
		var all = tab.$popupParent ? popups : tabs;
		var index = _.indexOf(all, tab);

		// remove tab
		all.splice(index, 1);
		
		if (tabs.length === 0) {
			selected = null;
		}
		if (_.isFunction(callback)) {
			callback();
		}
		if (tab.$popupParent) {
			return;
		}

		if (tab.selected) {
			if (index == tabs.length)
				index -= 1;
			_.each(all, function(tab){
				tab.selected = false;
			});
			var select = all[index];
			if (select) {
				select.selected = true;
				openTab(select);
			} else {
				$location.path('/');
			}
		}
	}

	function canCloseTab(tab) {
		return tab.closable === undefined ? true : tab.closable;
	}

	function closeTab(tab, callback) {
		var viewScope = tab.$viewScope;
		if (viewScope && viewScope.confirmDirty) {
			viewScope.confirmDirty(function(){
				__closeTab(tab, callback);
			});
		} else {
			__closeTab(tab, callback);
		}
	}
	
	function closeTabs(selection) {
		var all = _.flatten([selection], true);

		function select(tab) {
			if (!tab.selected) {
				tab.selected = true;
				openTab(tab);
			}
		}
		
		function close(tab, ignore) {
			var at = tabs.indexOf(tab);
			if (at > -1) {
				tabs.splice(at, 1);
			}
			closeTabs(_.difference(selection, [ignore, tab]));
			if (tabs.length === 0) {
				selected = null;
			}
		}

		function doConfirm(tab, viewScope) {
			return viewScope.confirmDirty(function(){
				return close(tab);
			}, function() {
				close(null, tab);
				viewScope.applyLater();
			});
		}

		for (var i = 0; i < all.length; i++) {
			var tab = all[i];
			var viewScope = tab.$viewScope;
			if (viewScope && viewScope.confirmDirty) {
				select(tab);
				return doConfirm(tab, viewScope);
			}
			return close(tab);
		}

		if (tabs.indexOf(selected) == -1) {
			selected = null;
		}
		
		if (selected) {
			return openTab(selected);
		}
		
		var first = _.first(tabs);
		if (first && !first.selected) {
			return openTab(first);
		}
		
		axelor.$adjustSize();
	}
	
	function closeTabOthers(current) {
		var rest = _.filter(tabs, function(tab) {
			return canCloseTab(tab) && tab !== current;
		});
		if (current && !current.selected) {
			current.selected = true;
			openTab(current);
		}
		return closeTabs(rest);
	}
	
	function closeTabAll() {
		closeTabOthers();
	}

	function reloadTab(current) {
		var viewScope = current.$viewScope;
		if (viewScope) {
			viewScope.$broadcast('on:tab-reload', current);
		}
	}

	function getTabs() {
		return tabs;
	}
	
	function getPopups() {
		return popups;
	}

	function getSelected() {
		return selected;
	}

	return {
		openTabByName: openTabByName,
		openTab: openTab,
		openView: openView,
		canCloseTab: canCloseTab,
		reloadTab: reloadTab,
		closeTab: closeTab,
		closeTabOthers: closeTabOthers,
		closeTabAll: closeTabAll,
		getTabs: getTabs,
		getPopups: getPopups,
		getSelected: getSelected
	};
}]);

NavCtrl.$inject = ['$scope', '$rootScope', '$location', 'NavService'];
function NavCtrl($scope, $rootScope, $location, NavService) {

	$scope.singleTabOnly = useSingleTabOnly();

	Object.defineProperty($scope, 'navTabs', {
		get: function() {
			return NavService.getTabs();
		}
	});
	
	Object.defineProperty($scope, 'navPopups', {
		get: function() {
			return NavService.getPopups();
		}
	});

	Object.defineProperty($scope, 'selectedTab', {
		get: function() {
			return NavService.getSelected();
		}
	});
	
	$scope.hasNabPopups = function () {
		return $scope.navPopups && $scope.navPopups.length > 0;
	};

	$scope.menuClick = function(event, record) {

		if (record.isFolder)
			return;

		if (axelor.device.small) {
			$("#offcanvas").removeClass("active");
		}

		$scope.openTabByName(record.action);
		$scope.$apply();
	};

	$scope.navClick = function(tab) {
		$scope.openTab(tab);
		$scope.$broadcast("on:nav-click", tab);
	};

	$scope.$on("on:update-route", update);

	function update(event) {

		var tab = $scope.selectedTab,
			scope = event.targetScope;

		if (!tab || !tab.action || scope !== tab.$viewScope || !scope.getRouteOptions) {
			return;
		}
		if (tab.action.indexOf('$act') > -1) {
			return;
		}

		var path = tab.action,
			opts = scope.getRouteOptions(),
			mode = opts.mode,
			args = opts.args;

		path = "/ds/" + path + "/" + mode;
		args = _.filter(args, function(arg) {
			return _.isNumber(args) || arg;
		});

		if (args.length) {
			path += "/" + args.join("/");
		}

		if ($location.$$path !== path) {
			$location.path(path);
			$location.search(opts.query || "");
		}
	}

	$scope.canCloseTab = function(tab) {
		return NavService.canCloseTab(tab);
	};
	
	$scope.openTab = function(tab, options) {
		return NavService.openTab(tab, options);
	};

	$scope.openTabByName = function(name, options) {
		return NavService.openTabByName(name, options);
	};

	$scope.closeTab = function(tab, callback) {
		var wasSelected = tab.selected;
		if (NavService.canCloseTab(tab)) {
			NavService.closeTab(tab, callback);
			if ($scope.selectedTab && wasSelected) {
				$scope.$broadcast("on:nav-click", $scope.selectedTab);
			}
		}
	};
	
	$scope.closeTabOthers = function(tab) {
		var wasSelected = tab.selected;
		NavService.closeTabOthers(tab);
		if ($scope.selectedTab === tab && !wasSelected) {
			$scope.$broadcast("on:nav-click", tab);
		}
	};
	
	$scope.closeTabAll = function() {
		return NavService.closeTabAll();
	};
	
	$scope.reloadTab = function(tab) {
		return NavService.reloadTab(tab);
	};

	$scope.tabTitle = function(tab) {
		var vs = tab.$viewScope || {};
		if (vs.viewType === "form") {
			return vs.viewTitle || tab.title;
		}
		return tab.title;
	};

	$scope.tabDirty = function(tab) {
		var viewScope = tab.$viewScope;
		if (viewScope && viewScope.isDirty) {
			return viewScope.isDirty();
		}
		return false;
	};

	// expose common methods to $rootScope
	$scope.$root.openTab = $scope.openTab;
	$scope.$root.openTabByName = $scope.openTabByName;

	$scope.$watch('selectedTab.viewType', function(viewType){
		if (viewType) {
			axelor.$adjustSize();
		}
	});

	$scope.$watch('routePath', function(path) {
		var homeAction = axelor.config["user.action"];
		if (!homeAction || _.last(path) !== "main") {
			return;
		}
		NavService.openTabByName(homeAction, {
			__tab_prepend: true,
			__tab_closable: false
		});
	});

	var confirm = _t('Current changes will be lost.');
	
	function onbeforeunload(e) {
		var tabs = $scope.navTabs || [];
		for (var i = 0; i < tabs.length; i++) {
			var vs = (tabs[i]||{}).$viewScope;
			if (vs && vs.$$dirty) {
				return confirm;
			}
		}
	}
	
	$(function () {

		 // menu toggle logic
		 var menuToggled = false;
		 var navigator = axelor.config["user.navigator"];

		 if (navigator !== 'hidden') {
			 $('#offcanvas-toggle').find('a').click(function (e) {
				 var active = ! $("#offcanvas").hasClass('inactive');
				 if (active && axelor.device.small) {
					 active =  $("#offcanvas").hasClass('active');
				 }
			     $("#offcanvas").toggleClass("active", !active && axelor.device.small);
			     $("#offcanvas").toggleClass("inactive", active && !axelor.device.small);
			     if (!axelor.device.mobile) {
			    	 setTimeout(axelor.$adjustSize, 100);
				 }
			 });
		 }

		 $("#offcanvas,#offcanvas-toggle").toggleClass("hidden-menu", navigator === "hidden");
		 if (navigator === "collapse") {
			 $("#offcanvas").addClass("inactive");
		 }
		 $scope.ajaxStop(function () {
			 setTimeout(function () {
				 $("#offcanvas,#offcanvas-toggle").removeClass("hidden");
			 }, 100);
		 }, 100);

		 $(window).on('resize', _.debounce(function () {
			 $("#offcanvas").removeClass(axelor.device.small ? 'inactive' : 'active');
		 }, 100));

		 // confirm dirty
		 $(window).on('beforeunload', onbeforeunload);
	});
}

TabCtrl.$inject = ['$scope', '$location', '$routeParams'];
function TabCtrl($scope, $location, $routeParams) {

	var homeAction = axelor.config["user.action"],
		params = _.clone($routeParams),
		search = _.clone($location.$$search);
	
	var opts = {
		mode: params.mode,
    	state: params.state,
    	search: search
	};

	if (homeAction === params.resource) {
		_.extend(opts, {
			__tab_prepend: true,
			__tab_closable: false
		});
	}

	if (params.resource) {
		$scope.openTabByName(params.resource, opts);
    }
}

app.controller("NavCtrl", NavCtrl);
app.controller("TabCtrl", TabCtrl);

})();
