import * as meta from "@/services/client/meta";
import { ActionView } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useCallback } from "react";

const actionViewsAtom = atom<Record<string, ActionView>>({});

export function useMeta() {
  const [actionViews, setActionViews] = useAtom(actionViewsAtom);

  const findActionView = useCallback(
    async (name: string) => {
      let view = actionViews[name];
      if (view) {
        return view;
      }
      view = await meta.actionView(name);
      view = { ...view, name };
      setActionViews((prev) => ({ ...prev, [name]: view }));
      return view;
    },
    [actionViews, setActionViews]
  );

  return {
    findActionView,
  };
}
