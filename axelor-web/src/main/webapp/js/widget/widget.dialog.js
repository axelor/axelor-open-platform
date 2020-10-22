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

ui.directive('uiDialog', function() {
  return {
    restrict: 'EA',
    link: function(scope, element, attrs) {

      var onBeforeClose = scope.$eval(attrs.onBeforeClose);

      var onOpen = scope.$eval(attrs.onOpen);
      var onClose = scope.$eval(attrs.onClose);
      var onOK = scope.$eval(attrs.onOk);
      var cssClass = attrs.css;
      var buttons = scope.$eval(attrs.buttons) || [];

      if(_.isEmpty(buttons) || (_.isUndefined(onClose) || _.isFunction(onClose))) {
        buttons.push({
            text: _t('Close'),
            'class': 'btn button-close',
            click: function() {
              element.dialog('close');
            }
          });
      }

      if(_.isEmpty(buttons) || _.isUndefined(onOK) || _.isFunction(onOK)){
        buttons.push({
            text: _t('OK'),
            'class': 'btn btn-primary button-ok',
            mousedown: function (event) {
              // XXX: Fix missing click event with touchpad when onChange is pending.
              event.preventDefault();
            },
            click: function() {
              // Force blur event on field so that onChange is triggered.
              dialog.parent().find('.ui-dialog-buttonpane .ui-dialog-buttonset .button-ok').focus();

              var doClick = function () {
                if (onOK) {
                  onOK();
                } else {
                  element.dialog('close');
                }
              };

              if (scope.waitForActions) {
                scope.waitForActions(doClick);
              } else {
                doClick();
              }
            }
          });
      }

      var dialog = element.dialog({
        dialogClass: 'ui-dialog-responsive ' + (cssClass || ''),
        resizable: false,
        draggable: false,
        autoOpen: false,
        closeOnEscape: true,
        modal: true,
        zIndex: 1100,
        show: {
          effect: 'fade',
          duration: 300
        },
        buttons: buttons
      });

      // fix IE11 issue
      if (axelor.browser.msie && axelor.browser.rv) {
        var headerHeight = 46;
        var footerHeight = 52;
        var dialogMargin = 64;

        function onResize() {
          var availableHeight = $(window).height() - headerHeight - footerHeight - dialogMargin - 8;
          var contentHeight = element.children().height();
          var myHeight = Math.min(availableHeight, contentHeight);
          element.height(myHeight);
        }

        dialog.on('dialogopen', function (e, ui) {
          $(window).on('resize', onResize);
          setTimeout(onResize);
        });
        dialog.on('dialogclose', function (e, ui) {
          $(window).off('resize', onResize);
        });
        scope.$on('$destroy', function() {
          $(window).off('resize', onResize);
        });

        element.addClass('ui-dialog-ie11');
      }

      // focus the previous visible dialog
      dialog.on('dialogclose', function(e, ui){
        var target = element.data('$target');
        if (target) {
          return setTimeout(function(){
            if (!axelor.device.mobile) {
              var input = target.find(':input:first');
              input.addClass('x-focus').focus().select();
              setTimeout(function () {
                input.removeClass('x-focus');
              });
            }
          });
        }
        $('body .ui-dialog:visible:last').focus();
      });

      dialog.on('dialogopen', onOpen)
          .on('dialogclose', onClose)
          .on('dialogbeforeclose', onBeforeClose);

      scope.$on('$destroy', function(){
        if (dialog) {
          if (dialog.data('dialog')) {
            dialog.dialog('destroy');
          }
          dialog.remove();
          dialog = null;
        }
      });
    }
  };
});

})();
