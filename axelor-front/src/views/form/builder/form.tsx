import { ScopeProvider } from "jotai-molecules";
import { memo, useCallback, useMemo, useRef } from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView } from "@/services/client/meta.types";
import { DefaultActionExecutor } from "@/view-containers/action";

import { useViewAction } from "@/view-containers/views/scope";
import { useAtomCallback } from "jotai/utils";
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
  const formAtom = useMemo(
    () =>
      createFormAtom({
        meta,
        record,
        parent,
        statesByName: initStates,
      }),
    [meta, record, parent, initStates]
  );

  const prepareContext = usePrepareContext(formAtom);
  const actionHandler = useMemo(() => {
    return new FormActionHandler((options?: DataContext) => {
      const ctx = prepareContext();
      return {
        ...ctx,
        ...options,
      };
    });
  }, [prepareContext]);

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

export function usePrepareContext(formAtom: FormAtom, options?: DataContext) {
  const actionView = useViewAction();
  const recordRef = useRef<DataRecord>();
  const contextRef = useRef<DataContext>();
  return useAtomCallback(
    useCallback(
      (get, set) => {
        const { meta, record, parent } = get(formAtom);
        if (recordRef.current === record) {
          return contextRef.current;
        }

        const json = meta.view.json ?? false;
        const ctxAtom = json && parent ? parent : formAtom;

        const { context, views } = actionView;
        const ctx: DataContext = parent
          ? {}
          : {
              ...context,
              _viewType: meta.view.type,
              _viewName: meta.view.name,
              _views: views?.map((x) => ({ name: x.name, type: x.type })),
            };
        const res = set(contextAtom, ctxAtom, {
          ...ctx,
          ...options,
        });
        recordRef.current = record;
        contextRef.current = res;
        return res;
      },
      [actionView, formAtom, options]
    )
  );
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
