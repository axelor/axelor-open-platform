import clsx from "clsx";
import { useAtomValue } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { memo, useCallback, useMemo, useState } from "react";

import {
  Menu as AxMenu,
  MenuDivider as AxMenuDivider,
  MenuItem as AxMenuItem,
  Box,
  NavItemProps,
  NavTabs as Tabs,
  getRGB,
  getSupportedColor,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { dialogs } from "@/components/dialogs";
import { useMenu } from "@/hooks/use-menu";
import { useSession } from "@/hooks/use-session";
import { useShortcut } from "@/hooks/use-shortcut";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";
import { MenuItem } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
import { PopupViews } from "@/view-containers/view-popup";
import { Views } from "@/view-containers/views";

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
  const color = iconColor && getSupportedColor(iconColor);
  const backgroundColor = color && getRGB(color, 0.25);

  return (
    <div className={clsx(styles.tabIcon)} style={{ color, backgroundColor }}>
      <i className={legacyClassNames("fa", icon)} />
    </div>
  );
});

const NavTab = memo(function NavTab({
  close,
  ...props
}: NavItemProps & { close: (view: any) => any }) {
  const tab = props as Tab;
  const { icon, iconColor } = useIcon(tab.id);

  const title = useAtomValue(
    useMemo(() => selectAtom(tab.state, (x) => x.title), [tab.state])
  );
  const dirty = useAtomValue(
    useMemo(() => selectAtom(tab.state, (x) => x.dirty ?? false), [tab.state])
  );

  const { data } = useSession();
  const showClose = tab.id !== data?.user?.action;

  const handleCloseConfirm = useCallback(async () => {
    await dialogs.confirmDirty(
      async () => dirty,
      async () => close(tab.id)
    );
  }, [close, dirty, tab.id]);

  const handleClose = useCallback<React.MouseEventHandler<HTMLDivElement>>(
    async (e) => {
      e.preventDefault();
      e.stopPropagation();
      await handleCloseConfirm();
    },
    [handleCloseConfirm]
  );

  const { active } = useTabs();

  useShortcut({
    key: "q",
    ctrlKey: true,
    canHandle: useCallback(() => active?.id === tab.id, [active, tab.id]),
    action: handleCloseConfirm,
  });

  return (
    <div
      data-tab={tab.id}
      className={clsx(styles.tab, {
        [styles.dirty]: dirty,
      })}
      onAuxClick={handleClose}
    >
      {icon && <NavIcon icon={icon} iconColor={iconColor} />}
      <div className={styles.tabTitle}>{title}</div>
      {showClose && (
        <div className={styles.tabClose} onClick={handleClose}>
          <MaterialIcon icon="close" fontSize={20} />
        </div>
      )}
    </div>
  );
});

export function NavTabs() {
  const { active, items, popups, open, close } = useTabs();
  const value = active?.id;

  const [menuTarget, setMenuTarget] = useState<HTMLElement | null>(null);
  const [menuOffset, setMenuOffset] = useState<[number, number]>([0, 0]);
  const [menuShow, setMenuShow] = useState(false);

  const doClose = useAtomCallback(
    useCallback(
      (get, set, tab: string) => {
        const found = items.find((x) => x.id === tab);
        if (found) {
          const { state } = found;
          const dirty = get(state).dirty ?? false;
          return dialogs.confirmDirty(
            async () => dirty,
            async () => close(tab)
          );
        }
        return Promise.resolve(true);
      },
      [close, items]
    )
  );

  const doCloseAll = useCallback(
    async (except?: string) => {
      for (const tab of items) {
        if (except && tab.id === except) continue;
        if (await doClose(tab.id)) continue;
        break;
      }
    },
    [doClose, items]
  );

  const doRefresh = useCallback((tab: string) => {
    const event = new CustomEvent("tab:refresh", {
      detail: tab,
    });
    document.dispatchEvent(event);
  }, []);

  const onContextMenu = useCallback((e: React.MouseEvent<HTMLElement>) => {
    e.preventDefault();
    e.stopPropagation();

    const { top, left, height } = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - left;
    const y = e.clientY - top - height;

    setMenuTarget(e.currentTarget);
    setMenuOffset([x, y]);
    setMenuShow(true);
  }, []);

  const handleContextClick = useCallback(
    (e: React.MouseEvent<HTMLElement>) => {
      e.stopPropagation();
      e.preventDefault();
      setMenuTarget(null);
      setMenuShow(false);

      if (menuTarget) {
        const action = e.currentTarget.getAttribute("data-action");
        const target = menuTarget.querySelector("[data-tab]");
        if (target) {
          const tab = target.getAttribute("data-tab");
          if (tab && action === "refresh") doRefresh(tab);
          if (tab && action === "close") doClose(tab);
          if (tab && action === "close-all") doCloseAll();
          if (tab && action === "close-others") doCloseAll(tab);
        }
      }
    },
    [doClose, doCloseAll, doRefresh, menuTarget]
  );

  const handleContextHide = useCallback(() => {
    setMenuTarget(null);
    setMenuShow(false);
  }, []);

  const handleSelect = useCallback((e: any, tab: any) => open(tab.id), [open]);

  return (
    <Box
      d="flex"
      flexDirection="column"
      overflow="hidden"
      flexGrow={1}
      className={styles.tabs}
    >
      {items.length > 0 && (
        <Tabs
          items={items}
          value={value}
          onItemRender={(item) => <NavTab {...item} close={close} />}
          onChange={handleSelect}
          onContextMenu={onContextMenu}
        />
      )}
      {items.map((tab) => (
        <div
          key={tab.id}
          data-tab-content={tab.id}
          data-tab-active={tab.id === value}
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
      <AxMenu
        show={menuShow}
        onHide={handleContextHide}
        navigation
        target={menuTarget}
        offset={menuOffset}
      >
        <AxMenuItem data-action="refresh" onClick={handleContextClick}>
          {i18n.get("Refresh")}
        </AxMenuItem>
        <AxMenuDivider />
        <AxMenuItem data-action="close" onClick={handleContextClick}>
          {i18n.get("Close")}
        </AxMenuItem>
        <AxMenuItem data-action="close-others" onClick={handleContextClick}>
          {i18n.get("Close Others")}
        </AxMenuItem>
        <AxMenuItem data-action="close-all" onClick={handleContextClick}>
          {i18n.get("Close All")}
        </AxMenuItem>
      </AxMenu>
    </Box>
  );
}
