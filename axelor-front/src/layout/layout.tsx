import { Box } from "@axelor/ui";
import { clsx } from "clsx";
import { Outlet, useLocation } from "react-router-dom";
import { NavDrawer } from "./nav-drawer";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";

import styles from "./layout.module.scss";

export function Layout() {
  const location = useLocation();

  const pathname = location.pathname;

  const showPage = pathname !== "/" && !pathname.startsWith("/ds");
  const showTabs = !showPage;

  const clsShowTabs = clsx({ [styles.hide]: !showTabs });
  const clsShowPage = clsx({ [styles.hide]: !showPage });
  const clsShowSide = clsx({ [styles.hide]: showPage });

  const contentClassName = clsx(clsShowSide, styles.content);
  const sidebarClassName = clsx(styles.sidebar);
  return (
    <Box d="flex" flexDirection="column" vh={100}>
      <Box borderBottom p={3}>
        <NavHeader />
      </Box>
      <Box d="flex" flexGrow={1} className={contentClassName}>
        <Box as="nav" flex="0 0 auto" borderEnd className={sidebarClassName}>
          <NavDrawer />
        </Box>
        <Box
          d="flex"
          flexDirection="column"
          overflow="hidden"
          flexGrow={1}
          className={clsShowTabs}
        >
          <NavTabs />
        </Box>
      </Box>
      <Box d="flex" flexGrow={1} className={clsShowPage}>
        <Outlet />
      </Box>
    </Box>
  );
}
