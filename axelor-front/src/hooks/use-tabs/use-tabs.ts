import { ActionView } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useCallback } from "react";

const tabAtom = atom<ActionView | null>(null);
const tabsAtom = atom<ActionView[]>([]);

export function useTabs() {
  const [active, setActive] = useAtom(tabAtom);
  const [tabs, setTabs] = useAtom(tabsAtom);

  const open = useCallback(
    (tab: ActionView) => {
      const found = tabs.find((x) => x.name === tab.name);
      if (found) {
        setActive(found);
      } else {
        setTabs([...tabs, tab]);
        setActive(tab);
      }
    },
    [tabs, setTabs, setActive]
  );

  const close = useCallback(
    (tab: ActionView) => {
      const index = tabs.findIndex((x) => x.name === tab.name);
      if (index > -1) {
        const prev = tabs[index - 1];
        setTabs(tabs.filter((x) => x.name !== tab.name));
        setActive(prev);
      }
    },
    [tabs, setTabs, setActive]
  );

  return {
    active,
    items: tabs,
    open,
    close,
  };
}
