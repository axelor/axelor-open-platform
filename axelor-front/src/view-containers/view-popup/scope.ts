import { atom, useAtomValue, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";

export type PopupOptions = {
  data?: any;
  dataStore?: DataStore;
  onNew?: () => Promise<void>;
  onRead?: (id: string | number) => Promise<DataRecord>;
  onEdit?: (record: DataRecord | null) => Promise<void>;
  onSave?: () => Promise<DataRecord>;
  onSearch?: (options?: SearchOptions) => Promise<SearchResult>;
  onRefresh?: () => Promise<void>;
};

export const PopupScope = createScope<PopupOptions>({});

const popupMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(PopupScope);
  return atom(initialView);
});

export function useSetPopupOptions() {
  const viewAtom = useMolecule(popupMolecule);
  return useSetAtom(viewAtom);
}

export function usePopupOptions() {
  const viewAtom = useMolecule(popupMolecule);
  return useAtomValue(viewAtom);
}
