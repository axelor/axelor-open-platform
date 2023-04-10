import { PrimitiveAtom, WritableAtom } from "jotai";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { Property, Schema } from "@/services/client/meta.types";
import { ActionExecutor, ActionHandler } from "@/view-containers/action";

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
  dirty?: boolean;
  record: DataRecord;
  states: Record<string, WidgetState>;
  statesByName: Record<string, WidgetState>;
  readonly model: string;
  readonly fields: Record<string, Property>;
  readonly parent?: FormAtom;
}

export type WidgetAtom = PrimitiveAtom<WidgetState>;

export type FormAtom = PrimitiveAtom<FormState>;

export type ValueAtom<T> = WritableAtom<
  T | null | undefined,
  [value: T | null | undefined, fireOnChange?: boolean],
  void
>;

export interface WidgetProps {
  schema: Schema;
  formAtom: FormAtom;
  widgetAtom: WidgetAtom;
  readonly?: boolean;
}

export interface FieldProps<T> extends WidgetProps {
  schema: Schema & { name: string };
  valueAtom: ValueAtom<T>;
}

export interface FormProps extends WidgetProps {
  record: DataRecord;
  fields: Record<string, Property>;
  actionHandler: ActionHandler;
  actionExecutor: ActionExecutor;
  className?: string;
  layout?: FormLayout;
}

export type FormLayout = (
  props: Omit<WidgetProps, "widgetAtom"> & {
    className?: string;
  }
) => JSX.Element;
