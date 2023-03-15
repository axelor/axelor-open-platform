import { atom, useAtom } from "jotai";

const sidebarAtom = atom<boolean>(true);

export function useSidebar() {
  const [sidebar, setSidebar] = useAtom(sidebarAtom);
  return {
    sidebar,
    setSidebar,
  };
}
