import { Schema } from "@/services/client/meta.types";
import { useMemo } from "react";
import { toKebabCase } from "../../utils/names";

function makeId(...parts: (string | undefined)[]): string | undefined {
  return parts.filter(Boolean).join(":") || undefined;
}

export function createSchemaTestId(
  schema: Schema,
  prefix?: string,
): string | undefined {
  const { name, title } = schema;
  if (name) return makeId(prefix, name);
  if (title) return makeId(prefix, toKebabCase(title));
}

export function useSchemaTestId(
  schema: Schema,
  prefix?: string,
): string | undefined {
  const { id, name, title, type, widget } = schema;
  return useMemo(
    () => createSchemaTestId({ id, name, title, type, widget }, prefix),
    [id, name, title, type, widget, prefix],
  );
}
