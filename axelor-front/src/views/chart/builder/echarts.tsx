import { useEffect, useRef } from "react";
import * as echarts from "echarts";

import { ChartProps, ChartType } from "./types";
import { getColor } from "./utils";
import classes from "./echarts.module.scss";

export function ECharts({
  type,
  height,
  width,
  options,
  legend = true,
  isMerge = false,
  lazyUpdate = false,
  onClick,
}: Pick<ChartProps, "legend" | "onClick"> & {
  type: ChartType;
  height: number | string;
  width: number | string;
  options: Partial<echarts.EChartsOption>;
  isMerge: boolean;
  lazyUpdate: boolean;
}) {
  const divRef = useRef<HTMLDivElement>(null);
  const chart = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (divRef.current !== null) {
      const $chart = (chart.current = echarts.init(divRef.current));
      return () => $chart.dispose();
    }
  }, []);

  useEffect(() => {
    const instance = chart.current;
    if (onClick && instance) {
      instance.on("click", function (event: any) {
        const context = event?.data?.raw?.[0];
        onClick(context);
        // TODO: call action
      });
    }
  }, [onClick]);

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
      chart.current.setOption(
        {
          ...options,
          ...(!legend && {
            legend: undefined,
          }),
          color: getColor(type),
        } as echarts.EChartOption,
        !isMerge,
        lazyUpdate
      );
    }
  }, [type, legend, options, isMerge, lazyUpdate]);

  return <div className={classes.echarts} ref={divRef} />;
}
