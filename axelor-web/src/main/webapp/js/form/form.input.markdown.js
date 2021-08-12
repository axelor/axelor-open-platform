/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
(function () {
  "use strict";

  var ui = angular.module("axelor.ui");

  // lib/toastui-editor/i18n
  var LOCALES = ['ar', 'cs-cz', 'de-de', 'es-es', 'fi-fi', 'fr-fr', 'gl-es', 'hr-hr', 'it-it',
    'ja-jp', 'ko-kr', 'nb-no', 'nl-nl', 'pl-pl', 'pt-br', 'ru-ru', 'sv-se', 'tr-tr', 'uk-ua',
    'zh-cn', 'zh-tw'];

  function getValue(scope) {
    return scope.getValue() || undefined;
  }

  function setValue(scope, markdown) {
    var value = markdown || null;
    if (scope.getValue() === value) {
      return;
    }
    scope.setValue(value, true);
    scope.$applyAsync();
  }

  function prefersDarkColorScheme() {
    var dark =
      window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: dark)").matches;
    if (dark) {
      return dark;
    }
    var color = $("body").css("background-color");
    var match = color.match(
      /^rgba?\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*(\d+(?:\.\d+)?)\s*)?\)$/i
    );
    if (!match) {
      return false;
    }
    var luma = 0.2126 * match[1] + 0.7152 * match[2] + 0.0722 * match[3];
    return luma < 64;
  }

  var editorTheme = "";
  var editorLocale = "";

  (function () {
    function init() {
      var interval = setInterval(function () {
        if (typeof toastui !== "undefined" && (toastui.Editor.plugin || {}).codeSyntaxHighlight) {
          clearInterval(interval);
          loadTheme();
          loadLocale();
        }
      }, 250);
    }

    function loadTheme() {
      if (prefersDarkColorScheme()) {
        editorTheme = "dark";
        $('<link rel="stylesheet">')
          .attr("href", "lib/toastui-editor/theme/toastui-editor-dark.min.css")
          .appendTo("head");
        $('<link rel="stylesheet">')
          .attr("href", "lib/prism/themes/prism-dark.min.css")
          .appendTo("head");
      } else {
        editorTheme = "light";
        $('<link rel="stylesheet">')
          .attr("href", "lib/prism/themes/prism.min.css")
          .appendTo("head");
      }
      $("body").trigger("toastui-theme-set");
    }

    function loadLocale() {
      var locale = ui.getPreferredLocale();
      var supportedLocale = ui.findSupportedLocale(LOCALES);
      if (supportedLocale) {
        $.getScript(
          _.sprintf("lib/toastui-editor/i18n/%s.min.js", supportedLocale),
          function () {
            setEditorLocale(locale);
          }
        );
      } else {
        setEditorLocale(locale);
      }
    }

    function setEditorLocale(locale) {
      editorLocale = locale;
      $("body").trigger("toastui-language-set");
    }

    if (_.isEmpty((axelor || {}).config)) {
      $("body").on("app:config-fetched", init);
    } else {
      init();
    }
  })();

  ui.formInput("Markdown", {
    css: "markdown-item",
    metaWidget: true,

    link_readonly: function (scope, element, attrs) {
      if (!editorTheme) {
        element.children(".markdown-loading").toggleClass("hidden", false);
        var self = this;
        var args = arguments;
        $("body").on("toastui-theme-set", function () {
          self.link_readonly.apply(self, args);
        });
        return;
      }
      this._super.apply(this, arguments);

      element.children(".markdown-loading").remove();
      element.children(".markdown-viewer").toggleClass("hidden", false);

      var Editor = toastui.Editor;
      var codeSyntaxHighlight = Editor.plugin.codeSyntaxHighlight;
      var viewer = Editor.factory({
        el: element.children(".markdown-viewer").first()[0],
        viewer: true,
        initialValue: getValue(scope),
        theme: editorTheme,
        plugins: [[codeSyntaxHighlight, { highlighter: Prism }]],
        usageStatistics: false
      });

      scope.$render_readonly = function () {
        viewer.setMarkdown(getValue(scope));
      };
    },

    link_editable: function (scope, element, attrs, model) {
      if (!editorLocale) {
        element.children(".markdown-loading").toggleClass("hidden", false);
        var self = this;
        var args = arguments;
        $("body").on("toastui-language-set", function () {
          self.link_editable.apply(self, args);
          scope.$render_editable();
        });
        return;
      }
      this._super.apply(this, arguments);

      element.children(".markdown-loading").remove();
      element.children(".markdown-editor").toggleClass("hidden", false);

      var editor = null;
      var Editor = toastui.Editor;
      var codeSyntaxHighlight = Editor.plugin.codeSyntaxHighlight;
      var toolbarItems = [
        [
          {
            name: "heading",
            tooltip: _t("Headings"),
            className: "fa fa-heading fa-header"
          },
          {
            name: "bold",
            command: "bold",
            tooltip: _t("Bold (Ctrl+B)"),
            className: "fa fa-bold"
          },
          {
            name: "italic",
            command: "italic",
            tooltip: _t("Italic (Ctrl+I)"),
            className: "fa fa-italic"
          },
          {
            name: "strike",
            command: "strike",
            tooltip: _t("Strikethrough (Ctrl+S)"),
            className: "fa fa-strikethrough "
          }
        ],
        [
          {
            removable: true,
            name: "link",
            tooltip: _t("Insert link"),
            className: "fa fa-external-link"
          },
          {
            removable: true,
            name: "image",
            tooltip: _t("Insert image"),
            className: "fa fa-image"
          },
          {
            removable: true,
            name: "table",
            tooltip: _t("Insert table"),
            className: "fa fa-table"
          }
        ],
        [
          {
            name: "ul",
            command: "bulletList",
            tooltip: _t("Unordered list"),
            className: "fa fa-list-ul"
          },
          {
            name: "ol",
            command: "orderedList",
            tooltip: _t("Ordered list"),
            className: "fa fa-list-ol"
          },
          {
            name: "task",
            command: "taskList",
            tooltip: _t("Task list"),
            className: "fa fa-check-square"
          },
          {
            removable: true,
            name: "outdent",
            command: "outdent",
            tooltip: _t("Outdent"),
            className: "fa fa-outdent"
          },
          {
            removable: true,
            name: "indent",
            command: "indent",
            tooltip: _t("Indent"),
            className: "fa fa-indent"
          }
        ],
        [
          {
            removable: true,
            name: "quote",
            command: "blockQuote",
            tooltip: _t("Insert quote"),
            className: "fa fa-quote-left"
          },
          {
            removable: true,
            name: "code",
            command: "code",
            tooltip: _t("Insert inline code"),
            className: "fa fa-code"
          },
          {
            removable: true,
            name: "codeblock",
            command: "codeBlock",
            tooltip: _t("Insert code block"),
            text: "CB",
            style: { fontWeight: "bold" }
          }
        ],
        ["scrollSync"]
      ];

      if ((scope.field || {}).lite) {
        var liteToolbarItems = [];
        _.each(toolbarItems, function (items) {
          items = _.where(items, { removable: undefined });
          if (items.length) {
            liteToolbarItems.push(items);
          }
        });
        toolbarItems = liteToolbarItems;
      }

      var widgetAttrs = _.extend({}, (scope.field || {}).widgetAttrs);
      _.each(Object.keys(widgetAttrs), function (key) {
        var value = widgetAttrs[key];
        if (value === 'true') value = true;
        else if (value === 'false') value = false;
        else if (!isNaN(value)) value = +value;
        widgetAttrs[key] = value;
      });

      scope.$render_editable = function () {
        var value = getValue(scope);

        var options = {
          el: element.children(".markdown-editor").first()[0],
          initialValue: value,
          theme: editorTheme,
          language: editorLocale,
          autofocus: false,
          usageStatistics: false,
          plugins: [[codeSyntaxHighlight, { highlighter: Prism }]],
          toolbarItems: toolbarItems,
          events: {
            load: function (ed) {
              // Needed in case of resetting with empty value
              if (_.isEmpty(value)) {
                ed.setMarkdown(value);
              }
            },
            blur: function () {
              setValue(scope, editor.getMarkdown());
            }
          }
        };
        _.extend(options, widgetAttrs);

        // Need to create new editor each time, until undo/redo history reset is supported:
        // https://github.com/nhn/tui.editor/issues/2010
        editor = new Editor(options);
      };
    },

    template_readonly:
      '<div class="form-item-container">' +
      '  <div ng-show="text" class="hidden markdown-loading">' +
      '    <div class="markdown-placeholder">{{text}}</div>' +
      '    <i class="fa fa-spinner fa-spin" />' +
      "  </div>" +
      '  <div class="hidden markdown-viewer"></div>' +
      "</div>",
    template_editable:
      '<div class="form-item-container">' +
      '  <div ng-show="text" class="hidden markdown-loading">' +
      '    <pre class="markdown-placeholder">{{text}}</pre>' +
      '    <i class="fa fa-spinner fa-spin" />' +
      "  </div>" +
      '  <div class="hidden markdown-editor"></div>' +
      "</div>"
  });
})();
