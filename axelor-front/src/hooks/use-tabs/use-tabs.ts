import { findActionView } from "@/services/client/meta-cache";
import { ActionView } from "@/services/client/meta.types";
import { atom, useAtomValue, useSetAtom } from "jotai";

export type Tab = {
  id: string;
  title: string;
  dirty?: boolean;
  view: ActionView;
};

const tabAtom = atom<Tab | null>(null);
const tabsAtom = atom<Tab[]>([]);

const viewName = (view: ActionView | string) =>
  typeof view === "string" ? view : view.name;

const openAtom = atom(
  null,
  async (get, set, view: ActionView | string): Promise<Tab | null> => {
    const tabs = get(tabsAtom);
    const curr = get(tabAtom);

    const name = viewName(view);
    const found = tabs.find((x) => x.id === name);

    if (found && curr === found) return null;
    if (found) {
      set(tabAtom, found);
      return found;
    }

    const actionView = await findActionView(name);
    if (actionView) {
      const { name: id, title } = actionView;
      const tab = { id, title, view: actionView };
      set(tabsAtom, (state) => [...state, tab]);
      set(tabAtom, tab);
      return tab;
    }

    return null;
  }
);

const closeAtom = atom(null, async (get, set, view: ActionView | string) => {
  const tabs = get(tabsAtom);
  const name = viewName(view);
  const index = tabs.findIndex((x) => x.id === name);
  if (index > -1) {
    const prev = tabs[index - 1] ?? null;
    set(tabsAtom, (state) => state.filter((x) => x.id !== name));
    set(tabAtom, prev);
  }
});

export function useTabs() {
  const active = useAtomValue(tabAtom);
  const items = useAtomValue(tabsAtom);

  const open = useSetAtom(openAtom);
  const close = useSetAtom(closeAtom);

  return {
    active,
    items,
    open,
    close,
  };
}
