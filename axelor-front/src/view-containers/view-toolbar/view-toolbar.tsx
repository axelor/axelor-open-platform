import { useMemo } from "react";

import { Box, CommandBar, CommandItemProps } from "@axelor/ui";
import { MaterialIconProps } from "@axelor/ui/src/icons/meterial-icon";

import { useSession } from "@/hooks/use-session";
import { ViewData } from "@/services/client/meta";
import { toTitleCase } from "@/utils/names";

import { useViewAction, useViewState, useViewSwitch } from "../views/scope";
import styles from "./view-toolbar.module.scss";

export type ViewToolBarProps = {
  actions: CommandItemProps[];
  children?: React.ReactNode;
  pagination?: {
    text?: string | (() => JSX.Element);
    canNext?: boolean;
    canPrev?: boolean;
    onNext: () => any;
    onPrev: () => any;
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

export function ViewToolBar(props: ViewToolBarProps) {
  const {
    meta,
    actions = [],
    children,
    pagination: { text: pageTextOrComp, canNext, canPrev, onNext, onPrev } = {},
  } = props;

  const pageActions = onPrev || onNext;

  const { data: sessionInfo } = useSession();
  const { actionId, views = [] } = useViewAction();
  const [{ type: viewType }] = useViewState();
  const switchTo = useViewSwitch();

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
        onClick: () => switchTo(key),
      };
    });
  }, [switchTo, views, viewType]);

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
    <Box className={styles.toolbar} shadow>
      <CommandBar
        className={styles.actions}
        iconProps={{
          weight: 300,
        }}
        iconOnly
        items={actions}
      />
      <Box className={styles.extra}>{children}</Box>
      {pageText && <Box className={styles.pageInfo}>{pageText}</Box>}
      {PageComp && <PageComp />}
      {pageActions && (
        <CommandBar
          className={styles.pageActions}
          iconProps={{
            weight: 300,
          }}
          items={[
            {
              key: "prev",
              text: "Prev",
              iconProps: {
                icon: "navigate_before",
              },
              disabled: !canPrev,
              onClick: onPrev,
            },
            {
              key: "next",
              text: "Next",
              iconSide: "end",
              iconProps: {
                icon: "navigate_next",
              },
              disabled: !canNext,
              onClick: onNext,
            },
          ]}
        />
      )}
      {switchActions && (
        <CommandBar
          items={switchActions}
          className={styles.viewSwitch}
          iconProps={{
            weight: 300,
          }}
        />
      )}
      {sessionInfo?.user.technical && (
        <CommandBar
          items={farItems}
          className={styles.farItems}
          iconProps={{
            weight: 300,
          }}
        />
      )}
    </Box>
  );
}
