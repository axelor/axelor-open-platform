/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

})();
