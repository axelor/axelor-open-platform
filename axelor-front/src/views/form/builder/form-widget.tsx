import { useAtomValue } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import isUndefined from "lodash/isUndefined";
import pick from "lodash/pick";
import React, { useCallback, useEffect, useMemo, useRef } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { createEvalContext } from "@/hooks/use-parser/context";
import {
  isLegacyExpression,
  parseAngularExp,
  parseExpression,
} from "@/hooks/use-parser/utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Schema } from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";
import { validate } from "@/utils/validate";
import { useViewAction, useViewDirtyAtom } from "@/view-containers/views/scope";

import { createValueAtom, createWidgetAtom } from "./atoms";
import { FieldEditor } from "./form-editors";
import { FieldViewer } from "./form-viewers";
import { useWidget } from "./hooks";
import { useFormScope } from "./scope";
import {
  FieldProps,
  FormState,
  FormStateUpdater,
  ValueAtom,
  WidgetAtom,
  WidgetProps,
} from "./types";
import { isExpandableWidget } from "./utils";

type FormWidgetProps = Omit<WidgetProps, "widgetAtom"> & {
  render?: (props: WidgetProps) => React.ReactNode;
};

export function FormWidget(props: FormWidgetProps) {
  const { schema, formAtom, parentAtom, render: Comp } = props;

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom, parentAtom }),
    [formAtom, parentAtom, schema],
  );

  const dirtyAtom = useViewDirtyAtom();
  const { actionExecutor } = useFormScope();

  const valueAtom = useMemo(
    () =>
      isField(schema)
        ? createValueAtom({
            schema,
            formAtom,
            dirtyAtom,
            actionExecutor,
          })
        : undefined,
    [actionExecutor, dirtyAtom, formAtom, schema],
  );

  const hidden = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs.hidden), [widgetAtom]),
  );

  const readonly =
    useAtomValue(
      useMemo(
        () => selectAtom(widgetAtom, (a) => a.attrs.readonly),
        [widgetAtom],
      ),
    ) || props.readonly;

  const required = useAtomValue(
    useMemo(
      () => selectAtom(widgetAtom, (a) => a.attrs.required),
      [widgetAtom],
    ),
  );

  const canEdit = useAtomValue(
    useMemo(
      () => selectAtom(widgetAtom, (a) => a.attrs.canEdit ?? true),
      [widgetAtom],
    ),
  );

  const canView = useAtomValue(
    useMemo(
      () => selectAtom(widgetAtom, (a) => a.attrs.canView ?? true),
      [widgetAtom],
    ),
  );

  const canShowEditor = schema.editor && valueAtom && canEdit && !readonly;
  const canShowViewer = schema.viewer && valueAtom && canView && readonly;
  const showEditorAsViewer =
    schema.editor?.viewer && valueAtom && canView && readonly;

  // eval field expression showIf, hideIf etc
  useExpressions({
    schema,
    widgetAtom,
    valueAtom,
    readonly,
    required,
  });

  if (Comp) {
    return <Comp {...props} widgetAtom={widgetAtom} />;
  }

  // special cases
  // 1. show/hide of panel-tabs loses active tab of it.
  // always mount panel-tabs to persist it's state.
  // 2. collection grid (o2m/m2m) with expandable/tree-grid widget
  // to persist expandable state of rows
  if (
    hidden &&
    (schema.type === "panel-tabs" ||
      (schema.serverType?.endsWith("TO_MANY") && isExpandableWidget(schema)))
  ) {
    return (
      <FormItem {...props} widgetAtom={widgetAtom} valueAtom={valueAtom} />
    );
  }

  if (hidden) {
    return null;
  }

  if (canShowViewer) {
    const viewerProps = props as FieldProps<any>;
    return (
      <FieldViewer
        {...viewerProps}
        widgetAtom={widgetAtom}
        valueAtom={valueAtom}
      />
    );
  }

  if (canShowEditor || showEditorAsViewer) {
    const editorProps = {
      ...props,
      readonly,
      widgetAtom,
      valueAtom,
    } as FieldProps<any>;
    return schema.json ? (
      <FieldEditor {...editorProps} />
    ) : (
      <FormField component={FieldEditor} {...editorProps} />
    );
  }

  return <FormItem {...props} widgetAtom={widgetAtom} valueAtom={valueAtom} />;
}

function isField(schema: Schema) {
  const type = schema.type;
  return schema.jsonField || type === "field" || type === "panel-related";
}

function FormItem(props: WidgetProps & { valueAtom?: ValueAtom<any> }) {
  const { schema, formAtom, widgetAtom, valueAtom, readonly } = props;
  const attrs = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs), [widgetAtom]),
  );
  const Comp = useWidget(schema);

  const widgetProps = {
    schema,
    formAtom,
    widgetAtom,
    readonly: readonly || attrs.readonly,
  };

  if (Comp) {
    return valueAtom ? (
      <FormField component={Comp} {...widgetProps} valueAtom={valueAtom} />
    ) : (
      <Comp {...widgetProps} />
    );
  }
  return <Unknown {...widgetProps} />;
}

function FormField({
  component: Comp,
  ...props
}: WidgetProps & { component: React.ElementType; valueAtom: ValueAtom<any> }) {
  const { schema, formAtom, widgetAtom, valueAtom } = props;
  const { name = "" } = schema;

  const serverErrorAtom = useMemo(
    () =>
      focusAtom(
        formAtom,
        (formState) => formState.statesByName[name]?.errors?.error,
        (formState, error) => {
          const { statesByName = {} } = formState;
          const state = statesByName[name] ?? {};
          const errors = state.errors ?? {};
          return {
            ...formState,
            statesByName: {
              ...statesByName,
              [name]: { ...state, errors: { ...errors, error } },
            },
          };
        },
      ),
    [formAtom, name],
  );

  const clearError = useAtomCallback(
    useCallback(
      (get, set) => {
        if (get(serverErrorAtom)) {
          set(serverErrorAtom, "");
        }
      },
      [serverErrorAtom],
    ),
  );

  const valueCheck = useAtomCallback(
    useCallback(
      (get, set) => {
        const value = get(valueAtom);
        const prev = get(widgetAtom);
        const record = get(formAtom).record;

        let errors = validate(value, {
          props: {
            ...schema,
            ...prev.attrs,
          } as any,
          context: record,
        });

        let invalid = prev.errors?.invalid;
        errors = invalid ? { invalid, ...errors } : errors;

        if (isEqual(prev.errors ?? {}, errors ?? {})) return;

        set(widgetAtom, (prev) => ({ ...prev, errors }));
      },
      [formAtom, schema, valueAtom, widgetAtom],
    ),
  );

  // trigger validation on value change
  const value = useAtomValue(valueAtom);
  useAsyncEffect(async (signal) => {
    signal.aborted || valueCheck();
  });

  useAsyncEffect(
    async (signal) => {
      signal.aborted || clearError();
    },
    [value, clearError],
  );

  const invalidAtom = useMemo(
    () =>
      selectAtom(
        widgetAtom,
        ({ errors = {} }) => Object.values(errors).filter(Boolean).length > 0,
      ),
    [widgetAtom],
  );
  const invalid = useAtomValue(invalidAtom);

  return <Comp {...props} invalid={invalid} />;
}

function useExpressions({
  schema,
  widgetAtom,
  valueAtom,
  readonly = false,
  required = false,
}: {
  schema: Schema;
  widgetAtom: WidgetAtom;
  valueAtom?: ValueAtom<any>;
  readonly?: boolean;
  required?: boolean;
}) {
  const { formAtom, recordHandler } = useFormScope();
  const actionView = useViewAction();
  const popup = !!actionView.params?.popup;

  const valid = useAtomCallback(
    useCallback(
      (get, set, name?: string) => {
        const { states = {} } = get(formAtom);
        const invalid = Object.entries(states).some(
          ([k, { errors = {} }]) =>
            (name === undefined || k === name) &&
            Object.keys(errors).length > 0,
        );
        return !invalid;
      },
      [formAtom],
    ),
  );

  const modeRef = useRef({ readonly, required });
  const recordRef = useRef<DataRecord>();
  const contextRef = useRef<DataContext>();

  const createContext = useCallback(
    (record: DataRecord) => {
      let ctx = contextRef.current;
      if (
        ctx === undefined ||
        recordRef.current !== record ||
        modeRef.current.readonly !== readonly ||
        modeRef.current.required !== required
      ) {
        ctx = createEvalContext(record, {
          valid,
          readonly,
          required,
          popup,
        });
        modeRef.current = { readonly, required };
        recordRef.current = record;
        contextRef.current = ctx;
      }
      return ctx;
    },
    [popup, readonly, required, valid],
  );

  const handleBind = useAtomCallback(
    useCallback(
      (get, set, context: DataContext, bind: string) => {
        if (valueAtom) {
          const func = isLegacyExpression(bind)
            ? parseAngularExp(bind)
            : parseExpression(bind);

          const prevValue = get(valueAtom);
          const value = func(context) ?? null;

          if (value === prevValue) return;

          // skip dirty for initial value set
          const isDirty = isUndefined(prevValue) ? false : prevValue !== value;
          set(valueAtom, value, false, isDirty);
        }
      },
      [valueAtom],
    ),
  );

  const handleCondition = useAtomCallback(
    useCallback(
      (
        get,
        set,
        context: DataContext,
        attr: string,
        expr: string,
        options?: {
          negate?: boolean;
          updater?: FormStateUpdater;
        },
      ) => {
        const { negate = false, updater } = options ?? {};
        const value = Boolean(parseExpression(expr)(context));
        const restoreState = pick(get(widgetAtom), ["name", "parent"]);

        updater?.((formState: FormState) => {
          const { states } = formState;
          const { uid } = schema;
          const state = formState?.states[uid];
          const attrs = state?.attrs ?? {};
          const prev = attrs[attr as keyof typeof attrs];
          const next = negate ? !value : value;

          if (next === prev) return formState;

          return {
            ...formState,
            states: {
              ...states,
              [uid]: {
                ...restoreState,
                ...state,
                attrs: { ...state?.attrs, [attr]: next },
              },
            },
          };
        });

        return value;
      },
      [schema, widgetAtom],
    ),
  );

  const handleValidation = useAtomCallback(
    useCallback(
      (get, set, context: DataContext, expr: string) => {
        const value = parseExpression(expr)(context);
        const state = get(widgetAtom);
        const errors = {
          ...state.errors,
          invalid: i18n.get("{0} is invalid", state.attrs.title),
        };
        if (value) Reflect.deleteProperty(errors, "invalid");
        if (!isEqual(state.errors, errors)) {
          set(widgetAtom, (prev) => ({ ...prev, errors }));
        }
      },
      [widgetAtom],
    ),
  );

  useEffect(() => {
    const {
      showIf,
      hideIf,
      readonlyIf,
      requiredIf,
      validIf,
      collapseIf,
      canNew,
      canEdit,
      canView,
      canSave,
      canCopy,
      canRemove,
      canDelete,
      canArchive,
      canAttach,
      canSelect,
      valueExpr,
    } = schema;

    const bind = schema.jsonField && valueExpr ? valueExpr : schema.bind;

    const isExpr = (attr: unknown) => typeof attr === "string";

    const hasExpression =
      showIf ||
      hideIf ||
      readonlyIf ||
      requiredIf ||
      validIf ||
      collapseIf ||
      isExpr(canNew) ||
      isExpr(canEdit) ||
      isExpr(canSave) ||
      isExpr(canCopy) ||
      isExpr(canView) ||
      isExpr(canRemove) ||
      isExpr(canDelete) ||
      isExpr(canArchive) ||
      isExpr(canAttach) ||
      isExpr(canSelect);

    if (hasExpression || bind) {
      return recordHandler.subscribe((record, updater) => {
        const ctx = createContext(record);
        const handleIf = (attr: string, expr: string, negate?: boolean) => {
          return handleCondition(ctx, attr, expr, { negate, updater });
        };

        let hidden = false;

        if (hideIf) hidden = handleIf("hidden", hideIf);
        if (!hidden && showIf) handleIf("hidden", showIf, true);

        if (bind) handleBind(ctx, bind);
        if (readonlyIf) handleIf("readonly", readonlyIf);
        if (requiredIf) handleIf("required", requiredIf);
        if (collapseIf) handleIf("collapse", collapseIf);
        if (validIf) handleValidation(ctx, validIf);

        if (isExpr(canNew)) handleIf("canNew", canNew);
        if (isExpr(canEdit)) handleIf("canEdit", canEdit);
        if (isExpr(canView)) handleIf("canView", canView);
        if (isExpr(canSave)) handleIf("canSave", canSave);
        if (isExpr(canCopy)) handleIf("canCopy", canCopy);
        if (isExpr(canRemove)) handleIf("canRemove", canRemove);
        if (isExpr(canDelete)) handleIf("canDelete", canDelete);
        if (isExpr(canArchive)) handleIf("canArchive", canArchive);
        if (isExpr(canAttach)) handleIf("canAttach", canAttach);
        if (isExpr(canSelect)) handleIf("canSelect", canSelect);
      });
    }
  }, [
    schema,
    recordHandler,
    handleBind,
    handleCondition,
    handleValidation,
    createContext,
  ]);
}

function Unknown(props: WidgetProps) {
  const { schema } = props;
  return <div>{schema.widget}</div>;
}
