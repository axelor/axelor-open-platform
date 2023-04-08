import { atom, useAtom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { useCallback } from "react";

import { Tab, TabAtom, TabProps, TabRoute, useTabs } from "@/hooks/use-tabs";
import { SavedFilter } from "@/services/client/meta.types";

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
 * This scoped hook can be used to access current tab state.
 *
 * @returns tuple of current state and setter
 */
export function useViewState() {
  const tab = useViewTab();
  return useAtom(tab.state);
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

  const switchTo = useCallback<SwitchTo>(
    (type, options) => {
      if (action) {
        open(action, { type, ...options });
      }
    },
    [action, open]
  );

  return switchTo;
}

/**
 * This hook can be used to keep track of view specific state in tab.
 *
 */
export function useViewProps() {
  const [{ type, props }, setViewState] = useViewState();

  const state = props?.[type];
  const setState = useCallback(
    (partial: Partial<TabProps>) => {
      const newState = {
        ...state,
        ...partial,
      };

      const newProps = {
        ...props,
        [type]: newState,
      };

      setViewState({ props: newProps });
    },
    [props, setViewState, state, type]
  );

  return [state, setState] as const;
}

/**
 * This hook can be used to get/set filters of tab
 *
 */
export function useViewFilters() {
  const [{ filters: state }, setViewState] = useViewState();

  const setState = useCallback(
    (filters: SavedFilter[]) => {
      setViewState({ filters });
    },
    [setViewState]
  );

  return [state, setState] as const;
}

/**
 * This hook should be used by views to get the current route options.
 *
 * @param type the view type for which to get the route options
 * @returns TabRoute option of the given view type
 */
export function useViewRoute(type: string) {
  const [{ routes }] = useViewState();
  const options = routes?.[type] ?? {};
  return options as TabRoute;
}
