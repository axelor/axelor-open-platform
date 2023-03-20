import { atom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { useCallback } from "react";

import { Tab, TabAtom, useTabs } from "@/hooks/use-tabs";

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

interface SwitchTo {
  (type: string): void;
  (route: { mode?: string; id?: string; qs?: Record<string, string> }): void;
}

export function useViewSwitch() {
  const tab = useViewTab();
  const action = tab.id;
  const setViewState = useSetAtom(tab.state);
  const { open } = useTabs();

  const switchTo = useCallback<SwitchTo>(
    (arg) => {
      if (typeof arg === "string") {
        setViewState({ type: arg });
      } else {
        open(action, arg);
      }
    },
    [action, open, setViewState]
  );

  return switchTo;
}
