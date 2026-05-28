import { useState, useMemo } from "react";
import { produce } from "immer";

import { ChartGroupType, ChartProps, ECharts } from "../../builder";
import {
  PlusData,
  applyTitles,
  getDataZoom,
  useIsDiscrete,
} from "../../builder/utils";
import { BarGroup } from "./bar-group";

const defaultOption = {
  legend: {
    bottom: 5,
    left: "center",
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

  const options = useMemo(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    return produce(defaultOption, (draft: any) => {
      const rotateLabels = source.length > 6;
      applyTitles(draft, data, {
        xAxis: { nameGap: rotateLabels ? 55 : 25 },
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
      draft.grid = { ...draft.grid, top: 45 };

      const configZoom = Number(data.config?.zoomThreshold);
      const dataZoom = getDataZoom(
        source.length,
        "xAxis",
        Number.isFinite(configZoom) ? configZoom : undefined,
      );

      if (dataZoom) {
        // With dataZoom, use compact labels since users can zoom in
        draft.xAxis.axisLabel = {
          rotate: 45,
          interval: 0,
          overflow: "truncate",
          width: 60,
          tooltip: { show: true },
        };
        draft.grid = { ...draft.grid, bottom: 130 };
        draft.dataZoom = dataZoom;
        draft.legend.bottom = 30;
        draft.xAxis.nameGap = 55;
      } else if (rotateLabels) {
        draft.xAxis.axisLabel = {
          ...draft.xAxis.axisLabel,
          rotate: 30,
          interval: 0,
          overflow: "truncate",
          width: 75,
          tooltip: { show: true },
        };
        draft.grid = { ...draft.grid, bottom: 105 };
      } else {
        draft.xAxis.axisLabel = { interval: 0 };
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
