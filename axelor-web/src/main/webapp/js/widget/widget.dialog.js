angular.module('axelor.ui').directive('uiDialog', function() {
	return {
		restrict: 'EA',
		link: function(scope, element, attrs) {

			var onBeforeClose = scope.$eval(attrs.onBeforeClose);
			
			var onOpen = scope.$eval(attrs.onOpen);
			var onClose = scope.$eval(attrs.onClose);
			var onOK = scope.$eval(attrs.onOk);
			
			var dialog = element.dialog({
				autoOpen: false,
				closeOnEscape: true,
				modal: true,
				zIndex: 1100,
				buttons: [
				    {
				    	text: _t('Close'),
				    	'class': 'btn',
				    	click: function() {
				    		element.dialog('close');
				    	}
				    },
				    {
				    	text: _t('OK'),
				    	'class': 'btn btn-primary',
				    	click: function() {
				    		if (onOK) {
				    			onOK();
				    		}
				    		else
				    			element.dialog('close');
				    	}
				    }
				]
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
