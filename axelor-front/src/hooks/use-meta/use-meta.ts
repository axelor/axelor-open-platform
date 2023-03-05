import * as meta from "@/services/client/meta";
import { ActionView, View } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useCallback } from "react";

const actionViewsAtom = atom<Record<string, ActionView>>({});
const viewsAtom = atom<Record<string, View>>({});

export function useMeta() {
  const [actionViews, setActionViews] = useAtom(actionViewsAtom);
  const [views, setViews] = useAtom(viewsAtom);

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

  const findView = (opts: {
    type: string;
    name?: string;
    model?: string;
  }) => {};

  return {
    findActionView,
  };
}
