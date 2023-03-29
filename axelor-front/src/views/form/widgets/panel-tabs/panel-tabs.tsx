import { Box, NavItemProps, NavTabs as Tabs } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useState } from "react";
import { FormWidget, WidgetProps } from "../../builder";

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

export function PanelTabs(props: WidgetProps) {
  const { schema, formAtom, readonly } = props;
  const { items } = schema;
  const [activeTab, setActiveTab] = useState<string | null>(null);

  const handleChange = useCallback(function handleChange(
    id: string,
    tab: NavItemProps
  ) {
    setActiveTab(id);
  },
  []);

  const tabItems = useMemo(() => {
    return items!.map((item, ind) => ({
      ...item,
      id: item.name || `tab_item_${ind}`,
    })) as NavItemProps[];
  }, [items]);

  useEffect(() => {
    const [firstItem] = tabItems;
    firstItem && setActiveTab(firstItem.id);
  }, [tabItems]);

  return (
    <Box d="flex" flexDirection="column">
      <Tabs items={tabItems!} value={activeTab!} onChange={handleChange} />
      {tabItems.map((item, index) => {
        const active = activeTab === item.id;
        return (
          <TabContent
            key={item.id || index}
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
