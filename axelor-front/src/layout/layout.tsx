import { Box } from "@axelor/ui";
import { Outlet } from "react-router-dom";
import { NavDrawer } from "./nav-drawer";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";

import clsx from "clsx";
import styles from "./layout.module.scss";
import { useSidebar } from "./nav-drawer/hook";

export function Layout() {
  const { sidebar } = useSidebar();
  return (
    <Box d="flex" flexDirection="column" vh={100}>
      <NavHeader />
      <Box
        d="flex"
        flexGrow={1}
        className={clsx(styles.content, { [styles.showSidebar]: sidebar })}
      >
        <Box
          as="nav"
          flex="0 0 auto"
          borderEnd
          className={clsx(styles.sidebar)}
        >
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
