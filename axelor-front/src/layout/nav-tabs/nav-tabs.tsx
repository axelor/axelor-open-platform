import clsx from "clsx";
import { useAtomValue } from "jotai";
import { memo, useCallback, useEffect, useRef } from "react";

import { Box, NavItemProps, NavTabs as Tabs } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { useMenu } from "@/hooks/use-menu";
import { useSession } from "@/hooks/use-session";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { MenuItem } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { Views } from "@/view-containers/views";

import { PopupViews } from "@/view-containers/view-popup";
import styles from "./nav-tabs.module.scss";

function useIcon(id: string) {
  const { menus } = useMenu();
  const map = menus.reduce(
    (prev, menu) => ({ ...prev, [menu.name]: menu }),
    {} as Record<string, MenuItem>
  );

  const found = menus.find((x) => x.action === id);

  let parent: MenuItem | undefined = undefined;
  let last = found?.parent;
  while (last) {
    parent = map[last];
    last = parent?.parent;
  }

  const icon = found?.icon ?? parent?.icon;
  const iconColor = found?.iconBackground ?? parent?.iconBackground;

  return {
    icon,
    iconColor,
  };
}

const NavIcon = memo(function NavIcon({
  icon,
  iconColor,
}: {
  icon: string;
  iconColor?: string;
}) {
  const color = iconColor?.startsWith("#") ? iconColor : undefined;
  const colorClass = color ? undefined : `fg-${iconColor}`;

  const elemRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (elemRef.current) {
      const style = getComputedStyle(elemRef.current);
      const bgColor = style.color.replace(/rgb\((.*?)\)/, "rgba($1, 0.25)");
      elemRef.current.style.backgroundColor = bgColor;
    }
  }, []);

  return (
    <div
      className={clsx(styles.tabIcon, legacyClassNames(colorClass))}
      ref={elemRef}
    >
      <i className={legacyClassNames("fa", icon)} />
    </div>
  );
});

const NavTab = memo(function NavTab({
  close,
  ...props
}: NavItemProps & { close: (view: any) => any }) {
  const tab = props as Tab;
  const { title } = useAtomValue(tab.state);
  const { icon, iconColor } = useIcon(tab.id);

  const { data } = useSession();
  const showClose = tab.id !== data?.user?.action;

  const handleClose = useCallback<React.MouseEventHandler<HTMLDivElement>>(
    (e) => {
      e.preventDefault();
      e.stopPropagation();
      close(tab.id);
    },
    [close, tab]
  );

  return (
    <div className={styles.tab}>
      {icon && <NavIcon icon={icon} iconColor={iconColor} />}
      <div className={styles.tabTitle}>{title}</div>
      {showClose && (
        <div className={styles.tabClose} onClick={handleClose}>
          <MaterialIcon icon="close" weight={300} opticalSize={20} />
        </div>
      )}
    </div>
  );
});

export function NavTabs() {
  const { active, items, popups, open, close } = useTabs();
  const value = active?.id;

  const handleSelect = useCallback((e: any, tab: any) => open(tab.id), [open]);

  return (
    <Box
      d="flex"
      flexDirection="column"
      overflow="hidden"
      flexGrow={1}
      className={styles.tabs}
    >
      <Tabs
        items={items}
        value={value}
        onItemRender={(item) => <NavTab {...item} close={close} />}
        onChange={handleSelect}
      />
      {items.map((tab) => (
        <div
          key={tab.id}
          className={clsx(styles.tabContent, {
            [styles.active]: tab.id === value,
          })}
        >
          <Views tab={tab} />
        </div>
      ))}
      {popups.map((tab) => (
        <PopupViews key={tab.id} tab={tab} />
      ))}
    </Box>
  );
}
