import { uniqueId } from "lodash";

import { Property, Schema } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";

import { Attrs, DEFAULT_ATTRS } from "./types";

export function defaultAttrs(schema: Schema): Attrs {
  const attrs = Object.entries(schema)
    .filter(([name]) => name in DEFAULT_ATTRS)
    .reduce((prev, [name, value]) => ({ ...prev, [name]: value }), {});
  return attrs;
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

  if (res.title === undefined) {
    res.title = res.autoTitle ?? field.title ?? field.autoTitle;
  }

  if (res.items) {
    res.items = res.items.map((item) =>
      processView(item, res.fields ?? fields)
    );
  }

  return res;
}
