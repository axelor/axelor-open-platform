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
(function(){

	function load(paths) {
		for(var i = 0 ; i < paths.length ; i++) {
			var path = paths[i],
			elem = document.createElement('script');
			elem.src = path;
			document.write(outerHTML(elem));
		}
	}

	function outerHTML(node){
		// if IE, Chrome take the internal method otherwise build one
		return node.outerHTML || (
		    function(n){
		        var div = document.createElement('div'), h;
		        div.appendChild(n);
		        h = div.innerHTML;
		        div = null;
		        return h;
		    })(node);
	}

// make sure i18n is loaded
if (this._t === undefined) {
	this._t = function(key) { return key; };
	this._t.put = function() {};
}

load([
	//-- js-begin
	'lib/underscore/underscore.js',
	'lib/underscore/underscore.string.js',
	'lib/moment/moment.js',

	'lib/jquery.ui/js/jquery.js',
	'lib/jquery.ui/js/jquery-ui.js',
	'lib/jquery.ui/js/jquery.ui.mask.js',
	'lib/bootstrap/js/bootstrap.js',
	'lib/jquery.timepicker/jquery-ui-timepicker-addon.js',

	'lib/slickgrid/lib/jquery.event.drag-2.0.js',
	'lib/slickgrid/slick.core.js',
	'lib/slickgrid/slick.grid.js',
	'lib/slickgrid/slick.dataview.js',
	'lib/slickgrid/slick.groupitemmetadataprovider.js',
	'lib/slickgrid/plugins/slick.headermenu.js',
	'lib/slickgrid/plugins/slick.rowselectionmodel.js',
	'lib/slickgrid/plugins/slick.checkboxselectcolumn.js',

	'lib/jquery.treetable/js/jquery.treetable.js',

	'lib/fullcalendar/fullcalendar.js',
	'lib/fullcalendar/gcal.js',

	'lib/d3/d3.v3.js',
	'lib/d3/nv/nv.d3.js',
	'lib/d3/radar/radar-chart.js',
	'lib/d3/gauge/gauge-chart.js',

	'lib/codemirror/codemirror.min.js',
	'lib/wysiwyg/wysiwyg.js',
	'lib/wysiwyg/wysiwyg-editor.js',

	'lib/angular/angular.js',
	'lib/angular/angular-resource.js',
	'lib/angular/angular-sanitize.js',
	'lib/angular/angular-if.js',

	'js/axelor.ng.js',
	
	'js/lib/utils.js',
	'js/lib/dialogs.js',
	'js/lib/tabs.js',
	'js/lib/navtree.js',

	'js/axelor.auth.js',
	'js/axelor.app.js',
	'js/axelor.ds.js',
	'js/axelor.data.js',
	'js/axelor.ui.js',
	'js/axelor.nav.js',
	'js/axelor.prefs.js',

	'js/widget/widget.navtabs.js',
	'js/widget/widget.navtree.js',
	'js/widget/widget.navmenu.js',
	'js/widget/widget.slickgrid.js',
	'js/widget/widget.dialog.js',
	'js/widget/widget.update.js',
	'js/widget/widget.search.js',
	'js/widget/widget.menubar.js',

	'js/form/form.base.js',
	'js/form/form.converters.js',
	'js/form/form.actions.js',
	'js/form/form.widget.js',
	'js/form/form.layout.js',
	'js/form/form.container.js',

	'js/form/form.input.static.js',
	'js/form/form.input.boolean.js',
	'js/form/form.input.text.js',
	'js/form/form.input.html.js',
	'js/form/form.input.number.js',
	'js/form/form.input.datetime.js',
	'js/form/form.input.select.js',
	'js/form/form.input.progress.js',
	'js/form/form.input.binary.js',

	'js/form/form.relational.base.js',
	'js/form/form.relational.single.js',
	'js/form/form.relational.multiple.js',
	'js/form/form.relational.nested.js',

	'js/form/form.mail.js',
	'js/form/form.code.js',

	'js/view/view.base.js',
	'js/view/view.form.js',
	'js/view/view.grid.js',
	'js/view/view.tree.js',
	'js/view/view.html.js',
	'js/view/view.search.js',
	'js/view/view.portal.js',
	'js/view/view.dashboard.js',
	'js/view/view.popup.js',
	'js/view/view.chart.js',
	'js/view/view.calendar.js'
	//-- js-end
]);

})(this);
