import clsx from "clsx";
import { useCallback } from "react";
import { Outlet } from "react-router-dom";

import { Box } from "@axelor/ui";

import { AlertsProvider } from "@/components/alerts";
import { DialogsProvider } from "@/components/dialogs";
import { HttpWatch } from "@/components/http-watch";
import { Loader } from "@/components/loader/loader";
import { useAppTitle } from "@/hooks/use-app-title";
import { useMenu } from "@/hooks/use-menu";
import { useShortcut } from "@/hooks/use-shortcut";
import { PopupsProvider } from "@/view-containers/view-popup";

import { NavDrawer } from "./nav-drawer";
import { useSidebar } from "./nav-drawer/hook";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";
import { NavTags } from "./nav-tags";

// import global utils for external apps
import "../utils/globals";

import styles from "./layout.module.scss";

export function Layout() {
  const { sidebar, setSidebar } = useSidebar();
  const { loading } = useMenu();

  useAppTitle();

  useShortcut({
    key: "F9",
    action: useCallback(() => setSidebar(!sidebar), [setSidebar, sidebar]),
  });

  if (loading) {
    return (
      <Box d="flex" flexDirection="column" vh={100}>
        <Loader />
      </Box>
    );
  }

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
      <NavTags />
      <PopupsProvider />
      <DialogsProvider />
      <AlertsProvider />
      <HttpWatch />
    </Box>
  );
}
