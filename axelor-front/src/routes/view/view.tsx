import { atom, useAtomValue, useSetAtom } from "jotai";
import { useEffect, useRef } from "react";
import { generatePath, useParams } from "react-router-dom";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useRoute } from "@/hooks/use-route";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { session } from "@/services/client/session";

const getURL = (
  action: string | null = null,
  mode: string | null = null,
  id: string | null = null
) => {
  if (!action || action.startsWith("$")) {
    return null;
  }
  return generatePath("/ds/:action/:mode?/:id?", {
    action,
    mode,
    id,
  });
};

const activeTabAtom = atom<Tab | null>(null);
const activeTabStateAtom = atom((get) => {
  const tab = get(activeTabAtom);
  const tabState = tab ? get(tab.state) : null;
  return tabState;
});

export function View() {
  const { redirect } = useRoute();

  const { action: viewAction, mode: viewMode, id: viewId } = useParams();
  const { active, open } = useTabs();

  const setActiveTabAtom = useSetAtom(activeTabAtom);
  const viewState = useAtomValue(activeTabStateAtom);

  const pathRef = useRef<string | null>(null);

  useEffect(() => {
    setActiveTabAtom(active ?? null);
  }, [active, setActiveTabAtom]);

  useAsyncEffect(async () => {
    if (viewState) {
      const { type } = viewState;
      const { action, mode, id } = viewState?.routes?.[type] ?? {};
      const path = getURL(action, mode, id);
      if (path && path !== pathRef.current) {
        pathRef.current = path;
        redirect(path);
      }
    }
  }, [viewState, redirect]);

  useAsyncEffect(async () => {
    if (viewAction) {
      const path = getURL(viewAction, viewMode, viewId);
      pathRef.current = path;
      await open(viewAction, {
        mode: viewMode,
        id: viewId,
      });
    } else {
      const homeAction = session.info?.user.action;
      if (homeAction) {
        await open(homeAction);
      }
    }
  }, [open, viewAction, viewMode, viewId]);

  return null;
}
