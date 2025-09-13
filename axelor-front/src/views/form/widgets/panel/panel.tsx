import { useAtomValue, useSetAtom } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Panel as AxPanel,
  CommandBarProps,
  CommandItemProps,
  clsx,
} from "@axelor/ui";

import { MenuItem, Schema } from "@/services/client/meta.types";
import { isTopLevelItem } from "@/services/client/meta-utils";

import {
  FieldLabel,
  FormLayout,
  GridLayout,
  StackLayout,
  WidgetProps,
} from "../../builder";
import { useFormScope } from "../../builder/scope";
import { useWidgetAttrsAtomByName } from "../../builder/atoms";

import styles from "./panel.module.scss";

export function usePanelClass(schema: Schema) {
  return useMemo(
    () =>
      clsx(styles.panelBox, {
        [styles.topLevel]: isTopLevelItem(schema),
      }),
    [schema],
  );
}

export function Panel(props: WidgetProps) {
  const { schema, formAtom, widgetAtom, readonly } = props;
  const {
    showTitle = true,
    showFrame,
    icon,
    collapseIf,
    canCollapse = Boolean(collapseIf),
  } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title, collapse = false } = attrs;
  const [collapsed, setCollapsed] = useState(collapse);

  const widgetAttrsAtom = useWidgetAttrsAtomByName({ schema, formAtom });
  const setWidgetAttrsByName = useSetAtom(widgetAttrsAtom);

  const resetStates = useCallback(() => {
    if (canCollapse) setCollapsed(collapse);
  }, [canCollapse, collapse]);

  useEffect(() => {
    resetStates();
    document.addEventListener("form:reset-states", resetStates);
    return () => {
      document.removeEventListener("form:reset-states", resetStates);
    };
  }, [resetStates]);

  const toggleCollapse = useCallback(() => {
    // reset collapse in statesByName
    setWidgetAttrsByName((_attrs) => {
      if (_attrs.collapse === collapsed) {
        _attrs.collapse = !collapsed;
      }
    });
    setCollapsed((prev) => !prev);
  }, [collapsed, setWidgetAttrsByName]);

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

  const panelClass = usePanelClass(schema);

  const isEmptyPanel =
    schema.items?.length === 1 &&
    schema.items?.every(
      (item) =>
        item.jsonFields &&
        Object.keys(item.jsonFields).length === 0 &&
        item.widget !== "json-raw",
    );

  if (isEmptyPanel) return null;

  const header = hasHeader ? (
    <div className={styles.title}>
      <FieldLabel
        icon={icon}
        schema={schema}
        formAtom={formAtom}
        widgetAtom={widgetAtom}
      />
    </div>
  ) : undefined;

  const Layout: FormLayout = schema.stacked ? StackLayout : GridLayout;

  return (
    <AxPanel
      header={header}
      toolbar={toolbar}
      collapsible={hasHeader && canCollapse}
      collapsed={collapseIf && collapsed == null ? true : collapsed}
      setCollapsed={toggleCollapse}
      className={clsx(styles.panel, panelClass, {
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
