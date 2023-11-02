/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Check whether the value is a string.
 *
 * @param value the value to check
 */
export function isString(value: any): value is string {
  return typeof value === "string";
}

/**
 * Check whether the value is a number.
 *
 * @param value the value to check
 */
export function isNumber(value: any): value is number {
  return typeof value === "number";
}

/**
 * Check whether the value is an array.
 *
 * @param value the value to check
 */
export function isArray(value: any): value is any[] {
  return Array.isArray(value);
}

/**
 * Check whether the value is an object.
 *
 * @param value the value to check
 */
export function isObject(value: any): value is object {
  return typeof value === "object";
}

/**
 * Check whether the value is a plain object.
 *
 * @param value the value to check
 */
export function isPlainObject(
  value: any,
): value is Record<string | symbol, any> {
  return value?.constructor === Object;
}

/**
 * Check whether the value is null.
 *
 * @param value the value to check
 */
export function isNull(value: any): value is null {
  return value === null;
}

/**
 * Check whether the value is undefined.
 *
 * @param value the value to check
 */
export function isUndefined(value: any): value is undefined {
  return value === undefined;
}

/**
 * Check whether the value is null or undefined.
 *
 * @param value the value to check
 */
export function isNil(value: any): value is null | undefined {
  return value === null || value === undefined;
}

/**
 * Check whether the value is empty.
 *
 * @param value the value to check
 */
export function isEmpty(
  value: any,
  options?: {
    strict?: boolean;
  },
) {
  const { strict = false } = options ?? {};
  return (
    isNil(value) ||
    (Array.isArray(value) && value.length === 0) ||
    (isPlainObject(value) &&
      Object.getOwnPropertyNames(value).length === 0 &&
      (!strict || Object.getOwnPropertySymbols(value).length === 0)) ||
    (typeof value === "string" && (strict ? value : value.trim()) === "")
  );
}
