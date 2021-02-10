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

ui.directive('uiNavTree', ['MenuService', 'TagService', function(MenuService, TagService) {

  return {
    scope: {
      itemClick: "&"
    },
    controller: ["$scope", "$q", function ($scope, $q) {

      var items = [];
      var menus = [];
      var nodes = {};
      var searchItems = [];
      var deferred = $q.defer();

      var handler = $scope.itemClick();

      function canAccept(item) {
        return item.left || item.left === undefined;
      }

      this.onClick = function (e, menu) {
        if (menu.action && (menu.children||[]).length === 0) {
          handler(e, menu);
        }
      };

      this.load = function (data) {
        if (!data || !data.length) return;

        items = data;
        items.forEach(function (item) {
          nodes[item.name] = item;
          item.children = [];
        });

        items.forEach(function (item) {
          var node = nodes[item.parent];
          if (node) {
            node.children.push(item);
          } else if (canAccept(item)){
            menus.push(item);
            item.icon = item.icon || 'fa-bars';
            item.iconBackground = item.iconBackground || 'green';
          }
        });

        var markForSidebar = function (item) {
          item.sidebar = true;
          item.children.forEach(markForSidebar);
        };
        menus.forEach(markForSidebar);

        items.forEach(function (item) {
          if (item.children.length === 0) {
            delete item.children;
            var label = item.title;
            var parent = nodes[item.parent];
            var lastParent;
            while (parent) {
              lastParent = parent;
              parent = nodes[parent.parent];
              if (parent) {
                label = lastParent.title + "/" + label;
              }
            }
            searchItems.push(_.extend({
              title: item.title,
              label: label,
              action: item.action,
              category: lastParent ? lastParent.name : '',
              categoryTitle: lastParent ? lastParent.title : ''
            }));
          }
        });
        $scope.menus = menus;
        $scope.searchItems = searchItems;
        deferred.resolve();
      };

      this.update = function (data) {
        if (!data || data.length === 0) return;
        data.forEach(function (item) {
          var node = nodes[item.name];
          if (node) {
            node.tag = item.tag;
            node.tagStyle = item.tagStyle;
            if (node.tagStyle) {
              node.tagCss = "label-" + node.tagStyle;
            }
          }
        });
      };

      var that =  this;
      TagService.listen(function (data) {
        that.update(data.tags);
      });

      function findProp(node, name) {
        if (node[name]) {
          return node[name];
        }
        var parent = nodes[node.parent];
        if (parent) {
          return findProp(parent, name);
        }
        return null;
      }

      function updateTabStyle(tab) {
        if (tab.icon || tab.fa) {
          return;
        }
        var node = _.findWhere(nodes, { action: tab.action, sidebar: true });
        if (node) {
          tab.icon = tab.icon || findProp(node, 'icon');
          tab.color = tab.color || findProp(node, 'iconBackground');
          if (tab.icon && tab.icon.indexOf('fa') === 0) {
            tab.fa = tab.icon;
            delete tab.icon;
          } else {
            tab.fa = tab.fa || findProp(node, 'fa');
          }
          if (tab.icon) {
            tab.fa = null;
          }
          if (tab.color && tab.color.indexOf('#') != 0) {
            tab.topCss = 'bg-' + tab.color;
            tab.fa = tab.fa ? tab.fa + ' fg-' + tab.color : null;
            tab.color = null;
          }
        }
      }

      MenuService.updateTabStyle = function (tab) {
        deferred.promise.then(function () {
          updateTabStyle(tab);
        });
      };
    }],
    link: function (scope, element, attrs, ctrl) {
      var input = element.find('input');
      scope.showSearch = !!axelor.device.mobile;
      scope.toggleSearch = function (show) {
        input.val('');
        if (!axelor.device.mobile) {
          scope.showSearch = show === undefined ? !scope.showSearch : show;
        }
      };
      scope.onShowSearch = function () {
        scope.showSearch = true;
        setTimeout(function () {
          input.val('').focus();
        });
      };

      input.attr('placeholder', _t('Search...'));
      input.blur(function (e) {
        scope.$timeout(function () {
          scope.toggleSearch(false);
        });
      });
      input.keydown(function (e) {
        if (e.keyCode ===  27) { // escape
          scope.$timeout(function () {
            scope.toggleSearch(false);
          });
        }
      });

      function search(request, response) {
        var term = request.term;
        var items = _.filter(scope.searchItems, function (item) {
          var text = item.categoryTitle + '/' + item.label;
          var search = term;
          if (search[0] === '/') {
            search = search.substring(1);
            text = item.title;
          }
          text = text.replace('/', '').toLowerCase();
          if (search[0] === '"' || search[0] === '=') {
            search = search.substring(1);
            if (search.indexOf('"') === search.length - 1) {
              search = search.substring(0, search.length - 1);
            }
            return text.indexOf(search) > -1;
          }
          var parts = search.toLowerCase().split(/\s+/);
          for (var i = 0; i < parts.length; i++) {
            if (text.indexOf(parts[i]) === -1) {
              return false;
            }
          }
          return parts.length > 0;
        });
        response(items);
      }

      MenuService.all().success(function (res) {
        ctrl.load(res.data);
        input.autocomplete({
          source: search,
          select: function (e, ui) {
            ctrl.onClick(e, ui.item);
            scope.$timeout(function () {
              scope.toggleSearch(false);
            });
          },
          appendTo: element.parent(),
          open: function () {
            element.children('.nav-tree').hide();
          },
          close: function (e) {
            element.children('.nav-tree').show();
          }
        });

        input.data('autocomplete')._renderMenu = function (ul, items) {
          var all = _.groupBy(items, 'category');
          var that = this;
          scope.menus.forEach(function (menu) {
            var found = all[menu.name];
            if (found) {
              ul.append($("<li class='ui-menu-category'>").html(menu.title));
              found.forEach(function (item) {
                that._renderItemData(ul, item);
              });
            }
          });
        };
      });
    },
    replace: true,
    template:
      "<div>" +
        "<div class='nav-search-toggle' ng-show='!showSearch'>" +
          "<i ng-click='onShowSearch()' class='fa fa-angle-down'></i>" +
        "</div>" +
        "<div class='nav-search' ng-show='showSearch'>" +
          "<input type='text'>" +
        "</div>" +
        "<ul class='nav nav-tree'>" +
          "<li ng-repeat='menu in menus track by menu.name' ui-nav-sub-tree x-menu='menu'></li>" +
        "</ul>" +
      "</div>"
  };
}]);

ui.directive('uiNavSubTree', ['$compile', function ($compile) {

  return {
    scope: {
      menu: "="
    },
    require: "^uiNavTree",
    link: function (scope, element, attrs, ctrl) {
      var menu = scope.menu;
      if (menu.icon && menu.icon.indexOf('fa') === 0) {
        menu.fa = menu.icon;
        delete menu.icon;
      }
      if (menu.tagStyle) {
        menu.tagCss = "label-" + menu.tagStyle;
      }
      if (menu.children) {
        var sub = $(
          "<ul class='nav ui-nav-sub-tree'>" +
            "<li ng-repeat='child in menu.children track by child.name' ui-nav-sub-tree x-menu='child'></li>" +
          "</ul>");
        sub = $compile(sub)(scope);
        sub.appendTo(element);
      }

      setTimeout(function () {
        var icon = element.find("span.nav-icon:first");
        if (menu.iconBackground && icon.length > 0) {
          var cssName = menu.parent ? 'color' : 'background-color';
          var clsName = menu.parent ? 'fg-' : 'bg-';

          if (!menu.parent) {
            icon.addClass("fg-white");
          }

          if (menu.iconBackground.indexOf("#") === 0) {
            icon.css(cssName, menu.iconBackground);
          } else {
            icon.addClass(clsName + menu.iconBackground);
          }

          // get computed color value
          var color = icon.css(cssName);
          var bright = d3.rgb(color).brighter(.3).toString();

          // add hover effect
          element.hover(function () {
            icon.css(cssName, bright);
          }, function () {
            icon.css(cssName, color);
          });

          // use same color for vertical line
          if (!menu.parent) {
            element.css("border-left-color", color);
            element.hover(function () {
              element.css("border-left-color", color);
            }, function () {
              element.css("border-left-color", bright);
            });
          }
        }
      });

      var animation = false;

      function show(el) {

        var parent = el.parent("li");

        if (animation || parent.hasClass('open')) {
          return;
        }

        function done() {
          parent.addClass('open');
          parent.removeClass('animate');
          el.height('');
          animation = false;
        }

        hide(parent.siblings("li.open").children('ul'));

        animation = true;

        parent.addClass('animate');
        el.height(el[0].scrollHeight);

        setTimeout(done, 300);
      }

      function hide(el) {

        var parent = el.parent("li");

        if (animation || !parent.hasClass('open')) {
          return;
        }

        function done() {
          parent.removeClass('open');
          parent.removeClass('animate');
          animation = false;
        }

        animation = true;

        el.height(el.height())[0].offsetHeight;
        parent.addClass('animate');
        el.height(0);

        setTimeout(done, 300);
      }

      element.on('click', '> a', function (e) {
        e.preventDefault();

        if (animation) return;

        var $list = element.children('ul');

        element.parents('.nav-tree').find('li.active').not(element).removeClass('active');
        element.addClass('active');

        if (menu.action && (menu.children||[]).length === 0) {
          scope.$applyAsync(function () {
            ctrl.onClick(e, menu);
          });
        }
        if ($list.length === 0) return;
        if (element.hasClass('open')) {
          hide($list);
        } else {
          show($list);
        }
      });

      if (menu.help) {
        setTimeout(function () {
          var tooltip = element.children('a')
          .addClass('has-help')
          .tooltip({
            html: true,
            title: menu.help,
            placement: 'right',
            delay: { show: 500, hide: 100 },
            container: 'body'
          });
        });
      }
    },
    replace: true,
    template:
      "<li ng-class='{folder: menu.children, tagged: menu.hasTag }' data-name='{{::menu.name}}'>" +
        "<a href='#'>" +
          "<img class='nav-image' ng-if='::menu.icon' ng-src='{{::menu.icon}}'></img>" +
          "<span class='nav-icon' ng-if='::menu.fa'><i class='fa' ng-class='::menu.fa'></i></span>" +
          "<span class='nav-label'>" +
            "<span class='nav-title'>{{::menu.title}}</span>" +
            "<span ng-show='menu.tag' ng-class='menu.tagCss' class='nav-tag label'>{{menu.tag}}</span>" +
          "</span>" +
        "</a>" +
      "</li>"
  };
}]);

})();
