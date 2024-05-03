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
    const { axisScale = "distinct", max } = data.config || {};

    setOptions(
      produce((draft: any) => {
        draft.tooltip.valueFormatter = formatter;
        draft.legend.data = [];
        draft.radar.indicator = [];
        draft.series[0].data = [];

        let maxValue = 0;

        source.forEach(({ x, y, raw, ...values }) => {
          draft.legend.data.push(x);

          for (const [k, v] of Object.entries(values)) {
            const found = draft.radar.indicator.find(
              (i: { name: string; max: number }) => i.name === k,
            );

            maxValue = Math.max(maxValue, v as number);

            if (!found) {
              draft.radar.indicator.push({
                name: k,
                ...(axisScale === "fixed" &&
                  max && {
                    max: +max,
                  }),
              });
            }
          }
        });

        if (axisScale === "unique") {
          draft.radar.indicator.forEach(
            (ind: { name: string; max: number }) => {
              ind.max = maxValue;
            },
          );
        }
        
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
