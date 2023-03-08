import { DataContext, DataRecord } from "@/services/client/data.types";
import { FormView, Property, Schema } from "@/services/client/meta.types";
import { PrimitiveAtom } from "jotai";

import { ViewData } from "@/services/client/meta";

// TODO: add more attrs that can be changed
export const DEFAULT_ATTRS = {
  readonly: undefined as unknown as boolean,
  required: undefined as unknown as boolean,
  hidden: undefined as unknown as boolean,
  domain: undefined as unknown as string,
  context: undefined as unknown as DataContext,
  precision: undefined as unknown as number,
  scale: undefined as unknown as number,
};

export type Attrs = Partial<typeof DEFAULT_ATTRS>;

export interface WidgetState {
  attrs: Attrs;
}

export interface FormState {
  record: DataRecord;
  states: Record<string, WidgetState>;
  fields: Record<string, Property>;
}

export interface WidgetProps {
  schema: Schema;
  formAtom: PrimitiveAtom<FormState>;
  widgetAtom: PrimitiveAtom<WidgetState>;
}

export interface FieldProps<T> extends WidgetProps {
  schema: Schema & { name: string };
  valueAtom: PrimitiveAtom<T>;
}

export interface FormProps {
  meta: ViewData<FormView>;
  className?: string;
  layout?: FormLayout;
}

export type FormLayout = (props: {
  schema: Schema;
  formAtom: PrimitiveAtom<FormState>;
  className?: string;
}) => JSX.Element;
