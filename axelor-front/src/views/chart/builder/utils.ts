import _, { isString, isNumber, sortBy, each, map, difference } from "lodash";

import { moment } from "@/services/client/l10n";
import { ChartDataRecord, ChartType } from "./types";
import { Formatters } from "@/utils/format";
import { ChartView, Field } from "@/services/client/meta.types";

const ChartColors = [
  [
    "#EEF2FF",
    "#E0E7FF",
    "#C7D2FE",
    "#A5B4FC",
    "#818CF8",
    "#6366F1",
    "#4F46E5",
    "#4338CA",
    "#3730A3",
    "#312E81",
  ],
  [
    "#FFF1F2",
    "#FFE4E6",
    "#FECDD3",
    "#FDA4AF",
    "#FB7185",
    "#F43F5E",
    "#E11D48",
    "#BE123C",
    "#9F1239",
    "#881337",
  ],
  [
    "#ECFEFF",
    "#CFFAFE",
    "#A5F3FC",
    "#67E8F9",
    "#22D3EE",
    "#06B6D4",
    "#0891B2",
    "#0E7490",
    "#155E75",
    "#164E63",
  ],
  [
    "#F0FDFA",
    "#CCFBF1",
    "#99F6E4",
    "#5EEAD4",
    "#2DD4BF",
    "#14B8A6",
    "#0D9488",
    "#0F766E",
    "#115E59",
    "#134E4A",
  ],
  [
    "#F0FDF4",
    "#DCFCE7",
    "#BBF7D0",
    "#86EFAC",
    "#4ADE80",
    "#22C55E",
    "#16A34A",
    "#15803D",
    "#166534",
    "#14532D",
  ],
  [
    "#FEFCE8",
    "#FEF9C3",
    "#FEF08A",
    "#FDE047",
    "#FACC15",
    "#EAB308",
    "#CA8A04",
    "#A16207",
    "#854D0E",
    "#713F12",
  ],
  [
    "#FDF4FF",
    "#FAE8FF",
    "#F5D0FE",
    "#F0ABFC",
    "#E879F9",
    "#D946EF",
    "#C026D3",
    "#A21CAF",
    "#86198F",
    "#701A75",
  ],
  [
    "#FFF7ED",
    "#FFEDD5",
    "#FED7AA",
    "#FDBA74",
    "#FB923C",
    "#F97316",
    "#EA580C",
    "#C2410C",
    "#9A3412",
    "#7C2D12",
  ],
  [
    "#FAFAF9",
    "#F5F5F4",
    "#E7E5E4",
    "#D6D3D1",
    "#A8A29E",
    "#78716C",
    "#57534E",
    "#44403C",
    "#292524",
    "#1C1917",
  ],
  [
    "#FEF2F2",
    "#FEE2E2",
    "#FECACA",
    "#FCA5A5",
    "#F87171",
    "#EF4444",
    "#DC2626",
    "#B91C1C",
    "#991B1B",
    "#7F1D1D",
  ],
  [
    "#FFFBEB",
    "#FEF3C7",
    "#FDE68A",
    "#FCD34D",
    "#FBBF24",
    "#F59E0B",
    "#D97706",
    "#B45309",
    "#92400E",
    "#78350F",
  ],
  [
    "#F7FEE7",
    "#ECFCCB",
    "#D9F99D",
    "#BEF264",
    "#A3E635",
    "#84CC16",
    "#65A30D",
    "#4D7C0F",
    "#3F6212",
    "#365314",
  ],
  [
    "#ECFDF5",
    "#D1FAE5",
    "#A7F3D0",
    "#6EE7B7",
    "#34D399",
    "#10B981",
    "#059669",
    "#047857",
    "#065F46",
    "#064E3B",
  ],
  [
    "#F0F9FF",
    "#E0F2FE",
    "#BAE6FD",
    "#7DD3FC",
    "#38BDF8",
    "#0EA5E9",
    "#0284C7",
    "#0369A1",
    "#075985",
    "#0C4A6E",
  ],
  [
    "#EFF6FF",
    "#DBEAFE",
    "#BFDBFE",
    "#93C5FD",
    "#60A5FA",
    "#3B82F6",
    "#2563EB",
    "#1D4ED8",
    "#1E40AF",
    "#1E3A8A",
  ],
  [
    "#F5F3FF",
    "#EDE9FE",
    "#DDD6FE",
    "#C4B5FD",
    "#A78BFA",
    "#8B5CF6",
    "#7C3AED",
    "#6D28D9",
    "#5B21B6",
    "#4C1D95",
  ],
  [
    "#FAF5FF",
    "#F3E8FF",
    "#E9D5FF",
    "#D8B4FE",
    "#C084FC",
    "#A855F7",
    "#9333EA",
    "#7E22CE",
    "#6B21A8",
    "#581C87",
  ],
  [
    "#FDF2F8",
    "#FCE7F3",
    "#FBCFE8",
    "#F9A8D4",
    "#F472B6",
    "#EC4899",
    "#DB2777",
    "#BE185D",
    "#9D174D",
    "#831843",
  ],
  [
    "#F8FAFC",
    "#F1F5F9",
    "#E2E8F0",
    "#CBD5E1",
    "#94A3B8",
    "#64748B",
    "#475569",
    "#334155",
    "#1E293B",
    "#0F172A",
  ],
  [
    "#F9FAFB",
    "#F3F4F6",
    "#E5E7EB",
    "#D1D5DB",
    "#9CA3AF",
    "#6B7280",
    "#4B5563",
    "#374151",
    "#1F2937",
    "#111827",
  ],
  [
    "#FAFAFA",
    "#F4F4F5",
    "#E4E4E7",
    "#D4D4D8",
    "#A1A1AA",
    "#71717A",
    "#52525B",
    "#3F3F46",
    "#27272A",
    "#18181B",
  ],
  [
    "#FAFAFA",
    "#F5F5F5",
    "#E5E5E5",
    "#D4D4D4",
    "#A3A3A3",
    "#737373",
    "#525252",
    "#404040",
    "#262626",
    "#171717",
  ],
];

export function getColor(type: ChartType) {
  if (["hbar", "bar", "line", "area"].includes(type)) {
    return ChartColors.reduce(
      ($colors, set) => $colors.concat([set[5], set[4]]),
      []
    );
  }
  return ChartColors.map((set) => set[5]);
}

const FIELD_FORMATTERS: Record<string, (d: any, config?: any) => any> = {
  date: function (d: any, config: any) {
    const f = config.xFormat || "DD/MM/YYYY";
    return moment(d).format(f);
  },
  month: function (d: any, config: any) {
    var v = "" + d;
    var f = config.xFormat;

    if (v.indexOf(".") > -1) return "";
    if (isString(d) && /^(\d+)$/.test(d)) {
      d = parseInt(d);
    }
    if (isNumber(d)) {
      return moment(`${moment().year()}-${d}-1`).format(f || "MMM");
    }
    if (isString(d) && d.indexOf("-") > 0) {
      return moment(d).format(f || "MMM, YYYY");
    }
    return d;
  },
  year: function (d: any) {
    return moment(`${moment().year()}-${d}-1`).format("YYYY");
  },
  number: (d: any) => Math.round(parseInt(d)),
  decimal: (d: any, config: any) => Formatters.decimal(d, { props: config }),
  text: (d: any) => d,
};

function isInteger(n: number) {
  return (n ^ 0) === n;
}

function hasIntegerValues(dataset: any, series: any) {
  const [record] = dataset || [];
  return series && record && isInteger(record[series.key]);
}

function $conv(value: any, type?: string) {
  if (!value && type === "text") return "N/A";
  if (!value) return 0;
  if (isNumber(value)) return value;
  if (/^(-)?\d+(\.\d+)?$/.test(value)) {
    return +value;
  }
  return value;
}

function getDataset({
  dataset,
  xAxis,
  series: [{ groupBy } = { groupBy: "" }],
}: any) {
  return dataset.map((data: any) => {
    if (xAxis && data[xAxis] === null) {
      data[xAxis] = "N/A";
    }
    if (groupBy && data[groupBy] === null) {
      data[groupBy] = "N/A";
    }
    return data;
  });
}

export function getScale(data: ChartView, dataset: ChartDataRecord[]) {
  const [series] = data.series || [];
  let scale: number = series?.scale as number;

  if (!isInteger(scale)) {
    scale = hasIntegerValues(dataset, series) ? 0 : 2;
  }

  return scale;
}

export function applyTitles(draft: any, data: any) {
  if (data.xTitle && draft.xAxis) {
    draft.xAxis.name = data.xTitle;
    draft.xAxis.nameLocation = "middle";
    draft.xAxis.nameGap = 25;
  }
  const [series] = data.series || [];
  if (series && series.title) {
    draft.yAxis.name = series.title;
    draft.yAxis.nameLocation = "middle";
    draft.yAxis.nameGap = 50;
  }
}

export function PlusData(data: any) {
  const {
    xAxis,
    series: [series],
    scale,
  } = data;
  const dataset = getDataset(data);
  const types = _.chain(dataset)
    .map(series.groupBy || xAxis)
    .uniq()
    .value();

  const result = _.chain(dataset)
    .groupBy(xAxis)
    .map((group, name) => {
      let value = 0;
      each(group, (item) => {
        value += $conv(item[series.key]);
      });
      if (!series.groupBy && xAxis) series.groupBy = xAxis;
      const groupBars = series.groupBy
        ? group.reduce(
            (attrs, rec) => ({
              ...attrs,
              [rec[series.groupBy]]: isNaN(rec[series.key])
                ? rec[series.key]
                : Number(rec[series.key]),
            }),
            {}
          )
        : {};
      return {
        ...groupBars,
        raw: group,
        x: name,
        y: value.toString(),
      };
    })
    .value();
  return {
    types,
    data: result,
    formatter: (value: any) =>
      Formatters.decimal(value, { props: { scale } as Field }),
  };
}

export function PlotData(data: any) {
  const {
    xAxis,
    series: [series],
    scale,
  } = data;
  const dataset = getDataset(data);
  const types = _.chain(dataset)
    .map(xAxis)
    .uniq()
    .map((v) => $conv(v, data.xType))
    .value();
  const { groupBy } = series;

  const result = _.chain(dataset)
    .groupBy(groupBy)
    .map((group: any, groupName: string) => {
      const name = groupBy ? groupName : null;
      let values = types.map((t) => {
        const item = group.find((x: any) => x[xAxis] === t);
        if (!item) return { x: 0, y: 0 };
        const x = $conv(item[data.xAxis], data.xType) || 0;
        const y = $conv(
          item[series.key] !== undefined ? item[series.key] : name || 0
        );
        return { x, y, item };
      });

      const my = map(values, "x");
      const missing = difference(types, my);

      if (types.length === missing.length) {
        return null;
      }

      values = sortBy(values, "x");

      return {
        key: name || series.title,
        type: series.type,
        values: values,
      } as any;
    })
    .filter((x) => x)
    .value();

  return {
    types,
    data: result,
    formatter: (value: any) =>
      Formatters.decimal(value, { props: { scale } as Field }),
  };
}

export function getChartData(
  view: ChartView,
  records: ChartDataRecord[],
  config: ChartView["config"]
): ChartDataRecord[] {
  if (!view || !(records || []).length) return records;
  const { xType, xAxis } = view;
  if (xType && xAxis && FIELD_FORMATTERS[xType]) {
    return records.map((data) => ({
      ...data,
      [xAxis]: FIELD_FORMATTERS[xType](data[xAxis], config) || data[xAxis],
    }));
  }
  return records;
}

export function prepareTheme(type: ChartType) {
  const style = getComputedStyle(document.body);
  const bg = style.getPropertyValue("--bs-body-bg");
  const color = style.getPropertyValue("--bs-body-color");
  const borderColor = style.getPropertyValue("--bs-border-color");

  const axisValues = {
    axisLine: { show: true, lineStyle: { color } },
    axisTick: { show: true, lineStyle: { color } },
    axisLabel: { show: true, color },
    splitLine: { show: true, lineStyle: { color: borderColor } },
    splitArea: { show: false, areaStyle: { color } },
  };

  return {
    color: getColor(type),
    backgroundColor: bg,
    textStyle: {},
    line: {
      itemStyle: { borderWidth: 1 },
      lineStyle: { width: 2 },
      symbolSize: 4,
      symbol: "circle",
      smooth: false,
    },
    categoryAxis: axisValues,
    valueAxis: axisValues,
    logAxis: axisValues,
    timeAxis: axisValues,
    legend: { textStyle: { color } },
    tooltip: {
      backgroundColor: bg,
      axisPointer: {
        lineStyle: { color, width: "1" },
        crossStyle: { color, width: "1" },
      },
    },
  };
}
