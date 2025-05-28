import {
  PrimitiveAtom,
  atom,
  getDefaultStore,
  useAtomValue,
  useSetAtom,
} from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import isNil from "lodash/isNil";
import omitBy from "lodash/omitBy";
import { useCallback } from "react";

import { GridState } from "@axelor/ui/grid";

import { dialogs } from "@/components/dialogs";
import { findActionView } from "@/services/client/meta-cache";
import {
  ActionView,
  HtmlView,
  SavedFilter,
} from "@/services/client/meta.types";
import { DataStore } from "@/services/client/data-store";
import { session } from "@/services/client/session";
import { device } from "@/utils/device";

import { useRoute } from "../use-route";
import { DataRecord } from "@/services/client/data.types";
import { SearchPage } from "@/services/client/data";

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
  selectedId?: number;
  name?: string;
  canNew?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  readonly?: boolean;
  recordId?: boolean;
  showSingle?: boolean;
  dataStore?: DataStore;
  gridState?: GridState;
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
   * The current view name
   *
   */
  name?: string;

  /**
   * Previous view type.
   *
   */
  prevType?: string;

  /**
   * Whether the tab is dirty
   *
   * Can be used to show confirmation dialog before closing the tab.
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
export type TabAtom = PrimitiveAtom<TabState>;

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
   * Whether this tab is a dashlet view.
   *
   */
  readonly dashlet?: boolean;

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

    /**
     * The method called after grid search request
     */
    onGridSearch?: (
      records: DataRecord[],
      page: SearchPage,
      search?: Record<string, string>,
    ) => DataRecord[];
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
  state.tabs.find((x) => x.id === state.active),
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

const getDefaultViewType = (action: ActionView) => {
  const type = action.viewType;
  const isPopup = !!action.params?.["popup"];
  if (isPopup) return type;
  // on mobile try to use cards view by default
  if (type === "grid" && device.isMobile) {
    return action.views?.find((x) => x.type === "cards")?.type ?? type;
  }
  return type;
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
  },
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
 * @returns a Tab or null if no tab was opened (due to non-existence of the action-view)
 */
export type OpenTab = (
  view: ActionView | string,
  options?: {
    type?: string;
    route?: Omit<TabRoute, "action">;
    props?: Record<string, any>;
    tab?: boolean;
  },
) => Promise<Tab | null>;

/**
 * Close the given view.
 *
 * @param view the action-view or name
 * @param type the initial view type
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
    tab?: boolean;
    onGridSearch?: (
      records: DataRecord[],
      page: SearchPage,
      search?: Record<string, string>,
    ) => DataRecord[];
  },
) {
  const {
    route: initRoute,
    props,
    tab: initAsTab,
    onGridSearch,
  } = options ?? {};
  const actionName = viewName(view);
  const actionView =
    typeof view === "object" && view.views?.length
      ? view
      : await findActionView(actionName);

  if (actionView) {
    const { name: id, title, context, views = [] } = actionView;

    const defaultType = getDefaultViewType(actionView);
    const type = getViewType(options?.type ?? defaultType);
    
    const route = context?._showRecord && views.some(v => v.type === 'form')
      ? {
          ...initRoute,
          mode: "edit",
          id: context?._showRecord,
        }
      : initRoute;
    const initState = updateTabState(id, { type, title }, { route, props });

    const tabAtom = atom<TabState>(initState);

    // create a derived atom to intercept mutation
    const state: TabAtom = atom(
      (get) => get(tabAtom),
      (get, set, arg) => {
        const prev = get(tabAtom);
        const next = typeof arg === "function" ? arg(prev) : arg;
        if (prev !== next) {
          set(tabAtom, () => {
            const type = next?.type ?? prev.type;
            const route = next?.routes?.[type] ?? prev.routes?.[type];
            const props = next?.props?.[type] ?? prev.props?.[type];
            return updateTabState(id, next, { route, props });
          });
        }
      },
    );

    const popup = !initAsTab && Boolean(actionView.params?.popup);
    const dashlet = Boolean(actionView.params?.dashlet);
    const popupOptions = {
      fullScreen: Boolean(actionView.params?.["popup.maximized"]),
      showToolbar: actionView.params?.["show-toolbar"] !== false,
      showEditIcon: actionView.params?.["_popup-edit-icon"] !== false,
      multiSelect: actionView.params?.["_popup-multi-select"] !== false,
      onGridSearch,
    };

    const tab: Tab = {
      id,
      title,
      action: actionView,
      state: state,
      dashlet,
      popup,
      popupOptions,
    };

    return tab;
  }

  return null;
}

function isSingleTab() {
  const info = session.info;
  return (
    device.isMobile ||
    info?.view?.singleTab ||
    info?.user?.singleTab ||
    info?.view?.maxTabs === 0 ||
    info?.view?.maxTabs === 1
  );
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
      tab?: boolean;
    } = {},
  ): Promise<Tab | null> => {
    const { active, tabs, popups } = get(tabsAtom);

    const singleTab = isSingleTab();
    const activeTab = tabs.find((x) => x.id === active);

    const name = viewName(view);

    const hash = window.location.hash;

    if (hash && hash !== "#/" && !hash.startsWith("#/ds/")) {
      window.location.hash = "";
    }

    // special case of relational field popups
    if (name.startsWith("$selector")) {
      return name === view ? null : await initTab(view, options);
    }

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

    if (singleTab && activeTab) {
      // close current one
      const canConfirm = activeTab.action.params?.["show-confirm"] !== false;
      const closed = await dialogs.confirmDirty(
        async () => (canConfirm && get(activeTab.state).dirty) ?? false,
        async () => {},
      );
      if (!closed) return activeTab;
    }

    const tab = await initTab(view, options);

    // html view with target="_blank"
    if (
      tab?.action.viewType === "html" &&
      tab?.action.params?.target === "_blank"
    ) {
      const html = tab.action.views?.find((x) => x.type === "html") as HtmlView;
      const url = html?.name || html?.resource;
      if (url) {
        window.open(url, tab?.action.params?.target, "noopener,noreferrer");
      }
      return tab;
    }

    if (tab) {
      set(tabsAtom, (state) => {
        const { active, tabs, popups } = state;
        const found =
          tabs.find((x) => x.id === name) ?? popups.find((x) => x.id === name);
        if (found) {
          return state;
        }
        const newState = tab.popup
          ? { active, tabs, popups: [...popups, tab] }
          : {
              active: tab.id,
              tabs: singleTab ? [tab] : [...tabs, tab],
              popups,
            };
        return newState;
      });
    }

    return tab;
  },
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
      [navigate],
    ),
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
export function openTab_internal(
  view: string | ActionView,
  options: {
    type?: string;
    route?: Omit<TabRoute, "action">;
    props?: Record<string, any>;
  } = {},
) {
  getDefaultStore().set(openTabAtom, view, options);
}

// for internal use only with action handler
export function closeTab_internal(view: string | ActionView) {
  getDefaultStore().set(closeTabAtom, view);
}

export function useActiveTab_internal() {
  const active = getDefaultStore().get(activeAtom);
  const state = active ? getDefaultStore().get(active.state) : undefined;
  const setState = (state: Partial<TabState>) => {
    if (active) {
      getDefaultStore().set(active.state, (prev) => ({ ...prev, ...state }));
    }
  };
  return [state, setState] as const;
}
