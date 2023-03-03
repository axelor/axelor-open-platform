import { useTabs } from "@/hooks/use-tabs";
import { Box, NavTabs as Tabs } from "@axelor/ui";
import { useCallback } from "react";

export function NavTabs() {
  const tabs = useTabs();
  const items = tabs.items.map((tab) => {
    return {
      id: tab.name,
      title: tab.title,
    };
  });

  const value = tabs.active?.name;

  const handleSelect = useCallback(
    (e: any, tab: any) => {
      const found = tabs.items.find((x) => x.name === tab.id);
      if (found) {
        tabs.open(found);
      }
    },
    [tabs]
  );

  return (
    <Box d="flex" flexDirection="column" overflow="hidden" flexGrow={1}>
      <Tabs items={items} value={value} onChange={handleSelect} />
    </Box>
  );
}
