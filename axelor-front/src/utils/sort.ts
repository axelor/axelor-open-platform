import { l10n } from "@/services/client/l10n";

/**
 * Compare values using user locale.
 * 
 * <p>It uses `Intl.Collator` for locale-aware comparisons.</p>
 */
export function compare(first: any, second: any): number {
  function toLocaleString(value?: string) {
    return (value || "").toLocaleString().toLocaleLowerCase();
  }

  if (first == null) {
    return 1;
  }
  if (second == null) {
    return -1;
  }
  if (isNaN(first) || isNaN(second)) {
    return l10n.getCompare()(toLocaleString(first), toLocaleString(second));
  }
  return first - second;
}

/**
 * The reverse of compare
 * 
 * @see {@link compare} for details
 */
export function reverseCompare(first: any, second: any): number {
  return compare(first, second) * -1;
}
