import { useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Box, CommandBar, CommandItem, CommandItemProps } from "@axelor/ui";
import { MaterialIconProps } from "@axelor/ui/icons/material-icon";

import { dialogs } from "@/components/dialogs";
import { useSession } from "@/hooks/use-session";
import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { toTitleCase } from "@/utils/names";

import {
  useSelectViewState,
  useViewAction,
  useViewConfirmDirty,
  useViewDirtyAtom,
  useViewSwitch,
  useViewTab,
} from "../views/scope";

import { Icon } from "@/components/icon";
import { createScriptContext } from "@/hooks/use-parser/context";
import { parseExpression } from "@/hooks/use-parser/utils";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { useRoute } from "@/hooks/use-route";
import { useNavShortcuts } from "@/hooks/use-shortcut";
import {
  Button,
  Menu,
  MenuDivider,
  MenuItem,
  Widget,
} from "@/services/client/meta.types";
import { RecordHandler } from "@/views/form/builder";
import { useAtomValue } from "jotai";
import { ActionExecutor } from "../action";

import styles from "./view-toolbar.module.scss";

export type ViewToolBarProps = {
  actions: CommandItemProps[];
  actionExecutor?: ActionExecutor;
  recordHandler?: RecordHandler;
  children?: React.ReactNode;
  pagination?: {
    text?: string | (() => JSX.Element);
    canNext?: boolean;
    canPrev?: boolean;
    onNext: () => any;
    onPrev: () => any;
    actions?: CommandItemProps[];
  };
  meta: ViewData<any>;
};

const ViewIcons: Record<string, MaterialIconProps["icon"]> = {
  grid: "list",
  form: "article",
  cards: "grid_view",
  tree: "account_tree",
  calendar: "calendar_month",
  kanban: "view_kanban",
  gantt: "calendar_month",
  chart: "monitoring",
  dashboard: "dashboard",
};

function ActionCommandItem({
  recordHandler,
  showIf,
  hideIf,
  readonlyIf,
  ...props
}: CommandItemProps &
  Pick<ViewToolBarProps, "recordHandler"> &
  Pick<Widget, "showIf" | "hideIf" | "readonlyIf">) {
  const [hidden, setHidden] = useState<boolean | undefined>(props.hidden);
  const [readonly, setReadonly] = useState<boolean>(false);
  const { action } = useViewTab();

  useEffect(() => {
    const updateAttrs = (record: DataRecord) => {
      (showIf || hideIf) &&
        setHidden((hidden) => {
          if (showIf) {
            hidden = !parseExpression(showIf)(record);
          } else if (hideIf) {
            hidden = parseExpression(hideIf)(record);
          }
          return hidden;
        });
      readonlyIf && setReadonly(parseExpression(readonlyIf)(record));
    };

    if (recordHandler) {
      return recordHandler.subscribe(updateAttrs);
    } else {
      updateAttrs(createScriptContext(action.context ?? {}));
    }
  }, [showIf, hideIf, readonlyIf, recordHandler, action.context]);

  return (
    <CommandItem
      {...props}
      hidden={hidden}
      {...(readonly && { onClick: undefined, disabled: true })}
    />
  );
}

type ToolbarItem = Menu | MenuItem | MenuDivider | Button;

function getTextFull(item: ToolbarItem) {
  return item.showTitle !== false ? item.title ?? "" : "";
}

function getTextResponsive(item: ToolbarItem) {
  return item.title ?? toTitleCase(item.name ?? "");
}

export function ToolbarActions({
  buttons,
  menus,
  recordHandler,
  actionExecutor,
  parentRef,
  parentWidth,
}: Pick<ViewToolBarProps, "actionExecutor" | "recordHandler"> & {
  buttons?: Button[];
  menus?: Menu[];
  parentRef?: React.RefObject<HTMLDivElement>;
  parentWidth?: number;
}) {
  const innerRef = useRef<HTMLDivElement | null>(null);

  const responsive = useMemo(() => {
    // Compute total width of children, excluding responsive dropdown menu.
    let width = innerRef.current?.offsetWidth ?? 0;
    const children = (parentRef?.current?.children ?? []) as HTMLElement[];
    for (let i = 1; i < children.length; ++i) {
      width += children[i].offsetWidth;
    }
    return (
      Math.ceil(parentWidth ?? parentRef?.current?.offsetWidth ?? 0) <
      Math.ceil(width ?? 0)
    );
  }, [parentRef, innerRef, parentWidth]);

  const getItems = useCallback(
    (getText: (item: ToolbarItem) => string = getTextFull) => {
      let ind = 0;

      const mapItem = (item: ToolbarItem): CommandItemProps => {
        const action = (item as Button).onClick || (item as MenuItem).action;
        const text = getText(item);
        const { icon, prompt, help } = item as Button;
        const key = `action_${++ind}`;
        const hasExpr = item.showIf || item.hideIf || item.readonlyIf;
        return {
          key,
          text,
          divider: item.type === "menu-item-devider",
          description: help,
          iconOnly: !text && icon,
          showDownArrow: item.type === "menu",
          ...(icon && {
            icon: ({ className }: { className: string }) => (
              <Icon icon={icon} className={className} />
            ),
          }),
          ...(action && {
            onClick: async () => {
              if (prompt) {
                const confirmed = await dialogs.confirm({
                  content: prompt,
                });
                if (!confirmed) return;
              }
              actionExecutor?.execute(action, {
                context: {
                  _signal: item.name,
                  _source: item.name,
                },
              });
            },
          }),
          ...((item as Menu).items && {
            items: (item as Menu).items?.map(mapItem),
          }),
          ...(hasExpr && {
            render: (props) => (
              <ActionCommandItem
                {...props}
                showIf={item.showIf}
                hideIf={item.hideIf}
                readonlyIf={item.readonlyIf}
                recordHandler={recordHandler}
              />
            ),
          }),
        } as CommandItemProps;
      };

      return [...(buttons || []), ...(menus || [])].map(mapItem);
    },
    [buttons, menus, actionExecutor, recordHandler]
  );

  const [items, responsiveItems] = useMemo(
    () => [getItems(), getItems(getTextResponsive)],
    [getItems]
  );

  return (
    <Box d="flex" textWrap={false} overflow="hidden">
      {responsive && (
        <CommandBar
          items={[
            {
              key: "more",
              iconOnly: true,
              iconProps: {
                icon: "settings",
              },
              items: responsiveItems,
              showDownArrow: true,
            } as CommandItemProps,
          ]}
        />
      )}
      <Box
        ref={innerRef}
        {...(responsive && {
          style: {
            visibility: "hidden",
          },
        })}
      >
        <CommandBar items={items} />
      </Box>
    </Box>
  );
}

export function ViewToolBar(props: ViewToolBarProps) {
  const {
    meta,
    actions = [],
    actionExecutor,
    recordHandler,
    children,
    pagination: {
      text: pageTextOrComp,
      canNext,
      canPrev,
      onNext,
      onPrev,
      actions: paginationActions,
    } = {},
  } = props;
  const { view } = meta;
  const { toolbar, menubar } = view;
  const pageActions = onPrev || onNext || paginationActions;

  const showConfirmDirty = useViewConfirmDirty();
  const dirtyAtom = useViewDirtyAtom();
  const dirty = useAtomValue(dirtyAtom) ?? false;

  const { data: sessionInfo } = useSession();
  const { actionId, views = [] } = useViewAction();
  const viewType = useSelectViewState(useCallback((state) => state.type, []));
  const viewTab = useViewTab();
  const switchTo = useViewSwitch();
  const { navigate } = useRoute();

  const switchToView = useAtomCallback(
    useCallback(
      (get, set, type: string) => {
        showConfirmDirty(
          async () => dirty,
          async () => {
            const { props = {} } = get(viewTab.state);
            const { selectedId = 0 } = props[viewType] ?? {};
            if (viewType === "grid" && type === "form" && selectedId > 0) {
              switchTo(type, {
                route: { id: String(selectedId) },
              });
            } else {
              switchTo(type);
            }
          }
        );
      },
      [dirty, switchTo, viewTab.state, viewType, showConfirmDirty]
    )
  );

  const handlePrev = useCallback(() => {
    showConfirmDirty(
      async () => dirty,
      async () => onPrev?.()
    );
  }, [dirty, onPrev, showConfirmDirty]);

  const handleNext = useCallback(() => {
    showConfirmDirty(
      async () => dirty,
      async () => onNext?.()
    );
  }, [dirty, onNext, showConfirmDirty]);

  useNavShortcuts({
    viewType: view.type,
    onPrev: canPrev ? handlePrev : undefined,
    onNext: canNext ? handleNext : undefined,
  });

  const switchActions = useMemo(() => {
    if (views.length === 1) return;
    return views.map((item) => {
      const key = item.type;
      const text = toTitleCase(item.type);
      return {
        key,
        text,
        description: text,
        iconOnly: true,
        disabled: viewType === key,
        iconProps: {
          icon: ViewIcons[key] ?? "table_view",
        },
        onClick: () => switchToView(key),
      };
    });
  }, [views, viewType, switchToView]);

  const farItems = useMemo(() => {
    const items: CommandItemProps[] = [];
    const view = meta.view;

    if (view?.viewId) {
      items.push({
        key: "view",
        text: "View...",
        onClick: () => {
          navigate(`/ds/form::com.axelor.meta.db.MetaView/edit/${view.viewId}`);
        },
      });
    }

    if (view?.modelId) {
      items.push({
        key: "model",
        text: "Model...",
        onClick: () => {
          navigate(
            `/ds/form::com.axelor.meta.db.MetaModel/edit/${view.modelId}`
          );
        },
      });
    }

    if (actionId) {
      items.push({
        key: "action",
        text: "Action...",
        onClick: () => {
          navigate(`/ds/form::com.axelor.meta.db.MetaAction/edit/${actionId}`);
        },
      });
    }

    const command: CommandItemProps = {
      key: "settings",
      iconOnly: true,
      iconProps: {
        icon: "settings",
      },
      items,
    };

    return [command];
  }, [actionId, meta.view, navigate]);

  const pageText =
    typeof pageTextOrComp === "string" ? pageTextOrComp : undefined;
  const PageComp =
    typeof pageTextOrComp === "function" ? pageTextOrComp : undefined;

  const { ref, width } = useResizeDetector();

  return (
    <Box className={styles.toolbar}>
      <CommandBar className={styles.actions} iconOnly items={actions} />
      <Box ref={ref} d="flex" className={styles.extra}>
        {(toolbar?.length > 0 || menubar?.length > 0) && (
          <ToolbarActions
            buttons={toolbar}
            menus={menubar}
            actionExecutor={actionExecutor}
            recordHandler={recordHandler}
            parentRef={ref}
            parentWidth={width}
          />
        )}
        {children}
      </Box>
      {pageText && <Box className={styles.pageInfo}>{pageText}</Box>}
      {PageComp && (
        <Box className={styles.pageInfo}>
          <PageComp />
        </Box>
      )}
      {pageActions && (
        <CommandBar
          className={styles.pageActions}
          items={[
            {
              key: "prev",
              iconProps: {
                icon: "navigate_before",
              },
              disabled: !canPrev,
              onClick: handlePrev,
            },
            {
              key: "next",
              iconSide: "end",
              iconProps: {
                icon: "navigate_next",
              },
              disabled: !canNext,
              onClick: handleNext,
            },
            ...(paginationActions || []),
          ]}
        />
      )}
      {switchActions && (
        <CommandBar items={switchActions} className={styles.viewSwitch} />
      )}
      {sessionInfo?.user?.technical && (
        <CommandBar items={farItems} className={styles.farItems} />
      )}
    </Box>
  );
}
