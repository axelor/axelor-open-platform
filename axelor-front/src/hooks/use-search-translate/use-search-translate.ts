import { useCallback, useMemo } from "react";

import { SearchOptions } from "@/services/client/data";
import { Criteria, Filter } from "@/services/client/data.types";
import { Property } from "@/services/client/meta.types";

export function useSearchTranslate(
  orderBy?: { name: string }[] | null,
  fields?: Record<string, Property>,
) {
  const nameField = useNameField(fields);
  const hasTranslatableSort = useMemo(
    () =>
      orderBy?.length
        ? orderBy.some((field) => fields?.[field.name]?.translatable)
        : fields?.[nameField ?? ""]?.translatable ?? false,
    [orderBy, fields, nameField],
  );

  return useCallback(
    (filter?: SearchOptions["filter"]) =>
      hasTranslatableSort ||
      hasTranslatableCriteria(filter?.criteria, fields) ||
      undefined,
    [hasTranslatableSort, fields],
  );
}

function useNameField(fields?: Record<string, Property>) {
  return useMemo(() => {
    if (fields) {
      const name = Object.entries(fields).find(
        ([, field]) => field.nameColumn,
      )?.[0];
      if (name) {
        return name;
      }
      if ("name" in fields) {
        return "name";
      }
    }
    return null;
  }, [fields]);
}

const hasTranslatableCriteria = (
  criteria?: (Filter | Criteria)[],
  fields?: Record<string, Property>,
): boolean =>
  criteria?.some(
    (item) =>
      ("fieldName" in item &&
        isTranslatableField(item.fieldName ?? "", fields)) ||
      ("criteria" in item &&
        hasTranslatableCriteria(item.criteria ?? [], fields)),
  ) ?? false;

const isTranslatableField = (
  name: string,
  fields?: Record<string, Property>,
) => {
  // Translatable field
  if (fields?.[name]?.translatable) {
    return true;
  }

  // Relational field
  // Currently, we don't have info about whether targetName is translatable
  const [baseFieldName, targetName] = splitFieldName(name);
  return fields?.[baseFieldName]?.targetName === targetName;
};

function splitFieldName(name: string) {
  const index = name.lastIndexOf(".");
  return [name.substring(0, index), name.substring(index + 1)];
}
