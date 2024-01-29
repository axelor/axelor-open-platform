import { produce } from "immer";
import { atom, useAtomValue, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { isEqual, isNumber, set as setDeep } from "lodash";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import {
  EvalContextOptions,
  createEvalContext,
} from "@/hooks/use-parser/context";
import { isCleanDummy, updateRecord } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { Schema, View } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import {
  ActionAttrsData,
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
  FormState,
  RecordHandler,
  RecordListener,
  WidgetErrors,
  WidgetState,
} from "./types";

type ContextCreator = () => DataContext;

export class FormRecordHandler implements RecordHandler {
  #listeners = new Set<RecordListener>();
  #timer: number = 0;
  #getState?: () => FormState;
  #setState?: (state: FormState) => void;
  #record?: DataContext | null = null;
  #getRecord?: () => DataContext;

  #clearTimer() {
    if ("requestIdleCallback" in window) {
      window.cancelIdleCallback(this.#timer);
    } else {
      clearTimeout(this.#timer);
    }
  }

  #setTimer(cb: () => void) {
    if ("requestIdleCallback" in window) {
      this.#timer = window.requestIdleCallback(cb);
    } else {
      this.#timer = setTimeout(cb, 100) as unknown as number;
    }
  }

  setRecordUpdater(getRecord: () => DataRecord) {
    this.#record = null;
    this.#getRecord = getRecord;
  }

  setStateUpdater(getter: () => FormState, setter: (state: FormState) => void) {
    this.#getState = getter;
    this.#setState = setter;
  }

  subscribe(subscriber: RecordListener) {
    this.#listeners.add(subscriber);
    this.notify();
    return () => {
      this.#listeners.delete(subscriber);
    };
  }

  notify() {
    this.#clearTimer();
    this.#setTimer(() => {
      const record = this.#record ?? (this.#record = this.#getRecord?.());
      if (this.#getState && this.#setState && record) {
        const lastState = this.#getState();
        let state = lastState;
        this.#listeners.forEach((fn) =>
          fn(record, (update) => {
            state = update(state);
          }),
        );

        if (lastState !== state) {
          this.#setState(state);
        }
      }
    });
  }

  completed(): void {
    this.#clearTimer();
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

  setAttrs(attrs: ActionAttrsData["attrs"]) {
    this.notify({
      type: "attrs",
      attrs: attrs.map((attr) => ({
        ...attr,
        name: ATTR_MAPPER[attr.name] ?? attr.name,
      })),
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
      value: isNumber(value) ? { id: value } : value,
    });
  }

  delValue(target: string, value: any): void {
    this.notify({
      op: "del",
      type: "value",
      target,
      value: isNumber(value) ? { id: value } : value,
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

export type FormEditableScopeState = {
  add: (fn: () => void) => () => void;
  commit: () => void;
};

export const FormEditableScope = createScope<FormEditableScopeState>({
  add: () => () => {},
  commit: () => {},
});

const formEditableMolecule = molecule((getMol, getScope) => {
  return atom(getScope(FormEditableScope));
});

export function useFormEditableScope() {
  const scopeAtom = useMolecule(formEditableMolecule);
  return useAtomValue(scopeAtom);
}

export const FormTabScope = createScope<{ active: boolean }>({ active: true });

const formTabMolecule = molecule((getMol, getScope) => {
  return atom(getScope(FormTabScope));
});

export function useFormTabScope() {
  const scopeAtom = useMolecule(formTabMolecule);
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

/**
 * This hook is used to get handler, that handler will only executed
 * When widget is activated.
 *
 */
export function useFormActiveHandler() {
  const { active } = useFormTabScope();
  const [handler, setHandler] = useState<(() => void) | null>(
    null,
  );

  useEffect(() => {
    if (active && handler) {
      handler?.();
      setHandler(null);
    }
  }, [active, handler]);

  const doHandle = useCallback((handler: () => void) => {
    setHandler(() => handler);
  }, []);

  return doHandle;
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
  const { actionExecutor } = useFormScope();
  const doneRef = useRef<boolean>(false);

  useEffect(() => {
    doneRef.current = false;
    return () => {
      doneRef.current = true;
    };
  }, []);

  useEffect(() => {
    const unsubscribe = actionHandler.subscribe((data) => {
      if (doneRef.current) return;
      if (data) {
        if (check(data)) {
          handler(data as T);
        }
      }
    });

    return () => {
      actionExecutor.wait().then(unsubscribe);
    };
  }, [actionExecutor, actionHandler, check, handler]);
}

function isCollection(item: Schema) {
  const type = item.serverType ?? item.widget ?? item.type ?? "";
  return toKebabCase(type).endsWith("-to-many");
}

function useActionAttrs({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  const { findItem } = useViewMeta();
  useActionData<ActionAttrsData>(
    useCallback((x) => x.type === "attrs", []),
    useAtomCallback(
      useCallback(
        (get, set, { attrs }) => {
          let { statesByName, states } = get(formAtom);

          attrs.forEach((attr) => {
            const { target, name, value } = attr;

            const updateState = (
              newState: WidgetState,
              fieldName: string,
              columnName?: string,
            ) => {
              statesByName = { ...statesByName, [fieldName]: newState };
              states = produce(states, (prev) => {
                // reset widget's own state so that the attribute set by the action get preference.
                Object.values(prev).forEach((state) => {
                  if (state.name !== fieldName) return;
                  if (columnName) {
                    delete (state.columns?.[columnName] as any)?.[name];
                  } else if (name in state.attrs) {
                    delete (state.attrs as any)?.[name];
                  }
                });
              });
            };

            // collection field column ?
            if (target.includes(".")) {
              const fieldName = target.split(".")[0];
              const field = findItem(fieldName);
              if (field && isCollection(field) && !field.editor) {
                const state = statesByName[fieldName] ?? {};
                const column = target.substring(target.indexOf(".") + 1);
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
                return updateState(newState, fieldName, column);
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

            updateState(newState, target);
          });

          set(formAtom, (prev) => ({
            ...prev,
            statesByName,
            states,
          }));
        },
        [findItem, formAtom],
      ),
    ),
    actionHandler,
  );
}

export function useCanDirty() {
  const { findItem } = useViewMeta();
  const canDirty = useCallback(
    (target: string) => {
      const field = findItem(target);
      if (field?.name === "selected") return false;
      return field?.canDirty !== false && !isCleanDummy(target);
    },
    [findItem],
  );
  return canDirty;
}

function useActionValue({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  const { findItem } = useViewMeta();
  const canDirty = useCanDirty();

  useActionData<ActionValueData>(
    useCallback((x) => x.type === "value", []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          const { record } = get(formAtom);
          const { target, op } = data;
          let newRecord = record;
          let value = data.value;

          if (Array.isArray(value)) {
            value = value.map((x) => (isNumber(x) ? { id: x } : x));
          }

          if (op === "set") {
            newRecord = produce(record, (draft) => {
              if (target.includes(".")) {
                const fieldName = target.split(".")[0];
                const field = findItem(fieldName);
                if (field && isCollection(field)) {
                  draft[fieldName]?.forEach?.((item: DataRecord) => {
                    setDeep(item, target.slice(fieldName.length + 1), value);
                  });
                  return draft;
                }
              }
              setDeep(draft, target, value);
            });
          }

          if (op === "add" && value) {
            const items: DataRecord[] = record[target] ?? [];
            const records = Array.isArray(value) ? value : [value];
            const newItems = records.filter(
              (x) => !x.id || items.findIndex((y) => x.id === y.id) === -1,
            );
            const curItems = items.map((x) => {
              const found = records.find((y) => y.id && x.id === y.id);
              return found ? { ...x, ...found } : x;
            });
            newRecord = produce(record, (draft) => {
              setDeep(draft, target, [...curItems, ...newItems]);
            });
          }

          if (op === "del" && value) {
            const items: DataRecord[] = record[target] ?? [];
            const records = Array.isArray(value) ? value : [value];
            newRecord = produce(record, (draft) => {
              setDeep(
                draft,
                target,
                items.filter((item) => records.every((x) => item.id !== x.id)),
              );
            });
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

const updateJsonAtom = atom(
  null,
  (get, set, formAtom: FormAtom, values: DataRecord) => {
    const { meta, fields, parent: parentAtom } = get(formAtom);
    const isJson = Boolean(meta.view.json);
    if (isJson) {
      const parentState = parentAtom ? get(parentAtom) : null;
      const parentFields = parentState?.fields ?? {};
      const isOwn = (key: string) => key in fields || !(key in parentFields);
      const isJson = (key: string) => parentFields[key]?.json;
      const toJson = (value: unknown) =>
        value && typeof value !== "string" ? JSON.stringify(value) : value;

      const update = (targetAtom: FormAtom, vals: DataRecord) => {
        const record = get(targetAtom).record;
        const { _dirty, ...result } = updateRecord(record, vals) ?? {};
        if (_dirty) {
          set(targetAtom, (prev) => {
            return {
              ...prev,
              dirty: prev.dirty || _dirty,
              record: result ?? {},
            };
          });
        }
      };

      const ownValues: DataRecord = {};
      const topValues: DataRecord = {};

      for (const [key, value] of Object.entries(values)) {
        if (isOwn(key)) {
          ownValues[key] = value;
        } else {
          topValues[key] = isJson(key) ? toJson(value) : value;
        }
      }

      if (formAtom) update(formAtom, ownValues);
      if (parentAtom) update(parentAtom, topValues);

      return true;
    }
  },
);

function useActionRecord({
  formAtom,
  actionHandler,
}: {
  formAtom: FormAtom;
  actionHandler: ActionHandler;
}) {
  const canDirty = useCanDirty();
  const updateJson = useSetAtom(updateJsonAtom);
  useActionData<ActionValueData>(
    useCallback((x) => x.type === "record" && Boolean(x.value), []),
    useAtomCallback(
      useCallback(
        (get, set, data) => {
          // json field values?
          if (updateJson(formAtom, data.value)) {
            return;
          }

          const { record, fields } = get(formAtom);
          const values = (data.value ?? {}) as DataRecord;
          const result = updateRecord(record, values, fields);
          const isDirty = () =>
            result._dirty &&
            Object.entries(values).some(
              ([k, v]) => record[k] !== v && canDirty(k),
            );
          set(formAtom, (prev) => ({
            ...prev,
            dirty: prev.dirty || isDirty(),
            record: result,
          }));
        },
        [canDirty, formAtom, updateJson],
      ),
    ),
    actionHandler,
  );
}

export function useActionExecutor(
  view: View,
  options?: {
    formAtom?: FormAtom | null; // custom formAtom
    getContext?: () => DataContext;
    onRefresh?: () => Promise<any>;
    onSave?: () => Promise<any>;
  },
) {
  const { onSave, onRefresh, getContext } = options || {};
  const formScope = useFormScope();
  const formAtom =
    options?.formAtom === undefined
      ? formScope.formAtom
      : options?.formAtom ?? fallbackFormAtom;

  const actionHandler = useMemo(() => {
    const actionHandler = new FormActionHandler(() => ({
      ...getContext?.(),
      _viewName: view.name,
      _model: view.model,
    }));

    onSave && actionHandler.setSaveHandler(onSave);
    onRefresh && actionHandler.setRefreshHandler(onRefresh);

    return actionHandler;
  }, [getContext, onRefresh, onSave, view.model, view.name]);

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
        actionExecutor.waitFor().then(() =>
          actionExecutor
            .wait()
            .then(reset)
            .then((func) => func())
            .then(resolve)
            .catch(reject)
            .finally(reset),
        );
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

  const getFormState = useAtomCallback(
    useCallback((get) => get(formAtom), [formAtom]),
  );
  const setFormState = useAtomCallback(
    useCallback(
      (get, set, state: FormState) => set(formAtom, state),
      [formAtom],
    ),
  );

  useEffect(() => {
    recordHandler.setStateUpdater(getFormState, setFormState);
  }, [recordHandler, getFormState, setFormState]);

  useEffect(() => {
    recordHandler.setRecordUpdater(() =>
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
  }, [action.context, fields, readonly, record, recordHandler]);

  const notify = useAfterActions(
    useCallback(async () => {
      if (isEqual(recordRef.current, record)) return;
      recordRef.current = record;
      recordHandler.notify();
    }, [record, recordHandler]),
  );

  useAsyncEffect(notify);

  useEffect(() => {
    return () => {
      recordHandler.completed?.();
    };
  }, [recordHandler]);

  return null;
}
