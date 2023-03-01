import * as meta from "@/services/client/meta";
import { ActionView, MenuItem } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useCallback, useEffect, useState } from "react";

type Views = Record<string, ActionView>;

const viewsAtom = atom<Views>({});
const menusAtom = atom<MenuItem[]>([]);

export function useMenu() {
  const [views, setViews] = useAtom(viewsAtom);
  const [menus, setMenus] = useAtom(menusAtom);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await meta.menus("all");
      setMenus(res);
    } finally {
      setLoading(false);
    }
  }, []);

  const execute = useCallback(
    async (action: string) => {
      let view = views[action];
      if (view) {
        return view;
      }
      view = await meta
        .actionView(action)
        .then((x) => ({ ...x, name: action }));
      setViews((prev) => ({
        ...prev,
        [action]: view,
      }));

      return view;
    },
    [views, setViews]
  );

  useEffect(() => {
    if (loading || menus.length) return;
    load();
  }, []);

  return {
    menus,
    execute,
    loading,
  };
}
