(function() {

var app = angular.module("axelor.app");

app.factory('NavService', ['$location', 'MenuService', function($location, MenuService) {

	var tabs = [];
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
			return openTab(tab);
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

	function openTab(tab, options) {

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
			setTimeout(function(){
				$.event.trigger('adjustSize');
			});
		});
	}

	function __closeTab(tab) {
		var index = _.indexOf(tabs, tab);

		// remove tab
		tabs.splice(index, 1);

		if (tab.selected) {
			if (index == tabs.length)
				index -= 1;
			_.each(tabs, function(tab){
				tab.selected = false;
			});
			var select = tabs[index];
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

	function closeTab(tab) {
		var viewScope = tab.$viewScope;
		if (viewScope && viewScope.confirmDirty) {
			viewScope.confirmDirty(function(){
				__closeTab(tab);
			});
		} else {
			__closeTab(tab);
		}
	}

	function getTabs() {
		return tabs;
	}

	function getSelected() {
		return selected;
	}

	return {
		openTabByName: openTabByName,
		openTab: openTab,
		canCloseTab: canCloseTab,
		closeTab: closeTab,
		getTabs: getTabs,
		getSelected: getSelected
	};
}]);

NavCtrl.$inject = ['$scope', '$rootScope', '$location', 'NavService'];
function NavCtrl($scope, $rootScope, $location, NavService) {

	$scope.tabs = Object.defineProperty($scope, 'tabs', {
		get: function() {
			return NavService.getTabs();
		}
	});

	$scope.selectedTab = Object.defineProperty($scope, 'selectedTab', {
		get: function() {
			return NavService.getSelected();
		}
	});

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

	$scope.openTab = function(tab, options) {
		return NavService.openTab(tab, options);
	};

	$scope.openTabByName = function(name, options) {
		return NavService.openTabByName(name, options);
	};

	$scope.closeTab = function(tab) {
		return NavService.closeTab(tab);
	};

	$scope.$watch('selectedTab.viewType', function(viewType){
		if (viewType) {
			setTimeout(function(){
				$.event.trigger('adjustSize');
			});
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

	var params = _.clone($routeParams),
		search = _.clone($location.$$search);

	var resource = params.resource,
		state = params.state,
		mode = params.mode;

	if (resource) {
        $scope.openTabByName(resource, {
        	mode: mode,
        	state: state,
        	search: search
        });
    }
}

app.controller("NavCtrl", NavCtrl);
app.controller("TabCtrl", TabCtrl);

}).call(this);
