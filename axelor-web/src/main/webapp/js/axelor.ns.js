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

  function sanitizeElement(element) {

    var attrs = _.filter(element.attributes, function(a) {
      var attr = a.name;
      var value = a.value;

      if (["src", "href", "action"].indexOf(attr) > -1 && value) {
        value = value.replace(/[\x00-\x20]+/g, ''); // https://www.owasp.org/index.php/XSS_Filter_Evasion_Cheat_Sheet#Embedded_tab
        value = value.replace(/<\!\-\-.*?\-\-\>/g, ''); // remove comments which might be interpreted as xml
      }

      return attr.indexOf('xss-on') === 0 || (value && value.match(/^javascript\:/i));
    });

    _.each(attrs, function (a) {
      $(element).removeAttr(a.name);
    });
  }

  // this function removes <script> and event attributes (onerror, onload etc.) from the given html text
  function sanitizeText(html) {
    if (typeof html !== 'string') {
      return html;
    }
    var value = "<div>" + html.replace(/(\s)(on(?:\w+))(\s*=)/, '$1xss-$2$3') + "</div>";
    var elems = $($.parseHTML(value, null, false));

    elems.find('*').each(function() {
      sanitizeElement(this);
    });

    return elems.html();
  }

  function sanitize() {
    if (arguments.length === 0) {
      return;
    }
    var args = arguments.length === 1 ? arguments[0] : Array.prototype.slice.call(arguments);
    return Array.isArray(args) ? args.map(function (item) {
      return Array.isArray(item) ? sanitize(item) : sanitizeText(item);
    }) : sanitizeText(args);
  }

  axelor.sanitize = sanitize;

  // sanitize jquery html function
  var jq = {
    html: $.fn.html
  };

  $.fn.html = function html() {
    return jq.html.apply(this, sanitize(Array.prototype.slice.call(arguments)));
  };

})();
