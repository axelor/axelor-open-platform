import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Box, NavItemProps, NavTabs as Tabs } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";
import { FormAtom, FormWidget, WidgetProps } from "../../builder";
import { createWidgetAtom } from "../../builder/atoms";

function TabContent({
  schema,
  active,
  formAtom,
  readonly,
}: {
  schema: NavItemProps;
  active: boolean;
  formAtom: WidgetProps["formAtom"];
  readonly?: boolean;
}) {
  const display = active ? "block" : "none";
  return (
    <Box d={display} pt={3}>
      <FormWidget readonly={readonly} schema={schema} formAtom={formAtom} />
    </Box>
  );
}

function Tab(
  props: NavItemProps & {
    schema?: Schema;
    formAtom?: FormAtom;
  }
) {
  const schema = props.schema!;
  const formAtom = props.formAtom!;

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema]
  );
  const { attrs } = useAtomValue(widgetAtom);
  const { title, hidden } = attrs;

  if (hidden) {
    return null;
  }

  return <div>{title}</div>;
}

export function PanelTabs(props: WidgetProps) {
  const { schema, formAtom, readonly } = props;
  const { items = [] } = schema;
  const [activeTab, setActiveTab] = useState<string | null>(null);

  const handleChange = useCallback(function handleChange(
    id: string,
    tab: NavItemProps
  ) {
    setActiveTab(id);
  },
  []);

  const tabItems = useMemo(() => {
    return items.map((item) => ({
      ...item,
      schema: item,
      formAtom,
      id: item.uid,
    })) as NavItemProps[];
  }, [formAtom, items]);

  useEffect(() => {
    const [firstItem] = tabItems;
    firstItem && setActiveTab(firstItem.id);
  }, [tabItems]);

  return (
    <Box d="flex" flexDirection="column">
      <Tabs
        items={tabItems}
        value={activeTab ?? undefined}
        onChange={handleChange}
        onItemRender={Tab}
      />
      {tabItems.map((item) => {
        const active = activeTab === item.id;
        return (
          <TabContent
            key={item.id}
            schema={item}
            active={active}
            formAtom={formAtom}
            readonly={readonly}
          />
        );
      })}
    </Box>
  );
}
