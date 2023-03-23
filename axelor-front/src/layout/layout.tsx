import { Box } from "@axelor/ui";
import clsx from "clsx";
import { Outlet } from "react-router-dom";

import { DialogsProvider } from "@/components/dialogs";

import { NavDrawer } from "./nav-drawer";
import { useSidebar } from "./nav-drawer/hook";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";

import styles from "./layout.module.scss";
import { AlertsProvider } from "@/components/alerts";

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
        <Box
          d="flex"
          flexDirection="column"
          flexGrow={1}
          className={styles.tabs}
        >
          <NavTabs />
          <div className={styles.page}>
            <Outlet />
          </div>
        </Box>
      </Box>
      <DialogsProvider />
      <AlertsProvider />
    </Box>
  );
}
