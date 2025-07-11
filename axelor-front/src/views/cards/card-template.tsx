import { useMemo } from "react";

import { TemplateRenderer, usePrepareTemplateContext } from "@/hooks/use-parser";
import { DataRecord } from "@/services/client/data.types";
import { MetaData } from "@/services/client/meta";
import { CardsView, KanbanView } from "@/services/client/meta.types";

export function CardTemplate({
  template,
  record,
  view,
  fields,
  onRefresh,
}: {
  template: string;
  view: CardsView | KanbanView;
  record: DataRecord;
  fields?: MetaData["fields"];
  onRefresh?: () => Promise<any>;
}) {
  const {
    context,
    options: { execute },
  } = usePrepareTemplateContext(record, { view, onRefresh });

  const options = useMemo(() => ({ execute, fields }), [execute, fields]);

  return (
    <TemplateRenderer template={template} context={context} options={options} />
  );
}
