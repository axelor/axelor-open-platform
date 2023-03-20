import { WritableAtom, atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";
import { isEqual, isNil, omitBy } from "lodash";

import { navigate } from "@/routes";
import { findActionView } from "@/services/client/meta-cache";
import { ActionView } from "@/services/client/meta.types";

export type TabRoute = {
  action: string;
  mode?: string;
  id?: string;
  qs?: Record<string, string>;
};

export type TabState = {
  title: string;
  type: string;
  dirty?: boolean;
  routes?: Record<string, TabRoute>;
};

export type TabAtom = WritableAtom<TabState, Partial<TabState>[], void>;

export type Tab = {
  readonly id: string;
  readonly title: string;
  readonly action: ActionView;
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

const updateRouteState = (state: TabState, route?: TabRoute) => {
  const { action, id, qs } = route ?? {};
  const type = getViewType(route?.mode ?? state.type);
  const mode = route?.mode ?? getViewMode(state.type);
  const prev = state.routes?.[type];
  const next = {
    ...prev,
    action,
    mode,
    id,
    qs,
  };

  let p = omitBy(prev, isNil) as any;
  let n = omitBy(next, isNil) as any;

  let routeSame = isEqual(p, n);
  if (routeSame && type === state.type) {
    return state;
  }

  if (routeSame) n = prev;

  return { ...state, type, routes: { ...state.routes, [type]: n } };
};

const openAtom = atom(
  null,
  async (
    get,
    set,
    view: ActionView | string,
    route?: TabRoute
  ): Promise<Tab | null> => {
    const { active, tabs } = get(tabsAtom);

    const name = viewName(view);
    const found = tabs.find((x) => x.id === name);

    if (found) {
      const viewState = get(found.state);
      const newState = updateRouteState(viewState, route);
      set(found.state, newState);
    }

    if (found && found.id === active) return null;
    if (found) {
      set(tabsAtom, (prev) => ({ ...prev, active: name }));
      return found;
    }

    const actionView = await findActionView(name);
    if (actionView) {
      const { name: id, title, viewType } = actionView;
      const type = getViewType(route?.mode ?? viewType);
      const mode = getViewMode(type, route?.mode);

      const initState = updateRouteState(
        { type, title },
        {
          ...route,
          mode,
          action: id,
        }
      );

      const tabAtom = atom<TabState>(initState);

      // create a derived atom to intercept mutation
      const state: TabAtom = atom(
        (get) => get(tabAtom),
        (get, set, arg) => {
          const prev = get(tabAtom);
          if (prev !== arg) {
            set(tabAtom, () => ({ ...prev, ...arg }));
          }
        }
      );

      const tab = {
        id,
        title,
        action: actionView,
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
