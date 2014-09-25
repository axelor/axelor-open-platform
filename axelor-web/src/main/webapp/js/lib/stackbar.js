/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
(function($, undefined) {

	var Stackbar = function(element) {
		this.element = $(element);
	};
	
	Stackbar.prototype = {
		
		constructor: Stackbar,

		init: function() {

			var elem = $(this.element),
				header = elem.children('div:first'),
				footer = elem.children('div:last'),
				center = header.next();

			center.css('top', header.outerHeight(true))
				  .add(footer)
				  .css('left', 0)
				  .css('right', 0)
				  .add(elem)
				  .css('position', 'absolute');

			if (footer.is(center)) {
				center.css('bottom', 0);
			} else {
				footer.css('bottom', 0);
				center.css('bottom', footer.outerHeight(true));
			}

			center.css('overflow', 'auto');
	
			elem.css({
				'top'	: 0,
				'bottom': 0,
				'left'	: 0,
				'right'	: 0,
				'overflow': 'hidden'
			});
		}
	};
	
	$.fn.stackbar = function (options) {
		return this.each(function () {
			var $this = $(this),
				data = $this.data('stackbar');
		  	if (!data) $this.data('stackbar', (data = new Stackbar(this, options)));
		  	if (typeof options == 'string') data[options]();
		});
	};

	$.fn.stackbar.Constructor = Stackbar;

})(window.jQuery);
