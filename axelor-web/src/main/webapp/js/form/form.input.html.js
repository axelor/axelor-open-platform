/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

function getStylePopup(element, styles) {

	return function($popup, $button) {

		var $list = $('<div/>').addClass('wysiwyg-plugin-list').attr('unselectable', 'on');

		$.each(styles, function(format, name) {
			var $link = $('<a/>').attr('href', '#').html(name).click(
					function(event) {
						$(element).wysiwyg('shell').format(format).closePopup();
						event.stopPropagation();
						event.preventDefault();
						return false;
					});
			$list.append($link);
		});

		$popup.append($list);
	};
}

function getFontNamePopup(element, fonts) {

	return function($popup, $button) {

		var $list = $('<div/>').addClass('wysiwyg-plugin-list').attr('unselectable', 'on');

		$.each(fonts, function(font, name) {
			var $link = $('<a/>').attr('href', '#').html(name).click(
					function(event) {
						try {
							document.execCommand('styleWithCSS', false, false);
							$(element).wysiwyg('shell').fontName(font).closePopup();
						} finally {
							document.execCommand('styleWithCSS', false, true);
						}
						event.stopPropagation();
						event.preventDefault();
						return false;
					});
			$list.append($link);
		});

		$popup.append($list);
	};
}

function getFontSizePopup(element, sizes) {

	return function($popup, $button) {

		var $list = $('<div/>').addClass('wysiwyg-plugin-list').attr('unselectable', 'on');

		$.each(sizes, function(size, name) {
			var $link = $('<a/>').attr('href', '#').html(name).click(
					function(event) {
						$(element).focus();
						try {
							document.execCommand('styleWithCSS', false, false);
							$(element).wysiwyg('shell').fontSize(size).closePopup();
						} finally {
							document.execCommand('styleWithCSS', false, true);
						}
						event.stopPropagation();
						event.preventDefault();
						return false;
					});
			$list.append($link);
		});

		$popup.append($list);
	};
}

function getButtons(scope, element) {

	var lite = scope.field.lite;

	return {
		style: lite ? false : {
			title: _t('Style'),
			image: '\uf1dd',
			popup: getStylePopup(element, {
				'<p>' 			: _t('Normal'),
				'<pre>' 		: '<pre>' + _t('Formatted') + '</pre>',
				'<blockquote>'	: '<blockquote>' + _t('Blockquote') + '</blockquote>',
				'<h1>': '<h1>' + _t('Header 1') + '</h1>',
				'<h2>': '<h2>' + _t('Header 2') + '</h2>',
				'<h3>': '<h3>' + _t('Header 3') + '</h3>',
				'<h4>': '<h4>' + _t('Header 4') + '</h4>',
				'<h5>': '<h5>' + _t('Header 5') + '</h5>',
				'<h6>': '<h6>' + _t('Header 6') + '</h6>'
		    })
		},
		fontName: lite ? false : {
			title: _t('Font'),
			image: '\uf031',
			popup: getFontNamePopup(element, {
				'"Times New Roman", Times, serif': '<span style="font-family: \"Times New Roman\", Times, serif">Times New Roman</span>',
				'Arial, Helvetica, sans-serif': '<span style="font-family: Arial, Helvetica, sans-serif">Arial</span>',
				'"Courier New", Courier, monospace': '<span style="font-family: \"Courier New\", Courier, monospace">Courier New</span>',
				'Comic Sans, Comic Sans MS, cursive': '<span style="font-family: Comic Sans, Comic Sans MS, cursive">Comic Sans</span>',
				'Impact, fantasy': '<span style="font-family: Impact, fantasy">Impact</span>'
		    })
		},
		fontSize: lite ? false : {
			title: _t('Font size'),
			image: '\uf035',
			popup: getFontSizePopup(element, {
				'1': '<span style="font-size: x-small">' + _t('Smaller') + '</span>',
				'2': '<span style="font-size: small">' + _t('Small') + '</span>',
				'3': '<span style="font-size: medium">' + _t('Medium') + '</span>',
				'4': '<span style="font-size: large">' + _t('Large') + '</span>',
				'5': '<span style="font-size: x-large">' + _t('Larger') + '</span>'
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
		},
		d8: lite ? false : {
			html: $('<span class="wysiwyg-toolbar-divider"></span>')
		},
		normalize: lite ? false : {
	    	title: _t('Normalize'),
	    	image: '\uf0d0',
	    	click: function () {
	    		scope.normalize();
	    	}
	    },
		showCode: lite ? false : {
			title: _t('Code'),
			image: '\uf121',
			click: function () {
				scope.toggleCode();
			}
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
		var textElement = element.find('textarea');
		var buttons = getButtons(scope, textElement);

		var props = scope.field,
			minSize = +props.minSize,
			maxSize = +props.maxSize,
			height = +(scope.field.height) || null;

		if (height) {
			height = Math.max(100, height);
		}

		function isParagraph(node) {
			return node && /^DIV|^P|^LI|^H[1-7]/.test(node.nodeName.toUpperCase());
		}

		function findParagraph(node) {
			if (!node) return null;
			if (node.classList && node.classList.contains('wysiwyg-editor')) return null;
			if (isParagraph(node)) return node;
			return findParagraph(node.parentNode);
		}

		textElement.wysiwyg({
			toolbar: 'top',
			buttons: buttons,
			submit: {
                title: _t('Submit'),
                image: '\uf00c'
            },
            selectImage: _t('Click or drop image'),
            placeholderUrl: 'www.example.com',
            maxImageSize: [600, 200],
			hijackContextmenu: false,
			onKeyPress: function(key, character, shiftKey, altKey, ctrlKey, metaKey) {
				if (key !== 13 || shiftKey) {
					return;
				}
		    	var parent = findParagraph(document.getSelection().anchorNode);
			    if (!parent) {
			    	document.execCommand('formatBlock', false, '<p>');
			    }
			}
		});

		var shell = textElement.wysiwyg('shell');
		var shellElement = $(shell.getElement());
		var shellActive = true;

		shellElement.addClass('html-content');

		if (scope.field.height) {
			shellElement.parent().height(height).resizable({
				handles: 's',
				resize: function () {
					shellElement.parent().width('');
				}
			});
		}

		function onChange(e) {

			var value = shellActive ? shell.getHTML() : textElement.val();

			var old = scope.getValue() || null;
			var txt = scope.parse(value) || null;

			if (old === txt) {
				return;
			}

			scope.setValue(value, true);
			scope.applyLater();
		}

		scope.$render_editable = function () {
			var value = scope.getValue() || "";
			scope.text = scope.format(value);

			var current = shellActive ? shell : textElement;
			var getter = shellActive ? 'getHTML' : 'val';
			var setter = shellActive ? 'setHTML' : 'val';

			var html = current[getter]();
			if (value !== html) {
				current[setter](value);
			}
		};

		textElement.on('input paste change blur', _.debounce(onChange, 100));

		textElement.on("focus", _.once(function (e) {

		    // Chrome and Edge supports this
			document.execCommand('defaultParagraphSeparator', false, 'p');

			// firefox uses attributes for some commands
			document.execCommand('styleWithCSS', false, true);
			document.execCommand('insertBrOnReturn', false, false);
		}));

		scope.toggleCode = function () {

			shellActive = !shellActive;

			element.parent().find('.wysiwyg-toolbar-icon')
				.toggleClass('disabled', !shellActive)
				.last().removeClass('disabled');

			if (shellActive) {
				textElement.hide();
				shellElement.show();
				shell.setHTML(textElement.val());
			} else {
				var height = Math.max(100, shellElement.outerHeight());
				shellElement.hide();
				textElement.show();

				if (!scope.field.height) {
					textElement.height(height);
				}
			}
		};

		scope.normalize = function () {

    		var html = shell.getHTML();
    		var div = $('<div>').html(html);

    		div.find('p').css({
    			'margin-top': 0,
    			'margin-bottom': '1em'});

    		div.find('ol,ul').each(function() {
    			var el = $(this);
    			if (el.parents('ol,ul').size()) return;
    			el.css({
	    			'margin-top': 0,
	    			'margin-bottom': '1em'});
    		});

    		div.find('blockquote').each(function() {
    			var el = $(this);
    			el.css({
	    			'margin': el.parents('blockquote').size() ? '0 0 0 2em' : '0 0 1em 2em',
	    			'border': 'none',
	    			'padding': 0
	    		});
    		});

    		shellElement.focus();
    		shell.setHTML(div[0].innerHTML);

    		div.remove();
		};

		scope.validate = function(value) {
			if (_.isEmpty(value)) {
				return true;
			}
			var length = value.length,
				valid = true;

			if (minSize) {
				valid = length >= minSize;
			}
			if(valid && maxSize) {
				valid = length <= maxSize;
			}

			return valid;
		};

	},

	template_readonly:
	'<div class="form-item-container">'+
		'<div class="html-viewer html-content" ui-bind-template x-text="text" x-locals="record" x-live="field.live"></div>'+
	'</div>',
	template_editable:
	'<div class="form-item-container">'+
		'<textarea class="html-editor html-content"></textarea>'+
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

})();

