import { produce } from "immer";
import { useEffect, useState } from "react";

import { ChartGroupType, ChartProps, ECharts } from "../../builder";
import { PlusData, applyTitles, useIsDiscrete } from "../../builder/utils";
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

export function Hbar(props: ChartProps) {
  const { data } = props;
  const isDiscrete = useIsDiscrete(data);
  const [type, setType] = useState<ChartGroupType>(
    data.stacked ? "stack" : "group",
  );
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    setOptions(
      produce((draft: any) => {
        applyTitles(draft, data, {
          yAxis: { nameGap: 85 },
        });
        draft.series = dimensions.map((key) => ({
          type: "bar",
          stack: isDiscrete || type === "stack" ? "all" : `${key}`,
          ...(isDiscrete && {
            label: {
              show: true,
              position: "right",
              formatter: (params: any) =>
                formatter(
                  params.value[params.dimensionNames[params.encode.x[0]]],
                ),
            },
            labelLayout: {
              hideOverlap: true,
            },
          }),
        }));
        draft.yAxis.axisLabel = {
          ...draft.yAxis.axisLabel,
          overflow: "truncate",
          width: 75,
        };
        draft.dataset.dimensions = ["x", ...dimensions];
        draft.dataset.source = source;
        draft.tooltip.valueFormatter = formatter;
      }),
    );
  }, [type, data, isDiscrete]);

  return (
    <>
      {!isDiscrete && <BarGroup value={type} onChange={setType} />}
      <ECharts options={options} {...(props as any)} />
    </>
  );
}
