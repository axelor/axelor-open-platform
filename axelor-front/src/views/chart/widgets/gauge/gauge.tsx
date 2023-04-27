import { produce } from "immer"
import { useState, useEffect } from "react";
import { ChartProps, ECharts } from "../../builder";

const defaultOption = {
  tooltip: {
    trigger: "item",
    formatter: "{c} %",
  },
  series: [
    {
      name: "",
      type: "gauge",
      detail: { formatter: "{value}%" },
      data: [{ value: 0, name: "" }],
    },
  ],
};

export function Gauge({ data, ...rest }: ChartProps) {
  const [options, setOptions] = useState(defaultOption);

  useEffect(() => {
    const { xAxis, dataset, config: { min = 0, max = 100 } = {} } = data;
    setOptions(
      produce((draft: any) => {
        draft.series[0].min = min;
        draft.series[0].max = max;
        draft.series[0].data[0].value = dataset[0] ? dataset[0][xAxis!] : 0;
      })
    );
  }, [data]);

  return <ECharts options={options} {...(rest as any)} />;
}
