import camelCase from "lodash/camelCase";
import capitalize from "lodash/capitalize";
import kebabCase from "lodash/kebabCase";
import snakeCase from "lodash/snakeCase";
import startCase from "lodash/startCase";
import upperFirst from "lodash/upperFirst";

export function toCamelCase(name: string): string;
export function toCamelCase(name: string, firstUpper: boolean): string;
export function toCamelCase(name: string, firstUpper: boolean = true) {
  const res = camelCase(name);
  return firstUpper ? upperFirst(res) : res;
}

export function toKebabCase(name: string) {
  return kebabCase(name);
}

export function toSnakeCase(name: string) {
  return snakeCase(name);
}

export function toTitleCase(name: string): string;
export function toTitleCase(name: string, allUpper: boolean): string;
export function toTitleCase(name: string, allUpper = false) {
  const res = startCase(name);
  return allUpper ? res : capitalize(res);
}
