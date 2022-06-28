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
(function() {

  "use strict";

  // create global axelor namespace if not exists
  window.axelor = window.axelor || {};
  window.axelor.config = {};

  // browser detection (adopted from jquery)
  var ua = navigator.userAgent.toLowerCase();
  var browser = {};
  var match =
    /(edge)[\/]([\w.]+)/.exec(ua) ||
    /(opr)[\/]([\w.]+)/.exec(ua) ||
    /(chrome)[ \/]([\w.]+)/.exec(ua) ||
    /(version)(applewebkit)[ \/]([\w.]+).*(safari)[ \/]([\w.]+)/.exec(ua) ||
        /(webkit)[ \/]([\w.]+).*(version)[ \/]([\w.]+).*(safari)[ \/]([\w.]+)/.exec(ua) ||
        /(webkit)[ \/]([\w.]+)/.exec(ua) ||
    /(opera)(?:.*version|)[ \/]([\w.]+)/.exec(ua) ||
    /(msie) ([\w.]+)/.exec(ua) ||
    ua.indexOf("trident") >= 0 && /(rv)(?::| )([\w.]+)/.exec( ua ) ||
    ua.indexOf("compatible") < 0 && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec(ua) ||
    [];

  var matched = {
    browser: match[5] || match[3] || match[1] || "",
    version: match[4] || match[2] || "0"
  };

  if (matched.browser) {
    browser[matched.browser] = true;
    browser.version = matched.version;
  }
  if (browser.chrome || browser.opr || browser.safari) {
    browser.webkit = true;
  }

  // IE11
  if (browser.rv) {
    var ie = "msie";
    matched.browser = ie;
    browser[ie] = true;
  }

  // recent opera
    if (browser.opr ) {
      var opera = "opera";
        matched.browser = opera;
        browser[opera] = true;
    }

  // screen size detection
  var device = {
    small: false,
    large: false
  };

  device.large = $(window).width() > 768;
  device.small = !device.large;
  device.mobile = /Mobile|Android|iPhone|iPad|iPod|BlackBerry|Windows Phone/i.test(ua);
  device.macLike = /Mac OS/i.test(ua);

  axelor.browser = browser;
  axelor.device = device;

  var lastCookieString;
  var lastCookies = {};

  function readCookie(name) {
    var cookieString = (document.cookie || '');
    if (cookieString !== lastCookieString) {
      lastCookieString = cookieString;
      lastCookies = _.reduce(cookieString.split('; '), function (obj, value) {
        var parts = value.split('=');
        if (!obj.hasOwnProperty(parts[0])) {
          obj[parts[0]] = parts[1];
        }
        return obj;
      }, {});
    }
    return lastCookies[name];
  }

  axelor.readCookie = readCookie;

  if (typeof DOMPurify !== 'undefined') {

    // this function removes <script> and event attributes (onerror, onload etc.) from the given html text
    function sanitize() {
      if (arguments.length === 0) {
        return;
      }
      var args = arguments.length === 1 ? arguments[0] : Array.prototype.slice.call(arguments);

      if (Array.isArray(args)) {
        return args.map(function (item) {
          if (Array.isArray(item)) {
            return sanitize(item);
          }
          if (item instanceof jQuery) {
            return $(DOMPurify.sanitize(item[0]));
          }
          return DOMPurify.sanitize(item);
        });
      }

      if (args instanceof jQuery) {
        return $(DOMPurify.sanitize(args[0]));
      }

      return DOMPurify.sanitize(args);
    }

    axelor.sanitize = sanitize;

    // sanitize jquery html function
    var jq = {
      html: $.fn.html
    };

    $.fn.staticHtml = function staticHtml() {
      var args = Array.prototype.slice.call(arguments);
      return jq.html.apply(this, sanitize(args));
    };
  }

  // Mutates the original moment by setting it to the start of the next unit of time.
  // Used for criteria with date ranges in order to exclude the upper limit.
  axelor.nextOf = function (mm, timeUnit) {
    return mm.add(1, timeUnit).startOf(timeUnit);
  };

  var INITIAL_PAGE_SIZE = 40;
  /**
   * Get the default number of items to display per page.
   *
   * @returns {number} page size
   */
  axelor.getDefaultPageSize = function() {
    if (axelor.config['api.pagination.max-per-page'] > 0) {
      return Math.min(axelor.config['api.pagination.max-per-page'], INITIAL_PAGE_SIZE);
    }
    return INITIAL_PAGE_SIZE;
  };

})();
