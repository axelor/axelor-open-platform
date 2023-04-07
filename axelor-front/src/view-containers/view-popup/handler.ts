import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { atom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

export type PopupHandler = {
  data?: any;
  dataStore?: DataStore;
  onNew?: () => Promise<void>;
  onRead?: (id: string | number) => Promise<DataRecord>;
  onEdit?: (record: DataRecord | null) => Promise<void>;
  onSave?: () => Promise<DataRecord>;
  onSearch?: (options?: SearchOptions) => Promise<SearchResult>;
  onRefresh?: () => Promise<void>;
};

export const PopupScope = createScope<PopupHandler>({});

const popupMolecule = molecule((getMol, getScope) => {
  return atom(getScope(PopupScope));
});

export function usePopupHandlerAtom() {
  return useMolecule(popupMolecule);
}
