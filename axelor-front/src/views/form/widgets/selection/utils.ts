export function getMultiValues(value?: null | number | string) {
  return value || value === 0 ? String(value).split(/\s*,\s*/) : [];
}

export function joinMultiValues(values: string[]) {
  return values.join(",");
}
