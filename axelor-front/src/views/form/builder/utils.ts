import { uniqueId } from "lodash";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";

import { Attrs, DEFAULT_ATTRS } from "./types";
import { MetaData } from "@/services/client/meta";

export const nextId = (() => {
  let id = 0;
  return () => --id;
})();

export function defaultAttrs(schema: Schema): Attrs {
  const props = {
    ...schema,
    ...schema.widgetAttrs,
  };
  const attrs = Object.entries(props)
    .filter(([name]) => name in DEFAULT_ATTRS)
    .reduce((prev, [name, value]) => ({ ...prev, [name]: value }), {});
  return attrs;
}

export function processActionValue(value: any) {
  function updateNullIdObject(value: any): any {
    if (value && typeof value === "object") {
      if (Array.isArray(value)) {
        return value.map(updateNullIdObject);
      }
      if (value.id === null) {
        return { ...value, id: nextId(), _dirty: true };
      }
    }
    return value;
  }
  return updateNullIdObject(value);
}

export function processContextValues(context: DataContext) {
  function setDummyIdToNull(value: any): any {
    if (value && typeof value === "object") {
      if (Array.isArray(value)) {
        return value.map(setDummyIdToNull);
      }
      if (value.id < 0) {
        return { ...value, id: null };
      }
    }
    return value;
  }

  for (let k in context) {
    context[k] = setDummyIdToNull(context[k]);
  }

  return context;
}

const NUMBER_ATTRS = [
  "width",
  "height",
  "cols",
  "colSpan",
  "rowSpan",
  "itemSpan",
  "gap",
];

function parseNumber(value: unknown) {
  if (typeof value === "string") {
    if (/^(-)?(\d+)$/.test(value)) return parseInt(value);
    if (/^(-)?(\d+(\.\d+)?)$/.test(value)) return parseFloat(value);
  }
  return value;
}

function processAttrs(
  schema: Schema,
  parse: (value: unknown) => unknown,
  ...names: string[]
) {
  for (const name of names) {
    const value = schema[name];
    const val = parse(value);
    if (value !== val) {
      schema[name] = val;
    }
  }
  return schema;
}

export function processView(schema: Schema, fields: Record<string, Property>) {
  const field = fields?.[schema.name!] ?? {};
  const attrs = defaultAttrs(field);

  // merge default attrs
  const res: Schema = { ...attrs, serverType: field?.type, ...schema };

  if (res.type === "field") {
    res.serverType = res.serverType ?? "STRING";
  }
  if (res.type === "panel-related") {
    res.serverType = res.serverType ?? "ONE_TO_MANY";
  }

  let type = res.widget ?? res.type;
  if (type === "field") {
    type = res.serverType;
  }

  if (!res.widget && field.image) {
    type = "image";
  }

  res.uid = uniqueId("w");
  res.widget = toKebabCase(type);

  // process attrs
  processAttrs(res, parseNumber, ...NUMBER_ATTRS);
  processAttrs(res.widgetAttrs ?? {}, parseNumber, ...NUMBER_ATTRS);

  if (
    res.widget !== "panel" &&
    res.widget !== "separator" &&
    res.widget !== "button" && 
    res.widget !== "label"
  ) {
    res.title = res.title ?? res.autoTitle ?? field.title ?? field.autoTitle;
  }

  if (res.showIf || res.hideIf) {
    res.hidden = true;
  }

  if (res.type === "panel-tabs") {
    res.items = res.items?.map((item) => {
      return item.type === "panel"
        ? {
            ...item,
            showTitle: item.showTitle ?? false,
            showFrame: item.showFrame ?? false,
          }
        : item;
    });
  }

  if (res.widget === "email") {
    res.pattern =
      "^([a-zA-Z0-9_.-])+@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]{2,4})+$";
  }

  if (res.items) {
    res.items = res.items.map((item) =>
      processView(item, res.fields ?? fields)
    );
  }

  if (Array.isArray(res.jsonFields)) {
    res.jsonFields = res.jsonFields.reduce(
      (prev, field) => ({ ...prev, [field.name!]: field }),
      {}
    );
  }

  return res;
}

export function parseDecimal(value: any, { scale }: Property) {
  if (scale) {
    const nums = String(value).split(".");
    // scale the decimal part
    const dec = parseFloat(`0.${nums[1] || 0}`).toFixed(+scale);
    // increment the integer part if decimal part is greater than 0 (due to rounding)
    const num = BigInt(nums[0]) + BigInt(parseInt(dec));
    // append the decimal part
    return num + dec.substring(1);
  }
  return value;
}

export function getDefaultValues(fields?: MetaData["fields"]) {
  const result: DataRecord = Object.entries(fields ?? {}).reduce(
    (acc, [key, field]) => {
      const { type, defaultValue } = field;
      if (defaultValue === undefined || key.includes(".")) {
        return acc;
      }
      const value =
        type === "DECIMAL" ? parseDecimal(defaultValue, field) : defaultValue;
      return { ...acc, [key]: value };
    },
    {}
  );
  return result;
}
