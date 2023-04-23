import { useCallback, useState } from "react";
import { useAtomValue } from "jotai";
import { Box, CommandBar, CommandItemProps } from "@axelor/ui";
import { PageText } from "@/components/page-text";
import {
  DashletHandler,
  useDashletHandlerAtom,
} from "@/view-containers/view-dashlet/handler";
import { DataStore } from "@/services/client/data-store";
import { useDataStore } from "@/hooks/use-data-store";
import { download } from "@/utils/download";
import { ChartView } from "@/services/client/meta.types";
import { i18n } from "@/services/client/i18n";
import classes from "./dashlet-actions.module.scss";

interface DashletMenuProps extends DashletHandler {
  viewType?: string;
  items?: CommandItemProps[];
}

export function DashletActions({
  viewType,
}: Pick<DashletMenuProps, "viewType">) {
  const { view, dataStore, onAction, onLegendShowHide, onRefresh, onExport } =
    useAtomValue(useDashletHandlerAtom());
  const hasPagination = ["grid", "cards", "tree"].includes(viewType!);

  return (
    <Box className={classes.actions}>
      {dataStore && hasPagination ? (
        <DashletListMenu
          view={view}
          viewType={viewType}
          dataStore={dataStore}
          onAction={onAction}
          onRefresh={onRefresh}
        />
      ) : (
        <DashletMenu
          view={view}
          viewType={viewType}
          onAction={onAction}
          onLegendShowHide={onLegendShowHide}
          onRefresh={onRefresh}
          onExport={onExport}
        />
      )}
    </Box>
  );
}

function DashletMenu({
  items = [],
  view,
  viewType,
  onLegendShowHide,
  onAction,
  onRefresh,
  onExport,
}: DashletMenuProps) {
  const [legend, showLegend] = useState(true);

  function getViewOptions() {
    const hideLegend =
      viewType === "chart" && ((view as ChartView)?.config?.hideLegend ?? true);
    if (hideLegend) {
      return [
        {
          key: "show-hide-legend",
          text: legend ? i18n.get("Hide Legend") : i18n.get("Show Legend"),
          onClick: () => {
            showLegend(!legend);
            onLegendShowHide?.(!legend);
          },
        },
      ];
    }
    return [];
  }

  function getViewActions() {
    const actions =
      (viewType === "chart" && (view as ChartView)?.actions) || [];
    if (actions.length > 0) {
      return [
        { key: "chart-actions-divider", divider: true },
        ...actions.map(({ name, title, action }) => ({
          key: name!,
          text: title,
          onClick: () => {
            action && onAction?.(action, { _signal: name });
          },
        })),
      ];
    }
    return [];
  }

  return (
    <CommandBar
      iconProps={{ fontSize: 20 }}
      items={[
        ...items,
        {
          key: "settings",
          iconOnly: true,
          iconProps: {
            icon: "settings",
          },
          items: [
            {
              key: "refresh",
              text: i18n.get("Refresh"),
              onClick: onRefresh,
            },
            ...getViewOptions(),
            {
              key: "export",
              text: i18n.get("Export"),
              hidden: !onExport,
              onClick: onExport,
            },
            ...getViewActions(),
          ],
        },
      ]}
    />
  );
}

// grid/card dashlet pagination
function DashletListMenu(
  props: DashletMenuProps & {
    dataStore: DataStore;
  }
) {
  const { dataStore, viewType, ...menuProps } = props;
  const page = useDataStore(dataStore, (store) => store.page);
  const { offset = 0, limit = 40, totalCount = 0 } = page;
  const canPrev = offset > 0;
  const canNext = offset + limit < totalCount;

  const onExport = useCallback(async () => {
    const { fileName } = await dataStore.export({});
    download(
      `ws/rest/${dataStore.model}/export/${fileName}?fileName=${fileName}`,
      fileName
    );
  }, [dataStore]);

  return (
    <>
      <PageText dataStore={dataStore} />
      <DashletMenu
        {...menuProps}
        {...(viewType === "grid" && { onExport })}
        items={[
          {
            key: "prev",
            text: i18n.get("Prev"),
            iconProps: {
              icon: "navigate_before",
            },
            disabled: !canPrev,
            onClick: () => dataStore.search({ offset: offset - limit }),
          },
          {
            key: "next",
            text: i18n.get("Next"),
            iconSide: "end",
            iconProps: {
              icon: "navigate_next",
            },
            disabled: !canNext,
            onClick: () => dataStore.search({ offset: offset + limit }),
          },
        ]}
      />
    </>
  );
}
