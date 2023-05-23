import { useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { createEvalContext } from "@/hooks/use-parser/eval-context";
import { parseExpression, parseTemplate } from "@/hooks/use-parser/utils";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { Schema } from "@/services/client/meta.types";
import { validate } from "@/utils/validate";
import { useViewAction, useViewDirtyAtom } from "@/view-containers/views/scope";

import { createValueAtom, createWidgetAtom } from "./atoms";
import { FieldEditor } from "./form-editors";
import { FieldViewer } from "./form-viewers";
import { useWidget } from "./hooks";
import { useFormScope } from "./scope";
import { FieldProps, ValueAtom, WidgetAtom, WidgetProps } from "./types";

export function FormWidget(props: Omit<WidgetProps, "widgetAtom">) {
  const { schema, formAtom, parentAtom } = props;

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom, parentAtom }),
    [formAtom, parentAtom, schema]
  );

  const dirtyAtom = useViewDirtyAtom();
  const { actionExecutor } = useFormScope();

  const valueAtom = useMemo(
    () =>
      isField(schema)
        ? createValueAtom({ schema, formAtom, dirtyAtom, actionExecutor })
        : undefined,
    [actionExecutor, dirtyAtom, formAtom, schema]
  );

  const hidden = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs.hidden), [widgetAtom])
  );

  const readonly =
    useAtomValue(
      useMemo(
        () => selectAtom(widgetAtom, (a) => a.attrs.readonly),
        [widgetAtom]
      )
    ) || props.readonly;

  const canEdit = useAtomValue(
    useMemo(
      () => selectAtom(widgetAtom, (a) => a.attrs.canEdit ?? true),
      [widgetAtom]
    )
  );

  const canView = useAtomValue(
    useMemo(
      () => selectAtom(widgetAtom, (a) => a.attrs.canView ?? true),
      [widgetAtom]
    )
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
  });

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
    const editorProps = props as FieldProps<any>;
    return (
      <FieldEditor
        {...editorProps}
        widgetAtom={widgetAtom}
        valueAtom={valueAtom}
      />
    );
  }

  return <FormItem {...props} widgetAtom={widgetAtom} valueAtom={valueAtom} />;
}

function isField(schema: Schema) {
  const type = schema.type;
  return type === "field" || type === "panel-related";
}

function FormItem(props: WidgetProps & { valueAtom?: ValueAtom<any> }) {
  const { schema, formAtom, widgetAtom, valueAtom, readonly } = props;
  const attrs = useAtomValue(
    useMemo(() => selectAtom(widgetAtom, (a) => a.attrs), [widgetAtom])
  );
  const Comp = useWidget(schema);

  if (attrs.hidden) return null;

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
      [formAtom, schema, valueAtom, widgetAtom]
    )
  );

  // trigger validation on value change
  useAtomValue(valueAtom);
  useAsyncEffect(async (signal) => {
    signal.aborted || valueCheck();
  });

  const invalidAtom = useMemo(
    () =>
      selectAtom(
        widgetAtom,
        ({ errors = {} }) => Object.values(errors).filter(Boolean).length > 0
      ),
    [widgetAtom]
  );
  const invalid = useAtomValue(invalidAtom);

  return <Comp {...props} invalid={invalid} />;
}

function useExpressions({
  schema,
  widgetAtom,
  valueAtom,
  readonly = false,
}: {
  schema: Schema;
  widgetAtom: WidgetAtom;
  valueAtom?: ValueAtom<any>;
  readonly?: boolean;
}) {
  const setWidgetAttrs = useSetAtom(widgetAtom);
  const { formAtom, recordHandler } = useFormScope();
  const actionView = useViewAction();
  const popup = !!actionView.params?.popup;

  const valid = useAtomCallback(
    useCallback(
      (get, set, name?: string) => {
        const { states = {} } = get(formAtom);
        const invalid = Object.entries(states).some(
          ([k, { errors = {} }]) =>
            (name === undefined || k === name) && Object.keys(errors).length > 0
        );
        return !invalid;
      },
      [formAtom]
    )
  );

  const modeRef = useRef(readonly);
  const recordRef = useRef<DataRecord>();
  const contextRef = useRef<DataContext>();

  const createContext = useCallback(
    (record: DataRecord) => {
      let ctx = contextRef.current;
      if (
        ctx === undefined ||
        recordRef.current !== record ||
        modeRef.current !== readonly
      ) {
        ctx = createEvalContext(record, {
          valid,
          readonly,
          popup,
        });
        modeRef.current = readonly;
        recordRef.current = record;
        contextRef.current = ctx;
      }
      return ctx;
    },
    [popup, readonly, valid]
  );

  const handleBind = useAtomCallback(
    useCallback(
      (get, set, context: DataContext, bind: string) => {
        if (valueAtom) {
          const value = parseTemplate(bind)(context) ?? null;
          set(valueAtom, value, false, false);
        }
      },
      [valueAtom]
    )
  );

  const handleCondition = useCallback(
    (context: DataContext, attr: string, expr: string, negate = false) => {
      const value = Boolean(parseExpression(expr)(context));
      setWidgetAttrs((state) => {
        const attrs = state.attrs ?? {};
        const prev = attrs[attr as keyof typeof attrs];
        const next = negate ? !value : value;
        if (next !== prev) {
          return { ...state, attrs: { ...attrs, [attr]: next } };
        }
        return state;
      });
    },
    [setWidgetAttrs]
  );

  const handleValidation = useCallback(
    (context: DataContext, expr: string) => {
      const value = parseExpression(expr)(context);
      setWidgetAttrs((state) => {
        const errors = {
          ...state.errors,
          invalid: i18n.get("{0} is invalid", state.attrs.title),
        };
        if (value) Reflect.deleteProperty(errors, "invalid");
        if (isEqual(state.errors, errors)) return state;
        return { ...state, errors };
      });
    },
    [setWidgetAttrs]
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
      canSave,
      canCopy,
      canRemove,
      canDelete,
      canArchive,
      canAttach,
      canSelect,
      bind,
    } = schema;

    const hasExpression =
      showIf ||
      hideIf ||
      readonlyIf ||
      requiredIf ||
      validIf ||
      collapseIf ||
      canNew ||
      canEdit ||
      canSave ||
      canCopy ||
      canRemove ||
      canDelete ||
      canArchive ||
      canAttach ||
      canSelect;

    if (hasExpression || bind) {
      return recordHandler.subscribe((record) => {
        const ctx = createContext(record);
        if (bind) handleBind(ctx, bind);
        if (showIf) handleCondition(ctx, "hidden", showIf, true);
        if (hideIf) handleCondition(ctx, "hidden", hideIf);
        if (readonlyIf) handleCondition(ctx, "readonly", readonlyIf);
        if (requiredIf) handleCondition(ctx, "required", requiredIf);
        if (collapseIf) handleCondition(ctx, "collapsed", collapseIf);
        if (validIf) handleValidation(ctx, validIf);

        if (canNew) handleCondition(ctx, "canNew", canNew);
        if (canEdit) handleCondition(ctx, "canEdit", canEdit);
        if (canSave) handleCondition(ctx, "canSave", canSave);
        if (canCopy) handleCondition(ctx, "canCopy", canCopy);
        if (canRemove) handleCondition(ctx, "canRemove", canRemove);
        if (canDelete) handleCondition(ctx, "canDelete", canDelete);
        if (canArchive) handleCondition(ctx, "canArchive", canArchive);
        if (canAttach) handleCondition(ctx, "canAttach", canAttach);
        if (canSelect) handleCondition(ctx, "canSelect", canSelect);
      });
    }
  }, [
    schema,
    recordHandler,
    setWidgetAttrs,
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
