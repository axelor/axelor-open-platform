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

var ui = angular.module('axelor.ui');

ui.formWidget('uiMailMessages', {
	scope: true,
	controller: ['$scope', 'DataSource', function ($scope, DataSource) {

		var dsFlags = DataSource.create('com.axelor.mail.db.MailFlags');

		$scope.onFlag = function (message, flagState) {
			var flags = _.extend({}, message.$flags);
			if (flagState === 1) flags.starred = true;
			if (flagState === -1) flags.starred = false;
			if (flagState === 2) flags.unread = false;
			if (flagState === -2) flags.unread = true;

			flags.message = _.pick(message, 'id');
			flags.user = {
				id: $scope.app.userId
			};
			dsFlags.save(flags).success(function (rec) {
				message.$flags = _.pick(rec, ['id', 'version', 'starred', 'unread', 'voted']);
				if ($scope.isInbox) {
					$scope.onLoadMessages();
				}
			});
		};

		$scope.onReply = function (message) {
			message.$reply = true;
			$scope.$broadcast("on:message-add");
		};

		$scope.formatEvent = function (message) {
			if (message.$eventLine) {
				return message.$eventLine;
			}
			var line = message.$eventText + " - " + moment(message.$eventTime).fromNow();
			message.$eventLine = line;
			return line;
		}
	}],
	link: function (scope, element, attrs) {

		var frame = element.find('iframe:first').hide();

		scope.onDownload = function (file) {
			var url = "ws/rest/com.axelor.meta.db.MetaFile/" + file.id + "/content/download";
			frame.attr("src", url);
			setTimeout(function(){
				frame.attr("src", "");
			}, 100);
		};
	},
	template_readonly: null,
	template_editable: null,
	template:
		"<div class='mail-messages panel panel-default span9'>" +
			"<div class='panel-body'>" +
				"<div class='mail-composer' ui-mail-composer></div>" +
				"<div class='mail-thread'>" +
				"<span ng-repeat='message in record.$messages'>" +
					"<div class='mail-message alert' ng-class='{ \"alert-info\": !message.parent || record.id > 0}'>" +
						"<div class='mail-message-left'>" +
							"<a href=''>" +
								"<img ng-src=''>" +
							"</a>" +
						"</div>" +
						"<div class='mail-message-center'>" +
							"<div class='mail-message-icons'>" +
								"<i class='fa fa-star-o' ng-show='!message.$flags.starred' ng-click='onFlag(message, 1)'></i> " +
								"<i class='fa fa-star' ng-show='message.$flags.starred' ng-click='onFlag(message, -1)'></i> " +
								"<i class='fa fa-reply' ng-show='message.$thread' ng-click='onReply(message)'></i> " +
								"<i class='fa fa-check' ng-show='message.$flags.unread' ng-click='onFlag(message, 2)'></i> " +
								"<i class='fa fa-upload' ng-show='!message.$flags.unread' ng-click='onFlag(message, -2)'></i>" +
							"</div>" +
							"<div class='mail-message-subject'>{{message.subject}}</div>" +
							"<div class='mail-message-body' ui-bind-template x-text='message.body'></div>" +
							"<div class='mail-message-files' ng-show='message.$files.length'>" +
								"<ul class='inline'>" +
									"<li ng-repeat='file in message.$files'>" +
										"<i class='fa fa-paperclip'></i> <a href='' ng-click='onDownload(file)'>{{file.fileName}}</a>" +
									"</li>" +
								"</ul>" +
							"</div>" +
							"<div class='mail-message-footer'>" +
								"<a href='' ng-click='showUser(message.author)'>{{message.author.name}}</a> " +
								"<span>{{formatEvent(message)}}</span>" +
							"</div>" +
						"</div>" +
					"</div>" +
					"<div ui-mail-composer ng-if='message.$thread'></div>" +
				"</span>" +
				"</div>" +
			"</div>" +
		"</div>"
});

ui.formWidget('uiMailComposer', {
	scope: true,
	require: '^uiMailMessages',
	controller: ['$scope', 'DataSource', function ($scope, DataSource) {

		function dataSource() {
			var msg = $scope.message;
			if (msg && msg.relatedModel) {
				return DataSource.create(msg.relatedModel);
			}
			return $scope._dataSource;
		}

		function dataRecord() {
			var msg = $scope.message;
			if (msg && msg.relatedModel) {
				return {
					id: msg.relatedId
				};
			}
			return $scope.record;
		}

		$scope.post = null;
		$scope.files = [];

		$scope.canShow = function () {
			var rec = $scope.record || {};
			var msg = $scope.message || {};
			return rec.id > 0 || msg.$reply;
		};

		$scope.canPost = function () {
			return $scope.post;
		};

		$scope.onPost = function () {

			var ds = dataSource();
			var record = dataRecord();
			var parent = _.pick($scope.message || {}, 'id');

			if (!record.id) {
				return;
			}

			var text = $scope.post.replace(/\n/g, '<br>');
			var files = _.pluck($scope.files, 'id');

			ds.messagePost(record.id, text, {
				parent: parent.id && parent,
				files: files
			}).success(function (res) {
				var message = _.first(res.data);
				var messages = record.$messages || [];

				messages.unshift(message);
				record.$messages = messages;
				$scope.post = null;
				$scope.files = [];

				if (parent.id && $scope.message) {
					$scope.message.$reply = false;
					return $scope.onNewHandler();
				}

				$scope.$broadcast('on:message-add');
			});
		};

	}],
	link: function (scope, element, attrs) {

		var textarea = element.find('textarea');
		var input = element.find('input[type=file]').hide();

		var uploadSize = scope.$eval('app.fileUploadSize');
		var fileModel = 'com.axelor.meta.db.MetaFile';

		textarea.on('blur', function () {
			if (scope.post || !scope.message) return;
			scope.message.$reply = false;
			scope.applyLater();
		});

		scope.$on('on:message-add', function () {
			if (scope.canShow()) {
				setTimeout(function () {
					textarea.focus();
				});
			}
		});

		scope.uploading = false;
		scope.onUpload = function() {
			input.click();
		};

		scope.onRemoveFile = function (file) {
			var i = scope.files.indexOf(file);
			if (i > -1) {
				scope.files.splice(i, 1);
			}
		};

		input.change(function(e) {

			var file = input.get(0).files[0];
			if (!file) {
				return;
			}

			if(file.size > 1048576 * parseInt(uploadSize)) {
				return axelor.dialogs.say(_t("You are not allow to upload a file bigger than") + ' ' + uploadSize + 'MB');
			}

		    var record = {
				fileName: file.name,
				mime: file.type,
				size: file.size,
				id: null,
				version: null
		    };

		    record.$upload = {
			    file: file
		    };

		    scope.uploading = true;

		    var newDS = scope._dataSource._new(fileModel, {});
		    newDS.save(record).progress(function(fn) {
			scope.uploading = fn < 100;
		    }).success(function(file) {
				scope.uploading = false;
				scope.files.push(file);
			}).error(function() {
				scope.uploading = false;
			});
		});
	},
	template: "" +
		"<div class='mail-composer' ng-show='canShow()'>" +
			"<textarea rows='1' ng-model='post' ui-textarea-auto-size class='span12' placeholder='Write your comment here'></textarea>" +
			"<input type='file' class='hidden'>" +
			"<iframe class='hidden'></iframe>" +
			"<div class='mail-composer-files' ng-show='files.length'>" +
				"<ul>" +
				"<li ng-repeat='file in files'>" +
					"<i class='fa fa-close' ng-click='onRemoveFile(file)'></i> <a href='' ng-click='onDowload(file)'>{{file.fileName}}</a>" +
				"</li>" +
				"</ul>" +
			"</div>" +
			"<div class='mail-composer-buttons' ng-show='canPost()'>" +
				"<button class='btn btn-primary' ng-click='onPost()' x-translate>Post</button>" +
				"<span class='btn btn-default' ng-click='onUpload()'>" +
					"<i class='fa fa-paperclip'></i>" +
				"</span>" +
			"</div>" +
		"</div>"
});

ui.formWidget('uiMailFollowers', {

	scope: true,
	controller: ['$scope', function ($scope) {

		var ds = $scope._dataSource;

		$scope.onFollow = function () {

			var record = $scope.record || {};
			var promise = ds.messageFollow(record.id);

			promise.success(function (res) {
				record.$followers = res.data;
				$scope.updateStatus();
			});
		};

		$scope.onUnfollow = function (user) {

			axelor.dialogs.confirm(_t('Are you sure to unfollow this document?'),
			function (confirmed) {
				if (confirmed) {
					doUnfollow(user);
				}
			});
		}

		function doUnfollow(user) {

			var record = $scope.record || {};
			var promise = ds.messageUnfollow(record.id, {
				records: _.compact([user])
			});

			promise.success(function (res) {
				record.$followers = res.data;
				$scope.updateStatus();
			});
		};

	}],
	link: function (scope, element, attrs) {

	},
	template_readonly: null,
	template_editable: null,
	template:
		"<div class='mail-followers panel panel-default span3'>" +
			"<div class='panel-header'>" +
			"<div class='panel-title'>" +
				"<span x-translate>Followers</span>" +
				"<div class='icons pull-right'>" +
					"<i class='fa fa-star-o' ng-click='onFollow()' ng-show='!record.$following'></i>" +
					"<i class='fa fa-star' ng-click='onUnfollow()' ng-show='record.$following'></i>" +
					"<i class='fa fa-plus' ng-click='onAddFollowers()'></i>" +
				"</div>" +
			"</div>" +
			"</div>" +
			"<div class='panel-body'>" +
			"<ul class='links'>" +
				"<li ng-repeat='follower in record.$followers'>" +
				"<a href='' ng-click='showUser(follower)'>{{follower.name}}</a> " +
				"<i class='fa fa-remove' ng-click='onUnfollow(follower)'></i>" +
				"</li>" +
			"</ul>" +
			"</div>" +
		"</div>"
});

ui.formWidget('PanelMail', {

	scope: true,
	controller: ['$scope', 'DataSource', 'ViewService', function ($scope, DataSource, ViewService) {

		var userEditor = null;
		var userSelector = null;

		$scope.editorCanSave = false;
		$scope._viewParams = {
			model: 'com.axelor.auth.db.User',
			viewType: 'form',
			views: [{type: 'form', type: 'grid'}]
		};

		$scope.getDomain = function () {
			return {};
		};

		$scope.updateStatus = function() {
			var record = $scope.record || {};
			var followers = record.$followers;

			var found = _.findWhere(followers, {
				code: $scope.app.login
			});

			record.$following = !!found;
		}

		$scope.select = function (records) {

			var ds = $scope._dataSource;
			var record = $scope.record || {};
			var promise = ds.messageFollow(record.id, {
				records: records
			});

			promise.success(function (res) {
				record.$followers = res.data;
				$scope.updateStatus();
			});
		};

		$scope.showUser = function (user) {

			if (!user || !user.id) {
				return;
			}

			if (userEditor == null) {
				userEditor = ViewService.compile('<div ui-editor-popup></div>')($scope);
			}

			var popup = userEditor.scope();
			popup.isEditable = function () {
				return false;
			};
			popup.show(user);
		};

		$scope.onAddFollowers = function () {

			if (userSelector == null) {
				userSelector = ViewService.compile('<div ui-selector-popup x-select-mode="multi"></div>')($scope);
			}

			var popup = userSelector.data('$scope');
			popup.show();
		};

		var messageDS = DataSource.create("com.axelor.mail.db.MailMessage");

		$scope.isInbox = $scope.tab && $scope.tab.action === "mail.inbox";

		$scope.onLoadMessages = function () {

			var record = $scope.record || {};
			var params = {};

			if (record.id > 0) {
				params.relatedId = record.id;
				params.relatedModel = $scope._model;
			}

			params.flag = $scope.isInbox;

			messageDS.messages(params).success(function (res) {
				var records = res.data;
				var first = _.first(records);

				if (record.id > 0 && first) {
					record.$following = first.$following;
					record.$followers = first.$followers;
					record.$messages = first.$messages;
				} else {
					record.__emptyTitle = first.values.__emptyTitle;
					record.__emptyDesc = first.values.__emptyDesc;
					record = _.extend(record, first.values);
				}
			});
		};

		$scope.$on("on:nav-click", function (e) {
			var rec = $scope.record;
			var tab = $scope.tab || {};
			if (tab.selected && rec) {
				$scope.onLoadMessages();
			}
		});
	}],
	link: function (scope, element, attrs) {
		var field = scope.field;
		if (field.items && field.items.length === 1) {
			setTimeout(function () {
				element.children('.mail-messages').removeClass('span9').addClass('span12');
				element.children('.mail-followers').removeClass('span3').addClass('span12');
			});
		}
	},
	transclude: true,
	template_readonly: null,
	template_editable: null,
	template: "<div class='form-mail row-fluid' ng-show='record.id > 0 || record.$messages.length > 0' ui-transclude></div>"
})

})(this);
