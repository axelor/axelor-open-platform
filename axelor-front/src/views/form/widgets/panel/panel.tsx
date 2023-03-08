import { GridLayout, WidgetProps } from "../../builder";
import styles from "./panel.module.css";

export function Panel(props: WidgetProps) {
  const { schema, formAtom } = props;
  const { title } = schema;

  return (
    <div className={styles.panel}>
      {title && <div className={styles.panelHeader}>{title}</div>}
      <GridLayout
        className={styles.panelBody}
        formAtom={formAtom}
        schema={schema}
      />
    </div>
  );
}
