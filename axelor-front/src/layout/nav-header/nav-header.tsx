import { useSession } from "@/hooks/use-session";
import {
  Badge,
  Box,
  CommandBar,
  CommandItem,
  CommandItemProps,
  Input,
  MenuItem,
  RenderCommandItemProps,
  TextField,
} from "@axelor/ui";
import {
  MaterialIcon,
  MaterialIconProps,
} from "@axelor/ui/icons/material-icon";
import { useCallback, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";

import { dialogs } from "@/components/dialogs";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEditor } from "@/hooks/use-relation";
import { useRoute } from "@/hooks/use-route";
import { openTab_internal as openTab, useTabs } from "@/hooks/use-tabs";
import { useTagsMail, useTagsTasks } from "@/hooks/use-tags";
import { About } from "@/routes/about";
import { Shortcuts } from "@/routes/shortcuts";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import {
  QuickMenu,
  QuickMenuItem as TQuickMenuItem,
} from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import { commonClassNames } from "@/styles/common";
import { unaccent } from "@/utils/sanitize.ts";
import {
  ActionExecutor,
  DefaultActionExecutor,
  DefaultActionHandler,
} from "@/view-containers/action";
import Avatar from "@/views/form/widgets/mail-messages/avatar/avatar";

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

function FavoriteItem(props: RenderCommandItemProps) {
  const { open: openTab, active } = useTabs();
  const { navigate } = useRoute();
  const { data: session } = useSession();
  const location = useLocation();

  // favorites state
  const [loading, setLoading] = useState(false);
  const [favorites, setFavorites] = useState<DataRecord[]>([]);

  const pathname = location?.pathname;
  const user = session?.user;
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
    [],
  );

  const fetchData = useCallback(async () => {
    setLoading(true);
    const { records } = await favoriteDataStore.search({
      offset: 0,
      limit: 40,
      sortBy: ["-priority"],
    });
    setFavorites(records);
    setLoading(false);
  }, [favoriteDataStore]);

  const handleFavoriteClick = useCallback(
    (data: DataRecord) => {
      data.link && navigate(data.link);
    },
    [navigate],
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
      if (!user) {
        return;
      }
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

  return (
    <Box d="flex" onClick={fetchData}>
      <CommandItem
        {...props}
        items={[
          {
            key: "fav-add",
            text: i18n.get("Add to favorites..."),
            disabled: !tabTitle,
            onClick: handleFavoriteAdd,
          },
          {
            key: "fav-d1",
            divider: true,
            hidden: favorites.length === 0 && !loading,
          },
          ...(loading
            ? [
                {
                  key: "loading",
                  text: i18n.get("Loading..."),
                },
              ]
            : favorites.map((fav, ind) => ({
                key: String(fav.id ?? `fav_${ind}`),
                text: fav.title,
                onClick: () => handleFavoriteClick(fav),
              }))),
          { key: "fav-d2", divider: true },
          {
            key: "fav-organise",
            text: i18n.get("Organize favorites..."),
            onClick: handleOrganizeFavorites,
          },
        ]}
      />
    </Box>
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
  const [searchText, setSearchText] = useState<Record<number, string>>({});

  const refresh = useCallback(async () => {
    setMenus(await quick());
    setSearchText("");
  }, []);

  useAsyncEffect(async () => {
    await refresh();
  }, [refresh]);

  const actionExecutor = useMemo(
    () => new DefaultActionExecutor(new DefaultActionHandler()),
    [],
  );

  const handleFilterChange = useCallback(
    (index: number, e: React.ChangeEvent<HTMLInputElement>) => {
      setSearchText((prevItems) => ({
        ...prevItems,
        [index]: e.target.value,
      }));
    },
    [],
  );

  const handleFilterSearchKeyDown = useCallback(
    (index: number, e: React.KeyboardEvent<HTMLDivElement>) => {
      if (e.key === "Escape") {
        setSearchText((prevItems) => ({
          ...prevItems,
          [index]: "",
        }));
      }
    },
    [],
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
            render: (props: RenderCommandItemProps) => (
              <>
                {(menu?.items?.length ?? 0) >= 10 && index === 0 && (
                  <TextField
                    placeholder={i18n.get("Search...")}
                    onChange={(e) => handleFilterChange(ind, e)}
                    onKeyDown={(e) => handleFilterSearchKeyDown(ind, e)}
                    value={searchText?.[ind] ?? ""}
                    className={styles.searchFiltersInput}
                  />
                )}
                {unaccent(item.title)
                  .toLowerCase()
                  .includes(
                    unaccent(searchText?.[ind] ?? "").toLowerCase(),
                  ) && (
                  <QuickMenuItem
                    key={key}
                    data={item}
                    menu={menu}
                    actionExecutor={actionExecutor}
                    onClick={props.onClick}
                    onRefresh={refresh}
                  />
                )}
              </>
            ),
          };
        }),
      })),
    [
      menus,
      handleFilterChange,
      handleFilterSearchKeyDown,
      searchText,
      actionExecutor,
      refresh,
    ],
  );

  return (
    <CommandBar
      items={items}
      menuProps={{ contentClassName: styles.quickMenus }}
    />
  );
}

function FarItems() {
  const { data, logout } = useSession();
  const { unread: unreadMailCount } = useTagsMail();
  const { current: currentTaskCount, pending: pendingTaskCount } =
    useTagsTasks();

  const showEditor = useEditor();

  const showPreferences = useCallback(() => {
    showEditor({
      model: "com.axelor.auth.db.User",
      title: i18n.get("Preferences"),
      viewName: "user-preferences-form",
      canAttach: false,
      record: {
        id: session.info?.user?.id,
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
          key: "fav",
          text: i18n.get("Favorite"),
          iconOnly: true,
          iconProps: {
            icon: "star",
          },
          render: FavoriteItem,
        },
        {
          key: "messages",
          text: i18n.get("Messages"),
          icon: (props: any) => (
            <BadgeIcon
              {...props}
              icon="notifications"
              count={currentTaskCount + unreadMailCount}
            />
          ),
          iconOnly: true,
          iconProps: {
            icon: "notifications",
          },
          items: [
            {
              key: "mail.messages",
              text: "Messages",
              subtext: unreadMailCount
                ? i18n.get("{0} messages", unreadMailCount)
                : i18n.get("no messages"),
              onClick: () => {
                const tabId = "mail.inbox";
                openTab(tabId);
                if (unreadMailCount) {
                  const event = new CustomEvent("tab:refresh", {
                    detail: { id: tabId },
                  });
                  document.dispatchEvent(event);
                }
              },
            },
            { key: "m-div-1", divider: true },
            {
              key: "tasks.due",
              text: i18n.get("Tasks due"),
              subtext: pendingTaskCount
                ? i18n.get("{0} tasks", pendingTaskCount)
                : i18n.get("no tasks"),
              onClick: () => openTab("team.tasks.due"),
            },
            { key: "m-div-2", divider: true },
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
              user={
                {
                  id: data?.user?.id,
                  code: data?.user?.login,
                  [data?.user?.nameField ?? "name"]: data?.user?.name,
                } as any
              }
              image={data?.user?.image ?? ""}
            />
          ),
          iconProps: {
            icon: "person",
          },
          items: [
            {
              key: "profile",
              text: data?.user?.name,
              subtext: i18n.get("Preferences"),
              onClick: showPreferences,
            },
            {
              key: "d-person",
              divider: true,
            },
            ...(!data?.application.home
              ? []
              : [
                  {
                    key: "homepage",
                    text: i18n.get("Home page"),
                    onClick: () => openHomePage(data.application.home),
                  },
                ]),
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

function openHomePage(homePage: string | undefined) {
  if (homePage) {
    window.open(homePage, "_blank", "noopener,noreferrer");
  }
}

export function NavHeader() {
  return (
    <div className={styles.header}>
      <div className={styles.menus}>
        <div className={commonClassNames("hide-sm", styles.quickMenu)}>
          <QuickMenuBar />
        </div>
      </div>
      <div className={styles.farItems}>
        <FarItems />
      </div>
    </div>
  );
}
