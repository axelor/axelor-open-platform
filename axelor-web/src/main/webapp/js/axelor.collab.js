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
            processLeft(user, users, scope);
          } else if (data.command === 'JOIN') {
            processJoin(user, users);
          } else if (data.command === 'STATE') {
            processState(user, users, scope, data);
          }

          scope.users = Object.values(users);
          applyStates(scope.users, data.states);
          scope.users.sort((a, b) => (a.$state || {}).joinDate - (b.$state || {}).joinDate);
          setInfo(scope);
        });
      });
    });

    function processLeft(user, users, scope) {
      user = users[user.id];
      const state = (user || {}).$state || {};
      const recordVersion = (scope.record || {}).version;

      // Keep user if they saved the record.
      if (state.version > recordVersion) {
        state.leftDate = moment();
        return;
      }

      delete users[user.id];
    }

    function processJoin(user, users) {
      let state = (users[user.id] || {}).$state;
      if (state) {
        delete state.leftDate;
      } else {
        users[user.id] = user || {};
        state = users[user.id].$state = {};
      }
      _.extend(state, { joinDate: moment() });
    }

    function processState(user, users, scope, data) {
      _.extend(users[user.id], user || {});
      var msg = _.extend({}, data.message);
      if (msg.version != null && msg.dirty === undefined) {
        msg.dirty = false;
      }
      if (msg.version <= (scope.record || {}).version) {
        delete msg.version;
        delete msg.versionDate;
      }
      _.chain(Object.keys(msg)).filter(k => !_.endsWith(k, 'Date')).each(k => {
        var dateKey = k + 'Date';
        var dateValue = msg[dateKey];
        msg[dateKey] = dateValue ? moment(dateValue) : moment();
      });
      users[user.id].$state = _.extend(users[user.id].$state || {}, msg);
    }

    function setInfo(scope) {
      scope.message = _t('{0} users', scope.users.length);
      scope.subtitle = null;
      scope.subtitleClass = null;
      scope.subtitleTooltip = null;
      scope.tooltip = _t("{0} users on this record", (scope.users || []).length);

      var recordVersion = (scope.record || {}).version;

      var saveUser = _.reduce(scope.users, (a, b) => {
        const stateA = (a || {}).$state || {};
        const stateB = (b || {}).$state || {};
        const versionA = stateA.version || 0;
        const versionB = stateB.version || 0;
        if (versionA > versionB) return a;
        if (versionA < versionB) return b;
        return stateB.versionDate && stateA.versionDate < stateB.versionDate ? b : a;
      }, null);
      if (_.isObject(saveUser) && (saveUser.$state || {}).version > recordVersion
        && saveUser.code !== currentUserCode) {
        scope.subtitle = '<i class="fa fa-floppy-o"/> ' + getUsersRepr([saveUser]);
        scope.subtitleClass = 'text-error';
        scope.subtitleTooltip = _t("Saved {0}", formatDate((saveUser.$state || {}).versionDate));
        return;
      }

      var dirtyUsers = scope.users.filter(u => (u.$state || {}).dirty
        && ((u.$state || {}).version == null || (u.$state || {}).version <= recordVersion)
        && u.code !== currentUserCode);
      if (!_.isEmpty(dirtyUsers)) {
        scope.subtitle = '<i class="fa fa-pencil"/> ' + getUsersRepr(dirtyUsers);
        scope.subtitleClass = 'text-warning';
        scope.subtitleTooltip = _t("Editing since {0}", formatDate((dirtyUsers[0].$state || {}).dirtyDate));
      }
    }

    var locale = ui.getPreferredLocale();

    function formatDate(date) {
      return moment(date).locale(locale).fromNow();
    }

    function getUsersRepr(users) {
      var names = users.map(user => UserService.getName(user));
      return names.join(', ');
    }

    function applyStates(users, states) {
      if (_.isEmpty(states)) return;
      _.each(users, user => {
        var state = states[user.code] || {};
        _.chain(Object.keys(state)).filter(k => _.endsWith(k, 'Date')).each(k => {
          state[k] = moment(state[k]);
        });
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
            message: { dirty: scope.isDirty() }
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
            console.error(`Already joined: ${key}`);
            return;
          }
          scopes.push(scope)
          allUsers[key] = {};

          var message;
          if (scope.isDirty()) {
            message = _.extend(message || {}, { dirty: true });
          }

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
          lastVersion = null;
          lastDirty = false;
        }
      };

      var unwatchId = scope.$watch('record.id', function (id, old) {
        if (id === old) return;
        leave(recordId);
        if (id > 0) {
          join(id);
        }
      });

      var lastVersion = null;
      var unwatchVersion = scope.$watch('record.version', function (version) {
        if (version == null) return;
        if (lastVersion != null) {
          lastDirty = false;
          channel.send({
            command: 'STATE', model: model, recordId: recordId,
            message: { version: version }
          });
          var currentUser = _.findWhere(scope.users, { code: currentUserCode });
          if (currentUser) {
            var now = moment();
            _.extend(currentUser.$state, {
              version: version, versionDate: now,
              dirty: lastDirty, dirtyDate: now,
            });
            removeLeftUsers();
          }
        }
        lastVersion = version;
        if (attrsReset) {
          attrsReset = false;
          removeLeftUsers();
        }
      });

      function removeLeftUsers() {
        const recordVersion = (scope.record || {}).version;
        const users = _.filter(scope.users, user => {
          if (!user) return false;
          const state = user.$state || {};
          if (!state.leftDate) return true;
          return state.version > recordVersion;
        });
        const changed = (scope.users || []).length != users.length;
        scope.users = users;
        if (changed) {
          setInfo(scope);
        }
      }

      const WAIT = 500;
      var lastDirty = false;
      var unwatchDirty = scope.$watch('isDirty()', _.throttle(function (dirty) {
        if (recordId == null || lastDirty === dirty
          || lastVersion != (scope.record || {}).version) return;
        lastDirty = dirty;
        channel.send({
          command: 'STATE', model: model, recordId: recordId,
          message: { dirty: dirty }
        });
        var currentUser = _.findWhere(scope.users, { code: currentUserCode });
        if (currentUser) {
          _.extend(currentUser.$state, { dirty: dirty, dirtyDate: moment() });
          scope.$emit('collaboration-users-updated', scope.users);
        }
      }, WAIT));

      var attrsReset = false;
      scope.$on('on:attrs-reset', function () {
        if (recordId == null) return;
        var currentUser = _.findWhere(scope.users, { code: currentUserCode });
        if (!currentUser) return;
        attrsReset = true;
        lastVersion = null;
        var state = currentUser.$state;
        if (!state) return;
        if (state.dirty) {
          state.dirty = false;
          channel.send({
            command: 'STATE', model: model, recordId: recordId,
            message: { dirty: state.dirty }
          });
        }
      });

      scope.$on('$destroy', function () {
        unwatchId();
        unwatchVersion();
        unwatchDirty();
        leave(recordId);
      });

      $rootScope.$on('$destroy', unsubscribe);
      return true;
    }

    return functions;
  }]);


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
      link: function (scope) {
        if (!CollaborationService.register(scope)
          || axelor.config['user.canViewCollaboration'] === false) {
          return;
        }

        scope.enabled = true;

        var currentUserCode = axelor.config['user.login'];
        var locale = ui.getPreferredLocale();

        function formatDate(date) {
          return moment(date).locale(locale).fromNow();
        }

        var computeUserData = function (user) {
          let dateKey;
          const state = user.$state || {};
          const recordVersion = (scope.record || {}).version;

          if (state.leftDate) {
            dateKey = 'left';
          } else if (state.version > recordVersion) {
            dateKey = 'version';
          } else if (state.dirty && (state.version == null || state.version <= recordVersion)) {
            dateKey = 'dirty';
          }

          if (dateKey === 'left') {
            user.$stateIcon = 'fa-sign-out text-error'
            user.$tooltip = _t('Saved and left {0}', formatDate(state.leftDate));
          } else if (dateKey === 'version') {
            user.$stateIcon = 'fa-floppy-o text-error';
            user.$tooltip = _t('Saved {0}', formatDate(state.versionDate));
          } else if (dateKey === 'dirty') {
            user.$stateIcon = 'fa-pencil text-warning';
            user.$tooltip = _t('Editing since {0}', formatDate(state.dirtyDate));
          } else {
            user.$stateIcon = 'fa-file-text-o text-success';
            user.$tooltip = _t('Joined {0}', formatDate(state.joinDate));
          }
        };

        scope.userName = function (user) {
          return UserService.getName(user);
        };

        scope.userNameOrMe = function (user) {
          return (user || {}).code === currentUserCode ? _t("Me") : UserService.getName(user);
        };

        scope.userNameStyle = function (user) {
          return (user || {}).code === currentUserCode ? 'self-collaboration-user' : null;
        }

        scope.userInitial = function (user) {
          return UserService.getName(user)[0];
        };

        scope.userColor = function (user) {
          return user.$avatar ? null : UserService.getColor(user);
        };

        scope.$watch('users', function (users) {
          _.each(users, user => {
            computeUserData(user);
            if (user.$avatar) {
              var url = user.$avatar;
              delete user.$avatar;
              UserService.checkUrl(url, () => user.$avatar = url);
            }
          });
        });

        scope.$on('collaboration-users-updated', function () {
          _.each(scope.users, user => computeUserData(user));
        })
      },
      template: `
      <ul ng-if="enabled" class="nav menu-bar view-collaboration hidden-phone" ng-show="users && users.length > 1">
        <li class="dropdown menu button-menu">
          <a class="dropdown-toggle btn view-collaboration-toggle" data-toggle="dropdown" title="{{tooltip}}">
            <span class="view-collaboration-users">{{message}}</span>
            <span class="view-collaboration-action" ng-show="subtitle" ng-class="subtitleClass" ng-bind-html="subtitle" title="{{subtitleTooltip}}"/>
          </a>
          <ul class="dropdown-menu pull-right">
            <li ng-repeat="user in users track by user.id" title={{user.$tooltip}} class="view-collaboration-user">
              <a href="">
                <i class="view-collaboration-state fa" ng-class="user.$stateIcon"/>
                <span class="avatar" ng-class="userColor(user)" title="{{::userName(user)}}">
                  <span ng-if="!user.$avatar">{{::userInitial(user)}}</span>
                  <img ng-if='user.$avatar' ng-src='{{user.$avatar}}' alt="{{::userName(user)}}">
                </span>
                <span ng-bind="::userNameOrMe(user)" ng-class="::userNameStyle(user)"/>
              </a>
            </li>
          </ul>
        </li>
      </ul>
      `
    };
  }]);

})();
