import {
  WritableAtom,
  atom,
  getDefaultStore,
  useAtomValue,
  useSetAtom,
} from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { isEqual, isNil, omitBy } from "lodash";
import { useCallback } from "react";

import { findActionView } from "@/services/client/meta-cache";
import { ActionView, SavedFilter } from "@/services/client/meta.types";
import { useRoute } from "../use-route";

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
  selectedId?: number;
  readonly?: boolean;
  recordId?: boolean;
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
   * Previous view type.
   *
   */
  prevType?: string;

  /**
   * Whether the tab is dirty
   *
   * Can be used to show confirmation dialong before closing the tab.
   */
  dirty?: boolean;

  /**
   * Keeps track of filters per tab.
   *
   */
  filters?: SavedFilter[];

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
   * Whether this tab is a popup view.
   *
   */
  readonly popup?: boolean;

  /**
   * Options for the popup views.
   *
   */
  readonly popupOptions?: {
    /**
     * Whether the popup should be displayed in fullscreen mode.
     *
     */
    fullScreen?: boolean;

    /**
     * Whether to show the toolbar.
     *
     */
    showToolbar?: boolean;

    /**
     * Whether to show the edit icon in grid popup.
     *
     */
    showEditIcon?: boolean;

    /**
     * Whether to allow multiple row selection in grid popup.
     *
     */
    multiSelect?: boolean;
  };

  /**
   * The volatile state of the tab
   *
   */
  state: TabAtom;
};

const tabsAtom = atom<{ active?: string; tabs: Tab[]; popups: Tab[] }>({
  tabs: [],
  popups: [],
});

const activeAtom = selectAtom(tabsAtom, (state) =>
  state.tabs.find((x) => x.id === state.active)
);

const viewName = (view: ActionView | string) =>
  typeof view === "string" ? view : view.name;

const getViewMode = (type: string) => {
  if (type === "form") return "edit";
  if (type === "grid") return "list";
  return type;
};

const getViewType = (mode: string) => {
  if (mode === "edit") return "form";
  if (mode === "list") return "grid";
  return mode;
};

/**
 * This function updates tab state of a specific view type.
 *
 * The target view type will be determined by the `options.type` or
 * `route.mode` or `state.type`.
 *
 * @param action the action name
 * @param state the `tab.state` to update
 * @param options the options for the view type
 * @returns updated `state`
 */
const updateTabState = (
  action: string,
  state: TabState,
  options: {
    type?: string;
    route?: Partial<TabRoute>;
    props?: Record<string, any>;
  }
) => {
  const { route, props } = options;
  const type = getViewType(options.type ?? route?.mode ?? state.type);
  const mode = getViewMode(type);

  const { id = "", qs = "" } = route ?? state.routes?.[type] ?? {};

  const prevRoute = omitBy(state.routes?.[type], isNil) as TabRoute;
  const nextRoute = omitBy({ action, mode, id, qs }, isNil) as TabRoute;

  const prevProps = omitBy(state.props?.[type], isNil);
  const nextProps =
    props === null ? {} : omitBy({ ...prevProps, ...props }, isNil);

  const routeSame = isEqual(prevRoute, nextRoute);
  const propsSame = isEqual(prevProps, nextProps);

  if (routeSame && propsSame && type === state.type) {
    return state;
  }

  const prevType = type !== state.type ? state.type : state.prevType;
  const newState = { ...state, type, prevType };

  if (!routeSame) newState.routes = { ...newState.routes, [type]: nextRoute };
  if (!propsSame) newState.props = { ...newState.props, [type]: nextProps };

  return newState;
};

/**
 * Open the given view with the optional view route and props.
 *
 * @param view the action-view or name
 * @param options the options for the view type
 * @returns a Tab or null if no tab was opened (due to non-existance of the action-view)
 */
export type OpenTab = (
  view: ActionView | string,
  options?: {
    type?: string;
    route?: Omit<TabRoute, "action">;
    props?: Record<string, any>;
  }
) => Promise<Tab | null>;

/**
 * Close the given view.
 *
 * @param view the action-view or name
 * @param type the intial view type
 */
export type CloseTab = (view: ActionView | string) => void;

/**
 * Initialize a view tab for the given action-view.
 *
 * @param view the action-view or name
 * @param options additional options for a view type
 * @returns a Tab or null if unable to find the action-view
 */
export async function initTab(
  view: ActionView | string,
  options?: {
    type?: string;
    route?: Omit<TabRoute, "action">;
    props?: Record<string, any>;
  }
) {
  const { route, props } = options ?? {};
  const actionName = viewName(view);
  const actionView =
    typeof view === "object" && view.views?.length
      ? view
      : await findActionView(actionName);

  if (actionView) {
    const { name: id, title } = actionView;
    const type = getViewType(options?.type ?? actionView.viewType);
    const initState = updateTabState(id, { type, title }, { route, props });

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
            const props = arg?.props?.[type] ?? prev.props?.[type];
            return updateTabState(id, { ...state, ...arg }, { route, props });
          });
        }
      }
    );

    const popup = Boolean(actionView.params?.popup);
    const popupOptions = {
      fullScreen: Boolean(actionView.params?.["popup-maximized"]),
      showToolbar: actionView.params?.["show-toolbar"] !== false,
      showEditIcon: actionView.params?.["_popup-edit-icon"] !== false,
      multiSelect: actionView.params?.["_popup-multi-select"] !== false,
    };

    const tab: Tab = {
      id,
      title,
      action: actionView,
      state: state,
      popup,
      popupOptions,
    };

    return tab;
  }

  return null;
}

const openTabAtom = atom(
  null,
  async (
    get,
    set,
    view: ActionView | string,
    options: {
      type?: string;
      route?: Omit<TabRoute, "action">;
      props?: Record<string, any>;
    } = {}
  ): Promise<Tab | null> => {
    const { active, tabs, popups } = get(tabsAtom);

    const name = viewName(view);
    const found =
      tabs.find((x) => x.id === name) ?? popups.find((x) => x.id === name);

    if (found) {
      const viewState = get(found.state);
      const newState = updateTabState(name, viewState, options);
      set(found.state, newState);
    }

    if (found && found.id === active) return null;
    if (found && tabs.includes(found)) {
      set(tabsAtom, (prev) => ({ ...prev, active: name }));
    }
    if (found) {
      return found;
    }

    const tab = await initTab(view, options);

    if (tab) {
      set(tabsAtom, (state) => {
        const { active, tabs, popups } = state;
        const newState = tab.popup
          ? { active, tabs, popups: [...popups, tab] }
          : { active: tab.id, tabs: [...tabs, tab], popups };
        return newState;
      });
    }

    return tab;
  }
);

const closeTabAtom = atom(null, async (get, set, view: ActionView | string) => {
  const { active, tabs, popups } = get(tabsAtom);
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
      popups,
    });
  }

  const popup = popups.find((x) => x.id === name);
  if (popup) {
    const newPopups = popups.filter((x) => x.id !== name);
    set(tabsAtom, {
      active,
      tabs,
      popups: newPopups,
    });
  }
});

/**
 * This hook can be used to handle view tabs/popups.
 *
 * @returns current state of opened views and open/close
 *          methods to open new views or close a view.
 */
export function useTabs() {
  const { tabs: items, popups } = useAtomValue(tabsAtom);
  const active = useAtomValue(activeAtom) ?? null;
  const { navigate } = useRoute();

  const open: OpenTab = useSetAtom(openTabAtom);
  const close: CloseTab = useAtomCallback(
    useCallback(
      (get, set, view) => {
        set(closeTabAtom, view);
        get(tabsAtom).tabs.length === 0 && navigate("/");
      },
      [navigate]
    )
  );

  return {
    active,
    items,
    popups,
    open,
    close,
  };
}

// for internal use only with action handler
export function openTab_internal(view: ActionView) {
  getDefaultStore().set(openTabAtom, view);
}
