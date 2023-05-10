import { atom, useAtomValue, useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { selectAtom } from "jotai/utils";
import { memo, useMemo } from "react";

import { Box, Fade } from "@axelor/ui";

import { Loader } from "@/components/loader/loader";
import { useAsync } from "@/hooks/use-async";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { Tab } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { filters as fetchFilters } from "@/services/client/meta";
import { findView } from "@/services/client/meta-cache";
import {
  AdvancedSearchAtom,
  SavedFilter,
  SearchFilters,
} from "@/services/client/meta.types";
import { toCamelCase, toKebabCase } from "@/utils/names";
import { findFields } from "@/services/client/meta-cache";

import { ViewScope } from "./scope";
import { AdvancedSearchState } from "../advance-search/types";

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
  const { model } = tab.action;
  const { state, data } = useAsync(
    async () => loadView({ model, ...view }),
    [model, view]
  );

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
          <Comp meta={meta} dataStore={dataStore} searchAtom={searchAtom} />
        </Box>
      </Fade>
    );
  }

  return null;
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
        <ViewContainer
          view={view}
          tab={tab}
          dataStore={dataStore}
          searchAtom={searchAtom}
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
    action: { name: actionName, domain, context, params },
  } = tab;
  const dataStore = new DataStore(model, {
    filter: {
      _domain: domain,
      _domainContext: context,
    },
  });

  // advanced search
  const searchAtom = useMemo<AdvancedSearchAtom>(
    () =>
      atom<AdvancedSearchState>({
        search: {},
        editor: { criteria: [] },
      }),
    []
  );
  const setSearchState = useSetAtom(searchAtom);

  const filterName = (params || {})["search-filters"];

  useAsync<void>(async () => {
    const name = filterName || `act:${actionName}`;
    const filters = await fetchFilters(name);
    setSearchState((state) => ({ ...state, filters }));
  }, [actionName, filterName, setSearchState]);

  useAsyncEffect(async () => {
    const { fields, jsonFields } = await findFields(model, context?.jsonModel);
    fields && setSearchState((state) => ({ ...state, fields, jsonFields }));
  }, [model]);

  useAsyncEffect(async () => {
    if (!filterName) return;
    const res = await findView<SearchFilters>({
      name: filterName,
      type: "search-filters",
      model,
    });
    setSearchState((state) => ({
      ...state,
      domains: res?.view?.filters || [],
    }));
  }, [model, filterName, setSearchState]);

  return <ViewPane tab={tab} dataStore={dataStore} searchAtom={searchAtom} />;
});

export const Views = memo(function Views({ tab }: { tab: Tab }) {
  const model = tab.action.model;
  return model ? <DataViews tab={tab} model={model} /> : <ViewPane tab={tab} />;
});
