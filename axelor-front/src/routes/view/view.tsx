import { atom, useAtomValue } from "jotai";
import { useEffect, useMemo, useRef } from "react";
import { generatePath, useParams } from "react-router-dom";

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
    const { action, mode, id } = tabState?.routes?.[tabState.type] ?? {};
    return { action, mode, id };
  }, [tabState]);

  const { redirect } = useRoute();

  const pathRef = useRef<string | null>(null);
  const tabPathRef = useRef<string | null>(null);
  const actionRef = useRef<string>();

  const homeAction = session.info?.user?.action;

  useEffect(() => {
    const { action: tabAction, mode: tabMode, id: tabId } = tabParams;
    const { action = homeAction, mode, id } = params;
    const tabPath = getURL(tabAction, tabMode, tabId);
    const path = getURL(action, mode, id);

    if (action && path && pathRef.current !== path) {
      pathRef.current = path;
      queue.add(() =>
        tabs.open(action, {
          route: { mode, id },
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
      redirect(tabPath);
    }
  }, [homeAction, params, redirect, tabParams, tabs]);

  return null;
}
