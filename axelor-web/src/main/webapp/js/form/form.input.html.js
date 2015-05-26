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
(function(){

var ui = angular.module('axelor.ui');

function getStylePopup(element, styles) {

	return function ($popup, $button) {

		var $list = $('<div/>').addClass('wysiwyg-plugin-list')
	                           .attr('unselectable', 'on');

		$.each(styles, function(format, name) {
	        var $link = $('<a/>').attr('href','#')
	                             .html(name)
	                             .click(function(event) {
	                                $(element).wysiwyg('shell').format(format).closePopup();
	                                event.stopPropagation();
	                                event.preventDefault();
	                                return false;
	                            });
	        $list.append($link);
	    });

		$popup.append($list);
	}
}

function getButtons(element, lite) {

	return {
		style: lite ? false : {
			title: _t('Style'),
			image: '\uf1dd',
			popup: getStylePopup(element, {
		        '<p>' 			: _t('Normal'),
		        '<pre>' 		: _t('Formated'),
	        	'<blockquote>'	: _t('Blockquote')
		    })
		},
		header: lite ? false : {
			title: _t('Header'),
			image: '\uf1dc',
			popup: getStylePopup(element, {
		        '<h1>': _t('Header 1'),
        		'<h2>': _t('Header 2'),
				'<h3>': _t('Header 3'),
				'<h4>': _t('Header 4'),
				'<h5>': _t('Header 5'),
				'<h6>': _t('Header 6')
		    })
		},
		d1: lite ? false : {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
		bold: {
	        title: _t('Bold (Ctrl+B)'),
	        image: '\uf032'
	    },
	    italic: {
	        title: _t('Italic (Ctrl+I)'),
	        image: '\uf033'
	    },
	    underline: {
	        title: _t('Underline (Ctrl+U)'),
	        image: '\uf0cd'
	    },
	    strikethrough: {
	        title: _t('Strikethrough (Ctrl+S)'),
	        image: '\uf0cc'
	    },
	    removeformat: {
	        title: _t('Remove format'),
	        image: '\uf12d'
	    },
	    d2: {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
	    forecolor: lite ? false : {
	        title: _t('Text color'),
	        image: '\uf1fc'
	    },
	    highlight: lite ? false : {
	        title: _t('Background color'),
	        image: '\uf043'
	    },
	    d3: lite ? false : {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
	    alignleft: {
	        title: _t('Left'),
	        image: '\uf036'
	    },
	    aligncenter: {
	        title: _t('Center'),
	        image: '\uf037'
	    },
	    alignright: {
	        title: _t('Right'),
	        image: '\uf038'
	    },
	    alignjustify: {
	        title: _t('Justify'),
	        image: '\uf039'
	    },
	    d4: {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
	    orderedList: {
	        title: _t('Ordered list'),
	        image: '\uf0cb'
	    },
	    unorderedList: {
	        title: _t('Unordered list'),
	        image: '\uf0ca'
	    },
	    d6: lite ? false : {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
	    indent: lite ? false : {
	        title: _t('Indent'),
	        image: '\uf03c'
	    },
	    outdent: lite ? false : {
	        title: _t('Outdent'),
	        image: '\uf03b'
	    },
	    d7: lite ? false : {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
		insertimage: lite ? false : {
            title: _t('Insert image'),
            image: '\uf030'
		},
		insertlink: lite ? false : {
            title: _t('Insert link'),
            image: '\uf08e'
		}
	};
}

ui.formInput('Html', {

	css: "html-item",

	init: function(scope) {

		scope.parse = function(value) {
			return value ? value : null;
		};
	},

	link_editable: function(scope, element, attrs, model) {
		this._super(scope, element, attrs, model);
		var editor = element.find('.html-editor');
		var buttons = getButtons(editor, scope.field.lite);

		editor.wysiwyg({
			toolbar: 'top',
			buttons: buttons,
			submit: {
                title: _t('Submit'),
                image: '\uf00c'
            },
            selectImage: _t('Click or drop image'),
            placeholderUrl: 'www.example.com',
            maxImageSize: [600, 200],
			hijackContextmenu: false
		});

		var shell = editor.wysiwyg('shell');

		function onChange(e) {
			var val = shell.getHTML();
			var old = scope.getValue();
			var txt = scope.parse(val);

			if (old === txt) {
				return;
			}

			scope.setValue(val, true);
			scope.applyLater();
		}

		scope.$render_editable = function () {
			var value = scope.getValue() || "",
				html = shell.getHTML();

			scope.text = scope.format(value);

			if (value === html) {
				return;
			}

			shell.setHTML(value);
		}

		editor.on('input', _.debounce(onChange, 100));
		editor.on('keypress', function(e) {
			// always start new paragraph on ENTER
		    if(e.keyCode == '13' && !e.shiftKey) {
		    	var current = document.queryCommandValue('formatBlock');
		    	if (!current) {
		    		document.execCommand('formatBlock', false, 'p');
		    	}
			}
		});
	},

	template_readonly:
	'<div class="form-item-container">'+
		'<div class="html-viewer" ui-bind-template x-text="text" x-locals="record" x-live="field.live"></div>'+
	'</div>',
	template_editable:
	'<div class="form-item-container">'+
		'<div class="html-editor"></div>'+
	'</div>'

});

ui.directive('uiBindTemplate', ['$interpolate', function($interpolate){

	function expand(scope, template) {
		if (!template || !template.match(/{{.*?}}/)) {
			return template;
		}
		return $interpolate(template)(scope.locals());
	}

	return {
		terminal: true,
		scope: {
			locals: "&",
			text: "=text",
			live: "&"
		},
		link: function(scope, element, attrs) {

			var template = null;

			function update() {
				var output = expand(scope, template) || "";
				element.html(output);
			}

			scope.$watch("text", function(text, old) {

				if (text === template) {
					return;
				}
				template = text;
				update();
			});

			var live = false;
			scope.$watch("live()", function(value) {
				if (live || !value) {
					return;
				}
				live = true;
				scope.$watch("locals()", function(value) {
					update();
				}, true);
			});
		}
	};
}]);

})(this);
