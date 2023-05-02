import { useSetAtom } from "jotai";
import { ScopeProvider } from "jotai-molecules";
import { memo, useMemo } from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { DefaultActionExecutor } from "@/view-containers/action";

import { useViewAction } from "@/view-containers/views/scope";
import { contextAtom, createFormAtom } from "./atoms";
import { GridLayout } from "./form-layouts";
import {
  ActionDataHandler,
  FormActionHandler,
  FormRecordHandler,
  FormRecordUpdates,
  FormScope,
} from "./scope";
import { FormAtom, FormProps, WidgetState } from "./types";
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
  parent?: FormAtom,
  initStates?: Record<string, WidgetState>
) {
  const { model = "", fields = {} } = meta;
  const formAtom = useMemo(
    () =>
      createFormAtom({
        model,
        record,
        fields,
        parent,
        statesByName: initStates,
      }),
    [fields, model, parent, record, initStates]
  );

  const json = meta.view.json ?? false;
  const ctxAtom = json && parent ? parent : formAtom;
  const actView = useViewAction();

  const prepareContext = useSetAtom(contextAtom);
  const actionHandler = useMemo(() => {
    // include action view details in root context
    const { context, views } = actView;
    const ctx: DataContext = parent
      ? {}
      : {
          ...context,
          _viewType: "form",
          _viewName: views?.find((x) => x.type === "form")?.name,
          _views: views?.map((x) => ({ name: x.name, type: x.type })),
        };
    return new FormActionHandler((options?: DataContext) =>
      prepareContext(ctxAtom, {
        ...ctx,
        ...options,
      })
    );
  }, [actView, ctxAtom, parent, prepareContext]);

  const actionExecutor = useMemo(
    () => new DefaultActionExecutor(actionHandler),
    [actionHandler]
  );

  const recordHandler = useMemo(() => new FormRecordHandler(), []);

  return {
    formAtom,
    recordHandler,
    actionHandler,
    actionExecutor,
  };
}

export const Form = memo(function Form({
  schema: view,
  fields,
  formAtom,
  recordHandler,
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
        recordHandler,
        formAtom,
      }}
    >
      <>
        <FormRecordUpdates
          fields={fields}
          readonly={readonly}
          formAtom={formAtom}
          recordHandler={recordHandler}
        />
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
});
