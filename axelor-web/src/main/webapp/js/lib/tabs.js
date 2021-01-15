/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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

        var _onResize = _.debounce(function () { self._adjustScroll(); }, 300);
        var _onAdjustSize = _.debounce(function () { self._adjustScroll(); });
        var _onAdjust = function(event) {
          event.stopPropagation();
          setTimeout(function (){
            self._adjustScroll();
          });
        };

        $(window).on('resize', _onResize);
        $(document).on('adjust:size', _onAdjustSize);

        this.element.on('adjust:tabs', _onAdjust);

        this.$elemTabs.on("click", " > li > a", function(event){
          self._adjustTab($(this).parent(), true);
        });

        this.element.on('$destroy', function () {
          $(window).off('resize', _onResize);
          $(document).off('adjust:size', _onAdjustSize);
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
      if (scrollTo === this._lastScrollTo) {
        return;
      }

      this._lastScrollTo = scrollTo;

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

      if (this._getTabsWidth() + this.$elemTabs.position().left > this.$elemStrip.width() + 1) {
        this.$elemRightScroller.removeClass('disabled');
      } else {
        this.$elemRightScroller.addClass('disabled');
      }
    },

    _adjustTab: function(tab, animate) {

      if (!$(tab).length) return;

            var w = this.$elemStrip.innerWidth();
            var scrollTo = this.$elemTabs.position().left;

            var left = $(tab).position().left + scrollTo;
            var right = left + $(tab).outerWidth(true);

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

      this.element.toggleClass("nav-tabs-overflow", widthStrip < widthTabs);

      var scrollTo = 0;

      if (widthStrip >= widthTabs) {
        this.$elemLeftScroller.hide();
        this.$elemRightScroller.hide();
        this.$elemMenu.hide();
        this.$elemStrip.css('margin', '0');
      } else {
        this.$elemLeftScroller.show();
        this.$elemRightScroller.show();
        this.$elemMenu.show();
        this.$elemStrip.css('margin', this.$elemMenu.length ? '0 32px 0 16px' : '0 16px');

        var left = this.$elemTabs.position().left;
        var right = widthTabs + left;
        if (right < widthStrip) {
          scrollTo = left + (widthStrip - right);
        } else {
          var tab = this.$elemTabs.find('> li.active');
          if (tab) {
            return this._adjustTab(tab);
          }
        }
      }

      this._scrollTabs(scrollTo);
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

})();
