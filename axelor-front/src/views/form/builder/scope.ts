import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { useCallback, useEffect, useRef } from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import {
  ActionAttrData,
  ActionData,
  ActionExecutor,
  ActionHandler,
  ActionValueData,
  DefaultActionExecutor,
  DefaultActionHandler,
} from "@/view-containers/action";

import { useAtomCallback } from "jotai/utils";
import { fallbackFormAtom } from "./atoms";
import { FormAtom } from "./types";

type ContextCreator = () => DataContext;

export class FormActionHandler extends DefaultActionHandler {
  #prepareContext: ContextCreator;

  constructor(prepareContext: ContextCreator) {
    super();
    this.#prepareContext = prepareContext;
  }

  getContext() {
    return this.#prepareContext();
  }

  setAttr(target: string, name: string, value: any) {
    if (name === "value" || name.startsWith("value:")) {
      const op = value.substring(6) ?? "set";
      return this.notify({
        op,
        type: "value",
        target,
        value,
      });
    }
    this.notify({
      type: "attr",
      target,
      name,
      value,
    });
  }

  setFocus(target: string) {
    this.notify({
      type: "focus",
      target,
      value: true,
    });
  }

  setValue(target: string, value: any): void {
    this.notify({
      op: "set",
      type: "value",
      target,
      value,
    });
  }

  async setValues(values: DataRecord) {
    Object.entries(values).forEach(([name, value]) => {
      this.setValue(name, value);
    });
  }
}

type FormScopeState = {
  actionHandler: ActionHandler;
  actionExecutor: ActionExecutor;
  formAtom: FormAtom;
};

const fallbackHandler = new FormActionHandler(() => ({}));
const fallbackExecutor = new DefaultActionExecutor(fallbackHandler);

export const FormScope = createScope<FormScopeState>({
  actionHandler: fallbackHandler,
  actionExecutor: fallbackExecutor,
  formAtom: fallbackFormAtom,
});

const formMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(FormScope);
  return atom(initialView);
});

export function useFormScope() {
  const scopeAtom = useMolecule(formMolecule);
  return useAtomValue(scopeAtom);
}

function useActionData<T extends ActionData>(
  check: (data: ActionData) => boolean,
  handler: (data: T) => void
) {
  const { actionHandler } = useFormScope();
  const doneRef = useRef<boolean>(false);

  useEffect(() => {
    doneRef.current = false;
    return () => {
      doneRef.current = true;
    };
  });

  useEffect(() => {
    return actionHandler.subscribe((data) => {
      if (doneRef.current) return;
      if (data) {
        if (check(data)) {
          handler(data as T);
        }
      }
    });
  });
}

function useActionAttrs({ formAtom }: { formAtom: FormAtom }) {
  useActionData<ActionAttrData>(
    useCallback((x) => x.type === "attr", []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const { statesByName } = get(formAtom);
          const state = statesByName[data.target] ?? {};
          const newState = {
            ...state,
            attrs: {
              ...state.attrs,
              [data.name]: data.value,
            },
          };
          const newStates = {
            ...statesByName,
            [data.target]: newState,
          };
          set(formAtom, (prev) => ({
            ...prev,
            statesByName: newStates,
          }));
        },
        [formAtom]
      )
    )
  );
}

function useActionValue({ formAtom }: { formAtom: FormAtom }) {
  useActionData<ActionValueData>(
    useCallback((x) => x.type === "value", []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const { record } = get(formAtom);
          const { target, value, op } = data;
          let newRecord = record;

          if (op === "set") {
            newRecord = {
              ...record,
              [target]: value,
            };
          }
          if (op === "add") {
            const items: DataRecord[] = record[target] ?? [];
            const found = items.find((x) => x.id === value.id);
            if (found) {
              newRecord = {
                ...record,
                [target]: items.map((x) =>
                  x.id === value.id ? { ...found, ...value } : x
                ),
              };
            } else {
              newRecord = {
                ...record,
                [target]: [...items, value],
              };
            }
          }
          if (op === "del") {
            const items: DataRecord[] = record[target] ?? [];
            newRecord = {
              ...record,
              [target]: items.filter((x) => x.id !== value.id),
            };
          }
          if (record !== newRecord) {
            set(formAtom, (prev) => ({ ...prev, record: newRecord }));
          }
        },
        [formAtom]
      )
    )
  );
}

export function ActionDataHandler({ formAtom }: { formAtom: FormAtom }) {
  useActionAttrs({ formAtom });
  useActionValue({ formAtom });
  return null;
}
