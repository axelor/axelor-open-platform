import { atom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { useCallback } from "react";

import { Tab, TabAtom, TabProps, TabRoute, useTabs } from "@/hooks/use-tabs";

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
   */
  (type: string): void;

  /**
   * Update the route with the given options.
   *
   * @param options route options
   */
  (route: { mode?: string; id?: string; qs?: Record<string, string> }): void;
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
  const setViewState = useSetAtom(tab.state);
  const { open } = useTabs();

  const switchTo = useCallback<SwitchTo>(
    (arg) => {
      if (typeof arg === "string") {
        setViewState({ type: arg });
      } else {
        open(action, arg);
      }
    },
    [action, open, setViewState]
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
