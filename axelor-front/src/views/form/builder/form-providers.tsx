import {
  forwardRef,
  ReactNode,
  useCallback,
  useImperativeHandle,
  useRef,
} from "react";
import { ScopeProvider } from "bunshi/react";

import {
  FormEditableScope,
  FormValidityHandler,
  FormValidityScope,
} from "./scope";
import { DataRecord } from "@/services/client/data.types";

export type FormWidgetsHandler = {
  invalid?: () => boolean;
  commit?: () => Promise<void | void[]>;
};

export const FormWidgetProviders = forwardRef<
  FormWidgetsHandler,
  { children: ReactNode; record?: DataRecord }
>(function FormWidgetProviders({ children, record }, ref) {
  const refs = useRef({
    widgets: new Set<FormValidityHandler>(),
    editableWidgets: new Set<() => void>(),
  });

  const handleAddWidgetValidator = useCallback((fn: FormValidityHandler) => {
    const widgets = refs.current.widgets;
    widgets.add(fn);
    return () => widgets.delete(fn);
  }, []);

  const handleAddEditableWidget = useCallback((fn: () => void) => {
    const editableWidgets = refs.current.editableWidgets;
    editableWidgets.add(fn);
    return () => editableWidgets.delete(fn);
  }, []);

  const handleCommitEditableWidgets = useCallback(() => {
    const editableWidgets = refs.current.editableWidgets;
    return Promise.all(Array.from(editableWidgets).map((fn) => fn()));
  }, []);

  const handleValidate = useCallback(() => {
    const widgets = refs.current.widgets;
    return Array.from(widgets).some((checkWidgetInvalid) =>
      checkWidgetInvalid(),
    );
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      invalid: handleValidate,
      commit: handleCommitEditableWidgets,
    }),
    [handleValidate, handleCommitEditableWidgets],
  );

  return (
    <ScopeProvider
      scope={FormEditableScope}
      value={{
        id: record?.id,
        add: handleAddEditableWidget,
        commit: handleCommitEditableWidgets,
      }}
    >
      <ScopeProvider
        scope={FormValidityScope}
        value={{
          add: handleAddWidgetValidator,
        }}
      >
        {children}
      </ScopeProvider>
    </ScopeProvider>
  );
});
