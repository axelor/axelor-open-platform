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

/**
 * The Form widget.
 *
 */
ui.formWidget('Form', {

  priority: 100,

  css: "dynamic-form",

  scope: false,

  compile: function(element, attrs) {

    element.hide();
    element.find('[x-field],[data-field]').each(function(){

      var elem = $(this),
        name = elem.attr('x-field') || elem.attr('data-field');

      if (name && elem.attr('ui-button') === undefined) {
        if (!elem.attr('ng-model')) {
          elem.attr('ng-model', 'record.' + name);
        }
        if (!elem.attr('ng-required')) {
          // always attache a required validator to make
          // dynamic `required` attribute change effective
          elem.attr('ng-required', false);
        }
      }
    });

    return ui.formCompile.apply(this, arguments);
  },

  link: function(scope, element, attrs, controller) {

    element.on('submit', function(e) {
      e.preventDefault();
    });

    scope.$watch('record', function formRecordWatch(rec, old) {
      if (element.is(':visible')) {
        return;
      }
      scope.ajaxStop(function() {
        element.show();
        axelor.$adjustSize();
      });
    });
  }
});

/**
 * This directive is used filter $watch on scopes of inactive tabs.
 *
 */
ui.directive('uiTabGate', function() {

  return {

    compile: function compile(tElement, tAttrs) {

      return {
        pre: function preLink(scope, element, attrs) {
          scope.$watchChecker(function(current) {
            if (current.$$popupStack.length) return true;
            if (current.tabSelected === undefined) {
              return !scope.tab || scope.tab.selected === undefined || scope.tab.selected;
            }
            return current.tabSelected;
          });
        }
      };
    }
  };
});

/**
 * This directive is used to filter $watch on scopes of hidden forms.
 *
 */
ui.directive('uiFormGate', function() {

  return {
    compile: function compile(tElement, tAttrs) {

      return {
        pre: function preLink(scope, element, attrs) {
          var parent = null;
          scope.$watchChecker(function(current) {
            if (scope.tabSelected === false) {
              return false;
            }
            if (parent === null) {
              parent = element.parents('[ui-show]:first');
            }
            // hack for hidden nested editors (#2173)
            if (scope.$$forceWatch) {
              return true;
            }
            return !(parent.hasClass('ui-hide') || parent.hasClass('ui-hide'));
          });
        }
      };
    }
  };
});

/**
 * This directive is used to filter $watch on scopes based on some condition.
 *
 */
ui.directive('uiWatchIf', ['$parse', function($parse) {

  return {
    compile: function compile(tElement, tAttrs) {
      return {
        pre: function preLink(scope, element, attrs) {
          var value = false,
            expression = $parse(attrs.uiWatchIf);

          scope.$watchChecker(function (current) {
            if (current === scope) {
              return value = expression(scope);
            }
            return value;
          });
        }
      };
    }
  };
}]);

function toBoolean(value) {
  if (value && value.length !== 0) {
    var v = angular.lowercase("" + value);
    value = !(v == 'f' || v == '0' || v == 'false' || v == 'no' || v == 'n' || v == '[]');
  } else {
    value = false;
  }
  return value;
}

/**
 * This directive is used to speedup uiFormGate.
 */
ui.directive('uiShow', function() {

  return {
    scope: true, // create new scope to always watch the expression
    link: function link(scope, element, attrs) {
      scope.$$shouldWatch = true;
      scope.$watch(attrs.uiShow, function uiShowWatchAction(value){
        var val = toBoolean(value);
        element.css({ display: val ? '' : 'none', opacity: 0 }).toggleClass('ui-hide', !val);
        if (val) {
          element.animate({ opacity: 1 }, 300);
        }
      });
    }
  };
});

/**
 * This directive is used by view-pane to attach/detach element from DOM tree
 */
ui.directive('uiAttach', function () {
  return function (scope, element, attrs) {
    var parent = null;
    var detachTimer = null;
    var uiAttachWatch = function uiAttachWatch(attach) {
      var result = toBoolean(attach);
      if (result) {
        if (parent) {
          if (detachTimer) {
            clearTimeout(detachTimer);
            detachTimer = null;
          } else {
            element.appendTo(parent);
          }
          parent = null;
          scope.$broadcast('dom:attach');
        }
      } else {
        parent = element.parent();
        scope.$broadcast('dom:detach');
        detachTimer = setTimeout(function () {
          detachTimer = null;
          element.detach();
        }, 200);
      }
    };

    uiAttachWatch.uiAttachWatch = true;

    scope.$watch(attrs.uiAttach, uiAttachWatch, true);
    scope.$on('$destroy', function () {
      if (detachTimer) {
        clearTimeout(detachTimer);
        detachTimer = null;
      }
      if (parent) {
        parent = null;
        element.remove();
      }
    });
  };
});

/**
 * This directive can be used by widget to restore scroll when element is re-attached to DOM tree.
 */
ui.directive('uiAttachScroll', function () {
  return function (scope, element, attrs) {
    setTimeout(function () {
      var elem = element;
      var scrollTop = 0;

      if (attrs.uiAttachScroll) {
        elem = element.find(attrs.uiAttachScroll);
      }

      elem.on('scroll', function () {
        scrollTop = this.scrollTop;
      });

      function resetScroll() {
        elem.scrollTop(scrollTop);
      }

      scope.$on('dom:attach', resetScroll);
      scope.$on('tab:select', resetScroll);
    }, 300);
  };
});

ui.directive('uiWidgetStates', ['$parse', '$interpolate', function($parse, $interpolate) {

  function isValid(scope, name) {
    if (!name) return scope.isValid();
    var ctrl = scope.form;
    if (ctrl) {
      ctrl = ctrl[name];
    }
    if (ctrl) {
      return ctrl.$valid;
    }
  }

  function withContext(scope, record) {
    var context = scope._context;
    var parent = scope.$parent;
    while (parent) {
      context = _.extend({}, parent._context, context);
      parent = parent.$parent;
    }
    var values = _.extend({}, context, scope._jsonContext, record);
    return _.extend(values, {
      $user: axelor.config['user.login'],
      $group: axelor.config['user.group'],
      $userId: axelor.config['user.id'],
    });
  }

  function handleCondition(scope, field, attr, condition, negative) {

    if (!condition || _.isBoolean(condition)) {
      return;
    }

    scope.$on("on:record-change", function(e, rec, force) {
      if (field && field.jsonField) {
        handle(scope.record);
      } else if (rec === scope.record || force) {
        handle(rec);
      }
    });
    scope.$on("on:grid-selection-change", function(e, context, force) {
      if (field && field.jsonField) return;
      if (!force && (scope._isNestedGrid === undefined || !scope._isNestedGrid)) return;
      if (!scope._isDetailsForm || force) {
        handle(context);
      }
    });

    scope.$watch("isReadonly()", watcher);
    scope.$watch("isRequired()", watcher);
    scope.$watch("isValid()", watcher);

    var expr = $parse(condition);

    function watcher(current, old) {
      var rec = scope.record;
      if (rec === undefined && current === old) return;
      if (rec === undefined && scope.getContext) {
        rec = scope.getContext();
      }
      handle(rec);
    }

    function handle(rec) {
      var value;
      try {
        value = !!axelor.$eval(scope, expr, withContext(scope, rec));
      } catch (e) {
        console.error('FAILED:', condition, e);
      }
      // defer attr change to allow field init, see RM-14998
      scope.$applyAsync(function () {
        scope.attr(attr, negative ? !value : value);
      });
    }
  }

  function handleHilites(scope, field) {
    if (!field || _.isEmpty(field.hilites)) {
      return;
    }

    var hilites = field.hilites || [];
    var exprs = _.map(_.pluck(hilites, 'condition'), function (s) { return $parse(s); });

    function handle(rec) {
      for (var i = 0; i < hilites.length; i++) {
        var hilite = hilites[i];
        var expr = exprs[i];
        var value = false;
        try {
          value = axelor.$eval(scope, expr, withContext(scope, rec));
        } catch (e) {
          console.error('FAILED:', hilite, e);
        }
        if (value) {
          return scope.attr('highlight', {
            hilite: hilite,
            passed: value
          });
        }
      }
      return scope.attr('highlight', {});
    }

    scope.$on("on:record-change", function(e, rec) {
      if (rec === scope.record) {
        handle(rec);
      }
    });
  }

  function handleBind(scope, field) {

    if (!field.bind || !field.name) {
      return;
    }

    var expr = $interpolate(field.bind);
    var last = null;

    function handle(rec) {
      var value;
      try {
        value = expr(withContext(scope, rec));
        if (value.length === 0) {
          value = null;
        }
      } catch (e) {
        console.error('FAILED:', field.bind, e);
      }

      if (scope.setValue && scope.record && last !== value) {
        scope.setValue(last = value);
      }
    }

    scope.$on("on:record-change", function(e, rec) {
      if (field && field.jsonField) {
        handle(scope.record);
      } else if (rec && rec === scope.record) {
        handle(rec);
      }
    });
  }

  function handleValueExpr(scope, field) {

    if (!field.valueExpr || !field.name) {
      return;
    }

    var expr = $parse(field.valueExpr);

    function handle(rec) {
      var value;
      try {
        value = axelor.$eval(scope, expr, withContext(scope, rec));
        if (value && value.length === 0) {
          value = null;
        }
      } catch (e) {
        console.error('FAILED:', field.valueExpr, e);
      }

      if (scope.setValue && scope.record) {
        scope.setValue(value, false);
      }
    }

    scope.$on("on:record-change", function(e, rec) {
      scope.waitForActions(function () {
        if (field && field.jsonField) {
          handle(scope.record);
        } else if (rec && rec === scope.record) {
          handle(rec);
        }
      });
    });
  }

  function handleFor(scope, field, attr, conditional, negative) {
    if (field[conditional]) {
      handleCondition(scope, field, attr, field[conditional], negative);
    }
  }

  function handleForField(scope) {
    var field = scope.field;
    if (!field) return;
    handleFor(scope, field, "valid", "validIf");
    handleFor(scope, field, "hidden", "hideIf");
    handleFor(scope, field, "hidden", "showIf", true);
    handleFor(scope, field, "readonly", "readonlyIf");
    handleFor(scope, field, "required", "requiredIf");
    handleFor(scope, field, "collapse", "collapseIf");
    handleFor(scope, field, "canNew", "canNew");
    handleFor(scope, field, "canView", "canView");
    handleFor(scope, field, "canEdit", "canEdit");
    handleFor(scope, field, "canRemove", "canRemove");
    handleFor(scope, field, "canSelect", "canSelect");
    handleHilites(scope, field);
    handleBind(scope, field);
    handleValueExpr(scope, field);
  }

  function handleForView(scope) {
    var field = scope.schema;
    if (!field) return;
    handleFor(scope, field, "canNew", "canNew");
    handleFor(scope, field, "canEdit", "canEdit");
    handleFor(scope, field, "canSave", "canSave");
    handleFor(scope, field, "canCopy", "canCopy");
    handleFor(scope, field, "canDelete", "canDelete");
    handleFor(scope, field, "canArchive", "canArchive");
    handleFor(scope, field, "canAttach", "canAttach");
  }

  return function(scope, element, attrs) {
    scope.$evalAsync(function() {
      if (element.is('[ui-form]')) {
        return handleForView(scope);
      }
      handleForField(scope);
    });
  };
}]);

})();
