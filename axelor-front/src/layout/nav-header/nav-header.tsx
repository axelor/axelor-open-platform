import { useSession } from "@/hooks/use-session";
import { navigate } from "@/routes";
import { Box, CommandBar } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { ReactComponent as AppLogo } from "../../assets/axelor.svg";
import { useSidebar } from "../nav-drawer/hook";
import styles from "./nav-header.module.scss";

function FarItems() {
  const { data, logout } = useSession();
  return (
    <CommandBar
      items={[
        {
          id: "home",
          title: "Home",
          iconOnly: true,
          iconProps: {
            icon: "home",
          },
          onClick: () => navigate("/"),
        },
        {
          id: "fav",
          title: "Favorite",
          iconOnly: true,
          iconProps: {
            icon: "bookmark",
          },
          items: [
            {
              id: "fav-add",
              title: "Add favorite",
            },
            { id: "fav-d1", divider: true },
            { id: "fav-d2", divider: true },
            {
              id: "fav-organise",
              title: "Organise favorites...",
            },
          ],
        },
        {
          id: "mail",
          title: "Mails",
          iconOnly: true,
          iconProps: {
            icon: "mail",
          },
        },
        {
          id: "messages",
          title: "Messages",
          iconOnly: true,
          iconProps: {
            icon: "notifications",
          },
        },
        {
          id: "user",
          title: "User",
          iconOnly: true,
          iconProps: {
            icon: "person",
          },
          items: [
            {
              id: "profile",
              title: data?.user.name,
              subtitle: "Preferences",
              onClick: () => navigate("/profile"),
            },
            {
              id: "d-person",
              divider: true,
            },
            {
              id: "shortcuts",
              title: "Shortcuts",
            },
            {
              id: "about",
              title: "About",
              onClick: () => navigate("/about"),
            },
            {
              id: "logout",
              title: "Logout",
              onClick: () => logout(),
            },
          ],
        },
      ]}
    />
  );
}

export function NavHeader() {
  const { data } = useSession();
  const { sidebar, setSidebar } = useSidebar();
  const appHome = data?.app.home ?? "#/";
  return (
    <Box className={styles.header} borderBottom>
      <Box className={styles.menuToggle} onClick={(e) => setSidebar(!sidebar)}>
        <MaterialIcon icon="menu" />
      </Box>
      <Box className={styles.appLogo}>
        <a href={appHome}>
          <AppLogo />
        </a>
      </Box>
      <Box className={styles.topMenu}></Box>
      <Box className={styles.quickMenu}></Box>
      <Box className={styles.farItems}>
        <FarItems />
      </Box>
    </Box>
  );
}
