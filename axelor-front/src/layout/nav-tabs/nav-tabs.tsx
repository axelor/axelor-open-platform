import clsx from "clsx";
import { atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import {
  ReactElement,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Menu as AxMenu,
  MenuDivider as AxMenuDivider,
  MenuItem as AxMenuItem,
  Box,
  NavTabItem,
  Portal,
  NavTabs as Tabs,
} from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { Tooltip } from "@/components/tooltip";
import { useMenu } from "@/hooks/use-menu";
import { useResponsiveContainer } from "@/hooks/use-responsive";
import { useSession } from "@/hooks/use-session";
import { useShortcut } from "@/hooks/use-shortcut";
import { Tab, useTabs } from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";
import { MenuItem } from "@/services/client/meta.types";
import { session } from "@/services/client/session";
import { PopupViews } from "@/view-containers/view-popup";
import { Views } from "@/view-containers/views";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Icon } from "@/components/icon";
import styles from "./nav-tabs.module.scss";

const tabContainerSizeAtom = atom<string | undefined>(undefined);

export function useNavTabsSize() {
  return useAtomValue(tabContainerSizeAtom);
}

export function NavTabs({ container }: { container: HTMLDivElement | null }) {
  const { active, items: tabs, popups, open, close } = useTabs();
  const value = active?.id;

  const [menuTarget, setMenuTarget] = useState<HTMLElement | null>(null);
  const [menuOffset, setMenuOffset] = useState<[number, number]>([0, 0]);
  const [menuShow, setMenuShow] = useState(false);

  const ref = useRef<HTMLDivElement>(null);
  const size = useResponsiveContainer(ref);
  const containerSize = Object.keys(size).find((x) => (size as any)[x]);

  const setTabContainerSize = useSetAtom(tabContainerSizeAtom);
  useEffect(
    () => setTabContainerSize(containerSize),
    [setTabContainerSize, containerSize]
  );

  const doClose = useAtomCallback(
    useCallback(
      (get, set, tab: string) => {
        const found = tabs.find((x) => x.id === tab);
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
      [close, tabs]
    )
  );

  const doCloseAll = useCallback(
    async (except?: string) => {
      for (const tab of tabs) {
        if (except && tab.id === except) continue;
        if (await doClose(tab.id)) continue;
        break;
      }
    },
    [doClose, tabs]
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

  const handleItemClick = useCallback(
    (item: NavTabItem) => {
      open(item.id);
      document.dispatchEvent(
        new CustomEvent("tab:click", {
          detail: item.id,
        })
      );
    },
    [open]
  );

  const handleAuxClick = useCallback<React.MouseEventHandler<HTMLDivElement>>(
    (e) => {
      const id = e.currentTarget.getAttribute("data-tab-id");
      // check middle scroll button click
      if (id && e.button === 1) {
        doClose(id);
      }
    },
    [doClose]
  );

  const items = useItems(tabs, close, handleAuxClick, onContextMenu);

  const showInPortal = items.length > 0 && !!container;
  const showInplace = items.length > 0 && !showInPortal;

  return (
    <div
      ref={ref}
      className={styles.tabs}
      data-tab-container-size={containerSize}
    >
      {showInPortal && (
        <Portal container={container}>
          <Tabs
            className={styles.tabList}
            items={items}
            active={value}
            onItemClick={handleItemClick}
          />
        </Portal>
      )}
      {showInplace && <SingleTab items={items} />}
      {tabs.map((tab) => (
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
    </div>
  );
}

function useItems(
  tabs: Tab[],
  closeTab: (view: any) => void,
  onAuxClick: React.MouseEventHandler<HTMLDivElement>,
  onContextMenu: React.MouseEventHandler<HTMLDivElement>
) {
  const { menus } = useMenu();
  const map = useMemo(
    () =>
      menus.reduce(
        (prev, menu) => ({ ...prev, [menu.name]: menu }),
        {} as Record<string, MenuItem>
      ),
    [menus]
  );

  const findIcon = useCallback(
    (id: string) => {
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
    },
    [map, menus]
  );

  return useMemo(() => {
    return tabs.map((tab) => {
      const { id } = tab;
      const { icon, iconColor } = findIcon(id);
      const item: NavTabItem = {
        id,
        title: <TabTitle tab={tab} close={closeTab} />,
        icon: icon ? () => <TabIcon icon={icon} /> : undefined,
        iconColor,
        onAuxClick,
        onContextMenu,
      };
      return item;
    });
  }, [findIcon, onAuxClick, closeTab, onContextMenu, tabs]);
}

function SingleTab({ items }: { items: NavTabItem[] }) {
  const [item] = items;
  if (item) {
    const { title } = item;
    return <div className={styles.singleTab}>{title}</div>;
  }
  return null;
}

function TabTitle({ tab, close }: { tab: Tab; close: (view: any) => any }) {
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
    <Popover title={title} tab={tab}>
      <div
        data-tab={tab.id}
        className={clsx(styles.tabTitle, {
          [styles.dirty]: dirty,
        })}
      >
        <div className={styles.tabText}>{title}</div>
        {showClose && (
          <div className={styles.tabClose} onClick={handleClose}>
            <MaterialIcon icon="close" fontSize={20} />
          </div>
        )}
      </div>
    </Popover>
  );
}

function Popover({
  children,
  title,
  tab,
}: {
  title: string;
  tab: Tab;
  children: ReactElement;
}) {
  const technical = session.info?.user?.technical;

  if (technical) {
    return (
      <Tooltip title={title} content={() => <PopoverContent tab={tab} />}>
        {children}
      </Tooltip>
    );
  }

  return children;
}

function PopoverContent({ tab }: { tab: Tab }) {
  const action = tab.action.name;
  const object = tab.action.model;
  const domain = tab.action.domain;
  const { name: view } = useAtomValue(tab.state);

  return (
    <Box className={styles.tooltip}>
      <dl>
        <dt>{i18n.get("Action")}</dt>
        <dd>
          <code>{action}</code>
        </dd>
        {object && (
          <>
            <dt>{i18n.get("Object")}</dt>
            <dd>
              <code>{object}</code>
            </dd>
          </>
        )}
        {domain && (
          <>
            <dt>{i18n.get("Domain")}</dt>
            <dd>
              <code>{domain}</code>
            </dd>
          </>
        )}
        {view && (
          <>
            <dt>{i18n.get("View")}</dt>
            <dd>
              <code>{view}</code>
            </dd>
          </>
        )}
      </dl>
    </Box>
  );
}

function TabIcon({ icon }: { icon: string }) {
  return <Icon icon={icon} className={clsx(styles.tabIcon)} />;
}
