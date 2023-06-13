import { useMediaQuery } from "@/hooks/use-media-query";
import { atom, useAtom } from "jotai";
import { useCallback } from "react";

const sidebarAtom = atom<{ small?: boolean; active?: boolean }>({});

export function useSidebar() {
  const small = useMediaQuery("(max-width: 768px)");
  const [state, setState] = useAtom(sidebarAtom);

  const setSidebar = useCallback(
    (active: boolean) => {
      setState({ small, active });
    },
    [setState, small]
  );

  let sidebar = state.active ?? !small;
  if (small && state.small) {
    sidebar = state?.active ?? false;
  } else if (state.small) {
    sidebar = true;
  } else if (small) {
    sidebar = false;
  }

  return {
    small,
    sidebar,
    setSidebar,
  };
}
