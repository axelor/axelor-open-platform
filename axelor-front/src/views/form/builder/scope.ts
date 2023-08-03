import { produce } from "immer";
import { atom, useAtomValue } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { isEqual, isNumber, set as setDeep } from "lodash";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import {
  EvalContextOptions,
  createEvalContext,
} from "@/hooks/use-parser/eval-context";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { View } from "@/services/client/meta.types";
import {
  ActionAttrData,
  ActionData,
  ActionExecutor,
  ActionHandler,
  ActionValueData,
  DefaultActionExecutor,
  DefaultActionHandler,
} from "@/view-containers/action";
import { useViewTab } from "@/view-containers/views/scope";

import { fallbackFormAtom } from "./atoms";
import {
  FormAtom,
  FormProps,
  RecordHandler,
  RecordListener,
  WidgetErrors,
  WidgetState,
} from "./types";
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

const ATTR_MAPPER: Record<string, string> = {
  "url:set": "url",
};

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
      name: ATTR_MAPPER[name] ?? name,
      value,
    });
  }

  setFocus(target: string) {
    this.notify({
      type: "focus",
      target,
    });
  }

  addValue(target: string, value: any): void {
    this.notify({
      op: "add",
      type: "value",
      target,
      value: isNumber(value) ? { id: value } : processActionValue(value),
    });
  }

  delValue(target: string, value: any): void {
    this.notify({
      op: "del",
      type: "value",
      target,
      value: isNumber(value) ? { id: value } : processActionValue(value),
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

export type FormValidityHandler = () => WidgetErrors[] | null;

export type FormValidityScopeState = {
  add: (fn: FormValidityHandler) => void;
};

export const FormValidityScope = createScope<FormValidityScopeState>({
  add: () => () => {},
});

const formValidityMolecule = molecule((getMol, getScope) => {
  return atom(getScope(FormValidityScope));
});

export function useFormValidityScope() {
  const scopeAtom = useMolecule(formValidityMolecule);
  return useAtomValue(scopeAtom);
}

export function useWidgetState(formAtom: FormAtom, widgetName: string) {
  const findAtom = useAtomCallback(
    useCallback(
      (get) => {
        const { widgetAtoms } = get(formAtom);
        const { widgetAtom } =
          Object.values(widgetAtoms).find((x) => x.name === widgetName) ?? {};
        return widgetAtom;
      },
      [formAtom, widgetName]
    )
  );

  const widgetAtom = useMemo(findAtom, [findAtom]);

  const getState = useAtomCallback(
    useCallback(
      (get) => (widgetAtom ? get(widgetAtom) : { attrs: {}, name: widgetName }),
      [widgetName, widgetAtom]
    )
  );

  return getState();
}

export function useFormRefresh(refresh?: () => Promise<any> | void) {
  const tab = useViewTab();
  const handleRefresh = useCallback(
    (e: Event) =>
      e instanceof CustomEvent && e.detail === tab.id && refresh?.(),
    [refresh, tab.id]
  );

  useEffect(() => {
    document.addEventListener("form:refresh", handleRefresh);
    return () => {
      document.removeEventListener("form:refresh", handleRefresh);
    };
  }, [handleRefresh]);
}

function useActionData<T extends ActionData>(
  check: (data: ActionData) => boolean,
  handler: (data: T) => void,
  actionHandler: ActionHandler
) {
  const doneRef = useRef<boolean>(false);

  useEffect(() => {
    doneRef.current = false;
    return () => {
      doneRef.current = true;
    };
  }, []);

  useEffect(() => {
    return actionHandler.subscribe((data) => {
      if (doneRef.current) return;
      if (data) {
        if (check(data)) {
          handler(data as T);
        }
      }
    });
  }, [actionHandler, check, handler]);
}

function useActionAttrs({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  useActionData<ActionAttrData>(
    useCallback((x) => x.type === "attr", []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const { statesByName, states: statesById, fields } = get(formAtom);
          const { target, name, value } = data;

          const updateStates = (fieldName: string, newState: WidgetState) => ({
            statesByName: { ...statesByName, [fieldName]: newState },
            states: produce(statesById, (prev) => {
              // reset widget's own state so that the attibute set by the action get preference.
              Object.values(prev).forEach((state) => {
                if (state.name === fieldName && name in state.attrs) {
                  delete (state.attrs as any)[name];
                }
              });
            }),
          });

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
              const newStates = updateStates(fieldName, newState);
              set(formAtom, (prev) => ({
                ...prev,
                ...newStates,
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

          const newStates = updateStates(data.target, newState);
          set(formAtom, (prev) => ({
            ...prev,
            ...newStates,
          }));
        },
        [formAtom]
      )
    ),
    actionHandler
  );
}

function useActionValue({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  useActionData<ActionValueData>(
    useCallback((x) => x.type === "value", []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const { record, fields } = get(formAtom);
          const { target, value, op } = data;
          let newRecord = record;

          if (op === "set") {
            newRecord = produce(record, (draft) => {
              if (target.includes(".")) {
                const fieldName = target.split(".")[0];
                const field = fields?.[fieldName];
                if (field?.type.endsWith("TO_MANY")) {
                  draft[fieldName]?.forEach?.((item: DataRecord) => {
                    setDeep(item, target.slice(fieldName.length + 1), value);
                  });
                  return draft;
                }
              }
              setDeep(draft, target, value);
            });
          }
          if (op === "add") {
            const items: DataRecord[] = record[target] ?? [];
            const found = items.find((x) => x.id === value.id);
            if (found) {
              newRecord = produce(record, (draft) => {
                setDeep(
                  draft,
                  target,
                  items.map((x) =>
                    x.id === value.id ? { ...found, ...value } : x
                  )
                );
              });
            } else {
              newRecord = produce(record, (draft) => {
                setDeep(draft, target, [...items, value]);
              });
            }
          }
          if (op === "del") {
            const items: DataRecord[] = record[target] ?? [];
            newRecord = setDeep(
              record,
              target,
              items.filter((x) => x.id !== value.id)
            );
          }

          if (record !== newRecord) {
            set(formAtom, (prev) => ({ ...prev, record: newRecord }));
          }
        },
        [formAtom]
      )
    ),
    actionHandler
  );
}

export function useActionExecutor(
  view: View,
  options?: {
    onRefresh?: () => Promise<any>;
    getContext?: () => DataContext;
  }
) {
  const { formAtom } = useFormScope();
  const { onRefresh, getContext } = options || {};

  const actionHandler = useMemo(() => {
    const actionHandler = new FormActionHandler(() => ({
      ...getContext?.(),
      _viewName: view.name,
      _model: view.model,
    }));

    onRefresh && actionHandler.setRefreshHandler(onRefresh);

    return actionHandler;
  }, [getContext, onRefresh, view.model, view.name]);

  const actionExecutor = useMemo(
    () => new DefaultActionExecutor(actionHandler),
    [actionHandler]
  );

  useActionAttrs({ formAtom, actionHandler });
  useActionValue({ formAtom, actionHandler });

  return actionExecutor;
}

export function ActionDataHandler({ formAtom }: { formAtom: FormAtom }) {
  const { actionHandler } = useFormScope();
  useActionAttrs({ formAtom, actionHandler });
  useActionValue({ formAtom, actionHandler });
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
  const recordRef = useRef<DataRecord | undefined | null>();
  const record = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom])
  );

  useAsyncEffect(async () => {
    if (isEqual(recordRef.current, record)) return;
    recordRef.current = record;
    recordHandler.notify(
      createEvalContext(record, {
        fields: fields as unknown as EvalContextOptions["fields"],
        readonly,
      })
    );
  }, [record, recordHandler, fields, readonly]);

  return null;
}
