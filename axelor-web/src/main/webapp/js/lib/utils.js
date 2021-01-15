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

// integrate underscore.string with underscore
_.mixin(_.str.exports());

var util = this.util ? this.util : this.util = {};

/**
 * Based on the `util.inherits` of `Node.js` with some additional features
 *
 * 1. The super prototype is attached to the base prototype as `super_`.
 * 2. The base prototype can be provided as third argument.
 *
 * Example:
 *
 * 	function Hello(message) {
 * 		this.message = message;
 * 	}
 *
 * 	Hello.prototype.say = function(what) {
 * 		console.log(what || this.message);
 * 	}
 *
 * 	function HelloWorld(message) {
 * 		this.super_.constructor.apply(this, arguments);
 * 	}
 *
 * 	util.inherits(HelloWorld, Hello, {
 *
 * 		say: function(what) {
 * 			// do something
 * 			this.super_.say.apply(this, arguments);
 * 		}
 * 	});
 *
 * @param ctor the base constructor
 * @param superCtor the super constructor
 * @param proto the prototype of the base constructor (optional)
 *
 * @returns the base constructor
 */
util.inherits = function(ctor, superCtor, /* optional */ proto) {

  var props = {
    constructor: {
      value: ctor,
      enumerable: false,
      writable: true,
      configurable: true
    }
  };

  Object.getOwnPropertyNames(proto||{}).forEach(function(name) {
    props[name] = Object.getOwnPropertyDescriptor(proto, name);
  });

  ctor.super_ = superCtor;
  ctor.prototype = Object.create(superCtor.prototype, props);
  ctor.prototype.super_ = superCtor.prototype;

  return ctor;
};

/**
 * Shortcut to `util.inherits`.
 *
 * @param superCtor the super constructor
 * @param proto the prototype for this contructor
 *
 * @returns this constructor itself
 */
Function.prototype.inherits = function(superCtor, /* optional */ proto) {
  return util.inherits(this, superCtor, proto);
};

}).call(this);
