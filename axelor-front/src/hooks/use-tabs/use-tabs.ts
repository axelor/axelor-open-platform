import { WritableAtom, atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";
import { isEqual } from "lodash";

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

export type TabAtom = WritableAtom<TabState, Partial<TabState>[], void>;

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

const getViewMode = (type: string, mode?: string) => {
  if (type === "form") {
    if (mode === "view" || mode === "edit") return mode;
    return "view";
  }
  if (type === "grid") return "list";
  return type;
};

const getViewType = (mode: string) => {
  if (mode === "view" || mode === "edit") return "form";
  if (mode === "list") return "grid";
  return mode;
};

const openAtom = atom(
  null,
  async (
    get,
    set,
    view: ActionView | string,
    route?: TabState["route"]
  ): Promise<Tab | null> => {
    const { active, tabs } = get(tabsAtom);

    const name = viewName(view);
    const found = tabs.find((x) => x.id === name);

    if (found) {
      const viewState = get(found.state);
      const type = getViewType(route?.mode ?? viewState.type);
      set(found.state, {
        type,
        route: {
          ...viewState.route,
          ...route,
        },
      });
    }

    if (found && found.id === active) return null;
    if (found) {
      set(tabsAtom, (prev) => ({ ...prev, active: name }));
      return found;
    }

    const actionView = await findActionView(name);
    if (actionView) {
      const { name: id, title, model, viewType } = actionView;
      const type = getViewType(route?.mode ?? viewType);
      const mode = getViewMode(type, route?.mode);

      const tabAtom = atom<TabState>({
        action: actionView,
        model,
        type,
        title,
        route: {
          ...route,
          mode,
        },
      });

      // create a derived atom to intercept mutation
      const state: TabAtom = atom(
        (get) => get(tabAtom),
        (get, set, arg) => {
          const state = get(tabAtom);
          if (state === arg) {
            return;
          }

          const { action, ...prev } = state;
          const value = { ...prev, ...arg };

          if (isEqual(value, prev)) {
            return;
          }

          // type changed, update route mode
          if (prev.type !== value.type) {
            const mode = getViewMode(value.type);
            value.route = {
              ...value.route,
              mode,
            };
          }

          set(tabAtom, (state) => ({ action, ...value }));
        }
      );

      const tab = {
        id,
        title,
        state: state,
      };

      set(tabsAtom, (state) => ({
        active: tab.id,
        tabs: [...state.tabs, tab],
      }));

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
