import { useMediaQuery } from "@/hooks/use-media-query";
import { useResponsive } from "@/hooks/use-responsive";
import { NavMenuProps } from "@axelor/ui";
import { atom, useAtom } from "jotai";
import { useCallback } from "react";

type Show = NavMenuProps["show"];
type Mode = NavMenuProps["mode"];

const sidebarAtom = atom<{ small?: boolean; active?: boolean }>({});

export function useSidebar() {
  const small = useMediaQuery("(max-width: 768px)");
  const [state, setState] = useAtom(sidebarAtom);
  const size = useResponsive();

  const setSidebar = useCallback(
    (active: boolean) => {
      setState({ small, active });
    },
    [setState, small]
  );

  let sidebar = state.active ?? !small;
  if (small && state.small) {
    sidebar = state.active ?? false;
  } else if (state.small) {
    sidebar = true;
  } else if (small) {
    sidebar = false;
  }

  let mode: Mode = "accordion";
  let show: Show = sidebar ? "inline" : "icons";

  if (size.xs) {
    show = state.active && state.small ? "overlay" : "none";
  }
  if (size.sm || size.md) {
    show = state.active && state.small ? "overlay" : "icons";
  }

  return {
    mode,
    show,
    small,
    sidebar,
    setSidebar,
  };
}
