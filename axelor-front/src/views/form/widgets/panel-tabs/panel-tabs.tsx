import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { isEqual } from "lodash";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Box, NavItemProps, NavTabs as Tabs } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

import { FormWidget, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";

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
  const [mount, setMount] = useState(false);
  const display = active ? "block" : "none";

  useEffect(() => {
    active && setMount(true);
  }, [active]);

  return (
    <>
      {mount && (
        <Box d={display} pt={3}>
          <FormWidget readonly={readonly} schema={schema} formAtom={formAtom} />
        </Box>
      )}
    </>
  );
}

export function PanelTabs(props: WidgetProps) {
  const { schema, formAtom, readonly } = props;
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const handleChange = useCallback((id: string) => setActiveTab(id), []);

  const items = useMemo(() => schema.items || [], [schema]);
  const tabItems = useMemo(
    () =>
      items.map((item) => ({
        ...item,
        id: item.uid,
      })) as NavItemProps[],
    [items]
  );

  const hiddenStateAtom = useMemo(() => {
    return selectAtom(
      formAtom,
      (formState) => {
        const { states = {}, statesByName = {} } = formState;
        return tabItems.reduce((acc, item: Schema) => {
          const attrs = item.name
            ? {
                hidden: item.hidden,
                ...statesByName[item.name]?.attrs,
                ...states[item.uid]?.attrs,
              }
            : item;
          if (attrs.hidden) {
            acc[item.uid] = true;
          }
          return acc;
        }, {} as Record<string, boolean>);
      },
      isEqual
    );
  }, [formAtom, tabItems]);

  const hiddenState = useAtomValue(hiddenStateAtom);
  const visibleTabs = useMemo(
    () => tabItems.filter((item) => !hiddenState[item.id]),
    [hiddenState, tabItems]
  );

  const { actionHandler } = useFormScope();

  useEffect(() => {
    return actionHandler.subscribe((data) => {
      if (
        data.type === "attr" &&
        data.name === "active" &&
        data.value &&
        items.some((item: Schema) => item.name === data.target)
      ) {
        let item = items.find((item: Schema) => item.name === data.target);
        if (item) {
          setActiveTab(item.uid);
        }
      }
    });
  }, [actionHandler, items]);

  useEffect(() => {
    if (visibleTabs.some((item) => item.id === activeTab)) return;
    if (activeTab) {
      let index = items.findIndex((item) => item.uid === activeTab);
      let prevIndex = index - 1;
      while (prevIndex >= 0) {
        let prev = items[prevIndex];
        if (!hiddenState[prev.uid]) {
          setActiveTab(prev.uid);
          return;
        }
        prevIndex--;
      }
    }
    let first = visibleTabs[0];
    if (first) {
      setActiveTab(first.id ?? null);
    }
  }, [activeTab, hiddenState, items, visibleTabs]);

  return (
    <Box d="flex" flexDirection="column">
      <Tabs
        items={visibleTabs}
        value={activeTab ?? undefined}
        onChange={handleChange}
      />
      {visibleTabs.map((item) => {
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
