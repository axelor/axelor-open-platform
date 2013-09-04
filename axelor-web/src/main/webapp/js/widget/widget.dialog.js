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
angular.module('axelor.ui').directive('uiDialog', function() {
	return {
		restrict: 'EA',
		link: function(scope, element, attrs) {

			var onBeforeClose = scope.$eval(attrs.onBeforeClose);
			
			var onOpen = scope.$eval(attrs.onOpen);
			var onClose = scope.$eval(attrs.onClose);
			var onOK = scope.$eval(attrs.onOk);
			var buttons = [];
			
			if(_.isUndefined(onClose) || _.isFunction(onClose)){
				buttons.push({
			    	text: _t('Close'),
			    	'class': 'btn',
			    	click: function() {
			    		element.dialog('close');
			    	}
			    });
			}
			
			if(_.isEmpty(buttons) || _.isUndefined(onOK) || _.isFunction(onOK)){
				buttons.push({
			    	text: _t('OK'),
			    	'class': 'btn btn-primary',
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
				autoOpen: false,
				closeOnEscape: true,
				modal: true,
				zIndex: 1100,
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
						target.find(':input:first').focus().select();
					});
				}
				$('body .ui-dialog:visible:last').focus();
			});
			
			dialog.on('dialogresizestop', function(){
				$.event.trigger('adjustSize');
			});

			dialog.on('dialogopen', onOpen)
				  .on('dialogclose', onClose)
				  .on('dialogbeforeclose', onBeforeClose);

			scope.$on('$destroy', function(){
				if (dialog) {
					dialog.dialog('destroy');
					dialog.remove();
					dialog = null;
				}
			});
		}
	};
});
