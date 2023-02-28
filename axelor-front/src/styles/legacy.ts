import { clsx, type ClassValue } from "clsx";
import styles from "./legacy.module.scss";

const clean = (names: string[]) =>
  names.flatMap((n) => n.trim().split(/\s+/)).filter(Boolean);

const names = (item: ClassValue): string[] => {
  if (Array.isArray(item)) return item.flatMap(names);
  if (item && typeof item === "object") {
    let items: string[] = [];
    for (let k in item) {
      if (item[k]) {
        items.push(k);
      }
    }
    return clean(items);
  }
  return item ? clean([item.toString()]) : [];
};

export const legacyClassNames = (...input: ClassValue[]): string => {
  return clsx(names(input).map((name) => styles[name] ?? name));
};
