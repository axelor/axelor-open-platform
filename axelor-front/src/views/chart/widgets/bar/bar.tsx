import { useState, useEffect } from "react";
import { produce } from "immer"

import { ChartGroupType, ChartProps, ECharts } from "../../builder";
import { PlusData, applyTitles } from "../../builder/utils";
import { BarGroup } from "./bar-group";

const defaultOption = {
  legend: {
    bottom: 5,
    type: "scroll",
  },
  tooltip: {},
  dataset: {
    dimensions: [],
    source: [],
  },
  xAxis: { type: "category" },
  yAxis: { axisLine: { show: true } },
  series: [],
};

export function Bar({ data, ...rest }: ChartProps) {
  const [type, setType] = useState<ChartGroupType>(
    data.stacked ? "stack" : "group"
  );
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    const isVertical = (source || []).some((s) => s.raw && s.raw.length > 1);
    setOptions(
      produce((draft: any) => {
        applyTitles(draft, data);
        draft.series = dimensions.map((key) => ({
          type: "bar",
          stack: type === "stack" ? "all" : `${key}`,
          ...(isVertical && type === "stack"
            ? {}
            : {
                label: {
                  show: true,
                  position: "top",
                  formatter: (v: any) => formatter(v.data[v.seriesName]),
                },
              }),
        }));
        draft.tooltip.valueFormatter = formatter;
        draft.dataset.dimensions = ["x", ...dimensions];
        draft.dataset.source = source;
      })
    );
  }, [type, data]);

  return (
    <>
      <BarGroup value={type} onChange={setType} />
      <ECharts options={options} {...(rest as any)} />
    </>
  );
}
