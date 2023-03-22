import { PrimitiveAtom, atom } from "jotai";
import { focusAtom } from "jotai-optics";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";

import { FormAtom, FormState, WidgetState } from "./types";
import { defaultAttrs } from "./utils";

export function createFormAtom(props: {
  model: string;
  record: DataRecord;
  fields: Record<string, Property>;
  parent?: PrimitiveAtom<FormState>;
}) {
  const { model, record, fields, parent } = props;
  const states: Record<string, WidgetState> = {};
  return atom<FormState>({
    model,
    record: { ...record },
    states,
    fields,
    parent,
  });
}

export function createWidgetAtom(props: {
  schema: Schema;
  formAtom: FormAtom;
}) {
  const { schema, formAtom } = props;
  const { uid } = schema;
  const attrs = defaultAttrs(schema);
  return focusAtom(formAtom, (o) =>
    o.prop("states").prop(uid).valueOr({ attrs })
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
    const { model, record, parent } = get(formAtom);
    const context = {
      ...options,
      ...record,
      _model: model,
    };

    if (parent) {
      context._parent = set(contextAtom, parent);
    }

    return context;
  }
);
