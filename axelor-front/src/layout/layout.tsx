import { Outlet } from "react-router-dom";

import { Box } from "@axelor/ui";

import { AlertsProvider } from "@/components/alerts";
import { DialogsProvider } from "@/components/dialogs";
import { HttpWatch } from "@/components/http-watch";
import { Loader } from "@/components/loader/loader";
import { useAppTitle } from "@/hooks/use-app-title";
import { useMenu } from "@/hooks/use-menu";
import { PopupsProvider } from "@/view-containers/view-popup";

import { NavDrawer } from "./nav-drawer";
import { NavHeader } from "./nav-header";
import { NavTabs } from "./nav-tabs";
import { NavTags } from "./nav-tags";

// import global utils for external apps
import "../utils/globals";

import styles from "./layout.module.scss";

export function Layout() {
  const { loading } = useMenu();

  useAppTitle();

  if (loading) {
    return (
      <Box d="flex" flexDirection="column" vh={100}>
        <Loader />
      </Box>
    );
  }

  return (
    <div className={styles.container}>
      <NavHeader />
      <div className={styles.content}>
        <div className={styles.sidebar}>
          <NavDrawer />
        </div>
        <div className={styles.tabs}>
          <NavTabs />
          <div className={styles.page}>
            <Outlet />
          </div>
        </div>
      </div>
      <NavTags />
      <PopupsProvider />
      <DialogsProvider />
      <AlertsProvider />
      <HttpWatch />
    </div>
  );
}
