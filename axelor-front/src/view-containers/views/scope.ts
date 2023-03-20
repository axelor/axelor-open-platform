import { atom, useAtom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

import { Tab, TabAtom } from "@/hooks/use-tabs";

const fallbackAtom: TabAtom = atom(
  () => ({
    title: "",
    type: "",
  }),
  () => {}
);

const fallbackTab: Tab = {
  id: "",
  title: "",
  action: {
    name: "",
    title: "",
    viewType: "",
  },
  state: fallbackAtom,
};

export const ViewScope = createScope<Tab>(fallbackTab);

const viewMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(ViewScope);
  return atom(initialView);
});

export function useViewTab() {
  const viewAtom = useMolecule(viewMolecule);
  return useAtomValue(viewAtom);
}

export function useViewAction() {
  const tab = useViewTab();
  return tab.action;
}

export function useViewState() {
  const tab = useViewTab();
  return useAtom(tab.state);
}
