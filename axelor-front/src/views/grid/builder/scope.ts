import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import uniq from "lodash/uniq";
import { useMemo } from "react";

import { JsonField, Property, Schema } from "@/services/client/meta.types";

export type GridHandler = {
  readonly?: boolean;
};

export const GridScope = createScope<GridHandler>({});

const gridMolecule = molecule((getMol, getScope) => {
  return atom(getScope(GridScope));
});

export function useGridScope() {
  return useAtomValue(useMolecule(gridMolecule));
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
        view.items!.reduce((names, item) => {
          const field = fields?.[item.name!];
          if ((item as JsonField).jsonField) {
            return [...names, (item as JsonField).jsonField as string];
          } else if (field) {
            return [...names, field.name];
          }
          return names;
        }, [] as string[]),
      ),
    [fields, view.items],
  );
}
