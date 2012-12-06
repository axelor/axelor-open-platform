(function($, undefined){
	
	var dialogs = {

		say: function(str) {
			return this.box(str, {
				title: _t('Information')
			});
		},

		warn: function(str, callback) {
			return this.box(str, {
				title: _t('Warning'),
				onClose: callback
			});
		},

		error: function(str, callback) {
			return this.box(str, {
				title: _t('Error'),
				onClose: callback
			});
		},

		confirm: function(str, callback) {
			var element = null,
				cb = callback || $.noop,
				doCall = true;
	
			return element = this.box(str, {
				title: _t('Question'),
				onClose: function() {
					if (doCall) cb(false);
				},
				buttons: [
					{
						text: _t('Cancel'),
						'class': 'btn',
						click: function() {
							cb(false);
							doCall = false;
							element.dialog('close');
						}
					},
					{
						text: _t('OK'),
						'class': 'btn btn-primary',
						click: function() {
							cb(true);
							doCall = false;
							element.dialog('close');
						}
					}
				]
			});
		},
		
		box: function(str, options) {
		
			var opts = $.extend({}, options);
			var title = opts.title || _t('Information');
			var buttons = opts.buttons;
			var onClose = opts.onClose || $.noop;
	
			if (buttons == null) {
				buttons = [
					{
						'text'	: _t('OK'),
						'class'	: 'btn btn-primary',
						'click'	: function() {
							element.dialog('close');
						}
					}
				];
			}
			
			var element = $('<div class="message-box" style="padding: 15px;"></div>').attr('title', title).html(str);
			var dialog = element.dialog({
				autoOpen: false,
				closeOnEscape: true,
				modal: true,
				zIndex: 1100,
				minWidth: 450,
				maxHeight: 500,
				close: function(e) {
					onClose(e);
					element.dialog('destroy');
					element.remove();
				},
				buttons: buttons
			});
			
			// maintain overlay opacity
			var opacity = null;
			dialog.on('dialogopen dialogclose', function(e, ui){
				var overlay = $('body .ui-widget-overlay');
				if (opacity === null) {
					opacity = overlay.last().css('opacity');
				}
				$('body .ui-widget-overlay')
					.css('opacity', 0).last()
					.css('opacity', opacity);
			});
			
			dialog.dialog('open');
			
			return dialog;
		}
	};
	
	if (this.axelor == null)
		this.axelor = {};
	
	this.axelor.dialogs = dialogs;
	
})(window.jQuery);