import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { FormState } from "@/views/form/builder";
import { atom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { ActionHandler } from "../action";

export type PopupHandler = {
  data?: any;
  dataStore?: DataStore;
  actionHandler?: ActionHandler;
  getState?: () => FormState;
  onNew?: () => Promise<void>;
  onRead?: (id: string | number) => Promise<DataRecord>;
  onEdit?: (record: DataRecord | null) => Promise<void>;
  onSave?: (
    callOnSave?: boolean,
    shouldSave?: boolean,
    callOnLoad?: boolean
  ) => Promise<DataRecord>;
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
