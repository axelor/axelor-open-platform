import { useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { useMemo } from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { DefaultActionExecutor } from "@/view-containers/action";

import { contextAtom, createFormAtom } from "./atoms";
import { GridLayout } from "./form-layouts";
import { ActionDataHandler, FormActionHandler, FormScope } from "./scope";
import { FormAtom, FormProps } from "./types";
import { processView } from "./utils";

/**
 * Hook to create form atom and action handlers
 *
 * @param meta the form meta
 * @param record the form record
 * @param parent the parent form atom
 * @returns form atom, action handler and action executor
 */
export function useFormHandlers(
  meta: ViewData<FormView>,
  record: DataRecord,
  parent?: FormAtom
) {
  const { model = "", fields = {} } = meta;
  const formAtom = useMemo(
    () => createFormAtom({ model, record, fields, parent }),
    [fields, model, parent, record]
  );

  const prepareContext = useSetAtom(contextAtom);

  const actionHandler = useMemo(
    () =>
      new FormActionHandler((options?: DataContext) =>
        prepareContext(formAtom, options)
      ),
    [formAtom, prepareContext]
  );

  const actionExecutor = useMemo(
    () => new DefaultActionExecutor(actionHandler),
    [actionHandler]
  );

  return {
    formAtom,
    actionHandler,
    actionExecutor,
  };
}

export function Form({
  schema: view,
  fields,
  formAtom,
  actionHandler,
  actionExecutor,
  className,
  readonly,
  layout: Layout = GridLayout,
}: FormProps) {
  const schema = useMemo(() => processView(view, fields), [view, fields]);
  return (
    <ScopeProvider
      scope={FormScope}
      value={{
        actionHandler,
        actionExecutor,
        formAtom,
      }}
    >
      <>
        <ActionDataHandler formAtom={formAtom} />
        <Layout
          className={className}
          readonly={readonly}
          schema={schema}
          formAtom={formAtom}
        />
      </>
    </ScopeProvider>
  );
}
