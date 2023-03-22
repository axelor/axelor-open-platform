import { useAtomValue } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { memo, useMemo } from "react";

import { Box, Fade } from "@axelor/ui";

import { Loader } from "@/components/loader/loader";
import { useAsync } from "@/hooks/use-async";
import { Tab } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { findView } from "@/services/client/meta-cache";
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
}: {
  tab: Tab;
  view: { name?: string; type: string };
  dataStore?: DataStore;
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
          <Comp meta={meta} dataStore={dataStore} />
        </Box>
      </Fade>
    );
  }

  return null;
}

function ViewPane({ tab, dataStore }: { tab: Tab; dataStore?: DataStore }) {
  const {
    action: { views = [] },
  } = tab;
  const tabAtom = tab.state;
  const viewState = useAtomValue(tabAtom);
  const view = useMemo(
    () => views.find((x) => x.type === viewState.type),
    [viewState.type, views]
  );

  if (view) {
    return (
      <ScopeProvider scope={ViewScope} value={tab}>
        <ViewContainer view={view} tab={tab} dataStore={dataStore} />
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
