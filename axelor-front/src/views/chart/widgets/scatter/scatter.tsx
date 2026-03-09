import { produce } from "immer";
import { useMemo } from "react";

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

export function Scatter({ data, ...rest }: ChartProps) {
  const options = useMemo(() => {
    const { data: series, types, formatter } = PlotData(data);
    return produce(defaultOption, (draft: any) => {
      applyTitles(draft, data);
      draft.series = series.map((line: any) => ({
        name: line.key,
        type: "scatter",
        data: line.values.map(({ y }: any) => y),
      }));
      draft.xAxis.data = types;
      draft.legend.data = series.map((x: any) => x.key).filter((x) => x);
      draft.tooltip.valueFormatter = formatter;
    });
  }, [data]);

  return <ECharts options={options} data={data} {...(rest as any)} />;
}
