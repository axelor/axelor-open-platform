import { Schema } from "@/services/client/meta.types";

export function isPopupMaximized(
  schema: Schema,
  type: "editor" | "selector",
): boolean {
  const { popupMaximized = schema.widgetAttrs?.popupMaximized } = schema;
  return [type, "all"].includes(popupMaximized);
}

/**
 * Group dotted field names by their relation (root) field.
 *
 * Only dotted fields are considered: for each `<relation>.<sub>` entry, the
 * `<sub>` part is collected under the `<relation>` key. Plain (non-dotted)
 * fields are ignored.
 *
 * @param fields the list of field names (e.g. `["contact.name", "contact.email", "title"]`)
 * @returns a map of relation name to its sub-fields
 *   (e.g. `{ contact: ["name", "email"] }`), or `undefined` when no dotted
 *   field is present
 */
export function computeExtraRelated(
  fields: string[],
): Record<string, string[]> | undefined {
  const result: Record<string, string[]> = {};
  for (const field of fields) {
    const dot = field.indexOf(".");
    if (dot !== -1) {
      const rel = field.slice(0, dot);
      const sub = field.slice(dot + 1);
      (result[rel] ??= []).push(sub);
    }
  }
  return Object.keys(result).length ? result : undefined;
}
