import { Box, StyleProps } from "@axelor/ui";
import { GridLayout, WidgetProps } from "../../builder";
import styles from "./panel.module.css";

const withHeaderProps: StyleProps = {
  rounded: true,
  border: true,
};

const withHeaderBodyProps: StyleProps = {
  p: 3,
};

export function Panel(props: WidgetProps) {
  const { schema, formAtom } = props;
  const { title, showTitle } = schema;

  const hasHeader = showTitle !== false && title;
  const moreProps = hasHeader && withHeaderProps;
  const bodyProps = hasHeader && withHeaderBodyProps;

  return (
    <Box className={styles.panel} {...moreProps}>
      {hasHeader && (
        <Box className={styles.panelHeader} borderBottom px={3} py={2}>
          {title}
        </Box>
      )}
      <Box className={styles.panelBody} {...bodyProps}>
        <GridLayout formAtom={formAtom} schema={schema} />
      </Box>
    </Box>
  );
}
