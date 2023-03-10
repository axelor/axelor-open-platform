import { useAsync } from "@/hooks/use-async";
import { useDataStore } from "@/hooks/use-data-store";
import { Tab } from "@/hooks/use-tabs";
import { findView } from "@/services/client/meta-cache";
import { toCamelCase, toKebabCase } from "@/utils/names";
import { ViewProps } from "@/views/types";

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

  return useAsync(
    async () => findView({ type, name, model }),
    [type, name, model]
  );
}

function DataView({ tab, meta, component: Comp }: ViewProps<any>) {
  const { view } = tab;
  const { model, domain, context } = view;
  const dataStore = useDataStore(model!, {
    filter: {
      _domain: domain,
      _domainContext: context,
    },
  });

  return <Comp tab={tab} meta={meta} dataStore={dataStore} />;
}

export function Views({ tab, className }: { tab: Tab; className?: string }) {
  const viewSchema = useViewSchema(tab);
  const viewComp = useViewComp(tab);

  if (viewSchema.state === "loading" || viewComp.state === "loading") {
    return <div>Loading...</div>;
  }

  const meta = viewSchema.data;
  const Comp = viewComp.data;

  if (meta?.model && Comp) {
    return (
      <div className={className}>
        <DataView tab={tab} meta={meta} component={Comp} />
      </div>
    );
  }

  return (
    <div className={className}>{Comp && <Comp tab={tab} meta={meta} />}</div>
  );
}
