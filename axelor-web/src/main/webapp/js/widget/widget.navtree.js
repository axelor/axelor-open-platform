angular.module('axelor.ui').directive('navTree', function() {
	return {
		restrict: 'EA',
		require: '?ngModel',
		scope: {
			itemClick: '&' 
		},

		controller: ['$scope', '$element', 'MenuService', function($scope, $element, MenuService) {
			
			$scope.load = function(parent, successFn) {

				var name = parent ? parent.name : null;

				MenuService.get(name).success(function(res){
					var items = res.data;
					if (successFn) {
						return successFn(items);
					}
					$element.navtree('addItems', items);
				});
			};
		}],
		
		link: function(scope, elem, attrs) {
			
			var onClickHandler = scope.itemClick();

			$(elem).navtree({
				idField: 'name',
				onLazyFetch: function(parent, successFn) {
					scope.load(parent, successFn);
				},
				onClick: function(e, record) {
					if (record.isFolder)
						return;
					onClickHandler(e, record);
				}
			});

			scope.load();
		},
		template: '<div></div>',
		replace: true
	};
});
