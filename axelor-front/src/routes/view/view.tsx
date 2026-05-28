import { atom, useAtomValue } from "jotai";
import { useEffect, useCallback, useMemo, useRef } from "react";
import { generatePath, useParams, useSearchParams } from "react-router";
import { useAtomCallback } from "jotai/utils";
import { produce } from "immer";

import { useRoute } from "@/hooks/use-route";
import { TabAtom, useTabs } from "@/hooks/use-tabs";
import { session } from "@/services/client/session";
import { TaskQueue } from "@/view-containers/action/queue";

const getURL = (
  action: string | null = null,
  mode: string | null = null,
  id: string | null = null,
) => {
  if (!action || action.startsWith("$")) return null;

  if (mode === "list") id = id || "1";

  if (!mode) return generatePath("/ds/:action", { action });
  if (!id) return generatePath("/ds/:action/:mode", { action, mode });

  return generatePath("/ds/:action/:mode/:id", { action, mode, id });
};

const noTabAtom: TabAtom = atom({}) as unknown as TabAtom;

const queue = new TaskQueue();

export function View() {
  const params = useParams();
  const tabs = useTabs();
  const tabAtom = useMemo(
    () => tabs.active?.state ?? noTabAtom,
    [tabs.active?.state],
  );
  const tabState = useAtomValue(tabAtom);
  const tabParams = useMemo(() => {
    const { action, mode, id, qs } = tabState?.routes?.[tabState.type] ?? {};
    return { action, mode, id, qs };
  }, [tabState]);

  const { redirect, location } = useRoute();
  const [searchParams] = useSearchParams();

  const pathRef = useRef<string>(null);
  const tabPathRef = useRef<string>(null);
  const actionRef = useRef<string>('');
  const qsRef = useRef<Record<string, Record<string, string>>>({});
  const homeAction = session.info?.user?.action;

  const updateQueryParamsInTabRoute = useAtomCallback(
    useCallback(
      (get, set, queryParams: Record<string, string>) =>
        set(
          tabAtom,
          produce(get(tabAtom), (state) => {
            if (state.routes?.[state.type]) {
              state.routes![state.type]!.qs = queryParams;
            }
          }),
        ),
      [tabAtom],
    ),
  );

  useEffect(() => {
    const values: Record<string, string> = {};
    for (const [key, value] of searchParams) {
      values[key] = value;
    }
    qsRef.current[location.pathname] = values;
    // sync query params with tab route
    // so when tab is in-active, switching back will open tab
    // with last searched query parameters
    updateQueryParamsInTabRoute(values);
  }, [location, searchParams, updateQueryParamsInTabRoute]);

  useEffect(() => {
    const {
      action: tabAction,
      mode: tabMode,
      id: tabId,
      qs: tabQueryString,
    } = tabParams;
    const { action = homeAction, mode, id } = params;
    const tabPath = getURL(tabAction, tabMode, tabId);
    const path = getURL(action, mode, id);

    if (action && path && pathRef.current !== path) {
      pathRef.current = path;
      queue.add(() =>
        tabs.open(action, {
          route: { mode, id, qs: qsRef.current[path] },
          tab: true,
        }),
      );
    } else if (
      tabPath &&
      tabAction &&
      actionRef.current !== tabAction &&
      tabPathRef.current !== tabPath
    ) {
      tabPathRef.current = tabPath;
      redirect(tabPath, {}, tabQueryString);
    } else if (tabs.items.length === 0) {
      pathRef.current = null;
      tabPathRef.current = null;
    }
  }, [homeAction, params, redirect, tabParams, tabs]);

  return null;
}
