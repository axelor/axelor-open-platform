import { PrimitiveAtom, atom } from "jotai";
import { focusAtom } from "jotai-optics";
import { SetStateAction } from "react";

import { isDummy, mergeDummy } from "@/services/client/data-utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";

import { FormAtom, FormState, WidgetState } from "./types";
import { defaultAttrs, processContextValues } from "./utils";

export function createFormAtom(props: {
  model: string;
  record: DataRecord;
  fields: Record<string, Property>;
  parent?: PrimitiveAtom<FormState>;
}) {
  const { model, record, fields, parent } = props;
  const states: Record<string, WidgetState> = {};
  const statesByName: Record<string, WidgetState> = {};
  return atom<FormState>({
    model,
    record: { ...record },
    states,
    statesByName,
    fields,
    parent,
  });
}

export function createWidgetAtom(props: {
  schema: Schema;
  formAtom: FormAtom;
}) {
  const { schema, formAtom } = props;
  const { uid, name = "__" } = schema;
  const attrs = defaultAttrs(schema);

  const attrsByIdAtom = focusAtom(formAtom, (o) =>
    o
      .prop("states")
      .prop(uid)
      .valueOr({ attrs: {} } as WidgetState)
  );

  const attrsByNameAtom = focusAtom(formAtom, (o) =>
    o
      .prop("statesByName")
      .prop(name)
      .valueOr({ attrs: {} } as WidgetState)
  );

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

      return {
        ...restByName,
        ...restById,
        columns: {
          ...columnsByName,
          ...columnsById,
        },
        errors: {
          ...errorsByName,
          ...errorsById,
        },
        attrs: {
          ...attrs,
          ...attrsByName,
          ...attrsById,
        },
      };
    },
    (get, set, state) => {
      set(attrsByIdAtom, state);
    }
  );

  return widgetAtom;
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
  const { name, onChange } = schema;
  const lensAtom = focusAtom(formAtom, (o) => {
    let lens = o.prop("record");
    if (name) {
      let path = name.split(".");
      let next = path.shift();
      while (next) {
        lens = lens.reread((v) => v || {});
        lens = lens.rewrite((v) => v || {});
        lens = lens.prop(next);
        next = path.shift();
      }
    }
    return lens;
  });
  return atom(
    (get) => get(lensAtom) as any,
    (
      get,
      set,
      value: any,
      fireOnChange: boolean = false,
      markDirty: boolean = true
    ) => {
      const prev = get(lensAtom);
      if (prev !== value) {
        const dirty = markDirty && Boolean(name && !isDummy(name));
        set(lensAtom, value);
        set(formAtom, (prev) => ({ ...prev, dirty: prev.dirty || dirty }));
        if (dirty) {
          set(dirtyAtom, true);
        }
      }
      if (onChange && fireOnChange) {
        actionExecutor.execute(
          onChange,
          name ? { context: { _source: name } } : {}
        );
      }
    }
  );
}

export const fallbackFormAtom = createFormAtom({
  model: "",
  record: {},
  fields: {},
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
      const { model, record, parent, statesByName } = get(formAtom);
      let context: DataContext = {
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
              : value
          );
        }
      }

      context = processContextValues(context);

      if (parent) {
        context._parent = prepare(parent, context);
      }
      return mergeDummy(context);
    };

    return prepare(formAtom, options);
  }
);
