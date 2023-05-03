import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { useCallback, useEffect, useMemo } from "react";

import {
  Tab,
  TabAtom,
  TabProps,
  TabRoute,
  TabState,
  useTabs,
} from "@/hooks/use-tabs";
import { SavedFilter } from "@/services/client/meta.types";
import { focusAtom } from "jotai-optics";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { isEqual } from "lodash";

const fallbackAtom: TabAtom = atom(
  () => ({
    title: "",
    type: "",
  }),
  () => {}
);

const fallbackTab: Tab = {
  id: "",
  title: "",
  action: {
    name: "",
    title: "",
    viewType: "",
  },
  state: fallbackAtom,
};

export const ViewScope = createScope<Tab>(fallbackTab);

const viewMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(ViewScope);
  return atom(initialView);
});

/**
 * This scoped hook can be used to access current Tab.
 *
 * @returns current Tab
 */
export function useViewTab() {
  const viewAtom = useMolecule(viewMolecule);
  return useAtomValue(viewAtom);
}

/**
 * This scoped hook can be used to access current action view.
 *
 * @returns current ActionView
 */
export function useViewAction() {
  const tab = useViewTab();
  return tab.action;
}

/**
 * This hook can be used to access current tab state.
 * @param selector the selector function
 * @returns selected state value
 */
export function useSelectViewState<T>(selector: (state: TabState) => T) {
  const tab = useViewTab();
  return useAtomValue(selectAtom(tab.state, selector, isEqual));
}

interface SwitchTo {
  /**
   * Switch to the given view type.
   *
   * @param type view type
   * @param options additional options for the given view type
   */
  (
    type: string,
    options?: {
      /**
       * The route options for the given view type
       */
      route?: { mode?: string; id?: string; qs?: Record<string, string> };

      /**
       * The additional state for the given view type
       */
      props?: Record<string, any>;
    }
  ): void;
}

/**
 * This scoped hook can be used to switch between different
 * views or update route of the current tab.
 *
 * @returns a function to switch views or update route
 */
export function useViewSwitch() {
  const tab = useViewTab();
  const action = tab.id;
  const { open } = useTabs();

  const switchTo: SwitchTo = useAtomCallback(
    useCallback(
      (get, set, type, options) => {
        if (action) {
          const state = get(tab.state);
          if (state.dirty && state.type !== type) {
            set(tab.state, { dirty: false });
          }
          open(action, { type, ...options });
        }
      },
      [action, open, tab.state]
    )
  );

  return switchTo;
}

/**
 * This hook can be used to keep track of view specific state in tab.
 *
 */
export function useViewProps() {
  const tab = useViewTab();
  const { type, props } = useSelectViewState(
    useCallback(({ type, props }) => ({ type, props }), [])
  );

  const state = props?.[type];
  const setState = useAtomCallback(
    useCallback(
      (get, set, partial: Partial<TabProps>) => {
        const newState = {
          ...state,
          ...partial,
        };

        const newProps = {
          ...props,
          [type]: newState,
        };

        set(tab.state, { props: newProps });
      },
      [props, state, tab.state, type]
    )
  );

  return [state, setState] as const;
}

export function useViewDirtyAtom() {
  const tab = useViewTab();
  return useMemo(
    () => focusAtom(tab.state, (o) => o.prop("dirty").valueOr(false)),
    [tab.state]
  );
}

/**
 * This hook can be used to get/set filters of tab
 *
 */
export function useViewFilters() {
  const tab = useViewTab();
  const state = useSelectViewState(useCallback(({ filters }) => filters, []));
  const setState = useAtomCallback(
    useCallback(
      (get, set, filters: SavedFilter[]) => {
        set(tab.state, { filters });
      },
      [tab.state]
    )
  );

  return [state, setState] as const;
}

/**
 * This hook should be used by views to get the current route options.
 *
 * @param type the view type for which to get the route options
 * @returns TabRoute option of the given view type
 */
export function useViewRoute() {
  const { type, routes } = useSelectViewState(
    useCallback(({ type, routes }) => ({ type, routes }), [])
  );
  const options = routes?.[type] ?? {};
  return options as TabRoute;
}

export function useViewTabRefresh(viewType: string, refresh: () => void) {
  const tab = useViewTab();
  const type = useSelectViewState(useCallback(({ type }) => type, []));
  const handleRefresh = useCallback(
    (e: Event) => {
      if (
        e instanceof CustomEvent &&
        e.detail === tab.id &&
        type === viewType
      ) {
        refresh();
      }
    },
    [refresh, tab.id, type, viewType]
  );

  useEffect(() => {
    document.addEventListener("tab:refresh", handleRefresh);
    return () => {
      document.removeEventListener("tab:refresh", handleRefresh);
    };
  }, [handleRefresh]);
}
