import { useAsync } from "@/hooks/use-async";
import { useDataStore } from "@/hooks/use-data-store";
import { useMeta } from "@/hooks/use-meta";
import { Tab } from "@/hooks/use-tabs";
import { ViewData } from "@/services/client/meta";
import { toCamelCase, toKebabCase } from "@/utils/names";

function useViewComp(tab: Tab) {
  return useAsync(async () => {
    const type = toKebabCase(tab.view.viewType!);
    const name = toCamelCase(type);
    const { [name]: Comp } = await import(`../../views/${type}/index.ts`);
    return Comp as React.ElementType;
  }, [tab]);
}

function useViewSchema(tab: Tab) {
  const { view } = tab;
  const { viewType: type, model, views = [] } = view;
  const { name } = views.find((x) => x.type === type) ?? {};

  const { findView } = useMeta();
  return useAsync(
    async () => findView({ type, name, model }),
    [type, name, model]
  );
}

function DataViews({
  tab,
  schema,
  component: Comp,
}: {
  tab: Tab;
  schema: ViewData<any>;
  component: React.ElementType;
}) {
  const { view } = tab;
  const { model, domain, context } = view;
  const dataStore = useDataStore(model!, {
    filter: {
      _domain: domain,
      _domainContext: context,
    },
  });

  return <Comp tab={tab} schema={schema} dataStore={dataStore} />;
}

export function Views({ tab, className }: { tab: Tab; className?: string }) {
  const viewSchema = useViewSchema(tab);
  const viewComp = useViewComp(tab);

  if (viewSchema.state === "loading" || viewComp.state === "loading") {
    return <div>Loading...</div>;
  }

  const schema = viewSchema.data;
  const Comp = viewComp.data;

  if (schema?.model && Comp) {
    return (
      <div className={className}>
        <DataViews tab={tab} schema={schema} component={Comp} />
      </div>
    );
  }

  return (
    <div className={className}>
      {Comp && <Comp tab={tab} schema={schema} />}
    </div>
  );
}
