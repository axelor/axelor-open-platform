import { useAtomValue } from "jotai";
import { useEffect, useMemo, useState } from "react";

import {
  Panel as AxPanel,
  CommandBarProps,
  CommandItemProps,
} from "@axelor/ui";

import { MenuItem } from "@/services/client/meta.types";

import { FieldLabel, GridLayout, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";

import styles from "./panel.module.scss";

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

  useEffect(() => {
    canCollapse && setCollapsed(collapse);
  }, [canCollapse, collapse]);

  const hasHeader = showTitle !== false && showFrame !== false && !!title;

  let style: any = {};

  if (!hasHeader && !showBorder) style["--ax-panel-border"] = "none";

  const { actionExecutor } = useFormScope();

  const toolbar = useMemo(() => {
    const items: MenuItem[] = schema.menu?.items ?? [];
    const actions = items.map((item, ind) => {
      const command: CommandItemProps = {
        key: item.name || `menu_item_${ind}`,
        text: item.title,
        onClick: async () => {
          await actionExecutor.waitFor();
          await actionExecutor.execute(item.action!, {
            context: {
              _source: item.name,
              _signal: item.name,
            },
          });
        },
      };
      return command;
    });

    if (actions.length === 0) return;

    const res: CommandBarProps = {
      items: [
        {
          key: "menu",
          iconProps: {
            icon: "more_vert",
          },
          items: actions,
        },
      ],
    };

    return res;
  }, [actionExecutor, schema.menu?.items]);

  const header = hasHeader ? (
    <div className={styles.title}>
      <FieldLabel schema={schema} formAtom={formAtom} widgetAtom={widgetAtom} />
    </div>
  ) : undefined;

  return (
    <AxPanel
      header={header}
      toolbar={toolbar}
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
