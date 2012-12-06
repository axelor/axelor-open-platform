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
