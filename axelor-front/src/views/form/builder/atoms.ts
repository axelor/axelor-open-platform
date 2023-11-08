import { PrimitiveAtom, WritableAtom, atom } from "jotai";
import { focusAtom } from "jotai-optics";
import isEqual from "lodash/isEqual";
import merge from "lodash/merge";
import { SetStateAction } from "react";

import { isCleanDummy, mergeCleanDummy } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView, Schema } from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";

import { FormAtom, FormState, WidgetAtom, WidgetState } from "./types";
import { defaultAttrs, processContextValues } from "./utils";

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
  const { uid, name } = schema;
  const attrs = defaultAttrs(schema);

  let prevState: WidgetState = { name, parent, attrs: {} };

  const attrsByIdAtom = focusAtom(formAtom, (o) =>
    o
      .prop("states")
      .prop(uid)
      .valueOr({ name, parent, attrs: {} } as WidgetState),
  );

  const attrsByNameAtom = focusAtom(formAtom, (o) =>
    o
      .prop("statesByName")
      .prop(name ?? "__")
      .valueOr({ name, parent, attrs: {} } as WidgetState),
  );

  const getStateByName = (key: keyof WidgetState, values: any) =>
    isEqual(prevState[key], values) ? prevState.columns : values;

  const widgetAtom = atom<WidgetState, [SetStateAction<WidgetState>], void>(
    (get) => {
      const {
        attrs: attrsByName,
        errors: errorsByName,
        columns: columnsByName,
        ...restByName
      } = get(attrsByNameAtom);

      const {
        attrs: attrsById,
        errors: errorsById,
        columns: columnsById,
        ...restById
      } = get(attrsByIdAtom);

      const columns = merge({}, columnsByName, columnsById);

      const nextState = {
        ...restByName,
        ...restById,
        columns: getStateByName("columns", columns),
        errors: {
          ...errorsByName,
          ...errorsById,
        },
        attrs: {
          ...attrs,
          ...attrsByName,
          ...attrsById,
        },
        name,
        parent,
      };

      return isEqual(prevState, nextState)
        ? prevState
        : (prevState = nextState);
    },
    (get, set, state) => {
      set(attrsByIdAtom, (prevState) => {
        const prev = get(widgetAtom);
        const next = typeof state === "function" ? state(prev) : state;
        if (next !== prev) {
          return next;
        }
        return prevState;
      });
    },
  );

  return widgetAtom;
}

export function createValueFocusAtom(formAtom: FormAtom, path: string) {
  const names = path?.split(".") ?? [];
  const lensAtom = focusAtom(formAtom, (o) => {
    let lens = o.prop("record");
    let next = names.shift();
    while (next) {
      lens = lens.reread((v) => v || {});
      lens = lens.rewrite((v) => v || {});
      lens = lens.prop(next);
      next = names.shift();
    }
    return lens;
  });
  return lensAtom as WritableAtom<any, [any], void>;
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
  const { name, editable, jsonField, jsonPath, readonly, onChange, $json } =
    schema;

  const triggerOnChange = () => {
    onChange &&
      actionExecutor.execute(
        onChange,
        name ? { context: { _source: name } } : {},
      );
  };

  if (!$json && jsonPath && jsonField) {
    const lensAtom = focusAtom(formAtom, (o) => {
      return o.prop("record").prop(jsonField);
    });
    const getJSON = (attrs: any) => {
      if (attrs && typeof attrs === "string") {
        try {
          return JSON.parse(attrs);
        } catch {
          // ignore
        }
      }
      return {};
    };
    return atom(
      (get) => {
        const attrs = getJSON(get(lensAtom));
        return attrs?.[jsonPath] ?? schema.defaultValue ?? "";
      },
      (
        get,
        set,
        value: any,
        fireOnChange: boolean = false,
        markDirty: boolean = true,
      ) => {
        const prev = get(lensAtom);
        const next = JSON.stringify({
          ...getJSON(prev),
          [jsonPath]: value,
        });
        if (prev !== next) {
          const dirty = Boolean(markDirty && name) && schema.canDirty !== false;

          set(lensAtom, next);
          set(formAtom, (prev) => ({ ...prev, dirty: prev.dirty || dirty }));
          dirty && set(dirtyAtom, true);
          fireOnChange && triggerOnChange();
        }
      },
    );
  }

  // special case for editable grid form
  const lensDottedAtom =
    editable && readonly && name?.includes(".")
      ? focusAtom(formAtom, (o) => o.prop("record").prop(name))
      : null;

  const lensAtom = createValueFocusAtom(formAtom, name ?? "");

  return atom(
    (get) => {
      const value = get(lensAtom);
      return (value ?? (lensDottedAtom ? get(lensDottedAtom) : value)) as any;
    },
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

      const dirty =
        markDirty &&
        schema.canDirty !== false &&
        Boolean(name && !isCleanDummy(name));

      if (!isEqual(prev, next)) {
        set(lensAtom, next);
        if (dirty) {
          set(formAtom, (prev) => (prev.dirty ? prev : { ...prev, dirty }));
          set(dirtyAtom, true);
        }
      }


      fireOnChange && triggerOnChange();
    },
  );
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
