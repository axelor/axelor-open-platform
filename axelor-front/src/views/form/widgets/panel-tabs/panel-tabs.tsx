import { produce } from "immer";
import { useAtom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { focusAtom } from "jotai-optics";
import { memo, useCallback, useEffect, useMemo, useState } from "react";

import { Fade, NavTabItem, NavTabs } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

import { FormAtom, FormWidget, WidgetAtom, WidgetProps } from "../../builder";
import { useAfterActions, useFormScope } from "../../builder/scope";

import styles from "./panel-tabs.module.scss";

export function PanelTabs(props: WidgetProps) {
  const { schema, formAtom, widgetAtom, readonly } = props;
  const [activeTab, setActiveTab] = useState<string | null>(null);

  const hidden = useAtomValue(widgetAtom).attrs?.hidden;

  const handleChange = useCallback(
    (item: NavTabItem) => setActiveTab(item.id),
    [],
  );

  const tabs = useMemo(
    () =>
      (schema.items || []).map(
        (tab) =>
          ({
            ...tab,
            id: tab.uid,
          }) as Schema,
      ),
    [schema],
  );

  const [hiddenTabs, setHiddenTabs] = useState<Record<string, boolean>>(() =>
    tabs
      .filter((x) => x.hidden)
      .reduce((acc, item) => ({ ...acc, [item.uid]: true }), {}),
  );

  const setHidden = useCallback((id: string, hidden: boolean) => {
    setHiddenTabs((prev) => {
      return produce(prev, (draft) => {
        draft[id] = hidden;
      });
    });
  }, []);

  const visibleTabs = useMemo(
    () =>
      tabs
        .filter((item) => !hiddenTabs[item.uid])
        .map((item) => {
          // remove showIf/hideIf to avoid double evaluation
          const { showIf, hideIf, ...rest } = item;
          return rest as Schema;
        }),
    [hiddenTabs, tabs],
  );

  useEffect(() => {
    if (visibleTabs.some((item) => item.uid === activeTab)) return;
    const timer = setTimeout(() => {
      if (activeTab) {
        let index = tabs.findIndex((item) => item.uid === activeTab);
        let prevIndex = index - 1;
        while (prevIndex >= 0) {
          let prev = tabs[prevIndex];
          if (!hiddenTabs[prev.uid]) {
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
    }, 50);
    return () => clearTimeout(timer);
  }, [activeTab, hiddenTabs, tabs, visibleTabs]);

  const { actionExecutor } = useFormScope();
  const handleOnSelect = useAfterActions(
    useCallback(
      async (tabs: Schema[], tabId: string | null) => {
        const tab = tabs.find((x) => x.uid === tabId);
        if (tab?.onTabSelect) {
          actionExecutor.execute(tab.onTabSelect);
        }
      },
      [actionExecutor],
    ),
  );

  useEffect(() => {
    handleOnSelect(visibleTabs, activeTab);
  }, [activeTab, handleOnSelect, visibleTabs]);

  if (hidden) return;

  return (
    <div className={styles.tabs}>
      <NavTabs
        items={visibleTabs as NavTabItem[]}
        active={activeTab ?? undefined}
        onItemClick={handleChange}
      />
      <DummyTabs
        tabs={tabs}
        formAtom={formAtom}
        parentAtom={widgetAtom}
        setHidden={setHidden}
        setActive={setActiveTab}
      />
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

// used to handle hidden state
const DummyTabs = memo(function DummyTabs({
  tabs,
  formAtom,
  parentAtom,
  setHidden,
  setActive,
}: {
  tabs: Schema[];
  formAtom: FormAtom;
  parentAtom: WidgetAtom;
  setHidden: (id: string, hidden: boolean) => void;
  setActive: (id: string) => void;
}) {
  const items = useMemo(() => {
    return tabs.map((item) => {
      const { id, uid, name, hidden, showIf, hideIf } = item;
      return {
        id,
        uid,
        name,
        hidden,
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
          render={(props) => (
            <DummyTab {...props} setHidden={setHidden} setActive={setActive} />
          )}
        />
      ))}
    </div>
  );
});

const DummyTab = memo(function DummyTab(
  props: WidgetProps & {
    setHidden: (id: string, hidden: boolean) => void;
    setActive: (id: string) => void;
  },
) {
  const { schema, widgetAtom, setHidden, setActive } = props;
  const hidden = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs.hidden), [widgetAtom]),
  );
  const [active, setActiveAttr] = useAtom(
    useMemo(
      () =>
        focusAtom(widgetAtom, (o) =>
          o.prop("attrs").valueOr({ active: false }).prop("active"),
        ),
      [widgetAtom],
    ),
  );

  useEffect(() => {
    setHidden(schema.uid, !!hidden);
  }, [hidden, schema.uid, setHidden]);

  useEffect(() => {
    if (active) {
      !hidden && setActive(schema.uid);
      setActiveAttr(false);
    }
  }, [active, schema.uid, hidden, setActive, setActiveAttr]);

  return null;
});
