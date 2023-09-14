import { cssx, type ClassValue } from "@axelor/ui";
import styles from "./legacy.module.scss";

export const legacyClassNames = (...input: ClassValue[]) =>
  cssx(styles, ...input);
