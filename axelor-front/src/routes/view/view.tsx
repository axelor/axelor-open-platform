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

function HandleTab() {
  const tabs = useTabs();
  const tabAtom = useMemo(
    () => tabs.active?.state ?? noTabAtom,
    [tabs.active?.state],
  );
  const tabState = useAtomValue(tabAtom);
  const params = useMemo(() => {
    const { action, mode, id } = tabState?.routes?.[tabState.type] ?? {};
    return { action, mode, id };
  }, [tabState]);

  const { redirect } = useRoute();
  const actionRef = useRef<string>();

  useEffect(() => {
    const { action, mode, id } = params;
    if (action && actionRef.current !== action) {
      const path = getURL(action, mode, id);
      if (path) {
        redirect(path);
      }
    }
  }, [redirect, params]);

  return null;
}

function HandlePath() {
  const tabs = useTabs();
  const params = useParams();
  const pathRef = useRef<string | null>(null);
  const homeAction = session.info?.user?.action;

  useEffect(() => {
    const { action = homeAction, mode, id } = params;
    const path = getURL(action, mode, id);
    if (pathRef.current !== path) {
      pathRef.current = path;
      if (action && path) {
        const found = tabs.items.find((x) => x.action.name === action);
        if (!found || found !== tabs.active) {
          queue.add(() =>
            tabs.open(action, {
              route: { mode, id },
              tab: true,
            }),
          );
        }
      }
    }
  }, [homeAction, params, tabs]);

  return null;
}

export function View() {
  return (
    <>
      <HandleTab />
      <HandlePath />
    </>
  );
}
