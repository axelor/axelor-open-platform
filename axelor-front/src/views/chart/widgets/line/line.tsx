import { produce } from "immer";
import { useState, useEffect } from "react";

import { Formatters } from "@/utils/format";
import { PlotData, applyTitles } from "../../builder/utils";
import { ChartProps, ECharts } from "../../builder";

const defaultOption = {
  legend: {
    bottom: 5,
    type: "scroll",
  },
  tooltip: {
    trigger: "axis",
  },
  xAxis: { type: "category", boundaryGap: false },
  yAxis: {
    type: "value",
  },
  series: [],
};

export function Line({ data, type, ...rest }: ChartProps) {
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { data: series, types, formatter } = PlotData(data);
    setOptions(
      produce((draft: any) => {
        const xType = ["date", "time"].includes(data?.xType ?? "")
          ? "time"
          : "category";
        applyTitles(draft, data);
        draft.series = series.map((line: any) => ({
          name: line.key,
          type: "line",
          data: line.values.map(
            xType === "time"
              ? ({ sort, y }: any) => [sort, y]
              : ({ y }: any) => y,
          ),
          ...(type === "area" ? { areaStyle: {} } : {}),
        }));
        draft.xAxis.type = xType;
        draft.xAxis.data = types;
        draft.legend.data = series.map((x: any) => x.key).filter((x) => x);
        draft.tooltip.valueFormatter = formatter;
        if (xType === "time") {
          draft.tooltip = {
            ...draft.tooltip,
            axisPointer: {
              label: {
                formatter: (params: any) => {
                  const labelFormatter =
                    params.seriesData?.[0].value?.[0]?.includes("T")
                      ? Formatters.datetime
                      : Formatters.date;
                  return labelFormatter(params.value);
                },
              },
            },
          };
        } else {
          draft.tooltip = {
            ...draft.tooltip,
            axisPointer: {},
          };
        }
      }),
    );
  }, [type, data]);

  return <ECharts options={options} data={data} {...(rest as any)} />;
}
