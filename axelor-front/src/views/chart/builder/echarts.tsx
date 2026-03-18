import { useEffect, useMemo, useRef } from "react";
import { useTheme } from "@axelor/ui";
import * as echarts from "echarts/core";
import {
  BarChart,
  FunnelChart,
  GaugeChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
} from "echarts/charts";
import {
  DatasetComponent,
  GridComponent,
  LegendScrollComponent,
  RadarComponent,
  TooltipComponent,
  TransformComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

import { ChartProps, ChartType } from "./types";
import { getColor, prepareTheme } from "./utils";
import { useAppTheme } from "@/hooks/use-app-theme";
import { DataRecord } from "@/services/client/data.types";
import classes from "./echarts.module.scss";

echarts.use([
  BarChart,
  FunnelChart,
  GaugeChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
  DatasetComponent,
  GridComponent,
  LegendScrollComponent,
  RadarComponent,
  TooltipComponent,
  TransformComponent,
  CanvasRenderer,
]);

export function ECharts({
  data,
  type,
  height,
  width,
  options,
  legend = true,
  onClick,
}: Pick<ChartProps, "data" | "legend" | "onClick"> & {
  type: ChartType;
  height: number;
  width: number;
  options: Partial<echarts.EChartsCoreOption>;
}) {
  const divRef = useRef<HTMLDivElement>(null);
  const chart = useRef<echarts.ECharts>(null);
  const theme = useAppTheme();
  const isRTL = useTheme().dir === "rtl";

  const seriesBy = useMemo(() => {
    const { series, xAxis } = data;
    const { groupBy } = series?.[0] ?? {};
    return groupBy || xAxis || "";
  }, [data.series, data.xAxis]);

  useEffect(() => {
    echarts.registerTheme(theme, prepareTheme(type));
  }, [type, theme]);

  useEffect(() => {
    if (divRef.current !== null) {
      const $chart = (chart.current = echarts.init(divRef.current, theme));
      return () => {
        $chart.dispose();
        chart.current = null;
      };
    }
  }, [theme]);

  useEffect(() => {
    const instance = chart.current;
    if (onClick && instance) {
      const handler = function (event: any) {
        const { seriesName, data: eData } = event || {};
        const context =
          eData?.raw?.find((r: DataRecord) => r[seriesBy] === seriesName) ??
          eData?.raw?.[0];
        onClick(context?._original ?? context);
      };
      instance.on("click", handler);
      return () => {
        if (!instance.isDisposed()) {
          instance.off("click", handler);
        }
      };
    }
  }, [seriesBy, onClick]);

  useEffect(() => {
    if (chart.current && width && height) {
      chart.current.resize({
        height,
        width,
      });
    }
  }, [height, width]);

  useEffect(() => {
    if (chart.current) {
      const $options = { ...options } as echarts.EChartsCoreOption;
      if (isRTL) {
        if ($options.yAxis) {
          $options.yAxis = {
            ...$options.yAxis,
            position: "right",
          };
        }
      }
      if ($options.legend && !Array.isArray($options.legend)) {
        $options.legend = {
          ...$options.legend,
          padding: [8, 32, 8, 32],
        };
      }

      const legendOption =
        $options.legend === undefined
          ? !legend
            ? { show: false }
            : undefined
          : Array.isArray($options.legend)
            ? $options.legend.map((item) => ({
                ...item,
                show: legend,
              }))
            : {
                ...$options.legend,
                show: legend,
              };

      chart.current.setOption(
        {
          ...$options,
          ...(legendOption !== undefined && { legend: legendOption }),
          color: getColor(type, data.config?.colors, data.config?.shades),
        },
        {
          // Full replacement avoids stale branches when options shrink.
          notMerge: true,
        },
      );
    }
  }, [isRTL, type, legend, options, data.config]);

  return <div className={classes.echarts} ref={divRef} />;
}
