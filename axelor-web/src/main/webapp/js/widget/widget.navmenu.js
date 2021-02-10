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

"use strict";

var ui = angular.module('axelor.ui');

NavMenuCtrl.$inject = ['$scope', '$element', 'MenuService', 'NavService'];
function NavMenuCtrl($scope, $element, MenuService, NavService) {

  $scope.menus = []; 	// the first four visible menus
  $scope.more = [];	// rest of the menus

  var hasSideBar = axelor.config['view.menubar.location'] !== 'top';

  MenuService.all().then(function(response) {
    var res = response.data,
      data = res.data;

    var items = {};
    var all = [];

    _.each(data, function(item) {
      items[item.name] = item;
      if (item.children === undefined) {
        item.children = [];
      }

    });

    _.each(data, function(item) {

      if (hasSideBar && !item.top) {
        return;
      }

      if (!item.parent) {
        return all.push(item);
      }
      var parent = items[item.parent];
      if (parent) {
        parent.children.push(item);
      }
    });

    $scope.menus = all;
    $scope.more = all;

    $scope.extra = {
      title: 'More',
      children: $scope.more
    };
  });

  this.isSubMenu = function(item) {
    return item && item.children && item.children.length > 0;
  };

  this.onItemClick = function(item) {
    if (item.action && !this.isSubMenu(item)) {
      NavService.openTabByName(item.action);
    }
  };

  $scope.hasImage = function (menu) {
    return menu.icon && menu.icon.indexOf('fa-') !== 0 && menu.icon.indexOf('empty') != -1;
  };

  $scope.hasIcon = function (menu) {
    return menu.icon && menu.icon.indexOf('fa-') === 0 && menu.icon.indexOf('empty') != -1;
  };

  $scope.hasText = function (menu) {
    return !menu.icon || menu.icon.indexOf('empty') === -1;
  };
}

ui.directive('navMenuBar', function() {

  return {

    replace: true,

    controller: NavMenuCtrl,

    scope: true,

    link: function(scope, element, attrs, ctrl) {

      var elemTop,
        elemSub,
        elemMore;

      var siblingsWidth = 0;
      var adjusting = false;

      element.hide();

      function adjust() {

        if (adjusting) {
          return;
        }

        adjusting = true;

        var count = 0;
        var parentWidth = element.parent().width() - 32;

        elemMore.hide();
        elemTop.hide();
        elemSub.hide();

        while (count < elemTop.length) {
          var elem = $(elemTop[count]).show();
          var width = siblingsWidth + element.width();
          if (width > parentWidth) {
            elem.hide();

            // show more...
            elemMore.show();
            width = siblingsWidth + element.width();
            if (width > parentWidth) {
              count--;
              $(elemTop[count]).hide();
            }

            break;
          }
          count++;
        }

        if (count === elemTop.length) {
          elemMore.hide();
        }
        while(count < elemTop.length) {
          $(elemSub[count++]).show();
        }

        adjusting = false;
      }

      function setup() {

        element.siblings().each(function () {
          siblingsWidth += $(this).width();
        });

        elemTop = element.find('.nav-menu.dropdown:not(.nav-menu-more)');
        elemMore = element.find('.nav-menu.dropdown.nav-menu-more');
        elemSub = elemMore.find('.dropdown-menu:first > .dropdown-submenu');

        element.show();
        adjust();

        $(window).on("resize.menubar", adjust);
      }

      element.on('$destroy', function () {
        if (element) {
          $(window).off("resize.menubar");
          element = null;
        }
      });

      var unwatch = scope.$watch('menus', function navMenusWatch(menus, old) {
        if (!menus || menus.length === 0  || menus === old) {
          return;
        }
        unwatch();
        setTimeout(setup, 100);
      });
    },

    template:
      "<ul class='nav nav-menu-bar'>" +
        "<li class='nav-menu dropdown' ng-class='{empty: !hasText(menu)}' ng-repeat='menu in menus track by menu.name'>" +
          "<a href='javascript:' class='dropdown-toggle' data-toggle='dropdown'>" +
            "<img ng-if='hasImage(menu)' ng-src='{{menu.icon}}'> " +
            "<i ng-if='hasIcon(menu)' class='fa {{menu.icon}}'></i> " +
            "<span ng-if='hasText(menu)' ng-bind='menu.title'></span> " +
            "<b class='caret'></b>" +
          "</a>" +
          "<ul nav-menu='menu'></ul>" +
        "</li>" +
        "<li class='nav-menu nav-menu-more dropdown' style='display: none;'>" +
          "<a href='javascript:' class='dropdown-toggle' data-toggle='dropdown'>" +
            "<span x-translate>More</span>" +
            "<b class='caret'></b>" +
          "</a>" +
          "<ul nav-menu='extra'></ul>" +
        "</li>" +
      "</ul>"
  };
});

ui.directive('navMenu', function() {

  return {
    replace: true,
    require: '^navMenuBar',
    scope: {
      menu: '=navMenu'
    },
    link: function(scope, element, attrs, ctrl) {

    },
    template:
      "<ul class='dropdown-menu'>" +
        "<li ng-repeat='item in menu.children track by item.name' nav-menu-item='item'>" +
      "</ul>"
  };
});

ui.directive('navMenuItem', ['$compile', function($compile) {

  return {
    replace: true,
    require: '^navMenuBar',
    scope: {
      item: '=navMenuItem'
    },
    link: function(scope, element, attrs, ctrl) {

      var item = scope.item;

      scope.isSubMenu = ctrl.isSubMenu(item);
      scope.isActionMenu = !!item.action;

      scope.onClick = function (e, item) {
        ctrl.onItemClick(item);
      };

      if (ctrl.isSubMenu(item)) {
        element.addClass("dropdown-submenu");
        $compile('<ul nav-menu="item"></ul>')(scope, function(cloned, scope) {
          element.append(cloned);
        });
      }
    },
    template:
      "<li>" +
        "<a href='javascript:' ng-click='onClick($event, item)'>{{item.title}}</a>" +
      "</li>"
  };
}]);

ui.directive('navMenuFav', function() {

  return {
    replace: true,
    controller: ['$scope', '$location', 'DataSource', 'NavService', function ($scope, $location, DataSource, NavService) {

      var ds = DataSource.create("com.axelor.meta.db.MetaMenu", {
        domain: "self.user = :__user__ and self.link is not null"
      });

      $scope.items = [];

      function update() {
        ds.search({
          fields: ["id", "name", "title", "link"],
          sortBy: ["-priority"]
        }).success(function (records, page) {
          $scope.items =  records;
        });
      }

      function add(values, callback) {
        var item = _.findWhere($scope.items, { link: values.link });
        if (item && item.title === values.title) {
          return callback();
        }
        if (item) {
          item.title = values.title;
        } else {
          item = values;
          item.name = values.link;
          item.user = {
            id: axelor.config['user.id']
          };
          item.hidden = true;
        }

        ds.save(item).success(update).then(callback, callback);
      }

      $scope.addFav = function () {

        var link = $location.path();
        if (link === "/") {
          return;
        }

        var tab = NavService.getSelected() || {};
        var vs = tab.$viewScope || {};
        var title = tab.title || (vs.schema || {}).title || "";

        if (vs.record && vs.record.id > 0) {
          title = title + " (" + vs.record.id + ")";
        }

        var item = _.findWhere($scope.items, { link: link });
        if (item) {
          title = item.title;
        }

        var dialog = axelor.dialogs.box("<input type='text' style='width: 100%;box-sizing: border-box;height: 28px;margin: 0;'>", {
          title: _t('Add to favorites...'),
          buttons: [{
            text: _t('Cancel'),
            'class': 'btn btn-default',
            click: function (e) {
              $(this).dialog('close');
            }
          }, {
            text: _t('OK'),
            'class': 'btn btn-primary',
            click: function (e) {
              title = dialog.find("input").val();
              add({ title: title, link: link }, function () {
                dialog.dialog('close');
              });
            }
          }]
        });

        setTimeout(function () {
          dialog.find("input").val(title).focus().select();
        });
      };

      $scope.manageFav = function () {
        NavService.openTabByName('menus.fav');
      };

      function onUpdate(e, _ds) {
        if (ds !== _ds && ds._model === _ds._model) {
          update();
        }
      }

      $scope.$on("ds:saved", onUpdate);
      $scope.$on("ds:removed", onUpdate);

      update();
    }],
    template:
      "<ul class='dropdown-menu'>" +
        "<li><a href='' ng-click='addFav()' x-translate>Add to favorites...</a></li>" +
        "<li class='divider'></li>" +
        "<li ng-repeat='item in items track by item.name'><a ng-href='#{{item.link}}'>{{item.title}}</a></li>" +
        "<li class='divider'></li>" +
        "<li><a href='' ng-click='manageFav()' x-translate>Organize favorites...</a></li>" +
      "</ul>"
  };
});

ui.directive('navMenuTasks', function() {
  return {
    replace: true,
    controller: ['$scope', '$location', 'TagService', 'NavService', function ($scope, $location, TagService, NavService) {

      var TEAM_TASK = "com.axelor.team.db.TeamTask";

      function taskText(count) {
        var n = count || 0;
        if (n <= 0) return _t('no tasks');
        return n > 1 ? _t('{0} tasks', n) : _t('{0} task', n);
      }

      function update(data) {
        var counts = data || {};
        if (counts.current) {
          counts.css = 'badge-primary';
        }
        if (counts.pending) {
          counts.css = 'badge-important';
        }
        counts.currentText = taskText(counts.current);
        counts.pendingText = taskText(counts.pending);
        counts.total = Math.min(99, counts.current);

        $scope.counts = counts;
      }

      TagService.listen(function (data) {
        update(data.tasks || {});
      });

      $scope.showTasks = function (type) {
        NavService.openTabByName('team.tasks.' + type);
      };

      function onDataChange(e, ds) {
        if (ds._model === TEAM_TASK) {
          TagService.find();
        }
      }

      $scope.$on('ds:saved', onDataChange);
      $scope.$on('ds:removed', onDataChange);

      update({});
    }],
    template:
      "<li class='dropdown'>" +
        "<a href='' class='nav-link-tasks dropdown-toggle' data-toggle='dropdown'>" +
          "<i class='fa fa-bell'></i>" +
          "<span class='badge' ng-show='counts.css' ng-class='counts.css'>{{counts.total}}</span>" +
        "</a>" +
        "<ul class='dropdown-menu'>" +
          "<li>" +
            "<a href='' ng-click='showTasks(\"due\")'>" +
              "<span class='nav-link-user-name' x-translate>Tasks due</span>" +
              "<span class='nav-link-user-sub' ng-class='{\"fg-red\": counts.pending > 0}'>{{counts.pendingText}}</span>" +
            "</a>" +
          "</li>" +
          "<li class='divider'></li>" +
          "<li>" +
            "<a href='' ng-click='showTasks((\"todo\"))'>" +
              "<span class='nav-link-user-name' x-translate>Tasks todo</span>" +
              "<span class='nav-link-user-sub'>{{counts.currentText}}</span>" +
            "</a>" +
          "</li>" +
        "</ul>" +
      "</li>"
  };
});

})();
