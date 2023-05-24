import { atom, useAtomValue } from "jotai";
import { useMemo } from "react";

import { WidgetAtom } from "../../builder";

export function useReadonly(widgetAtom: WidgetAtom, checkRoot = false) {
  const readonlyAtom = useMemo(() => {
    return atom((get) => {
      let state = get(widgetAtom);
      let attrs = state.attrs;
      let parent = state.parent;
      if (attrs.readonly) return true;
      while (parent) {
        state = get(parent);
        attrs = state.attrs;
        parent = state.parent;
        if (checkRoot && !parent) return !!attrs.readonly;
        if (parent && attrs.readonly) return true;
      }
      return false;
    });
  }, [checkRoot, widgetAtom]);

  return useAtomValue(readonlyAtom);
}
