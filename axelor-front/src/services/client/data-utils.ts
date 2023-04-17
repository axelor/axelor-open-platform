import { isEqual } from "lodash";
import { DataRecord } from "./data.types";

export function isDummy(name: string) {
  return name.startsWith("$");
}

export function isNil(value: any): value is null | undefined {
  return value === null || value === undefined;
}

export function isPlainObject(value: any): value is Record<string, any> {
  return value?.constructor === Object;
}

export function extractDummy(record: DataRecord): Record<string, any> {
  return Object.entries(record)
    .filter(([k]) => isDummy(k))
    .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {});
}

export function excludeDummy(record: DataRecord): DataRecord {
  return Object.entries(record)
    .filter(([k]) => !isDummy(k))
    .reduce((prev, [k, v]) => ({ ...prev, [k]: v }), {});
}

export function mergeDummy(record: DataRecord): DataRecord {
  const data = excludeDummy(record);
  const dummy = extractDummy(record);

  Object.entries(dummy).forEach(([k, v]) => {
    const n = k.substring(1);
    data[n] = data[n] ?? v;
  });

  return data;
}

function compact<T>(value: T): T {
  if (Array.isArray(value)) return value.filter((x) => !isNil(x)) as any;
  if (isPlainObject(value)) {
    if (value.id > 0 && value.version === undefined) return value.id;
    const result = Object.entries(value)
      .filter(([k, v]) => !isNil(v) && k !== "selected") // ignore null, undefined and selected
      .map(([k, v]) => [k, compact(v)]) // compact children
      .map(([k, v]) => [k, isPlainObject(v) ? excludeDummy(v) : v]) // exclude dummy fields
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
