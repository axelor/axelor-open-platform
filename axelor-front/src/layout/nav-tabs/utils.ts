export function getActiveTabId(level = 0) {
  const elems = [
    document.querySelector(`[data-tab-content][data-tab-active='true']`),
    ...document.querySelectorAll("body > [data-dialog='true']"),
  ];

  for (let i = 0; i < level; ++i) {
    elems.pop();
  }

  const elem = elems[elems.length - 1] as HTMLElement;
  const parent = elem && (elem.querySelector("[data-view-id]") as HTMLElement);

  return parent?.getAttribute("data-view-id");
}
