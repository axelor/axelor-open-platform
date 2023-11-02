const ids: Record<string | symbol, number> = {};

/**
 * Generate a unique id.
 */
export function uniqueId(): string;

/**
 * Generate a unique id with given prefix.
 *
 * @param prefix the prefix
 */
export function uniqueId(prefix: string): string;

export function uniqueId(prefix: string = "uid"): string {
  return `${prefix}${(ids[prefix] = (ids[prefix] ?? 0) + 1)}`;
}
