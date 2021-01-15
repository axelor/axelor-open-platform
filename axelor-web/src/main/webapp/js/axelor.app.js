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
/**
 * Application Module
 *
 */
(function() {

  "use strict";

  var loadingElem = null,
    loadingTimer = null,
    loadingCounter = 0;

  function updateLoadingCounter(val) {
    loadingCounter += val;
    loadingCounter = Math.max(0, loadingCounter);
  }

  function hideLoading() {
    if (loadingTimer) {
      clearTimeout(loadingTimer);
      loadingTimer = null;
    }
    if (loadingCounter > 0) {
      loadingTimer = _.delay(hideLoading, 500);
      return;
    }
    loadingTimer = _.delay(function () {
      loadingTimer = null;
      if (loadingElem) {
        loadingElem.fadeOut(100);
      }
    }, 500);
  }

  function onHttpStart() {

    updateLoadingCounter(1);

    if (loadingTimer) {
      clearTimeout(loadingTimer);
      loadingTimer = null;
    }

    if (loadingElem === null) {
      loadingElem = $('<div><span class="label label-important loading-counter">' + _t('Loading') + '...</span></div>')
        .css({
          position: 'fixed',
          top: 0,
          width: '100%',
          'text-align': 'center',
          'z-index': 2000
        }).appendTo('body');
    }
    loadingElem.show();
  }

  function onHttpStop() {
    updateLoadingCounter(-1);
    hideLoading();
  }

  axelor.$evalScope = function (scope) {

    var evalScope = scope.$new(true);

    function isValid(name) {
      if (!name) {
        if (_.isFunction(scope.isValid)) {
          return scope.isValid();
        }
        return scope.isValid === undefined || scope.isValid;
      }

      var ctrl = scope.form;
      if (ctrl) {
        ctrl = ctrl[name];
      }
      if (ctrl) {
        return ctrl.$valid;
      }
      return true;
    }

    evalScope.$get = function(n) {
      var context = this.$context || this.record || {};
      if (context.hasOwnProperty(n)) {
        return context[n];
      }
      return evalScope.$eval(n, context);
    };
    evalScope.$moment = function(d) { return moment(d); };		// moment.js helper
    evalScope.$number = function(d) { return +d; };				// number helper
    evalScope.$popup = function() { return scope._isPopup; };	// popup detect
    evalScope.$iif = function(c, t, f) {
      console.warn('Use ternary operator instead of $iif() helper.');
      return c ? t : f;
    };

    evalScope.$sum = function (items, field, operation, field2) {
      var total = 0;
      if (items && items.length) {
        items.forEach(function (item) {
          var value = 0;
          var value2 = 0;
          if (field in item) {
            value = +(item[field] || 0);
          }
          if (operation && field2 && field2 in item) {
            value2 = +(item[field2] || 0);
            switch (operation) {
            case '*':
              value = value * value2;
              break;
            case '/':
              value = value2 ? value / value2 : value;
              break;
            case '+':
              value = value + value2;
              break;
            case '-':
              value = value - value2;
              break;
            }
          }
          if (value) {
            total += value;
          }
        });
      }
      return total;
    };

    // current user and group
    evalScope.$user = axelor.config['user.login'];
    evalScope.$group = axelor.config['user.group'];
    evalScope.$userId = axelor.config['user.id'];

    evalScope.$contains = function(iter, item) {
      if (iter && iter.indexOf)
        return iter.indexOf(item) > -1;
      return false;
    };

    // access json field values
    evalScope.$json = function (name) {
      var value = (scope.record || {})[name];
      if (value) {
        return angular.fromJson(value);
      }
    };

    evalScope.$readonly = scope.isReadonly ? _.bind(scope.isReadonly, scope) : angular.noop;
    evalScope.$required = scope.isRequired ? _.bind(scope.isRequired, scope) : angular.noop;

    evalScope.$valid = function(name) {
      return isValid(scope, name);
    };

    evalScope.$invalid = function(name) {
      return !isValid(scope, name);
    };

    return evalScope;
  };

  axelor.$eval = function (scope, expr, context) {
    if (!scope || !expr) {
      return null;
    }

    var evalScope = axelor.$evalScope(scope);
    evalScope.$context = context;
    try {
      return evalScope.$eval(expr, context);
    } finally {
      evalScope.$destroy();
      evalScope = null;
    }
  };

  axelor.$adjustSize = _.debounce(function () {
    $(document).trigger('adjust:size');
  }, 100);

  var module = angular.module('axelor.app', ['axelor.ng', 'axelor.ds', 'axelor.ui', 'axelor.auth']);

  module.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    var tabResource = {
      action: 'main.tab',
      controller: 'TabCtrl',
      template: "<span><!-- dummy template --></span>"
    };

    $routeProvider

    .when('/preferences', { action: 'preferences' })
    .when('/about', { action: 'about' })
    .when('/system', { action: 'system' })
    .when('/', { action: 'main' })

    .when('/ds/:resource', tabResource)
    .when('/ds/:resource/:mode', tabResource)
    .when('/ds/:resource/:mode/:state', tabResource)

    .otherwise({ redirectTo: '/' });
  }]);

  module.config(['$httpProvider', function(provider) {

    provider.useApplyAsync(true);

    var toString = Object.prototype.toString;

    function isFile(obj) {
      return toString.call(obj) === '[object File]';
    }

    function isFormData(obj) {
      return toString.call(obj) === '[object FormData]';
    }

    function isBlob(obj) {
      return toString.call(obj) === '[object Blob]';
    }

    // restore old behavior
    // breaking change (https://github.com/angular/angular.js/commit/c054288c9722875e3595e6e6162193e0fb67a251)
    function jsonReplacer(key, value) {
      if (typeof key === 'string' && key.charAt(0) === '$') {
        return undefined;
      }
      return value;
    }

    function transformRequest(d) {
      return angular.isObject(d) && !isFile(d) && !isBlob(d) && !isFormData(d) ? JSON.stringify(d, jsonReplacer) : d;
      }

    provider.interceptors.push('httpIndicator');
    provider.defaults.transformRequest.unshift(transformRequest);
    provider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
    provider.defaults.xsrfCookieName = 'CSRF-TOKEN';
    provider.defaults.xsrfHeaderName = 'X-CSRF-Token';
    provider.useApplyAsync(true);
  }]);

  // only enable animation on element with ng-animate css class
  module.config(['$animateProvider', function($animateProvider) {
        $animateProvider.classNameFilter(/x-animate/);
    }]);

  module.factory('httpIndicator', ['$rootScope', '$q', function($rootScope, $q){

    var doc = $(document);
    var body = $('body');
    var blocker = $('<div class="blocker-overlay"></div>')
      .appendTo('body')
      .hide()
      .css({
        position: 'absolute',
        zIndex: 100000,
        width: '100%', height: '100%'
      });

    var spinner = $('<div class="blocker-wait"></div>')
      .append('<div class="blocker-spinner"><i class="fa fa-spinner fa-spin"></div>')
      .append('<div class="blocker-message">' + _t('Please wait...') + '</div>')
      .appendTo(blocker);

    var blocked = false;
    var blockedCounter = 0;
    var blockedTimer = null;
    var spinnerTime = 0;

    function block(callback) {
      if (blocked) return true;
      if (blockedTimer) { clearTimeout(blockedTimer); blockedTimer = null; }
      if (loadingCounter > 0 || blockedCounter > 0) {
        blocked = true;
        doc.on("keydown.blockui mousedown.blockui", function(e) {
          if ($('#loginWindow').is(':visible')) {
            return;
          }
          e.preventDefault();
          e.stopPropagation();
        });
        body.css("cursor", "wait");
        blocker.show();
      }
      unblock(callback);
      return blocked;
    }

    function unblock(callback) {
      if (blockedTimer) { clearTimeout(blockedTimer); blockedTimer = null; }
      if (loadingCounter > 0 || blockedCounter > 0 || loadingTimer) {
        if (spinnerTime === 0) {
          spinnerTime = moment();
        }
        // show spinner after 5 seconds
        if (moment().diff(spinnerTime, "seconds") > 5) {
          blocker.addClass('wait');
        }
        if (blockedCounter > 0) {
          blockedCounter = blockedCounter - 10;
        }
        blockedTimer = _.delay(unblock, 200, callback);
        return;
      }
      doc.off("keydown.blockui mousedown.blockui");
      body.css("cursor", "");
      blocker.removeClass('wait').hide();
      spinnerTime = 0;
      if (callback) {
        callback(blocked);
      }
      blocked = false;
    }

    axelor.blockUI = function(callback, minimum) {
      if (minimum && minimum > blockedCounter) {
        blockedCounter = Math.max(0, blockedCounter);
        blockedCounter = Math.max(minimum, blockedCounter);
      }
      return block(callback);
    };

    axelor.unblockUI = function() {
      return unblock();
    };

    function notSilent(config) {
      return config && !config.silent;
    }

    return {
      request: function(config) {
        if (notSilent(config)) {
          onHttpStart();
        }
        return config;
      },
      response: function(response) {
        if (notSilent(response.config)) {
          onHttpStop();
        }
        if (response.data) {
          if (response.data.status === -1) { // STATUS_FAILURE
            if (notSilent(response.config)) $rootScope.$broadcast('event:http-error', response.data);
            return $q.reject(response);
          }
          if (response.data.status === -7) { // STATUS_LOGIN_REQUIRED
            if (axelor.config['auth.central.client']) {
              // redirect to central login page
              window.location.href = './?client_name=' + axelor.config['auth.central.client']
                + "&hash_location=" + encodeURIComponent(window.location.hash);
            } else if (notSilent(response.config)) {
              // ajax login
              $rootScope.$broadcast('event:auth-loginRequired', response.data);
            }
            return $q.reject(response);
          }
        }
        return response;
      },
      responseError: function(error) {
        if (notSilent(error.config)) {
          onHttpStop();
          $rootScope.$broadcast('event:http-error', error);
        }
        return $q.reject(error);
      }
    };
  }]);

  module.filter('unaccent', function() {
    var source = 'ąàáäâãåæăćčĉęèéëêĝĥìíïîĵłľńňòóöőôõðøśșşšŝťțţŭùúüűûñÿýçżźž';
    var target = 'aaaaaaaaaccceeeeeghiiiijllnnoooooooossssstttuuuuuunyyczzz';

    source += source.toUpperCase();
    target += target.toUpperCase();

    var unaccent = function (text) {
      return typeof(text) !== 'string' ? text : text.replace(/.{1}/g, function(c) {
        var i = source.indexOf(c);
        return i === -1 ? c : target[i];
      });
    };
    return function(input) {
      return unaccent(input);
    };
  });

  module.filter('t', function(){
    return function(input) {
      var t = _t || angular.nop;
      return t(input);
    };
  });

  module.directive('translate', function(){
    return function(scope, element, attrs) {
      var t = _t || angular.nop;
      setTimeout(function(){
        element.html(t(element.text()));
      });
    };
  });

  module.controller('AppCtrl', AppCtrl);

  AppCtrl.$inject = ['$rootScope', '$exceptionHandler', '$scope', '$http', '$route', 'authService', 'MessageService', 'NavService'];
  function AppCtrl($rootScope, $exceptionHandler, $scope, $http, $route, authService, MessageService, NavService) {

    function fetchConfig() {
      return $http.get('ws/app/info').then(function(response) {
        var config = _.extend(axelor.config, response.data);
        $scope.$user.id = config["user.id"];
        $scope.$user.name = config["user.name"];
        $scope.$user.image = config["user.image"];
        config.DEV = config['application.mode'] == 'dev';
        config.PROD = config['application.mode'] == 'prod';

        if (config['view.confirm.yes-no'] === true) {
          _.extend(axelor.dialogs.config, {
            yesNo: true
          });
        }
      });
    }

    function openHomeTab() {
      var path = $scope.routePath;
      var homeAction = axelor.config["user.action"];
      if (!homeAction || _.last(path) !== "main") {
        return;
      }
      NavService.openTabByName(homeAction, {
        __tab_prepend: true,
        __tab_closable: false
      });
    }

    // load app config
    fetchConfig().then(function () {
      openHomeTab();
    });

    $scope.$user = {};
    $scope.$year = moment().year();
    $scope.openHomeTab = openHomeTab;
    $scope.$unreadMailCount = function () {
      return MessageService.unreadCount();
    };

    $scope.showMailBox = function() {
      NavService.openTabByName('mail.inbox');
      $scope.$timeout(function () {
        $scope.$broadcast("on:nav-click", NavService.getSelected());
      });
    };

    $scope.showShortcuts = function () {
      var content = $("<table class='keyboard-shortcuts'>");
      var shortcuts = [
        [[_t('Ctrl'), _t('Insert')], _t('create new record')],
        [[_t('Ctrl'), 'E'], _t('edit selected record')],
        [[_t('Ctrl'), 'S'], _t('save current record')],
        [[_t('Ctrl'), 'D'], _t('delete current/selected record(s)')],
        [[_t('Ctrl'), 'R'], _t('refresh current view')],
        [[_t('Ctrl'), 'Q'], _t('close the current view tab')],
        [[_t('Alt'), 'F'], _t('search for records')],
        [[_t('Alt'), 'G'], _t('focus first or selected item in view')],
        [[_t('Ctrl'), 'J'], _t('navigate to previous page/record')],
        [[_t('Ctrl'), 'K'], _t('navigate to next page/record')],
        [[_t('Ctrl'), 'M'], _t('focus left menu search box')],
        [['F9'], _t('toggle left menu')],
      ];

      shortcuts.forEach(function (item) {
        var keys = item[0];
        var text = item[1];

        var d1 = $("<td class='keys'>").appendTo(content);
        var d2 = $("<td>").appendTo(content).append(text);

        keys.forEach(function (x, i) {
          $("<kbd>").text(x).appendTo(d1);
          if (i < keys.length - 1) {
            d1.append(" + ");
          }
        });

        $("<tr>").append(d1).append(d2).appendTo(content);
      });

      axelor.dialogs.box(content, {
        title: _t("Keyboard Shortcuts")
      });
    };

    axelor.$openHtmlTab = function (url, title) {
      $scope.openTab({
        title: title || url,
        action: "$act" + new Date().getTime(),
        viewType: "html",
        views: [{
          type: "html",
          resource: url
        }]
      });
    };

    var loginAttempts = 0;
    var loginWindow = null;
    var errorWindow = null;

    function showLogin(hide) {

      if (loginWindow === null) {
        loginWindow = $('#loginWindow')
        .attr('title', _t('Log in'))
        .dialog({
          dialogClass: 'no-close ui-dialog-responsive ui-dialog-small',
          autoOpen: false,
          modal: true,
          position: "center",
          width: "auto",
          resizable: false,
          closeOnEscape: false,
          zIndex: 100001,
          show: {
            effect: 'fade',
            duration: 300
          },
          buttons: [{
            text: _t("Log in"),
            'class': 'btn btn-primary',
            click: function(){
              $scope.doLogin();
            }
          }]
        });

        $('#loginWindow input').keypress(function(event){
          if (event.keyCode === 13)
            $scope.doLogin();
        });
      }
      return loginWindow.dialog(hide ? 'close' : 'open').height('auto');
    }

    function showError(hide) {
      if (errorWindow === null) {
        errorWindow = $('#errorWindow')
        .attr('title', _t('Error'))
        .dialog({
          dialogClass: 'ui-dialog-error ui-dialog-responsive',
          draggable: true,
          resizable: false,
          closeOnEscape: true,
          modal: true,
          zIndex: 1100,
          width: 420,
          open: function(e, ui) {
            setTimeout(function () {
              if (errorWindow.dialog('isOpen')) {
                errorWindow.dialog('moveToTop', true);
              }
            }, 300);
          },
          close: function() {
            $scope.httpError = {};
            $scope.$applyAsync();
          },
          show: {
            effect: 'fade',
            duration: 300
          },
          buttons: [{
            text: _t("Show Details"),
            'class': 'btn',
            click: function(){
              var elem = $(this);
              $scope.onErrorWindowShow('stacktrace');
              $scope.$applyAsync(function () {
                setTimeout(function () {
                  var maxHeight = $(document).height() - 132;
                  var height = maxHeight;
                  if (height > elem[0].scrollHeight) {
                    height = elem[0].scrollHeight + 8;
                  }
                  elem.height(height);
                  elem.dialog('option', 'position', 'center');
                  elem.dialog('widget').height(elem.dialog('widget').height());
                }, 100);
              });
            }
          }, {
            text: _t("Close"),
            'class': 'btn btn-primary',
            click: function() {
              errorWindow.dialog('close');
            }
          }]
        });
      }

      return errorWindow
        .dialog(hide ? 'close' : 'open')
        .dialog('widget').css('top', 6)
        .height('auto');
    }

    function showNotification(options) {
      axelor.notify.error('<p>' + options.message.replace('\n', '<br>') + '</p>', {
        title: options.title || options.type || _t('Error')
      });
    }

    $scope.doLogin = function() {

      var data = {
        username: $('#loginWindow form input:first').val(),
        password: $('#loginWindow form input:last').val()
      };

      var last = axelor.config["user.login"];

      $http.post('callback', data).then(function(response){
        authService.loginConfirmed();
        $('#loginWindow form input').val('');
        $('#loginWindow .alert').hide();
        if (last !== data.username) {
          window.location.reload();
        }
      });
    };

    $scope.$on('event:auth-loginRequired', function(event, status) {
      $('#loginWindow .alert').hide();
      showLogin();
      if (loginAttempts++ > 0)
        $('#loginWindow .alert.login-failed').show();
      if (status === 0 || status === 502)
           $('#loginWindow .alert.login-offline').show();
      setTimeout(function(){
        $('#loginWindow input:first').focus();
      }, 300);
    });
    $scope.$on('event:auth-loginConfirmed', function() {
      showLogin(true);
      loginAttempts = 0;
      fetchConfig();
    });

    $scope.httpError = {};
    $scope.$on('event:http-error', function(event, data) {
      var message = _t("Internal Server Error"),
        report = data.data || data, stacktrace = null, cause = null, exception;

      // unauthorized errors are handled separately
      if (data.status === 401) {
        return;
      }

      if (report.popup) {
        message = report.message || _t('A server error occurred. Please contact the administrator.');
        return axelor.dialogs.box(message, {
          title: report.title
        });
      } else if (report.stacktrace) {
        message = report.message || report.string;
        exception = report['class'] || '';

        if (exception.match(/(OptimisticLockException|StaleObjectStateException)/)) {
          message = "<b>" + _t('Concurrent updates error') + '</b><br>' + message;
        }

        stacktrace = report.stacktrace;
        cause = report.cause;
      } else if (report.message) {
        return showNotification(report);
      } else if (_.isString(report)) {
        stacktrace = report.replace(/.*<body>|<\/body>.*/g, '');
      } else {
        return; // no error report, so ignore
      }
      _.extend($scope.httpError, {
        message: message,
        stacktrace: stacktrace,
        cause: cause
      });
      showError();
    });
    $scope.onErrorWindowShow = function(what) {
      $scope.httpError.show = what;
    };

    $scope.$on('$routeChangeSuccess', function(event, current, prev) {

      var route = current.$$route,
        path = route && route.action ? route.action.split('.') : null;

      if (path) {
        $scope.routePath = path;
      }
    });

    $scope.routePath = ["main"];
    $route.reload();
  }

    //trigger adjustSize event on window resize -->
  $(function(){
    $(window).resize(function(event){
      if (!event.isTrigger) {
        $(document).trigger('adjust:size');
      }
      $('body').toggleClass('device-small', axelor.device.small);
      $('body').toggleClass('device-mobile', axelor.device.mobile);
    });
  });

})();
