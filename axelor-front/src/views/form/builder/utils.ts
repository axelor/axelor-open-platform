import { uniqueId } from "lodash";

import { DataContext, DataRecord } from "@/services/client/data.types";
import {
  Field,
  Panel,
  Property,
  Schema,
  Widget,
} from "@/services/client/meta.types";
import { toCamelCase, toKebabCase, toSnakeCase } from "@/utils/names";

import { isPlainObject } from "@/services/client/data-utils";
import { MetaData } from "@/services/client/meta";
import convert from "@/utils/convert";
import { Attrs, DEFAULT_ATTRS, FormState } from "./types";

import * as WIDGETS from "../widgets";

const SERVER_TYPES: string[] = [
  "string",
  "boolean",
  "integer",
  "long",
  "decimal",
  "date",
  "time",
  "datetime",
  "text",
  "binary",
  "enum",
  "one-to-one",
  "many-to-one",
  "one-to-many",
  "many-to-many",
];

const FIELD_WIDGETS: Record<string, string[]> = {
  boolean: [
    "BooleanSelect",
    "BooleanRadio",
    "BooleanSwitch",
    "InlineCheckbox",
    "Toggle",
  ],
  integer: ["Duration", "Progress"],
  datetime: ["RelativeTime"],
  text: ["CodeEditor", "Html"],
  "many-to-one": ["BinaryLink", "Image"],
};

function getDefaultServerType(schema: Schema): string {
  const widget = toKebabCase(schema.widget ?? "");

  let serverType = "string";

  if (SERVER_TYPES.includes(widget)) {
    serverType = widget;
  } else {
    const fieldType = Object.keys(FIELD_WIDGETS).find((k) =>
      FIELD_WIDGETS[k].includes(toCamelCase(widget)),
    );
    serverType = fieldType || serverType;
  }

  return toSnakeCase(serverType).toUpperCase();
}

function getWidget(schema: Schema, field: any): string {
  let widget = schema.widget ?? schema.type;
  
  // default widget depending on field server type
  if (!schema.widget && (schema.type === "field" || schema.type === "panel-related")) {
    widget = schema.serverType;
  }

  // default image fields
  if (!schema.widget && field?.image) {
    widget = "image";
  }

  // adapt widget naming, ie boolean-select to BooleanSelect
  if (schema.type === "field" || schema.type === "panel-related") {
    widget = Object.keys(WIDGETS).find(
      (name) => 
        toCamelCase(name).toLowerCase() === toCamelCase(widget).toLowerCase()) ?? widget;
  }
  
  return toKebabCase(widget);
}

export function isField(schema: Schema) {
  const type = schema.type;
  return schema.jsonField || type === "field" || type === "panel-related";
}

export function isIntegerField(schema: Schema) {
  const type = toKebabCase(schema.serverType ?? schema.widget);
  return ["integer", "long"].includes(type);
}

export function isReferenceField(schema: Schema) {
  const type = toKebabCase(schema.serverType ?? schema.widget);
  return Boolean(type?.endsWith("-to-one"));
}

export function isCollectionField(schema: Schema) {
  const type = toKebabCase(schema.serverType ?? schema.widget);
  return Boolean(type?.endsWith("-to-many"));
}

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

export function processActionValue(value: any): any {
  if (Array.isArray(value)) return value.map(processActionValue);
  if (isPlainObject(value) && value.id == null) {
    return { ...value, id: nextId(), _dirty: true };
  }
  return value;
}

export function processContextValues(context: DataContext) {
  const IGNORE = [
    "$attachments",
    "$processInstanceId",
    "_dirty",
    "_showRecord",
    "_showSingle",
  ];

  function process(_value: DataContext) {
    if (typeof _value !== "object") return _value;

    const value = { ..._value };
    for (let k in value) {
      const v = value[k];

      // ignore values
      if (IGNORE.includes(k) || k.startsWith("$t:")) {
        delete value[k];
      }

      if (v && typeof v === "object") {
        if (Array.isArray(v)) {
          value[k] = v.map(process);
        } else {
          value[k] = process(v);
        }
      }
    }

    // set dummy id to null
    if ((value?.id ?? 0) < 0) {
      return { ...value, id: null };
    }

    return value;
  }

  return process(context);
}

export function processSaveValues(
  record: DataRecord,
  fields: FormState["fields"],
) {
  const values = processContextValues(record);

  Object.keys(values).forEach((fieldName) => {
    const field = fields[fieldName];
    if (field?.json) {
      let value = values[fieldName];
      if (value && typeof value === "string") {
        try {
          value = JSON.parse(value);
        } catch {
          // handle error
        }
      }
      values[fieldName] = value ? compactJson(value) : value;
    }
  });

  return values;
}

function compactJson(record: DataRecord) {
  const rec: DataRecord = {};
  Object.entries(record).forEach(([k, v]) => {
    if (k.indexOf("$") === 0 || v === null || v === undefined) return;
    if (typeof v === "string" && v.trim() === "") return;
    if (Array.isArray(v)) {
      if (v.length === 0) return;
      v = v.map(function (x) {
        return x.id ? { id: x.id } : x;
      });
    }
    rec[k] = v;
  });
  return JSON.stringify(rec);
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

export function processView(
  schema: Schema,
  fields: Record<string, Property>,
  parent?: Schema,
) {
  const field = fields?.[schema.name!] ?? {};
  const attrs = defaultAttrs(field);

  // merge default attrs
  const res: Schema = { ...attrs, serverType: field?.type, ...schema };

  if (res.type === "field") {
    res.serverType = res.serverType ?? getDefaultServerType(res);
  }
  if (res.type === "panel-related") {
    res.serverType = res.serverType ?? "ONE_TO_MANY";
  }

  res.uid = uniqueId("w");
  res.widget = getWidget(res, field);

  if (res.widget === "progress") {
    res.minSize = res.minSize ?? 0;
    res.maxSize = res.maxSize ?? 100;
  }

  // process attrs
  processAttrs(res, parseNumber, ...NUMBER_ATTRS);
  processAttrs(res.widgetAttrs ?? {}, parseNumber, ...NUMBER_ATTRS);

  if (
    !["panel", "dashlet", "separator", "button", "label"].includes(res.widget)
  ) {
    res.title = res.title ?? res.autoTitle ?? field.title ?? field.autoTitle;
  }

  const isCollectionItem = toKebabCase(parent?.serverType || "").endsWith(
    "-to-many",
  );
  const isPanelTabs = res.type === "panel-tabs";

  if ((res.showIf || res.hideIf) && !isCollectionItem && !isPanelTabs) {
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
      processView(item, res.fields ?? fields, res),
    );
  }

  if (Array.isArray(res.jsonFields)) {
    res.jsonFields = res.jsonFields.reduce(
      (prev, field) => ({ ...prev, [field.name!]: field }),
      {},
    );
  }

  return res;
}

export function getDefaultValues(
  fields?: MetaData["fields"],
  widgets?: Widget[],
) {
  const defaultJsonFieldValues = getDefaultJsonFieldValues(widgets);
  const defaultFieldValues = getDefaultFieldValues(fields);
  return { ...defaultJsonFieldValues, ...defaultFieldValues };
}

function getDefaultFieldValues(fields?: MetaData["fields"]) {
  const result: DataRecord = Object.entries(fields ?? {}).reduce(
    (acc, [key, field]) => {
      const { defaultValue } = field;
      if (defaultValue === undefined || key.includes(".")) {
        return acc;
      }
      const value = convert(defaultValue, { props: field });
      return { ...acc, [key]: value };
    },
    {},
  );
  return result;
}

function getDefaultJsonFieldValues(widgets?: Widget[]) {
  const result: DataRecord = {};

  for (const widget of widgets ?? []) {
    const { type } = widget;

    if (type === "panel") {
      const defaultValues = getDefaultJsonFieldValues((widget as Panel).items);
      Object.assign(result, defaultValues);
    } else if (type === "field") {
      const { jsonFields, name } = widget as Field;

      if (jsonFields) {
        const fields = Object.fromEntries(
          jsonFields.map(({ name, type, sequence, ...rest }) => [
            name,
            {
              name,
              type: toSnakeCase(type).toUpperCase(),
              ...rest,
            } as Property,
          ]),
        );
        const defaultValues = getDefaultFieldValues(fields);
        result[name] = JSON.stringify(defaultValues);
      }
    }
  }

  return result;
}

/**
 * Renames version to $version so that it is not sent in requests.
 *
 * @param {DataRecord} record - The data record to be processed.
 * @return {DataRecord} - The data record with $version.
 */
export function removeVersion(record: DataRecord) {
  const { version, $version, ...rest } = record;
  return { ...rest, $version: version ?? $version } as DataRecord;
}
