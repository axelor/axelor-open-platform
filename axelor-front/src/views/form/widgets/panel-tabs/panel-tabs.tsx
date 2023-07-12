import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import isEqual from "lodash/isEqual";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Fade, NavTabItem, NavTabs } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

import { FormAtom, FormWidget, WidgetAtom, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";

import styles from "./panel-tabs.module.scss";

export function PanelTabs(props: WidgetProps) {
  const { schema, formAtom, widgetAtom, readonly } = props;
  const [activeTab, setActiveTab] = useState<string | null>(null);

  const handleChange = useCallback(
    (item: NavTabItem) => setActiveTab(item.id),
    []
  );

  const tabs = useMemo(
    () =>
      (schema.items || []).map(
        (tab) =>
          ({
            ...tab,
            id: tab.uid,
          } as Schema)
      ),
    [schema]
  );

  const hiddenStateAtom = useMemo(() => {
    return selectAtom(
      formAtom,
      (formState) => {
        const { states = {}, statesByName = {} } = formState;
        return tabs.reduce((acc, item) => {
          const attrs = {
            hidden: item.hidden,
            ...statesByName[item.name ?? ""]?.attrs,
            ...states[item.uid]?.attrs,
          };
          if (attrs.hidden) {
            acc[item.uid] = true;
          }
          return acc;
        }, {} as Record<string, boolean>);
      },
      isEqual
    );
  }, [formAtom, tabs]);

  const hiddenState = useAtomValue(hiddenStateAtom);
  const visibleTabs = useMemo(
    () =>
      tabs
        .filter((item) => !hiddenState[item.uid])
        .map((item) => {
          // remove showIf/hideIf to avoid double evaluation
          const { showIf, hideIf, ...rest } = item;
          return rest as Schema;
        }),
    [hiddenState, tabs]
  );

  const { actionHandler } = useFormScope();

  useEffect(() => {
    return actionHandler.subscribe((data) => {
      if (
        data.type === "attr" &&
        data.name === "active" &&
        data.value &&
        tabs.some((item) => item.name === data.target)
      ) {
        let item = tabs.find((item) => item.name === data.target);
        if (item) {
          setActiveTab(item.uid);
        }
      }
    });
  }, [actionHandler, tabs]);

  useEffect(() => {
    if (visibleTabs.some((item) => item.uid === activeTab)) return;
    if (activeTab) {
      let index = tabs.findIndex((item) => item.uid === activeTab);
      let prevIndex = index - 1;
      while (prevIndex >= 0) {
        let prev = tabs[prevIndex];
        if (!hiddenState[prev.uid]) {
          setActiveTab(prev.uid);
          return;
        }
        prevIndex--;
      }
    }
    let first = visibleTabs[0];
    if (first) {
      setActiveTab(first.uid ?? null);
    }
  }, [activeTab, hiddenState, tabs, visibleTabs]);

  return (
    <div className={styles.tabs}>
      <NavTabs
        items={visibleTabs as NavTabItem[]}
        active={activeTab ?? undefined}
        onItemClick={handleChange}
      />
      <DummyTabs tabs={tabs} formAtom={formAtom} parentAtom={widgetAtom} />
      {visibleTabs.map((item) => {
        const active = activeTab === item.uid;
        return (
          <TabContent
            key={item.uid}
            schema={item}
            active={active}
            formAtom={formAtom}
            parentAtom={widgetAtom}
            readonly={readonly}
          />
        );
      })}
    </div>
  );
}

function TabContent({
  schema,
  active,
  formAtom,
  parentAtom,
  readonly,
}: {
  schema: Schema;
  active: boolean;
  formAtom: WidgetProps["formAtom"];
  parentAtom: WidgetProps["parentAtom"];
  readonly?: boolean;
}) {
  const display = active ? "block" : "none";

  return (
    <Fade in={active} mountOnEnter>
      <div className={styles.tabContent} style={{ display }}>
        <FormWidget
          readonly={readonly}
          schema={schema}
          formAtom={formAtom}
          parentAtom={parentAtom}
        />
      </div>
    </Fade>
  );
}

// required for showIf/hideIf
function DummyTabs({
  tabs,
  formAtom,
  parentAtom,
}: {
  formAtom: FormAtom;
  parentAtom: WidgetAtom;
  tabs: Schema[];
}) {
  const items = useMemo(() => {
    return tabs.map((item) => {
      const { id, uid, name, showIf, hideIf } = item;
      return {
        id,
        uid,
        name,
        showIf,
        hideIf,
        widget: "spacer",
      } as Schema;
    });
  }, [tabs]);

  return (
    <div style={{ display: "none" }}>
      {items.map((item) => (
        <FormWidget
          key={item.uid}
          schema={item}
          formAtom={formAtom}
          parentAtom={parentAtom}
        />
      ))}
    </div>
  );
}
