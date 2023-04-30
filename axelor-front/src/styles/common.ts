import { cssx, type ClassValue } from "@axelor/ui";
import styles from "./common.module.scss";

export const commonClassNames = (...input: ClassValue[]) =>
  cssx(styles, ...input);
