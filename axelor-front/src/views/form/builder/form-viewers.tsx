import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useMemo } from "react";

import { useTemplate } from "@/hooks/use-parser";
import { DataRecord } from "@/services/client/data.types";
import { Property } from "@/services/client/meta.types";

import { ValueAtom, WidgetProps } from "./types";

import styles from "./form-viewers.module.scss";

export type FieldViewerProps = WidgetProps & { valueAtom: ValueAtom<any> };
export type FormViewerProps = FieldViewerProps & {
  template: string;
  fields: Record<string, Property>;
};

export function FieldViewer(props: FieldViewerProps) {
  const { schema, formAtom } = props;
  const fieldsAtom = useMemo(
    () => selectAtom(formAtom, (o) => o.fields),
    [formAtom]
  );

  const formFields = useAtomValue(fieldsAtom);
  const fields = useMemo(
    () => schema.fields ?? formFields,
    [formFields, schema.fields]
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

function SimpleViewer(props: FormViewerProps) {
  const { schema, formAtom, widgetAtom } = props;
  const showTitle = schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  const { record } = useAtomValue(formAtom);
  return (
    <div className={styles.viewer}>
      {showTitle && title && <label>{title}</label>}
      <RecordViewer {...props} record={record} />
    </div>
  );
}

function ReferenceViewer(props: FormViewerProps) {
  const { schema, valueAtom, widgetAtom } = props;
  const showTitle = schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  const value = useAtomValue(valueAtom);
  const record = useMemo(() => value ?? {}, [value]);
  return (
    <div className={styles.viewer}>
      {showTitle && title && <label>{title}</label>}
      <RecordViewer {...props} record={{ record }} />
    </div>
  );
}

function CollectionViewer(props: FormViewerProps) {
  const { schema, valueAtom, widgetAtom } = props;
  const showTitle = schema.showTitle ?? true;
  const { attrs } = useAtomValue(widgetAtom);
  const { title } = attrs;
  const items = useAtomValue(valueAtom);
  const records: DataRecord[] = useMemo(() => items ?? [], [items]);

  return (
    <div className={styles.viewer}>
      {showTitle && title && <label>{title}</label>}
      {records.map((record) => (
        <RecordViewer key={record.id} {...props} record={{ record }} />
      ))}
    </div>
  );
}

function RecordViewer(props: FormViewerProps & { record: DataRecord }) {
  const { template, record, fields } = props;
  const Template = useTemplate(template);
  return (
    <div className={styles.content}>
      <Template context={record} options={{ fields } as any} />
    </div>
  );
}
