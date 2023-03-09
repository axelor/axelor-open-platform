import { Box } from "@axelor/ui";
import { GridLayout, WidgetProps } from "../../builder";
import styles from "./panel.module.css";

export function Panel(props: WidgetProps) {
  const { schema, formAtom } = props;
  const { title } = schema;

  return (
    <Box className={styles.panel} rounded border>
      {title && (
        <Box className={styles.panelHeader} borderBottom>
          {title}
        </Box>
      )}
      <Box className={styles.panelBody}>
        <GridLayout formAtom={formAtom} schema={schema} />
      </Box>
    </Box>
  );
}
