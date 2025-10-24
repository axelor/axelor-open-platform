import { useState, useEffect, useMemo } from "react";
import { produce } from "immer";

import { ChartGroupType, ChartProps, ECharts } from "../../builder";
import { PlusData, applyTitles, useIsDiscrete } from "../../builder/utils";
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

export function Bar(props: ChartProps) {
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
        const rotateLabels = source.length > 6;
        applyTitles(draft, data, {
          xAxis: { nameGap: rotateLabels ? 75 : 25 },
        });
        draft.series = dimensions.map((key) => ({
          type: "bar",
          stack: isDiscrete || type === "stack" ? "all" : `${key}`,
          ...(isDiscrete && {
            label: {
              show: true,
              position: "top",
              formatter: (params: any) =>
                formatter(
                  params.value[params.dimensionNames[params.encode.y[0]]],
                ),
            },
            labelLayout: {
              hideOverlap: true,
            },
          }),
        }));
        draft.tooltip.valueFormatter = formatter;
        draft.dataset.dimensions = ["x", ...dimensions];
        draft.dataset.source = source;

        if (rotateLabels) {
          draft.xAxis.axisLabel = {
            ...draft.xAxis.axisLabel,
            rotate: 30,
            interval: 0,
            overflow: "truncate",
            width: 110,
          };
          draft.grid = {
            ...draft.grid,
            bottom: 120,
          };
        } else {
          draft.xAxis.axisLabel = {
            interval: 0,
          };
          draft.grid = {};
        }
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
