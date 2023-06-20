import { Box, Collapse, StyleProps } from "@axelor/ui";
import { useAtomValue } from "jotai";
import { useEffect, useState } from "react";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import clsx from "clsx";

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
  const {
    showBorder,
    showTitle = true,
    showFrame = true,
    canCollapse,
  } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title, collapse } = attrs;
  const [isCollapse, setCollapse] = useState(collapse);

  const hasHeader = showTitle !== false && showFrame !== false && title;
  const moreProps = (hasHeader || showBorder) && withHeaderProps;
  const bodyProps = (hasHeader || showBorder) && withHeaderBodyProps;

  useEffect(() => {
    canCollapse && setCollapse(collapse);
  }, [canCollapse, collapse]);

  function renderBody() {
    return (
      <Box className={styles.panelBody} {...bodyProps}>
        <GridLayout
          readonly={readonly}
          formAtom={formAtom}
          parentAtom={widgetAtom}
          schema={schema}
        />
      </Box>
    );
  }

  return (
    <Box className={styles.panel} {...moreProps}>
      {hasHeader && (
        <Box
          className={clsx(styles.panelHeader, {
            [styles.collapsible]: canCollapse,
          })}
          borderBottom
          px={3}
          py={2}
          {...(canCollapse && {
            onClick: () => setCollapse((c) => !c),
          })}
        >
          <FieldLabel
            schema={schema}
            formAtom={formAtom}
            widgetAtom={widgetAtom}
          />
          {canCollapse && (
            <MaterialIcon icon={isCollapse ? "expand_more" : "expand_less"} />
          )}
        </Box>
      )}
      {canCollapse ? (
        <Collapse in={!isCollapse}>{renderBody()}</Collapse>
      ) : (
        renderBody()
      )}
    </Box>
  );
}
