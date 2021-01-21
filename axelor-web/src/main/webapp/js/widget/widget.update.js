/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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

'use strict';

var ui = angular.module('axelor.ui');


ui.directive('uiDeleteButton', [function () {
  return {
    link: function (scope, element, attrs) {

    },
    replace: true,
    template:
      "<div class='btn-group delete-button'>" +
        "<button class='btn' ng-click='onDelete()' ng-if='hasButton(\"delete\")' ng-disabled='!canDelete()' title='{{ \"Delete\" | t}}'>" +
          "<i class='fa fa-trash-o'></i> <span ng-if='::!tbTitleHide' x-translate>Delete</span>" +
        "</button>" +
        "<button class='btn dropdown-toggle' data-toggle='dropdown' ng-if='hasButton(\"archive\")' ng-disabled='!canArchive()'>" +
          "<i class='fa fa-caret-down'></i>" +
        "</button>" +
        "<ul class='dropdown-menu' ng-if='hasButton(\"archive\")'>" +
          "<li><a href='' ng-click='onArchive()' x-translate>Archive</a></li>" +
          "<li><a href='' ng-click='onUnarchive()' x-translate>Unarchive</a></li>" +
        "</ul>" +
      "</div>"
  };
}]);

ui.directive('uiUpdateButton', ['$compile', function ($compile) {

  return {
    scope: {
      handler: '='
    },
    link: function (scope, element, attrs) {

      var menu = element.find('.update-menu'),
        toggleButton = null;

      scope.visible = false;

      scope.onMassUpdate = function (e) {
        if (menu && menu.is(':visible')) {
          hideMenu();
          return;
        }
        toggleButton = $(e.currentTarget);
        toggleButton.addClass("active");

        scope.onShow(e, menu);

        $(document).on('mousedown.update-menu', onMouseDown);

        scope.$applyAsync(function () {
          scope.visible = true;
        });
      };

      scope.onCancel = function () {
        hideMenu();
      };

      scope.canMassUpdate = function () {
        return true;
      };

      if (scope.handler && scope.handler.canMassUpdate) {
        scope.canMassUpdate = scope.handler.canMassUpdate;
      }

      function hideMenu() {
        $(document).off('mousedown.update-menu', onMouseDown);
        if (toggleButton) {
          toggleButton.removeClass("active");
        }
        scope.$applyAsync(function () {
          scope.visible = false;
        });
        return menu.hide();
      }

      function onMouseDown(e) {
        var all = $(menu).add(toggleButton);
        if (all.is(e.target) || all.has(e.target).length > 0) {
          return;
        }
        all = $('.ui-widget-overlay,.ui-datepicker:visible,.ui-dialog:visible,.ui-menu:visible');
        if (all.is(e.target) || all.has(e.target).length > 0) {
          return;
        }
        if(menu){
          hideMenu();
        }
      }

      // append box after the button
      scope.$timeout(function () {
        element.parents('.view-container').after(menu);
      });

      scope.$on('$destroy', function() {
        $(document).off('mousedown.update-menu', onMouseDown);
        if (menu) {
          menu.remove();
          menu = null;
        }
      });
    },
    replace: true,
    template:
      "<button class='btn update-menu-button' ng-click='onMassUpdate($event)' ng-disabled='!canMassUpdate()' >" +
        "<i class='fa fa-caret-down'></i>" +
        "<div ui-update-menu x-handler='handler' x-visible='visible'></div>" +
      "</button>"
  };
}]);

ui.directive('uiUpdateDummy', function () {

  return {
    require: '^uiUpdateForm',
    scope: {
      record: '='
    },
    controller: ['$scope', '$element', 'DataSource', 'ViewService', function($scope, $element, DataSource, ViewService) {

      var parent = $scope.$parent;
      var handler = parent.handler;

      $scope._viewParams = {
        model: handler._model,
        views: []
      };

      ui.ViewCtrl($scope, DataSource, ViewService);
      ui.FormViewCtrl.call(this, $scope, $element);

      function prepare(fields) {

        var schema = {
          cols: 1,
          type: 'form',
          items: _.values(fields)
        };

        $scope.fields = fields;
        $scope.schema = schema;
        $scope.schema.loaded = true;
      }

      var initialized = false;
      $scope.show = function () {

        if (initialized) return;
        initialized = true;

        var unwatch = parent.$watch('fields', function massFieldsWatch(fields) {
          if (_.isEmpty(fields)) return;
          unwatch();
          prepare(fields);
        });
      };

      $scope.setEditable();
      $scope.show();
    }],
    link: function (scope, element, attrs) {
      element.hide();
    },
    template: "<div class='hide' ui-view-form x-handler='true'></div>"
  };
});

ui.directive('uiUpdateForm',  function () {

  function findFields(fields, items) {

    var all = {};
    var accept = function (field) {
      var name = field.name;
      if (!field.massUpdate) return;
      if (/^(id|version|selected|archived|((updated|created)(On|By)))$/.test(name)) return;
      if (field.large || field.unique) return;
      switch (field.type) {
      case 'one-to-many':
      case 'many-to-many':
      case 'binary':
        return;
      }

      if (field.target) {
        field.canNew = false;
        field.canEdit = false;
      }
      field.hidden = false;
      field.required = false;
      field.readonly = false;
      field.onChange = null;
      field.placeholder = field.placeholder || field.title;

      all[name] = field;
    };

    _.each(fields, function (field, name) { accept(field); });
    _.each(items, function (item) {
      var field = fields[item.name];
      if (field) {
        accept(_.extend({}, field, item, { type: field.type }));
      }
    });

    return all;
  }

  return {
    replace: true,
    controller: ['$scope', 'ViewService', function ($scope, ViewService) {

      $scope.filters = [{}];
      $scope.options = [];

      $scope.onInit = _.once(function (view) {

        var handler = $scope.handler;
        var promise = ViewService.getFields(handler._model);
        promise.success(function (fields) {
          $scope.fields = findFields(fields, view.items);
          $scope.options = _.sortBy(_.values($scope.fields), 'title');
          $scope.record = {};
        });
      });

      $scope.addFilter = function (filter) {
        var all = $scope.filters;
        var last = _.last(all);
        if (last && !last.field) return;
        if (all.length > 0 && all.length === $scope.options.length) return;
        $scope.filters.push(filter || {});
        $scope.updateSelection();
      };

      $scope.removeFilter = function(filter) {
        var index = $scope.filters.indexOf(filter);
        if (index > -1) {
          $scope.filters.splice(index, 1);
        }
        if ($scope.filters.length === 0) {
          $scope.addFilter();
        }
      };

      $scope.notSelected = function (filter) {
        return function (opt) {
          return filter.field === opt.name || !opt.selected;
        };
      };

      var values = null;
      var canUpdate = false;

      function updateValues(record) {
        var keys = _.pluck($scope.filters, 'field'),
          vals = {};

        _.each(keys, function (key) {
          if (key) {
            vals[key] = (record || {})[key];
            if (vals[key] === undefined) {
              vals[key] = null;
            }
          }
        });

        values = vals;
        canUpdate = !_.isEmpty(values);
      }

      $scope.updateSelection = function updateSelection () {

        var selected = _.pluck($scope.filters, 'field');
        _.each($scope.options, function (opt) {
          opt.selected = selected.indexOf(opt.name) > -1;
        });

        updateValues($scope.record);
      };

      $scope.$watch('record', updateValues, true);

      $scope.canUpdate = function () {
        return canUpdate;
      };

      $scope.updateAll = false;

      $scope.applyUpdate = function () {

        var handler = $scope.handler;
        var ds = handler._dataSource;

        function doUpdate() {
          var promise, items;

          items = _.map(handler.selection, function(index) {
            return handler.dataView.getItem(index);
          });
          items = _.pluck(items, "id");

          if ($scope.updateAll) {
            items = null;
          } else if (items.length === 0) {
            return $scope.onCancel();
          }

          promise = ds.updateMass(values, items);
          promise.success(function () {
            handler.onRefresh();
            $scope.onCancel();
          });
        }

        var count;
        if ($scope.updateAll) {
          count = ds._page.total;
        } else if(handler.selection && handler.selection.length > 0) {
          count = handler.selection.length;
        } else {
          return;
        }

        var message = _t('Do you really want to update all {0} record(s)?', count);
        axelor.dialogs.confirm(message, function (confirmed) {
          if (confirmed) {
            doUpdate();
          }
        });
      };
    }],
    link: function (scope, element, attrs) {

      scope.onSelect = function (name) {
        scope.updateSelection();
        setTimeout(adjustEditors);
      };

      scope.clearFilter = function() {
        scope.filters.length = 0;
        scope.addFilter();
        scope.record = {};
        adjustEditors();
      };

      scope.remove = function(filter) {
        scope.removeFilter(filter);
        adjustEditors();
      };

      scope.onCancel = function () {
        if (scope.$parent.onCancel) {
          scope.$parent.onCancel();
        }
      };

      function adjustEditors() {

        element.find('[x-place-for] [x-field]').each(function () {
          var editor = $(this);
          var parent = editor.data('$parent');
          editor.appendTo(parent);
        });

        _.each(scope.filters, function (filter) {
          adjustEditor(filter.field);
        });
      }

      function adjustEditor(name) {
        var span = element.find('[x-place-for=' + name + ']');
        var editor = element.find('[x-field=' + name + '].form-item-container,[x-field=' + name + '].boolean-item').first();
        var parent = editor.data('$parent');
        if (!parent) {
          parent = editor.parent();
          editor.data('$parent', parent);
        }
        editor.appendTo(span);
      }
    },
    template:
      "<form class='form-inline update-form filter-form'>" +
        "<strong x-translate>Mass Update</strong> " +
        "<hr>" +
        "<table class='form-layout'>" +
          "<tr ng-repeat='filter in filters' class='form-inline'>" +
            "<td class='filter-remove'>" +
              "<a href='' ng-click='remove(filter)'><i class='fa fa-times'></i></a>" +
            "</td>" +
            "<td class='form-item'>" +
              "<span class='form-item-container'>" +
              "<select ng-model='filter.field' ng-options='v.name as v.title for v in options | filter:notSelected(filter)' ng-change='onSelect(filter.field)'></select>" +
              "</span>" +
            "</td>" +
            "<td class='form-item' x-place-for='{{filter.field}}'>" +
            "</td>" +
          "</tr>" +
        "</table>" +
        "<div class='links'>"+
          "<a href='' ng-click='addFilter()' x-translate>Add Field</a>" +
          "<span class='divider'>|</span>"+
          "<a href='' ng-click='clearFilter()' x-translate>Clear</a>" +
        "</div>" +
        "<div ui-update-dummy x-record='record'></div>"+
      "</form>"
  };
});

ui.directive('uiUpdateMenu', function () {

  return {
    replace: true,
    scope: {
      handler: '='
    },
    link: function (scope, element, attrs) {

      scope.$parent.onShow = function (event, menu) {
        scope.handler._viewPromise.then(function (view) {
          var elem = $(event.currentTarget);
          if (scope.onInit) {
            scope.onInit(view);
          }
          menu.show();
          menu.position({
            my: "left top",
            at: "left bottom",
            of: elem
          });
        });
      };
    },
    template:
      "<div class='update-menu filter-menu' ui-watch-if='$parent.visible'>" +
        "<div ui-update-form></div>" +
        "<hr>" +
        "<div class='form-inline'>" +
          "<button class='btn btn-small' ng-disabled='!canUpdate()' ng-click='applyUpdate()'><span x-translate>Update</span></button> " +
          "<button class='btn btn-small' ng-click='onCancel()'><span x-translate>Cancel</span></button> " +
          "<label class='checkbox update-all'>" +
            "<input type='checkbox' ng-model='updateAll'> <span x-translate>Update all</span>" +
          "</label> " +
        "</div>" +
      "</div>"
  };
});

})();
