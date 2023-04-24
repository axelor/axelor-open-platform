import { Box, StyleProps } from "@axelor/ui";
import { GridLayout, WidgetProps } from "../../builder";
import { useAtomValue } from "jotai";
import styles from "./panel.module.css";

const withHeaderProps: StyleProps = {
  rounded: true,
  border: true,
};

const withHeaderBodyProps: StyleProps = {
  p: 3,
};

export function Panel(props: WidgetProps) {
  const { schema, formAtom, widgetAtom, readonly } = props;
  const { showTitle = true, showFrame = true } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;

  const hasHeader = showTitle !== false && showFrame !== false && title;
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
        <GridLayout readonly={readonly} formAtom={formAtom} schema={schema} />
      </Box>
    </Box>
  );
}
