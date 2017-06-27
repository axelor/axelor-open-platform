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

var ui = angular.module('axelor.ui');

ui.directive('uiDialog', function() {
	return {
		restrict: 'EA',
		link: function(scope, element, attrs) {

			var onBeforeClose = scope.$eval(attrs.onBeforeClose);
			
			var onOpen = scope.$eval(attrs.onOpen);
			var onClose = scope.$eval(attrs.onClose);
			var onOK = scope.$eval(attrs.onOk);
			var cssClass = attrs.css;
			var buttons = scope.$eval(attrs.buttons) || [];

			if(_.isEmpty(buttons) || (_.isUndefined(onClose) || _.isFunction(onClose))) {
				buttons.push({
			    	text: _t('Close'),
			    	'class': 'btn button-close',
			    	click: function() {
			    		element.dialog('close');
			    	}
			    });
			}
			
			if(_.isEmpty(buttons) || _.isUndefined(onOK) || _.isFunction(onOK)){
				buttons.push({
			    	text: _t('OK'),
			    	'class': 'btn btn-primary button-ok',
			    	click: function() {
			    		if (onOK) {
			    			onOK();
			    		}
			    		else
			    			element.dialog('close');
			    	}
			    });
			}
			
			var dialog = element.dialog({
				dialogClass: 'ui-dialog-responsive ' + (cssClass || ''),
				resizable: false,
				draggable: false,
				autoOpen: false,
				closeOnEscape: true,
				modal: true,
				zIndex: 1100,
				show: {
					effect: 'fade',
					duration: 300
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
			
			// focus the previous visible dialog
			dialog.on('dialogclose', function(e, ui){
				var target = element.data('$target');
				if (target) {
					return setTimeout(function(){
						if (!axelor.device.mobile) {
							var input = target.find(':input:first');
							input.addClass('x-focus').focus().select();
							setTimeout(function () {
								input.removeClass('x-focus');
							});
						}
					});
				}
				$('body .ui-dialog:visible:last').focus();
			});
			
			dialog.on('dialogopen', onOpen)
				  .on('dialogclose', onClose)
				  .on('dialogbeforeclose', onBeforeClose);

			scope.$on('$destroy', function(){
				if (dialog) {
					if (dialog.data('dialog')) {
						dialog.dialog('destroy');
					}
					dialog.remove();
					dialog = null;
				}
			});
		}
	};
});

})();
