import { useTabs } from "@/hooks/use-tabs";
import { Views } from "@/view-containers/views";
import { Box, NavTabs as Tabs } from "@axelor/ui";
import clsx from "clsx";
import { useCallback } from "react";

import styles from "./nav-tabs.module.css";

export function NavTabs() {
  const { active, items, open } = useTabs();
  const value = active?.id;

  const handleSelect = useCallback((e: any, tab: any) => open(tab.id), [open]);

  return (
    <Box d="flex" flexDirection="column" overflow="hidden" flexGrow={1}>
      <Tabs items={items} value={value} onChange={handleSelect} />
      {items.map((tab) => (
        <Views
          key={tab.id}
          tab={tab}
          className={clsx(styles.tabContent, {
            [styles.active]: tab.id === value,
          })}
        />
      ))}
    </Box>
  );
}
