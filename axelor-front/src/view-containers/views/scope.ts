import { SetStateAction, useCallback, useEffect, useMemo } from "react";
import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { focusAtom } from "jotai-optics";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { isEqual } from "lodash";

import {
  Tab,
  TabAtom,
  TabProps,
  TabRoute,
  TabState,
  useTabs,
} from "@/hooks/use-tabs";
import { useFormScope } from "@/views/form/builder/scope";
import { usePrepareContext } from "@/views/form/builder";
import { dialogs } from "@/components/dialogs";
import { DataContext } from "@/services/client/data.types";

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
 * This scoped hook can be used to access view context.
 *
 * @returns Context
 */
export function useViewContext() {
  const { action, dashlet } = useViewTab();
  const { formAtom } = useFormScope();
  const getFormContext = usePrepareContext(formAtom);

  const recordId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom])
  );

  return useCallback(
    () =>
      (dashlet || recordId
        ? {
            ...getFormContext(),
            ...action.context,
          }
        : action.context) as DataContext,
    [dashlet, recordId, action.context, getFormContext]
  );
}

/**
 * This hook can be used to access current tab state.
 * @param selector the selector function
 * @returns selected state value
 */
export function useSelectViewState<T>(selector: (state: TabState) => T) {
  const tab = useViewTab();
  return useAtomValue(
    useMemo(
      () => selectAtom(tab.state, selector, isEqual),
      [tab.state, selector]
    )
  );
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
          const hasView = tab.action?.views?.some((v) => v.type === type);
          if (!hasView) return;
          if (state.dirty && state.type !== type) {
            set(tab.state, { dirty: false });
          }
          open(action, { type, ...options });
        }
      },
      [action, open, tab.state, tab.action]
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
      (get, set, setter: SetStateAction<TabProps>) => {
        const state = get(tab.state);
        const { type, props } = state;
        const viewState = state.props?.[type];

        const newViewState = {
          ...viewState,
          ...(typeof setter === "function" ? setter(viewState || {}) : setter),
        };

        const newProps = {
          ...props,
          [type]: newViewState,
        };

        set(tab.state, { ...state, props: newProps });
      },
      [tab.state]
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

export function useViewConfirmDirty() {
  const tab = useViewTab();
  const canConfirm = tab.action.params?.["show-confirm"] !== false;
  return useCallback(
    (
      check: () => Promise<boolean>,
      callback: () => Promise<any>,
      options?: {
        title?: string;
        content?: React.ReactNode;
      }
    ) =>
      dialogs.confirmDirty(
        async () => canConfirm && (await check()),
        callback,
        options
      ),
    [canConfirm]
  );
}
