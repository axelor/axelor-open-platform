import { produce } from "immer";
import { useMemo, useState } from "react";

import { ChartGroupType, ChartProps, ECharts } from "../../builder";
import {
  PlusData,
  applyTitles,
  getDataZoom,
  useIsDiscrete,
} from "../../builder/utils";
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
  yAxis: { type: "category", inverse: true },
  series: [],
};

export function Hbar(props: ChartProps) {
  const { data } = props;
  const isDiscrete = useIsDiscrete(data);
  const [type, setType] = useState<ChartGroupType>(
    data.stacked ? "stack" : "group",
  );

  const options = useMemo(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    return produce(defaultOption, (draft: any) => {
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
        tooltip: { show: true },
      };
      draft.dataset.dimensions = ["x", ...dimensions];
      draft.dataset.source = source;
      draft.tooltip.valueFormatter = formatter;

      const dataZoom = getDataZoom(source.length, "yAxis");
      if (dataZoom) {
        draft.dataZoom = dataZoom;
        // Slider on the right side — no legend overlap
        draft.grid = { ...draft.grid, right: 80 };
      }
    });
  }, [type, data, isDiscrete]);

  return (
    <>
      {!isDiscrete && <BarGroup value={type} onChange={setType} />}
      <ECharts options={options} {...(props as any)} />
    </>
  );
}
