import { PrimitiveAtom, WritableAtom } from "jotai";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView, Property, Schema } from "@/services/client/meta.types";
import { ActionExecutor, ActionHandler } from "@/view-containers/action";

// TODO: add more attrs that can be changed
export const DEFAULT_ATTRS = {
  readonly: undefined as unknown as boolean,
  required: undefined as unknown as boolean,
  focus: undefined as unknown as boolean,
  hidden: undefined as unknown as boolean,
  collapse: undefined as unknown as boolean,
  domain: undefined as unknown as string,
  context: undefined as unknown as DataContext,
  precision: undefined as unknown as number,
  scale: undefined as unknown as number,
  prompt: undefined as unknown as string,
  title: undefined as unknown as string,
  css: undefined as unknown as string,
  icon: undefined as unknown as string,
  selectionIn: undefined as unknown as string,
  canNew: undefined as unknown as boolean,
  canView: undefined as unknown as boolean,
  canEdit: undefined as unknown as boolean,
  canSave: undefined as unknown as boolean,
  canCopy: undefined as unknown as boolean,
  canRemove: undefined as unknown as boolean,
  canDelete: undefined as unknown as boolean,
  canArchive: undefined as unknown as boolean,
  canAttach: undefined as unknown as boolean,
  canSelect: undefined as unknown as boolean,
};

export type Attrs = Partial<typeof DEFAULT_ATTRS>;

export type WidgetErrors = {
  required?: string;
  invalid?: string;
  error?: string;
  min?: string;
  max?: string;
  pattern?: string;
};

export interface WidgetState {
  attrs: Attrs;
  errors?: WidgetErrors;
  columns?: Record<string, Attrs>;
  selected?: number[];
  readonly name?: string;
  readonly parent?: WidgetAtom;
}

export interface FormState {
  dirty?: boolean;
  record: DataRecord;
  original?: DataRecord;
  states: Record<string, WidgetState>;
  statesByName: Record<string, WidgetState>;
  widgetAtoms: Record<
    string,
    {
      name?: string;
      widgetAtom: WidgetAtom;
    }
  >;
  readonly meta: ViewData<FormView>;
  readonly model: string;
  readonly fields: Record<string, Property>;
  readonly parent?: FormAtom;
}

export type WidgetAtom = PrimitiveAtom<WidgetState>;

export type FormAtom = PrimitiveAtom<FormState>;

export type ValueAtom<T> = WritableAtom<
  T | null | undefined,
  [value: T | null | undefined, fireOnChange?: boolean, markDirty?: boolean],
  void
>;

export interface WidgetProps {
  schema: Schema;
  formAtom: FormAtom;
  widgetAtom: WidgetAtom;
  parentAtom?: WidgetAtom;
  readonly?: boolean;
}

export interface FieldProps<T> extends WidgetProps {
  schema: Schema & { name: string };
  valueAtom: ValueAtom<T>;
  invalid?: boolean;
}

export interface FormProps extends WidgetProps {
  fields: Record<string, Property>;
  actionHandler: ActionHandler;
  recordHandler: RecordHandler;
  actionExecutor: ActionExecutor;
  className?: string;
  layout?: FormLayout;
}

export type FormLayout = (
  props: Omit<WidgetProps, "widgetAtom"> & {
    className?: string;
  }
) => React.ReactNode;

export type RecordListener = (data: FormState["record"]) => void;

export interface RecordHandler {
  subscribe(subscriber: RecordListener): () => void;
  notify(data: FormState["record"]): void;
}
