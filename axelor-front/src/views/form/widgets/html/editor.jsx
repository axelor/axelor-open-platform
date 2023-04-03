import { useRef, useCallback, useEffect, useState, useMemo } from "react";
import Popup from "./popup/base";
import {
  isMediaNode,
  isOrContainsNode,
  addEvent,
  removeEvent,
  cancelEvent,
  filecontents,
  pasteHtmlAtCaret,
  selectionInside,
  getSelectionHtml,
  collapseSelectionEnd,
  normalizeHTML,
  getActions,
} from "./utils";

import "./editor.scss";

function Link(props) {
  return (
    <a unselectable="on" alt="Action" {...props}>
      {props.children}
    </a>
  );
}

function Toolbar({ toggle, emitChange, actions, onAction }) {
  return (
    <div className="toolbar toolbar-top custom-html-editor-toolbar">
      {actions.map((action, ind) => {
        const disabled = toggle && action.command !== "toggleCode";
        return action.divider ? (
          <span key={ind} className="custom-html-editor-toolbar-divider" />
        ) : (
          <Link
            href="#"
            key={ind}
            className={`custom-html-editor-toolbar-icon${
              disabled ? " disabled" : ""
            }`}
            onClick={(e) => {
              e.preventDefault();
              if (!disabled) {
                onAction(e, action);
                if (action.command === "normalize") {
                  emitChange();
                }
              }
            }}
            title={action.title}
          >
            {action.image}
          </Link>
        );
      })}
    </div>
  );
}

function HTMLEditor({
  t: _t,
  lite,
  placeholder,
  hijackMenu,
  value,
  autoFocus,
  onDirty,
  onChange,
  onBlur,
  onKeyDown: _onKeyDown,
}) {
  const containerRef = useRef(null);
  const contentEditableRef = useRef(null);
  const textareaRef = useRef(null);
  const initRef = useRef(false);
  const htmlRef = useRef("");
  const refs = useRef({
    _selection: null,
    _mouseDownTarget: null,
  });
  const [dirty, setDirty] = useState(false);
  const [popup, setPopup] = useState(null);
  const [toggle, setToggle] = useState(false);
  let timer = null;

  const getRef = (key) => refs.current[key];
  const setRef = (key, value) => (refs.current[key] = value);
  const getEditor = useCallback(() => contentEditableRef.current, []);

  const { commands, updates, getSelection, saveSelection } = useMemo(() => {
    const getSelection = () => getRef("_selection");
    const setSelection = (val) => setRef("_selection", val);
    const clearSelection = () => setSelection(null);

    function saveSelection() {
      const sel = window.getSelection();
      setSelection(sel.rangeCount > 0 ? sel.getRangeAt(0) : null);
    }

    function restoreSelection() {
      const savedSel = getSelection();
      if (savedSel) {
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(savedSel);
      }
    }

    function execCommand(command, param, selection) {
      // give selection to contenteditable element
      restoreSelection();
      // tried to avoid forcing focus()
      getEditor().focus();

      // returns 'selection inside editor'
      if (!selectionInside(getEditor(), selection)) return false;

      // Buggy, call within 'try/catch'
      try {
        return document.queryCommandSupported &&
          !document.queryCommandSupported(command)
          ? false
          : document.execCommand(command, false, param);
      } catch (e) {}
      return false;
    }

    function updates(_clearSelection) {
      // handle saved selection
      if (_clearSelection) {
        collapseSelectionEnd();
        clearSelection(); // selection destroyed
      } else if (getSelection()) {
        saveSelection();
      }
    }

    function collapseSelection() {
      collapseSelectionEnd();
      clearSelection();
    }

    function openPopup(e, config) {
      !getSelection() && saveSelection();
      setPopup({ ...config, target: e.target });
    }

    function closePopup() {
      setPopup(null);
    }

    function getHTML() {
      return getEditor().innerHTML;
    }

    function setHTML(html) {
      getEditor().innerHTML = html || "";
    }

    function normalize() {
      const html = normalizeHTML(getHTML());
      getEditor().focus();
      setHTML(html);
      return html;
    }

    function toggleCode() {
      setToggle((toggle) => !toggle);
    }

    const commands = {
      format: (tag) => execCommand("formatBlock", tag),
      foreColor: (color) => execCommand("foreColor", color),
      highlight: (color) => {
        // some browsers apply 'backColor' to the whole block
        if (!execCommand("hiliteColor", color)) execCommand("backColor", color);
      },
      removeFormat: () => {
        execCommand("removeFormat");
        execCommand("unlink");
      },
      fontName: (name) => execCommand("fontName", name),
      fontSize: (size) => execCommand("fontSize", size),
      insertLink: (url) => execCommand("createLink", url),
      insertImage: (url) => execCommand("insertImage", url, true),
      insertHTML: (html) => {
        if (!execCommand("insertHTML", html, true)) {
          // IE 11 still does not support 'insertHTML'
          restoreSelection();
          selectionInside(getEditor(), true);
          pasteHtmlAtCaret(getEditor(), html);
        }
        updates();
      },
      normalize,
      toggleCode,
      collapseSelection,
      getSelectedHTML: () => {
        restoreSelection();
        if (!selectionInside(getEditor())) return null;
        return getSelectionHtml(getEditor());
      },
      getHTML,
      setHTML,
      execCommand,
      openPopup,
      closePopup,
    };

    [
      "bold",
      "italic",
      "underline",
      "strikeThrough",
      "justifyLeft",
      "justifyRight",
      "justifyCenter",
      "justifyFull",
      "indent",
      "outdent",
      "subscript",
      "superscript",
      "insertOrderedList",
      "insertUnorderedList",
    ].forEach((cmd) => {
      commands[cmd] = () => {
        execCommand(cmd);
        collapseSelection();
        // closePopup();
      };
    });

    return { commands, updates, getSelection, saveSelection };
  }, [getEditor, setPopup]);

  const actions = useMemo(
    () =>
      getActions().filter(
        ({ lite: _lite = lite }) => Boolean(_lite) === Boolean(lite)
      ),
    [lite]
  );

  function onKeyDown(e) {
    const key = e.which || e.keyCode;
    const character = String.fromCharCode(key || e.charCode);

    // Exec hotkey (onkeydown because e.g. CTRL+B would oben the bookmarks)
    if (character && !e.shiftKey && !e.altKey && e.ctrlKey && !e.metaKey) {
      const action = actions.find(
        (x) => (x.hotkey || "").toLowerCase() === character.toLowerCase()
      );
      if (action && action.command) {
        commands[action.command](e);
        updates();
        cancelEvent(e);
      }
    }

    if (getSelection()) saveSelection();
    _onKeyDown && _onKeyDown(e);
  }

  function mouseHandler(e, rightclick) {
    // mouse button
    if (e.which && e.which === 3) rightclick = true;
    else if (e.button && e.button === 2) rightclick = true;

    // remove event handler
    removeEvent(window, "mouseup", mouseHandler);
    // Callback selection
    getSelection() && saveSelection();
    if (!hijackMenu && rightclick) return;
  }

  function onMouseDown(e) {
    // catch event if 'mouseup' outside 'contenteditable'
    removeEvent(window, "mouseup", mouseHandler);
    addEvent(window, "mouseup", mouseHandler);
    // remember target
    setRef("_mouseDownTarget", e.target);
  }

  function onMouseUp(e) {
    const node = e.target;
    if (
      node &&
      node.nodeType === Node.ELEMENT_NODE &&
      node === getRef("_mouseDownTarget") &&
      isMediaNode(node) &&
      isOrContainsNode(getEditor(), node, true)
    ) {
      const selection = window.getSelection();
      const range = document.createRange();
      range.setStartBefore(node);
      range.setEndAfter(node);
      selection.removeAllRanges();
      selection.addRange(range);
    }
    // handle click
    mouseHandler(e);
  }

  function onContextMenu(e) {
    mouseHandler(e, true);
    cancelEvent(e);
  }

  function onPaste(e) {
    const { clipboardData: { items } = {} } = e;
    if (!items || !items.length) return;
    const [item] = items;

    if (!item.type.match(/^image\//)) return;
    // Insert image from clipboard
    filecontents(item.getAsFile(), function (type, dataurl) {
      commands.execCommand("insertImage", dataurl);
    });
    cancelEvent(e); // dismiss paste
  }

  function onAction(e, { popup, popupProps, popupStyle, command }) {
    if (popup) {
      commands.openPopup(e, {
        component: popup,
        style: popupStyle,
        props: popupProps,
      });
    } else {
      const $action = commands[command];
      const externalCommands = ["insertLink", "insertImage", "insertHTML"];
      if ($action) {
        $action(e);
        updates(externalCommands.includes(command));
      }
    }
    cancelEvent(e);
  }

  function handleChange(e) {
    setDirty(value === e.target.value ? false : true);
    onChange && onChange((htmlRef.current = e.target.value));
  }

  function emitFocus() {
    clearTimeout(timer);
  }

  function handleBlur(e) {
    timer = setTimeout(() => {
      onBlur && onBlur(e.target.value);
    }, 150);
  }

  function emitChange() {
    const html = commands.getHTML();
    handleChange({ target: { value: html } });
  }

  function emitBlur() {
    handleBlur({ target: { value: commands.getHTML() } });
  }

  useEffect(() => {
    if (toggle) {
      textareaRef.current && (textareaRef.current.value = htmlRef.current);
    } else {
      commands.setHTML(htmlRef.current);
    }
  }, [toggle, commands]);

  useEffect(() => {
    if (htmlRef.current !== value) {
      commands.setHTML((htmlRef.current = value));
      updates(true);
      setDirty(false);
    }
  }, [value, commands, updates]);

  useEffect(() => {
    initRef.current && onDirty && onDirty(dirty);
  }, [dirty, onDirty]);

  useEffect(() => {
    initRef.current = true;
    // Chrome and Edge supports this
    document.execCommand("defaultParagraphSeparator", false, "p");

    // firefox uses attributes for some commands
    document.execCommand("styleWithCSS", false, true);
    document.execCommand("insertBrOnReturn", false, false);
  }, []);

  useEffect(() => {
    if (autoFocus) {
      toggle && textareaRef.current.focus();
      !toggle && contentEditableRef.current.focus();
    }
  }, [toggle, autoFocus]);

  return (
    <div className="relative">
      <div ref={containerRef} className="custom-html-editor-container">
        {actions.length > 0 && (
          <Toolbar
            toggle={toggle}
            actions={actions}
            onAction={onAction}
            emitChange={emitChange}
          />
        )}
        {toggle ? (
          <textarea
            ref={textareaRef}
            placeholder={placeholder}
            onChange={handleChange}
            onBlur={handleBlur}
          />
        ) : (
          <div
            contentEditable="true"
            className="custom-html-editor-content"
            data-placeholder={placeholder}
            ref={contentEditableRef}
            onKeyDown={onKeyDown}
            onMouseDown={onMouseDown}
            onMouseUp={onMouseUp}
            onDoubleClick={(e) => onMouseDown(e)}
            onSelect={(e) => onMouseDown(e)}
            onPaste={onPaste}
            onInput={emitChange}
            onBlur={emitBlur}
            onFocus={emitFocus}
            {...(hijackMenu ? { onContextMenu } : {})}
          ></div>
        )}
      </div>
      <Popup
        data={popup}
        commands={commands}
        container={containerRef.current}
        getEditor={getEditor}
      />
    </div>
  );
}

// HTMLEditor.propTypes = {
//   actions: PropTypes.array,
//   placeholder: PropTypes.string,
//   hijackMenu: PropTypes.bool,
//   value: PropTypes.string,
//   onChange: PropTypes.func,
// };

HTMLEditor.defaultProps = {
  actions: [],
  placeholder: "",
  hijackMenu: false,
  value: "",
};

export default HTMLEditor;
