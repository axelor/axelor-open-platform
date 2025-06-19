export function getActivePopups() {
  return document.querySelectorAll("body > [data-dialog='true']");
}

function getLastViewId(elems: (Element | null)[]) {
  const elem = elems[elems.length - 1] as HTMLElement;
  const parent = elem && (elem.querySelector("[data-view-id]") as HTMLElement);

  return parent?.getAttribute("data-view-id");
}

// if level is negative then it will always return active tab id
export function getActiveTabId(level = 0) {
  const elems = [
    document.querySelector(`[data-tab-content][data-tab-active='true']`),
    ...(level >= 0 ? getActivePopups() : []),
  ];

  for (let i = 0; i < level; ++i) {
    elems.pop();
  }

  return getLastViewId(elems);
}

export function getActivePopupTabId() {
  return getLastViewId([...getActivePopups()]);
}
