import { useRef, useEffect } from "react";
import { useTheme } from "@axelor/ui";
import { isOrContainsNode, addEvent, removeEvent } from "../utils";

function getPopupPosition(container, popup, { top, left }) {
  let node = container,
    popup_parent = node.offsetParent;

  while (node) {
    const node_style = getComputedStyle(node);
    if (node_style["position"] !== "static") break;
    left += node.offsetLeft;
    top += node.offsetTop;
    popup_parent = node;
    node = node.offsetParent;
  }
  // Move popup as high as possible in the DOM tree
  popup_parent.appendChild(popup);
  // Trim to viewport
  const rect = popup_parent.getBoundingClientRect();
  const documentElement = document.documentElement;
  const viewport_width = Math.min(
    window.innerWidth,
    Math.max(documentElement.offsetWidth, documentElement.scrollWidth)
  );
  const viewport_height = window.innerHeight;
  const popup_width = popup.offsetWidth; // accurate to integer
  const popup_height = popup.offsetHeight;
  if (rect.left + left < 1) left = 1 - rect.left;
  else if (rect.left + left + popup_width > viewport_width - 1)
    left = Math.max(
      1 - rect.left,
      viewport_width - 1 - rect.left - popup_width
    );
  if (rect.top + top < 1) top = 1 - rect.top;
  else if (rect.top + top + popup_height > viewport_height - 1)
    top = Math.max(1 - rect.top, viewport_height - 1 - rect.top - popup_height);
  // Set offset

  return { left: parseInt(left) + "px", top: parseInt(top) + "px" };
}

function Popup({ data, ...rest }) {
  const popupRef = useRef();
  const {
    component,
    props: componentProps,
    style: componentStyle,
    target,
  } = data || {};
  const { container, commands } = rest;
  const isOpen = Boolean(data);
  const rtl = useTheme().dir === "rtl";

  useEffect(() => {
    if (isOpen) {
      function popupOutsideClick(e) {
        let target = e.target || e.srcElement;
        if (target.nodeType === Node.TEXT_NODE)
          // defeat Safari bug
          target = target.parentNode;
        // Click within popup?
        if (isOrContainsNode(popupRef.current, target)) return;
        // close popup
        commands.closePopup();
      }
      addEvent(window, "mousedown", popupOutsideClick);
      return () => removeEvent(window, "mousedown", popupOutsideClick);
    }
  }, [isOpen, commands]);

  useEffect(() => {
    if (isOpen) {
      const popup = popupRef.current;
      const containerOffset = container.getBoundingClientRect();
      const buttonOffset = target.getBoundingClientRect();
      const popup_width = popup.offsetWidth;

      // Point is the top/bottom-center of the button
      const left =
        buttonOffset.left -
        containerOffset.left +
        parseInt(target.clientWidth / 2) -
        parseInt(popup_width / 2);
      const top = buttonOffset.top - containerOffset.top + target.offsetHeight;
      const positions = getPopupPosition(container, popup, { top, left });
      if (positions) {
        const { top, left } = positions;
        const getStyle = (e, defaultValue) =>
          (componentStyle || {})[e] !== undefined
            ? componentStyle[e]
            : defaultValue;
        popup.style.top = getStyle("top", top);
        if (rtl) {
          popup.style.right = getStyle("left", left);
          popup.style.left = "unset";
        } else {
          popup.style.left = getStyle("left", left);
        }
      }
    }
  }, [isOpen, rtl, container, target, componentStyle]);

  function renderComponent(Component, props) {
    return <Component {...rest} {...props} />;
  }

  return (
    <div ref={popupRef} className="custom-html-editor-popup">
      {component && renderComponent(component, componentProps)}
    </div>
  );
}

export default Popup;
