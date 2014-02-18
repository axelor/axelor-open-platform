(function() {

'use strict';

var ng = angular.module('ng');

var lowercase = function(string){return isString(string) ? string.toLowerCase() : string;};
var uppercase = function(string){return isString(string) ? string.toUpperCase() : string;};

function isString(value){return typeof value == 'string';}

function toBoolean(value) {
  if (value && value.length !== 0) {
    var v = lowercase("" + value);
    value = !(v == 'f' || v == '0' || v == 'false' || v == 'no' || v == 'n' || v == '[]');
  } else {
    value = false;
  }
  return value;
}

var ngIfDirective = function() {
  return {
    transclude: 'element',
    priority: 1000,
    terminal: true,
    restrict: 'A',
    compile: function (element, attr, transclude) {
      return function ($scope, $element, $attr) {
        var childElement, childScope;
        $scope.$watch($attr.ngIf, function ngIfWatchAction(value) {
          if (childElement) {
        	childElement.remove();
            childElement = undefined;
          }
          if (childScope) {
            childScope.$destroy();
            childScope = undefined;
          }
          if (toBoolean(value)) {
            childScope = $scope.$new();
            transclude(childScope, function (clone) {
              childElement = clone;
              $element.after(clone);
            });
          }
        });
      }
    }
  }
};

ng.directive('ngIf', ngIfDirective);

}).call(this);