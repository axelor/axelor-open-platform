import { useAtomValue } from "jotai";
import { useEffect, useState } from "react";

import { Panel as AxPanel } from "@axelor/ui";

import { FieldLabel, GridLayout, WidgetProps } from "../../builder";

import styles from "./panel.module.css";

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
  const [collapsed, setCollapsed] = useState(collapse);

  const hasHeader = showTitle !== false && showFrame !== false && !!title;

  let style: any = {};

  if (showBorder === false) style["--ax-panel-border"] = "none";
  if (hasHeader === false) style["--ax-panel-border"] = "none";

  useEffect(() => {
    canCollapse && setCollapsed(collapse);
  }, [canCollapse, collapse]);

  const header = hasHeader ? (
    <div className={styles.title}>
      <FieldLabel schema={schema} formAtom={formAtom} widgetAtom={widgetAtom} />
    </div>
  ) : undefined;

  return (
    <AxPanel
      header={header}
      collapsible={canCollapse}
      collapsed={collapsed}
      className={styles.panel}
      style={style}
    >
      <GridLayout
        readonly={readonly}
        formAtom={formAtom}
        parentAtom={widgetAtom}
        schema={schema}
      />
    </AxPanel>
  );
}
