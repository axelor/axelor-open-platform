import isUndefined from "lodash/isUndefined";
import pick from "lodash/pick";
import uniq from "lodash/uniq";
import uniqueId from "lodash/uniqueId";
import omit from "lodash/omit";

import { DataContext, DataRecord } from "@/services/client/data.types";
import {
  ActionView,
  Field,
  FormView,
  JsonField,
  Panel,
  Property,
  Schema,
  View,
  Widget,
} from "@/services/client/meta.types";
import { getJSON } from "@/utils/data-record";
import { toCamelCase, toKebabCase, toSnakeCase } from "@/utils/names";

import {
  getBaseDummy,
  isCleanDummy,
  isDummy,
} from "@/services/client/data-utils";
import { moment } from "@/services/client/l10n.ts";
import { MetaData, ViewData } from "@/services/client/meta";
import { LoadingCache } from "@/utils/cache";
import convert from "@/utils/convert";
import { findViewItems } from "@/utils/schema";
import { Attrs, DEFAULT_ATTRS, FormState } from "./types";

import * as WIDGETS from "../widgets";

export const SERVER_TYPES: string[] = [
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
  integer: ["Duration", "Progress", "Rating"],
  datetime: ["RelativeTime"],
  text: ["CodeEditor", "Html"],
  "many-to-one": ["BinaryLink", "Image", "SuggestBox", "Drawing", "Tag"],
  "many-to-many": ["Tags", "TagSelect"],
};

function getDefaultServerType(schema: Schema): string {
  const widget = toKebabCase(normalizeWidget(schema.widget) ?? "");

  if (
    schema.serverType &&
    SERVER_TYPES.includes(toKebabCase(schema.serverType))
  ) {
    return schema.serverType;
  }

  let serverType = schema.serverType ?? "string";

  if (SERVER_TYPES.includes(widget)) {
    serverType = widget;
  } else {
    const fieldType = Object.keys(FIELD_WIDGETS).find((k) =>
      FIELD_WIDGETS[k].includes(toCamelCase(widget)),
    );
    serverType = fieldType || serverType;
  }

  return serverType;
}

export function isValidWidget(widget: string): boolean {
  return !!normalizeWidget(widget);
}

function _normalizeWidget(widget: string): string | null {
  return (
    Object.keys(WIDGETS).find(
      (name) => toCamelCase(name).toLowerCase() === widget,
    ) ?? null
  );
}

const normalizeWidgetCache = new LoadingCache<string | null>();

export function normalizeWidget(widget: string): string | undefined {
  return (
    normalizeWidgetCache.get(
      toCamelCase(widget).toLowerCase(),
      _normalizeWidget,
    ) ?? undefined
  );
}

export function getWidget(schema: Schema, field: any): string {
  let widget = schema.widget ?? schema.type;

  // default widget depending on field server type
  if (!isValidWidget(schema.widget) && schema.type === "field") {
    widget = schema.serverType;
  }

  // default image fields
  if (!schema.widget && field?.image) {
    widget = "image";
  }

  // adapt widget naming, ie boolean-select to BooleanSelect
  widget = normalizeWidget(widget) ?? widget;

  return toKebabCase(widget);
}

export function getFieldServerType(
  schema: Schema,
  field: any,
): string | undefined {
  let serverType = field?.type;

  if (!serverType) {
    if (schema.type === "field") {
      serverType = getDefaultServerType(schema);
    } else if (schema.type === "panel-related") {
      serverType = schema.serverType ?? "ONE_TO_MANY";
    }
  }

  return serverType ? toSnakeCase(serverType).toUpperCase() : undefined;
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
  const type = toKebabCase(schema.serverType ?? schema.widget ?? schema.type);
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

export function processContextValues(
  context: DataContext,
  meta?: FormState["meta"],
) {
  const IGNORE = [
    "$processInstanceId",
    "_dirty",
    "_fetched",
    "_showRecord",
    "__check_version",
    "_showSingle",
  ];

  function process(_value: DataContext) {
    if (typeof _value !== "object") return _value;

    const value = { ..._value };
    for (let k in value) {
      const v = value[k];
      const isDummy =
        !IGNORE.includes(k) && k !== "$version" && isCleanDummy(k);

      // ignore values
      if (IGNORE.includes(k) || k.startsWith("$t:")) {
        delete value[k];
      } else if (isDummy) {
        k = getBaseDummy(k);
        value[k] = value[k] ?? v;
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

  const values = process(context);

  if (meta) {
    const { fields = {}, jsonFields } = meta;

    Object.keys(values).forEach((fieldName) => {
      const isJson =
        fields[fieldName]?.json || Boolean(jsonFields?.[fieldName]);
      if (isJson) {
        let value = values[fieldName];
        if (value && typeof value === "string") {
          try {
            value = JSON.parse(value);
          } catch {
            // handle error
          }
        }
        values[fieldName] = value
          ? compactJson(value, {
              findItem: (jsonPath: string) => {
                function findTargetNames(schema: Schema): string[] {
                  const items =
                    schema.type !== "panel-related" ? (schema.items ?? []) : [];
                  const nested = items.flatMap((item) => findTargetNames(item));
                  return [
                    ...items
                      .filter((item) => item.json && item.name === fieldName)
                      .map((item) => {
                        const itemFields: Record<string, JsonField> =
                          item.jsonFields ?? {};
                        return Object.values(itemFields).find(
                          (jsonItem: JsonField) => jsonItem.name === jsonPath,
                        )?.targetName as string;
                      })
                      .filter(Boolean),
                    ...nested,
                  ];
                }
                const field = jsonFields?.[fieldName]?.[jsonPath];
                return {
                  name: jsonPath,
                  ...field,
                  targetNames: uniq(
                    [field?.targetName, ...findTargetNames(meta.view)].filter(
                      Boolean,
                    ),
                  ),
                } as Schema;
              },
            })
          : value;
      }
    });
  }

  return values;
}

export function processSaveValues(record: DataRecord, meta: FormState["meta"]) {
  return { ...processContextValues(record, meta), $attachments: undefined };
}

export function compactJson(
  record: DataRecord,
  { findItem }: { findItem?: (name: string) => Schema } = {},
) {
  const rec: DataRecord = {};
  Object.entries(record).forEach(([k, v]) => {
    const field = findItem?.(k);
    if (k.indexOf("$") === 0 || v === null || v === undefined) return;
    if (typeof v === "string" && v.trim() === "") return;
    if (Array.isArray(v)) {
      if (v.length === 0) return;
      v = v.map(function (x) {
        return x.id ? { id: x.id } : x;
      });
    } else if (v && typeof v === "object" && field && isReferenceField(field)) {
      const { targetNames = [] } = field;
      v = {
        id: v.id,
        $version: v.version ?? v.$version,
        ...(targetNames as string[]).reduce(
          (vals, name) => ({
            ...vals,
            [name]: v[name],
          }),
          {},
        ),
      };
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
  const res: Schema = { ...attrs, ...schema };

  res.serverType = getFieldServerType(res, field);
  res.uid = res.uid ?? uniqueId("w");

  if (res.widget) {
    res.widgetAttrs = { ...res.widgetAttrs, widget: res.widget };
  }

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
    // custom json fields when showIf/hideIf is defined
    if (!isUndefined(res.widgetAttrs?.hidden)) {
      res.widgetAttrs.hidden = true;
    }
  }

  // for editable grid case : avoid fields blinking
  if (isField(res) && res.readonlyIf && parent?.editable) {
    res.readonly = true;
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
      "^[a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9]{2,}$";
  }

  if (res.items) {
    res.items = res.items.map((item) =>
      processView(item, res.fields ?? fields, res),
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
      let { defaultValue, defaultNow } = field;
      if (defaultValue === undefined || key.includes(".")) {
        return acc;
      }
      if (defaultNow) {
        // ensure correct date/datetime
        defaultValue =
          field.type.toLowerCase() === "date"
            ? moment().startOf("day").format("YYYY-MM-DD")
            : moment().toISOString();
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

  function setJSONValues(name: string, values: Record<string, any>) {
    result[name] = JSON.stringify({
      ...getJSON(result[name] || "{}"),
      ...values,
    });
  }

  for (const widget of widgets ?? []) {
    const { type } = widget;

    if (["panel", "panel-json"].includes(type)) {
      const defaultValues = getDefaultJsonFieldValues((widget as Panel).items);
      Object.entries(defaultValues).forEach(([k, v]) =>
        setJSONValues(k, getJSON(v)),
      );
    } else if ((widget as Schema).jsonField && (widget as Schema).jsonPath) {
      const { jsonField, jsonPath } = widget as Schema;
      const defaultValues = getDefaultFieldValues({
        [jsonPath]: widget as Property,
      });
      setJSONValues(jsonField, defaultValues);
    } else if (type === "field") {
      const { jsonFields, name } = widget as Field;

      if (jsonFields) {
        const fields = Object.keys(jsonFields).reduce((_fields, key) => {
          const { name, type, sequence, ...rest } = jsonFields[key];
          return {
            ..._fields,
            [name]: {
              name,
              type: toSnakeCase(type).toUpperCase(),
              ...rest,
            } as Property,
          };
        }, {});
        const defaultValues = getDefaultFieldValues(fields);
        setJSONValues(name, defaultValues);
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

export function processM2OValue(record: DataRecord, targetName: string) {
  return pick(removeVersion(record), ["id", "$version", targetName]);
}

export function isExpandableWidget(schema: Schema) {
  return ["tree-grid", "expandable"].includes(schema.widget ?? "");
}

// Types that are saved independently from main record
const INDEPENDENT_TYPES = new Set([
  "MANY_TO_ONE",
  "ONE_TO_ONE",
  "MANY_TO_MANY",
]);

/**
 * Processes the original record to avoid version check on independent relations.
 *
 * @param {DataRecord} record - The original data record to be processed.
 * @param {MetaData["fields"]} fields - The metadata fields used for processing.
 * @return {DataRecord} - The processed original data record.
 */
export function processOriginal(
  record: DataRecord,
  fields: MetaData["fields"],
) {
  const original = { ...record };
  Object.values(fields)
    .filter((field: Property) => INDEPENDENT_TYPES.has(field.type))
    .forEach((field: Property) => {
      const value = original[field.name];
      if (value) {
        original[field.name] = Array.isArray(value)
          ? value.map(removeVersion)
          : removeVersion(value);
      }
    });
  return original;
}

export function createContextParams(
  schema: Schema | View,
  action?: ActionView,
) {
  const { name, type } = schema;
  if (action) {
    const { model, views: _views, viewType, context } = action;
    const found = _views?.some((x) => x.type === type);
    const _viewType = found ? type : viewType;

    const _view = _views?.find((x) => x.type === _viewType);
    const _viewName = _view ? (_view.name ?? name) : name;

    // ignore special context names
    const IGNORE = ["_showRecord", "_showSingle", "__check_version"];

    return {
      ...omit(context ?? {}, IGNORE),
      _model: model,
      _viewName,
      _viewType,
      _views,
    };
  }

  const { target, formView, gridView } = schema as Schema;
  const source = name ? { _source: name } : undefined;
  if (!target) return source;
  const form = formView && { type: "form", name: formView };
  const grid = gridView && { type: "grid", name: gridView };
  const _views = [grid, form].filter(Boolean);
  const _viewName = _views[0]?.name;
  const _viewType = _views[0]?.type ?? "grid";
  return {
    ...source,
    _viewName,
    _viewType,
    _views,
  };
}

export function resetFormDummyFieldsState(
  meta: ViewData<FormView>,
  statesByName: FormState["statesByName"],
) {
  const fieldList = findViewItems(meta, (item: Schema) =>
    Boolean(item?.name && isField(item)),
  ).map((item) => item.name!);

  const fieldNames = Object.keys(meta.fields ?? {});

  // reset dummy fields state only
  // only keep real fields and non field items like panels state
  return Object.keys(statesByName)
    .filter((key) => !fieldList.includes(key) || !isDummy(key, fieldNames))
    .reduce(
      (state, key) => ({
        ...state,
        [key]: statesByName[key],
      }),
      {},
    );
}
