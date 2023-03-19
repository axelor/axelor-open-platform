import { useAtom, useAtomValue } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { memo } from "react";

import { useAsync } from "@/hooks/use-async";
import { Tab, TabAtom, TabState } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { findView } from "@/services/client/meta-cache";
import { toCamelCase, toKebabCase } from "@/utils/names";

import { ViewScope } from "./scope";

function useViewComp(state: TabState) {
  return useAsync(async () => {
    const type = toKebabCase(state.type);
    const name = toCamelCase(type);
    const { [name]: Comp } = await import(`../../views/${type}/index.ts`);
    return Comp as React.ElementType;
  }, [state]);
}

function useViewSchema(state: TabState) {
  const { type, model, action } = state;
  const { views = [] } = action;
  const { name } = views.find((x) => x.type === type) ?? {};

  return useAsync(
    async () => findView({ type, name, model }),
    [type, name, model]
  );
}

function View({
  tabAtom,
  dataStore,
}: {
  tabAtom: TabAtom;
  dataStore?: DataStore;
}) {
  const [view] = useAtom(tabAtom);
  const viewSchema = useViewSchema(view);
  const viewComp = useViewComp(view);

  if (viewSchema.state === "loading" || viewComp.state === "loading") {
    return <div>Loading...</div>;
  }

  const meta = viewSchema.data;
  const Comp = viewComp.data;

  if (Comp) {
    return (
      <ScopeProvider scope={ViewScope} value={tabAtom}>
        <Comp meta={meta} dataStore={dataStore} />;
      </ScopeProvider>
    );
  }

  return null;
}

const DataViews = memo(function DataViews({
  tabAtom,
  model,
}: {
  tabAtom: TabAtom;
  model: string;
}) {
  const viewState = useAtomValue(tabAtom);
  const { domain, context } = viewState.action;
  const dataStore = new DataStore(model, {
    filter: {
      _domain: domain,
      _domainContext: context,
    },
  });
  return <View tabAtom={tabAtom} dataStore={dataStore} />;
});

export function Views({ tab, className }: { tab: Tab; className?: string }) {
  const tabAtom = tab.state;
  const tabState = useAtomValue(tabAtom);
  const { model } = tabState;
  if (model) {
    return (
      <div className={className}>
        <DataViews tabAtom={tabAtom} model={model} />
      </div>
    );
  }
  return (
    <div className={className}>
      <View tabAtom={tabAtom} />
    </div>
  );
}
