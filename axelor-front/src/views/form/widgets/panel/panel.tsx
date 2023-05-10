import { Box, StyleProps } from "@axelor/ui";
import { useAtomValue } from "jotai";
import { FieldLabel, GridLayout, WidgetProps } from "../../builder";
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
          <FieldLabel
            schema={schema}
            formAtom={formAtom}
            widgetAtom={widgetAtom}
          />
        </Box>
      )}
      <Box className={styles.panelBody} {...bodyProps}>
        <GridLayout
          readonly={readonly}
          formAtom={formAtom}
          parentAtom={widgetAtom}
          schema={schema}
        />
      </Box>
    </Box>
  );
}
