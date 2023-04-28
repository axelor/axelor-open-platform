import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { useCallback, useEffect, useMemo, useRef } from "react";

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

import { fallbackFormAtom } from "./atoms";
import { FormAtom, FormProps, RecordHandler, RecordListener } from "./types";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import {
  EvalContextOptions,
  createEvalContext,
} from "@/hooks/use-parser/eval-context";
import { processActionValue } from "./utils";

type ContextCreator = () => DataContext;

export class FormRecordHandler implements RecordHandler {
  #listeners = new Set<RecordListener>();
  #record: DataContext = {};

  subscribe(subscriber: RecordListener) {
    this.#listeners.add(subscriber);
    subscriber(this.#record);
    return () => {
      this.#listeners.delete(subscriber);
    };
  }

  notify(data: DataContext) {
    this.#record = data;
    this.#listeners.forEach((fn) => fn(data));
  }
}

interface SaveHandler {
  (): Promise<void>;
  (record?: DataRecord): Promise<void>;
}

type RefreshHandler = () => Promise<void>;

export class FormActionHandler extends DefaultActionHandler {
  #prepareContext: ContextCreator;
  #saveHandler?: SaveHandler;
  #refreshHandler?: RefreshHandler;

  constructor(prepareContext: ContextCreator) {
    super();
    this.#prepareContext = prepareContext;
  }

  setSaveHandler(handler: SaveHandler) {
    this.#saveHandler = handler;
  }

  setRefreshHandler(handler: RefreshHandler) {
    this.#refreshHandler = handler;
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
    });
  }

  setValue(target: string, value: any): void {
    this.notify({
      op: "set",
      type: "value",
      target,
      value: processActionValue(value),
    });
  }

  async setValues(values: DataRecord) {
    Object.entries(values).forEach(([name, value]) => {
      this.setValue(name, value);
    });
  }

  async save(record?: DataRecord) {
    return await this.#saveHandler?.(record);
  }

  async refresh() {
    return await this.#refreshHandler?.();
  }

  async close() {
    this.notify({
      type: "close",
    });
  }
}

type FormScopeState = {
  actionHandler: ActionHandler;
  actionExecutor: ActionExecutor;
  recordHandler: RecordHandler;
  formAtom: FormAtom;
};

const fallbackHandler = new FormActionHandler(() => ({}));
const fallbackExecutor = new DefaultActionExecutor(fallbackHandler);
const fallbackRecordHandler = new FormRecordHandler();

export const FormScope = createScope<FormScopeState>({
  actionHandler: fallbackHandler,
  actionExecutor: fallbackExecutor,
  formAtom: fallbackFormAtom,
  recordHandler: fallbackRecordHandler,
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
          const { statesByName, fields } = get(formAtom);
          const { target, name, value } = data;

          // collection field column ?
          if (target.includes(".")) {
            const fieldName = target.split(".")[0];
            const field = fields[fieldName];
            if (field?.type.endsWith("_TO_MANY")) {
              const state = statesByName[fieldName] ?? {};
              const column = target.split(".")[1];
              const columns = state.columns ?? {};
              const newState = {
                ...state,
                columns: {
                  ...columns,
                  [column]: {
                    ...columns[column],
                    [name]: value,
                  },
                },
              };
              const newStates = {
                ...statesByName,
                [fieldName]: newState,
              };
              set(formAtom, (prev) => ({
                ...prev,
                statesByName: newStates,
              }));
              return;
            }
          }

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

export function FormRecordUpdates({
  fields,
  readonly,
  formAtom,
  recordHandler,
}: {
  readonly?: boolean;
  fields: FormProps["fields"];
  formAtom: FormAtom;
  recordHandler: RecordHandler;
}) {
  const record = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom])
  );

  useAsyncEffect(async () => {
    recordHandler.notify(
      createEvalContext(record, {
        fields: fields as unknown as EvalContextOptions["fields"],
        readonly,
      })
    );
  }, [record, recordHandler, fields, readonly]);

  return null;
}
