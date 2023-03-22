import { useMemo } from "react";

import { GridLayout } from "./form-layouts";
import { FormProps } from "./types";
import { processView } from "./utils";
import { createFormAtom } from "./atoms";

export function Form({
  schema: view,
  fields = {},
  formAtom: parent,
  className,
  readonly,
  layout: Layout = GridLayout,
}: FormProps) {
  const schema = useMemo(() => processView(view, fields), [view, fields]);
  const formAtom = useMemo(
    () => createFormAtom({ fields, parent }),
    [fields, parent]
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
