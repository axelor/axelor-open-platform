import { ChartView } from "@/services/client/meta.types";

export type ChartDataRecord = {
  [key: string]: any;
};

export type ChartProps = {
  data: ChartView & {
    scale?: number;
    dataset: ChartDataRecord[];
  };
  type?: string;
  legend?: boolean;
  onClick?: (record?: ChartDataRecord) => Promise<any>;
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
  | "text";

export type ChartGroupType = "stack" | "group";
