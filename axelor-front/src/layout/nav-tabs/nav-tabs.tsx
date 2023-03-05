import { useTabs } from "@/hooks/use-tabs";
import { Box, NavTabs as Tabs } from "@axelor/ui";
import { useCallback } from "react";

export function NavTabs() {
  const { active, items, open } = useTabs();
  const value = active?.id;

  const handleSelect = useCallback((e: any, tab: any) => open(tab.id), [open]);

  return (
    <Box d="flex" flexDirection="column" overflow="hidden" flexGrow={1}>
      <Tabs items={items} value={value} onChange={handleSelect} />
    </Box>
  );
}
