/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

var ui = angular.module('axelor.ui');

function BaseCardsCtrl(type, $scope, $element) {

	ui.DSViewCtrl(type, $scope, $element);

	$scope.getRouteOptions = function() {
		return {
			mode: type,
			args: []
		};
	};

	$scope.setRouteOptions = function(options) {
		if (!$scope.isNested) {
			$scope.updateRoute();
		}
	};

	var ds = $scope._dataSource;
	var initialized = false;

	$scope.onShow = function (viewPromise) {

		if (initialized) {
			return $scope.onRefresh();
		}

		initialized = true;

		viewPromise.then(function (meta) {
			$scope.parse(meta.fields, meta.view);
			$scope.$broadcast("on:clear-filter-silent");
		});
	};

	$scope.parse = function (fields, view) {

	};

	$scope.onNew = function () {
		$scope.switchTo('form', function (formScope) {
			formScope.edit(null);
			formScope.setEditable();
			formScope.$broadcast("on:new");
		});
	};

	$scope.onRefresh = function () {
		return $scope.filter({});
	};

	function update(records) {
		$scope.records = records;
	}

	$scope.filter = function(options) {
		var view = $scope.schema;
		var opts = {
			fields: _.pluck($scope.fields, 'name')
		};

		if (options.criteria || options._domains) {
			opts.filter = options;
		}
		if (options.archived !== undefined) {
			opts.archived = options.archived;
		}
		if (view.orderBy) {
			opts.sortBy = view.orderBy.split(',');
		}

		return ds.search(opts).success(update).then(function () {
			return ds.fixPage();
		});
	};

	$scope.pagerText = function() {
		var page = ds._page;
		if (page && page.from !== undefined) {
			if (page.total === 0) return null;
			return _t("{0} to {1} of {2}", page.from + 1, page.to, page.total);
		}
	};

	$scope.onNext = function() {
		var fields = _.pluck($scope.fields, 'name');
		return ds.next(fields).success(update);
	};

	$scope.onPrev = function() {
		var fields = _.pluck($scope.fields, 'name');
		return ds.prev(fields).success(update);
	};
}

ui.controller("CardsCtrl", ['$scope', '$element', function CardsCtrl($scope, $element) {

	BaseCardsCtrl.call(this, 'cards', $scope, $element);

	$scope.viewItems = {};

	$scope.parse = function (fields, view) {
		var viewItems = {};
		_.each(view.items, function (item) {
			if (item.name) {
				viewItems[item.name] = _.extend({}, item, fields[item.name], item.widgetAttrs);
			}
		});
		$scope.viewItems = viewItems;
		$scope.onRefresh();
		$scope.waitForActions(axelor.$adjustSize);
	};

	$scope.onExport = function (full) {
		var fields = full ? [] : _.pluck($scope.viewItems, 'name');
		return $scope._dataSource.export_(fields).success(function(res) {
			var fileName = res.fileName;
			var filePath = 'ws/rest/' + $scope._model + '/export/' + fileName;
			ui.download(filePath, fileName);
		});
	};
}]);

ui.controller("KanbanCtrl", ['$scope', '$element', 'ActionService', function KanbanCtrl($scope, $element, ActionService) {

	BaseCardsCtrl.call(this, 'kanban', $scope, $element);

	$scope.parse = function (fields, view) {
		var columnBy = fields[view.columnBy] || {};
		var columns = _.map(columnBy.selectionList, function (item) {
			return item;
		});

		columns = _.first(columns, 6);

		var first = _.first(columns);
		if (view.onNew) {
			first.canCreate = true;
		}

		var sequenceBy = fields[view.sequenceBy] || {};
		if (["integer", "long"].indexOf(sequenceBy.type) === -1 || ["id", "version"].indexOf(sequenceBy.name) > -1) {
			throw new Error("Invalid sequenceBy field in view: " + view.name);
		}

		$scope.sortableOptions.disabled = !view.draggable || !$scope.hasPermission('write');
		$scope.columns = columns;
		$scope.colSpan = "kanban-cs-" + columns.length;
	};

	$scope.move = function (record, to, next, prev) {
		if(!record) {
		    return;
		}
		var view = $scope.schema;
		var rec = _.pick(record, "id", "version", view.sequenceBy);
		var ds = $scope._dataSource._new($scope._model);

		// update columnBy
		rec[view.columnBy] = to;

		// update sequenceBy
		var all = _.compact([prev, rec, next]);
		var offset = _.min(_.pluck(all, view.sequenceBy)) || 0;

		_.each(all, function (item, i) {
			item[view.sequenceBy] = offset + i;
		});

		function doSave() {
			return ds.saveAll(all).success(function (records) {
				_.each(all, function (item) {
					_.extend(item, ds.get(item.id));
				});
				record.version = rec.version;
			});
		}

		if (view.onMove) {
			var actScope = $scope.$new();
			actScope.record = rec;
			actScope.getContext = function () {
				return _.extend({}, $scope._context, rec);
			};
			return ActionService.handler(actScope, $(), { action: view.onMove}).handle().then(function () {
				return doSave();
			}, function (err) {
				axelor.notify.error(_t('Unable to move the record.'));
				$scope.onRefresh();
			});
		}

		return doSave();
	};

	$scope.onRefresh = function () {
		$scope.$broadcast("on:refresh");
	};

	$scope.filter = function(searchFilter) {
		var options = {};
		if (searchFilter.criteria || searchFilter._domains) {
			options = {
				filter: searchFilter
			};
			if (searchFilter.archived !== undefined) {
				options.archived = searchFilter.archived;
			}
			$scope.$broadcast("on:filter", options);
		}
	};

	$scope.sortableOptions = {
		connectWith: ".kanban-card-list",
		items: ".kanban-card",
		tolerance: "pointer",
		stop: function (event, ui) {
			var item = ui.item;
			var sortable = item.sortable;
			var source = sortable.source.scope();
			var target = (sortable.droptarget || $(this)).scope();

			var next = item.next().scope();
			var prev = item.prev().scope();
			if (next) next = next.record;
			if (prev) prev = prev.record;

			var index = sortable.dropindex;
			if (source === target && sortable.index === index) {
				return;
			}

			$scope.move(target.records[index], target.column.value, next, prev);
			$scope.$applyAsync();
		}
	};
}]);

ui.directive('uiKanbanColumn', ["ActionService", function (ActionService) {

	return {
		scope: true,
		link: function (scope, element, attrs) {

			var ds = scope._dataSource._new(scope._model);
			var view = scope.schema;
			var elemMore = element.children(".kanban-more");

			ds._context = _.extend({}, scope._dataSource._context);
			ds._context[view.columnBy] = scope.column.value;
			ds._page.limit = view.limit || 20;

			var domain = "self." + view.columnBy + " = :" + view.columnBy;
			ds._domain = scope._dataSource._domain ? scope._dataSource._domain + " AND " + domain : domain;

			scope.records = [];

			function fetch(options) {
				var opts = _.extend({
					offset: 0,
					sortBy: [view.sequenceBy]
				}, options);
				elemMore.hide();
				return ds.search(opts).success(function (records) {
					scope.records = scope.records.concat(records);
					elemMore.fadeIn('slow');
				});
			}

			scope.hasMore = function () {
				var page = ds._page;
				var next = page.from + page.limit;
				return next < page.total;
			};
			
			scope.isEmpty = function () {
				return scope.records.length == 0;
			};

			scope.onMore = function () {
				var page = ds._page;
				var next = scope.records.length;
				if (next < page.total) {
					return fetch({
						offset: next
					});
				}
			};

			var onNew = null;

			scope.getContext = function () {
				var ctx = _.extend({}, scope._context);
				ctx._value = scope.newItem;
				return ctx;
			};

			scope.newItem = null;
			scope.onCreate = function () {

				var rec = scope.record = {};
				var view = scope.schema;

				rec[view.columnBy] = scope.column.value;

				if (onNew === null) {
					onNew = ActionService.handler(scope, element, {
						action: view.onNew
					});
				}

				var ds = scope._dataSource;
				var promise = onNew.handle();
				promise.then(function () {
					ds.save(scope.record).success(function (rec) {
						scope.newItem = null;
						scope.records.unshift(rec);
					});
				});
			};

			scope.onEdit = function (record, readonly) {
				scope.switchTo('form', function (formScope) {
					formScope.edit(record);
					formScope.setEditable(!readonly && scope.hasPermission('write') && formScope.canEdit());
				});
			};

			scope.onDelete = function (record) {
				axelor.dialogs.confirm(_t("Do you really want to delete the selected record?"),
				function(confirmed) {
					if (!confirmed) {
						return;
					}
					ds.removeAll([record]).success(function(records, page) {
						var index = scope.records.indexOf(record);
						scope.records.splice(index, 1);
					});
				});
			};

			scope.$on("on:refresh", function (e) {
				scope.newItem = null;
				scope.records.length = 0;
				fetch();
			});

			scope.$on("on:filter", function (e, options) {
				scope.newItem = null;
				scope.records.length = 0;
				return fetch(options);
			});
			
			element.on("click", ".kanban-card", function (e) {
				if ($(e.target).parents(".kanban-card-menu").size()) {
					return;
				}
				var elem = $(this);
				var record = elem.scope().record;
				scope.onEdit(record, true);
				scope.applyLater();
			});

			fetch();
		}
	};
}]);

ui.directive('uiCards', function () {

	return function (scope, element, attrs) {

		var onRefresh = scope.onRefresh;
		scope.onRefresh = function () {
			scope.records = null;
			return onRefresh.apply(scope, arguments);
		};

		scope.onEdit = function (record, readonly) {
			var ds = scope._dataSource;
			var page = ds._page;
			page.index = record ? ds._data.indexOf(record) : -1;
			scope.switchTo('form', function (formScope) {
				formScope.setEditable(!readonly && scope.hasPermission('write') && formScope.canEdit());
			});
		};

		scope.onDelete = function (record) {
			axelor.dialogs.confirm(_t("Do you really want to delete the selected record?"),
			function(confirmed) {
				if (!confirmed) {
					return;
				}
				var ds = scope._dataSource;
				ds.removeAll([record]).success(function(records, page) {
					var index = scope.records.indexOf(record);
					scope.records.splice(index, 1);
				});
			});
		};

		element.on("click", ".kanban-card", function (e) {
			if ($(e.target).parents(".kanban-card-menu").size() ||
				$(e.target).is('[ng-click],[ui-action-click],button,a,.iswitch,.ibox')) {
				return;
			}
			var elem = $(this);
			var record = elem.scope().record;
			scope.onEdit(record, true);
			scope.applyLater();
		});
	};
});

ui.directive('uiCard', ["$compile", function ($compile) {

	return {
		scope: true,
		link: function (scope, element, attrs) {

			var body = element.find(".kanban-card-body");
			var record = scope.record;
			var evalScope = axelor.$evalScope(scope);

			evalScope.record = record;
			evalScope.getContext = scope.getContext = function () {
				var ctx = _.extend({}, scope._context, scope.record);
				ctx._model = scope._model;
				return ctx;
			};

			if (!record.$processed) {
				element.hide();
			}

			function process(record) {
				if (record.$processed) {
					return record;
				}
				record.$processed = true;
				for (var name in record) {
					if (!record.hasOwnProperty(name) || name.indexOf('.') === -1) {
						continue;
					}
					var nested = record;
					var names = name.split('.');
					var head = _.first(names, names.length - 1);
					var last = _.last(names);
					var i, n;
					for (i = 0; i < head.length; i++) {
						n = head[i];
						nested = nested[n] || (nested[n] = {});
					}
					nested[last] = record[name];
				}
				return record;
			}

			evalScope.$watch("record", function (record) {
				_.extend(evalScope, process(record));
			}, true);

			evalScope.$image = function (fieldName, imageName) {
				return ui.formatters.$image(scope, fieldName, imageName);
			};

			evalScope.$fmt = function (fieldName) {
				return ui.formatters.$fmt(scope, fieldName, evalScope[fieldName]);
			};

			var template = (scope.schema.template || "<span></span>").trim();
			if (template.indexOf('<') !== 0) {
				template = "<span>" + template + "</span>";
			}

			scope.hilite = null;

			$compile(template)(evalScope).appendTo(body);

			var hilites = scope.schema.hilites || [];
			for (var i = 0; i < hilites.length; i++) {
				var hilite = hilites[i];
				if (axelor.$eval(evalScope, hilite.condition, scope.record)) {
					scope.hilite = hilite;
					break;
				}
			}

			if (scope.schema.cardWidth) {
				element.parent().css("width", scope.schema.cardWidth);
			}

			element.fadeIn("slow");
		}
	};
}]);

})();
