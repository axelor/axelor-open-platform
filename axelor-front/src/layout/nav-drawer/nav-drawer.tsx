import { useCallback, useMemo, useState } from "react";

import { Badge, NavMenu, NavMenuItem, TBackground } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { useMenu } from "@/hooks/use-menu";
import { useSession } from "@/hooks/use-session";
import { useShortcut } from "@/hooks/use-shortcut";
import { useTabs } from "@/hooks/use-tabs";
import { useTagsList } from "@/hooks/use-tags";
import { i18n } from "@/services/client/i18n";
import { MenuItem, Tag } from "@/services/client/meta.types";
import { unaccent } from "@/utils/sanitize";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useSidebar } from "./hook";

import { ReactComponent as AppIcon } from "../../assets/axelor-icon.svg";
import { ReactComponent as AppLogo } from "../../assets/axelor.svg";


import styles from "./nav-drawer.module.scss";

function MenuTag({
  item,
  tag,
  color,
}: {
  item: MenuItem;
  tag: string;
  color?: string;
}) {
  const bg = (
    !color || color === "default" ? "secondary" : color
  ) as TBackground;
  return (
    <Badge data-tag-name={item.name} bg={bg}>
      {`${tag}`.toUpperCase()}
    </Badge>
  );
}

function MenuIcon({ icon, color }: { icon: string; color?: string }) {
  return <Icon icon={icon} />;
}

function load(res: MenuItem[], tags: Tag[]) {
  const menus = res.filter((item) => item.left !== false);
  const toNavItemProps = (item: MenuItem): NavMenuItem => {
    const {
      name,
      title,
      action,
      tag,
      tagStyle: tagColor,
      icon,
      iconBackground: iconColor,
    } = item;
    const items = action
      ? undefined
      : menus.filter((x) => x.parent === name).map(toNavItemProps);

    const props: NavMenuItem = {
      id: name,
      title,
      items,
      tagColor,
      iconColor,
    };

    if (icon) {
      props.icon = () => <MenuIcon icon={icon} color={iconColor} />;
    }

    const updatedTag = tags.filter((x) => item.name === x.name)[0];
    if (tag || updatedTag) {
      props.tag = () => (
        <MenuTag item={item} tag={updatedTag?.value ?? tag} color={tagColor} />
      );
    }

    return props;
  };
  return menus.filter((item) => !item.parent).map(toNavItemProps);
}

export function NavDrawer() {
  const { open: openTab } = useTabs();
  const { loading, menus } = useMenu();
  const { mode, show, small, sidebar, setSidebar } = useSidebar();
  const [showSearch, setShowSearch] = useState(false);

  useShortcut({
    key: "F9",
    action: useCallback(() => setSidebar(!sidebar), [setSidebar, sidebar]),
  });

  useShortcut({
    key: "M",
    ctrlKey: true,
    action: useCallback(() => setShowSearch(!showSearch), [showSearch]),
  });

  const handleClick = useCallback(
    async (item: NavMenuItem) => {
      const menu = menus.find((x) => x.name === item.id);
      if (menu?.action) {
        if (small) setSidebar(false);
        await openTab(menu.action, { tab: true });
      }
    },
    [menus, small, setSidebar, openTab],
  );

  const handleSearchShow = useCallback(() => setShowSearch(true), []);
  const handleSearchHide = useCallback(() => setShowSearch(false), []);
  const handleSearchFilter = useCallback((item: NavMenuItem, text: string) => {
    const title = unaccent(item.title.toLocaleLowerCase());
    const search = unaccent(text.toLocaleLowerCase());
    const parts = search.split(/\s+/);
    return title.includes(search) || parts.every((p) => title.includes(p));
  }, []);

  const tags = useTagsList();

  const items = useMemo(() => load(menus, tags), [menus, tags]);

  if (loading) return null;

  return (
    <NavMenu
      mode={mode}
      show={show}
      items={items}
      onItemClick={handleClick}
      searchActive={showSearch}
      searchOptions={{
        title: i18n.get("Search menu..."),
        onShow: handleSearchShow,
        onHide: handleSearchHide,
        filter: handleSearchFilter,
      }}
      header={<Header />}
      headerSmall={<HeaderSmall />}
    />
  );
}

function Header() {
  const { data } = useSession();
  const { sidebar, setSidebar } = useSidebar();
  const { open: openTab } = useTabs();

  const appHome = data?.user?.action;
  const { logo: appLogo, name: appName = "logo" } = data?.application ?? {};

  const onLogoClick = useCallback(() => {
    if (appHome) {
      openTab(appHome);
    }
  }, [appHome, openTab]);

  return (
    <div className={styles.header}>
      <div className={styles.toggle} onClick={(e) => setSidebar(!sidebar)}>
        <MaterialIcon className={styles.toggleIcon} icon="menu" />
      </div>
      <div className={styles.appLogo} onClick={onLogoClick}>
        {appLogo ? <img src={appLogo} alt={appName} /> : <AppLogo />}
      </div>
    </div>
  );
}

function HeaderSmall() {
  const { data } = useSession();
  const { icon: appIcon, name: appName = "icon" } = data?.application ?? {};
  return (
    <div className={styles.header}>
      <div className={styles.appIcon}>
        {appIcon ? (
          <img src={appIcon} alt={appName} />
        ) : (
          <AppIcon viewBox="0 0 241 228" />
        )}
      </div>
    </div>
  );
}
