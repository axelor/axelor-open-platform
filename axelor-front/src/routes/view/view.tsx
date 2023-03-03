import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useMeta } from "@/hooks/use-meta";
import { useRoute } from "@/hooks/use-route";
import { useTabs } from "@/hooks/use-tabs";
import { ActionView } from "@/services/client/meta.types";
import { useEffect, useRef } from "react";
import { useParams } from "react-router-dom";

const getURL = (tab: ActionView) => `/ds/${tab.name}`;

/**
 * This component doesn't render anything but keeps
 * route path in sync with current view.
 *
 * It will also load the view if opened directly via a url.
 */
export function View() {
  const { redirect } = useRoute();

  const { action } = useParams();
  const { active, items, open } = useTabs();
  const { findActionView } = useMeta();

  const pathRef = useRef<string>();

  useAsyncEffect(async () => {
    if (action) {
      let tab = items.find((x) => x.name === action);
      if (tab === undefined) {
        tab = await findActionView(action);
      }
      if (tab) {
        pathRef.current = getURL(tab);
        open(tab);
      }
    }
  }, [action, items, open]);

  useEffect(() => {
    const tab = active;
    if (tab) {
      const path = getURL(tab);
      if (path && path !== pathRef.current) {
        pathRef.current = path;
        redirect(path);
      }
    }
  }, [redirect, active]);

  return null;
}
