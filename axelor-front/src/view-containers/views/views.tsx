import { useAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { memo, useMemo } from "react";

import { Box, Fade } from "@axelor/ui";

import { Loader } from "@/components/loader/loader";
import { useAsync } from "@/hooks/use-async";
import { Tab } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { findView } from "@/services/client/meta-cache";
import { filters as fetchFilters } from "@/services/client/meta";
import {
  SearchFilter,
  SearchFilters,
} from "@/services/client/meta.types";
import { toCamelCase, toKebabCase } from "@/utils/names";

import { ViewScope } from "./scope";

function useViewComp(viewType: string) {
  return useAsync(async () => {
    const type = toKebabCase(viewType);
    const name = toCamelCase(type);
    const { [name]: Comp } = await import(`../../views/${type}/index.ts`);
    return Comp as React.ElementType;
  }, [viewType]);
}

function useViewSchema({
  name,
  type,
  model,
}: {
  type: string;
  name?: string;
  model?: string;
}) {
  return useAsync(
    async () => findView({ type, name, model }),
    [type, name, model]
  );
}

function ViewContainer({
  tab,
  view,
  dataStore,
  domains,
}: {
  tab: Tab;
  view: { name?: string; type: string };
  dataStore?: DataStore;
  domains?: SearchFilter[];
}) {
  const { model } = tab.action;

  const viewSchema = useViewSchema({ model, ...view });
  const viewComp = useViewComp(view.type);

  if (viewSchema.state === "loading" || viewComp.state === "loading") {
    return <Loader />;
  }

  const meta = viewSchema.data;
  const Comp = viewComp.data;

  if (Comp) {
    return (
      <Fade in={true} timeout={400} mountOnEnter>
        <Box d="flex" flex={1} style={{ minWidth: 0, minHeight: 0 }}>
          <Comp meta={meta} dataStore={dataStore} domains={domains} />
        </Box>
      </Fade>
    );
  }

  return null;
}

function ViewPane({ tab, dataStore }: { tab: Tab; dataStore?: DataStore }) {
  const {
    action: { name: actionName, views = [], params, model },
  } = tab;
  const tabAtom = tab.state;
  const [viewState, setViewState] = useAtom(tabAtom);
  const view = useMemo(
    () => views.find((x) => x.type === viewState.type),
    [viewState.type, views]
  );

  const filterName = (params || {})["search-filters"];

  useAsync<void>(async () => {
    const name = filterName || `act:${actionName}`;
    const filters = await fetchFilters(name);
    setViewState({
      filters: filters,
    });
  }, [actionName, filterName]);

  const { data: searchFilters } = useAsync<SearchFilter[]>(async () => {
    if (!filterName) return [];
    const res = await findView<SearchFilters>({
      name: filterName,
      type: "search-filters",
      model,
    });
    return (res?.view?.filters || []).map((filter) => ({
      ...filter,
      id: filter.name,
    }));
  }, [model, filterName]);

  if (view) {
    return (
      <ScopeProvider scope={ViewScope} value={tab}>
        <ViewContainer
          view={view}
          tab={tab}
          dataStore={dataStore}
          domains={searchFilters}
        />
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
    action: { domain, context },
  } = tab;
  const dataStore = new DataStore(model, {
    filter: {
      _domain: domain,
      _domainContext: context,
    },
  });
  return <ViewPane tab={tab} dataStore={dataStore} />;
});

export const Views = memo(function Views({ tab }: { tab: Tab }) {
  const model = tab.action.model;
  return model ? <DataViews tab={tab} model={model} /> : <ViewPane tab={tab} />;
});
