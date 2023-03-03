import { ActionView } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";

const tabAtom = atom<ActionView | null>(null);
const tabsAtom = atom<ActionView[]>([]);

const openAtom = atom(null, (get, set, tab: ActionView) => {
  const tabs = get(tabsAtom);
  const curr = get(tabAtom);
  if (curr === tab) return;
  const found = tabs.find((x) => x.name === tab.name);
  if (found) {
    set(tabAtom, found);
  } else {
    set(tabsAtom, (state) => [...state, tab]);
    set(tabAtom, tab);
  }
});

const closeAtom = atom(null, (get, set, tab: ActionView) => {
  const tabs = get(tabsAtom);
  const index = tabs.findIndex((x) => x.name === tab.name);
  if (index > -1) {
    const prev = tabs[index - 1] ?? null;
    set(tabsAtom, (state) => state.filter((x) => x.name !== tab.name));
    set(tabAtom, prev);
  }
});

export function useTabs() {
  const [active] = useAtom(tabAtom);
  const [items] = useAtom(tabsAtom);

  const [, open] = useAtom(openAtom);
  const [, close] = useAtom(closeAtom);

  return {
    active,
    items,
    open,
    close,
  };
}
