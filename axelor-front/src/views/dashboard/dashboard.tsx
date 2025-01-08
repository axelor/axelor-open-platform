import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Layout, Layouts, Responsive, WidthProvider } from "react-grid-layout";

import { Box, clsx, useTheme } from "@axelor/ui";

import { saveView } from "@/services/client/meta-cache";
import {
  Dashboard as DashboardView,
  PanelDashlet,
  Schema,
} from "@/services/client/meta.types";

import { DashletComponent } from "../form/widgets/dashlet";
import { ViewProps } from "../types";
import { useResizeDetector } from "@/hooks/use-resize-detector";
import { isUserAllowedCustomizeViews } from "@/utils/app-settings.ts";
import { useViewContext } from "@/view-containers/views/scope";
import { useDevice } from "@/hooks/use-responsive";
import { DataRecord } from "@/services/client/data.types";
import { createScriptContext } from "@/hooks/use-parser/context";
import { parseExpression } from "@/hooks/use-parser/utils";

import "react-grid-layout/css/styles.css";
import "./react-grid-layout.css";

import { DashboardContext } from "./scope";
import { prepareSearchFormMeta } from "../search/utils";
import { DashboardSearch } from "./dashboard-search";
import dashletStyles from "../form/widgets/dashlet/dashlet.module.scss";
import styles from "./dashboard.module.scss";

const GridLayout = WidthProvider(Responsive);

type MEDIA_TYPE = "xxs" | "xs" | "sm" | "md" | "lg";

const CARD_HEIGHT = 50;
const BREAKPOINTS: Record<MEDIA_TYPE, number> = {
  lg: 1200,
  md: 996,
  sm: 768,
  xs: 480,
  xxs: 480,
};
const COLS: Record<MEDIA_TYPE, number> = {
  lg: 12,
  md: 6,
  sm: 6,
  xs: 4,
  xxs: 4,
} as const;

const toNumbers = (str: number | string) =>
  str ? String(str).split(",").map(Number) : [];

const getHeight = (span: number, fixedHeight?: string) => {
  if (fixedHeight) {
    const h = parseInt(fixedHeight);
    if (h) {
      return Math.floor(h / CARD_HEIGHT);
    }
  }
  return (50 * span) / CARD_HEIGHT;
};

const getAttrs = (item: PanelDashlet, type: MEDIA_TYPE) => {
  const { colOffset = "", rowOffset = "", colSpan = "", rowSpan = "" } = item;

  const X = toNumbers(colOffset);
  const Y = toNumbers(rowOffset);
  const W = toNumbers(colSpan);
  const H = toNumbers(rowSpan);

  const obj = ["lg", "md", "sm", "xs", "xxs"].reduce(
    (obj: any, k, ind) => ({
      ...obj,
      [k]: {
        x: X[ind],
        y: Y[ind],
        w: W[ind],
        h: H[ind],
      },
    }),
    {},
  );
  return obj[type];
};

export function Dashboard({ meta }: ViewProps<DashboardView>) {
  const { view } = meta;
  const { items = [] } = view;
  const [layouts, setLayouts] = useState<Layouts | null>(null);
  const { ref, width } = useResizeDetector();
  const { isMobile } = useDevice();
  const saved = useRef(false);
  const getContext = useViewContext();

  // dashboard context
  const [context, setContext] = useState({} as DataRecord);
  const [isFormInit, setFormInit] = useState(!view.onInit);

  const isRTL = useTheme().dir === "rtl";

  const hasViewCustomize = isUserAllowedCustomizeViews() && !isMobile;

  const searchFormMeta = useMemo(
    () =>
      meta.view.searchFields?.length
        ? prepareSearchFormMeta({
            ...meta,
            view: {
              ...meta.view,
              name: "__search-fields-view__",
              title: "",
            },
            model: "com.axelor.meta.db.MetaAction",
          })
        : null,
    [meta],
  );

  const updateLayout = useCallback(
    (updater: (key: MEDIA_TYPE, layouts?: Layout[]) => Layout[]) => {
      setLayouts((layouts) => {
        return Object.keys(COLS).reduce(
          (obj, k) => ({
            ...obj,
            [k]: updater(k as MEDIA_TYPE, layouts?.[k]),
          }),
          [],
        ) as unknown as Layouts;
      });
    },
    [],
  );

  const handleDragStart = useCallback(() => {
    saved.current = true;
  }, []);

  const handleDragStop = useCallback(() => {
    saved.current = true;
  }, []);

  const handleInit = useCallback(() => {
    saved.current = true;
  }, []);

  const handleLayoutChange = useCallback(
    async (layout: Layout[], allLayout: Layouts) => {
      setLayouts(allLayout);
    },
    [],
  );

  const handleResize = useCallback(
    (layout: Layout[], oldItem: Layout, newItem: Layout) => {
      if (oldItem.w !== newItem.w || oldItem.h !== newItem.h) {
        saved.current = true;
      }
    },
    [],
  );

  const handleItemViewLoad = useCallback(
    (schema: Schema, viewId?: number, viewType?: string) => {
      const viewItems = items;
      updateLayout((type: MEDIA_TYPE, items?: Layout[]) => {
        return (items || []).map((item) =>
          String(item.i) === String(viewId)
            ? (() => {
                const custom = viewType === "custom";
                const bounded = ["chart", "grid"].includes(viewType!);
                const viewItem = viewItems[
                  item.i as unknown as number
                ] as unknown as PanelDashlet;
                const attrs = getAttrs(viewItem, type);
                return {
                  ...item,
                  h: attrs.h ?? getHeight(custom ? 3 : 7, viewItem?.height),
                  minH: item?.minH || (bounded ? getHeight(6) : getHeight(3)),
                  minW: item?.minW || (bounded ? 4 : 1),
                };
              })()
            : item,
        );
      });
    },
    [updateLayout, items],
  );

  useEffect(() => {
    updateLayout((type: MEDIA_TYPE) => {
      const { layout } = items
        .map((item, index) => ({ ...item, $key: `dashlet_${index}` }))
        .reduce(
          (acc, item, index) => {
            const span = Number(item.colSpan || 6);
            const attrs = getAttrs(item, type);

            if (acc.x + span > 12) {
              acc.x = 0;
              acc.y += 1;
            }

            (acc.layout as any).push({
              x: attrs.x ?? acc.x,
              y: attrs.y ?? acc.y,
              w: attrs.w ?? span,
              h: attrs.h ?? getHeight(6),
              minH: (item as any)?.minH || getHeight(3),
              minW: (item as any)?.minW || 1,
              i: `${index}`,
            });

            acc.x += span;

            return acc;
          },
          { layout: [], x: 0, y: 0 },
        );
      return layout;
    });
  }, [items, updateLayout]);

  useEffect(() => {
    if (saved.current && hasViewCustomize) {
      saved.current = false;
      (async () => {
        const { items } = view;
        function getItem(item: PanelDashlet, index: number) {
          const rowOffset: number[] = [];
          const colOffset: number[] = [];
          const rowSpan: number[] = [];
          const colSpan: number[] = [];

          if (layouts) {
            ["lg", "md", "sm", "xs", "xxs"].forEach((m) => {
              const $item = layouts[m as MEDIA_TYPE]?.find?.(
                (x) => String(x.i) === String(index),
              );
              if ($item) {
                colOffset.push($item.x);
                rowOffset.push($item.y);
                colSpan.push($item.w);
                rowSpan.push($item.h);
              }
            });
          }

          return {
            ...item,
            rowOffset: rowOffset.join(","),
            colOffset: colOffset.join(","),
            rowSpan: rowSpan.join(","),
            colSpan: colSpan.join(","),
          };
        }

        items &&
          (await saveView({
            ...view,
            items: items.map((item, index) => getItem(item, index)),
          }));
      })();
    }
  }, [hasViewCustomize, items, view, layouts]);

  useEffect(() => {
    if (ref.current && width) {
      window.dispatchEvent(new Event("resize"));
    }
  }, [ref, width]);

  const onSearchValueChanged = useCallback((values: DataRecord) => {
    setContext((vals) => ({ ...vals, ...values }));
  }, []);

  const onInitCompleted = useCallback(() => {
    setFormInit(true);
  }, []);

  const evalContext = useMemo(
    () => createScriptContext(getContext()),
    [getContext],
  );

  const getExpressionValue = useCallback(
    (expr?: string) => {
      if (!expr) return;
      if (["true", "false"].includes(String(expr).toLowerCase())) {
        return String(expr).toLowerCase() === "true";
      }
      return parseExpression(expr)(evalContext);
    },
    [evalContext],
  );

  const children = useMemo(
    () =>
      items.map((item, index) => (
        <Box key={index} d="flex" className={styles.container} rounded>
          <DashletComponent
            dashboard
            className={clsx(styles.dashlet, {
              [styles.customize]: hasViewCustomize,
            })}
            schema={item}
            viewId={index}
            canEdit={getExpressionValue(item.canEdit)}
            canDelete={item.canDelete !== undefined && getExpressionValue(item.canDelete)}
            canNew={item.canNew !== undefined && getExpressionValue(item.canNew)}
            onViewLoad={handleItemViewLoad}
            getContext={getContext}
          />
        </Box>
      )),
    [items, hasViewCustomize, handleItemViewLoad, getContext],
  );

  const isReady = searchFormMeta ? isFormInit : true;

  return (
    <Box
      ref={ref}
      className={clsx({
        [styles.rtl]: isRTL,
      })}
      d="flex"
      overflow="auto"
      flexGrow={1}
    >
      <Box w={100}>
        {searchFormMeta && (
          <Box d="flex" m={2} border roundedTop>
            <DashboardSearch
              meta={searchFormMeta}
              onInit={view.onInit}
              onInitCompleted={onInitCompleted}
              onChange={onSearchValueChanged}
            />
          </Box>
        )}
        <DashboardContext.Provider value={context}>
          {isReady && layouts && (
            <GridLayout
              isBounded={true}
              isDraggable={hasViewCustomize}
              className={styles.layout}
              layouts={layouts}
              rowHeight={CARD_HEIGHT}
              resizeHandles={["se"]}
              breakpoints={BREAKPOINTS}
              cols={COLS}
              draggableHandle={`.${dashletStyles.header}`}
              onLayoutChange={handleLayoutChange}
              onDragStart={handleDragStart}
              onDragStop={handleDragStop}
              onResizeStart={handleInit}
              onResizeStop={handleResize}
            >
              {children}
            </GridLayout>
          )}
        </DashboardContext.Provider>
      </Box>
    </Box>
  );
}
