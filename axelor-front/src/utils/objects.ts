import { isPlainObject } from "./types";

export type ObjectKey = string | symbol;

export type DeepPath = ObjectKey | Array<ObjectKey | number>;

function includesKey(
  within: ObjectKey[] | ((key: ObjectKey) => boolean),
  value: ObjectKey,
) {
  return typeof within === "function" ? within(value) : within.includes(value);
}

/**
 * Exclude the keys from the object matched by the exclude callback.
 *
 * @param obj the object
 * @param exclude the callback to check the keys
 */
export function omit<T extends Record<ObjectKey, unknown>>(
  obj: T,
  exclude: (key: ObjectKey) => boolean,
): Partial<T>;
/**
 * Exclude the givem keys from the object.
 *
 * @param obj the object
 * @param exclude the keys to exclude
 */
export function omit<T extends Record<ObjectKey, unknown>>(
  obj: T,
  exclude: ObjectKey[],
): Partial<T>;
export function omit<T extends Record<ObjectKey, unknown>>(
  obj: T,
  exclude: ObjectKey[] | ((key: ObjectKey) => boolean),
): Partial<T> {
  return Object.entries(obj)
    .filter(([key]) => !includesKey(exclude, key))
    .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});
}

/**
 * Pick the keys from the object matched by the given callback.
 *
 * @param obj the object
 * @param include the callback to check the keys
 */
export function pick<T extends Record<ObjectKey, unknown>>(
  obj: T,
  include: (key: ObjectKey) => boolean,
): Partial<T>;
/**
 * Pick the given keys from the object.
 *
 * @param obj the object
 * @param include the keys to include
 */
export function pick<T extends Record<ObjectKey, unknown>>(
  obj: T,
  include: ObjectKey[],
): Partial<T>;
export function pick<T extends Record<ObjectKey, unknown>>(
  obj: T,
  include: ObjectKey[] | ((key: ObjectKey) => boolean),
): Partial<T> {
  return omit(obj, (key) => includesKey(include, key));
}

function toArray(path: ObjectKey | number): DeepPath {
  if (typeof path === "number") return [path];
  if (typeof path === "symbol") return [path];
  return path.split(".").flatMap((part) => {
    const prop = part.trim();
    if (prop.match(/^\d+$/)) return [parseInt(prop)];
    const match = prop.match(/(?<name>.*)(\[(?<index>\d+)\])/);
    return match?.groups
      ? match.groups.name
        ? [match.groups.name, parseInt(match.groups.index)]
        : [parseInt(match.groups.index)]
      : [prop];
  });
}

function toPath(path: DeepPath) {
  return [path]
    .flat()
    .flatMap((part) => toArray(part))
    .flat();
}

/**
 * Get value from the object by the given path.
 *
 * @param obj the object
 * @param path the path to the value
 * @param defaultValue default value to return if no value found
 */
export function deepGet<T = any>(
  obj: object,
  path: DeepPath,
  defaultValue?: T,
): T | undefined {
  const props = toPath(path);
  let target: object | undefined = obj;
  while (props.length) {
    const prop = props.shift();
    if (target === null || target === undefined) return defaultValue;
    if (prop === undefined) continue;
    target = Reflect.get(target, prop);
  }
  return (target as T) ?? defaultValue;
}

/**
 * Set value to the object at given path.
 *
 * @param obj the object
 * @param path the path to the value
 * @param value the value to set
 */
export function deepSet<T extends object>(
  obj: T,
  path: DeepPath,
  value: unknown,
) {
  const props = toPath(path);
  const last = props.pop();

  let target: object = obj;
  while (props.length) {
    const prop = props.shift();
    let value = Reflect.get(target, prop!);
    if (value === null || value === undefined) {
      const next = props[0] ?? last;
      value = typeof next === "number" ? [] : {};
      Reflect.set(target, prop!, value);
    }
    target = value;
  }

  if (target) Reflect.set(target, last!, value);

  return obj;
}

/**
 * Compare two values deeply.
 *
 * It only checkes plain objects deeply, other objects are
 * checked by reference. Use custom `equals` option to change
 * this behaviour.
 *
 * @param obj the first object
 * @param other the other object
 * @param options additional options to customise the check
 * @param options.strict whether to check stricty
 * @param options.equals custom eqaulity checker, default is `Object.is`
 * @param options.ignore callback to ignore keys during equality check
 */
export function deepEqual(
  obj: unknown,
  other: unknown,
  options?: {
    strict?: boolean;
    equals?: (a: unknown, b: unknown) => boolean;
    ignore?: (key: ObjectKey, value: unknown) => boolean;
  },
): boolean {
  const {
    strict = true,
    equals = Object.is,
    ignore = () => false,
  } = options ?? {};
  if (equals(obj, other)) return true;
  if (Array.isArray(obj) && Array.isArray(other)) {
    if (obj.length !== other.length) return false;
    return strict
      ? obj.every((x, i) => deepEqual(x, other[i], options))
      : obj.every((x) => other.some((o) => deepEqual(o, x, options)));
  }
  if (isPlainObject(obj) && isPlainObject(other)) {
    const objKeys = Object.keys(obj).filter((x) => !ignore(x, obj[x]));
    const otherKeys = Object.keys(other).filter((x) => !ignore(x, other[x]));
    if (objKeys.length !== otherKeys.length) return false;
    if (objKeys.some((x) => !otherKeys.includes(x))) return false;
    if (otherKeys.some((x) => !objKeys.includes(x))) return false;
    return objKeys.every((name) => deepEqual(obj[name], other[name], options));
  }
  return equals(obj, other);
}

const defaultMerge =
  (equal = deepEqual) =>
  (target: unknown, source: unknown) => {
    return isPlainObject(target) && isPlainObject(source)
      ? deepMerge(target, source)
      : Array.isArray(target) && Array.isArray(source)
        ? source.map((item) => {
            if (isPlainObject(item)) {
              const found = target.find((x) => equal(x, item));
              if (isPlainObject(found)) return deepMerge(found, item);
            }
            return item;
          })
        : source;
  };

/**
 * Deep merge two objects.
 *
 * @param target the target object
 * @param source the source object
 * @param options options to customize merging
 * @param options.merge custom merge logic, default is deep merge
 * @param options.equal custom equal logic, default is deep equal
 * @returns a new merged object
 */
export function deepMerge<
  T extends object,
  S extends object,
  R extends object = T & S,
>(
  target: Partial<T>,
  source: Partial<S>,
  options?: {
    merge?: (source: unknown, target: unknown) => unknown;
    equal?: (source: unknown, target: unknown) => boolean;
  },
): R {
  const { equal = deepEqual, merge = defaultMerge(equal) } = options ?? {};
  const obj = { ...target, ...source } as R;
  return Object.keys(obj).reduce((acc, key) => {
    const t = target[key as keyof T];
    const s = source[key as keyof S];
    const v = equal(t, s)
      ? t
      : typeof t === "object" && typeof s === "object"
        ? merge(t, s)
        : s === undefined
          ? t
          : s;
    return { ...acc, [key]: v };
  }, obj);
}

function baseClone<T>(value: T, cloned = new WeakMap()): T {
  // non-object types
  if (value === null || typeof value !== "object") {
    return value;
  }

  // circular references
  if (cloned.has(value)) {
    return cloned.get(value);
  }

  // having clone
  if ("clone" in value && typeof value.clone === "function") {
    const clone = value.clone();
    cloned.set(value, clone);
    return clone;
  }

  // date
  if (value instanceof Date) {
    const clone = new Date(value.getTime()) as T;
    cloned.set(value, clone);
    return clone;
  }

  // array
  if (Array.isArray(value)) {
    const clone: T[] = [];
    cloned.set(value, clone);
    for (const item of value) {
      clone.push(baseClone(item, cloned));
    }
    return clone as T;
  }

  // plain object
  if (isPlainObject(value)) {
    const clone: Record<string, unknown> = {};
    cloned.set(value, clone);
    for (const [k, v] of Object.entries(value)) {
      clone[k] = baseClone(v, cloned);
    }
    return clone as T;
  }

  // ignore others
  return value;
}

/**
 * Deep clone given object.
 *
 * This utility can clone plain objects, arrays, date
 * and objects with clone method.
 *
 * @param value the object to clone
 * @returns cloned object
 */
export function deepClone<T>(value: T): T {
  return baseClone(value);
}
