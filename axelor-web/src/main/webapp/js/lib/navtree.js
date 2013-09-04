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
				$('<i class="icon-caret-right"></i>').appendTo(el);
				icon = "img/tree-folder.png";
			}
			$('<img>').attr('src', item.icon || icon).appendTo(el);
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
			if (this.selectedItem)
				this.selectedItem.removeClass('active');
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
				el.find('i').attr('class', 'icon-spinner');

				return this.opts.onLazyFetch(record, function success(items) {

					self.addItems(items, el);

					state.isOpen = true;
					state.isLoaded = true;
					el.find('i').attr('class', 'icon-caret-down');

					delete state.isLoading;

					el.trigger('navtree:expand', record);
				});
			}

			var children = this.$findChildren(id);
			if (openState) {
				children.filter('li[data-parent=' + id + ']').show();
				el.find('i').attr('class', 'icon-caret-down');
			} else {
				children.hide().each(function(){
					self.toggleItem(this, false);
				});
				children.add(el).find('i').attr('class', 'icon-caret-right');
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

