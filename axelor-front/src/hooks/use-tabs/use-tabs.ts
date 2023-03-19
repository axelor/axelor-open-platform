import { PrimitiveAtom, atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";

import { navigate } from "@/routes";
import { findActionView } from "@/services/client/meta-cache";
import { ActionView } from "@/services/client/meta.types";

export type TabState = {
  readonly action: ActionView;
  readonly model?: string;
  title: string;
  type: string;
  dirty?: boolean;
  route?: {
    mode?: string;
    id?: string;
    qs?: Record<string, string>;
  };
};

export type TabAtom = PrimitiveAtom<TabState>;

export type Tab = {
  id: string;
  title: string;
  state: TabAtom;
};

const tabsAtom = atom<{ active?: string; tabs: Tab[] }>({
  tabs: [],
});

const activeAtom = selectAtom(tabsAtom, (state) =>
  state.tabs.find((x) => x.id === state.active)
);

const viewName = (view: ActionView | string) =>
  typeof view === "string" ? view : view.name;

const openAtom = atom(
  null,
  async (get, set, view: ActionView | string): Promise<Tab | null> => {
    const { active, tabs } = get(tabsAtom);

    const name = viewName(view);
    const found = tabs.find((x) => x.id === name);

    if (found && found.id === active) return null;
    if (found) {
      set(tabsAtom, (prev) => {
        return { ...prev, active: name };
      });
      return found;
    }

    const actionView = await findActionView(name);
    if (actionView) {
      const { name: id, title } = actionView;
      const tab = {
        id,
        title,
        view: actionView,
        state: atom<TabState>({
          action: actionView,
          model: actionView.model,
          type: actionView.viewType,
          title,
        }),
      };
      set(tabsAtom, (state) => {
        return {
          active: tab.id,
          tabs: [...state.tabs, tab],
        };
      });
      return tab;
    }

    return null;
  }
);

const closeAtom = atom(null, async (get, set, view: ActionView | string) => {
  const { active, tabs } = get(tabsAtom);
  const name = viewName(view);
  const found = tabs.find((x) => x.id === name);
  if (found) {
    let index = tabs.indexOf(found);
    let next = active;

    if (next === name) {
      next = tabs[index + 1]?.id ?? tabs[index - 1]?.id;
    }

    const newTabs = tabs.filter((x) => x.id !== name);

    set(tabsAtom, {
      active: next,
      tabs: newTabs,
    });

    // if it was a last tab
    if (newTabs.length === 0) {
      navigate("/");
    }
  }
});

export function useTabs() {
  const { tabs: items } = useAtomValue(tabsAtom);
  const active = useAtomValue(activeAtom) ?? null;

  const open = useSetAtom(openAtom);
  const close = useSetAtom(closeAtom);

  return {
    active,
    items,
    open,
    close,
  };
}
