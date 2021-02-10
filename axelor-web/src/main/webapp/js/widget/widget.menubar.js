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

MenuBarCtrl.$inject = ['$scope', '$element'];
function MenuBarCtrl($scope, $element) {

  this.isDivider = function(item) {
    return !item.title && !item.icon;
  };

  this.isSubMenu = function(item) {
    return item && item.items && item.items.length > 0;
  };

  $scope.isImage = function (menu) {
    return menu.icon && menu.icon.indexOf('fa-') !== 0;
  };

  $scope.isIcon = function (menu) {
    return menu.icon && menu.icon.indexOf('fa-') === 0;
  };

  $scope.canShowTitle = function(menu) {
    return menu.showTitle === null || menu.showTitle === undefined || menu.showTitle;
  };
}

ui.directive('uiMenuBar', function() {

  return {
    replace: true,
    controller: MenuBarCtrl,
    scope: {
      menus: '=',
      handler: '='
    },
    link: function(scope, element, attrs, ctrl) {

      ctrl.handler = scope.handler;

      scope.onMenuClick = _.once(function onMenuClick(e) {
        element.find('.dropdown-toggle').dropdown();
        $(e.currentTarget).dropdown('toggle');
      });
    },

    template:
      "<ul class='nav menu-bar'>" +
        "<li class='menu dropdown button-menu' ng-class='::{\"button-menu\": menu.isButton, \"has-icon\": menu.icon}' ng-repeat='menu in ::menus'>" +
          "<a href='' class='dropdown-toggle btn' ng-class='::{\"btn\": menu.isButton}' data-toggle='dropdown' ng-click='onMenuClick($event)'>" +
            "<img ng-if='::isImage(menu)' ng-src='{{menu.icon}}'> " +
            "<i class='fa {{::menu.icon}}' ng-if='::isIcon(menu)'></i> " +
            "<span class='menu-title' ng-show='::canShowTitle(menu)'>{{::menu.title}}</span> " +
            "<b class='caret'></b>" +
          "</a>" +
          "<ul ui-menu='menu'></ul>" +
        "</li>" +
      "</ul>"
  };
});

ui.directive('uiMenu', function() {

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
        "<li ng-repeat='item in ::menu.items' ui-menu-item='item'>" +
      "</ul>"
  };
});

ui.directive('uiMenuItem', ['$compile', 'ActionService', function($compile, ActionService) {

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
      scope.isSubMenu = ctrl.isSubMenu(item);

      if (item.action) {
        handler = ActionService.handler(ctrl.handler, element, {
          action: item.action,
          prompt: item.prompt
        });
        element.addClass("action-item").attr("x-name", item.name);
      }

      scope.isRequired = function(){};
      scope.isValid = function(){};

      attrs = {
        hidden: !!item.hidden,
        readonly: !!item.readonly
      };

      scope.attr = function(name, value) {
        attrs[name] = value;
      };

      scope.isReadonly = function(){
        if (attrs.readonly) return true;
        if (_.isFunction(item.active)) {
          return !item.active();
        }
        return false;
      };

      scope.isHidden = function(){
        if (attrs.hidden) return true;
        if (_.isFunction(item.visible)) {
          return !item.visible();
        }
        return false;
      };

      var form = element.parents('.form-view:first');
      var formScope = form.data('$scope');

      if (formScope) {
        formScope.$watch('record', function menubarRecordWatch(rec) {
          scope.record = rec;
        });
      }

      scope.onClick = function(e) {
        element.parents('.dropdown').dropdown('toggle');
        if (scope.isSubMenu) return;
        if (item.action) {
          return handler.onClick();
        }
        if (_.isFunction(item.click)) {
          return item.click(e);
        }
      };

      scope.cssClass = function() {
        if (scope.isDivider) {
          return 'divider';
        }
        if (scope.isSubMenu) {
          return 'dropdown-submenu';
        }
      };


      if (scope.isSubMenu) {
        $compile('<ul ui-menu="item"></ul>')(scope, function(cloned, scope) {
          element.append(cloned);
        });
      }
    },
    template:
      "<li ng-class='cssClass()' ui-widget-states	ng-show='!isHidden()'>" +
        "<a href='' ng-show='isReadonly()' class='disabled'>{{item.title}}</a>" +
        "<a href='' ng-show='!isDivider && !isReadonly()' ng-click='onClick($event)'>{{item.title}}</a>" +
      "</li>"
  };
}]);

ui.directive('uiToolbarAdjust', function() {

  return function (scope, element, attrs) {

    var elemMenubar = null;
    var elemToolbar = null;
    var elemSiblings = null;

    var elemToolbarMobile = null;

    function setup() {
      elemMenubar = element.children('.view-menubar');
      elemToolbar = element.children('.view-toolbar');
      elemSiblings = element.children(':not(.view-menubar,.view-toolbar,.view-toolbar-mobile)');

      elemToolbarMobile = element.children('.view-toolbar-mobile').hide();

      var running = false;
      scope.$onAdjust(function () {
        if (running) {
          return;
        }
        running = true;
        try {
          adjust();
        } finally {
          running = false;
        }
      });

      scope.$callWhen(function () {
        return element.is(':visible');
      }, adjust);
    }

    function hideAndShow(first, second, visibility) {
      [elemMenubar, elemToolbar, elemToolbarMobile].forEach(function (elem) {
        elem.hide().css('visibility', 'hidden');
      });
      [first, second].forEach(function (elem) {
        if (elem && element.is(':visible')) {
          elem.show().css('visibility', visibility || '');
        }
      });
    }

    function adjust() {

      if (elemMenubar === null) {
        return;
      }

      var width = element.width() - 8;
      elemSiblings.each(function (i) {
        width -= $(this).width();
      });

      if (axelor.device.small) {
        if (width > elemToolbarMobile.width() + elemMenubar.width()) {
          hideAndShow(elemToolbarMobile, elemMenubar);
        } else if (width > elemToolbarMobile.width()) {
          hideAndShow(elemToolbarMobile);
        } else if (width > elemMenubar.width()) {
          hideAndShow(elemMenubar);
        }
        return;
      }

      function canShow(first, second) {
        hideAndShow(first, second, 'hidden');
        if (width > first.width() + second.width()) {
          first.css('visibility', '');
          second.css('visibility', '');
          return true;
        }
        return false;
      }

      canShow(elemToolbar, elemMenubar) ||
      canShow(elemToolbarMobile, elemMenubar) ||
      canShow(elemToolbar, $()) ||
      canShow(elemToolbarMobile, $());
    }

    scope.waitForActions(setup, 100);
  };
});

})();
