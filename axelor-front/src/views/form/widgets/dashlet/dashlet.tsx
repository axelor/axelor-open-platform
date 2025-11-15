import { useAtomValue } from "jotai";
import { ScopeProvider } from "bunshi/react";
import { selectAtom, useAtomCallback } from "jotai/utils";
import uniqueId from "lodash/uniqueId";
import pick from "lodash/pick";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { produce } from "immer";
import { useSchemaTestId } from "@/hooks/use-testid";

import { Box, clsx } from "@axelor/ui";

import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { Tab, TabProps, initTab } from "@/hooks/use-tabs";
import { DataContext } from "@/services/client/data.types";
import { findActionView } from "@/services/client/meta-cache";
import { CardsView, Schema } from "@/services/client/meta.types";
import { AdvanceSearch } from "@/view-containers/advance-search";
import { DashletView } from "@/view-containers/view-dashlet";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { PopupScope } from "@/view-containers/view-popup/handler";
import { Views } from "@/view-containers/views";
import { useViewAction, useViewTab } from "@/view-containers/views/scope";
import { isUndefined } from "@/utils/types";

import {
  Attrs,
  FieldLabelTitle,
  HelpPopover,
  WidgetProps,
  usePermission,
  usePrepareContext,
} from "../../builder";
import {
  useAfterActions,
  useFormScope,
  useFormTabScope,
} from "../../builder/scope";
import { DashletActions } from "./dashlet-actions";
import { createWidgetAtom } from "../../builder/atoms";
import { processContextValues } from "../../builder/utils";
import classes from "./dashlet.module.scss";

interface DashletProps {
  schema: Schema;
  className?: string;
  popup?: boolean;
  readonly?: boolean;
  canNew?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  dashboard?: boolean;
  attrs?: Attrs;
  viewContext?: DataContext;
  getContext?: () => DataContext;
  viewId?: number;
  onViewLoad?: (schema: Schema, viewId?: number, viewType?: string) => void;
}

function DashletTitle({
  title,
  model,
  schema,
}: {
  title?: string;
  model?: string;
  schema: Schema;
}) {
  const { formAtom } = useFormScope();
  const widgetAtom = useRef(createWidgetAtom({ schema, formAtom })).current;

  const dashlet = useAtomValue(useDashletHandlerAtom());
  const displayTitle = dashlet.title || title;
  const $schema = useMemo(() => ({ ...schema, model }), [schema, model]);

  return (
    <Box className={classes.title}>
      <HelpPopover
        title={displayTitle}
        schema={$schema}
        formAtom={formAtom}
        widgetAtom={widgetAtom}
      >
        <FieldLabelTitle title={displayTitle} />
      </HelpPopover>
    </Box>
  );
}

export function DashletComponent({
  schema,
  attrs,
  className,
  readonly,
  canNew,
  canEdit,
  canDelete,
  popup,
  viewId,
  viewContext,
  dashboard,
  onViewLoad,
  getContext,
}: DashletProps): any {
  const { title, action, canSearch, widgetAttrs } = schema;
  const height = schema.height ?? widgetAttrs?.height;
  
  const testId = useSchemaTestId(schema, "dashlet");

  const load = useAfterActions(
    useCallback(async () => {
      const context = getContext?.();
      const actionView = await findActionView(action, context, {
        silent: true,
      });
      const ctx = {
        ...actionView.context,
        ...context,
      };
      const { _id, _showRecord } = actionView.context || {};
      return await initTab({
        ...actionView,
        name: uniqueId("$dashlet"),
        params: {
          ...actionView.params,
          dashlet: true,
          "show-toolbar": false,
          "dashlet.canSearch": canSearch,
          "dashlet.params": actionView.params,
          ...(popup && {
            "dashlet.in.popup": popup,
          }),
        },
        context: {
          ...(dashboard
            ? ctx
            : {
                ...processContextValues(viewContext ?? {}),
                ...actionView.context,
                _id: _id || viewContext?._id,
                _showRecord,
              }),
          ...pick(ctx, ["_viewName", "_viewType", "_views"]),
          _model: ctx.model ?? ctx._model,
          _domainAction: action,
        },
      });
    }, [dashboard, action, popup, canSearch, viewContext, getContext]),
  );

  const { data: tab, state } = useAsync(load, [load]);

  const setTabViewProps = useAtomCallback(
    useCallback(
      (get, set, tab: Tab, viewType: string, params: Partial<TabProps>) => {
        const tabState = get(tab.state);
        set(
          tab.state,
          produce(tabState, (draft) => {
            if (!draft.props) draft.props = {};
            if (!draft.props[viewType]) draft.props[viewType] = {};
            const viewProps = draft.props[viewType];

            for (const [param, value] of Object.entries<any>(params)) {
              const oldValue = viewProps[param as keyof TabProps];
              if (oldValue !== value) {
                viewProps[param as keyof TabProps] = value;
              }
            }
          }),
        );
      },
      [],
    ),
  );

  useEffect(() => {
    // for grid view to update readonly to show edit icon
    const tabProps = { canNew, canEdit, canDelete };
    if (tab && tab?.action?.viewType === "grid") {
      setTabViewProps(tab, "grid", {
        ...tabProps,
        readonly: Boolean(readonly || schema.readonly),
      });
    } else if (
      tab &&
      ["cards", "calendar", "kanban"].includes(tab?.action?.viewType)
    ) {
      setTabViewProps(tab, tab.action.viewType, tabProps);
    }
  }, [
    tab,
    schema.readonly,
    readonly,
    canNew,
    canEdit,
    canDelete,
    setTabViewProps,
  ]);

  useEffect(() => {
    // for html view to update url
    if (tab && tab?.action?.viewType === "html") {
      setTabViewProps(tab, "html", { name: attrs?.url });
    }
  }, [tab, attrs?.url, setTabViewProps]);

  if (state === "loading") return null;

  const { viewType = "" } = tab?.action ?? {};
  const hasSearch = canSearch && ["cards"].includes(viewType);

  return (
    tab && (
      <DashletView>
        <Box
          data-testid={testId}
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
            <DashletTitle
              schema={schema}
              model={tab.action.model}
              title={attrs?.title ?? (title || tab?.title)}
            />
            {hasSearch && <DashletSearch />}
            <DashletRefresh count={attrs?.refresh ?? 0} />
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
          <Box className={classes.content}>
            <ScopeProvider scope={PopupScope} value={{}}>
              {tab && <Views tab={tab} />}
            </ScopeProvider>
          </Box>
        </Box>
      </DashletView>
    )
  );
}

function DashletRefresh({ count }: { count: number }) {
  const { onRefresh } = useAtomValue(useDashletHandlerAtom());
  const doRefresh = useAfterActions(
    useCallback(async () => onRefresh?.(), [onRefresh]),
  );
  const hasGridInitialized = Boolean(onRefresh);
  const initDashlet = useRef(false);

  useAsyncEffect(async () => {
    // Prevent unnecessary dashlet reload during initial grid setup.
    // The grid initialization itself loads the dashlet data.
    // Once the grid is fully initialized, mark it as ready (initDashlet = true)
    // so that subsequent changes to `count` trigger a refresh.
    if (initDashlet.current && count) {
      doRefresh();
    }
    if (hasGridInitialized) {
      initDashlet.current = true;
    }
  }, [count, doRefresh]);
  
  return null;
}

function DashletSearch() {
  const { view, dataStore, onRefresh, searchAtom } = useAtomValue(
    useDashletHandlerAtom(),
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
  const { active } = useFormTabScope();
  const [show, setShow] = useState(active);

  useEffect(() => {
    setShow((prev) => prev || active);
  }, [active]);

  return show ? <DashletWrapper {...props} /> : null;
}

function DashletWrapper(props: WidgetProps) {
  const { schema, readonly, widgetAtom, formAtom } = props;
  const tab = useViewTab();
  const { attrs } = useAtomValue(widgetAtom);
  const { hasButton } = usePermission(schema, widgetAtom);

  const ready = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.ready), [formAtom]),
  );

  const viewContext = useViewAction()?.context;
  const getFormContext = usePrepareContext(formAtom);

  const getContext = useCallback(() => {
    return {
      _model: tab.action.model,
      ...getFormContext(),
    } as DataContext;
  }, [tab.action.model, getFormContext]);

  // Be able to edit even if readonly mode
  const canEdit =
    (isUndefined(schema.canEdit) ? !readonly : schema.canEdit) &&
    hasButton("edit");
  // Be able to create/delete if explicitly defined and not in readonly mode
  const canNew = !readonly && schema.canNew !== undefined && hasButton("new");
  const canDelete =
    !readonly && schema.canDelete !== undefined && hasButton("delete");

  return (
    ready && (
      <DashletComponent
        {...{
          schema,
          attrs,
          getContext,
          viewContext,
          canNew,
          canEdit,
          canDelete,
        }}
        popup={tab.popup}
        readonly={!canEdit}
      />
    )
  );
}
