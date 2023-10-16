import _ from "lodash";

import { DataContext } from "@/services/client/data.types";
import { l10n, moment } from "@/services/client/l10n";
import { Field, JsonField } from "@/services/client/meta.types";

import { toKebabCase } from "./names";

export type FormatOptions = {
  props?: Field;
  context?: DataContext;
};

export type Formatter = (value: any, opts?: FormatOptions) => string;

export function getTimeFormat({ props }: FormatOptions = {}) {
  const seconds = props?.seconds || props?.widgetAttrs?.seconds;
  let format = "HH:mm";
  if (seconds) {
    format += ":ss";
  }
  return format;
}

export function getDateFormat(opts: FormatOptions = {}) {
  return l10n.getDateFormat();
}

export function getDateTimeFormat(opts: FormatOptions = {}) {
  const dateFormat = getDateFormat(opts);
  return dateFormat + " " + getTimeFormat(opts);
}

export function getJSON(jsonStr: string) {
  let value = {};
  try {
    value = JSON.parse(jsonStr);
  } catch (e) {
    // Ignore
  }
  return value;
}

const formatDuration: Formatter = (value, opts = {}) => {
  if ((!value && value !== 0) || isNaN(value)) {
    return value;
  }

  value = Number(value);

  const props = opts?.props;

  let h = "" + Math.floor(value / 3600);
  let m = "" + Math.floor((value % 3600) / 60);
  let s = "" + Math.floor((value % 3600) % 60);

  h = _.padStart(h, props?.big ? 3 : 2, "0");
  m = _.padStart(m, 2, "0");
  s = _.padStart(s, 2, "0");

  let text = h + ":" + m;

  if (props?.seconds || props?.widgetAttrs?.seconds) {
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

function addCurrency(value: string, symbol: string) {
  if (value && symbol) {
    const lang = l10n.getLocale().split(/-|_/)[0];
    if (lang === "fr") {
      return value.endsWith(symbol) ? value : value + " " + symbol;
    }
    return value.startsWith(symbol) ? value : symbol + " " + value;
  }
  return value;
}

export const DEFAULT_SCALE = 2;

const MIN_SCALE = 0;
const MAX_SCALE = 20;

export function limitScale(value?: number) {
  if (value == null) {
    return value;
  }

  if (value < MIN_SCALE) {
    return MIN_SCALE;
  }

  if (value > MAX_SCALE) {
    return MAX_SCALE;
  }

  return value;
}

const formatNumber: Formatter = (value, opts = {}) => {
  const { props, context = {} } = opts;
  let { scale, currency, serverType, type } = props ?? {};

  if (
    (value === null || value === undefined) &&
    props?.defaultValue === undefined
  ) {
    return value;
  }

  if ((serverType ?? type)?.toUpperCase() === "DECIMAL") {
    // referencing another field in the context?
    if (typeof scale === "string") {
      scale = +((_.get(context, scale) as number) ?? scale);
    }

    if (typeof scale !== "number" || isNaN(scale)) {
      scale = DEFAULT_SCALE;
    }
  } else {
    scale = 0;
  }

  // referencing another field in the context?
  if (currency) {
    currency = (_.get(context, currency) as string) ?? currency;
  }

  let num = +value;
  if (num === 0 || num) {
    const opts: Intl.NumberFormatOptions = {};
    opts.minimumFractionDigits = scale;
    opts.maximumFractionDigits = scale;
    if (currency) {
      opts.style = "currency";
      opts.currency = currency;
    }
    try {
      return l10n.formatNumber(num, opts);
    } catch (e) {
      // Fall back to adding currency symbol
      if (currency) {
        const result = l10n.formatNumber(num, {
          minimumFractionDigits: opts.minimumFractionDigits,
          maximumFractionDigits: opts.maximumFractionDigits,
        });
        return addCurrency(result, currency);
      }
      throw e;
    }
  }
  if (typeof value === "string" && currency) {
    return addCurrency(value, currency);
  }
  return value;
};

const formatInteger: Formatter = (value, opts = {}) => {
  const { props: { scale, ...props } = {} as Field } = opts;
  return formatNumber(value, { ...opts, props });
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
      opts.minimumFractionDigits = +scale || 2;
      opts.maximumFractionDigits = +scale || 2;
    }
    return l10n.formatNumber(num, opts);
  }

  return value;
};

const formatBoolean: Formatter = (value, opts = {}) => {
  return Boolean(value).toString();
};

const formatSelection: Formatter = (value, opts = {}) => {
  const { props: { selectionList } = {} } = opts;
  const item = (selectionList ?? []).find(
    (x) => String(x.value) === String(value),
  );
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
  integer: formatInteger,
  long: formatInteger,
  decimal: formatNumber,
  percent: formatPercent,
  boolean: formatBoolean,
  date: formatDate,
  time: formatTime,
  datetime: formatDateTime,
  duration: formatDuration,
  selection: formatSelection,
  enumeration: formatSelection,
  "one-to-one": formatOne,
  "one-to-many": formatMany,
  "many-to-one": formatOne,
  "many-to-many": formatMany,
};

const format: Formatter = (value, opts = {}) => {
  let { props, context } = opts;
  let type = props?.serverType ?? props?.type;
  if (type) type = toKebabCase(type);

  if (type === "enum") type = "enumeration";
  if (props?.widget === "duration") type = "duration";
  if (props?.selection) type = "selection";

  let val = value;
  let name = props?.name;

  if (context && val === undefined) {
    if (props && (props as JsonField).jsonField) {
      const { jsonField, jsonPath } = props as JsonField;
      val = _.get(
        getJSON(_.get(context, jsonField as string)),
        jsonPath as string,
      );
    } else if (name?.includes(".") && value === undefined) {
      val = _.get(context, name);
    }
  }

  let func = Formatters[type as keyof typeof Formatters];
  if (func) {
    return func(val, opts);
  }

  return val ?? "";
};

export default format;
