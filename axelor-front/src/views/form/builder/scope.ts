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
} from "@/hooks/use-parser/context";
import { isCleanDummy } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { View } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import {
  ActionAttrData,
  ActionData,
  ActionExecutor,
  ActionHandler,
  ActionValueData,
  DefaultActionExecutor,
  DefaultActionHandler,
} from "@/view-containers/action";
import { useViewMeta, useViewTab } from "@/view-containers/views/scope";

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

type AsyncHandler = () => Promise<void>;

const ATTR_MAPPER: Record<string, string> = {
  "url:set": "url",
};

export class FormActionHandler extends DefaultActionHandler {
  #prepareContext: ContextCreator;
  #saveHandler?: SaveHandler;
  #refreshHandler?: AsyncHandler;
  #validateHandler?: AsyncHandler;

  constructor(prepareContext: ContextCreator) {
    super();
    this.#prepareContext = prepareContext;
  }

  setSaveHandler(handler: SaveHandler) {
    this.#saveHandler = handler;
  }

  setRefreshHandler(handler: AsyncHandler) {
    this.#refreshHandler = handler;
  }

  setValidateHandler(handler: AsyncHandler) {
    this.#validateHandler = handler;
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
    this.notify({
      type: "record",
      value: values,
    });
  }

  async validate() {
    await this.#validateHandler?.();
  }

  async save(record?: DataRecord) {
    await this.#saveHandler?.(record);
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
  const { findItem } = useViewMeta();

  const field = useMemo(() => findItem(widgetName), [findItem, widgetName]);

  const findState = useAtomCallback(
    useCallback(
      (get) => {
        const { states, statesByName } = get(formAtom);
        const state =
          Object.values(states).find((x) => x.name === widgetName) ??
          statesByName[widgetName];
        return {
          name: widgetName,
          ...state,
          attrs: { ...field, ...state?.attrs },
        };
      },
      [formAtom, widgetName, field],
    ),
  );

  const state = findState();
  return state ?? {};
}

export function useFormRefresh(refresh?: () => Promise<any> | void) {
  const tab = useViewTab();
  const handleRefresh = useCallback(
    (e: Event) =>
      e instanceof CustomEvent && e.detail === tab.id && refresh?.(),
    [refresh, tab.id],
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
  actionHandler: ActionHandler,
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

          const updateStates = (
            newState: WidgetState,
            fieldName: string,
            columnName?: string,
          ) => ({
            statesByName: { ...statesByName, [fieldName]: newState },
            states: produce(statesById, (prev) => {
              // reset widget's own state so that the attibute set by the action get preference.
              Object.values(prev).forEach((state) => {
                if (state.name !== fieldName) return;
                if (columnName) {
                  delete (state.columns?.[columnName] as any)?.[name];
                } else if (name in state.attrs) {
                  delete (state.attrs as any)?.[name];
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
              const newStates = updateStates(newState, fieldName, column);
              set(formAtom, (prev) => ({
                ...prev,
                ...newStates,
              }));
              return;
            }
          }

          const state = statesByName[target] ?? {};
          const newState = {
            ...state,
            ...(name === "error"
              ? { errors: { ...state.errors, error: value } }
              : {
                  attrs: {
                    ...state.attrs,
                    [name]: (() => {
                      if (name === "refresh") {
                        return value ? (state.attrs?.refresh ?? 0) + 1 : 0;
                      }
                      return value;
                    })(),
                  },
                }),
          };

          const newStates = updateStates(newState, data.target);
          set(formAtom, (prev) => ({
            ...prev,
            ...newStates,
          }));
        },
        [formAtom],
      ),
    ),
    actionHandler,
  );
}

function useActionValue({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  const { findItem } = useViewMeta();

  const canDirty = useCallback(
    (target: string) => {
      const field = findItem(target);
      return field?.canDirty !== false && !isCleanDummy(target);
    },
    [findItem],
  );

  useActionData<ActionValueData>(
    useCallback((x) => x.type === "value", []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const { record } = get(formAtom);
          const { target, value, op } = data;
          let newRecord = record;

          if (op === "set") {
            newRecord = produce(record, (draft) => {
              if (target.includes(".")) {
                const fieldName = target.split(".")[0];
                const field = findItem(fieldName);
                const type = toKebabCase(field?.serverType ?? field?.widget);
                if (type?.endsWith("-to-many")) {
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
                    x.id === value.id ? { ...found, ...value } : x,
                  ),
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
              items.filter((x) => x.id !== value.id),
            );
          }

          if (record !== newRecord) {
            set(formAtom, (prev) => ({
              ...prev,
              dirty: prev.dirty || canDirty(target),
              record: newRecord,
            }));
          }
        },
        [canDirty, findItem, formAtom],
      ),
    ),
    actionHandler,
  );
}

function useActionRecord({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  const { findItem } = useViewMeta();

  const canDirty = useCallback(
    (target: string) => {
      const field = findItem(target);
      return field?.canDirty !== false && !isCleanDummy(target);
    },
    [findItem],
  );

  useActionData<ActionValueData>(
    useCallback((x) => x.type === "record" && Boolean(x.value), []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const record = get(formAtom).record;
          const values = Object.entries(data.value).reduce(
            (acc, [k, v]) => ({
              ...acc,
              [k]: processActionValue(v),
            }),
            {} as DataRecord,
          );
          const result = { ...record, ...values };
          const isDirty = () =>
            Object.entries(values).some(
              ([k, v]) => record[k] !== v && canDirty(k),
            );
          set(formAtom, (prev) => ({
            ...prev,
            dirty: prev.dirty || isDirty(),
            record: result,
          }));
        },
        [canDirty, formAtom],
      ),
    ),
    actionHandler,
  );
}

export function useActionExecutor(
  view: View,
  options?: {
    formActions?: boolean; // flag to handle form actions
    getContext?: () => DataContext;
    onRefresh?: () => Promise<any>;
  },
) {
  const { formActions = true, onRefresh, getContext } = options || {};
  const formScope = useFormScope();
  const formAtom = formActions ? formScope.formAtom : fallbackFormAtom;

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
    [actionHandler],
  );

  useActionAttrs({ formAtom, actionHandler });
  useActionValue({ formAtom, actionHandler });
  useActionRecord({ formAtom, actionHandler });

  return actionExecutor;
}

/**
 * A hook to execute the callback after all the actions are executed.
 *
 * @param callback a stable callback (prefer using `useCallback`)
 * @returns a debounced callback that only executes after all the actions are completed
 */
export function useAfterActions<Type, Args extends Array<unknown>>(
  callback: (...args: Args) => Promise<Type>,
) {
  const { actionExecutor } = useFormScope();

  const waitRef = useRef<Promise<Type>>();
  const argsRef = useRef<Args>();
  const funcRef = useRef<(...args: Args) => Promise<Type>>();

  const reset = useCallback(() => {
    const func = funcRef.current!;
    const args = argsRef.current!;
    funcRef.current = undefined;
    argsRef.current = undefined;
    waitRef.current = undefined;
    return () => func?.(...args);
  }, []);

  return useCallback(
    async (...params: Args): Promise<Type> => {
      funcRef.current = callback;
      argsRef.current = params;
      if (waitRef.current) return waitRef.current;
      const promise = new Promise<Type>((resolve, reject) => {
        actionExecutor
          .wait()
          .then(reset)
          .then((func) => func())
          .then(resolve)
          .catch(reject)
          .finally(reset);
      });
      waitRef.current = promise;
      return promise;
    },
    [actionExecutor, callback, reset],
  );
}

export function ActionDataHandler({ formAtom }: { formAtom: FormAtom }) {
  const { actionHandler } = useFormScope();
  useActionAttrs({ formAtom, actionHandler });
  useActionValue({ formAtom, actionHandler });
  useActionRecord({ formAtom, actionHandler });
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
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom]),
  );
  const { action } = useViewTab();

  const notify = useAfterActions(
    useCallback(async () => {
      if (isEqual(recordRef.current, record)) return;
      recordRef.current = record;
      recordHandler.notify(
        createEvalContext(
          {
            ...action.context,
            ...record,
          },
          {
            fields: fields as unknown as EvalContextOptions["fields"],
            readonly,
          },
        ),
      );
    }, [action.context, fields, readonly, record, recordHandler]),
  );

  useAsyncEffect(notify);

  return null;
}
