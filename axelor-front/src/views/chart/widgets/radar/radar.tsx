import { useState, useEffect } from "react";
import { produce } from "immer";

import { ChartProps, ECharts } from "../../builder";
import { PlusData } from "../../builder/utils";

const defaultOption = {
  legend: {
    bottom: 0,
    type: "scroll",
    data: [],
  },
  tooltip: {},
  radar: {
    shape: "circle",
    radius: "70%",
    indicator: [],
  },
  series: [
    {
      type: "radar",
      data: [],
    },
  ],
};

export function Radar(props: ChartProps) {
  const { data } = props;
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { data: source, formatter } = PlusData(data);
    setOptions(
      produce((draft: any) => {
        draft.tooltip.valueFormatter = formatter;
        draft.legend.data = [];
        draft.radar.indicator = [];

        source.forEach(({ x, y, raw, ...values }) => {
          draft.legend.data.push(x);

          for (const [k] of Object.entries(values)) {
            const found = draft.radar.indicator.find(
              (i: { name: string; max: number }) => i.name === k,
            );
            if (!found) {
              draft.radar.indicator.push({
                name: k,
              });
            }
          }
        });

        draft.series[0].data = [];
        source.forEach((s) => {
          draft.series[0].data.push({
            name: s.x,
            value: draft.radar.indicator.map(
              (i: { name: string; max: number }) => s[i.name] ?? 0,
            ),
          });
        });
      }),
    );
  }, [data]);

  return <ECharts options={options} {...(props as any)} />;
}
