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

	'lib/ace/js/ace.js',
	'lib/tinymce/tinymce.min.js',

	'lib/angular/angular.js',
	'lib/angular/angular-resource.js',
	'lib/angular/angular-sanitize.js',
	'lib/angular/angular-if.js',

	'js/lib/utils.js',
	'js/lib/dialogs.js',
	'js/lib/tabs.js',
	'js/lib/navtree.js',
	'js/lib/splitter.js',

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
	'js/widget/widget.splitter.js',
	'js/widget/widget.slickgrid.js',
	'js/widget/widget.dialog.js',
	'js/widget/widget.search.js',
	'js/widget/widget.menubar.js',

	'js/form/form.base.js',
	'js/form/form.actions.js',
	'js/form/form.widget.js',
	'js/form/form.layout.js',
	'js/form/form.container.js',

	'js/form/form.input.static.js',
	'js/form/form.input.boolean.js',
	'js/form/form.input.text.js',
	'js/form/form.input.number.js',
	'js/form/form.input.datetime.js',
	'js/form/form.input.select.js',
	'js/form/form.input.progress.js',
	'js/form/form.input.binary.js',

	'js/form/form.relational.base.js',
	'js/form/form.relational.single.js',
	'js/form/form.relational.multiple.js',
	'js/form/form.relational.nested.js',
	'js/form/form.code.js',
	'js/form/form.tinymce.js',

	'js/view/view.base.js',
	'js/view/view.form.js',
	'js/view/view.grid.js',
	'js/view/view.tree.js',
	'js/view/view.html.js',
	'js/view/view.search.js',
	'js/view/view.portal.js',
	'js/view/view.popup.js',
	'js/view/view.chart.js',
	'js/view/view.calendar.js',
	'js/view/view.trail.js'
	//-- js-end
]);

})(this);
