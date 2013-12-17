/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
(function() {

var app = angular.module("axelor.app");

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
	};

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
			if (closable == undefined && view.params) {
				closable = view.params.closable;
			}

			tab = view;
			tab.action = name;
			tab.closable = closable;

			openTab(tab, options);
		});
	}
	
	function openTabAsPopup(tab, options) {
		popups.push(tab);
	}

	function openTab(tab, options) {

		if (tab && tab.$popupParent) {
			return openTabAsPopup(tab, options);
		}
		
		var found = findTab(tab.action);

		options = options || tab.options;

		if (options && options.mode) {
			tab.viewType = VIEW_TYPES[options.mode] || options.mode;
		}

		tab.options = options;
		tab.title = tab.title || findTabTitle(tab);

		if (!found) {
			found = tab;
			if (options && options.__tab_prepend) {
				tabs.unshift(tab);
			} else {
				tabs.push(tab);
			}
		}

		_.each(tabs, function(tab) { tab.selected = false; });

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

		for (var i = 0; i < all.length; i++) {
			var tab = all[i];
			var viewScope = tab.$viewScope;
			if (viewScope && viewScope.confirmDirty) {
				select(tab);
				return viewScope.confirmDirty(function(){
					return close(tab);
				}, function() {
					close(null, tab);
					viewScope.applyLater();
				});
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
		canCloseTab: canCloseTab,
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

	$scope.navTabs = Object.defineProperty($scope, 'navTabs', {
		get: function() {
			return NavService.getTabs();
		}
	});
	
	$scope.navPopups = Object.defineProperty($scope, 'navPopups', {
		get: function() {
			return NavService.getPopups();
		}
	});

	$scope.selectedTab = Object.defineProperty($scope, 'selectedTab', {
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
			$location.search(opts.query);
		}
	}

	$scope.canCloseTab = function(tab) {
		return NavService.canCloseTab(tab);
	};
	
	function ensureScopeApply(func) {
		var args = _.rest(arguments);
		var promise = func.apply(NavService, args);
		if (promise && promise.then) {
			promise.then(function () {
				$scope.applyLater();
			});
		} else {
			$scope.applyLater();
		}
		return promise;
	}

	$scope.openTab = function(tab, options) {
		return ensureScopeApply(NavService.openTab, tab, options);
	};

	$scope.openTabByName = function(name, options) {
		return ensureScopeApply(NavService.openTabByName, name, options);
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
	
	$scope.tabTitle = function(tab) {
		return tab.title;
	};

	$scope.tabDirty = function(tab) {
		var viewScope = tab.$viewScope;
		if (viewScope && viewScope.isDirty) {
			return viewScope.isDirty();
		}
		return false;
	};
	
	$scope.$watch('selectedTab.viewType', function(viewType){
		if (viewType) {
			axelor.$adjustSize();
		}
	});

	$scope.$watch('routePath', function(path) {
		var app = $scope.app || {};
		if (!app.homeAction || _.last(path) !== "main") {
			return;
		}
		NavService.openTabByName(app.homeAction, {
			__tab_prepend: true,
			__tab_closable: false
		});
	});
}

TabCtrl.$inject = ['$scope', '$location', '$routeParams'];
function TabCtrl($scope, $location, $routeParams) {

	var app = $scope.app || {},
		params = _.clone($routeParams),
		search = _.clone($location.$$search);

	if (app.homeAction) {
		$scope.openTabByName(app.homeAction, {
			__tab_prepend: true,
			__tab_closable: false
		});
	}

	if (params.resource) {
        $scope.openTabByName(params.resource, {
    		mode: params.mode,
        	state: params.state,
        	search: search
    	});
    }
}

app.controller("NavCtrl", NavCtrl);
app.controller("TabCtrl", TabCtrl);

}).call(this);
