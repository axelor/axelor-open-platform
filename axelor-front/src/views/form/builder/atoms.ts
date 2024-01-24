import { produce } from "immer";
import { PrimitiveAtom, SetStateAction, atom } from "jotai";

import { isCleanDummy, mergeCleanDummy } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView, Schema } from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";
import { deepEqual, deepGet, deepMerge, deepSet } from "@/utils/objects";
import { ActionExecutor } from "@/view-containers/action";

import { FormAtom, FormState, WidgetAtom, WidgetState } from "./types";
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
  return atom<FormState>({
    meta,
    model,
    record: { ...record },
    original: { ...record },
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

  const widgetAtom = focusAtom(
    formAtom,
    ({ states = {}, statesByName = {} }) => {
      const stateById = states[uid] ?? defaultState;
      const stateByName = statesByName[name] ?? defaultState;

      const {
        attrs: attrsByName,
        errors: errorsByName,
        columns: columnsByName,
        ...restByName
      } = stateByName;

      const {
        attrs: attrsById,
        errors: errorsById,
        columns: columnsById,
        ...restById
      } = stateById;

      const columns =
        columnsByName && columnsById
          ? deepMerge(columnsByName, columnsById)
          : columnsById || columnsByName;

      const nextState: WidgetState = {
        ...restByName,
        ...restById,
        columns,
        errors: {
          ...errorsByName,
          ...errorsById,
        },
        attrs: {
          ...defaultAttrs,
          ...attrsByName,
          ...attrsById,
        },
        name,
        parent,
      };

      return nextState;
    },
    (state, slice) => {
      return { ...state, states: { ...state.states, [uid]: slice } };
    },
    deepEqual,
  );

  return widgetAtom;
}

export function createValueAtom({
  schema,
  formAtom,
  dirtyAtom,
  actionExecutor,
  widgetSchema,
}: {
  schema: Schema;
  formAtom: FormAtom;
  dirtyAtom: PrimitiveAtom<boolean>;
  actionExecutor: ActionExecutor;
  widgetSchema?: Schema;
}) {
  const { name, editable, readonly, onChange } = schema;

  const triggerOnChange = () => {
    if (onChange) {
      const params = createContextParams(schema);
      const opts = params ? { context: params } : undefined;
      return actionExecutor.execute(onChange, opts);
    }
  };

  // special case for editable grid form
  const dotted = editable && readonly && name?.includes(".");

  const prop = name!;
  const lensAtom = dotted
    ? focusAtom(
        formAtom,
        (base) => deepGet(base.record, prop) ?? base.record[prop],
        (base, value) => {
          return { ...base, record: { ...base.record, [prop]: value } };
        },
      )
    : focusAtom(
        formAtom,
        (base) => deepGet(base.record, prop),
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
        Boolean(name && !isCleanDummy(name)) &&
        canDirty(widgetSchema);

      set(lensAtom, next);

      if (dirty) {
        set(formAtom, (prev) => (prev.dirty ? prev : { ...prev, dirty }));
        set(dirtyAtom, true);
      }

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
  (get, set, formAtom: FormAtom, options: DataContext = {}): DataContext => {
    const prepare = (formAtom: FormAtom, options?: DataContext) => {
      const {
        model,
        record,
        context: _context,
        parent,
        statesByName,
      } = get(formAtom);

      let context: DataContext = {
        ..._context,
        ...options,
        ...record,
        _model: model,
      };

      // set selected flag for o2m/m2m fields
      for (let name in statesByName) {
        const { selected } = statesByName[name];
        if (selected && Array.isArray(context[name])) {
          context[name] = context[name].map((value: DataRecord) =>
            value.id && selected.includes(value.id)
              ? { ...value, selected: true }
              : value,
          );
        }
      }

      context = processContextValues(context);

      if (parent) {
        context._parent = prepare(parent);
      }
      return mergeCleanDummy(context);
    };

    return prepare(formAtom, options);
  },
);
