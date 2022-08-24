/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/*jshint esversion: 6 */
(function () {

  "use strict";

  var ds = angular.module('axelor.ds');

  ds.factory('UserService', [function () {
    var userName = axelor.config['user.nameField'] || 'name';

    function getName(user) {
      if (!user) return null;
      return user[userName] || user.name || '?';
    }

    var userColors = {};
    var usedColors = [];
    var colorNames = [
      'blue',
      'green',
      'red',
      'orange',
      'yellow',
      'olive',
      'teal',
      'violet',
      'purple',
      'pink',
      'brown'
    ];

    function getColor(user) {
      if (!user) return null;
      if (userColors[user.code]) {
        return userColors[user.code];
      }
      if (usedColors.length === colorNames.length) {
        usedColors = [];
      }
      var color = _.find(colorNames, function (n) {
        return usedColors.indexOf(n) === -1;
      });
      usedColors.push(color);
      var bgColor = 'bg-' + color;
      userColors[user.code] = bgColor;
      return bgColor;
    }

    var allowedUrls = new Map();
    var allowedUrlsMaxSize = 1000;
    var fetchingUrls = {};

    function trimMap(map, maxSize) {
      if (map.size <= maxSize) return;
      const it = map.keys();
      const half = maxSize / 2;
      while (map.size > half) {
        map.delete(it.next().value);
      }
    }

    function checkUrl(url, onAllowed, onForbidden) {
      trimMap(allowedUrls, allowedUrlsMaxSize);

      onAllowed = onAllowed || angular.noop;
      onForbidden = onForbidden || angular.noop;

      if (!url) {
        onForbidden(url);
        return;
      }

      var perm = allowedUrls.get(url);
      if (perm !== undefined) {
        if (perm) {
          onAllowed(url);
        } else {
          onForbidden(url);
        }
        return;
      }

      var fetchingUrl = fetchingUrls[url];
      if (fetchingUrl) {
        fetchingUrl.then(data => {
          if (data.status < 400) {
            onAllowed(url);
          } else {
            onForbidden(url);
          }
        });
        return;
      }

      fetchingUrls[url] = fetch(url, { method: 'HEAD' }).then(data => {
        if (data.status < 400) {
          allowedUrls.set(url, true);
          onAllowed(url);
        } else {
          allowedUrls.set(url, false);
          onForbidden(url);
        }
        return data;
      }).catch(error => {
        console.error(error);
      }).finally(() => {
        delete fetchingUrls[url];
      });
    }

    return {
      getName: getName,
      getColor: getColor,
      checkUrl: checkUrl
    };
  }]);

})();
