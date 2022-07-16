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
(function () {

  "use strict";

  var ds = angular.module('axelor.ds');
  var ui = angular.module('axelor.ui');

  ds.factory('CollaborationService', ['$rootScope', 'Socket', 'UserService', function ($rootScope, Socket, UserService) {
    var functions = {
      register: angular.noop
    };

    if (axelor.config['view.collaboration'] === false) {
      return functions;
    }

    var allScopes = {};
    var allUsers = {};
    var rejoins = {};

    function onOpen() {
      _.each(rejoins, function (rejoin) {
        rejoin();
      })
    }

    var channel = Socket("collaboration", { onopen: onOpen });

    function getKey(model, recordId) {
      return model + ':' + recordId;
    }

    var currentUserCode = axelor.config['user.login'];

    var unsubscribe = channel.subscribe(function (message) {
      var key = getKey(message.model, message.recordId);
      var scopes = allScopes[key];

      if (_.isEmpty(scopes)) {
        return;
      }

      _.each(scopes, function (scope) {
        scope.$apply(function () {
          if (message.model !== scope._model || message.recordId !== (scope.record || {}).id) {
            return;
          }

          var users = allUsers[key];

          if (!users) {
            allUsers[key] = users = {};
          }

          var data = message;
          var user = data.user;

          if (data.users) {
            _.each(data.users, u => users[u.id] = u);
          }


          if (data.command === 'LEFT') {
            delete users[user.id];
          } else if (data.command === 'JOIN') {
            users[user.id] = user || {};
          } else if (data.command === 'STATE') {
            _.extend(users[user.id], user || {});
            users[user.id].$state = _.extend(users[user.id].$state || {}, data.message);
          }

          scope.users = Object.values(users);
          applyStates(scope.users, data.states);
          scope.message = _t('{0} users viewing', scope.users.length);
          setSubtitle(scope);
        });
      });
    });

    // Show third-party activity in subtitle
    function setSubtitle(scope) {
      var user = _.max(scope.users, u => (u.$state || {}).version);
      if (_.isObject(user) && (user.$state || {}).version > 0 && currentUserCode !== user.code) {
        scope.subtitle = _t('Saved: {0}', getUsersRepr([user]));
        return;
      }

      var users;

      users = scope.users.filter(u => (u.$state || {}).dirty && currentUserCode !== u.code);
      if (!_.isEmpty(users)) {
        scope.subtitle = _t('Pending changes: {0}', getUsersRepr(users));
        return;
      }

      users = scope.users.filter(u => (u.$state || {}).editable && currentUserCode !== u.code);
      if (!_.isEmpty(users)) {
        scope.subtitle = _t('Edit: {0}', getUsersRepr(users));
        return;
      }

      scope.subtitle = null;
    }

    function getUsersRepr(users) {
      var names = users.map(user => UserService.getName(user));
      return getArrayRepr(names, 1);
    }

    function getArrayRepr(array, maxLength) {
      var text = array.slice(0, maxLength).join(', ');
      return array.length > maxLength ? text + '…' : text;
    }

    function applyStates(users, states) {
      if (_.isEmpty(states)) return;
      _.each(users, user => {
        var state = states[user.code] || {};
        user.$state = state;
      });
    }

    functions.register = function (scope) {
      var model = scope._model;
      var recordId = null;

      var rejoin = function () {
        if (recordId) {
          channel.send({
            command: 'JOIN', model: model, recordId: recordId,
            message: { editable: scope.isEditable(), dirty: scope.isDirty() }
          });
        }
      }

      var join = function (id) {
        recordId = id;
        if (id) {
          var key = getKey(scope._model, id);
          rejoins[key] = rejoin;
          var scopes = allScopes[key];
          if (!scopes) {
            scopes = allScopes[key] = [];
          }
          var index = scopes.indexOf(scope);
          if (index >= 0) {
            console.error("Already joined: " + key);
            return;
          }
          scopes.push(scope)
          allUsers[key] = {};

          var message;
          if (scope.isEditable()) message = _.extend(message || {}, { editable: true });
          if (scope.isDirty()) message = _.extend(message || {}, { dirty: true });

          channel.send({ command: 'JOIN', model: model, recordId: id, message: message });
          scope.users = [];
        }
      };

      var leave = function (id) {
        if (id) {
          var key = getKey(scope._model, id);
          delete rejoins[key];
          var scopes = allScopes[key];
          var index = scopes.indexOf(scope);
          if (index >= 0) {
            scopes.splice(index, 1);
            channel.send({ command: 'LEFT', model: model, recordId: id });
          }
          if (_.isEmpty(scopes)) {
            delete allUsers[key];
            delete allScopes[key];
          }
          recordId = null;
          scope.users = [];
        }
      };

      var unwatchId = scope.$watch('record.id', function (id, old) {
        if (id === old) return;
        leave(recordId);
        if (id > 0) {
          join(id);
        }
      });

      var lastVersion;
      var unwatchVersion = scope.$watch('record.version', function (version) {
        if (!version) return;
        if (lastVersion) {
          channel.send({
            command: 'STATE', model: model, recordId: recordId,
            message: { version: version }
          });
        }
        lastVersion = version;
      });

      var lastEditable = false;
      var unwatchEditable = scope.$watch('isEditable()', _.throttle(function (editable) {
        if (recordId == null || lastEditable === editable) return;
        lastEditable = editable;
        channel.send({
          command: 'STATE', model: model, recordId: recordId,
          message: { editable: editable }
        });
      }, WAIT));

      const WAIT = 500;
      var lastDirty = false;
      var unwatchDirty = scope.$watch('isDirty()', _.throttle(function (dirty) {
        if (recordId == null || lastDirty === dirty) return;
        lastDirty = dirty;
        channel.send({
          command: 'STATE', model: model, recordId: recordId,
          message: { dirty: dirty }
        });
      }, WAIT));

      scope.$on('$destroy', function () {
        unwatchId();
        unwatchVersion();
        unwatchEditable();
        unwatchDirty();
        leave(recordId);
      });

      $rootScope.$on('$destroy', unsubscribe);
    }

    return functions;
  }]);


  ds.factory('UserService', [function () {
    var userName = axelor.config['user.nameField'] || 'name';

    function getName(user) {
      return user[userName] || user.name;
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
        })
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
    }
  }]);

  ui.directive('uiViewCollaboration', ['CollaborationService', 'UserService', function (CollaborationService, UserService) {
    return {
      scope: true,
      replace: true,
      link: function (scope) {
        CollaborationService.register(scope);

        scope.userName = function (user) {
          return UserService.getName(user);
        };

        scope.userColor = function (user) {
          if (user.$avatar) {
            return null;
          }
          return UserService.getColor(user);
        };

        scope.$watch('users', function (users) {
          _.each(users, user => {
            if (user.$avatar) {
              var url = user.$avatar;
              delete user.$avatar;
              UserService.checkUrl(url, () => user.$avatar = url);
            }
          });
        });
      },
      template: `
      <ul class="nav menu-bar view-collaboration hidden-phone" ng-show="users.length &gt; 1">
        <li class="dropdown menu button-menu">
          <a class="dropdown-toggle btn view-collaboration-toggle" data-toggle="dropdown" title="{{ 'Watchers' | t}}">
            <span class="view-collaboration-users">{{message}}</span>
            <span class="view-collaboration-action" ng-show="subtitle">{{subtitle}}</span>
          </a>
          <ul class="dropdown-menu">
            <li ng-repeat="user in users track by user.id">
              <a href="">
                <span class="avatar" ng-class="userColor(user)" title="{{userName(user)}}">
                  <span ng-if="!user.$avatar">{{userName(user)[0]}}</span>
                  <img ng-if='user.$avatar' ng-src='{{user.$avatar}}' width='20px'>
                </span>
                {{userName(user)}}
              </a>
            </li>
          </ul>
        </li>
      </ul>
      `
    };
  }]);

})();
