import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useRoute } from "@/hooks/use-route";
import { useSession } from "@/hooks/use-session";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { useEffect } from "react";
import { useParams } from "react-router-dom";

const getURL = (tab: Tab | null) => {
  const id = tab?.id;
  if (id && !id.startsWith("$")) {
    return `/ds/${id}`;
  }
};

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
  const { info } = useSession();

  useAsyncEffect(async () => {
    const name = action ?? info?.user?.action ?? active?.id;
    if (name) {
      const tab = await open(name);
      const path = getURL(tab);
      if (path && !action) {
        // if coming from other places
        redirect(path);
      }
    }
  }, [action, active, items, open]);

  useEffect(() => {
    const tab = active;
    const path = getURL(tab);
    if (path) {
      redirect(path);
    }
  }, [redirect, active]);

  return null;
}
