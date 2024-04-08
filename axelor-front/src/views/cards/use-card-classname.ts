import { useMemo } from "react";

import { CardsView, KanbanView } from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { useViewAction } from "@/view-containers/views/scope";
import { useHilites } from "@/hooks/use-parser";
import { legacyClassNames } from "@/styles/legacy";

export function useCardClassName(
  view: CardsView | KanbanView,
  record: DataRecord,
) {
  const { context } = useViewAction();
  const { hilites } = view;
  const getHilites = useHilites(hilites ?? []);

  return useMemo(() => {
    const data = { ...context, ...record };
    const color = getHilites(data)?.[0]?.color;
    return legacyClassNames({
      [`hilite-${color}-card`]: color,
    });
  }, [getHilites, context, record]);
}
