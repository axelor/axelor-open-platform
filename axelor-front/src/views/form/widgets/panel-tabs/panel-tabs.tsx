import { ScopeProvider } from "bunshi/react";
import { produce } from "immer";
import { useAtom, useAtomValue } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import {
  memo,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";

import { Box, NavTabItem, NavTabs, clsx } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

import { createSchemaTestId, useSchemaTestId } from "@/hooks/use-testid";
import {
  FieldLabelTitle,
  FormAtom,
  FormWidget,
  HelpPopover,
  WidgetAtom,
  WidgetProps,
} from "../../builder";
import {
  fallbackWidgetAtom,
  useWidgetAttrsAtomByName,
} from "../../builder/atoms";
import {
  FormTabScope,
  useAfterActions,
  useFormRefresh,
  useFormScope,
} from "../../builder/scope";

import styles from "./panel-tabs.module.scss";

export function PanelTabs(props: WidgetProps) {
  const { schema, formAtom, widgetAtom, readonly } = props;
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [tabTitles, setTabTitles] = useState<
    Record<string, string | undefined>
  >({});

  const shouldRefocus = useRef<boolean | null>(true);
  const lastActiveTab = useRef<string>(null);

  const hidden = useAtomValue(widgetAtom).attrs?.hidden;
  const parentId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom]),
  );

  const isFormDirty = useAtomCallback(
    useCallback((get) => get(formAtom).dirty, [formAtom]),
  );

  const setTabActive = useCallback((tabId: string) => {
    // once tab is selected manually by user click
    // then auto re-focus will be disabled
    shouldRefocus.current = null;
    lastActiveTab.current = tabId;
    setActiveTab(tabId);
  }, []);

  const handleChange = useCallback(
    (item: NavTabItem) => setTabActive(item.id),
    [setTabActive],
  );

  const tabs = useMemo(
    () =>
      (schema.items || []).map(
        (tab) =>
          ({
            ...tab,
            id: tab.uid,
          }) as unknown as Schema,
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

  const setTabTitle = useCallback((id: string, title?: string) => {
    setTabTitles((titles) => {
      if (title || titles[id]) {
        return { ...titles, [id]: title };
      }
      return titles;
    });
  }, []);

  const visibleTabs = useMemo(
    () =>
      tabs
        .filter((item) => !hiddenTabs[item.uid])
        .map((item) => {
          // remove showIf/hideIf to avoid double evaluation
          const { showIf, hideIf, title, ...rest } = item;
          return {
            ...rest,
            title: tabTitles[item.id] || title,
            panelTabSchema: item,
          } as Schema;
        }),
    [hiddenTabs, tabs, tabTitles],
  );

  useEffect(() => {
    // check for last selected tab (by user) is not hidden
    // then it should select that tab
    const lastSelectedId = lastActiveTab.current;
    const isLastSelectedPresent = lastSelectedId && !hiddenTabs[lastSelectedId];

    if (isLastSelectedPresent) {
      return setActiveTab(lastSelectedId);
    }

    if (visibleTabs.some((item) => item.uid === activeTab)) {
      if (shouldRefocus.current) {
        const activeIndex = visibleTabs.findIndex((t) => t.uid === activeTab);
        const firstTab = visibleTabs.find((t, i) => i < activeIndex);
        const formDirty = isFormDirty();
        if (firstTab || formDirty) {
          shouldRefocus.current = false;
          // after any changes to form it should not shift the focus
          if (!formDirty && firstTab) {
            setActiveTab(firstTab.uid);
          }
        }
      }
      return;
    }

    const timer = setTimeout(() => {
      if (activeTab) {
        const index = tabs.findIndex((item) => item.uid === activeTab);
        let prevIndex = index - 1;
        while (prevIndex >= 0) {
          const prev = tabs[prevIndex];
          if (!hiddenTabs[prev.uid]) {
            setActiveTab(prev.uid);
            return;
          }
          prevIndex--;
        }
      }
      const first = visibleTabs[0];
      if (first) {
        setActiveTab(first.uid ?? null);
      }
    }, 50);
    return () => clearTimeout(timer);
  }, [activeTab, hiddenTabs, tabs, visibleTabs, isFormDirty]);

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

  const resetTabRefocus = useCallback(() => {
    if (shouldRefocus.current === false) {
      shouldRefocus.current = true;
    }
  }, []);

  useFormRefresh(resetTabRefocus);
  useEffect(resetTabRefocus, [resetTabRefocus, parentId]);

  useEffect(() => {
    handleOnSelect(visibleTabs, activeTab);
  }, [activeTab, handleOnSelect, visibleTabs]);

  const prefixId = useId();
  const testId = useSchemaTestId(schema, "panel-tabs");

  const navTabs = useMemo(
    () =>
      visibleTabs.map((tab, index) => {
        const id = `${prefixId}-${index}`;
        const tabTestId = createSchemaTestId(tab, "tab") ?? `tab:${index}`;
        return {
          ...tab,
          ...(tab.title && {
            title: (
              <HelpPopover
                schema={tab}
                formAtom={formAtom}
                widgetAtom={fallbackWidgetAtom}
              >
                <Box p={1} id={`${id}-title`}>
                  <FieldLabelTitle title={tab.title} />
                </Box>
              </HelpPopover>
            ),
          }),
          htmlProps: {
            id,
            "aria-controls": `${id}-panel`,
            "aria-labelledby": `${id}-title`,
            "data-testid": tabTestId,
          },
        };
      }) as NavTabItem[],
    [visibleTabs, prefixId, formAtom],
  );

  if (hidden) return;

  return (
    <div className={styles.tabs}>
      <NavTabs
        items={navTabs}
        active={activeTab ?? undefined}
        onItemClick={handleChange}
        data-testid={testId}
      />
      <DummyTabs
        tabs={tabs}
        formAtom={formAtom}
        parentAtom={widgetAtom}
        setHidden={setHidden}
        setActive={setTabActive}
        setTitle={setTabTitle}
      />
      {visibleTabs.map((item, index) => {
        const active = activeTab === item.uid;
        const tab = navTabs[index];
        return (
          <TabContent
            key={item.uid}
            item={tab}
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
  item,
  schema,
  active,
  formAtom,
  parentAtom,
  readonly,
}: {
  item: NavTabItem;
  schema: Schema;
  active: boolean;
  formAtom: WidgetProps["formAtom"];
  parentAtom: WidgetProps["parentAtom"];
  readonly?: boolean;
}) {
  const display = active ? "block" : "none";

  const id = item.htmlProps?.["aria-controls"];
  const labelledBy = item.htmlProps?.["aria-labelledby"];

  return (
    <div
      className={clsx(styles.tabContent, {
        [styles.noRadius]: schema.widgetAttrs?.displayMode === "tree",
      })}
      style={{ display }}
      role="tabpanel"
      id={id}
      aria-labelledby={labelledBy}
    >
      <ScopeProvider scope={FormTabScope} value={{ active }}>
        <FormWidget
          readonly={readonly}
          schema={schema}
          formAtom={formAtom}
          parentAtom={parentAtom}
        />
      </ScopeProvider>
    </div>
  );
}

// used to handle hidden state
const DummyTabs = memo(function DummyTabs({
  tabs,
  formAtom,
  parentAtom,
  setHidden,
  setActive,
  setTitle,
}: {
  tabs: Schema[];
  formAtom: FormAtom;
  parentAtom: WidgetAtom;
  setHidden: (id: string, hidden: boolean) => void;
  setActive: (id: string) => void;
  setTitle: (id: string, title?: string) => void;
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
            <DummyTab
              {...props}
              setHidden={setHidden}
              setActive={setActive}
              setTitle={setTitle}
            />
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
    setTitle: (id: string, title?: string) => void;
  },
) {
  const { schema, formAtom, widgetAtom, setHidden, setActive, setTitle } =
    props;
  const hidden = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs.hidden), [widgetAtom]),
  );
  const title = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs.title), [widgetAtom]),
  );

  const widgetAttrsAtom = useWidgetAttrsAtomByName({ schema, formAtom });
  const [{ active }, setWidgetAttrsByName] = useAtom(widgetAttrsAtom);

  useEffect(() => {
    setHidden(schema.uid, !!hidden);
  }, [hidden, schema.uid, setHidden]);

  useEffect(() => {
    setTitle(schema.uid, title);
  }, [title, schema.uid, setTitle]);

  useEffect(() => {
    if (active) {
      if (!hidden) {
        setActive(schema.uid);
      }
      // reset active in statesByName
      setWidgetAttrsByName((_attrs) => {
        if (_attrs.active) {
          _attrs.active = false;
        }
      });
    }
  }, [active, schema.uid, hidden, setActive, setWidgetAttrsByName]);

  return null;
});
