import { atom } from "jotai";
import { createScope, molecule, useMolecule } from "bunshi/react";

import { DataRecord } from "@/services/client/data.types";

export type DMSGridHandler = {
  getSelectedDocuments?: () => DataRecord[] | null;
};

export const DMSGridScope = createScope<DMSGridHandler>({});

const dmsGridMolecule = molecule((getMol, getScope) => {
  return atom(getScope(DMSGridScope));
});

export function useDMSGridHandlerAtom() {
  return useMolecule(dmsGridMolecule);
}
