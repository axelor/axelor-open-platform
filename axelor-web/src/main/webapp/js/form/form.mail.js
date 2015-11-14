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
				id: axelor.config['user.id']
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

			var message = scope.message;
			var body = message.body || "{}";
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

			message.$title = (body||{}).title || message.subject;

			scope.showFull = !message.summary;
			scope.onShowFull = function () {
				scope.showFull = true;
			};

			setTimeout(function () {
				element.addClass('fadeIn');
			});
		},
		template: "" +
			"<div class='fade'>" +
				"<a href='' class='pull-left avatar' title='{{::$userName(message)}}' ng-click='showAuthor(message)'>" +
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
					"<div class='mail-message-header' ng-if='::message.$name || message.$title'>" +
						"<span class='subject'>" +
							"<a ng-if='message.relatedId && message.$name' href='#ds/form::{{::message.relatedModel}}/edit/{{::message.relatedId}}'>{{::message.$name}}</a>" +
							"<span ng-if='::!message.relatedId && message.$name'>{{::message.$name}}</span>" +
							"<span ng-if='::message.$name'> - </span>" +
							"<span ng-if='::message.$title'>{{::message.$title}}</span>" +
						"</span>" +
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
						"<div ng-if='!body'>" +
							"<div ng-if='message.summary && !showFull' ui-bind-template x-text='message.summary'></div>" +
							"<div ng-if='!message.summary || showFull' ui-bind-template x-text='message.body'></div>" +
							"<a ng-if='!showFull' href='' ng-click='onShowFull()' class='show-full'><i class='fa fa-ellipsis-h'></i></a>" +
						"</div>" +
						"<div class='mail-message-files' ng-show='::message.$files.length'>" +
							"<div ui-mail-files x-removable='false' x-files='message.$files' class='inline'></div>" +
						"</div>" +
						"<div class='mail-message-footer'>" +
							"<span ng-if='message.$numReplies' class='pull-right'>" +
								"<a href='' ng-click='onReplies(message)'>{{formatNumReplies(message)}}</a>" +
							"</span>" +
							"<span>" +
								"<a href='' ng-click='showAuthor(message)'>{{::$userName(message)}}</a> " +
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
			var unread = [];
			_.each(messages, function (message) {
				if (!(message.$flags||{}).isRead) {
					unread.push(message);
				}
				_.each(message.$children, function (item) {
					if (!(item.$flags||{}).isRead) {
						unread.push(item);
					}
				});
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
			var children = (message.$children||[]).length || 0;
			var total = message.$numReplies || 0;
			return _t('replies ({0} of {1})', children, total);
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
						updateReadCount(found);
					}
				}, 100);

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

ui.directive('uiMailFiles', [function () {
	return {
		scope: {
			files: "=",
			removable: "@"
		},
		link: function (scope, element, attrs) {

			scope.onRemoveFile = function (file) {
				var files = scope.files || [];
				var i = files.indexOf(file);
				if (i > -1) {
					files.splice(i, 1);
				}
			};

			scope.onDownload = function (file) {
				var url = "ws/rest/com.axelor.meta.db.MetaFile/" + file.id + "/content/download";
				ui.download(url, file.fileName);
			};
		},
		replace: true,
		template:
			"<ul class='mail-files'>" +
				"<li ng-repeat='file in files'>" +
					"<i class='fa fa-close' ng-if='removable != \"false\"' ng-click='onRemoveFile(file)'></i>" +
					"<i class='fa fa-paperclip' ng-if='removable == \"false\"'></i>" +
					"<a href='' ng-click='onDownload(file)'>{{file.fileName}}</a>" +
				"</li>" +
			"</ul>"
	};
}]);

ui.directive('uiMailEditor', ["$compile", function ($compile) {

	return function (scope, element, attrs) {

		var title = scope.$eval(attrs.emailTitle) || _t("Email");
		var sendTitle = scope.$eval(attrs.sendTitle) || _t("Send");

		scope._viewParams = {
			model: "com.axelor.mail.db.MailMessage",
			type: "form",
			fields: {
			},
			views: [{
				type: "form",
				title: title,
				css: "mail-editor",
				items: [{
					type: "panel",
					showFrame: false,
					itemSpan: 12,
					items: [{
						name: "recipients",
						widget: "mail-select",
						showTitle: false,
						placeholder: _t("Recipients")
					}, {
						name: "subject",
						showTitle: false,
						placeholder: _t("Subject")
					}, {
						name: "body",
						showTitle: false,
						widget: "html",
						height: 250
					}, {
						name: "files",
						showTitle: false,
						widget: "mail-file-list"
					}]
				}]
			}]
		};

		var popup = null;

		scope.$on("$destroy", function () {
			if (popup) {
				popup.$destroy();
				popup = null;
			}
		});

		scope.select = function (record) {

		};

		scope.onEditEmail = function (record, onSelect) {

			if (popup === null) {
				popup = scope.$new();
				popup.fields = {};

				var editor = $compile('<div ui-editor-popup x-buttons="buttons"></div>')(popup);
				var buttons = editor.parent().find(".ui-dialog-buttonpane");

				buttons.find(".button-ok").text(sendTitle);

				var attach = $('<button type="button" class="btn"><i class="fa fa-paperclip"></i></button>')
				.click(function() {
					var uploader = $compile('<div ui-dms-popup x-on-select="onSelectFiles"></div>')(popup);
					uploader.isolateScope().showPopup();
					uploader.isolateScope().applyLater();
				});

				$('<div class="pull-left ui-dialog-buttonset" style="float: left;"></div>')
					.append(attach)
					.appendTo(buttons);

				popup = editor.isolateScope();
				popup.onSelectFiles = function (files) {
					var rec = popup.record || {};
					var all = rec.files || [];
					rec.files = all.concat(files);
					popup.record = rec;
				};
			}

			popup.show(record, onSelect);
		};
	};
}]);

ui.formWidget('uiMailFileList', {

	link: function () {

	},
	template_readonly: null,
	template_editable: null,
	template: "<div>" +
			"<div ui-mail-files x-files='record.files' class='inline'></div>" +
			"</div>"
});

ui.formInput('uiMailSelect', 'MultiSelect', {

	init: function (scope) {
		this._super.apply(this, arguments);
		scope.isReadonly = function () { return false; }
	},

	link_editable: function (scope, element, attrs, model) {
		this._super.apply(this, arguments);

		var field = scope.field;
		var selectionMap = {};

		scope.formatItem = function(value) {
			if (!value) { return ""; }
			var item = selectionMap[value];
			if (item) {
				return item.label || value;
			}
			return item;
		};

		var handleSelect = scope.handleSelect;
		scope.handleSelect = function(e, ui) {
			var item = ui.item;
			selectionMap[item.value] = item;
			// store selection map in record to find display names
			if (scope.record) {
				scope.record["$" + field.name + "Map"] = selectionMap;
			}
			return handleSelect.apply(scope, arguments);
		};

		scope.loadSelection = function(request, response) {

			var $http = scope._dataSource._http;
			var data = {
				search: request.term,
				selected: _.pluck(scope.getSelection(), "value")
			};

			if (_.isEmpty(data.selected)) {
				selectionMap = {};
			}

			$http.post("ws/search/emails", { data: data }).then(function (resp) {
				var res = resp.data;
				var items = _.map(res.data, function (item) {
					return {
						label: item.personal || item.address,
						value: item.address
					};
				});
				response(items);
			}, function (err) {
				response([]);
			});
		};
	}
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
			var text = $scope.post.replace(/\n/g, '<br>');
			var files = $scope.files;
			return $scope.sendEmail({
				body: text,
				files: files
			});
		};

		$scope.sendEmail = function (email) {

			var ds = dataSource();
			var record = dataRecord();
			var parent = _.pick($scope.message || {}, 'id');

			if (!record.id) {
				return;
			}

			var text = email.body || email.post;
			var files = _.pluck(email.files, "id");
			var recipients = email.recipients;

			return ds.messagePost(record.id, text, {
				type: 'comment',
				parent: parent.id && parent,
				files: files,
				recipients: recipients
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

		function findName() {
			var record = scope.record || {};
			var fields = scope.fields || {};
			var name = _.findWhere(fields, {nameColumn: true});
			return record[name] || record.name || record.code || "";
		}

		scope.showEditor = function () {
			var record = {
				subject: "Re: " + findName(),
				body: scope.post,
				files: scope.files
			};
			scope.onEditEmail(record, function (record) {
				var map = record.$recipientsMap || {};
				var recipients = record.recipients || "";

				record.recipients = _.map(recipients.split(","), function (item) {
					var email = item.trim();
					return _.extend({
						address: email,
						personal: (map[email]||{}).label || email
					});
				});

				scope.sendEmail(record);
			});
		};
	},
	template: "" +
		"<div class='mail-composer' ng-show='canShow()'>" +
			"<textarea rows='1' ng-model='post' ui-textarea-auto-size class='span12' placeholder='Write your comment here'></textarea>" +
			"<div class='mail-composer-files' ng-show='files.length'>" +
				"<div ui-mail-files x-files='files'></div>" +
			"</div>" +
			"<div class='mail-composer-buttons' ng-show='canPost()'>" +
				"<button type='button' class='btn btn-primary' ng-click='onPost()' x-translate>Post</button>" +
				"<span class='btn btn-default' ui-mail-uploader ng-click='onUpload()'>" +
					"<i class='fa fa-paperclip'></i>" +
				"</span>" +
				"<span class='btn btn-default' ui-mail-editor ng-click='showEditor()'>" +
					"<i class='fa fa-pencil'></i>" +
				"</span>" +
			"</div>" +
		"</div>"
});

ui.formWidget('uiMailFollowers', {

	scope: true,
	controller: ['$scope', 'ViewService', 'MessageService', function ($scope, ViewService, MessageService) {

		var ds = $scope._dataSource;

		$scope.emailTitle = _t("Add followers");
		$scope.sendTitle = _t("Add");

		$scope.following = false;
		$scope.followers = [];

		$scope.updateStatus = function() {
			var followers = $scope.followers || [];
			var found = _.find(followers, function (item) {
				return item.$author && item.$author.code === axelor.config["user.login"]
			});
			$scope.following = !!found;
		};

		function sendEmail(email) {

			var ds = $scope._dataSource;
			var record = $scope.record || {};
			var promise = ds.messageFollow(record.id, {
				email: email
			});

			promise.success(function (res) {
				$scope.followers = res.data || [];
				$scope.updateStatus();
			});
		};

		function findName() {
			var record = $scope.record || {};
			var fields = $scope.fields || {};
			var name = _.findWhere(fields, {nameColumn: true});
			return record[name] || record.name || record.code || "";
		}

		$scope.onAddFollowers = function () {
			var record = {
				subject: _t("Follow: {0}", findName())
			};
			$scope.onEditEmail(record, function (record) {

				var map = record.$recipientsMap || {};
				var recipients = record.recipients || "";

				record.recipients = _.map(recipients.split(","), function (item) {
					var email = item.trim();
					return _.extend({
						address: email,
						personal: (map[email]||{}).label || email
					});
				});
				sendEmail(record);
			});
		};

		$scope.onFollow = function () {

			var record = $scope.record || {};
			var promise = ds.messageFollow(record.id);

			promise.success(function (res) {
				$scope.followers = res.data || [];
				$scope.updateStatus();
			});
		};

		$scope.onUnfollow = function (follower) {
			axelor.dialogs.confirm(_t('Are you sure to unfollow this document?'),
			function (confirmed) {
				if (confirmed) {
					doUnfollow(follower);
				}
			});
		};

		function doUnfollow(follower) {
			var followerId = (follower||{}).id;
			var record = $scope.record || {};
			var promise = ds.messageUnfollow(record.id, {
				records: _.compact([followerId])
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
					"<i class='fa fa-plus' ui-mail-editor x-email-title='emailTitle' x-send-title='sendTitle' ng-click='onAddFollowers()'></i>" +
				"</div>" +
			"</div>" +
			"</div>" +
			"<div class='panel-body'>" +
			"<ul class='links'>" +
				"<li ng-repeat='follower in followers'>" +
				"<a href='' ng-click='showAuthor(follower)'>{{$userName(follower)}}</a> " +
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

		$scope.showAuthor = function (message) {
			var msg = message || {};
			var act = msg.$authorAction;
			var author = msg.$author;
			var model = msg.$authorModel;
			if (!author || !author.id) {
				return;
			}
			NavService.openTab({
				action: act || _.uniqueId('$act'),
				model: model,
				viewType: "form",
				views: [{ type: "form" }]
			}, {
				mode: "edit",
				state: author.id
			});
		};

		$scope.$userName = function (message) {
			var msg = message || {};
			var author = msg.$author || msg.$from;
			if (!author) {
				return null;
			}
			var key = axelor.config["user.nameField"] || "name";
			return author.personal || author[key] || author.name || author.fullName || author.displayName;
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
