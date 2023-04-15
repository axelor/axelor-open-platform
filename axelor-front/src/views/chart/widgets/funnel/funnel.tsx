import { useState, useEffect } from "react";
import produce from "immer";

import { ChartProps, ECharts } from "../../builder";
import { Formatters } from "@/utils/format";

const defaultOption = {
  legend: {
    bottom: 5,
    type: "scroll",
  },
  tooltip: {
    trigger: "item",
    formatter: "{b} : {c}",
  },
  xAxis: { type: "category" },
  yAxis: {},
  series: [
    {
      name: "",
      type: "funnel",
      left: "10%",
      top: 10,
      bottom: 60,
      width: "80%",
      min: 0,
      max: 100,
      minSize: "20%",
      maxSize: "100%",
      sort: "descending",
      gap: 2,
      label: {
        show: true,
        position: "inside",
      },
      labelLine: {
        length: 10,
        lineStyle: {
          width: 1,
          type: "solid",
        },
      },
      itemStyle: {
        borderColor: "#fff",
        borderWidth: 1,
      },
      emphasis: {
        label: {
          fontSize: 20,
        },
      },
      data: [],
    },
  ],
};

export function Funnel({ data, ...rest }: ChartProps) {
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const {
      xAxis,
      dataset,
      series: [{ key }],
    } = data;

    setOptions(
      produce((draft: any) => {
        const values = dataset.map((x: any) => parseFloat(x[key]));
        draft.series[0].min = Math.min(...values);
        draft.series[0].max = Math.max(...values);
        draft.series[0].data = dataset.map((x: any) => ({
          name: x[xAxis],
          value: x[key],
        }));
        draft.tooltip.valueFormatter = (v: any) =>
          Formatters.decimal(v, { props: data });
      })
    );
  }, [data]);

  return <ECharts options={options} {...(rest as any)} />;
}
