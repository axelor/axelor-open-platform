import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { FormState, WidgetErrors } from "@/views/form/builder";
import { WritableAtom, atom, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "bunshi/react";
import { ActionExecutor, ActionHandler } from "../action";
import { CommandItemProps } from "@axelor/ui";
import { useCallback } from "react";

export type PopupHandler = {
  data?: any;
  dataRecords?: DataRecord[];
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
  attachmentItem?: CommandItemProps | null;
  close?: (result?: boolean) => void;
};

export const PopupScope = createScope<PopupHandler>({});

const popupMolecule = molecule((getMol, getScope) => {
  return atom(getScope(PopupScope));
});

export function usePopupHandlerAtom() {
  return useMolecule(popupMolecule);
}

export function useSetPopupHandlers() {
  const popupHandlerAtom = usePopupHandlerAtom();
  const setPopupHandlers = useSetAtom(popupHandlerAtom);

  return useCallback(
    (handlers: PopupHandler) => {
      setPopupHandlers((draft) => ({
        // only restore popup actions, which is set by popup itself
        // so it can't be overriden by view set popupHandlers
        close: draft.close,
        ...handlers,
      }));
    },
    [setPopupHandlers],
  );
}
