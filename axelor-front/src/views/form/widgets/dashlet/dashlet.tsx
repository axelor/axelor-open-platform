import { selectAtom, useAtomCallback } from "jotai/utils";
import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo } from "react";
import uniqueId from "lodash/uniqueId";

import { Box, clsx } from "@axelor/ui";

import { Tab, TabProps, initTab } from "@/hooks/use-tabs";
import { DataContext } from "@/services/client/data.types";
import { CardsView, Schema } from "@/services/client/meta.types";
import { DashletView } from "@/view-containers/view-dashlet";
import { Views } from "@/view-containers/views";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { useAsync } from "@/hooks/use-async";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useViewTab } from "@/view-containers/views/scope";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { findActionView } from "@/services/client/meta-cache";

import { Attrs, WidgetProps } from "../../builder";
import { DashletActions } from "./dashlet-actions";
import classes from "./dashlet.module.scss";

interface DashletProps {
  schema: Schema;
  className?: string;
  readonly?: boolean;
  dashboard?: boolean;
  attrs?: Attrs;
  getContext?: () => DataContext;
  viewId?: number;
  onViewLoad?: (schema: Schema, viewId?: number, viewType?: string) => void;
}

function DashletTitle({ title }: { title?: string }) {
  const dashlet = useAtomValue(useDashletHandlerAtom());
  return <Box className={classes.title}>{dashlet.title || title}</Box>;
}

export function DashletComponent({
  schema,
  attrs,
  className,
  readonly,
  viewId,
  dashboard,
  onViewLoad,
  getContext,
}: DashletProps): any {
  const { title, action, canSearch, widgetAttrs } = schema;
  const height = schema.height ?? widgetAttrs?.height;
  const { data: tab, state } = useAsync<Tab | null>(async () => {
    const context = getContext?.();
    const actionView = await findActionView(action, context);
    const ctx = {
      ...actionView.context,
      ...context,
    };
    return await initTab({
      ...actionView,
      name: uniqueId("$dashlet"),
      params: {
        dashlet: true,
        "show-toolbar": false,
        "dashlet.canSearch": canSearch,
        ...actionView.params,
      },
      context: {
        ...ctx,
        _id: (ctx?._id ?? ctx?.id)!,
        _model: ctx.model ?? ctx._model,
        _domainAction: action,
      },
    });
  }, [action, canSearch, getContext]);

  const setTabViewProps = useAtomCallback(
    useCallback(
      (
        get,
        set,
        tab: Tab,
        viewType: string,
        param: keyof TabProps,
        value: any
      ) => {
        const props = get(tab.state).props;
        const viewProps = props?.[viewType];
        if (viewProps?.[param] !== value) {
          set(tab.state, {
            props: {
              ...props,
              [viewType]: {
                ...viewProps,
                [param]: value,
              },
            },
          });
        }
      },
      []
    )
  );

  useEffect(() => {
    // for grid view to update readonly to show edit icon
    if (tab && tab?.action?.viewType === "grid") {
      setTabViewProps(tab, "grid", "readonly", readonly);
    }
  }, [tab, readonly, setTabViewProps]);

  useEffect(() => {
    // for html view to update url
    if (tab && tab?.action?.viewType === "html") {
      setTabViewProps(tab, "html", "name", attrs?.url);
    }
  }, [tab, attrs?.url, setTabViewProps]);

  if (state === "loading") return null;

  const { viewType = "" } = tab?.action ?? {};
  const hasSearch = canSearch && ["cards"].includes(viewType);

  return (
    tab && (
      <DashletView>
        <Box
          d="flex"
          flexDirection="column"
          className={clsx(classes.container, className)}
          border
          roundedTop
          style={{ height }}
        >
          <Box
            className={clsx(classes.header, {
              [classes.search]: hasSearch,
            })}
          >
            <DashletTitle title={title || tab?.title} />
            {hasSearch && <DashletSearch />}
            {attrs?.refresh && <DashletRefresh count={attrs.refresh} />}
            <DashletActions
              dashboard={dashboard}
              viewType={viewType}
              showBars={widgetAttrs?.showBars}
            />
            {viewType && onViewLoad && (
              <DashletViewLoad
                schema={schema}
                viewId={viewId}
                viewType={viewType}
                onViewLoad={onViewLoad}
              />
            )}
          </Box>
          <Box className={classes.content}>{tab && <Views tab={tab} />}</Box>
        </Box>
      </DashletView>
    )
  );
}

function DashletRefresh({ count }: { count: number }) {
  const { onRefresh } = useAtomValue(useDashletHandlerAtom());

  useAsyncEffect(async () => {
    count && onRefresh?.();
  }, [count, onRefresh]);

  return null;
}

function DashletSearch() {
  const { view, dataStore, onRefresh, searchAtom } = useAtomValue(
    useDashletHandlerAtom()
  );
  if (!view) return null;
  const { items, customSearch, freeSearch } = view as CardsView;
  return (
    searchAtom &&
    dataStore &&
    onRefresh && (
      <Box d="flex">
        <AdvanceSearch
          stateAtom={searchAtom}
          dataStore={dataStore!}
          items={items}
          customSearch={customSearch}
          freeSearch={freeSearch}
          onSearch={onRefresh}
        />
      </Box>
    )
  );
}

function DashletViewLoad({
  schema,
  viewId,
  viewType,
  onViewLoad,
}: Pick<DashletProps, "schema" | "viewId" | "onViewLoad"> & {
  viewType: string;
}) {
  const { view } = useAtomValue(useDashletHandlerAtom());
  const $viewType = view?.type || viewType;

  useEffect(() => {
    if ($viewType && onViewLoad) {
      onViewLoad(schema, viewId, $viewType);
    }
  }, [schema, viewId, onViewLoad, $viewType]);

  return null;
}

export function Dashlet(props: WidgetProps) {
  const { schema, readonly, widgetAtom, formAtom } = props;
  const tab = useViewTab();
  const { attrs } = useAtomValue(widgetAtom);

  const ready = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.ready), [formAtom])
  );

  const getContext = useAtomCallback(
    useCallback(
      (get) => {
        const ctx = formAtom ? get(formAtom).record : {};
        return {
          _model: tab.action.model,
          ...tab.action.context,
          ...ctx,
        } as DataContext;
      },
      [formAtom, tab.action.context, tab.action.model]
    )
  );

  return (
    ready && (
      <DashletComponent
        schema={schema}
        attrs={attrs}
        readonly={readonly}
        getContext={getContext}
      />
    )
  );
}
