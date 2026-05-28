import type * as echarts from "echarts/core";
import { ChartView } from "@/services/client/meta.types";

export type ChartDataRecord = {
  [key: string]: any;
};

export type ChartProps = {
  data: ChartView & {
    scale?: number;
    dataset: ChartDataRecord[];
  };
  height?: number;
  width?: number;
  type?: string;
  legend?: boolean;
  loading?: boolean;
  onClick?: (record?: ChartDataRecord) => Promise<any>;
  onChartReady?: (instance: echarts.ECharts | null) => void;
};

export type ChartType =
  | "bar"
  | "hbar"
  | "line"
  | "area"
  | "pie"
  | "donut"
  | "funnel"
  | "gauge"
  | "text"
  | "radar";

export type ChartGroupType = "stack" | "group";
