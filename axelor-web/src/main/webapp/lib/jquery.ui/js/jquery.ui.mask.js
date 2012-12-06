/*
 * jQuery UI Mask @VERSION
 *
 * Copyright (c) 2009 AUTHORS.txt (http://jqueryui.com/about)
 * Dual licensed under the MIT (MIT-LICENSE.txt)
 * and GPL (GPL-LICENSE.txt) licenses.
 *
 * Based on the jquery.maskedinput.js plugin by Josh Bush (digitalbush.com)
 *
 * http://docs.jquery.com/UI/Mask
 *
 * Depends:
 *   ui.core.js
 */

//TODO: implemented in jquery.ui v1.9 (upgrade)

(function($) {

var pasteEventName = ($.browser.msie ? 'paste' : 'input') + ".mask";

$.widget("ui.mask", {
	
	_init: function() {

		var options = this.options, self = this;

		$.extend(this, { caret: function(begin, end){ return $.ui.mask.caret(self.element, begin, end); } });

		if(!options.mask || !options.mask.length){ return; } //no mask pattern defined. no point in continuing.
		if(!options.placeholder || !options.placeholder.length){
			options.placeholder = '_'; //in case the user decided to nix a placeholder. 
		}

		this._prepareBuffer();
		this._bindEvents();
		this._checkVal((this.element.val().length && options.allowPartials)); //Perform initial check for existing values
	},

	destroy: function() {
		this.element
			.unbind('.mask')
			.removeData('mask');
	},

	value: function() {
		var input = this.element,
			self = this,
			res = $.map(self.buffer, function(c, i){ return self.tests[i] ? c : null; }).join(''),
			r = new RegExp('\\'+this.options.placeholder, 'gi');
		return res.replace(r, '');
	},
	
	formatted: function(){
		var r = new RegExp('\\'+this.options.placeholder, 'gi'),
			res = this.element.val();
		return res.replace(r, '');
	},
	
	apply: function(){
		this.element.trigger('apply.mask');
	},
	
	_setData: function(key, value) {

		$.widget.prototype._setData.apply(this, arguments);
		var options = this.options;

		switch (key) {
			case 'mask':
				//no mask pattern defined. no point in continuing.
				if(!options.mask || !options.mask.length){ 
					this.element.unbind('.mask')
					break;
				}
			case 'placeholder':
				if(!options.placeholder || !options.placeholder.length){
					options.placeholder = '_'; //in case the user decided to nix a placeholder.
				}
				this.element.val('');
				this._prepareBuffer();
				!this.eventsBound && this._bindEvents();
				break;
		}

	},

	_prepareBuffer: function(){
		
		this._escapeMask();

		var self = this, 
			input = this.element,
			options = this.options,
			mask = options.mask,
			defs = $.ui.mask.definitions,
			tests = [],
			partialPosition = mask.length,
			firstNonMaskPos = null,
			len = mask.length;

		//if we're applying the mask to an element which is not an input, it won't have a val() method. fake one for our purposes.
		if(!input.is(':input')) input.val = input.html;

		$.each(mask.split(""), function(i, c) {
			if (c == '?') {
				len--;
				partialPosition = i;
			}
			else if (defs[c]){
				tests.push(new RegExp(defs[c]));
				if(firstNonMaskPos==null)
					firstNonMaskPos =  tests.length - 1;
			}
			else{
				tests.push(null);
			}
		});

		$.extend(this, {
			buffer: $.map(mask.split(""), function(c, i){
				if (c != '?'){
					return defs[c] ? options.placeholder : c;
				}
			}),
			tests: tests,
			firstNonMaskPos: firstNonMaskPos,
			partialPosition: partialPosition
		});

		this.buffer = $.map(this.buffer, function(c, i){
			if(c == "\t"){
				return self.maskEscaped[i];
			}
			return c;
		});

		this.options.mask = mask = this.maskEscaped;		
	},	

	_bindEvents: function(){
		
		var self = this,
			input = this.element,
			ignore = false,  			//Variable for ignoring control keys
			focusText = input.val();
			
		function keydownEvent(e) {
			e = e || window.event;
			var pos = self.caret(),
				k = e.keyCode,
				keyCode = $.ui.keyCode;
				
			ignore = (k < keyCode.SHIFT || (k > keyCode.SHIFT && k < keyCode.SPACE) || (k > keyCode.SPACE && k < 41));

			//delete selection before proceeding
			if ((pos.begin - pos.end) != 0 && (!ignore || k == keyCode.BACKSPACE || k == keyCode.DELETE))
				self._clearBuffer(pos.begin, pos.end);

			//backspace, delete, and escape get special treatment
			if (k == keyCode.BACKSPACE || k == keyCode.DELETE) {//backspace/delete
				self._shiftL(pos.begin + ((k == keyCode.DELETE || (k == keyCode.BACKSPACE && pos.begin!=pos.end))  ? 0 : -1), Math.abs(pos.begin - pos.end));
				return false;
			}
			else if (k == keyCode.ESCAPE) {//escape
				input.val(focusText);
				self.caret(0, self._checkVal());
				return false;
			}
		};

		function keypressEvent(e) {

			e = e || window.event;
			
			var k = e.charCode || e.keyCode || e.which, 
				keyCode = $.ui.keyCode,
				len = self.options.mask.length;

			if (ignore) {
				ignore = false;
				//Fixes Mac FF bug on backspace
				return (e.keyCode == keyCode.BACKSPACE) ? false : null;
			}
			
			var pos = self.caret();

			if (e.ctrlKey || e.altKey || e.metaKey) {//Ignore
				return true;
			}
			else if ((k >= keyCode.SPACE && k <= 125) || k > 186) {//typeable characters
				var p = self._seekNext(pos.begin - 1);
				if (p < len) {
					var c = String.fromCharCode(k);
					if (self.tests[p] && self.tests[p].test(c)) {
						self._shiftR(p);
						self.buffer[p] = c;
						self._writeBuffer();
						var next = self._seekNext(p);
						self.caret(next);
						self.options.completed && next == len && self.options.completed.call(input);
					}
				}
			}
			return false;
		};

		if (!input.attr("readonly")){
			input
				.bind("focus.mask", function() {
					focusText = input.val();
					var pos = self._checkVal();
					self._writeBuffer();
					setTimeout(function() {
						if (pos == self.options.mask.length)
							self.caret(0, pos);
						else
							self.caret(pos);
					}, 0);
				})
				.bind("blur.mask", function() {
					self._checkVal();
					if (input.val() != focusText)
						input.change();
				})
				.bind('apply.mask', function(){ //changing the value of an input without keyboard input requires re-applying the mask.
					focusText = input.val();
					var pos = self._checkVal();
					self._writeBuffer();					
				})				
				.bind("keydown.mask", keydownEvent)
				.bind("keypress.mask", keypressEvent)
				.bind(pasteEventName, function() {
					setTimeout(function() { self.caret(self._checkVal(true)); }, 0);
				});
			this.eventsBound = true;
		}
	},
	
	_writeBuffer: function(){
		return this.element.val(this.buffer.join('')).val(); 
	},
	
	_clearBuffer: function(start, end){
		var len = this.options.mask.length;
		for (var i = start; i < end && i < len; i++) {
			if (this.tests[i])
				this.buffer[i] = this.options.placeholder;
		}
	},
	
	_seekNext: function(pos){
		var len = this.options.mask.length;
		while (++pos <= len && !this.tests[pos]);
		return pos;
	},
	
	_shiftR: function(pos){
		var len = this.options.mask.length;
		for (var i = pos, c = this.options.placeholder; i < len; i++) {
			if (this.tests[i]) {
				var j = this._seekNext(i);
				var t = this.buffer[i];
				this.buffer[i] = c;
				if (j < len && this.tests[j].test(t)){
					c = t;
				}
				else{
					break;
				}
			}
		}
	},
	
	_shiftL: function(pos, length){

		while (!this.tests[pos] && --pos >= 0);
	
		var originalPos = pos,
			len = this.options.mask.length,
			placeholder = this.options.placeholder;
		
		for(var i = pos; i < len && (i >= 0 || length > 1); i++) {
			if (this.tests[i]) {
				this.buffer[i] = placeholder;
				var j = this._seekNext(i);
				if (j < len && this.tests[i].test(this.buffer[j])) {
					this.buffer[pos] = this.buffer[j];
					this.buffer[j] = placeholder;
					pos++;
					while(!this.tests[pos]) pos++;
				}
			}
		}			
		this._writeBuffer();
		this.caret(Math.max(this.firstNonMaskPos, originalPos));
	},
	
	_checkVal: function(allow){
		//try to place characters where they belong
		var input = this.element,
			test = input.val(),
			len = this.options.mask.length,
			lastMatch = -1;
			
		for (var i = 0, pos = 0; i < len; i++) {
			if (this.tests[i]) {
				this.buffer[i] = this.options.placeholder;
				while (pos++ < test.length) {
					var c = test.charAt(pos - 1);
					if (this.tests[i].test(c)) {
						this.buffer[i] = c;
						lastMatch = i;
						break;
					}
				}
				if (pos > test.length)
					break;
			}
			else if (this.buffer[i] == test[pos] && i != this.partialPosition) {
				pos++;
				lastMatch = i;
			} 
		}
		if (!allow && lastMatch + 1 < this.partialPosition) {
			if(!this.options.allowPartials || !this.value().length){
				input.val("");
				this._clearBuffer(0, len);
			}
			else //if we're allowing partial input/inital values, and the element we're masking isnt an input, then we need to allow the mask to apply.
				if(!input.is(':input')) this._writeBuffer();
				
		}
		else if (allow || lastMatch + 1 >= this.partialPosition) {
			this._writeBuffer();
			if (!allow) input.val(input.val().substring(0, lastMatch + 1));
		}
		return (this.partialPosition ? i : this.firstNonMaskPos);
	},
	
	_escapeMask: function(){
		var mask = this.options.mask,
			literals = [],
			replacements = [];
				
		for(var i = 0; i < mask.length; i++){
			var c, temp = mask[i] || mask.charAt(i);
			if(temp != "\\" || mask[i-1] == "\\"){
				if(mask[i-1] == "\\"){
					c = "\t";
					replacements[literals.length] = temp;
				}
				else{
					c = temp;
				}
				literals[literals.length] = c;
			}
		}
		
		this.options.mask = literals.join('');
		
		for(var i = 0; i < literals.length; i++){
			if(replacements[i] !== undefined){
				literals[i] = replacements[i];
			}
		}
		
		this.maskEscaped = literals.join('');
	}	
	
});

$.extend($.ui.mask, {
	version: "@VERSION",
	getter: "value formatted",
	defaults: {
		mask: '',
		placeholder: '_',
		completed: null,
		allowPartials: false
	},
	definitions: { //Predefined character definitions
		'#': "[\\d]",
		'a': "[A-Za-z]",
		'*': "[A-Za-z0-9]"
	},
	caret: function(element, begin, end) {	//Helper Function for Caret positioning
		var input = element[0];
		if (typeof begin == 'number') {
			end = (typeof end == 'number') ? end : begin;
			if (input.setSelectionRange) {
				input.focus();
				input.setSelectionRange(begin, end);
			} else if (input.createTextRange) {
				var range = input.createTextRange();
				range.collapse(true);
				range.moveEnd('character', end);
				range.moveStart('character', begin);
				range.select();
			}
			return element;
		} else {
			if (input.setSelectionRange) {
				begin = input.selectionStart;
				end = input.selectionEnd;
			}
			else if (document.selection && document.selection.createRange) {
				var range = document.selection.createRange();
				begin = 0 - range.duplicate().moveStart('character', -100000);
				end = begin + range.text.length;
			}
			return { begin: begin, end: end };
		}
	}		
});

})(jQuery);