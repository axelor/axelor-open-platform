/*
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
