export function getMultiValues(value?: null | number | string) {
  return value ? String(value).split(/\s*,\s*/) : [];
}

export function joinMultiValues(values: string[]) {
  return values.join(",");
}
