import { Tab } from "@/hooks/use-tabs";
import { atom, useAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

export interface ViewState extends Tab {
  type: string;
  model?: string;
}

export const ViewScope = createScope<ViewState>({
  id: "",
  type: "",
  title: "",
  view: {
    name: "",
    title: "",
    viewType: "",
  },
});

const viewMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(ViewScope);
  return atom(initialView);
});

export function useView() {
  const viewAtom = useMolecule(viewMolecule);
  return useAtom(viewAtom);
}
