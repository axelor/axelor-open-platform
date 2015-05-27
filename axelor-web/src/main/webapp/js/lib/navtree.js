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
(function($, undefined) {

	var NavTree = function(element, options) {
		this.element = $(element);
		this.init(options);
	};
	
	NavTree.prototype = {
		
		constructor: NavTree,
		
		defaults: {
			idField: 'id',
			onLazyFetch: function(parent, successFn){},
			onClick: function(e, item){}
		},

		init: function(options) {
			this.opts = $.extend({}, this.defaults, options),
			this.ul = $('<ul class="nav nav-list nav-tree"></ul>').appendTo(this.element);
			
			if (this.opts.onClick) {
				this.element.on('navtree:click', this.opts.onClick);
			}
		},
		
		addItems: function(items, parent) {
			var self = this;
			var after = parent;
			
			$(items).each(function(){
				var el = self.addItem(this);
				if (after) {
					after.after(el);
				} else
					self.ul.append(el);
				after = el;
			});
			
		},
		
		addItem: function(item) {
		
			var self = this;
			var pid = item.parent;
			
			var el = $('<a href=""></a>').click(function(e){
				e.preventDefault();
			});
			var li = $('<li></li>').click(function(e){
				var el = $(this);
				self.selectItem(el);
				self.toggleItem(el);

				el.trigger('navtree:click', el.data('record'));
			});
			
			li.keydown(function(e){
				var elem = $(this),
					next = null;
				
				if (e.keyCode == 9 && !elem.is(this.selectedItem))
					next = elem;
				
				else if (e.keyCode == 40)
					next = elem.nextAll(':visible:first');

				else if (e.keyCode == 38)
					next = elem.prevAll(':visible:first');

				if (next && next.size()) {
					e.preventDefault();
					self.selectItem(next);
				}
			});

			var icon = "img/tree-document.png";
			if (item.isFolder) {
				$('<i class="fa fa-caret-right handle"></i>').appendTo(el);
				icon = "img/tree-folder.png";
			}
			if (item.icon) {
				icon = item.icon;
			}
			if (icon.indexOf('fa-') === 0) {
				$('<i class="icon fa">').addClass(icon).appendTo(el);
			} else {
				$('<img>').attr('src', item.icon || icon).appendTo(el);
			}
			el.append(item.title);

			li.attr('data-id', item[this.opts.idField]);
			li.attr('data-parent', item.parent);
			
			var parent = this.$findItem(pid);
			var padding = 0;
			if (parent) {
				var state = parent.data('state');
				if (state)
					padding = state.padding + 36;
			}
			if (item.isFolder && padding > 0)
				padding -= 18;
			
			li.data('state', {
				isOpen: false,
				isLoaded: false,
				isFolder: item.isFolder,
				padding: padding
			}).data('record', item);
			
			el.css('padding-left', padding);
			
			return li.append(el);
		},
		
		$findItem: function(id) {
			return this.ul.children('li[data-id=' + id + ']');
		},

		$findChildren: function(id) {
			var parent = this.$findItem(id),
				id = parent.attr('data-id');
			return parent.nextAll('li[data-parent=' + id + ']');
		},
		
		selectItem: function(item) {
			var el = $(item);
			if (this.selectedItem) {
				this.selectedItem.removeClass('active');
			}
			this.selectedItem = el.addClass('active');
			el.find('a').focus();
			el.trigger('navtree:select', el.data('record'));
		},

		toggleItem: function(item, openState) {
			var el = $(item),
				state = el.data('state');

			openState = openState === undefined ? !state.isOpen : openState;
			
			if (!state.isFolder || state.isLoading || openState === state.isOpen)
				return;
			
			var self = this,
				id = el.attr('data-id');
				record = el.data('record');
			
			if (openState && !state.isLoaded) {

				state.isLoading = true;
				el.find('i.handle').attr('class', 'fa fa-spinner handle');

				return this.opts.onLazyFetch(record, function success(items) {

					self.addItems(items, el);

					state.isOpen = true;
					state.isLoaded = true;
					el.find('i.handle').attr('class', 'fa fa-caret-down handle');

					delete state.isLoading;

					el.trigger('navtree:expand', record);
				});
			}

			var children = this.$findChildren(id);
			if (openState) {
				children.filter('li[data-parent=' + id + ']').show();
				el.find('i.handle').attr('class', 'fa fa-caret-down handle');
			} else {
				children.hide().each(function(){
					self.toggleItem(this, false);
				});
				children.add(el).find('i.handle').attr('class', 'fa fa-caret-right handle');
			}
			state.isOpen = openState;

			el.trigger(openState ? 'navtree:expand' : 'navtree:collapse', record);
		}
	};
	
	$.fn.navtree = function (options) {
	var params = [].slice.call(arguments, 1);
	return this.each(function () {
	  var $this = $(this)
	    , data = $this.data('nav-tree');
	  if (!data) $this.data('nav-tree', (data = new NavTree(this, options)));
	  if (typeof options == 'string') data[options].apply(data, params);
	});
  };

  $.fn.navtree.Constructor = NavTree;

})(window.jQuery);

