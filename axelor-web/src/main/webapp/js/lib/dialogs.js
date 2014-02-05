/*
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
