import * as meta from "@/services/client/meta";
import { MenuItem } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useState } from "react";
import { useAsyncEffect } from "../use-async-effect";

const menusAtom = atom<MenuItem[]>([]);

export function useMenu() {
  const [menus, setMenus] = useAtom(menusAtom);
  const [loading, setLoading] = useState(false);

  useAsyncEffect(async () => {
    if (loading || menus.length) return;
    setLoading(true);
    try {
      const res = await meta.menus("all");
      setMenus(res ?? []);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    menus,
    loading,
  };
}
