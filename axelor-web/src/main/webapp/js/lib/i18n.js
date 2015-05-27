/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

var bundle = {};

function gettext(key) {
	var message = bundle[key] || key;
	if (message && arguments.length > 1) {
		for(var i = 1 ; i < arguments.length ; i++) {
			var placeholder = new RegExp('\\{' + (i-1) + '\\}', 'g');
			var value = arguments[i];
			message = message.replace(placeholder, value);
		}
	}
	return message;
}

gettext.put = function(messages) {
	message = messages || {};
	for(var key in messages) {
		bundle[key] = messages[key];
	}
};

this._t = gettext;

}).call(this);