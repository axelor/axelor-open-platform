import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useMemo } from "react";

import { useTemplate } from "@/hooks/use-parser";
import { DataRecord } from "@/services/client/data.types";
import { Property } from "@/services/client/meta.types";
import { useFormField, useFormScope } from "./scope";

import { FieldControl } from "./form-field";
import { FieldProps } from "./types";

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
    () => schema.fields ?? formFields,
    [formFields, schema.fields],
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
  return (
    <FieldControl {...props} className={styles.viewer}>
      <RecordViewer
        template={template}
        fields={fields}
        record={record}
        {...props}
      />
    </FieldControl>
  );
}

function ReferenceViewer({ template, fields, ...props }: FormViewerProps) {
  const { valueAtom } = props;
  const value = useAtomValue(valueAtom);
  const record = useMemo(() => value ?? {}, [value]);
  return (
    <FieldControl {...props} className={styles.viewer}>
      <RecordViewer
        template={template}
        fields={fields}
        record={record}
        {...props}
      />
    </FieldControl>
  );
}

function CollectionViewer({ template, fields, ...props }: FormViewerProps) {
  const { valueAtom } = props;
  const items = useAtomValue(valueAtom);
  const records: DataRecord[] = useMemo(() => items ?? [], [items]);

  return (
    <FieldControl {...props} className={styles.viewer}>
      {records.map((record) => (
        <RecordViewer
          key={record.id}
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
  template,
  fields,
  record,
}: FormViewerProps & { record: DataRecord }) {
  const { formAtom } = useFormScope();

  const Template = useTemplate(template);
  const $getField = useFormField(formAtom);
  const { actionExecutor } = useFormScope();

  const execute = actionExecutor.execute.bind(actionExecutor);

  // legacy templates may be using `record.` prefix
  const rec = useMemo(() => ({ ...record, record }), [record]);
  return (
    <div className={styles.content}>
      <Template
        context={rec}
        options={
          {
            execute,
            fields,
            helpers: { $getField },
          } as any
        }
      />
    </div>
  );
}
