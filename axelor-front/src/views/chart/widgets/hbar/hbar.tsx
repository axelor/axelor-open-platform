import { produce } from "immer"
import { useEffect, useState } from "react";

import { ChartGroupType, ChartProps, ECharts } from "../../builder";
import { PlusData, applyTitles } from "../../builder/utils";
import { BarGroup } from "../bar/bar-group";

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
  xAxis: { axisLine: { show: true } },
  yAxis: { type: "category" },
  series: [],
};

export function Hbar({ data, ...rest }: ChartProps) {
  const [type, setType] = useState<ChartGroupType>(
    data.stacked ? "stack" : "group"
  );
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    setOptions(
      produce((draft: any) => {
        applyTitles(draft, data);
        draft.series = dimensions.map((key) => ({
          type: "bar",
          stack: type === "stack" ? "all" : `${key}`,
        }));
        draft.dataset.dimensions = ["x", ...dimensions];
        draft.dataset.source = source;
        draft.tooltip.valueFormatter = formatter;
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
