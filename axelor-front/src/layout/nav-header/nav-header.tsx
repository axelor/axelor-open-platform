import { useSession } from "@/hooks/use-session";
import { useCallback, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import {
  Box,
  CommandBar,
  CommandItem,
  CommandItemProps,
  Input,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { ReactComponent as AppLogo } from "../../assets/axelor.svg";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { dialogs } from "@/components/dialogs";
import { useTabs } from "@/hooks/use-tabs";
import { useRoute } from "@/hooks/use-route";
import { useSidebar } from "../nav-drawer/hook";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { i18n } from "@/services/client/i18n";
import styles from "./nav-header.module.scss";

function FavoriteItem(props: CommandItemProps) {
  const { open: openTab, active } = useTabs();
  const { navigate } = useRoute();
  const { data: session } = useSession();
  const location = useLocation();

  // favorites state
  const [favorites, setFavorites] = useState<DataRecord[]>([]);

  const pathname = location?.pathname;
  const user = session?.user!;
  const tabTitle = active?.action?.title;

  const favoriteDataStore = useMemo<DataStore>(
    () =>
      new DataStore("com.axelor.meta.db.MetaMenu", {
        fields: ["id", "name", "title", "link"],
        filter: {
          _domain: "self.user = :__user__ and self.link is not null",
          _domainContext: {},
        },
      }),
    []
  );

  const handleFavoriteClick = useCallback(
    (data: DataRecord) => {
      data.link && navigate(data.link);
    },
    [navigate]
  );

  const handleFavoriteAdd = useCallback(async () => {
    const [type, id] = pathname.split("/").slice(-2);
    const tabId = type === "edit" && id ? id : "";
    let inputTitle = `${tabTitle}${tabId ? ` (${tabId})` : ""}`;

    const confirmed = await dialogs.confirm({
      title: i18n.get("Add to favorites..."),
      content: (
        <Box d="flex" w={100}>
          <Input
            type="text"
            autoFocus={true}
            defaultValue={inputTitle}
            onChange={(e) => {
              inputTitle = e.target.value;
            }}
          />
        </Box>
      ),
    });

    if (confirmed && inputTitle) {
      const result = await favoriteDataStore.save({
        title: inputTitle,
        link: pathname,
        name: pathname,
        user: { id: user.id },
        hidden: true,
      });
      if (result) {
        setFavorites((list) => [result, ...list]);
      }
    }
  }, [favoriteDataStore, tabTitle, pathname, user]);

  const handleOrganizeFavorites = useCallback(() => {
    openTab("menus.fav");
  }, [openTab]);

  useAsyncEffect(async () => {
    const { records } = await favoriteDataStore.search({
      offset: 0,
      limit: 40,
      sortBy: ["-priority"],
    });
    setFavorites(records);
  }, [favoriteDataStore]);

  return (
    <>
      <CommandItem
        {...props}
        items={[
          {
            key: "fav-add",
            text: "Add to favorites...",
            disabled: !tabTitle,
            onClick: handleFavoriteAdd,
          },
          { key: "fav-d1", divider: true, hidden: favorites.length === 0 },
          ...favorites.map((fav, ind) => ({
            key: String(fav.id ?? `fav_${ind}`),
            text: fav.title,
            onClick: () => handleFavoriteClick(fav),
          })),
          { key: "fav-d2", divider: true },
          {
            key: "fav-organise",
            text: i18n.get("Organize favorites..."),
            onClick: handleOrganizeFavorites,
          },
        ]}
      />
    </>
  );
}

function FarItems() {
  const { data, logout } = useSession();
  const { navigate } = useRoute();
  return (
    <>
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
              icon: "star",
            },
            render: FavoriteItem,
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
    </>
  );
}

export function NavHeader() {
  const { data } = useSession();
  const { sidebar, setSidebar } = useSidebar();
  const appHome = data?.app.home ?? "#/";
  const appLogo = data?.app.logo;
  return (
    <Box className={styles.header} borderBottom>
      <Box className={styles.menuToggle} onClick={(e) => setSidebar(!sidebar)}>
        <MaterialIcon icon="menu" />
      </Box>
      <Box className={styles.appLogo}>
        <a href={appHome}>
          {appLogo ? <img src={appLogo} alt="logo" /> : <AppLogo />}
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
