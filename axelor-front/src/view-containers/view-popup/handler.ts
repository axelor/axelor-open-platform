import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { FormState, WidgetErrors } from "@/views/form/builder";
import { WritableAtom, atom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { ActionExecutor, ActionHandler } from "../action";

export type PopupHandler = {
  data?: any;
  dataStore?: DataStore;
  actionHandler?: ActionHandler;
  actionExecutor?: ActionExecutor;
  getState?: () => FormState;
  getErrors?: () => WidgetErrors[] | undefined;
  commitForm?: () => Promise<void | void[]>;
  onNew?: () => Promise<void>;
  onRead?: (id: string | number) => Promise<DataRecord>;
  onEdit?: (record: DataRecord | null) => Promise<void>;
  onSave?: (options?: {
    shouldSave?: boolean;
    callOnSave?: boolean;
    callOnRead?: boolean;
    callOnLoad?: boolean;
  }) => Promise<DataRecord>;
  onSearch?: (options?: SearchOptions) => Promise<SearchResult>;
  onRefresh?: () => Promise<void>;
  readyAtom?: WritableAtom<boolean | undefined, [boolean | undefined], void>;
  dirtyAtom?: WritableAtom<boolean, [boolean], void>;
};

export const PopupScope = createScope<PopupHandler>({});

const popupMolecule = molecule((getMol, getScope) => {
  return atom(getScope(PopupScope));
});

export function usePopupHandlerAtom() {
  return useMolecule(popupMolecule);
}
