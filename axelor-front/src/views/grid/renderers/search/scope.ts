import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

export type SearchState = Record<string, string>;

export type GridSearchScopeState = {
  search: SearchState;
  setSearch?: (
    stateOrStateAction: SearchState | ((state: SearchState) => SearchState)
  ) => void;
  onSearch?: (state: SearchState) => void;
};

export const GridSearchScope = createScope<GridSearchScopeState>({
  search: {},
});

const searchMolecule = molecule((getMol, getScope) => {
  const initialState = getScope(GridSearchScope);
  return atom(initialState);
});

export function useGridSearchFieldScope() {
  const scopeAtom = useMolecule(searchMolecule);
  return useAtomValue(scopeAtom);
}
