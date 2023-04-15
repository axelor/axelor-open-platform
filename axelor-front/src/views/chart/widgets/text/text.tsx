import first from "lodash/first";

import { i18n } from "@/services/client/i18n";
import { ChartProps } from "../../builder";

export function Text({ data }: ChartProps) {
  var values: any = first(data.dataset) || {};
  var series: any = first(data.series) || {};

  var value = values[series.key];

  var config: any = {
    strong: true,
    shadow: false,
    fontSize: 22,
    ...data?.config,
  };

  if (config.format) {
    value = i18n.get(config.format, value);
  }

  return (
    <svg style={{ height: "249px", width: "100%" }}>
      <text
        x="50%"
        y="50%"
        dy=".3em"
        textAnchor="middle"
        style={{
          ...(config.color ? { color: config.color } : {}),
          ...(config.fontSize ? { fontSize: config.fontSize } : {}),
          ...(config.shadow
            ? { textShadow: "0 1px 2px rgba(0, 0, 0, .5)" }
            : {}),
          ...(config.strong ? { fontWeight: "bold" } : {}),
        }}
      >
        {value}
      </text>
    </svg>
  );
}
