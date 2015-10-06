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
(function() {

"use strict";

var ui = angular.module('axelor.ui');

ui.factory('MessageService', ['$q', '$timeout', 'DataSource', function($q, $timeout, DataSource) {

	var POLL_INTERVAL = 10000;

	var dsFlags = DataSource.create('com.axelor.mail.db.MailFlags');
	var dsMessage = DataSource.create('com.axelor.mail.db.MailMessage');

	var pollResult = {};
	var pollPromise = null;

	function checkUnreadMessages() {

		if (pollPromise) {
			$timeout.cancel(pollPromise);
		}

		var params = {
			folder: 'inbox',
			count: true
		};

		dsMessage.messages(params).success(function (res) {
			var item = _.first(res.data) || {};
			var count = item.values || {};
			pollResult = count;
			pollPromise = $timeout(checkUnreadMessages, POLL_INTERVAL);
		});
	}

	/**
	 * Get the followers of the given record.
	 *
	 */
	function getFollowers(relatedModel, relatedId) {
		var ds = DataSource.create(relatedModel);
		return ds.followers(relatedId);
	}

	/**
	 * Get messages with the given options.
	 *
	 * The options can have following values:
	 *
	 * - relatedId = related record id
	 * - relatedModel = related model
	 * - folder = only from given folder
	 *
	 * @param {Object} options - search options
	 * @return {Promise}
	 */
	function getMessages(options) {
		var opts = _.extend({}, options);
		return dsMessage.messages(opts);
	}

	/**
	 * Get replies of the given message.
	 *
	 */
	function getReplies(parent) {
		var deferred = $q.defer();
		var opts = {
			parent: parent.id
		};

		var promise = dsMessage.messages(opts)
		.error(deferred.reject)
		.success(function (res) {
			var records = res.data || [];
			deferred.resolve(records);
		});

		return deferred.promise;
	}

	/**
	 * Toggle flags on the given message.
	 *
	 * Flag state can be:
	 *
	 * 1 = mark read
	 * 2 = mark starred
	 * 3 = mark archived
	 *
	 * Negative value unmarks the flag.
	 *
	 * @param {object|array} message - the message
	 * @param {number} flagState - flag state
	 */
	function flagMessage(message, flagState) {
		var messages = _.isArray(message) ? message : [message];
		var ref = {};
		var all = _.map(messages, function (message) {
			var flags = _.extend({}, message.$flags);
			if (flagState === 1) flags.isRead = true;
			if (flagState === -1) flags.isRead = false;
			if (flagState === 2) flags.isStarred = true;
			if (flagState === -2) flags.isStarred = false;
			if (flagState === 3) flags.isArchived = true;
			if (flagState === -3) flags.isArchived = false;

			flags.message = _.pick(message, 'id');
			flags.user = {
				id: __appSettings['user.id']
			};

			ref[""+message.id] = message;

			return flags;
		});

		var promise = dsFlags.saveAll(all);
		promise.success(function (records) {
			_.each(records, function (rec) {
				var msg = ref[""+rec.message.id];
				if (msg) {
					msg.$flags = _.pick(rec, ['id', 'version', 'isStarred', 'isRead', 'isArchived']);
				}
			});
			// force unread check
			if (flagState === 1 || flagState == -1) {
				checkUnreadMessages();
			}
		});

		return promise;
	}

	function removeMessage(message) {
		var promise = dsMessage.remove(message);
		promise.then(function () {
			checkUnreadMessages(); // force unread check
		});
		return promise;
	}

	// start polling
	checkUnreadMessages();

	return {
		getFollowers: getFollowers,
		getMessages: getMessages,
		getReplies: getReplies,
		flagMessage: flagMessage,
		removeMessage: removeMessage,
		checkUnreadMessages: checkUnreadMessages,
		unreadCount: function () {
			return pollResult.unread;
		}
	};
}]);

ui.directive('uiMailMessage', function () {
	return {
		require: '^uiMailMessages',
		replace: true,
		link: function (scope, element, attrs) {

			var body = scope.message.body || "{}";
			if (body.indexOf("{") === 0) {
				body = angular.fromJson(body);
				body.tags = _.map(body.tags, function (tag) {
					if (tag.style) {
						tag.css = 'label-' + tag.style;
					}
					return tag;
				});
				scope.body = body;
			} else {
				scope.body = null;
			}

			setTimeout(function () {
				element.addClass('fadeIn');
			});
		},
		template: "" +
			"<div class='fade'>" +
				"<a href='' class='pull-left avatar' title='{{::$userName(message.createdBy)}}' ng-click='showUser(message.author)'>" +
					"<img ng-src='{{::message.$avatar}}' width='32px'>" +
				"</a>" +
				"<div class='mail-message'>" +
					"<span class='arrow left'></span>" +
					"<div class='mail-message-icons'>" +
						"<span ng-if='::message.$thread'>" +
							"<i class='fa fa-reply' ng-show='::message.$thread' ng-click='onReply(message)'></i> " +
						"</span>" +
						"<div class='btn-group'>" +
							"<a href='javascript:' class='btn btn-link dropdown-toggle' data-toggle='dropdown'>" +
								"<i class='fa fa-caret-down'></i>" +
							"</a>" +
							"<ul class='dropdown-menu pull-right'>" +
								"<li>" +
									"<a href='javascript:' ng-show='::!message.$flags.isRead' ng-click='onFlag(message, 1)' x-translate>Mark as read</a>" +
									"<a href='javascript:' ng-show='::message.$flags.isRead' ng-click='onFlag(message, -1)' x-translate>Mark as unread</a>" +
								"</li>" +
								"<li>" +
									"<a href='javascript:' ng-show='::!message.$flags.isStarred' ng-click='onFlag(message, 2)' x-translate>Mark as important</a>" +
									"<a href='javascript:' ng-show='::message.$flags.isStarred' ng-click='onFlag(message, -2)' x-translate>Mark as not important</a>" +
								"</li>" +
								"<li ng-if='message.$thread' ng-show='::!message.parent'>" +
									"<a href='javascript:' ng-show='::!message.$flags.isArchived' ng-click='onFlag(message, 3)'>Move to archive</a>" +
									"<a href='javascript:' ng-show='::message.$flags.isArchived' ng-click='onFlag(message, -3)'>Move to inbox</a>" +
								"</li>" +
								"<li>" +
									"<a href='javascript:' ng-show='::message.$canDelete' ng-click='onRemove(message)'>Delete</a>" +
								"</li>" +
				            "</ul>" +
						"</div>" +
					"</div>" +
					"<div class='mail-message-header' ng-if='::message.$name || body.title'>" +
						"<span class='subject' ng-if='::message.$name'>" +
							"<a ng-if='message.relatedId' href='#ds/form::{{::message.relatedModel}}/edit/{{::message.relatedId}}'>{{::message.$name}}</a>" +
							"<span ng-if='::!message.relatedId'>{{::message.$name}}</span>" +
							"<span ng-if='::message.subject'> : {{::message.subject}}</span>" +
						"</span>" +
						"<span class='track-message'>{{:: _t(body.title) }}</span>" +
						"<span class='track-tags'>" +
							"<span class='label' ng-class='::item.css' ng-repeat='item in ::body.tags'>{{:: _t(item.title) }}</span>" +
						"</span>" +
					"</div>" +
					"<div class='mail-message-body'>" +
						"<ul class='track-fields' ng-if='::body'>" +
							"<li ng-repeat='item in ::body.tracks'>" +
								"<strong>{{:: _t(item.title) }}</strong> : <span ng-bind-html='::item.value'></span>" +
							"</li>" +
						"</ul>" +
						"<div ng-if='!body' ui-bind-template x-text='message.body'></div>" +
						"<div class='mail-message-files' ng-show='::message.$files.length'>" +
							"<ul class='inline'>" +
								"<li ng-repeat='file in ::message.$files'>" +
									"<i class='fa fa-paperclip'></i> <a href='' ng-click='onDownload(file)'>{{::file.fileName}}</a>" +
								"</li>" +
							"</ul>" +
						"</div>" +
						"<div class='mail-message-footer'>" +
							"<span ng-if='message.$numReplies' class='pull-right'>" +
								"<a href='' ng-click='onReplies(message)'>{{formatNumReplies(message)}}</a>" +
							"</span>" +
							"<span>" +
								"<a href='' ng-click='showUser(message.author)'>{{::$userName(message.author)}}</a> " +
								"<span>{{formatEvent(message)}}</span>" +
							"</span>" +
						"</div>" +
					"</div>" +
				"</div>" +
				"<div ui-mail-composer ng-if='::message.$thread'></div>" +
			"</div>"
	};
});

ui.formWidget('uiMailMessages', {
	scope: true,
	controller: ['$scope', 'MessageService', function ($scope, MessageService) {

		function updateReadCount(messages) {
			var unread = _.filter(messages, function (msg) {
				return !(msg.$flags||{}).isRead;
			});
			if (unread.length) {
				MessageService.flagMessage(unread, 1);
			}
		}

		$scope.onFlag = function (message, flagState) {
			MessageService.flagMessage(message, flagState).success(function (res) {
				if (flagState !== 1 && flagState !== -1) {
					$scope.onLoadMessages();
				}
			});
		};

		$scope.onRemove = function(message) {
			var index = $scope.messages.indexOf(message);
			MessageService.removeMessage(message).success(function (res) {
				if (message.$afterDelete) {
					message.$afterDelete();
				} else if (index > -1) {
					$scope.messages.splice(index, 1);
				}
			});
		};

		$scope.onReply = function (message) {
			message.$reply = true;
			$scope.$broadcast("on:message-add");
		};

		$scope.onReplies = function (message) {
			MessageService.getReplies(message)
			.then(function (data) {
				_.each(data, function (item, i) {
					item.$afterReply = item.$afterDelete = function () {
						$scope.onReplies(message);
					};
				});
				message.$numReplies = data.length;
				message.$children = data;
				updateReadCount(data);
			});
		};

		$scope.formatEvent = function (message) {
			if (message.$eventLine) {
				return message.$eventLine;
			}
			var line = message.$eventText + " - " + moment(message.$eventTime).fromNow();
			message.$eventLine = line;
			return line;
		};

		$scope.formatNumReplies = function (message) {
			return _t('replies ({0})', message.$numReplies);
		};

		var folder = $scope.folder;

		$scope.messages = [];
		$scope.hasMore = false;
		$scope.hasMessages = true;

		$scope.animation = {};

		$scope.onLoadMessages = function (offset) {

			var record = $scope.record || {};
			var limit = ($scope.field || {}).limit || 10;
			var params = {
				limit: limit,
				offset: offset,
				folder: folder
			};

			if (record.id > 0) {
				params.relatedId = record.id;
				params.relatedModel = $scope._model;
			}

			if (!offset && folder) {
				$scope.animation = {
					"fadeDim": true
				};
			}

			return MessageService.getMessages(params).success(function (res) {
				var found = res.data;
				var count = res.total;

				if (!offset) {
					$scope.hasMore = false;
					$scope.messages.length = 0;
				}

				Array.prototype.push.apply($scope.messages, found);
				$scope.hasMore = $scope.messages.length < count;

				$scope.waitForActions(function () {
					if (folder) {
						$scope.record.__empty = count === 0;
					}
					updateReadCount(found);
				});

				$scope.animation = {
					'fade': true,
					'fadeIn': true
				};
			});
		};

		$scope.onLoadMore = function () {
			var offset = $scope.messages.length;
			$scope.onLoadMessages(offset);
		};

		$scope.$on("on:new", function (e) {
			if (folder) {
				$scope.onLoadMessages();
			}
		});

		$scope.$watch("record", function (record, old) {
			if (record === old) { return; }
			if (record && record.id) {
				$scope.onLoadMessages();
			}
		});

		$scope.$on("on:nav-click", function (e, tab) {
			var action = ($scope.tab||{}).action;
			if (action !== tab.action) { return ; }
			if (folder) {
				$scope.onLoadMessages();
			}
		});
	}],
	link: function (scope, element, attrs) {

		scope.onDownload = function (file) {
			var frame = $("<iframe>").hide().appendTo(element);
			var url = "ws/rest/com.axelor.meta.db.MetaFile/" + file.id + "/content/download";
			frame.attr("src", url);
			setTimeout(function(){
				frame.attr("src", "");
				frame.remove();
			}, 100);
		};
	},
	template_readonly: null,
	template_editable: null,
	template:
		"<div class='mail-messages panel panel-default span9' ng-class='animation'>" +
			"<div class='panel-body'>" +
				"<div class='mail-composer' ui-mail-composer></div>" +
				"<div class='mail-thread'>" +
					"<span ng-repeat='message in messages'>" +
						"<div ui-mail-message></div>" +
						"<div ng-repeat='message in message.$children' class='mail-message-indent' ui-mail-message></div>" +
					"</span>" +
					"<div ng-show='hasMore' class='mail-thread-more'>" +
						"<a href='' ng-click='onLoadMore()' class='muted' x-translate>load more</a>" +
					"</div>" +
				"</div>" +
			"</div>" +
		"</div>"
});

ui.directive('uiMailUploader', ["$compile", function ($compile) {

	return function (scope, element, attrs) {

		scope.onSelect = function (items) {
			for (var i = 0; i < items.length; i++) {
				var file = items[i];
				var fileId = file.metaFile ? file.metaFile.id : file['metaFile.id'];
				if (file.isDirectory || !fileId) continue;
				if (_.findWhere(scope.files, {id: fileId})) continue;
				scope.files.push({
					id: fileId,
					fileName: file.fileName
				});
			}
		};

		scope.onUpload = function () {
			var popup = $compile('<div ui-dms-popup x-on-select="onSelect"></div>')(scope);
			popup.isolateScope().showPopup();
		};
	};
}]);

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
				type: 'comment',
				parent: parent.id && parent,
				files: files
			}).success(function (res) {
				var message = _.first(res.data);
				var messages = $scope.$parent.messages || [];

				$scope.post = null;
				$scope.files = [];

				if (parent.id && $scope.message) {
					$scope.message.$reply = false;
					if ($scope.message.$afterReply) {
						return $scope.message.$afterReply();
					}
					return $scope.onReplies($scope.message);
				} else {
					messages.unshift(message);
				}

				$scope.$broadcast('on:message-add');
			});
		};

	}],
	link: function (scope, element, attrs) {

		var textarea = element.find('textarea');

		textarea.on('blur', function () {
			if (scope.post || !scope.message) return;
			scope.message.$reply = false;
			scope.applyLater();
		});

		scope.$on('on:message-add', function () {
			if (scope.canShow()) {
				scope.post = null;
				setTimeout(function () {
					textarea.focus();
				});
			}
		});

		scope.onRemoveFile = function (file) {
			var i = scope.files.indexOf(file);
			if (i > -1) {
				scope.files.splice(i, 1);
			}
		};
	},
	template: "" +
		"<div class='mail-composer' ng-show='canShow()'>" +
			"<textarea rows='1' ng-model='post' ui-textarea-auto-size class='span12' placeholder='Write your comment here'></textarea>" +
			"<div class='mail-composer-files' ng-show='files.length'>" +
				"<ul>" +
				"<li ng-repeat='file in files'>" +
					"<i class='fa fa-close' ng-click='onRemoveFile(file)'></i> <a href='' ng-click='onDowload(file)'>{{file.fileName}}</a>" +
				"</li>" +
				"</ul>" +
			"</div>" +
			"<div class='mail-composer-buttons' ng-show='canPost()'>" +
				"<button class='btn btn-primary' ng-click='onPost()' x-translate>Post</button>" +
				"<span class='btn btn-default' ui-mail-uploader ng-click='onUpload()'>" +
					"<i class='fa fa-paperclip'></i>" +
				"</span>" +
			"</div>" +
		"</div>"
});

ui.formWidget('uiMailFollowers', {

	scope: true,
	controller: ['$scope', 'ViewService', 'MessageService', function ($scope, ViewService, MessageService) {

		var ds = $scope._dataSource;
		var userSelector = null;

		$scope.editorCanSave = false;
		$scope._viewParams = {
			model: 'com.axelor.auth.db.User',
			viewType: 'form',
			views: [{type: 'form'}, {type: 'grid'}]
		};

		$scope.following = false;
		$scope.followers = [];

		$scope.updateStatus = function() {
			var followers = $scope.followers || [];
			var found = _.findWhere(followers, {
				code: $scope.$root.app.login
			});
			$scope.following = !!found;
		};

		$scope.select = function (records) {

			var ds = $scope._dataSource;
			var record = $scope.record || {};
			var promise = ds.messageFollow(record.id, {
				records: records
			});

			promise.success(function (res) {
				$scope.followers = res.data || [];
				$scope.updateStatus();
			});
		};

		$scope.onAddFollowers = function () {

			if (userSelector == null) {
				userSelector = ViewService.compile('<div ui-selector-popup x-select-mode="multi"></div>')($scope);
			}

			var popup = userSelector.isolateScope();
			popup.show();
		};

		$scope.onFollow = function () {

			var record = $scope.record || {};
			var promise = ds.messageFollow(record.id);

			promise.success(function (res) {
				$scope.followers = res.data || [];
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
		};

		function doUnfollow(user) {

			var record = $scope.record || {};
			var promise = ds.messageUnfollow(record.id, {
				records: _.compact([user])
			});

			promise.success(function (res) {
				$scope.followers = res.data || [];
				$scope.updateStatus();
			});
		}

		function doLoadFollowers(record) {
			MessageService.getFollowers($scope._model, record.id)
			.success(function (res) {
				$scope.followers = res.data;
				$scope.updateStatus();
			});
		}

		$scope.$watch("record", function (record) {
			if (record && record.id) {
				doLoadFollowers(record);
			}
		});
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
					"<i class='fa fa-star-o' ng-click='onFollow()' ng-show='!following'></i>" +
					"<i class='fa fa-star' ng-click='onUnfollow()' ng-show='following'></i>" +
					"<i class='fa fa-plus' ng-click='onAddFollowers()'></i>" +
				"</div>" +
			"</div>" +
			"</div>" +
			"<div class='panel-body'>" +
			"<ul class='links'>" +
				"<li ng-repeat='follower in followers'>" +
				"<a href='' ng-click='showUser(follower)'>{{$userName(follower)}}</a> " +
				"<i class='fa fa-remove' ng-click='onUnfollow(follower)'></i>" +
				"</li>" +
			"</ul>" +
			"</div>" +
		"</div>"
});

ui.formWidget('PanelMail', {

	scope: true,
	controller: ['$scope', '$timeout', 'NavService', 'ViewService', 'MessageService', function ($scope, $timeout, NavService, ViewService, MessageService) {

		$scope.getDomain = function () {
			return {};
		};

		$scope.showUser = function (author) {
			if (!author) {
				return;
			}
			NavService.openTabByName("form:user-form", {
				mode: "edit",
				state: author.id
			});
		};

		$scope.$userName = function (user) {
			if (!user) return null;
			var key = __appSettings["user.nameField"] || "name";
			return user[key] || user.name;
		};

		var folder;
		var tab = $scope.tab || {};

		if (tab.action === "mail.inbox") folder = "inbox";
		if (tab.action === "mail.important") folder = "important";
		if (tab.action === "mail.archive") folder = "archive";

		$scope.folder = folder;
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
	template: "<div class='form-mail row-fluid' ng-show='record.id > 0 || folder' ui-transclude></div>"
});

ui.controller("MailGroupListCtrl", MailGroupListCtrl);
MailGroupListCtrl.$inject = ['$scope', '$element'];
function MailGroupListCtrl($scope, $element) {
	ui.GridViewCtrl.call(this, $scope, $element);

	$scope.getUrl = function (record) {
		if (!record || !record.id) return null;
		return "ws/rest/com.axelor.mail.db.MailGroup/" + record.id + "/image/download?image=true&v=" + record.version;
	};

	$scope.onEdit = function(record) {
		$scope.switchTo('form', function (formScope) {
			if (formScope.canEdit()) {
				formScope.edit(record);
			}
		});
	};
	
	$scope.onFollow = function (record) {

		var ds = $scope._dataSource;
		var promise = ds.messageFollow(record.id);

		promise.success(function (res) {
			record.$following = true;
		});
	};

	$scope.onUnfollow = function (record) {

		axelor.dialogs.confirm(_t('Are you sure to unfollow this group?'),
		function (confirmed) {
			if (confirmed) {
				doUnfollow(record);
			}
		});
	};

	function doUnfollow(record) {

		var ds = $scope._dataSource;
		var promise = ds.messageUnfollow(record.id);

		promise.success(function (res) {
			record.$following = false;
		});
	}
}

})();
