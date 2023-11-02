/**
 * Remove duplicates from the array.
 *
 * @param array the array
 * @param equals equality checker, default is `Object.is`
 * @returns same array if no duplicates found else a new array with duplicates removed
 */
export function unique<T>(
  array: T[],
  equals: (a: T, b: T) => boolean = Object.is,
) {
  const result = array.filter(
    (item, index, array) => array.findIndex((a) => equals(a, item)) === index,
  );
  return result.length === array.length ? array : result;
}

/**
 * Group the array by given key mapper.
 *
 * @param array the array to transform
 * @param mapper the key mapper
 */
export function groupBy<T extends object>(
  array: T[],
  mapper: (value: T) => string,
) {
  return array.reduce(
    (acc, item) => {
      const key = mapper(item);
      const items = acc[key] ?? (acc[key] = []);
      items.push(item);
      return acc;
    },
    {} as Record<string, T[]>,
  );
}
