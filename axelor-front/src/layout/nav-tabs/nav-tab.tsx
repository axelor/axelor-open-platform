import { Box } from "@axelor/ui";
import { Outlet } from "react-router-dom";

export function NavTabs() {
  return (
    <Box d="flex" flexDirection="column" overflow="hidden" flexGrow={1}>
      <Box m="auto">Tabs</Box>
      <Outlet />
    </Box>
  );
}
