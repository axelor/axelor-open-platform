import { isEqual } from "lodash";
import { DataRecord } from "./data.types";

/**
 * Checks if the given name is the name of a dummy field.
 * A dummy field is a field that exists in the view, but not in the model.
 *
 */
export function isDummy(name: string, fieldNames: string[]) {
  return (
    // in model
    !fieldNames.includes(name) &&
    // id and version may not be in fieldNames
    !["id", "version"].includes(name) &&
    // special case for enum fields
    !(name.endsWith("$value") && fieldNames.includes(name.slice(0, -6))) &&
    // extra data
    !["$attachments", "$processInstanceId", "_dirty"].includes(name) &&
    // key of translatable fields
    !name.startsWith("$t:")
  );
}

export function extractDummy(
  record: DataRecord,
  fieldNames: string[],
): Record<string, any> {
  return Object.entries(record)
    .filter(([k]) => isDummy(k, fieldNames))
    .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {});
}

/**
 * Checks if the given name is the name of a clean dummy field.
 * A clean dummy field is a dummy field that cannot dirty the view.
 *
 */
export function isCleanDummy(name: string) {
  return name.startsWith("$") || name.startsWith("__");
}

export function getBaseDummy(name: string) {
  return name.replace(/^\$+/, "");
}

export function isNil(value: any): value is null | undefined {
  return value === null || value === undefined;
}

export function isPlainObject(value: any): value is Record<string, any> {
  return value?.constructor === Object;
}

export function extractCleanDummy(record: DataRecord): Record<string, any> {
  return Object.entries(record)
    .filter(([k]) => isCleanDummy(k))
    .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {});
}

export function excludeCleanDummy(record: DataRecord): DataRecord {
  return Object.entries(record)
    .filter(([k]) => !isCleanDummy(k))
    .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {});
}

export function mergeCleanDummy(record: DataRecord): DataRecord {
  const data = { ...record };
  const dummy = extractCleanDummy(record);

  //XXX: merge dummy fields by removing `$`, should be removed in next major release
  Object.entries(dummy).forEach(([k, v]) => {
    const n = getBaseDummy(k);
    data[n] = data[n] ?? v;
  });

  return data;
}

function compact<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.filter((x) => !isNil(x)).map((x) => compact(x)) as any;
  }
  if (isPlainObject(value)) {
    if (value.id > 0 && value.version === undefined) return value.id;
    const result = Object.entries(value)
      .filter(([k, v]) => !isNil(v) && k !== "selected") // ignore null, undefined and selected
      .map(([k, v]) => [k, compact(v)]) // compact children
      .map(([k, v]) => [k, isPlainObject(v) ? excludeCleanDummy(v) : v]) // exclude clean dummy fields
      .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {}) as any;
    if (value.$id) result.id = result.id || value.$id; // make sure to use dummy id
    return result;
  }
  return value;
}

export function equals(value: DataRecord, other: DataRecord): boolean {
  const a = compact(value);
  const b = compact(other);
  return isEqual(a, b);
}

export function diff(a: DataRecord, b: DataRecord): DataRecord {
  if (a === b) return a;
  if (a === null || b === null) return a;
  if (!a && !b) return a;
  if (!a.id || a.id < 1) return a;

  const result = Object.entries(a).reduce((prev, [key, value]) => {
    if (key === "id" || key === "version" || !equals(value, b[key])) {
      return { ...prev, [key]: value };
    }
    return prev;
  }, {});

  return result;
}

function toCompact(value: DataRecord | null | undefined) {
  if (!value) return value;
  if (value.version === undefined) return value;
  if (!value.id) return value;
  const { version: $version, ...rest } = value;
  return { ...rest, $version };
}

export function updateRecord(target: DataRecord, source: DataRecord) {
  if (equals(target, source)) {
    return target;
  }

  let result = target;
  let changed = false;

  for (const [key, value] of Object.entries(source)) {
    let newValue = value;
    if (newValue === result[key]) {
      continue;
    }

    let isSelectedChanged = false;

    if (Array.isArray(value)) {
      const curr: DataRecord[] = result[key] ?? [];
      newValue = value.map((item) => {
        const found = curr.find((x) =>
          item.id > 0 ? x.id === item.id : equals(x, item),
        );
        if (found) {
          let newItem = updateRecord(found, item);
          if (found.selected !== item.selected) {
            newItem = { ...newItem, selected: item.selected };
            isSelectedChanged = true;
          }
          return newItem;
        }
        if (item.selected) {
          isSelectedChanged = true;
        }
        return item;
      });
    }

    if (isPlainObject(value)) {
      const curr: DataRecord = result[key] ?? {};
      if (curr.id === value.id) {
        if (curr.version! >= 0) {
          newValue = updateRecord(curr, value);
        } else {
          // update nested-editor values?
          newValue = { ...curr, $updatedValues: value };
        }
      } else {
        newValue = toCompact(value);
      }
    }

    const isChanged = key !== "selected" && !equals(result[key], newValue);
    if (isChanged || isSelectedChanged) {
      changed = changed || isChanged;
      result = { ...result, [key]: newValue };
    }
  }

  return changed ? { ...result, _dirty: true } : result;
}
