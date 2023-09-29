import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { useMemo } from "react";

import { useTemplate } from "@/hooks/use-parser";
import { DataRecord } from "@/services/client/data.types";
import { FormView, GridView, Property } from "@/services/client/meta.types";

import { FieldControl } from "./form-field";
import { FieldProps } from "./types";

import { ViewData } from "@/services/client/meta";
import { MetaScope } from "@/view-containers/views/scope";
import { ScopeProvider } from "jotai-molecules";
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

  const meta = useMemo<ViewData<FormView>>(() => {
    return {
      view: {
        type: "form",
        items: [],
      },
      fields,
    };
  }, [fields]);

  return (
    <FieldControl {...props} className={styles.viewer}>
      <ScopeProvider scope={MetaScope} value={meta}>
        <RecordViewer
          template={template}
          fields={fields}
          record={record}
          {...props}
        />
      </ScopeProvider>
    </FieldControl>
  );
}

function CollectionViewer({ template, fields, ...props }: FormViewerProps) {
  const { valueAtom, schema } = props;
  const items = useAtomValue(valueAtom);
  const records: DataRecord[] = useMemo(() => items ?? [], [items]);
  const meta = useMemo<ViewData<GridView>>(() => {
    return {
      view: {
        type: "grid",
        items: schema.items as GridView["items"],
      },
      fields,
    };
  }, [fields, schema.items]);

  return (
    <FieldControl {...props} className={styles.viewer}>
      <ScopeProvider scope={MetaScope} value={meta}>
        {records.map((record) => (
          <RecordViewer
            key={record.id}
            template={template}
            fields={fields}
            record={record}
            {...props}
          />
        ))}
      </ScopeProvider>
    </FieldControl>
  );
}

function RecordViewer({
  template,
  record,
}: FormViewerProps & { record: DataRecord }) {
  const Template = useTemplate(template);


  return (
    <div className={styles.content}>
      <Template context={record} />
    </div>
  );
}
