import { atom, useAtomValue } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { useCallback } from "react";

import * as meta from "@/services/client/meta";
import { MenuItem } from "@/services/client/meta.types";
import { useAsyncEffect } from "../use-async-effect";

const menusAtom = atom<MenuItem[] | null>(null);
const loadingAtom = atom<boolean | null>(null);
const errorAtom = atom<boolean>(false);

// a empty array to be used as const reference
const emptyMenus: MenuItem[] = [];

export function useMenu() {
  const menus = useAtomValue(menusAtom);
  const loading = useAtomValue(loadingAtom);

  const fetchMenus = useAtomCallback(
    useCallback(async (get, set) => {
      const loading = get(loadingAtom);
      const menus = get(menusAtom);
      const error = get(errorAtom);
      if (loading || error || menus) return;

      set(loadingAtom, true);
      try {
        const res = await meta.menus("all");
        set(menusAtom, res ?? []);
      } catch {
        set(errorAtom, true);
      } finally {
        set(loadingAtom, false);
      }
    }, []),
  );

  useAsyncEffect(async () => {
    await fetchMenus();
  }, [fetchMenus]);

  return {
    menus: menus ?? emptyMenus,
    loading: loading ?? true,
  };
}
