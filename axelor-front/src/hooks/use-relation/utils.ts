import { Schema } from "@/services/client/meta.types";

export function isPopupMaximized(
  schema: Schema,
  type: "editor" | "selector",
): boolean {
  const { popupMaximized = schema.widgetAttrs?.popupMaximized } = schema;
  return [type, "all"].includes(popupMaximized);
}
