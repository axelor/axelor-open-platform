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
  clsx,
  Menu as AxMenu,
  MenuDivider as AxMenuDivider,
  MenuItem as AxMenuItem,
  Box,
  NavTabItem,
  Portal,
  NavTabs as Tabs,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

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
import { isProduction } from "@/utils/app-settings.ts";

import { Icon } from "@/components/icon";
import styles from "./nav-tabs.module.scss";
import colors from "@/styles/legacy/_colors.module.scss";

const tabContainerSizeAtom = atom<string | undefined>(undefined);

export function useNavTabsSize() {
  return useAtomValue(tabContainerSizeAtom);
}

export function NavTabs({ container }: { container: HTMLDivElement | null }) {
  const { active, items: tabs, popups, open, close } = useTabs();
  const { data: sessionInfo } = useSession();
  const activeTabId = active?.id;

  const [menuTarget, setMenuTarget] = useState<HTMLElement | null>(null);
  const [menuOffset, setMenuOffset] = useState<[number, number]>([0, 0]);
  const [menuShow, setMenuShow] = useState(false);

  const ref = useRef<HTMLDivElement>(null);
  const size = useResponsiveContainer(ref);
  const containerSize = Object.keys(size).find((x) => (size as any)[x]);

  const setTabContainerSize = useSetAtom(tabContainerSizeAtom);
  useEffect(
    () => setTabContainerSize(containerSize),
    [setTabContainerSize, containerSize],
  );

  /**
   * The tab id of the `menuTarget` HTML element
   */
  const activeMenuTabId = useMemo(() => {
    return (
      menuTarget?.querySelector("[data-tab]")?.getAttribute("data-tab") ??
      undefined
    );
  }, [menuTarget]);

  /**
   * Determines if a given tab name matches the user home action.
   *
   * @param {string | undefined} tabName - The name of the tab to check.
   * @returns {boolean} - Returns `true` if `tabName` matches the user home action. Otherwise, returns `false`.
   */
  const isHomeAction = useCallback(
    (tabName: string | undefined) => {
      const userAction = sessionInfo?.user?.action;
      return !!userAction && tabName === userAction;
    },
    [sessionInfo],
  );

  const doClose = useAtomCallback(
    useCallback(
      (get, set, tab: string) => {
        const found = tabs.find((x) => x.id === tab);
        if (found && !isHomeAction(found.id)) {
          const {
            state,
            action: { params },
          } = found;
          const dirty = get(state).dirty ?? false;
          return dialogs.confirmDirty(
            async () => params?.["show-confirm"] !== false && dirty,
            async () => close(tab),
          );
        }
        return Promise.resolve(true);
      },
      [close, isHomeAction, tabs],
    ),
  );

  const doCloseAll = useCallback(
    async (except?: string) => {
      for (const tab of tabs) {
        if (except && tab.id === except) continue;
        if (await doClose(tab.id)) continue;
        break;
      }
    },
    [doClose, tabs],
  );

  const doRefresh = useCallback((tab: string) => {
    const event = new CustomEvent("tab:refresh", {
      detail: { id: tab },
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

      if (activeMenuTabId) {
        const action = e.currentTarget.getAttribute("data-action");
        if (action === "refresh") doRefresh(activeMenuTabId);
        else if (action === "close") doClose(activeMenuTabId);
        else if (action === "close-all") doCloseAll();
        else if (action === "close-others") doCloseAll(activeMenuTabId);
      }
    },
    [activeMenuTabId, doClose, doCloseAll, doRefresh],
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
        }),
      );
    },
    [open],
  );

  const handleAuxClick = useCallback<React.MouseEventHandler<HTMLDivElement>>(
    (e) => {
      const id = e.currentTarget.getAttribute("data-tab-id");
      // check middle scroll button click
      if (id && e.button === 1) {
        doClose(id);
      }
    },
    [doClose],
  );

  const items = useItems(tabs, close, handleAuxClick, onContextMenu);

  const showInPortal = items.length > 0 && !!container;
  const showInplace = items.length > 0 && !showInPortal;

  useTabAutoReload(active, doRefresh);

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
            active={activeTabId}
            onItemClick={handleItemClick}
          />
        </Portal>
      )}
      {showInplace && <SingleTab items={items} />}
      {tabs.map((tab) => (
        <div
          key={tab.id}
          data-tab-content={tab.id}
          data-tab-active={tab.id === activeTabId}
          className={clsx(styles.tabContent, {
            [styles.active]: tab.id === activeTabId,
          })}
        >
          <Views tab={tab} />
        </div>
      ))}
      {isProduction() && <TabsDirtyCheck tabs={tabs} />}
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
        {!isHomeAction(activeMenuTabId) && (
          <AxMenuItem data-action="close" onClick={handleContextClick}>
            {i18n.get("Close")}
          </AxMenuItem>
        )}
        <AxMenuItem data-action="close-others" onClick={handleContextClick}>
          {i18n.get("Close Others")}
        </AxMenuItem>
        {!isHomeAction(activeMenuTabId) && (
          <AxMenuItem data-action="close-all" onClick={handleContextClick}>
            {i18n.get("Close All")}
          </AxMenuItem>
        )}
      </AxMenu>
    </div>
  );
}

function TabsDirtyCheck({ tabs }: { tabs: Tab[] }) {
  const isTabDirty = useAtomCallback(
    useCallback((get) => tabs.some((tab) => get(tab.state).dirty), [tabs]),
  );

  useEffect(() => {
    function beforeUnload(e: BeforeUnloadEvent) {
      if (isTabDirty()) {
        e.preventDefault();
      }
    }

    window.addEventListener("beforeunload", beforeUnload);
    return () => {
      window.removeEventListener("beforeunload", beforeUnload);
    };
  }, [isTabDirty]);

  return null;
}

function useItems(
  tabs: Tab[],
  closeTab: (view: any) => void,
  onAuxClick: React.MouseEventHandler<HTMLDivElement>,
  onContextMenu: React.MouseEventHandler<HTMLDivElement>,
) {
  const { menus } = useMenu();
  const map = useMemo(
    () =>
      menus.reduce(
        (prev, menu) => ({ ...prev, [menu.name]: menu }),
        {} as Record<string, MenuItem>,
      ),
    [menus],
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
        iconColor: iconColor && (colors[iconColor] || iconColor),
      };
    },
    [map, menus],
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

function useTabAutoReload(tab: Tab | null, onRefresh: (tab: string) => void) {
  const [documentVisible, setDocumentVisible] = useState(true);

  const autoReload = tab?.action?.params?.["auto-reload"];

  const doReload = useAtomCallback(
    useCallback(
      (get) => {
        const activeTabId = tab?.id;
        if (activeTabId && tab?.state) {
          const state = get(tab.state);
          const isEditable =
            state.type === "form" &&
            Boolean(state.props?.form?.readonly) === false;
          const canAutoReload = !document.hidden && !isEditable;
          canAutoReload && onRefresh(activeTabId);
        }
      },
      [tab?.id, tab?.state, onRefresh],
    ),
  );

  useEffect(() => {
    if (tab?.id && documentVisible && autoReload) {
      const interval = Number(autoReload) * 1000;
      if (isNaN(interval) || interval <= 0) {
        return console.warn(
          `${tab.id} auto-reload value must be a positive number in seconds: ${interval}`,
        );
      }

      const timer = setInterval(() => doReload(), interval);

      return () => {
        clearInterval(timer);
      };
    }
  }, [tab?.id, autoReload, documentVisible, doReload]);

  useEffect(() => {
    if (!autoReload) return;
    const updateDocumentVisibility = () =>
      setDocumentVisible(document.visibilityState === "visible");

    document.addEventListener("visibilitychange", updateDocumentVisibility);

    return () => {
      document.removeEventListener(
        "visibilitychange",
        updateDocumentVisibility,
      );
    };
  }, [autoReload]);
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
    useMemo(() => selectAtom(tab.state, (x) => x.title), [tab.state]),
  );
  const dirty = useAtomValue(
    useMemo(() => selectAtom(tab.state, (x) => x.dirty ?? false), [tab.state]),
  );

  const { data } = useSession();
  const showClose = tab.id !== data?.user?.action;
  const canConfirm = tab.action.params?.["show-confirm"] !== false;

  const handleCloseConfirm = useCallback(async () => {
    await dialogs.confirmDirty(
      async () => canConfirm && dirty,
      async () => close(tab.id),
    );
  }, [close, dirty, canConfirm, tab.id]);

  const handleClose = useCallback<React.MouseEventHandler<HTMLDivElement>>(
    async (e) => {
      e.preventDefault();
      e.stopPropagation();
      await handleCloseConfirm();
    },
    [handleCloseConfirm],
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
            <MaterialIcon icon="close" />
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
