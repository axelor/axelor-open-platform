import { useAtomValue } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { useCallback, useMemo } from "react";

import { useTemplate } from "@/hooks/use-parser";
import { isReactTemplate } from "@/hooks/use-parser/utils";
import { DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView, Property } from "@/services/client/meta.types";
import { MetaScope } from "@/view-containers/views/scope";

import { useFormHandlers } from "./form";
import { FieldControl } from "./form-field";
import { FormScope } from "./scope";
import { FieldProps } from "./types";
import { isReferenceField } from "./utils";

import styles from "./form-viewers.module.scss";

export type FieldViewerProps = FieldProps<any>;
export type FormViewerProps = FieldViewerProps & {
  template: string;
  fields: Record<string, Property>;
};

export function FieldViewer(props: FieldViewerProps) {
  const { schema, formAtom } = props;
  const fieldsAtom = useMemo(
    () => selectAtom(formAtom, (o) => o.fields),
    [formAtom],
  );

  const formFields = useAtomValue(fieldsAtom);
  const fields = useMemo(
    () => schema.viewer.fields ?? schema.fields ?? formFields,
    [formFields, schema.fields, schema.viewer],
  );

  const { template } = schema.viewer!;

  // reference field?
  if (schema.serverType?.endsWith("_TO_ONE")) {
    return <ReferenceViewer {...props} template={template} fields={fields} />;
  }
  // collection field?
  if (schema.serverType?.endsWith("_TO_MANY")) {
    return <CollectionViewer {...props} template={template} fields={fields} />;
  }

  return <SimpleViewer {...props} template={template} fields={fields} />;
}

function SimpleViewer({ template, fields, ...props }: FormViewerProps) {
  const { formAtom } = props;
  const record = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record), [formAtom]),
  );
  const model = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.model), [formAtom]),
  );
  return (
    <FieldControl {...props} className={styles.viewer}>
      <TemplateViewer
        template={template}
        fields={fields}
        record={record}
        model={model}
        {...props}
      />
    </FieldControl>
  );
}

function ReferenceViewer({ template, fields, ...props }: FormViewerProps) {
  const { schema, valueAtom } = props;

  const model = schema.target!;
  const value = useAtomValue(valueAtom);
  const record = useMemo(() => value ?? {}, [value]);

  return (
    <FieldControl {...props} className={styles.viewer}>
      <RecordViewer
        model={model}
        template={template}
        fields={fields}
        record={record}
        {...props}
      />
    </FieldControl>
  );
}

function CollectionViewer({ template, fields, ...props }: FormViewerProps) {
  const { schema, valueAtom } = props;

  const model = schema.target!;
  const items = useAtomValue(valueAtom);
  const records: DataRecord[] = useMemo(() => items ?? [], [items]);

  return (
    <FieldControl {...props} className={styles.viewer}>
      {records.map((record) => (
        <RecordViewer
          key={record.id}
          model={model}
          template={template}
          fields={fields}
          record={record}
          {...props}
        />
      ))}
    </FieldControl>
  );
}

function RecordViewer({
  schema,
  model,
  fields,
  template,
  record,
  valueAtom,
  widgetAtom,
  formAtom: parent,
}: FormViewerProps & { model: string; record: DataRecord }) {
  const meta: ViewData<FormView> = useMemo(
    () => ({
      model,
      fields,
      view: {
        type: "form",
        items: (schema.items ?? []) as FormView["items"],
      },
    }),
    [fields, model, schema.items],
  );

  const { formAtom, actionHandler, actionExecutor, recordHandler } =
    useFormHandlers(meta, record, { parent });

  return (
    <ScopeProvider scope={MetaScope} value={meta}>
      <ScopeProvider
        scope={FormScope}
        value={{
          actionHandler,
          actionExecutor,
          recordHandler,
          formAtom,
        }}
      >
        <TemplateViewer
          model={model}
          fields={fields}
          template={template}
          record={record}
          schema={schema}
          formAtom={formAtom}
          valueAtom={valueAtom}
          widgetAtom={widgetAtom}
        />
      </ScopeProvider>
    </ScopeProvider>
  );
}

function TemplateViewer({
  template,
  record,
  model: _model,
  formAtom,
  fields,
  schema,
}: FormViewerProps & { model: string; record: DataRecord }) {
  const Template = useTemplate(template, schema);

  const isReact = useMemo(() => isReactTemplate(template), [template]);
  const isReference = useMemo(() => isReferenceField(schema), [schema]);

  // in v6, single-value relational field gets current record as
  // context, instead of current reference value
  const getLegacyContext = useAtomCallback(
    useCallback(
      (get) => {
        if (isReact || !isReference) return undefined;
        const { parent } = get(formAtom);
        const { model: _model, record } = get(parent!);
        return { _model, ...record };
      },
      [formAtom, isReact, isReference],
    ),
  );

  const ctx = useMemo(
    () => getLegacyContext() ?? { _model, ...record },
    [_model, getLegacyContext, record],
  );

  return (
    <div className={styles.content}>
      <Template context={ctx} options={{ fields }} />
    </div>
  );
}
