import { useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Box, CommandBar, CommandItem, CommandItemProps } from "@axelor/ui";
import { MaterialIconProps } from "@axelor/ui/src/icons/meterial-icon";

import { dialogs } from "@/components/dialogs";
import { useSession } from "@/hooks/use-session";
import { ViewData } from "@/services/client/meta";
import { toTitleCase } from "@/utils/names";

import {
  useSelectViewState,
  useViewAction,
  useViewDirtyAtom,
  useViewSwitch,
  useViewTab,
} from "../views/scope";

import { parseExpression } from "@/hooks/use-parser/utils";
import {
  Button,
  Menu,
  MenuDivider,
  MenuItem,
  Widget,
} from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";
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
  grid: "table",
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

  useEffect(() => {
    if (recordHandler) {
      return recordHandler.subscribe((record) => {
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
      });
    }
  }, [showIf, hideIf, readonlyIf, recordHandler]);

  return (
    <CommandItem
      {...props}
      hidden={hidden}
      {...(readonly && { onClick: undefined })}
    />
  );
}

function ToolbarActions({
  buttons,
  menus,
  recordHandler,
  actionExecutor,
}: Pick<ViewToolBarProps, "actionExecutor" | "recordHandler"> & {
  buttons?: Button[];
  menus?: Menu[];
}) {
  const items = useMemo(() => {
    let ind = 0;
    const mapItem = (
      item: Menu | MenuItem | MenuDivider | Button
    ): CommandItemProps => {
      const action = (item as Button).onClick || (item as MenuItem).action;
      const text = item.showTitle !== false ? item.title : "";
      const icon = (item as Button).icon;
      const key = `action_${++ind}`;
      const hasExpr = item.showIf || item.hideIf || item.readonlyIf;
      return {
        key,
        text,
        divider: item.type === "menu-item-devider",
        iconOnly: !text && icon,
        ...(icon && {
          icon: ({ className }: { className: string }) => (
            <i
              className={legacyClassNames("fa", icon, styles.icon, className)}
            />
          ),
        }),
        ...(action && {
          onClick: () =>
            actionExecutor?.execute(action, {
              context: {
                _signal: item.name,
                _source: item.name,
              },
            }),
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
  }, [buttons, menus, actionExecutor, recordHandler]);

  return <CommandBar items={items} />;
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

  const dirtyAtom = useViewDirtyAtom();
  const dirty = useAtomValue(dirtyAtom) ?? false;

  const { data: sessionInfo } = useSession();
  const { actionId, views = [] } = useViewAction();
  const viewType = useSelectViewState(useCallback((state) => state.type, []));
  const viewTab = useViewTab();
  const switchTo = useViewSwitch();

  const switchToView = useAtomCallback(
    useCallback(
      (get, set, type: string) => {
        dialogs.confirmDirty(
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
      [dirty, switchTo, viewTab.state, viewType]
    )
  );

  const handlePrev = useCallback(() => {
    dialogs.confirmDirty(
      async () => dirty,
      async () => onPrev?.()
    );
  }, [dirty, onPrev]);

  const handleNext = useCallback(() => {
    dialogs.confirmDirty(
      async () => dirty,
      async () => onNext?.()
    );
  }, [dirty, onNext]);

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
      });
    }

    if (view?.modelId) {
      items.push({
        key: "model",
        text: "Model...",
      });
    }

    if (actionId) {
      items.push({
        key: "action",
        text: "Action...",
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
  }, [actionId, meta.view]);

  const pageText =
    typeof pageTextOrComp === "string" ? pageTextOrComp : undefined;
  const PageComp =
    typeof pageTextOrComp === "function" ? pageTextOrComp : undefined;

  return (
    <Box className={styles.toolbar} borderBottom>
      <CommandBar className={styles.actions} iconOnly items={actions} />
      {toolbar?.length > 0 && (
        <ToolbarActions
          buttons={toolbar}
          menus={menubar}
          actionExecutor={actionExecutor}
          recordHandler={recordHandler}
        />
      )}
      <Box className={styles.extra}>{children}</Box>
      {pageText && <Box className={styles.pageInfo}>{pageText}</Box>}
      {PageComp && <PageComp />}
      {pageActions && (
        <CommandBar
          className={styles.pageActions}
          items={[
            {
              key: "prev",
              text: "Prev",
              iconProps: {
                icon: "navigate_before",
              },
              disabled: !canPrev,
              onClick: handlePrev,
            },
            {
              key: "next",
              text: "Next",
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
      {sessionInfo?.user.technical && (
        <CommandBar items={farItems} className={styles.farItems} />
      )}
    </Box>
  );
}
