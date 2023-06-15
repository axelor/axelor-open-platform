import { useSession } from "@/hooks/use-session";
import {
  Badge,
  Box,
  CommandBar,
  CommandItem,
  CommandItemProps,
  Input,
  MenuItem,
} from "@axelor/ui";
import {
  MaterialIcon,
  MaterialIconProps,
} from "@axelor/ui/icons/meterial-icon";
import { useCallback, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";

import { dialogs } from "@/components/dialogs";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEditor } from "@/hooks/use-relation";
import { useRoute } from "@/hooks/use-route";
import { useTabs } from "@/hooks/use-tabs";
import { useTagsMail, useTagsTasks } from "@/hooks/use-tags";
import { About } from "@/routes/about";
import { Shortcuts } from "@/routes/shortcuts";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import Avatar from "@/views/form/widgets/mail-messages/avatar/avatar";
import {
  QuickMenu,
  QuickMenuItem as TQuickMenuItem,
} from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import { commonClassNames } from "@/styles/common";
import {
  ActionExecutor,
  DefaultActionExecutor,
  DefaultActionHandler,
} from "@/view-containers/action";
import { ReactComponent as AppLogo } from "../../assets/axelor.svg";
import { useSidebar } from "../nav-drawer/hook";
import { quick } from "./utils";
import styles from "./nav-header.module.scss";

function BadgeIcon({
  count,
  ...props
}: MaterialIconProps & {
  count?: number;
}) {
  return (
    <Box as="span" d="flex" position="relative">
      <MaterialIcon {...props} />
      {count ? (
        <Badge bg="danger" className={styles.badge}>
          {count}
        </Badge>
      ) : null}
    </Box>
  );
}

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
  );
}

function QuickMenuItem({
  menu,
  data,
  actionExecutor,
  onClick,
  onRefresh,
}: CommandItemProps & {
  menu: QuickMenu;
  data: TQuickMenuItem;
  actionExecutor: ActionExecutor;
  onClick?: (e: any) => void;
  onRefresh?: () => void;
}) {
  async function handleClick(e: any) {
    onClick?.(e);
    if (data.action) {
      await actionExecutor.execute(data.action, {
        context: {
          ...data.context,
          _model: data.model ?? "com.axelor.meta.db.MetaAction",
        },
      });
      await onRefresh?.();
    }
  }

  return (
    <MenuItem onClick={handleClick}>
      {menu.showingSelected && (
        <Input
          me={2}
          type="radio"
          checked={data.selected}
          onChange={() => {}}
        />
      )}
      {data.title}
    </MenuItem>
  );
}

function QuickMenuBar() {
  const [menus, setMenus] = useState<QuickMenu[]>([]);
  const refresh = useCallback(async () => {
    setMenus(await quick());
  }, []);

  useAsyncEffect(async () => {
    await refresh();
  }, [refresh]);

  const actionExecutor = useMemo(
    () => new DefaultActionExecutor(new DefaultActionHandler()),
    []
  );

  const items = useMemo(
    () =>
      menus.map((menu, ind) => ({
        key: `quick_menu_${ind}`,
        text: menu.title,
        showDownArrow: true,
        items: menu?.items?.map((item, index) => {
          const key = String(index);
          return {
            key,
            text: item.title,
            render: (props: CommandItemProps) => (
              <QuickMenuItem
                key={key}
                data={item}
                menu={menu}
                actionExecutor={actionExecutor}
                onClick={props.onClick}
                onRefresh={refresh}
              />
            ),
          };
        }),
      })),
    [menus, actionExecutor, refresh]
  );

  return <CommandBar items={items} />;
}

function FarItems() {
  const { data, logout } = useSession();
  const { open: openTab } = useTabs();
  const { unread: unreadMailCount } = useTagsMail();
  const { current: currentTaskCount, pending: pendingTaskCount } =
    useTagsTasks();

  const showEditor = useEditor();

  const showPreferences = useCallback(() => {
    showEditor({
      model: "com.axelor.auth.db.User",
      title: i18n.get("Preferences"),
      viewName: "user-preferences-form",
      record: {
        id: session.info?.user.id,
      },
      onSelect() {
        window.location.reload();
      },
    });
  }, [showEditor]);

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
          onClick: () => {
            if (session.info?.user?.action) {
              openTab(session.info.user.action);
            }
          },
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
          icon: (props: any) => (
            <BadgeIcon {...props} icon="mail" count={unreadMailCount} />
          ),
          iconOnly: true,
          iconProps: {
            icon: "mail",
          },
          onClick: () => openTab("mail.inbox"),
        },
        {
          key: "messages",
          text: "Messages",
          icon: (props: any) => (
            <BadgeIcon
              {...props}
              icon="notifications"
              count={currentTaskCount}
            />
          ),
          iconOnly: true,
          iconProps: {
            icon: "notifications",
          },
          items: [
            {
              key: "tasks.due",
              text: i18n.get("Tasks due"),
              subtext: pendingTaskCount
                ? i18n.get("{0} tasks", pendingTaskCount)
                : i18n.get("no tasks"),
              onClick: () => openTab("team.tasks.due"),
            },
            { key: "m-div", divider: true },
            {
              key: "tasks.todo",
              text: i18n.get("Tasks todo"),
              subtext: currentTaskCount
                ? i18n.get("{0} tasks", currentTaskCount)
                : i18n.get("no tasks"),
              onClick: () => openTab("team.tasks.todo"),
            },
          ],
        },
        {
          key: "user",
          text: i18n.get("User"),
          className: styles.user,
          iconOnly: true,
          showDownArrow: true,
          icon: () => (
            <Avatar
              user={data?.user as any}
              image={data?.user?.image ?? ""}
              color={" "}
            />
          ),
          iconProps: {
            icon: "person",
          },
          items: [
            {
              key: "profile",
              text: data?.user.name,
              subtext: i18n.get("Preferences"),
              onClick: showPreferences,
            },
            {
              key: "d-person",
              divider: true,
            },
            {
              key: "shortcuts",
              text: i18n.get("Shortcuts"),
              onClick: showShortcuts,
            },
            {
              key: "about",
              text: i18n.get("About"),
              onClick: showAbout,
            },
            {
              key: "logout",
              text: i18n.get("Log out"),
              onClick: () => logout(),
            },
          ],
        },
      ]}
    />
  );
}

function showAbout() {
  dialogs.info({
    title: i18n.get("About"),
    content: <About />,
  });
}

function showShortcuts() {
  dialogs.info({
    size: "md",
    title: i18n.get("Keyboard Shortcuts"),
    content: <Shortcuts />,
  });
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
      <Box className={styles.menus}>
        <Box className={commonClassNames("hide-sm", styles.topMenu)}></Box>
        <Box className={commonClassNames("hide-sm", styles.quickMenu)}>
          <QuickMenuBar />
        </Box>
      </Box>
      <Box className={styles.farItems}>
        <FarItems />
      </Box>
    </Box>
  );
}
