import { useRef, useEffect } from 'react';
import { findClosestAnchorNode } from '../utils';

function encodeHTML(text) {
  return text.replace(/[&<>"]/g, tag => {
    const charsToReplace = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' };
    return charsToReplace[tag] || tag;
  });
}

function LinkPopup({ commands, getEditor }) {
  const inputRef = useRef();
  const linkElement = findClosestAnchorNode(getEditor());

  function handleChange(e) {
    const url = e.target.value;
    url && linkElement && (linkElement.href = url);
  }

  function handleKeyPress(e) {
    const key = e.which || e.keyCode;
    const url = e.target.value.trim();
    if (key !== 13) return;
    if (!linkElement && url) {
      const $url = !/^[a-z0-9]+:\/\//.test(url) ? `http://${url}` : url;
      if (commands.getSelectedHTML()) {
        commands.insertLink($url);
      } else {
        commands.insertHTML('<a href="' + encodeHTML($url) + '">' + encodeHTML(url) + '</a>');
      }
    }
    commands.collapseSelection();
    commands.closePopup();
    getEditor().focus();
  }

  useEffect(() => {
    setTimeout(() => {
      inputRef.current.focus();
    }, 100);
  }, []);

  return (
    <input
      ref={inputRef}
      type="text"
      placeholder="www.example.com"
      defaultValue={linkElement ? linkElement.href : ''}
      onChange={handleChange}
      onKeyPress={handleKeyPress}
    />
  );
}

export default LinkPopup;
