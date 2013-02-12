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

	'lib/slickgrid/lib/jquery.event.drag-2.0.min.js',
	'lib/slickgrid/slick.core.js',
	'lib/slickgrid/slick.grid.js',
	'lib/slickgrid/slick.dataview.js',
	'lib/slickgrid/plugins/slick.rowselectionmodel.js',
	'lib/slickgrid/plugins/slick.checkboxselectcolumn.js',
	
	'lib/ace/js/ace.js',
	
	'lib/angular/angular.js',
	'lib/angular/angular-resource.js',
	'lib/angular/angular-sanitize.js',
	
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
	
	'js/widget/widget.navtabs.js',
	'js/widget/widget.navtree.js',
	'js/widget/widget.splitter.js',
	'js/widget/widget.slickgrid.js',
	'js/widget/widget.dialog.js',

	'js/form/form.base.js',
	'js/form/form.actions.js',
	'js/form/form.widget.js',
	'js/form/form.layout.js',
	'js/form/form.container.js',
	'js/form/form.input.js',
	'js/form/form.relational.js',
	'js/form/form.code.js',
	
	'js/view/view.base.js',
	'js/view/view.form.js',
	'js/view/view.grid.js',
	'js/view/view.html.js',
	'js/view/view.search.js',
	'js/view/view.portal.js',
	'js/view/view.popup.js'
	//-- js-end
]);

})(this);
