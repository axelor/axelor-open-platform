import { DataContext } from "@/services/client/data.types";
import { DefaultActionHandler } from "@/view-containers/action";
import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

export class GridActionHandler extends DefaultActionHandler {
  #prepareContext: () => DataContext;

  constructor(prepareContext: () => DataContext) {
    super();
    this.#prepareContext = prepareContext;
  }

  getContext() {
    return this.#prepareContext();
  }
}

export type GridHandler = {
  readonly?: boolean;
}

export const GridScope = createScope<GridHandler>({});

const gridMolecule = molecule((getMol, getScope) => {
  return atom(getScope(GridScope));
});

export function useGridScope() {
  return useAtomValue(useMolecule(gridMolecule));
}
