import { useAtomValue } from "jotai";
import { useEffect, useMemo, useState } from "react";

import {
  Panel as AxPanel,
  CommandBarProps,
  CommandItemProps,
  clsx,
} from "@axelor/ui";

import { MenuItem } from "@/services/client/meta.types";

import {
  FieldLabel,
  FormLayout,
  GridLayout,
  StackLayout,
  WidgetProps,
} from "../../builder";
import { useFormScope } from "../../builder/scope";

import styles from "./panel.module.scss";

export function Panel(props: WidgetProps) {
  const { schema, formAtom, widgetAtom, readonly } = props;
  const { showTitle = true, showFrame, canCollapse } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title, collapse } = attrs;
  const [collapsed, setCollapsed] = useState(collapse);

  useEffect(() => {
    canCollapse && setCollapsed(collapse);
  }, [canCollapse, collapse]);

  const hasHeader = showTitle !== false && showFrame !== false && !!title;

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

  const isEmptyPanel =
    schema.items?.length === 1 &&
    schema.items?.every(
      (item) =>
        item.jsonFields &&
        Object.keys(item.jsonFields).length === 0 &&
        item.widget !== "json-raw"
    );

  if (isEmptyPanel) return null;

  const header = hasHeader ? (
    <div className={styles.title}>
      <FieldLabel schema={schema} formAtom={formAtom} widgetAtom={widgetAtom} />
    </div>
  ) : undefined;

  const Layout: FormLayout = schema.stacked ? StackLayout : GridLayout;

  return (
    <AxPanel
      header={header}
      toolbar={toolbar}
      collapsible={hasHeader && canCollapse}
      collapsed={collapsed}
      className={clsx(styles.panel, {
        [styles.noFrame]: showFrame === false,
        [styles.hasHeader]: hasHeader,
        [styles.hasFrame]: showFrame === true,
      })}
    >
      <Layout
        readonly={readonly}
        formAtom={formAtom}
        parentAtom={widgetAtom}
        schema={schema}
      />
    </AxPanel>
  );
}
