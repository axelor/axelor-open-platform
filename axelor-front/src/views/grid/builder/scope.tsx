import { DataRecord } from "@/services/client/data.types";
import { createContext, useCallback, useContext, useMemo } from "react";
import uniq from "lodash/uniq";

import { JsonField, Property, Schema } from "@/services/client/meta.types";
import { parseOrderBy } from "./utils";

export type GridHandler = {
  readonly?: boolean;
};

export const GridContext = createContext<GridHandler>({});

export function useGridContext() {
  return useContext(GridContext);
}

export function useGridColumnNames({
  view,
  fields,
}: {
  view: Schema;
  fields?: Record<string, Property>;
}) {
  return useMemo(
    () =>
      uniq(
        [...view.items!, ...(parseOrderBy(view.orderBy) ?? [])].reduce(
          (names, item) => {
            const field = fields?.[item.name!];
            if ((item as JsonField).jsonField) {
              return [...names, (item as JsonField).jsonField as string];
            } else if (field) {
              return [
                ...names,
                field.name,
                ...(field.type?.endsWith("TO_ONE") &&
                (item as Schema).target &&
                (item as Schema).targetName &&
                (item as Schema).targetName !== field.targetName
                  ? [`${field.name}.${(item as Schema).targetName}`]
                  : []),
              ];
            }
            return names;
          },
          [] as string[],
        ),
      ),
    [fields, view.items, view.orderBy],
  );
}
