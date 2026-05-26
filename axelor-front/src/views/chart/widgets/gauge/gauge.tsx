import { produce } from "immer";
import { useMemo } from "react";
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

export function Gauge(props: ChartProps) {
  const { data } = props;

  const options = useMemo(() => {
    const { xAxis, series, dataset, config: { min = 0, max = 100 } = {} } = data;
    const valueKey = series?.[0]?.key ?? xAxis;
    return produce(defaultOption, (draft: any) => {
      draft.series[0].min = min;
      draft.series[0].max = max;
      draft.series[0].data[0].value = dataset[0] ? (dataset[0][valueKey!] ?? 0) : 0;
      draft.series[0].data[0].raw = dataset[0] ? [dataset[0]] : [];
    });
  }, [data]);

  return <ECharts options={options} {...(props as any)} />;
}
