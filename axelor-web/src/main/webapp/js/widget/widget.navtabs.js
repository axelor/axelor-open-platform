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

ui.directive('uiNavTabs', function() {
  return {
    restrict: 'EA',
    replace: true,
    link: function(scope, elem, attrs) {

      if (scope.singleTabOnly) {
        return elem.parent().addClass("view-tabs-single");
      }

      scope.$watch('tabs.length', function navTabsWatch(value, oldValue){
        if (value != oldValue) elem.trigger('adjust:tabs');
      });

      var menu = $();

      setTimeout(function () {

        elem.bsTabs();
        elem.on('contextmenu', '.nav-tabs-main > li > a', showMenu);

        menu = elem.find('#nav-tabs-menu');
        menu.css({
          position: 'absolute',
          zIndex: 1000
        }).hide();
      });

      function showMenu(e) {
        var tabElem = $(e.target).parents('li:first');
        var tabScope = tabElem.data('$scope');
        if (!tabScope || !tabScope.tab) {
          return;
        }

        e.preventDefault();
        e.stopPropagation();

        scope.current = tabScope.tab;
        scope.$timeout(function () {
          var offset = elem.offset();
          menu.show().css({
            left: e.pageX - offset.left,
            top: e.pageY - offset.top
          });
          $(document).on('click.nav-tabs-menu', hideMenu);
        });
      }

      function hideMenu(e) {
        scope.$timeout(function () {
          scope.current = null;
          menu.hide();
        });
        $(document).off('click.nav-tabs-menu');
      }
    },
    templateUrl: 'partials/nav-tabs.html'
  };
});

})();
