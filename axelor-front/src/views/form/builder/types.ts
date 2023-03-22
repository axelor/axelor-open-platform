import { PrimitiveAtom } from "jotai";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";

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
  readonly fields: Record<string, Property>;
  readonly parent?: FormAtom;
}

export type WidgetAtom = PrimitiveAtom<WidgetState>;

export type FormAtom = PrimitiveAtom<FormState>;

export interface WidgetProps {
  schema: Schema;
  formAtom: FormAtom;
  widgetAtom: WidgetAtom;
  readonly?: boolean;
}

export interface FieldProps<T> extends WidgetProps {
  schema: Schema & { name: string };
  valueAtom: PrimitiveAtom<T>;
}

export interface FormProps extends WidgetProps {
  fields: Record<string, Property>;
  className?: string;
  layout?: FormLayout;
}

export type FormLayout = (
  props: Omit<WidgetProps, "widgetAtom"> & {
    className?: string;
  }
) => JSX.Element;
