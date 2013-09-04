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
