import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "bunshi/react";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import omit from "lodash/omit";
import pick from "lodash/pick";
import {
  SetStateAction,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { dialogs } from "@/components/dialogs";
import {
  Tab,
  TabAtom,
  TabProps,
  TabRoute,
  TabState,
  useTabs,
} from "@/hooks/use-tabs";
import { SearchOptions } from "@/services/client/data";
import { equalsIgnoreClean } from "@/services/client/data-utils";
import { ViewData } from "@/services/client/meta";
import { Schema, ViewType } from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";
import { findViewItem, findViewItems, processViewItem } from "@/utils/schema";
import { FormAtom, usePrepareContext } from "@/views/form/builder";
import {
  useCanDirty,
  useFormActiveHandler,
  useFormScope,
} from "@/views/form/builder/scope";
import { processContextValues } from "@/views/form/builder/utils";
import { useDashboardContext } from "@/views/dashboard/scope";

const fallbackAtom: TabAtom = atom(
  () => ({
    title: "",
    type: "",
  }),
  () => {},
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

export const MetaScope = createScope<ViewData<ViewType>>({
  view: { type: "form" },
  fields: {},
});

const metaMolecule = molecule((getMol, getScope) => {
  const meta = getScope(MetaScope);
  return atom<{ meta: typeof meta; items?: Schema[] }>({ meta });
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

const DEFAULT_CONTEXT = {};

/**
 * This scoped hook can be used to access view context.
 *
 * @returns Context
 */
export function useViewContext({
  dashboard = true,
}: { dashboard?: boolean } = {}) {
  const { action, dashlet } = useViewTab();
  const { formAtom } = useFormScope();
  const _dashboardContext = useDashboardContext();
  const dashboardContext = dashboard ? _dashboardContext : DEFAULT_CONTEXT;

  const setState = useFormActiveHandler();

  const getFormContext = usePrepareContext(formAtom);

  const _recordId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom]),
  );
  const [recordId, setRecordId] = useState(_recordId);

  useEffect(() => {
    // In case of form view dashlet, changing recordId will cause
    // new callback generation as below, this will eventually result into
    // search data of respective view, so now it will delay recordId change
    // unless component get activated
    setState(() => setRecordId(_recordId));
  }, [_recordId, setState]);

  return useCallback(
    (actionContext?: boolean) => {
      const formCtx = dashlet || recordId ? getFormContext() : undefined;
      const _parent = (() => {
        if (dashlet) {
          return {
            ...(actionContext ? omit(formCtx, ["_domainAction"]) : formCtx),
            ...pick(action.context ?? {}, [
              "_model",
              "_viewName",
              "_viewType",
              "_views",
            ]),
          };
        }
        return formCtx;
      })();

      return processContextValues(
        actionContext
          ? {
              ...action.context,
              _parent,
              ...dashboardContext,
            }
          : {
              ..._parent,
              ...action.context,
              ...dashboardContext,
            },
      );
    },
    [dashlet, recordId, dashboardContext, action.context, getFormContext],
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
      [tab.state, selector],
    ),
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
    },
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
            set(tab.state, { ...state, dirty: false });
          }
          open(action, { type, ...options });
        }
      },
      [action, open, tab.state, tab.action],
    ),
  );

  return switchTo;
}

/**
 * This hook can be used to set props of specific view state in tab.
 *
 */
export function useSetViewProps() {
  const tab = useViewTab();
  return useAtomCallback(
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
      [tab.state],
    ),
  );
}

/**
 * This hook can be used to keep track of view specific state in tab.
 *
 */
export function useViewProps() {
  const viewState = useSelectViewState(
    useCallback(({ type, props }) => ({ type, props }), []),
  );

  const state = viewState.props?.[viewState.type];
  const setState = useSetViewProps();

  return [state, setState] as const;
}

/**
 * This hook can be used to keep track of particular view specific state prop in tab.
 *
 */
export function useViewProp<T>(propName: keyof TabProps) {
  return useSelectViewState(
    useCallback((state) => state.props?.[state.type]?.[propName], [propName]),
  ) as T;
}

export function useViewDirtyAtom() {
  const tab = useViewTab();
  return useMemo(
    () =>
      focusAtom(
        tab.state,
        (o) => o.dirty ?? false,
        (o, v) => ({ ...o, dirty: v }),
      ),
    [tab.state],
  );
}

/**
 * Hook that provides a function to update the view dirty state
 * by comparing the record with the original.
 *
 * Intended use is for resetting dirty state after discarding/reverting changes.
 *
 * @param {FormAtom} formAtom
 * @returns A callback function to update view dirty state.
 */
export function useUpdateViewDirty(formAtom: FormAtom) {
  const viewDirtyAtom = useViewDirtyAtom();
  const canDirty = useCanDirty();

  const updateDirty = useAtomCallback(
    useCallback(
      (get, set) => {
        const { record, original } = get(formAtom);
        const dirty = !equalsIgnoreClean(record, original ?? {}, canDirty);
        set(viewDirtyAtom, dirty);
      },
      [canDirty, formAtom, viewDirtyAtom],
    ),
  );

  return updateDirty;
}

/**
 * This hook should be used by views to get the current route options.
 *
 * @param type the view type for which to get the route options
 * @returns TabRoute option of the given view type
 */
export function useViewRoute() {
  const { type, routes } = useSelectViewState(
    useCallback(({ type, routes }) => ({ type, routes }), []),
  );
  const options = routes?.[type] ?? {};
  return options as TabRoute;
}

export function useViewTabRefresh(
  viewType: string,
  refresh: (
    options?: Partial<SearchOptions> & {
      forceReload?: boolean;
    },
  ) => void,
) {
  const tab = useViewTab();
  const type = useSelectViewState(useCallback(({ type }) => type, []));
  const handleRefresh = useCallback(
    (e: Event) => {
      if (
        e instanceof CustomEvent &&
        e.detail?.id === tab.id &&
        type === viewType
      ) {
        refresh({ forceReload: e.detail?.forceReload });
      }
    },
    [refresh, tab.id, type, viewType],
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
      },
    ) =>
      dialogs.confirmDirty(
        async () => canConfirm && (await check()),
        callback,
        options,
      ),
    [canConfirm],
  );
}

/**
 * This hook can be used to access the current view's meta data
 *
 */
export function useViewMeta() {
  const metaAtom = useMolecule(metaMolecule);
  const meta = useAtomValue(
    useMemo(() => selectAtom(metaAtom, (s) => s.meta), [metaAtom]),
  );

  const findField = useCallback((name: string) => meta.fields?.[name], [meta]);

  const findItem = useCallback(
    (fieldName: string) => findViewItem(meta, fieldName),
    [meta],
  );

  const findItems = useAtomCallback(
    useCallback(
      (get, set) => {
        const { items } = get(metaAtom);
        return (
          items ??
          (() => {
            const viewItems = findViewItems(meta).map((item) =>
              item.name
                ? processViewItem(item, meta.fields?.[item.name])
                : item,
            );
            set(metaAtom, (prev) => ({ ...prev, items: viewItems }));
            return viewItems;
          })()
        );
      },
      [metaAtom, meta],
    ),
  );

  return {
    meta,
    findField,
    findItem,
    findItems,
  } as const;
}
