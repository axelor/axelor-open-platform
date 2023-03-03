import { Box } from "@axelor/ui";
import { Outlet } from "react-router-dom";
import { NavDrawer } from "./nav-drawer";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";

import styles from "./layout.module.scss";

export function Layout() {
  return (
    <Box d="flex" flexDirection="column" vh={100}>
      <Box borderBottom p={3}>
        <NavHeader />
      </Box>
      <Box d="flex" flexGrow={1} className={styles.content}>
        <Box as="nav" flex="0 0 auto" borderEnd className={styles.sidebar}>
          <NavDrawer />
        </Box>
        <Box d="flex" flexDirection="column" overflow="hidden" flexGrow={1}>
          <NavTabs />
          <div className={styles.page}>
            <Outlet />
          </div>
        </Box>
      </Box>
    </Box>
  );
}
