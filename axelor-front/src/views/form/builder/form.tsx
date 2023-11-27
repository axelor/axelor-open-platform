import { ScopeProvider } from "jotai-molecules";
import { memo, useCallback, useMemo, useRef } from "react";

import { DataContext, DataRecord } from "@/services/client/data.types";
import { ViewData } from "@/services/client/meta";
import { FormView, Perms, Schema } from "@/services/client/meta.types";
import { DefaultActionExecutor } from "@/view-containers/action";

import { usePerms } from "@/hooks/use-perms";
import { useViewAction } from "@/view-containers/views/scope";
import { useAtomValue } from "jotai";
import { useAtomCallback } from "jotai/utils";
import { contextAtom, createFormAtom } from "./atoms";
import { DottedValues } from "./dotted";
import { GridLayout } from "./form-layouts";
import {
  ActionDataHandler,
  FormActionHandler,
  FormRecordHandler,
  FormRecordUpdates,
  FormScope,
} from "./scope";
import { FormAtom, FormProps, WidgetAtom, WidgetState } from "./types";
import { processView } from "./utils";

/**
 * Hook to create form atom and action handlers
 *
 * @param meta the form meta
 * @param record the form record
 * @param options contains parent, initialize states, existing form atom
 * @returns form atom, action handler and action executor
 */
export function useFormHandlers(
  meta: ViewData<FormView>,
  record: DataRecord,
  options?: {
    parent?: FormAtom;
    states?: Record<string, WidgetState>;
    formAtom?: FormAtom;
    context?: DataContext;
  },
) {
  const {
    parent,
    context,
    states: statesByName,
    formAtom: givenFormAtom,
  } = options || {};
  const formAtom = useMemo(
    () =>
      givenFormAtom ??
      createFormAtom({
        meta,
        record,
        parent,
        context,
        statesByName,
      }),
    [givenFormAtom, meta, record, context, parent, statesByName],
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
    [actionHandler],
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
  const statesByNameRef = useRef<Record<string, WidgetState>>({});

  return useAtomCallback(
    useCallback(
      (get, set) => {
        const { meta, record, statesByName, parent } = get(formAtom);
        if (
          recordRef.current === record &&
          statesByName === statesByNameRef.current
        ) {
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
        statesByNameRef.current = statesByName;
        recordRef.current = record;
        contextRef.current = res;
        return res;
      },
      [actionView, formAtom, options],
    ),
  );
}

export function usePermission(
  schema: Schema,
  widgetAtom: WidgetAtom,
  perms?: Perms,
) {
  const { attrs } = useAtomValue(widgetAtom);
  const props = useMemo(() => ({ ...schema, ...attrs }), [attrs, schema]);
  return usePerms(props, perms);
}

export const Form = memo(function Form({
  schema: view,
  fields,
  formAtom,
  widgetAtom,
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
        <DottedValues formAtom={formAtom} />
        <Layout
          className={className}
          readonly={readonly}
          schema={schema}
          formAtom={formAtom}
          parentAtom={widgetAtom}
        />
      </>
    </ScopeProvider>
  );
});
