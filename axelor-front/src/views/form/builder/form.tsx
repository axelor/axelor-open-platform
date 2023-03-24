import { useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { useMemo } from "react";

import { DefaultActionExecutor } from "@/view-containers/action";

import { contextAtom, createFormAtom } from "./atoms";
import { GridLayout } from "./form-layouts";
import { FormActionHandler, FormScope } from "./scope";
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
  const model = schema.model;
  const formAtom = useMemo(
    () => createFormAtom({ model, record, fields, parent }),
    [model, record, fields, parent]
  );

  const prepareContext = useSetAtom(contextAtom);

  const actionHandler = useMemo(
    () => new FormActionHandler(() => prepareContext(formAtom)),
    [formAtom, prepareContext]
  );

  const actionExecutor = useMemo(
    () => new DefaultActionExecutor(actionHandler),
    [actionHandler]
  );

  return (
    <ScopeProvider
      scope={FormScope}
      value={{
        actionHandler,
        actionExecutor,
        formAtom,
      }}
    >
      <Layout
        className={className}
        readonly={readonly}
        schema={schema}
        formAtom={formAtom}
      />
    </ScopeProvider>
  );
}
