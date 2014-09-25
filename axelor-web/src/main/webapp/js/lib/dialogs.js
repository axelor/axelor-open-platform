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
(function($, undefined){
	
	var dialogs = {

		config: {
			yesNo: false
		},
		
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

		confirm: function(str, callback, options) {
			var element = null,
				opts = null,
				cb = angular.noop,
				doCall = true;

			for (var i = 1; i < 3; i++) {
				var arg = arguments[i];
				if (_.isFunction(arg)) cb = arg;
				if (_.isObject(arg)) opts = arg;
			}

			opts = _.extend({
				title: _t('Question')
			}, this.config, opts);

			var titleOK = opts.yesNo ? _t('Yes') : _t('OK');
			var titleCancel = opts.yesNo ? _t('No') : _t('Cancel');

			return element = this.box(str, {
				title: opts.title,
				onClose: function() {
					if (doCall) cb(false);
				},
				buttons: [
					{
						text: titleCancel,
						'class': 'btn',
						click: function() {
							cb(false);
							doCall = false;
							element.dialog('close');
						}
					},
					{
						text: titleOK,
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
				dialogClass: 'ui-dialog-responsive ui-dialog-small ui-dialog-dragged',
				resizable: false,
				draggable: true,
				autoOpen: false,
				closeOnEscape: true,
				modal: true,
				zIndex: 1100,
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

	var elemNotifyStack = null;
	var elemNotifyText = '<div class="alert alert-block fade in">'+
						 '  <button type="button" class="close" data-dismiss="alert">×</button>'+
						 '  <h4 class="alert-heading">#title#</h4>'+
						 '  <p>#message#</p>'+
						 '</div>';
	var elemNotifyText2 = '<div class="alert alert-block fade in">'+
						 '  <button type="button" class="close" data-dismiss="alert">×</button>'+
						 '  <strong>#title#</strong> #message#'+
						 '</div>';

	function doNotify(message, options) {
		if (elemNotifyStack === null) {
			elemNotifyStack = $('<div class="notify-stack"></div>')
				.css('position', 'fixed')
				.css('bottom', 0)
				.css('right', 10)
				.zIndex(9999999)
				.appendTo("body");
		}
		
		var opts = _.extend({
			timeout: 5000
		}, options);
		var tmpl, elem;
		
		tmpl = opts.alt ? elemNotifyText2 : elemNotifyText;
		tmpl = tmpl.replace("#title#", opts.title || '').replace("#message#", message);

		elem = $(tmpl)
			.css('margin-bottom', 7)
			.appendTo(elemNotifyStack);
		
		if (opts.css) {
			elem.addClass(opts.css);
		}
		
		_.delay(function () {
			if (elem) {
				elem.alert("close");
				elem = null;
			}
		}, opts.timeout);

		elem.alert();
	}
	
	var notify = {
		
		info: function(message, options) {
			var opts = _.extend({
				title: _t('Information'),
				css: 'alert-info'
			}, options);
			return doNotify(message, opts);
		},

		alert: function(message, options) {
			var opts = _.extend({
				title: _t('Alert')
			}, options);
			return doNotify(message, options);
		},
		
		success: function(message, options) {
			var opts = _.extend({
				title: _t('Success'),
				css: 'alert-success'
			}, options);
			return doNotify(message, opts);
		},

		error: function(message, options) {
			var opts = _.extend({
				title: _t('Error'),
				css: 'alert-error'
			}, options);
			return doNotify(message, opts);
		}
	};
	
	if (this.axelor == null) {
		this.axelor = {};
	}

	this.axelor.dialogs = dialogs;
	this.axelor.notify = notify;

})(window.jQuery);
