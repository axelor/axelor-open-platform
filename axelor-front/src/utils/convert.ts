import { DataContext } from "@/services/client/data.types";
import { Field, Property, Schema } from "@/services/client/meta.types";

import { limitScale } from "./format";
import { toKebabCase } from "./names";

type Merge<T, U> = T & Omit<U, keyof T>;

export type ConvertOptions = {
  props?: Merge<Schema, Merge<Field, Property>>;
  context?: DataContext;
};

export type Converter<T> = (
  value: unknown,
  opts?: ConvertOptions,
) => T | null | undefined;

const isNullOrUndefined = (value: unknown): value is null | undefined =>
  value === null || value === undefined;

const isNullOrEmpty = (value: unknown) =>
  value === null || (typeof value === "string" && value.trim() === "");

const convertNone = <T>(value: unknown, nullable: boolean, defaultValue?: T) =>
  isNullOrEmpty(value) ? (nullable ? null : defaultValue) : value;

export const convertDecimal: Converter<string> = (value, { props } = {}) => {
  const { nullable = false, scale = null } = props ?? {};
  const input = convertNone(value, nullable, 0);

  if (isNullOrUndefined(input)) return input;
  if (scale === null) return String(input);

  const nums = String(input).split(".");
  // scale the decimal part
  const limitedScale = limitScale(+scale);
  const dec = parseFloat(`0.${nums[1] || 0}`).toFixed(limitedScale);
  // increment the integer part if decimal part is greater than 0 (due to rounding)
  const num = BigInt(nums[0]) + BigInt(parseInt(dec));
  // append the decimal part
  return num + dec.substring(1);
};

export const convertInteger: Converter<number> = (value, { props } = {}) => {
  const { nullable = false } = props ?? {};
  const input = convertNone(value, nullable, 0);
  return isNullOrUndefined(input) ? input : parseInt(String(input));
};

export const Converters = {
  decimal: convertDecimal,
  integer: convertInteger,
  long: convertInteger,
};

const convert: Converter<any> = (value, opts = {}) => {
  const { props } = opts;
  let type = props?.serverType ?? props?.type;
  if (type) type = toKebabCase(type);

  const func = Converters[type as keyof typeof Converters];
  if (func) {
    return func(value, opts);
  }

  return isNullOrEmpty(value) ? null : value;
};

export default convert;
