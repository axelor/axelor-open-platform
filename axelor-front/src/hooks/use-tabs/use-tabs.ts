import { WritableAtom, atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";
import { isEqual, isNil, omitBy } from "lodash";

import { navigate } from "@/routes";
import { findActionView } from "@/services/client/meta-cache";
import { ActionView } from "@/services/client/meta.types";

/**
 * The route state of a specific view type.
 *
 */
export type TabRoute = {
  action: string;
  mode?: string;
  id?: string;
  qs?: Record<string, string>;
};

/**
 * Additional state of a specific view type.
 *
 */
export type TabProps = {
  scrollY?: number;
  scrollX?: number;
  selectedCell?: number[];
};

/**
 * The TabState represents volatile state of a tab.
 *
 */
export type TabState = {
  /**
   * The tab title
   *
   */
  title: string;

  /**
   * The current view type
   *
   */
  type: string;

  /**
   * Whether the tab is dirty
   *
   * Can be used to show confirmation dialong before closing the tab.
   */
  dirty?: boolean;

  /**
   * Keeps track of route options per view type.
   *
   */
  routes?: Record<string, TabRoute>;

  /**
   * Keep track of additional state per view type.
   *
   */
  props?: Record<string, TabProps>;
};

/**
 * This atom is used to keep track of tab state.
 *
 */
export type TabAtom = WritableAtom<TabState, Partial<TabState>[], void>;

/**
 * A tab represents an action-view visible as a tab or popup view.
 *
 */
export type Tab = {
  /**
   * Unique id of the tab, usually the name of the action-view.
   *
   * If id starts with `$`, browser route won't be handled for that tab.
   */
  readonly id: string;

  /**
   * The initial display title.
   *
   * The actual title is displayed from the `state`.
   */
  readonly title: string;

  /**
   * The action-view of this tab.
   *
   */
  readonly action: ActionView;

  /**
   * The volatile state of the tab
   *
   */
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

/**
 * This function updates route state of a specific view type.
 *
 * The target view type will be determined by the `route.mode`
 * or if not given by the `state.type`.
 *
 * @param action the action name
 * @param state the `tab.state` to update
 * @param route the route options to update
 * @returns updated `state`
 */
const updateRouteState = (
  action: string,
  state: TabState,
  route?: Partial<TabRoute>
) => {
  const { id, qs } = route ?? {};
  const type = getViewType(route?.mode ?? state.type);
  const mode = route?.mode ?? getViewMode(state.type);
  const prev = state.routes?.[type];
  const next = {
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

/**
 * Open the given view with the given optional route options.
 *
 * @param view the action-view or name
 * @param route the route options
 * @returns a Tab or null if no tab was opened (due to non-existance of the action-view)
 */
export type OpenTab = (
  view: ActionView | string,
  route?: Omit<TabRoute, "action">
) => Promise<Tab | null>;

/**
 * Close the given view.
 *
 * @param view the action-view or name
 */
export type CloseTab = (view: ActionView | string) => void;

const openAtom = atom(
  null,
  async (
    get,
    set,
    view: ActionView | string,
    route?: Omit<TabRoute, "action">
  ): Promise<Tab | null> => {
    const { active, tabs } = get(tabsAtom);

    const name = viewName(view);
    const found = tabs.find((x) => x.id === name);

    if (found) {
      const viewState = get(found.state);
      const newState = updateRouteState(name, viewState, route);
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
        id,
        { type, title },
        {
          ...route,
          mode,
        }
      );

      const tabAtom = atom<TabState>(initState);

      // create a derived atom to intercept mutation
      const state: TabAtom = atom(
        (get) => get(tabAtom),
        (get, set, arg) => {
          const prev = get(tabAtom);
          if (prev !== arg) {
            set(tabAtom, (state) => {
              const type = arg?.type ?? prev.type;
              const route = arg?.routes?.[type] ?? prev.routes?.[type];
              return updateRouteState(id, { ...state, ...arg }, route);
            });
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

/**
 * This hook can be used to handle view tabs/popups.
 *
 * @returns current state of opened views and open/close
 *          methods to open new views or close a view.
 */
export function useTabs() {
  const { tabs: items } = useAtomValue(tabsAtom);
  const active = useAtomValue(activeAtom) ?? null;

  const open: OpenTab = useSetAtom(openAtom);
  const close: CloseTab = useSetAtom(closeAtom);

  return {
    active,
    items,
    open,
    close,
  };
}
