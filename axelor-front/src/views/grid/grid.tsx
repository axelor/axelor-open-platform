import { GridView } from "@/services/client/meta.types";
import { ViewProps } from "../types";

import styles from "./grid.module.scss";

export function Grid(props: ViewProps<GridView>) {
  return <div className={styles.grid}></div>;
}
