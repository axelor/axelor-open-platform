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
(function($, undefined) {

    var Splitter = function(element, options) {
        this.element = $(element);
        this.init(options);
    };

    Splitter.prototype = {

        constructor: Splitter,

        defaults: {
            // splitter position
            position: 200,
            
            // toggle effect duration
            duration: 200,
            
            // whether to keep splitter on opposite side
            inverse: false,

            // live drag updates
            live: true,
            
            // orientation (vertical or horizontal),
            orientation: 'vertical',
            
            // toggle on event (if null then disable toggle)
            toggleOn: 'dblclick'
        },

        init: function(options) {

            var opts = this.opts = $.extend({}, this.defaults, options),
                horz = this.horz = opts.orientation == 'horizontal',
                css = 'splitter-' + (horz ? 'horizontal' : 'vertical'),
                axis = horz ? 'y' : 'x';

            this.element.addClass('splitter-container splitter-panel').css('overflow', 'hidden');

            this.elemOne = this.element.children('div:first').addClass('splitter-panel');
            this.elemTwo = this.element.children('div:last').addClass('splitter-panel');

            this.elemOne.add(this.elemTwo).children('.splitter-center').each(function() {
                var center = $(this);
                if (center.prev().is('.splitter-header')) center.css('top', center.prev().outerHeight(true));
                if (center.next().is('.splitter-footer')) center.css('bottom', center.next().outerHeight(true));
            });

            var dragger = this.element.find('> .' + css);
            if (dragger.size() === 0) {
                dragger = $('<div></div>').addClass(css).appendTo(this.element);
                $('<span class="splitter-handle"></span>').appendTo(dragger);
            }

            var pos = +opts.position,
                side = opts.inverse === true ? (horz ? 'bottom' : 'right') : (horz ? 'top' : 'left'),
                self = this;

            var draggerOpts = {
                axis: axis,
                containment: 'parent'
            };

            if (!opts.live) {
                draggerOpts.helper = 'clone';
            }

            this.dragger = dragger.draggable(draggerOpts)
            .on('drag', function(event, ui) {
                if (opts.live) {
                    self._adjust(true);
                    self._collapsed = false;
                }
                self.element.trigger('splitter:drag');
            })
            .on('dragstart', function(event, ui) {
                self.element.trigger('splitter:dragstart');
            })
            .on('dragstop', function(event, ui) {
                if (opts.live) {
                    self.element.trigger('splitter:dragstop');
                } else {
                    var side = self.horz ? 'top' : 'left';
                    self._animate(side, ui.offset[side], function() {
                        self._collapsed = false;
                        self.element.trigger('splitter:dragstop');
                    });
                }
            });

            this.dragger.css(side, pos);
            this._adjust();
            
            opts.evtHandler = $(window).resize(function() {
                self._adjust();
            });

            var toggleOn = opts.toggleOn;
            if (typeof toggleOn == 'string') {
                dragger.on(toggleOn, function(e) {
                    self.toggle();
                });
            }
        },

        _animate: function(side, position, complete) {

            var that = this,
                effect = {},
                params = {};

            effect[side] = position;
            params['duration'] = this.opts.duration || 200;
            params['step'] = function() {
                that._adjust(false);
            };
            params['complete'] = function() {
                that._adjust(true);
                if (complete) complete.call();
            };

            this.dragger.animate(effect, params);
        },

        toggle: function() {

            var opts = this.opts,
                side = this.horz ? (opts.inverse ? 'bottom' : 'top') : (opts.inverse ? 'right' : 'left');

            if (this._collapsed != true) {
                this._lastPosition = parseInt(this.dragger.css(side));
            }
            if (this._lastPosition == undefined) {
                this._lastPosition = this.opts.position;
            }

            this._collapsed = !this._collapsed;

            if (this._lastPosition < 1) {
                this._lastPosition = this.opts.position;
                this._collapsed = false;
            }

            var that = this,
                pos = this._collapsed ? 0 : this._lastPosition;
            this._animate(side, pos, function() {
                that.element.trigger('splitter:dragstop');
            });
        },

        _adjust: function(notify) {

            var el = this.dragger,
                that = this,
                opts = this.opts,
                sideProp = this.horz ? 'top' : 'left',
                sizeProp = this.horz ? 'height' : 'width';

            setTimeout(function() {
                var pos = el.position(),
                    dim = el[sizeProp](),
                    x = Math.max(0, pos[sideProp]),
                    y = Math.max(dim, x + dim);

                x = Math.round(x);
                y = Math.round(y);
                
                if (y === dim) {
                    el.css(sideProp, 0);
                }

                that.elemOne[sizeProp](x);
                that.elemTwo.css(sideProp, y);

                // handle inverse resize
                if (opts.inverse && x > 0) {
                    el.css(that.horz ? 'bottom' : 'right', that.elemTwo[sizeProp]());
                    el.css(that.horz ? 'top' : 'left', '');
                }

                if (notify) {
                    $.event.trigger('splitter:adjust', {
                        element: that.element
                    });
                }
            });
        },

        destroy: function() {
            $(window).unbind('resize', this.options.evtHandler);
        }
    };

    $.fn.splitter = function(options) {
        return this.each(function() {
            var $this = $(this),
                data = $this.data('splitter');
            if (!data) $this.data('splitter', (data = new Splitter(this, options)));
            if (typeof options == 'string') data[options]();
        });
    };

    $.fn.splitter.Constructor = Splitter;

})(window.jQuery);
