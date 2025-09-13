import { useMemo } from "react";
import { produce, WritableDraft } from "immer";
import { PrimitiveAtom, SetStateAction, atom } from "jotai";

import { isCleanDummy, mergeCleanDummy } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { ActionView, FormView, Schema } from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";
import { deepEqual, deepGet, deepSet } from "@/utils/objects";
import { ActionExecutor } from "@/view-containers/action";

import { Attrs, FormAtom, FormState, WidgetAtom, WidgetState } from "./types";
import {
  createContextParams,
  defaultAttrs as getDefaultAttrs,
  processContextValues,
} from "./utils";

export function createFormAtom(props: {
  meta: ViewData<FormView>;
  record: DataRecord;
  context?: DataContext;
  parent?: PrimitiveAtom<FormState>;
  statesByName?: Record<string, WidgetState>;
}) {
  const { meta, record, parent, context, statesByName = {} } = props;
  const { model = "", fields = {} } = meta;
  const states: Record<string, WidgetState> = {};
  const select = Object.values(fields)
    .filter((field) => field.type?.endsWith("TO_MANY"))
    .reduce(
      (select, field) => ({
        ...select,
        [field.name]: {
          _model: field.target,
        },
      }),
      {},
    );
  return atom<FormState>({
    meta,
    model,
    record: { ...record },
    original: { ...record },
    select,
    states,
    statesByName,
    fields,
    parent,
    context,
  });
}

export function createWidgetAtom(props: {
  schema: Schema;
  formAtom: FormAtom;
  parentAtom?: WidgetAtom;
}) {
  const { schema, formAtom, parentAtom: parent } = props;
  const { uid, name = "__" } = schema;

  const defaultAttrs = getDefaultAttrs(schema);
  const defaultState: WidgetState = { name, parent, attrs: {} };

  let currentState: WidgetState;

  const widgetAtom = focusAtom(
    formAtom,
    ({ states = {} }) => {
      const stateById = states[uid] ?? defaultState;
      return {
        name,
        parent,
        ...stateById,
      } as WidgetState;
    },
    (state, slice) => ({ ...state, states: { ...state.states, [uid]: slice } }),
  );

  const derivedWidgetAtom = atom(
    (get) => {
      const { statesByName = {} } = get(formAtom);
      const stateByName = statesByName[name] ?? defaultState;

      const {
        attrs: attrsById,
        errors: errorsById,
        ...restById
      } = get(widgetAtom);

      const {
        attrs: attrsByName,
        errors: errorsByName,
        columns: columnsByName,
        ...restByName
      } = stateByName;

      const nextState = {
        ...restByName,
        ...restById,
        columns: columnsByName,
        errors: {
          ...errorsByName,
          ...errorsById,
        },
        attrs: {
          ...defaultAttrs,
          ...attrsByName,
          ...attrsById,
        },
      } as WidgetState;

      return currentState && deepEqual(currentState, nextState)
        ? currentState
        : (currentState = nextState);
    },
    (get, set, value: SetStateAction<WidgetState>) => set(widgetAtom, value),
  );

  return derivedWidgetAtom;
}

export function formDirtyUpdater(prev: FormState) {
  return prev.dirty ? prev : { ...prev, dirty: true };
}

export function createValueAtom({
  schema,
  formAtom,
  dirtyAtom,
  actionExecutor,
}: {
  schema: Schema;
  formAtom: FormAtom;
  dirtyAtom: PrimitiveAtom<boolean>;
  actionExecutor: ActionExecutor;
}) {
  const { name, readonly, onChange, inGridEditor } = schema;

  const triggerOnChange = () => {
    if (onChange) {
      const params = createContextParams(schema);
      const opts = params ? { context: params } : undefined;
      return actionExecutor.execute(onChange, opts);
    }
  };

  // special case for editable grid form
  const dotted = inGridEditor && readonly && name?.includes(".");

  const prop = name!;
  const lensAtom = dotted
    ? focusAtom(
        formAtom,
        (base) => {
          try {
            return deepGet(base.record, prop) ?? base.record[prop];
          } catch (err) {
            return null;
          }
        },
        (base, value) => {
          return { ...base, record: { ...base.record, [prop]: value } };
        },
      )
    : focusAtom(
        formAtom,
        (base) => {
          try {
            return deepGet(base.record, prop);
          } catch (err) {
            return null;
          }
        },
        (base, value) => {
          return produce(base, (draft) => {
            deepSet(draft.record, prop, value);
          });
        },
      );

  return atom<WidgetState, [SetStateAction<WidgetState>], void>(
    (get) => get(lensAtom),
    (
      get,
      set,
      value: any,
      fireOnChange: boolean = false,
      markDirty: boolean = true,
    ) => {
      const prev = get(lensAtom);
      const next =
        typeof value === "string" && value.trim() === "" ? null : value;

      if (deepEqual(prev, next)) return;

      const dirty =
        markDirty &&
        schema.canDirty !== false &&
        Boolean(name && !isCleanDummy(name));

      if (dirty) {
        set(formAtom, formDirtyUpdater);
        // skip view dirty atom update in editable grid changes
        if (!schema.inGridEditor) {
          set(dirtyAtom, true);
        }
      }

      set(lensAtom, next);

      return fireOnChange && triggerOnChange();
    },
  );
}

function canDirty(schema?: Schema) {
  if (!schema) return true;
  const { name, canDirty } = schema;
  return canDirty !== false && name && !isCleanDummy(name);
}

export const fallbackFormAtom = createFormAtom({
  meta: { view: { type: "form" } },
  record: {},
});

export const fallbackWidgetAtom = createWidgetAtom({
  formAtom: fallbackFormAtom,
  schema: {},
});

/**
 * This atom can be used to prepare context for the given formAtom.
 *
 * Example usage:
 *
 * ```tsx
 * const prepareContext = useSetAtom(contextAtom);
 * const context = prepareContext(formAtom, {
 *   _signal: "some"
 * })
 * ```
 */
export const contextAtom = atom(
  null,
  (
    get,
    set,
    formAtom: FormAtom,
    options: DataContext = {},
    actionView?: ActionView,
  ): DataContext => {
    const prepare = (formAtom: FormAtom, options?: DataContext) => {
      const { meta, model, record, context: _context, statesByName, parent } = get(formAtom);

      let context: DataContext = {
        ..._context,
        ...options,
        ...record,
        _model: model,
      };

      // set selected flag for o2m/m2m fields
      for (const name in statesByName) {
        const { selected } = statesByName[name];
        if (selected && Array.isArray(context[name])) {
          context[name] = context[name].map((value: DataRecord) =>
            value.id && selected.includes(value.id)
              ? { ...value, selected: true }
              : value,
          );
        }
      }

      const { view } = meta;
      const json = view.json ?? false;

      const [ctxView, ctxAction] = (() => {
        if (parent) {
          if (json) {
            // json is part of the main view
            return [get(parent)?.meta?.view, actionView];
          }
          // editor, parent context will be available
          return [view];
        }
        return [view, actionView];
      })();

      context = {
        ...createContextParams(ctxView, ctxAction),
        ...processContextValues(context, meta),
      };

      if (parent) {
        context._parent = prepare(parent);
      }
      return mergeCleanDummy(context);
    };

    return prepare(formAtom, options);
  },
);

const DEFAULT_ATTRS = {};

export function useWidgetAttrsAtomByName({
  schema,
  formAtom,
}: {
  schema: Schema;
  formAtom: FormAtom;
}) {
  const { name = "" } = schema;
  return useMemo(
    () =>
      atom(
        (get) => get(formAtom).statesByName?.[name]?.attrs ?? DEFAULT_ATTRS,
        (get, set, updater: (draft: WritableDraft<Attrs>) => void) => {
          set(
            formAtom,
            produce((draft) => {
              if (!draft.statesByName[name]) {
                draft.statesByName[name] = { attrs: {} };
              } else if (!draft.statesByName[name].attrs) {
                draft.statesByName[name].attrs = {};
              }
              const attrs = draft.statesByName[name].attrs!;
              updater(attrs);
            }),
          );
        },
      ),
    [name, formAtom],
  );
}
