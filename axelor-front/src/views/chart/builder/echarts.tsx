import { useEffect, useMemo, useRef } from "react";
import { useTheme } from "@axelor/ui";
import * as echarts from "echarts";

import { ChartProps, ChartType } from "./types";
import { getColor, prepareTheme } from "./utils";
import { useAppTheme } from "@/hooks/use-app-theme";
import { DataRecord } from "@/services/client/data.types";
import classes from "./echarts.module.scss";

export function ECharts({
  data,
  type,
  height,
  width,
  options,
  legend = true,
  isMerge = false,
  lazyUpdate = false,
  onClick,
}: Pick<ChartProps, "data" | "legend" | "onClick"> & {
  type: ChartType;
  height: number | string;
  width: number | string;
  options: Partial<echarts.EChartsOption>;
  isMerge: boolean;
  lazyUpdate: boolean;
}) {
  const divRef = useRef<HTMLDivElement>(null);
  const chart = useRef<echarts.ECharts | null>(null);
  const theme = useAppTheme();
  const isRTL = useTheme().dir === "rtl";

  const seriesBy = useMemo(() => {
    const { series, xAxis } = data;
    const { groupBy } = series?.[0] ?? {};
    return groupBy || xAxis || "";
  }, [data]);

  useEffect(() => {
    echarts.registerTheme(theme, prepareTheme(type));
  }, [type, theme]);

  useEffect(() => {
    if (divRef.current !== null) {
      const $chart = (chart.current = echarts.init(divRef.current, theme));
      return () => $chart.dispose();
    }
  }, [theme]);

  useEffect(() => {
    const instance = chart.current;
    if (onClick && instance) {
      instance.on("click", function (event: any) {
        const { seriesName, data } = event || {};
        const context =
          data?.raw?.find((r: DataRecord) => r[seriesBy] === seriesName) ??
          data?.raw?.[0];
        onClick(context?._original ?? context);
      });
    }
  }, [seriesBy, onClick]);

  useEffect(() => {
    chart.current &&
      width &&
      height &&
      chart.current.resize({
        height,
        width,
      });
  }, [height, width]);

  useEffect(() => {
    if (chart.current) {
      const $options = { ...options };
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
      chart.current.setOption(
        {
          ...$options,
          ...(!legend && {
            legend: undefined,
          }),
          color: getColor(type),
        } as echarts.EChartOption,
        !isMerge,
        lazyUpdate,
      );
    }
  }, [isRTL, type, legend, options, isMerge, lazyUpdate]);

  return <div className={classes.echarts} ref={divRef} />;
}
