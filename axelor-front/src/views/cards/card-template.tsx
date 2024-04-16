import { FunctionComponent } from "react";

import { useTemplateContext } from "@/hooks/use-parser";
import { EvalContextOptions } from "@/hooks/use-parser/context";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { MetaData } from "@/services/client/meta";

export function CardTemplate({
  component: TemplateComponent,
  record,
  fields,
  onRefresh,
}: {
  component: FunctionComponent<{
    context: DataContext;
    options?: EvalContextOptions;
  }>;
  record: DataRecord;
  fields?: MetaData["fields"];
  onRefresh?: () => Promise<any>;
}) {
  const {
    context,
    options: { execute },
  } = useTemplateContext(record, onRefresh);

  return (
    <TemplateComponent
      context={context}
      options={{
        execute,
        fields,
      }}
    />
  );
}
