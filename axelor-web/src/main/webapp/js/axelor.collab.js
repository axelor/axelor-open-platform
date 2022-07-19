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

  const MAX_USERNAME_LENGTH = 30;
  const MAX_SUBTITLE_USERS = 2;

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
            users[user.id].$state = { joinDate: moment() };
          } else if (data.command === 'STATE') {
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

          scope.users = Object.values(users);
          applyStates(scope.users, data.states);
          scope.users.sort((a, b) => (a.$state || {}).joinDate - (b.$state || {}).joinDate);
          scope.message = _t('{0} users', scope.users.length);
          setSubtitle(scope);
        });
      });
    });

    function setSubtitle(scope) {
      scope.subtitle = null;
      scope.subtitleClass = null;

      var user = _.reduce(scope.users, (a, b) => {
        const stateA = a.$state || {};
        const stateB = b.$state || {};
        const versionA = stateA.version || 0;
        const versionB = stateB.version || 0;
        if (versionA > versionB) return a;
        if (versionA < versionB) return b;
        return stateB.versionDate && stateA.versionDate < stateB.versionDate ? b : a;
      });
      if (_.isObject(user) && (user.$state || {}).version > (scope.record || {}).version
        && user.code !== currentUserCode) {
        scope.subtitle = _t('Saved: {0}', getUsersRepr([user]));
        scope.subtitleClass = 'text-error';
        return;
      }

      var users;

      users = scope.users.filter(u => (u.$state || {}).dirty && u.code !== currentUserCode);
      if (!_.isEmpty(users)) {
        scope.subtitle = _t('Dirty: {0}', getUsersRepr(users));
        scope.subtitleClass = 'text-warning';
        return;
      }

      users = scope.users.filter(u => (u.$state || {}).editable && u.code !== currentUserCode);
      if (!_.isEmpty(users)) {
        scope.subtitle = _t('Editing: {0}', getUsersRepr(users));
        scope.subtitleClass = 'text-info';
      }
    }

    function getUsersRepr(users) {
      var names = users.map(user => UserService.getName(user, MAX_USERNAME_LENGTH));
      return getArrayRepr(names, MAX_SUBTITLE_USERS);
    }

    function getArrayRepr(array, maxLength) {
      var text = array.slice(0, maxLength).join(', ');
      return array.length > maxLength ? text + '…' : text;
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
          lastVersion = null;
          lastEditable = false;
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
          }
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
        var currentUser = _.findWhere(scope.users, { code: currentUserCode });
        if (currentUser) {
          _.extend(currentUser.$state, { editable: editable, editableDate: moment() });
        }
      }, WAIT));

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
        }
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

    function getName(user, maxLength) {
      var name = user[userName] || user.name || '?';

      if (maxLength && name.length > maxLength) {
        name = name.slice(0, maxLength) + '…';
      }

      return name;
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
        var locale = ui.getPreferredLocale();

        function formatDate(date) {
          return moment(date).locale(locale).fromNow();
        }

        scope.userText = function (user) {
          var userClass = '';
          var extra;
          var state = user.$state || {};
          var dateKey = _.chain(Object.keys(state))
            .filter(k => _.endsWith(k, 'Date') && state[k.slice(0, -4)])
            .reduce((a, b) => !a || state[a] < state[b] ? b : a, '').value().slice(0, -4);

          if (dateKey === 'version') {
            extra = _.sprintf(_t('saved %s'), formatDate(state.versionDate));
            userClass = 'text-error';
          } else if (dateKey === 'dirty') {
            extra = _.sprintf(_t('dirty since %s'), formatDate(state.dirtyDate));
            userClass = 'text-warning';
          } else if (dateKey === 'editable') {
            extra = _.sprintf(_t('editing since %s'), formatDate(state.editableDate));
            userClass = 'text-info';
          } else {
            extra = _.sprintf(_t('joined %s'), formatDate(state.joinDate));
            userClass = 'text-success';
          }

          return _.sprintf('%s <span class="%s">(%s)</span>',
            UserService.getName(user, MAX_USERNAME_LENGTH), userClass, extra);
        };

        scope.userInitial = function (user) {
          return UserService.getName(user)[0];
        };

        scope.userColor = function (user) {
          return user.$avatar ? null : UserService.getColor(user);
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
      <ul class="nav menu-bar view-collaboration hidden-phone" ng-show="users && users.length > 1">
        <li class="dropdown menu button-menu">
          <a class="dropdown-toggle btn view-collaboration-toggle" data-toggle="dropdown" title="{{ 'Users on this record' | t }}">
            <span class="view-collaboration-users">{{message}}</span>
            <span class="view-collaboration-action" ng-show="subtitle" ng-class="subtitleClass">{{subtitle}}</span>
          </a>
          <ul class="dropdown-menu">
            <li ng-repeat="user in users track by user.id">
              <a href="">
                <span class="avatar" ng-class="userColor(user)" title="{{userName(user)}}">
                  <span ng-if="!user.$avatar">{{userInitial(user)}}</span>
                  <img ng-if='user.$avatar' ng-src='{{user.$avatar}}'>
                </span>
                <span ng-bind-html="userText(user)"></span>
              </a>
            </li>
          </ul>
        </li>
      </ul>
      `
    };
  }]);

})();
