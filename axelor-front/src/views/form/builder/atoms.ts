import { PrimitiveAtom, atom } from "jotai";
import { focusAtom } from "jotai-optics";

import { DataRecord } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";

import { FormAtom, FormState, WidgetState } from "./types";
import { defaultAttrs } from "./utils";

export function createFormAtom(props: {
  record: DataRecord;
  fields: Record<string, Property>;
  parent?: PrimitiveAtom<FormState>;
}) {
  const { record, fields, parent } = props;
  const states: Record<string, WidgetState> = {};
  return atom<FormState>({ record: { ...record }, states, fields, parent });
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
  record: {},
  fields: {},
});

export const fallbackWidgetAtom = createWidgetAtom({
  formAtom: fallbackFormAtom,
  schema: {},
});
