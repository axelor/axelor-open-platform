import { i18n } from "@/services/client/i18n";
import {
  ColorPalette as ColorPalettePopup,
  CreateLink as CreateLinkPopup,
  CreateImage as CreateImagePopup,
  Dropdown as DropdownPopup,
} from "./popup";

// prevent default
export function cancelEvent(e) {
  e.preventDefault();
  e.stopPropagation();
}

export function addEvent(element, type, handler, useCapture) {
  element.addEventListener(type, handler, useCapture ? true : false);
}

export function removeEvent(element, type, handler, useCapture) {
  element.removeEventListener(type, handler, useCapture ? true : false);
}

export function isOrContainsNode(ancestor, descendant, within) {
  let node = within ? descendant.parentNode : descendant;
  while (node) {
    if (node === ancestor) return true;
    node = node.parentNode;
  }
  return false;
}

export function isMediaNode(node) {
  return [
    "IMG",
    "PICTURE",
    "SVG",
    "VIDEO",
    "AUDIO",
    "IFRAME",
    "MAP",
    "OBJECT",
    "EMBED",
  ].includes(node.nodeName);
}

export function selectionInside(containerNode, force) {
  // selection inside editor?
  const sel = window.getSelection();
  if (
    isOrContainsNode(containerNode, sel.anchorNode) &&
    isOrContainsNode(containerNode, sel.focusNode)
  )
    return true;
  // selection at least partly outside editor
  if (!force) return false;
  // force selection to editor
  const range = document.createRange();
  range.selectNodeContents(containerNode);
  range.collapse(false);
  sel.removeAllRanges();
  sel.addRange(range);
  return true;
}

// read file as data-url
export function filecontents(file, callback) {
  // base64 a 2GB video is insane: 16MB should work for an average image
  if (file.size > 0x1000000) return;

  // read file as data-url
  const normalize_dataurl = function (orientation) {
    const filereader = new FileReader();
    filereader.onload = function (e) {
      if (!orientation || orientation === 1 || orientation > 8)
        return callback(file.type, e.target.result);
      // normalize
      const img = new Image();
      img.src = e.target.result;
      img.onload = function () {
        let width = img.width;
        let height = img.height;
        if (width > height) {
          const max_width = 4096;
          if (width > max_width) {
            height *= max_width / width;
            width = max_width;
          }
        } else {
          const max_height = 4096;
          if (height > max_height) {
            width *= max_height / height;
            height = max_height;
          }
        }
        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");
        canvas.width = width;
        canvas.height = height;
        ctx.save();
        if (orientation > 4) {
          canvas.width = height;
          canvas.height = width;
        }
        switch (orientation) {
          case 2:
            ctx.translate(width, 0);
            ctx.scale(-1, 1);
            break;
          case 3:
            ctx.translate(width, height);
            ctx.rotate(Math.PI);
            break;
          case 4:
            ctx.translate(0, height);
            ctx.scale(1, -1);
            break;
          case 5:
            ctx.rotate(0.5 * Math.PI);
            ctx.scale(1, -1);
            break;
          case 6:
            ctx.rotate(0.5 * Math.PI);
            ctx.translate(0, -height);
            break;
          case 7:
            ctx.rotate(0.5 * Math.PI);
            ctx.translate(width, -height);
            ctx.scale(-1, 1);
            break;
          case 8:
            ctx.rotate(-0.5 * Math.PI);
            ctx.translate(-width, 0);
            break;
          default:
            break;
        }
        ctx.drawImage(img, 0, 0, width, height);
        ctx.restore();
        const dataURL = canvas.toDataURL("image/jpeg", 0.99);
        callback(file.type, dataURL);
      };
    };
    filereader.readAsDataURL(file);
  };
  if (!window.DataView) return normalize_dataurl();

  // get orientation - https://stackoverflow.com/questions/7584794/accessing-jpeg-exif-rotation-data-in-javascript-on-the-client-side
  const filereader = new FileReader();
  filereader.onload = function (e) {
    const contents = e.target.result;
    const view = new DataView(contents);
    // Not a JPEG at all?
    if (view.getUint16(0, false) !== 0xffd8) return normalize_dataurl();
    const length = view.byteLength;
    let offset = 2;
    while (offset < length) {
      // Missing EXIF?
      if (view.getUint16(offset + 2, false) <= 8) return normalize_dataurl();
      const marker = view.getUint16(offset, false);
      offset += 2;
      if (marker === 0xffe1) {
        if (view.getUint32((offset += 2), false) !== 0x45786966)
          return normalize_dataurl();
        const little = view.getUint16((offset += 6), false) === 0x4949;
        offset += view.getUint32(offset + 4, little);
        const tags = view.getUint16(offset, little);
        offset += 2;
        for (let i = 0; i < tags; ++i) {
          if (view.getUint16(offset + i * 12, little) === 0x0112) {
            const orientation = view.getUint16(offset + i * 12 + 8, little);
            return normalize_dataurl(orientation);
          }
        }
      } else if ((marker & 0xff00) !== 0xff00) break;
      else offset += view.getUint16(offset, false);
    }
    return normalize_dataurl();
  };
  filereader.readAsArrayBuffer(file);
}

export function collapseSelectionEnd() {
  const sel = window.getSelection();
  if (!sel.isCollapsed) {
    // Form-submits via Enter throw 'NS_ERROR_FAILURE' on Firefox 34
    try {
      sel.collapseToEnd();
    } catch (e) {}
  }
}

export function pasteHtmlAtCaret(containerNode, html) {
  const sel = window.getSelection();
  if (sel.getRangeAt && sel.rangeCount) {
    let range = sel.getRangeAt(0);
    // Range.createContextualFragment() would be useful here but is
    // only relatively recently standardized and is not supported in
    // some browsers (IE9, for one)
    const el = document.createElement("div");
    el.innerHTML = html;
    const frag = document.createDocumentFragment();
    let node, lastNode;
    while ((node = el.firstChild)) {
      lastNode = frag.appendChild(node);
    }
    if (isOrContainsNode(containerNode, range.commonAncestorContainer)) {
      range.deleteContents();
      range.insertNode(frag);
    } else {
      containerNode.appendChild(frag);
    }
    // Preserve the selection
    if (lastNode) {
      range = range.cloneRange();
      range.setStartAfter(lastNode);
      range.collapse(true);
      sel.removeAllRanges();
      sel.addRange(range);
    }
  }
}

export function getSelectionCollapsed() {
  const sel = window.getSelection();
  if (sel.isCollapsed) return true;
  return false;
}

function getSelectedNodes(containerNode) {
  const sel = window.getSelection();
  if (!sel.rangeCount) return [];
  const nodes = [];
  for (let i = 0; i < sel.rangeCount; ++i) {
    const range = sel.getRangeAt(i);
    let node = range.startContainer;
    let endNode = range.endContainer;
    while (node) {
      // add this node?
      if (node !== containerNode) {
        let node_inside_selection = false;
        if (sel.containsNode)
          node_inside_selection = sel.containsNode(node, true);
        // IE11
        else {
          // http://stackoverflow.com/questions/5884210/how-to-find-if-a-htmlelement-is-enclosed-in-selected-text
          const noderange = document.createRange();
          noderange.selectNodeContents(node);
          for (let i = 0; i < sel.rangeCount; ++i) {
            const range = sel.getRangeAt(i);
            // start after or end before -> skip node
            if (
              range.compareBoundaryPoints(range.END_TO_START, noderange) >= 0 &&
              range.compareBoundaryPoints(range.START_TO_END, noderange) <= 0
            ) {
              node_inside_selection = true;
              break;
            }
          }
        }
        if (node_inside_selection) nodes.push(node);
      }
      // http://stackoverflow.com/questions/667951/how-to-get-nodes-lying-inside-a-range-with-javascript
      const nextNode = function (node, container) {
        if (node.firstChild) return node.firstChild;
        while (node) {
          if (node === container)
            // do not walk out of the container
            return null;
          if (node.nextSibling) return node.nextSibling;
          node = node.parentNode;
        }
        return null;
      };
      node = nextNode(node, node === endNode ? endNode : containerNode);
    }
  }
  // Fallback
  if (
    nodes.length === 0 &&
    isOrContainsNode(containerNode, sel.focusNode) &&
    sel.focusNode !== containerNode
  )
    nodes.push(sel.focusNode);
  return nodes;
}

export function getSelectionHtml(containerNode) {
  if (getSelectionCollapsed()) return null;
  const sel = window.getSelection();
  if (sel.rangeCount) {
    const container = document.createElement("div"),
      len = sel.rangeCount;
    for (let i = 0; i < len; ++i) {
      const contents = sel.getRangeAt(i).cloneContents();
      container.appendChild(contents);
    }
    return container.innerHTML;
  }
  return null;
}

export function findClosestAnchorNode(editor) {
  const nodes = getSelectedNodes(editor);
  let lastLink;
  for (let i = 0; i < nodes.length; ++i) {
    const node = nodes[i];
    const closest =
      node.closest || // latest
      function (selector) {
        // IE + Edge - https://github.com/nefe/You-Dont-Need-jQuery
        let node = this;
        while (node) {
          const matchesSelector =
            node.matches ||
            node.webkitMatchesSelector ||
            node.mozMatchesSelector ||
            node.msMatchesSelector;
          if (matchesSelector && matchesSelector.call(node, selector))
            return node;
          node = node.parentElement;
        }
        return null;
      };
    lastLink = closest.call(node, "a");
    if (lastLink) break;
  }
  return lastLink;
}

function checkParents(e, parents = []) {
  const result = [];
  for (let p = e && e.parentElement; p; p = p.parentElement) {
    parents.includes((p.tagName || "").toLowerCase()) && result.push(p);
  }
  return result;
}

export function normalizeHTML(html) {
  const div = document.createElement("div");
  div.innerHTML = html;

  const paras = div.querySelectorAll("p");

  for (let i = 0; i < paras.length; i++) {
    const el = paras[i];
    el.style.marginTop = "0px";
    el.style.marginBottom = "1em";
  }

  const lists = div.querySelectorAll("ol,ul");

  for (let i = 0; i < lists.length; i++) {
    const el = lists[i];
    if (!checkParents(el, ["ol", "ul"]).length) {
      el.style.marginTop = "0px";
      el.style.marginBottom = "1em";
    }
  }

  const blocks = div.querySelectorAll("blockquote");

  for (let i = 0; i < blocks.length; i++) {
    const el = blocks[i];
    el.style.margin = checkParents(el, ["blockquote"]).length
      ? "0 0 0 2em"
      : "0 0 1em 2em";
    el.style.border = "none";
    el.style.padding = "0";
  }

  return div.innerHTML;
}

const divider = (lite) => ({ divider: true, lite });
const getDropdown = (data, onClick, style) => ({
  popup: DropdownPopup,
  popupStyle: { left: 0 },
  popupProps: {
    data,
    onClick,
  },
});

export function getActions() {
  const _t = i18n.get;
  return [
    {
      lite: false,
      title: "Style",
      image: "\uf1dd",
      popup: DropdownPopup,
      ...getDropdown(
        {
          "<p>": _t("Normal"),
          "<pre>": <pre>{_t("Formatted")}</pre>,
          "<blockquote>": <blockquote>{_t("Blockquote")}</blockquote>,
          "<h1>": <h1>{_t("Header 1")}</h1>,
          "<h2>": <h2>{_t("Header 2")}</h2>,
          "<h3>": <h3>{_t("Header 3")}</h3>,
          "<h4>": <h4>{_t("Header 4")}</h4>,
          "<h5>": <h5>{_t("Header 5")}</h5>,
          "<h6>": <h6>{_t("Header 6")}</h6>,
        },
        (commands, format) => {
          commands.format(format);
          commands.closePopup();
        }
      ),
    },
    {
      lite: false,
      title: "Font",
      image: "\uf031",
      ...getDropdown(
        [
          '"Times New Roman", Times, serif',
          "Arial, Helvetica, sans-serif",
          '"Courier New", Courier, monospace',
          "Comic Sans, Comic Sans MS, cursive",
          "Impact, fantasy",
        ].reduce(
          (fonts, font) =>
            Object.assign(fonts, {
              [font]: (
                <span style={{ fontFamily: font }}>
                  {font.split(",")[0].replace(/"/g, "")}
                </span>
              ),
            }),
          {}
        ),
        (commands, font) => {
          try {
            document.execCommand("styleWithCSS", false, false);
            commands.fontName(font);
            commands.closePopup();
          } finally {
            document.execCommand("styleWithCSS", false, true);
          }
        }
      ),
    },
    {
      lite: false,
      title: "Font size",
      image: "\uf035",
      ...getDropdown(
        {
          1: <span style={{ fontSize: "x-small" }}>{_t("Smaller")}</span>,
          2: <span style={{ fontSize: "small" }}>{_t("Small")}</span>,
          3: <span style={{ fontSize: "medium" }}>{_t("Medium")}</span>,
          4: <span style={{ fontSize: "large" }}>{_t("Large")}</span>,
          5: <span style={{ fontSize: "x-large" }}>{_t("Larger")}</span>,
        },
        (commands, size) => {
          try {
            document.execCommand("styleWithCSS", false, false);
            commands.fontSize(size);
            commands.closePopup();
          } finally {
            document.execCommand("styleWithCSS", false, true);
          }
        }
      ),
    },
    divider(false),
    {
      title: "Bold (Ctrl+B)",
      image: "\uf032",
      command: "bold",
      hotkey: "b",
    },
    {
      title: "Italic (Ctrl+I)",
      image: "\uf033",
      command: "italic",
      hotkey: "i",
    },
    {
      title: "Underline (Ctrl+U)",
      image: "\uf0cd",
      command: "underline",
      hotkey: "u",
    },
    {
      title: "Strikethrough (Ctrl+S)",
      image: "\uf0cc",
      command: "strikeThrough",
      hotkey: "s",
    },
    {
      title: "Remove format",
      image: "\uf12d",
      command: "removeFormat",
    },
    divider(),
    {
      lite: false,
      title: "Text color",
      image: "\uf1fc",
      popup: ColorPalettePopup,
      popupProps: {
        onClick: (commands, color) => {
          commands.foreColor(color);
          commands.collapseSelection();
          commands.closePopup();
        },
      },
    },
    {
      lite: false,
      title: "Background color",
      image: "\uf043",
      popup: ColorPalettePopup,
      popupProps: {
        onClick: (commands, color) => {
          commands.highlight(color);
          commands.collapseSelection();
          commands.closePopup();
        },
      },
    },
    divider(false),
    {
      lite: false,
      title: "Insert link",
      image: "\uf08e",
      popup: CreateLinkPopup,
    },
    {
      lite: false,
      title: "Insert image",
      image: "\uf030",
      popup: CreateImagePopup,
    },
    divider(false),
    {
      title: "Left",
      image: "\uf036",
      command: "justifyLeft",
    },
    {
      title: "Center",
      image: "\uf037",
      command: "justifyCenter",
    },
    {
      title: "Right",
      image: "\uf038",
      command: "justifyRight",
    },
    {
      title: "To Justify",
      image: "\uf039",
      command: "justifyFull",
    },
    divider(),
    {
      title: "Ordered list",
      image: "\uf0cb",
      command: "insertOrderedList",
    },
    {
      title: "Unordered list",
      image: "\uf0ca",
      command: "insertUnorderedList",
    },
    divider(false),
    {
      lite: false,
      title: "Indent",
      image: "\uf03c",
      command: "indent",
    },
    {
      lite: false,
      title: "Outdent",
      image: "\uf03b",
      command: "outdent",
    },
    divider(false),
    {
      lite: false,
      title: "Normalize",
      image: "\uf0d0",
      command: "normalize",
    },
    {
      lite: false,
      title: "Code",
      image: "\uf121",
      command: "toggleCode",
    },
  ].map((item) => ({ ...item, title: item.title && _t(item.title) }));
}
