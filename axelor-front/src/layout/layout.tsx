import { Box } from "@axelor/ui";
import { clsx } from "clsx";
import { Outlet, useLocation } from "react-router-dom";
import { NavDrawer } from "./nav-drawer";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";

import styles from "./layout.module.scss";

export function Layout() {
  const location = useLocation();

  const showTabs = location.pathname?.startsWith("/ds");
  const showPage = location.pathname !== "/" && !showTabs;

  const clsShowTabs = clsx({ [styles.hide]: !showTabs });
  const clsShowPage = clsx({ [styles.hide]: !showPage });
  const clsShowSide = clsx({ [styles.hide]: showPage });

  return (
    <Box d="flex" flexDirection="column" vh={100 as any}>
      <Box borderBottom p={3}>
        <NavHeader />
      </Box>
      <Box d="flex" flexGrow={1} className={clsShowSide}>
        <Box as="nav" flex="0 0 auto" borderEnd p={5}>
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
