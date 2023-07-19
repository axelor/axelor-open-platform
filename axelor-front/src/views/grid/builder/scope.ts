import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

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
