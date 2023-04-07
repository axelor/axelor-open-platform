import moment from "dayjs";
import _ from "lodash";

import { DataContext } from "@/services/client/data.types";
import { l10n } from "@/services/client/l10n";
import { Field, JsonField } from "@/services/client/meta.types";

import { toKebabCase } from "./names";

export type FormatOptions = {
  props?: Field;
  context?: DataContext;
};

export type Formatter = (value: any, opts?: FormatOptions) => string;

export function getTimeFormat(opts: FormatOptions = {}) {
  let props = opts?.props;
  let format = "HH:mm";
  if (props?.seconds) {
    format += ":ss";
  }
  return format;
}

export function getDateTimeFormat(opts: FormatOptions = {}) {
  const dateFormat = l10n.getDateFormat();
  return dateFormat + " " + getTimeFormat(opts);
}

function getJSON(jsonStr: string) {
  let value = {};
  try {
    value = JSON.parse(jsonStr);
  } finally {
    return value;
  }
}

const formatDuration: Formatter = (value, opts = {}) => {
  if (!value || !_.isNumber(value)) {
    return value;
  }

  const props = opts?.props;

  let h = "" + Math.floor(value / 3600);
  let m = "" + Math.floor((value % 3600) / 60);
  let s = "" + Math.floor((value % 3600) % 60);

  h = _.pad(h, props?.big ? 3 : 2, "0");
  m = _.pad(m, 2, "0");
  s = _.pad(s, 2, "0");

  var text = h + ":" + m;

  if (props?.seconds) {
    text = text + ":" + s;
  }

  return text;
};

const formatDate: Formatter = (value, opts = {}) => {
  const format = l10n.getDateFormat();
  return value ? moment(value).format(format) : "";
};

const formatTime: Formatter = (value, opts = {}) => {
  const format = getTimeFormat(opts);
  return value
    ? moment(value, [
        "YYYY-MM-DDTHH:mm:ss",
        "YYYY-MM-DDTHH:mm:ssZ[Z]",
        "HH:mm:ss",
        "HH:mm",
      ]).format(format)
    : "";
};

const formatDateTime: Formatter = (value, opts = {}) => {
  const format = getDateTimeFormat(opts);
  return value ? moment(value).format(format) : "";
};

const formatString: Formatter = (value, opts = {}) => {
  const { props, context } = opts;
  if (props?.translatable && context) {
    const key = `$t:${props.name}`;
    return context[key] ?? value;
  }
  return value;
};

const formatNumber: Formatter = (value, opts = {}) => {
  const { props, context = {} } = opts;
  let { scale, currency } = props ?? {};

  if (
    (value === null || value === undefined) &&
    props?.defaultValue === undefined
  ) {
    return value;
  }

  // referencing another field in the context?
  if (typeof scale === "string") {
    scale = (_.get(context, scale) as number) ?? scale;
  }

  // referencing another field in the context?
  if (currency) {
    currency = (_.get(context, currency) as string) ?? currency;
  }

  let num = +value;
  if (num === 0 || num) {
    const opts: Intl.NumberFormatOptions = {};
    if (scale || currency) {
      opts.minimumFractionDigits = +(scale ?? 2);
      opts.maximumFractionDigits = +(scale ?? 2);
    }
    if (currency) {
      opts.style = "currency";
      opts.currency = currency;
    }
    return l10n.formatNumber(num, opts);
  }
  return value;
};

const formatPercent: Formatter = (value, opts = {}) => {
  const { props, context = {} } = opts;
  let { scale } = props ?? {};

  // referencing another field in the context?
  if (typeof scale === "string") {
    scale = (_.get(context, scale) as number) ?? scale;
  }

  let num = +value;
  if (num === 0 || num) {
    const opts: Intl.NumberFormatOptions = { style: "percent" };
    if (scale) {
      opts.minimumFractionDigits = +scale;
      opts.maximumFractionDigits = +scale;
    }
    return l10n.formatNumber(num, opts);
  }

  return value;
};

const formatBoolean: Formatter = (value, opts = {}) => {
  return Boolean(value).toString();
};

const formatSelection: Formatter = (value, opts = {}) => {
  const { props: { selectionList = [] } = {} } = opts;
  const item = selectionList.find((x) => String(x.value) === String(value));
  return item?.title ?? "";
};

const formatOne: Formatter = (value, opts = {}) => {
  const { props } = opts;

  if (!value) return "";
  if (!props?.targetName) return value.name ?? value.code ?? value.id ?? "";

  const name = props.targetName;
  const key = `$t:${name}`;

  return value[key] ?? value[name];
};

const formatMany: Formatter = (value, opts = {}) => {
  return value ? `(${value.length})` : "";
};

export const Formatters = {
  string: formatString,
  integer: formatNumber,
  long: formatNumber,
  decimal: formatNumber,
  percent: formatPercent,
  boolean: formatBoolean,
  date: formatDate,
  time: formatTime,
  datetime: formatDateTime,
  duration: formatDuration,
  selection: formatSelection,
  "one-to-one": formatOne,
  "one-to-many": formatMany,
  "many-to-one": formatOne,
  "many-to-many": formatMany,
};

const format: Formatter = (value, opts = {}) => {
  let { props, context = {} } = opts;
  let type = props?.serverType ?? props?.type;
  if (type) type = toKebabCase(type);

  if (type === "enum") type = "enumeration";
  if (props?.selection) type = "selection";

  let val = value;
  let name = props?.name;

  if ((props as JsonField).jsonField) {
    const { jsonField, jsonPath } = props as JsonField;
    val = _.get(
      getJSON(_.get(context, jsonField as string)),
      jsonPath as string
    );
  } else if (name?.includes(".") && value === undefined) {
    val = _.get(context, name);
  }

  let func = Formatters[type as keyof typeof Formatters];
  if (func) {
    return func(val, opts);
  }

  return "";
};

export default format;
