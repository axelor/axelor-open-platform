import { atom, useAtomValue } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { useCallback } from "react";

import * as meta from "@/services/client/meta";
import { MenuItem } from "@/services/client/meta.types";
import { useAsyncEffect } from "../use-async-effect";

const menusAtom = atom<MenuItem[]>([]);
const loadingAtom = atom<boolean | null>(null);

export function useMenu() {
  const menus = useAtomValue(menusAtom);
  const loading = useAtomValue(loadingAtom);

  const fetchMenus = useAtomCallback(
    useCallback(async (get, set) => {
      const loading = get(loadingAtom);
      const menus = get(menusAtom);
      if (loading || menus.length) return;

      set(loadingAtom, true);
      try {
        const res = await meta.menus("all");
        set(menusAtom, res ?? []);
      } finally {
        set(loadingAtom, false);
      }
    }, [])
  );

  useAsyncEffect(async () => {
    await fetchMenus();
  }, [fetchMenus]);

  return {
    menus,
    loading: loading ?? true,
  };
}
