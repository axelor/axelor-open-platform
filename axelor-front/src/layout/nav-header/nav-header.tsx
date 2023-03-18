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
          key: "home",
          text: "Home",
          iconOnly: true,
          iconProps: {
            icon: "home",
          },
          onClick: () => navigate("/"),
        },
        {
          key: "fav",
          text: "Favorite",
          iconOnly: true,
          iconProps: {
            icon: "bookmark",
          },
          items: [
            {
              key: "fav-add",
              text: "Add favorite",
            },
            { key: "fav-d1", divider: true },
            { key: "fav-d2", divider: true },
            {
              key: "fav-organise",
              text: "Organise favorites...",
            },
          ],
        },
        {
          key: "mail",
          text: "Mails",
          iconOnly: true,
          iconProps: {
            icon: "mail",
          },
        },
        {
          key: "messages",
          text: "Messages",
          iconOnly: true,
          iconProps: {
            icon: "notifications",
          },
        },
        {
          key: "user",
          text: "User",
          iconOnly: true,
          iconProps: {
            icon: "person",
          },
          items: [
            {
              key: "profile",
              text: data?.user.name,
              subtext: "Preferences",
              onClick: () => navigate("/profile"),
            },
            {
              key: "d-person",
              divider: true,
            },
            {
              key: "shortcuts",
              text: "Shortcuts",
            },
            {
              key: "about",
              text: "About",
              onClick: () => navigate("/about"),
            },
            {
              key: "logout",
              text: "Logout",
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
