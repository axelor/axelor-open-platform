import * as meta from "@/services/client/meta";
import { MenuItem } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useCallback, useEffect, useState } from "react";

const menusAtom = atom<MenuItem[]>([]);

export function useMenu() {
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

  useEffect(() => {
    if (loading || menus.length) return;
    load();
  }, []);

  return {
    menus,
    loading,
  };
}
