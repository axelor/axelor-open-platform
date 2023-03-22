import { useMemo } from "react";

import { createFormAtom } from "./atoms";
import { GridLayout } from "./form-layouts";
import { FormProps } from "./types";
import { processView } from "./utils";

export function Form({
  schema: view,
  fields,
  record,
  formAtom: parent,
  className,
  readonly,
  layout: Layout = GridLayout,
}: FormProps) {
  const schema = useMemo(() => processView(view, fields), [view, fields]);
  const formAtom = useMemo(
    () => createFormAtom({ record, fields, parent }),
    [record, fields, parent]
  );
  return (
    <Layout
      className={className}
      readonly={readonly}
      schema={schema}
      formAtom={formAtom}
    />
  );
}
