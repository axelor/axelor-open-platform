import { useAsync } from "@/hooks/use-async";
import { DataStore, useDataStore } from "@/hooks/use-data-store";
import { Tab } from "@/hooks/use-tabs";
import { findView } from "@/services/client/meta-cache";
import { toCamelCase, toKebabCase } from "@/utils/names";
import { ScopeProvider } from "jotai-molecules";
import { memo } from "react";
import { useView, ViewScope, ViewState } from "./hooks";

function useViewComp(state: ViewState) {
  return useAsync(async () => {
    const type = toKebabCase(state.type);
    const name = toCamelCase(type);
    const { [name]: Comp } = await import(`../../views/${type}/index.ts`);
    return Comp as React.ElementType;
  }, [state]);
}

function useViewSchema(state: ViewState) {
  const { type, model, view } = state;
  const { views = [] } = view;
  const { name } = views.find((x) => x.type === type) ?? {};

  return useAsync(
    async () => findView({ type, name, model }),
    [type, name, model]
  );
}

function View({ dataStore }: { dataStore?: DataStore }) {
  const [view] = useView();
  const viewSchema = useViewSchema(view);
  const viewComp = useViewComp(view);

  if (viewSchema.state === "loading" || viewComp.state === "loading") {
    return <div>Loading...</div>;
  }

  const meta = viewSchema.data;
  const Comp = viewComp.data;

  if (Comp) {
    return <Comp meta={meta} dataStore={dataStore} />;
  }

  return null;
}

function ViewPane({ tab, dataStore }: { tab: Tab; dataStore?: DataStore }) {
  const { view } = tab;
  const { viewType: type, model } = view;

  const initialState = {
    ...tab,
    type,
    model,
  };

  return (
    <ScopeProvider scope={ViewScope} value={initialState}>
      <View dataStore={dataStore} />
    </ScopeProvider>
  );
}

const DataViews = memo(function DataViews({
  tab,
  model,
}: {
  tab: Tab;
  model: string;
}) {
  const { view } = tab;
  const { domain, context } = view;
  const dataStore = useDataStore(model, {
    filter: {
      _domain: domain,
      _domainContext: context,
    },
  });
  return <ViewPane tab={tab} dataStore={dataStore} />;
});

export function Views({ tab, className }: { tab: Tab; className?: string }) {
  const { view } = tab;
  const { model } = view;
  if (model) {
    return (
      <div className={className}>
        <DataViews tab={tab} model={model} />
      </div>
    );
  }
  return (
    <div className={className}>
      <ViewPane tab={tab} />
    </div>
  );
}
