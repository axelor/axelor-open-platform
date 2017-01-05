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
(function($, undefined){

	var BSTabs = function(element) {
		this.element = $(element);
		this._setup();
	};
	
	BSTabs.prototype = {
		
		constructor: BSTabs,
		
		_setup: function() {
			
			this.$elemStrip = this.element.find('.nav-tabs-strip:first');
		    this.$elemLeftScroller = this.element.find('.nav-tabs-scroll-l:first');
		    this.$elemRightScroller = this.element.find('.nav-tabs-scroll-r:first');
		    this.$elemMenu = this.element.find('.nav-tabs-menu:first');

		    this.$elemTabs = this.element.find('.nav-tabs:first').addClass('nav-tabs-scrollable');
		    
		    if (this.$elemMenu.length) {
		    	this.$elemRightScroller.css('right', 16);
		    }

		    var self = this;
		    this.$elemLeftScroller.click(function(){
		    	self._scrollLeft();
		    	return false;
		    });
		    this.$elemRightScroller.click(function(){
		    	self._scrollRight();
		    	return false;
		    });
		    
		    this.element.on('adjustSize', function(event){
		    	self._adjustScroll();
		    });
		     
		    this.element.on('adjust', function(event){
		    	event.stopPropagation();
		    	var tab = self.$elemTabs.find('> li.active');
				if (tab) {
					self._adjustTab(tab, true);
				}
				self._adjustScroll();
		    });
		},
		
		_getTabsWidth: function() {
			var widthTabs = 0;
			this.$elemTabs.find('> li:visible').each(function(){
				widthTabs += $(this).outerWidth(true);
			});
			return widthTabs;
		},
		
		_scrollLeft: function() {
			if (this.$elemLeftScroller.hasClass('disabled'))
				return;
			
			var x = this.$elemTabs.position().left;
		    var scrollTo = Math.min(0, x + 100);
		    
		    this._scrollTabs(scrollTo, true);
		},
		
		_scrollRight: function() {
			if (this.$elemRightScroller.hasClass('disabled'))
				return;
			
			var x = this.$elemTabs.position().left;
			var w = this._getTabsWidth();
			
			var mx = - (w - this.$elemStrip.width());
			
		    var scrollTo = Math.max(mx, x - 100);
		    
		    this._scrollTabs(scrollTo, true);
		},
		
		_scrollTabs: function(scrollTo, animate) {
			if (animate) {
				var self = this;
				return this.$elemTabs.animate({
					'left': scrollTo
				}, 300, function() {
					self._activateScrollers();
				});
			}
			this.$elemTabs.css('left', scrollTo);
			this._activateScrollers();
		},
		
		_activateScrollers: function() {
		
			if (this.$elemTabs.position().left < 0) {
				this.$elemLeftScroller.removeClass('disabled');
			} else {
				this.$elemLeftScroller.addClass('disabled');
			}
			
			if (this._getTabsWidth() + this.$elemTabs.position().left > this.$elemStrip.width()) {
				this.$elemRightScroller.removeClass('disabled');
			} else {
				this.$elemRightScroller.addClass('disabled');
			}
		},
		
		_adjustTab: function(tab, animate) {
		
			if (!$(tab).size()) return;

            var w = this.$elemStrip.innerWidth();
            var scrollTo = this.$elemTabs.position().left;
            
            var left = $(tab).position().left + scrollTo;
            var right = left + $(tab).width();

            if (left < 0) {
                scrollTo -= left;
            } else if (right > w){
                scrollTo -= right - w;
            }
			
			this._scrollTabs(scrollTo, animate);
		},
		
		_adjustScroll: function() {
		
			var widthStrip = this.$elemStrip.width();
			var widthTabs = this._getTabsWidth();
			
			if (widthStrip >= widthTabs) {
				this.$elemLeftScroller.hide();
				this.$elemRightScroller.hide();
				this.$elemMenu.hide();
				this.$elemStrip.css('margin', '0');
				this.$elemTabs.css('left', 0);
			} else {
				this.$elemLeftScroller.show();
				this.$elemRightScroller.show();
				this.$elemMenu.show();
				this.$elemStrip.css('margin', this.$elemMenu.length ? '0 32px 0 16px' : '0 16px');
				var left = this.$elemTabs.position().left;
				var right = widthTabs + left;
				
				if (right < widthStrip) {
				    this.$elemTabs.css('left', left + (widthStrip - right));
				} else {				
					var tab = this.$elemTabs.find('> li.active');
					if (tab) {
						this._adjustTab(tab);
					}
				}
			}
		}
	};
	
	$.fn.bsTabs = function () {
		return this.each(function () {
			var $this = $(this),
				data = $this.data('bsTabs');
			if (!data) $this.data('bsTabs', (data = new BSTabs(this)));
		});
	};

	$.fn.bsTabs.Constructor = BSTabs;
	
}(jQuery));


