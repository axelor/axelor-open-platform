import { produce } from "immer";
import { useEffect, useState } from "react";
import { useTheme } from "@axelor/ui";

import { Field } from "@/services/client/meta.types";
import { Formatters } from "@/utils/format";
import { ChartProps, ECharts } from "../../builder";

const defaultOption = {
  legend: {
    bottom: 5,
    type: "scroll",
  },
  tooltip: {},
  series: [
    {
      name: "",
      type: "funnel",
      top: 10,
      bottom: 60,
      width: "80%",
      min: 0,
      max: 100,
      minSize: "20%",
      maxSize: "100%",
      sort: "descending",
      gap: 2,
      label: {
        show: true,
        position: "inside",
      },
      itemStyle: {
        borderColor: "#fff",
        borderWidth: 1,
      },
      emphasis: {
        label: {
          fontWeight: "bold",
        },
      },
      data: [],
    },
  ],
};

export function Funnel(props: ChartProps) {
  const { data } = props;
  const [options, setOptions] = useState(defaultOption);
  const isRTL = useTheme().dir === "rtl";

  useEffect(() => {
    const { xAxis, dataset, scale, series: [{ key }] = [] } = data;

    setOptions(
      produce((draft: any) => {
        if (key && xAxis) {
          const values = dataset.map((x) => parseFloat(x[key]));
          draft.series[0].min = Math.min(...values);
          draft.series[0].max = Math.max(...values);
          draft.series[0].data = dataset.map((x: any) => ({
            name: x[xAxis],
            value: x[key],
          }));
        }
        draft.series[0][isRTL ? "right" : "left"] = "10%";
        draft.tooltip.valueFormatter = (v: any) =>
          Formatters.decimal(v, {
            props: {
              serverType: "DECIMAL",
              scale,
            } as unknown as Field,
          });
      }),
    );
  }, [isRTL, data]);

  return <ECharts options={options} {...(props as any)} />;
}
