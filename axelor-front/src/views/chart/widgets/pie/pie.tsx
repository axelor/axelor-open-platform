import { produce } from "immer";
import { useState, useEffect } from "react";

import { ECharts, ChartProps } from "../../builder";
import { PlusData } from "../../builder/utils";

const defaultOption = {
  tooltip: {},
  legend: {
    type: "scroll",
    orient: "horizontal",
    bottom: 5,
    data: [],
  },
  series: [
    {
      type: "pie",
      radius: "75%",
      center: ["50%", "50%"],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: "rgba(0, 0, 0, 0.5)",
        },
      },
      data: [],
    },
  ],
};

export function Pie({ data, ...rest }: ChartProps) {
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    setOptions(
      produce((draft: any) => {
        draft.legend.data = dimensions;
        draft.series[0].data = source.map(({ x, y, ...rest }) => ({
          ...rest,
          name: x,
          value: y,
        }));
        draft.tooltip.valueFormatter = formatter;
      })
    );
  }, [data]);

  return <ECharts options={options} {...(rest as any)} />;
}
