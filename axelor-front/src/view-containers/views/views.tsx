import { atom, useAtomValue, useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { focusAtom } from "jotai-optics";
import { selectAtom } from "jotai/utils";
import { memo, useCallback, useEffect, useMemo } from "react";
import { ErrorBoundary, FallbackProps } from "react-error-boundary";

import { Box, Fade } from "@axelor/ui";

import { ErrorBox } from "@/components/error-box";
import { Loader } from "@/components/loader/loader";
import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useSession } from "@/hooks/use-session";
import { Tab } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { filters as fetchFilters } from "@/services/client/meta";
import { findFields, findView } from "@/services/client/meta-cache";
import {
  AdvancedSearchAtom,
  Property,
  SearchFilter,
  SearchFilters,
} from "@/services/client/meta.types";
import { toCamelCase, toKebabCase } from "@/utils/names";

import { processContextValues } from "@/views/form/builder/utils";
import { AdvancedSearchState } from "../advance-search/types";
import { prepareAdvanceSearchQuery } from "../advance-search/utils";
import { MetaScope, ViewScope } from "./scope";

async function loadComp(viewType: string) {
  const type = toKebabCase(viewType);
  const name = toCamelCase(type);
  const { [name]: Comp } = await import(`../../views/${type}/index.ts`);
  return Comp as React.ElementType;
}

async function loadView(props: {
  type: string;
  name?: string;
  model?: string;
  resource?: string;
}) {
  const { type } = props;
  const meta = await findView(props);
  const Comp = await loadComp(type);
  return { meta, type, Comp };
}

function ViewContainer({
  tab,
  view,
  searchAtom,
  dataStore,
}: {
  tab: Tab;
  view: { name?: string; type: string };
  searchAtom?: AdvancedSearchAtom;
  dataStore?: DataStore;
}) {
  const { model, params } = tab.action;
  const { state, data } = useAsync(
    async () => loadView({ model, ...view }),
    [model, view],
  );

  const forceTitle = params?.["forceTitle"];
  const viewData = data?.meta?.view;
  const viewTitle =
    (!forceTitle &&
      view.type === "form" &&
      view.type === viewData?.type &&
      viewData?.title) ||
    tab.title;
  const viewName = viewData?.name;

  const setTabTitle = useSetAtom(
    useMemo(
      () => focusAtom(tab.state, (state) => state.prop("title")),
      [tab.state],
    ),
  );

  const setTabName = useSetAtom(
    useMemo(
      () => focusAtom(tab.state, (state) => state.prop("name")),
      [tab.state],
    ),
  );

  useEffect(() => {
    viewTitle && setTabTitle(viewTitle);
    setTabName(viewName);
  }, [viewTitle, viewName, setTabTitle, setTabName]);

  if (state === "loading" || data?.type !== view.type) {
    return <Loader />;
  }

  const { meta, Comp } = data;

  if (Comp) {
    return (
      <Fade in={true} timeout={400} mountOnEnter>
        <Box
          data-view-id={tab.id}
          d="flex"
          flex={1}
          style={{ minWidth: 0, minHeight: 0 }}
        >
          <ScopeProvider scope={MetaScope} value={meta}>
            <Comp meta={meta} dataStore={dataStore} searchAtom={searchAtom} />
          </ScopeProvider>
        </Box>
      </Fade>
    );
  }

  return null;
}

function ViewError({ error, resetErrorBoundary }: FallbackProps) {
  const session = useSession();
  const handleReset = useCallback(
    () => resetErrorBoundary(),
    [resetErrorBoundary],
  );
  return (
    <ErrorBox
      status={500}
      error={session.data?.user?.technical ? error : undefined}
      onReset={handleReset}
    />
  );
}

function ViewPane({
  tab,
  dataStore,
  searchAtom,
}: {
  tab: Tab;
  dataStore?: DataStore;
  searchAtom?: AdvancedSearchAtom;
}) {
  const {
    action: { views = [] },
  } = tab;
  const typeAtom = useMemo(() => {
    return selectAtom(tab.state, (x) => x.type);
  }, [tab.state]);

  const type = useAtomValue(typeAtom);
  const view = useMemo(() => views.find((x) => x.type === type), [type, views]);

  if (view) {
    return (
      <ScopeProvider scope={ViewScope} value={tab}>
        <ErrorBoundary FallbackComponent={ViewError}>
          <ViewContainer
            view={view}
            tab={tab}
            dataStore={dataStore}
            searchAtom={searchAtom}
          />
        </ErrorBoundary>
      </ScopeProvider>
    );
  }

  return null;
}

const DataViews = memo(function DataViews({
  tab,
  model,
}: {
  tab: Tab;
  model: string;
}) {
  const {
    action: { name: actionName, domain, context, params },
  } = tab;
  const limit = +params?.limit || 0;

  const dataStore = new DataStore(model, {
    ...(limit && { limit }),
    filter: {
      _domain: domain,
      ...(context && {
        _domainContext: processContextValues({ _model: model, ...context }),
      }),
    },
  });

  // advanced search
  const searchAtom = useMemo<AdvancedSearchAtom>(
    () =>
      atom<AdvancedSearchState>({
        search: {},
        editor: { criteria: [] },
      }),
    [],
  );
  const setSearchState = useSetAtom(searchAtom);

  const filterName = params?.["search-filters"];
  const defaultSearchFilter = params?.["default-search-filters"];
  const dashlet = actionName?.startsWith("$dashlet");
  const selector = actionName?.startsWith("$selector");
  const dashletSearch = params?.["dashlet.canSearch"];
  const showArchived = params?.["showArchived"];
  const hasAdvanceSearch = !selector && (dashlet ? dashletSearch : true);

  useAsyncEffect(async () => {
    if (!hasAdvanceSearch) return;
    const { fields = {}, jsonFields = {} } = await findFields(
      model,
      context?.jsonModel,
    );
    const dashletActionName =
      dashlet && context?._domainAction ? `act:${context._domainAction}` : "";
    const filters = await fetchFilters(
      filterName || dashletActionName || `act:${actionName}`,
    );

    setSearchState((state) => ({
      ...state,
      filters,
      fields: { ...state.fields, ...fields },
      jsonFields: { ...state.jsonFields, ...jsonFields },
    }));
  }, [filterName, actionName, hasAdvanceSearch]);

  useAsyncEffect(async () => {
    let domains: SearchFilter[] = [];
    let items: Property[] = [];
    let fields: Record<string, Property> = {};

    if (hasAdvanceSearch && filterName) {
      const selectedDomains: string[] = (defaultSearchFilter || "")?.split(",");
      const res = await findView<SearchFilters>({
        name: filterName,
        type: "search-filters",
        model,
      });
      fields = res?.fields || {};
      items = (res?.view?.items || []).map((item) => {
        const field = fields[item.name || ""] || {};
        return {
          ...field,
          ...item,
          type: field.type ?? "STRING",
        } as Property;
      });
      domains = (res?.view?.filters || []).map((d) => ({
        ...d,
        checked: selectedDomains.includes(d.name!),
      }));
    }

    setSearchState((_state) => {
      const state = {
        ..._state,
        domains,
        items,
        archived: showArchived,
        fields: {
          ..._state.fields,
          ...fields,
        },
      };
      return { ...state, ...prepareAdvanceSearchQuery(state) };
    });
  }, [
    model,
    showArchived,
    hasAdvanceSearch,
    defaultSearchFilter,
    filterName,
    setSearchState,
  ]);

  return <ViewPane tab={tab} dataStore={dataStore} searchAtom={searchAtom} />;
});

export const Views = memo(function Views({ tab }: { tab: Tab }) {
  const model = tab.action.model;
  return model ? <DataViews tab={tab} model={model} /> : <ViewPane tab={tab} />;
});
