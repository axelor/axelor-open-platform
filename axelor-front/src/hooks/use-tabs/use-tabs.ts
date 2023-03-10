import { findActionView } from "@/services/client/meta-cache";
import { ActionView } from "@/services/client/meta.types";
import { atom, useAtomValue, useSetAtom } from "jotai";
import { useCallback } from "react";

export type Tab = {
  id: string;
  title: string;
  dirty?: boolean;
  view: ActionView;
};

const tabAtom = atom<Tab | null>(null);
const tabsAtom = atom<Tab[]>([]);

const openAtom = atom(null, (get, set, tab: Tab) => {
  const tabs = get(tabsAtom);
  const curr = get(tabAtom);
  if (curr === tab) return;
  const found = tabs.find((x) => x.id === tab.id);
  if (found) {
    set(tabAtom, found);
  } else {
    set(tabsAtom, (state) => [...state, tab]);
    set(tabAtom, tab);
  }
});

const closeAtom = atom(null, (get, set, tab: Tab) => {
  const tabs = get(tabsAtom);
  const index = tabs.findIndex((x) => x.id === tab.id);
  if (index > -1) {
    const prev = tabs[index - 1] ?? null;
    set(tabsAtom, (state) => state.filter((x) => x.id !== tab.id));
    set(tabAtom, prev);
  }
});

export function useTabs() {
  const active = useAtomValue(tabAtom);
  const items = useAtomValue(tabsAtom);

  const openTab = useSetAtom(openAtom);
  const close = useSetAtom(closeAtom);

  const open = useCallback(
    async (tab: Tab | string) => {
      const id = typeof tab === "string" ? tab : tab.id;
      let found = items.find((x) => x.id === id);
      if (found === undefined) {
        const view = await findActionView(id);
        if (view) {
          const { title } = view;
          found = {
            id,
            title,
            view,
          };
        }
      }

      if (found) {
        openTab(found);
      }

      return found ?? null;
    },
    [items, openTab]
  );

  return {
    active,
    items,
    open,
    close,
  };
}
