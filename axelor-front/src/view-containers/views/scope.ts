import { atom, useAtom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

import { TabAtom, TabState } from "@/hooks/use-tabs";

const fallbackAtom = atom<TabState>({
  action: {
    name: "",
    title: "",
    viewType: "",
  },
  title: "",
  type: "",
});

export const ViewScope = createScope<TabAtom>(fallbackAtom);

const viewMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(ViewScope);
  return atom(initialView);
});

export function useViewState() {
  const viewAtom = useMolecule(viewMolecule);
  const tabAtom = useAtomValue(viewAtom);
  return useAtom(tabAtom);
}
