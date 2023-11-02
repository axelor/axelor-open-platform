import { PrimitiveAtom, atom } from "jotai";
import { selectAtom } from "jotai/utils";

const isFunction = <T>(x: T): x is T & ((...args: any[]) => any) =>
  typeof x === "function";

const getCached = <T>(c: () => T, m: WeakMap<object, T>, k: object): T =>
  (m.has(k) ? m : m.set(k, c())).get(k) as T;

const cache1 = new WeakMap();

const memo4 = <T>(
  create: () => T,
  dep1: object,
  dep2: object,
  dep3: object,
  dep4: object,
): T => {
  const cache2 = getCached(() => new WeakMap(), cache1, dep1);
  const cache3 = getCached(() => new WeakMap(), cache2, dep2);
  const cache4 = getCached(() => new WeakMap(), cache3, dep3);
  return getCached(create, cache4, dep4);
};

/**
 * Create a derived atom from a deeply nested state of a base atom.
 *
 * @param baseAtom the base atom
 * @param getter the nested value getter
 * @param setter the nested value setter
 * @param equal equality checker
 */
export function focusAtom<Value, Slice>(
  baseAtom: PrimitiveAtom<Value>,
  getter: (base: Value, prevSlice?: Slice) => Slice,
  setter: (base: Value, slice: Slice, prevSlice?: Slice) => Value,
  equal: (a: Slice, b: Slice) => boolean = Object.is,
): PrimitiveAtom<Slice> {
  return memo4(
    () => {
      const readAtom = selectAtom(baseAtom, getter, equal);
      const derivedAtom = atom(
        (get) => get(readAtom),
        (get, set, slice) => {
          const prevSlice = get(derivedAtom);
          const nextSlice = isFunction(slice) ? slice(prevSlice) : slice;
          if (equal(nextSlice, prevSlice)) {
            return;
          }
          const prevValue = get(baseAtom);
          const nextValue = setter(prevValue, nextSlice, prevSlice);
          if (nextValue !== prevValue) {
            set(baseAtom, nextValue);
          }
        },
      ) as unknown as PrimitiveAtom<Slice>;

      return derivedAtom;
    },
    baseAtom,
    getter,
    setter,
    equal,
  );
}
