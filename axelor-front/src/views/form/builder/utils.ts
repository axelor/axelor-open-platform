import { uniqueId } from "lodash";

import { DataContext } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";

import { Attrs, DEFAULT_ATTRS } from "./types";

const nextId = (() => {
  let id = 0;
  return () => --id;
})();

export function defaultAttrs(schema: Schema): Attrs {
  const attrs = Object.entries(schema)
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

  res.uid = uniqueId("w");
  res.widget = toKebabCase(type);

  if (res.widget !== "panel" && res.widget !== "separator") {
    res.title = res.title ?? res.autoTitle ?? field.title ?? field.autoTitle;
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
